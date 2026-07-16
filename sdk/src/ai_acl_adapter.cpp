#include "ai_acl_adapter.h"

#include <mutex>
#include <new>
#include <string>
#include <vector>
#include <fstream>
#include <cstring>
#include <sstream>
#include <algorithm>
#include <cmath>
#include <cctype>
#include <cstdint>

#include <cstdio>
#include <cerrno>
#include <sys/time.h>

#include "svp_acl.h"
#include "svp_acl_mdl.h"
#include "svp_acl_rt.h"

/*
 * Hi3519DV500 SVP ACL inference backend.
 *
 * This adapter owns the ACL runtime, active OM model, reusable input/output
 * Datasets, post-processing, and a short-lived detection cache. g_acl_mutex
 * serializes public calls so model switching cannot race active inference.
 */
namespace {

struct AiDetection {
    td_s32 class_id;
    float confidence;
    float x1;
    float y1;
    float x2;
    float y2;
    std::string class_name;
};

struct AiAclAdapterContext {
    std::string model_path;
    std::string config_path;
    std::string classes_path;
    bool inference_enabled;
    bool reporter_enabled;
    bool model_receiver_enabled;
    int device_id;
    bool acl_inited;
    bool device_opened;
    bool timeout_set;
    bool context_created;
    bool stream_created;
    bool model_loaded;
    svp_acl_rt_context context;
    svp_acl_rt_stream stream;
    td_void *model_mem_ptr;
    td_ulong model_mem_size;
    td_u32 model_id;
    svp_acl_mdl_desc *model_desc;
    size_t input_count;
    size_t output_count;
    svp_acl_mdl_dataset *input_dataset;
    svp_acl_mdl_dataset *output_dataset;
    std::vector<std::string> classes;
    std::vector<AiDetection> cached_detections;
    td_s32 cached_frame_id;
    td_u32 cached_frame_w;
    td_u32 cached_frame_h;
    td_s32 empty_infer_count;
};

struct AiPreprocessMeta {
    td_u32 model_w;
    td_u32 model_h;
    td_u32 pad_x;
    td_u32 pad_y;
    td_u32 resized_w;
    td_u32 resized_h;
    float scale_x;
    float scale_y;
    float letterbox_scale;
    td_bool use_letterbox;
};

struct AiYolo11OutputTensor {
    td_void *buffer;
    size_t buffer_size;
    svp_acl_data_type type;
    svp_acl_mdl_io_dims dims;
    size_t stride_bytes;
    td_s32 channels;
    td_s32 grid_h;
    td_s32 grid_w;
};

constexpr float AI_DET_SCORE_THRESHOLD = 0.25f;
constexpr float AI_DET_LIVE_SCORE_THRESHOLD = 0.25f;
constexpr float AI_DET_NMS_THRESHOLD = 0.45f;
constexpr float AI_RPN_NMS_THRESHOLD = 0.9f;
constexpr float AI_RPN_SCORE_THRESHOLD = 0.15f;
constexpr float AI_RPN_MIN_HEIGHT = 1.0f;
constexpr float AI_RPN_MIN_WIDTH = 1.0f;
constexpr td_s32 AI_DET_HOLD_FRAMES = 30;
constexpr td_s32 AI_DET_CLEAR_EMPTY_INFER = 3;
constexpr size_t AI_DET_MAX_RAW_BOXES = 84000;
constexpr td_s32 AI_YOLO11_CLASS_COUNT = 80;
constexpr td_s32 AI_YOLO11_DFL_LEN = 16;
constexpr td_s32 AI_YOLO11_MODEL_SIZE = 640;
constexpr td_bool AI_DRAW_LABEL_TEXT = TD_TRUE;

/* Lock order when both are required: g_acl_mutex before g_detection_mutex. */
std::mutex g_acl_mutex;
std::mutex g_detection_mutex;
AiAclAdapterContext *g_acl_ctx = nullptr;
std::string g_acl_block_reason;

td_void ai_acl_adapter_destroy_dataset(svp_acl_mdl_dataset **dataset);

td_u64 ai_acl_adapter_now_ms()
{
    struct timeval tv;
    gettimeofday(&tv, nullptr);
    return (td_u64)tv.tv_sec * 1000 + (td_u64)tv.tv_usec / 1000;
}

void set_block_reason_text(const std::string &reason)
{
    g_acl_block_reason = reason;
}

void set_block_reason_errno(const char *prefix, const std::string &path)
{
    std::ostringstream oss;
    oss << prefix << ": path=\"" << path << "\", errno=" << errno
        << " (" << std::strerror(errno) << ")";
    set_block_reason_text(oss.str());
}

const char *safe_cstr(const td_char *value)
{
    return (value != nullptr) ? value : "";
}

const char *acl_data_type_name(svp_acl_data_type type)
{
    switch (type) {
        case SVP_ACL_FLOAT:
            return "float32";
        case SVP_ACL_FLOAT16:
            return "float16";
        case SVP_ACL_INT8:
            return "int8";
        case SVP_ACL_INT32:
            return "int32";
        case SVP_ACL_UINT8:
            return "uint8";
        case SVP_ACL_INT16:
            return "int16";
        case SVP_ACL_UINT16:
            return "uint16";
        case SVP_ACL_UINT32:
            return "uint32";
        case SVP_ACL_INT64:
            return "int64";
        case SVP_ACL_UINT64:
            return "uint64";
        case SVP_ACL_DOUBLE:
            return "double";
        default:
            return "unknown";
    }
}

const char *acl_format_name(svp_acl_format format)
{
    switch (format) {
        case SVP_ACL_FORMAT_NCHW:
            return "NCHW";
        case SVP_ACL_FORMAT_NHWC:
            return "NHWC";
        case SVP_ACL_FORMAT_ND:
            return "ND";
        case SVP_ACL_FORMAT_NC1HWC0:
            return "NC1HWC0";
        case SVP_ACL_FORMAT_FRACTAL_Z:
            return "FRACTAL_Z";
        case SVP_ACL_FORMAT_NC1HWC0_C04:
            return "NC1HWC0_C04";
        case SVP_ACL_FORMAT_FRACTAL_NZ:
            return "FRACTAL_NZ";
        default:
            return "UNKNOWN";
    }
}

size_t acl_data_type_size_bytes(svp_acl_data_type type)
{
    switch (type) {
        case SVP_ACL_FLOAT:
            return sizeof(float);
        case SVP_ACL_FLOAT16:
            return sizeof(uint16_t);
        case SVP_ACL_INT8:
        case SVP_ACL_UINT8:
            return sizeof(uint8_t);
        case SVP_ACL_INT16:
        case SVP_ACL_UINT16:
            return sizeof(uint16_t);
        case SVP_ACL_INT32:
        case SVP_ACL_UINT32:
            return sizeof(uint32_t);
        case SVP_ACL_INT64:
        case SVP_ACL_UINT64:
            return sizeof(uint64_t);
        case SVP_ACL_DOUBLE:
            return sizeof(double);
        default:
            return 0;
    }
}

float fp16_to_float(uint16_t value)
{
    uint32_t sign = (uint32_t)(value & 0x8000U) << 16;
    uint32_t exponent = (value >> 10) & 0x1FU;
    uint32_t mantissa = value & 0x03FFU;
    uint32_t bits;
    float result;

    if (exponent == 0) {
        if (mantissa == 0) {
            bits = sign;
        } else {
            exponent = 1;
            while ((mantissa & 0x0400U) == 0) {
                mantissa <<= 1;
                exponent--;
            }
            mantissa &= 0x03FFU;
            bits = sign | ((exponent + (127 - 15)) << 23) | (mantissa << 13);
        }
    } else if (exponent == 0x1FU) {
        bits = sign | 0x7F800000U | (mantissa << 13);
    } else {
        bits = sign | ((exponent + (127 - 15)) << 23) | (mantissa << 13);
    }

    std::memcpy(&result, &bits, sizeof(result));
    return result;
}

td_u8 clamp_u8(td_s32 value)
{
    if (value < 0) {
        return 0;
    }
    if (value > 255) {
        return 255;
    }
    return (td_u8)value;
}

td_s32 clamp_s32(td_s32 value, td_s32 min_value, td_s32 max_value)
{
    if (value < min_value) {
        return min_value;
    }
    if (value > max_value) {
        return max_value;
    }
    return value;
}

td_void rgb_to_yuv(td_u8 r, td_u8 g, td_u8 b, td_u8 *y, td_u8 *u, td_u8 *v)
{
    td_s32 y_tmp;
    td_s32 u_tmp;
    td_s32 v_tmp;

    y_tmp = ((66 * r + 129 * g + 25 * b + 128) >> 8) + 16;
    u_tmp = ((-38 * r - 74 * g + 112 * b + 128) >> 8) + 128;
    v_tmp = ((112 * r - 94 * g - 18 * b + 128) >> 8) + 128;

    *y = clamp_u8(y_tmp);
    *u = clamp_u8(u_tmp);
    *v = clamp_u8(v_tmp);
}

td_void yuv420sp_put_pixel(td_u8 *buffer, td_u32 width, td_u32 height, td_u32 stride_y, td_u32 stride_uv,
    td_bool is_yvu_semiplanar_420, td_s32 x, td_s32 y, td_u8 y_value, td_u8 u_value, td_u8 v_value)
{
    td_u8 *y_plane;
    td_u8 *uv_plane;
    td_u32 uv_x;
    td_u32 uv_y;
    td_u32 uv_index;

    if (buffer == nullptr || x < 0 || y < 0 || (td_u32)x >= width || (td_u32)y >= height) {
        return;
    }

    y_plane = buffer;
    uv_plane = buffer + stride_y * height;
    y_plane[y * stride_y + x] = y_value;

    uv_x = ((td_u32)x) & ~1U;
    uv_y = ((td_u32)y) & ~1U;
    uv_index = (uv_y / 2) * stride_uv + uv_x;
    if (is_yvu_semiplanar_420 == TD_TRUE) {
        uv_plane[uv_index + 0] = v_value;
        uv_plane[uv_index + 1] = u_value;
    } else {
        uv_plane[uv_index + 0] = u_value;
        uv_plane[uv_index + 1] = v_value;
    }
}

td_void yuv420sp_fill_rect(td_u8 *buffer, td_u32 width, td_u32 height, td_u32 stride_y, td_u32 stride_uv,
    td_bool is_yvu_semiplanar_420, td_s32 x1, td_s32 y1, td_s32 x2, td_s32 y2, td_u8 y_value, td_u8 u_value,
    td_u8 v_value)
{
    td_s32 x;
    td_s32 y;

    x1 = clamp_s32(x1, 0, (td_s32)width);
    y1 = clamp_s32(y1, 0, (td_s32)height);
    x2 = clamp_s32(x2, 0, (td_s32)width);
    y2 = clamp_s32(y2, 0, (td_s32)height);
    if (x2 <= x1 || y2 <= y1) {
        return;
    }

    for (y = y1; y < y2; ++y) {
        for (x = x1; x < x2; ++x) {
            yuv420sp_put_pixel(buffer, width, height, stride_y, stride_uv, is_yvu_semiplanar_420,
                x, y, y_value, u_value, v_value);
        }
    }
}

td_void yuv420sp_draw_rect(td_u8 *buffer, td_u32 width, td_u32 height, td_u32 stride_y, td_u32 stride_uv,
    td_bool is_yvu_semiplanar_420, td_s32 x1, td_s32 y1, td_s32 x2, td_s32 y2, td_s32 thickness, td_u8 y_value,
    td_u8 u_value, td_u8 v_value)
{
    td_s32 i;

    if (thickness <= 0) {
        thickness = 1;
    }

    for (i = 0; i < thickness; ++i) {
        yuv420sp_fill_rect(buffer, width, height, stride_y, stride_uv, is_yvu_semiplanar_420,
            x1, y1 + i, x2, y1 + i + 1, y_value, u_value, v_value);
        yuv420sp_fill_rect(buffer, width, height, stride_y, stride_uv, is_yvu_semiplanar_420,
            x1, y2 - i - 1, x2, y2 - i, y_value, u_value, v_value);
        yuv420sp_fill_rect(buffer, width, height, stride_y, stride_uv, is_yvu_semiplanar_420,
            x1 + i, y1, x1 + i + 1, y2, y_value, u_value, v_value);
        yuv420sp_fill_rect(buffer, width, height, stride_y, stride_uv, is_yvu_semiplanar_420,
            x2 - i - 1, y1, x2 - i, y2, y_value, u_value, v_value);
    }
}

const td_u8 *ai_acl_adapter_font5x7(char c)
{
    static const td_u8 glyph_space[7] = {0, 0, 0, 0, 0, 0, 0};
    static const td_u8 glyph_dash[7]  = {0, 0, 0, 0x1F, 0, 0, 0};
    static const td_u8 glyph_dot[7]   = {0, 0, 0, 0, 0, 0x0C, 0x0C};
    static const td_u8 glyph_percent[7] = {0x19, 0x19, 0x02, 0x04, 0x08, 0x13, 0x13};
    static const td_u8 glyph_0[7] = {0x0E, 0x11, 0x13, 0x15, 0x19, 0x11, 0x0E};
    static const td_u8 glyph_1[7] = {0x04, 0x0C, 0x14, 0x04, 0x04, 0x04, 0x1F};
    static const td_u8 glyph_2[7] = {0x0E, 0x11, 0x01, 0x02, 0x04, 0x08, 0x1F};
    static const td_u8 glyph_3[7] = {0x1E, 0x01, 0x01, 0x0E, 0x01, 0x01, 0x1E};
    static const td_u8 glyph_4[7] = {0x02, 0x06, 0x0A, 0x12, 0x1F, 0x02, 0x02};
    static const td_u8 glyph_5[7] = {0x1F, 0x10, 0x10, 0x1E, 0x01, 0x01, 0x1E};
    static const td_u8 glyph_6[7] = {0x06, 0x08, 0x10, 0x1E, 0x11, 0x11, 0x0E};
    static const td_u8 glyph_7[7] = {0x1F, 0x01, 0x02, 0x04, 0x08, 0x08, 0x08};
    static const td_u8 glyph_8[7] = {0x0E, 0x11, 0x11, 0x0E, 0x11, 0x11, 0x0E};
    static const td_u8 glyph_9[7] = {0x0E, 0x11, 0x11, 0x0F, 0x01, 0x02, 0x0C};
    static const td_u8 glyph_a[7] = {0x0E, 0x11, 0x11, 0x1F, 0x11, 0x11, 0x11};
    static const td_u8 glyph_b[7] = {0x1E, 0x11, 0x11, 0x1E, 0x11, 0x11, 0x1E};
    static const td_u8 glyph_c[7] = {0x0F, 0x10, 0x10, 0x10, 0x10, 0x10, 0x0F};
    static const td_u8 glyph_d[7] = {0x1E, 0x11, 0x11, 0x11, 0x11, 0x11, 0x1E};
    static const td_u8 glyph_e[7] = {0x1F, 0x10, 0x10, 0x1E, 0x10, 0x10, 0x1F};
    static const td_u8 glyph_f[7] = {0x1F, 0x10, 0x10, 0x1E, 0x10, 0x10, 0x10};
    static const td_u8 glyph_g[7] = {0x0F, 0x10, 0x10, 0x13, 0x11, 0x11, 0x0F};
    static const td_u8 glyph_h[7] = {0x11, 0x11, 0x11, 0x1F, 0x11, 0x11, 0x11};
    static const td_u8 glyph_i[7] = {0x1F, 0x04, 0x04, 0x04, 0x04, 0x04, 0x1F};
    static const td_u8 glyph_j[7] = {0x1F, 0x02, 0x02, 0x02, 0x12, 0x12, 0x0C};
    static const td_u8 glyph_k[7] = {0x11, 0x12, 0x14, 0x18, 0x14, 0x12, 0x11};
    static const td_u8 glyph_l[7] = {0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x1F};
    static const td_u8 glyph_m[7] = {0x11, 0x1B, 0x15, 0x15, 0x11, 0x11, 0x11};
    static const td_u8 glyph_n[7] = {0x11, 0x11, 0x19, 0x15, 0x13, 0x11, 0x11};
    static const td_u8 glyph_o[7] = {0x0E, 0x11, 0x11, 0x11, 0x11, 0x11, 0x0E};
    static const td_u8 glyph_p[7] = {0x1E, 0x11, 0x11, 0x1E, 0x10, 0x10, 0x10};
    static const td_u8 glyph_q[7] = {0x0E, 0x11, 0x11, 0x11, 0x15, 0x12, 0x0D};
    static const td_u8 glyph_r[7] = {0x1E, 0x11, 0x11, 0x1E, 0x14, 0x12, 0x11};
    static const td_u8 glyph_s[7] = {0x0F, 0x10, 0x10, 0x0E, 0x01, 0x01, 0x1E};
    static const td_u8 glyph_t[7] = {0x1F, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04};
    static const td_u8 glyph_u[7] = {0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x0E};
    static const td_u8 glyph_v[7] = {0x11, 0x11, 0x11, 0x11, 0x11, 0x0A, 0x04};
    static const td_u8 glyph_w[7] = {0x11, 0x11, 0x11, 0x15, 0x15, 0x15, 0x0A};
    static const td_u8 glyph_x[7] = {0x11, 0x11, 0x0A, 0x04, 0x0A, 0x11, 0x11};
    static const td_u8 glyph_y[7] = {0x11, 0x11, 0x0A, 0x04, 0x04, 0x04, 0x04};
    static const td_u8 glyph_z[7] = {0x1F, 0x01, 0x02, 0x04, 0x08, 0x10, 0x1F};

    switch (c) {
        case ' ':
            return glyph_space;
        case '-':
            return glyph_dash;
        case '.':
            return glyph_dot;
        case '%':
            return glyph_percent;
        case '0':
            return glyph_0;
        case '1':
            return glyph_1;
        case '2':
            return glyph_2;
        case '3':
            return glyph_3;
        case '4':
            return glyph_4;
        case '5':
            return glyph_5;
        case '6':
            return glyph_6;
        case '7':
            return glyph_7;
        case '8':
            return glyph_8;
        case '9':
            return glyph_9;
        case 'A':
            return glyph_a;
        case 'B':
            return glyph_b;
        case 'C':
            return glyph_c;
        case 'D':
            return glyph_d;
        case 'E':
            return glyph_e;
        case 'F':
            return glyph_f;
        case 'G':
            return glyph_g;
        case 'H':
            return glyph_h;
        case 'I':
            return glyph_i;
        case 'J':
            return glyph_j;
        case 'K':
            return glyph_k;
        case 'L':
            return glyph_l;
        case 'M':
            return glyph_m;
        case 'N':
            return glyph_n;
        case 'O':
            return glyph_o;
        case 'P':
            return glyph_p;
        case 'Q':
            return glyph_q;
        case 'R':
            return glyph_r;
        case 'S':
            return glyph_s;
        case 'T':
            return glyph_t;
        case 'U':
            return glyph_u;
        case 'V':
            return glyph_v;
        case 'W':
            return glyph_w;
        case 'X':
            return glyph_x;
        case 'Y':
            return glyph_y;
        case 'Z':
            return glyph_z;
        default:
            return glyph_space;
    }
}

td_void yuv420sp_draw_char(td_u8 *buffer, td_u32 width, td_u32 height, td_u32 stride_y, td_u32 stride_uv,
    td_bool is_yvu_semiplanar_420, td_s32 origin_x, td_s32 origin_y, char c, td_s32 scale, td_u8 y_value,
    td_u8 u_value, td_u8 v_value)
{
    const td_u8 *glyph = ai_acl_adapter_font5x7(c);
    td_s32 row;
    td_s32 col;
    td_s32 dy;
    td_s32 dx;

    if (scale <= 0) {
        scale = 1;
    }

    for (row = 0; row < 7; ++row) {
        for (col = 0; col < 5; ++col) {
            if ((glyph[row] & (1U << (4 - col))) == 0) {
                continue;
            }
            for (dy = 0; dy < scale; ++dy) {
                for (dx = 0; dx < scale; ++dx) {
                    yuv420sp_put_pixel(buffer, width, height, stride_y, stride_uv, is_yvu_semiplanar_420,
                        origin_x + col * scale + dx, origin_y + row * scale + dy, y_value, u_value, v_value);
                }
            }
        }
    }
}

std::string ai_acl_adapter_make_label(const std::string &class_name, float confidence)
{
    std::string label;
    int confidence_percent;

    label.reserve(class_name.size() + 8);
    for (char c : class_name) {
        if (std::islower(static_cast<unsigned char>(c)) != 0) {
            label.push_back(static_cast<char>(std::toupper(static_cast<unsigned char>(c))));
        } else if (std::isupper(static_cast<unsigned char>(c)) != 0 ||
            std::isdigit(static_cast<unsigned char>(c)) != 0 || c == ' ' || c == '-' || c == '.') {
            label.push_back(c);
        } else {
            label.push_back(' ');
        }
        if (label.size() >= 24) {
            break;
        }
    }

    confidence = std::max(0.0f, std::min(1.0f, confidence));
    confidence_percent = static_cast<int>(confidence * 100.0f + 0.5f);
    label += " ";
    label += std::to_string(confidence_percent);
    label += "%";
    return label;
}

td_void yuv420sp_draw_text(td_u8 *buffer, td_u32 width, td_u32 height, td_u32 stride_y, td_u32 stride_uv,
    td_bool is_yvu_semiplanar_420, td_s32 origin_x, td_s32 origin_y, const std::string &text, td_s32 scale,
    td_u8 y_value, td_u8 u_value, td_u8 v_value)
{
    td_s32 cursor_x = origin_x;

    for (char c : text) {
        yuv420sp_draw_char(buffer, width, height, stride_y, stride_uv, is_yvu_semiplanar_420,
            cursor_x, origin_y, c, scale, y_value, u_value, v_value);
        cursor_x += 6 * scale;
    }
}

td_void yvu420sp_to_rgb_pixel(const ai_acl_adapter_frame *frame, td_u32 src_x, td_u32 src_y,
    td_u8 *r, td_u8 *g, td_u8 *b)
{
    const td_u8 *y_plane = frame->input_yuv420p;
    const td_u8 *uv_plane = frame->input_yuv420p + frame->stride_y * frame->height;
    td_u32 uv_index = (src_y / 2) * frame->stride_uv + (src_x / 2) * 2;
    td_s32 y = (td_s32)y_plane[src_y * frame->stride_y + src_x];
    td_s32 u;
    td_s32 v;
    td_s32 c;
    td_s32 d;
    td_s32 e;
    td_s32 r_tmp;
    td_s32 g_tmp;
    td_s32 b_tmp;

    if (frame->input_is_yvu_semiplanar_420 == TD_TRUE) {
        v = (td_s32)uv_plane[uv_index + 0];
        u = (td_s32)uv_plane[uv_index + 1];
    } else {
        u = (td_s32)uv_plane[uv_index + 0];
        v = (td_s32)uv_plane[uv_index + 1];
    }

    c = y - 16;
    d = u - 128;
    e = v - 128;
    if (c < 0) {
        c = 0;
    }

    r_tmp = (298 * c + 409 * e + 128) >> 8;
    g_tmp = (298 * c - 100 * d - 208 * e + 128) >> 8;
    b_tmp = (298 * c + 516 * d + 128) >> 8;

    *r = clamp_u8(r_tmp);
    *g = clamp_u8(g_tmp);
    *b = clamp_u8(b_tmp);
}

td_void ai_acl_adapter_load_classes(AiAclAdapterContext *ctx)
{
    std::ifstream input;
    std::string line;

    if (ctx == nullptr) {
        return;
    }

    ctx->classes.clear();
    if (ctx->classes_path.empty()) {
        return;
    }

    input.open(ctx->classes_path.c_str());
    if (!input.is_open()) {
        return;
    }

    while (std::getline(input, line)) {
        if (!line.empty() && line.back() == '\r') {
            line.pop_back();
        }
        if (!line.empty()) {
            ctx->classes.push_back(line);
        }
    }
}

const std::string &ai_acl_adapter_get_class_name(AiAclAdapterContext *ctx, td_s32 class_id)
{
    static const std::string k_unknown("CLASS");

    if (ctx == nullptr || class_id < 0 || static_cast<size_t>(class_id) >= ctx->classes.size()) {
        return k_unknown;
    }
    return ctx->classes[static_cast<size_t>(class_id)];
}

td_void ai_acl_adapter_map_model_to_frame(const AiPreprocessMeta *meta, td_u32 frame_w, td_u32 frame_h,
    float model_x, float model_y, float *frame_x, float *frame_y)
{
    if (meta == nullptr || frame_x == nullptr || frame_y == nullptr) {
        return;
    }

    /* Remove letterbox padding before scaling model coordinates back to the source frame. */
    if (meta->use_letterbox == TD_TRUE && meta->letterbox_scale > 0.0f) {
        *frame_x = (model_x - static_cast<float>(meta->pad_x)) / meta->letterbox_scale;
        *frame_y = (model_y - static_cast<float>(meta->pad_y)) / meta->letterbox_scale;
    } else {
        *frame_x = model_x * static_cast<float>(frame_w) / static_cast<float>(meta->model_w);
        *frame_y = model_y * static_cast<float>(frame_h) / static_cast<float>(meta->model_h);
    }
}

td_bool ai_acl_adapter_output_value(const td_void *buffer, size_t buffer_size, svp_acl_data_type type,
    const svp_acl_mdl_io_dims *dims, size_t stride_bytes, size_t logical_index, float *value)
{
    size_t elem_size = acl_data_type_size_bytes(type);
    size_t byte_offset;

    if (buffer == nullptr || dims == nullptr || value == nullptr || elem_size == 0 || dims->dim_count == 0) {
        return TD_FALSE;
    }

    if (stride_bytes > 0 && dims->dims[dims->dim_count - 1] > 0) {
        size_t last_dim = (size_t)dims->dims[dims->dim_count - 1];
        size_t prefix = logical_index / last_dim;
        size_t column = logical_index % last_dim;
        byte_offset = prefix * stride_bytes + column * elem_size;
    } else {
        byte_offset = logical_index * elem_size;
    }

    if (byte_offset + elem_size > buffer_size) {
        return TD_FALSE;
    }

    switch (type) {
        case SVP_ACL_FLOAT:
            std::memcpy(value, static_cast<const td_u8 *>(buffer) + byte_offset, sizeof(float));
            return TD_TRUE;
        case SVP_ACL_FLOAT16: {
            uint16_t raw;
            std::memcpy(&raw, static_cast<const td_u8 *>(buffer) + byte_offset, sizeof(raw));
            *value = fp16_to_float(raw);
            return TD_TRUE;
        }
        case SVP_ACL_UINT8:
            *value = (float)*(static_cast<const td_u8 *>(buffer) + byte_offset);
            return TD_TRUE;
        case SVP_ACL_INT8:
            *value = (float)*(reinterpret_cast<const int8_t *>(static_cast<const td_u8 *>(buffer) + byte_offset));
            return TD_TRUE;
        case SVP_ACL_INT32: {
            int32_t raw;
            std::memcpy(&raw, static_cast<const td_u8 *>(buffer) + byte_offset, sizeof(raw));
            *value = (float)raw;
            return TD_TRUE;
        }
        default:
            return TD_FALSE;
    }
}

size_t ai_acl_adapter_dims_count(const svp_acl_mdl_io_dims *dims)
{
    size_t total = 1;

    if (dims == nullptr || dims->dim_count == 0) {
        return 0;
    }

    for (size_t i = 0; i < dims->dim_count; ++i) {
        if (dims->dims[i] <= 0) {
            return 0;
        }
        total *= (size_t)dims->dims[i];
    }
    return total;
}

td_bool ai_acl_adapter_calc_strided_alloc_size(const svp_acl_mdl_io_dims *dims, svp_acl_data_type type,
    size_t stride_bytes, size_t *alloc_size)
{
    size_t elem_size = acl_data_type_size_bytes(type);
    size_t loop_times = 1;

    if (dims == nullptr || alloc_size == nullptr || elem_size == 0 ||
        dims->dim_count == 0 || stride_bytes == 0) {
        return TD_FALSE;
    }

    for (size_t i = 0; i + 1 < dims->dim_count; ++i) {
        if (dims->dims[i] <= 0) {
            return TD_FALSE;
        }
        loop_times *= static_cast<size_t>(dims->dims[i]);
    }

    if (dims->dims[dims->dim_count - 1] <= 0 ||
        stride_bytes < static_cast<size_t>(dims->dims[dims->dim_count - 1]) * elem_size) {
        return TD_FALSE;
    }

    *alloc_size = loop_times * stride_bytes;
    return TD_TRUE;
}

td_bool ai_acl_adapter_get_input_alloc_size(AiAclAdapterContext *ctx, size_t index, size_t *alloc_size)
{
    svp_acl_mdl_io_dims dims = {0};
    svp_acl_data_type type;
    size_t stride;
    size_t model_size;

    if (ctx == nullptr || ctx->model_desc == nullptr || alloc_size == nullptr) {
        return TD_FALSE;
    }

    model_size = svp_acl_mdl_get_input_size_by_index(ctx->model_desc, index);
    stride = svp_acl_mdl_get_input_default_stride(ctx->model_desc, index);
    type = svp_acl_mdl_get_input_data_type(ctx->model_desc, index);
    if (svp_acl_mdl_get_input_dims(ctx->model_desc, index, &dims) != SVP_ACL_SUCCESS ||
        ai_acl_adapter_calc_strided_alloc_size(&dims, type, stride, alloc_size) != TD_TRUE) {
        *alloc_size = model_size;
        return model_size > 0 ? TD_TRUE : TD_FALSE;
    }

    if (*alloc_size < model_size) {
        *alloc_size = model_size;
    }
    return *alloc_size > 0 ? TD_TRUE : TD_FALSE;
}

td_bool ai_acl_adapter_is_verified_yolov8_rpn(AiAclAdapterContext *ctx)
{
    svp_acl_mdl_io_dims input0 = {0};
    svp_acl_mdl_io_dims input1 = {0};
    svp_acl_mdl_io_dims output0 = {0};
    svp_acl_mdl_io_dims output1 = {0};

    if (ctx == nullptr || ctx->model_desc == nullptr || ctx->input_count < 4 || ctx->output_count < 2) {
        return TD_FALSE;
    }

    if (svp_acl_mdl_get_input_dims(ctx->model_desc, 0, &input0) != SVP_ACL_SUCCESS ||
        svp_acl_mdl_get_input_dims(ctx->model_desc, 1, &input1) != SVP_ACL_SUCCESS ||
        svp_acl_mdl_get_output_dims(ctx->model_desc, 0, &output0) != SVP_ACL_SUCCESS ||
        svp_acl_mdl_get_output_dims(ctx->model_desc, 1, &output1) != SVP_ACL_SUCCESS) {
        return TD_FALSE;
    }

    if (svp_acl_mdl_get_input_data_type(ctx->model_desc, 0) != SVP_ACL_UINT8 ||
        svp_acl_mdl_get_input_format(ctx->model_desc, 0) != SVP_ACL_FORMAT_NCHW) {
        return TD_FALSE;
    }

    return (input0.dim_count == 4 && input0.dims[0] == 1 && input0.dims[1] == 3 &&
        input0.dims[2] == 640 && input0.dims[3] == 640 &&
        input1.dim_count == 2 && input1.dims[0] == 1 && input1.dims[1] == 4 &&
        output0.dim_count == 4 && output0.dims[0] == 1 && output0.dims[1] == 1 &&
        output0.dims[2] == 1 && output0.dims[3] == 1 &&
        output1.dim_count >= 2 && output1.dims[output1.dim_count - 2] == 6 &&
        output1.dims[output1.dim_count - 1] > 0) ? TD_TRUE : TD_FALSE;
}

td_bool ai_acl_adapter_is_yolo11_9out(AiAclAdapterContext *ctx)
{
    svp_acl_mdl_io_dims input0 = {0};
    const td_s32 grids[3] = {20, 40, 80};

    if (ctx == nullptr || ctx->model_desc == nullptr || ctx->input_count < 3 || ctx->output_count != 9) {
        return TD_FALSE;
    }

    if (svp_acl_mdl_get_input_dims(ctx->model_desc, 0, &input0) != SVP_ACL_SUCCESS ||
        input0.dim_count != 4 || input0.dims[0] != 1 || input0.dims[1] != 3 ||
        input0.dims[2] != AI_YOLO11_MODEL_SIZE || input0.dims[3] != AI_YOLO11_MODEL_SIZE ||
        svp_acl_mdl_get_input_data_type(ctx->model_desc, 0) != SVP_ACL_UINT8 ||
        svp_acl_mdl_get_input_format(ctx->model_desc, 0) != SVP_ACL_FORMAT_NCHW) {
        return TD_FALSE;
    }

    for (size_t branch = 0; branch < 3; ++branch) {
        svp_acl_mdl_io_dims box_dims = {0};
        svp_acl_mdl_io_dims score_dims = {0};
        svp_acl_mdl_io_dims sum_dims = {0};
        size_t box_idx = branch * 3;
        size_t score_idx = branch * 3 + 1;
        size_t sum_idx = branch * 3 + 2;
        td_s32 grid = grids[branch];

        if (svp_acl_mdl_get_output_dims(ctx->model_desc, box_idx, &box_dims) != SVP_ACL_SUCCESS ||
            svp_acl_mdl_get_output_dims(ctx->model_desc, score_idx, &score_dims) != SVP_ACL_SUCCESS ||
            svp_acl_mdl_get_output_dims(ctx->model_desc, sum_idx, &sum_dims) != SVP_ACL_SUCCESS ||
            box_dims.dim_count != 4 || score_dims.dim_count != 4 || sum_dims.dim_count != 4) {
            return TD_FALSE;
        }

        if (box_dims.dims[0] != 1 || box_dims.dims[1] != AI_YOLO11_DFL_LEN * 4 ||
            box_dims.dims[2] != grid || box_dims.dims[3] != grid ||
            score_dims.dims[0] != 1 || score_dims.dims[1] != AI_YOLO11_CLASS_COUNT ||
            score_dims.dims[2] != grid || score_dims.dims[3] != grid ||
            sum_dims.dims[0] != 1 || sum_dims.dims[1] != 1 ||
            sum_dims.dims[2] != grid || sum_dims.dims[3] != grid) {
            return TD_FALSE;
        }
    }

    return TD_TRUE;
}

float ai_acl_adapter_score_value(float value)
{
    if (!std::isfinite(value)) {
        return 0.0f;
    }
    if (value < 0.0f || value > 1.0f) {
        return 1.0f / (1.0f + std::exp(-value));
    }
    return value;
}

td_bool ai_acl_adapter_decode_xywh_box(const AiPreprocessMeta *meta, td_u32 frame_w, td_u32 frame_h,
    float cx, float cy, float w, float h, float *x1, float *y1, float *x2, float *y2)
{
    float left;
    float top;
    float right;
    float bottom;
    td_bool normalized;

    if (meta == nullptr || x1 == nullptr || y1 == nullptr || x2 == nullptr || y2 == nullptr ||
        !std::isfinite(cx) || !std::isfinite(cy) || !std::isfinite(w) || !std::isfinite(h)) {
        return TD_FALSE;
    }

    normalized = (std::fabs(cx) <= 2.0f && std::fabs(cy) <= 2.0f &&
        std::fabs(w) <= 2.0f && std::fabs(h) <= 2.0f) ? TD_TRUE : TD_FALSE;
    if (normalized == TD_TRUE) {
        cx *= static_cast<float>(meta->model_w);
        w *= static_cast<float>(meta->model_w);
        cy *= static_cast<float>(meta->model_h);
        h *= static_cast<float>(meta->model_h);
    }

    left = cx - w * 0.5f;
    top = cy - h * 0.5f;
    right = cx + w * 0.5f;
    bottom = cy + h * 0.5f;

    ai_acl_adapter_map_model_to_frame(meta, frame_w, frame_h, left, top, &left, &top);
    ai_acl_adapter_map_model_to_frame(meta, frame_w, frame_h, right, bottom, &right, &bottom);

    left = std::max(0.0f, std::min(left, static_cast<float>(frame_w - 1)));
    top = std::max(0.0f, std::min(top, static_cast<float>(frame_h - 1)));
    right = std::max(0.0f, std::min(right, static_cast<float>(frame_w - 1)));
    bottom = std::max(0.0f, std::min(bottom, static_cast<float>(frame_h - 1)));

    if (right - left < 2.0f || bottom - top < 2.0f) {
        return TD_FALSE;
    }

    *x1 = left;
    *y1 = top;
    *x2 = right;
    *y2 = bottom;
    return TD_TRUE;
}

td_bool ai_acl_adapter_decode_box(const AiPreprocessMeta *meta, td_u32 frame_w, td_u32 frame_h,
    float a, float b, float c, float d, float *x1, float *y1, float *x2, float *y2)
{
    float left;
    float top;
    float right;
    float bottom;
    float max_coord;
    td_bool looks_xyxy;
    td_bool normalized;

    if (meta == nullptr || x1 == nullptr || y1 == nullptr || x2 == nullptr || y2 == nullptr) {
        return TD_FALSE;
    }

    looks_xyxy = (c > a && d > b) ? TD_TRUE : TD_FALSE;
    normalized = (std::fabs(a) <= 2.0f && std::fabs(b) <= 2.0f &&
        std::fabs(c) <= 2.0f && std::fabs(d) <= 2.0f) ? TD_TRUE : TD_FALSE;

    if (looks_xyxy == TD_TRUE) {
        left = a;
        top = b;
        right = c;
        bottom = d;
    } else {
        left = a - c * 0.5f;
        top = b - d * 0.5f;
        right = a + c * 0.5f;
        bottom = b + d * 0.5f;
    }

    if (normalized == TD_TRUE) {
        left *= static_cast<float>(meta->model_w);
        right *= static_cast<float>(meta->model_w);
        top *= static_cast<float>(meta->model_h);
        bottom *= static_cast<float>(meta->model_h);
    }

    max_coord = std::max(std::max(std::fabs(left), std::fabs(top)),
        std::max(std::fabs(right), std::fabs(bottom)));

    if (max_coord <= static_cast<float>(std::max(meta->model_w, meta->model_h)) * 1.25f) {
        ai_acl_adapter_map_model_to_frame(meta, frame_w, frame_h, left, top, &left, &top);
        ai_acl_adapter_map_model_to_frame(meta, frame_w, frame_h, right, bottom, &right, &bottom);
    }

    left = std::max(0.0f, std::min(left, static_cast<float>(frame_w - 1)));
    top = std::max(0.0f, std::min(top, static_cast<float>(frame_h - 1)));
    right = std::max(0.0f, std::min(right, static_cast<float>(frame_w - 1)));
    bottom = std::max(0.0f, std::min(bottom, static_cast<float>(frame_h - 1)));

    if (right - left < 2.0f || bottom - top < 2.0f) {
        return TD_FALSE;
    }

    *x1 = left;
    *y1 = top;
    *x2 = right;
    *y2 = bottom;
    return TD_TRUE;
}

float ai_acl_adapter_iou(const AiDetection &lhs, const AiDetection &rhs)
{
    float inter_left = std::max(lhs.x1, rhs.x1);
    float inter_top = std::max(lhs.y1, rhs.y1);
    float inter_right = std::min(lhs.x2, rhs.x2);
    float inter_bottom = std::min(lhs.y2, rhs.y2);
    float inter_w = std::max(0.0f, inter_right - inter_left);
    float inter_h = std::max(0.0f, inter_bottom - inter_top);
    float inter_area = inter_w * inter_h;
    float lhs_area = std::max(0.0f, lhs.x2 - lhs.x1) * std::max(0.0f, lhs.y2 - lhs.y1);
    float rhs_area = std::max(0.0f, rhs.x2 - rhs.x1) * std::max(0.0f, rhs.y2 - rhs.y1);
    float union_area = lhs_area + rhs_area - inter_area;

    if (union_area <= 0.0f) {
        return 0.0f;
    }
    return inter_area / union_area;
}

td_void ai_acl_adapter_apply_nms(std::vector<AiDetection> *detections, float iou_threshold)
{
    std::vector<AiDetection> filtered;

    if (detections == nullptr) {
        return;
    }

    std::sort(detections->begin(), detections->end(),
        [](const AiDetection &lhs, const AiDetection &rhs) { return lhs.confidence > rhs.confidence; });

    for (const AiDetection &candidate : *detections) {
        td_bool keep = TD_TRUE;
        for (const AiDetection &kept : filtered) {
            if (candidate.class_id == kept.class_id && ai_acl_adapter_iou(candidate, kept) > iou_threshold) {
                keep = TD_FALSE;
                break;
            }
        }
        if (keep == TD_TRUE) {
            filtered.push_back(candidate);
        }
        if (filtered.size() >= 32) {
            break;
        }
    }

    detections->swap(filtered);
}

td_bool ai_acl_adapter_get_yolo11_output_tensor(AiAclAdapterContext *ctx, svp_acl_mdl_dataset *dataset,
    size_t index, AiYolo11OutputTensor *tensor)
{
    svp_acl_data_buffer *data_buffer;

    if (ctx == nullptr || dataset == nullptr || tensor == nullptr || ctx->model_desc == nullptr) {
        return TD_FALSE;
    }

    std::memset(tensor, 0, sizeof(*tensor));
    if (svp_acl_mdl_get_output_dims(ctx->model_desc, index, &tensor->dims) != SVP_ACL_SUCCESS ||
        tensor->dims.dim_count != 4 || tensor->dims.dims[1] <= 0 ||
        tensor->dims.dims[2] <= 0 || tensor->dims.dims[3] <= 0) {
        return TD_FALSE;
    }

    data_buffer = svp_acl_mdl_get_dataset_buffer(dataset, index);
    if (data_buffer == nullptr) {
        return TD_FALSE;
    }

    tensor->buffer = svp_acl_get_data_buffer_addr(data_buffer);
    tensor->buffer_size = svp_acl_get_data_buffer_size(data_buffer);
    tensor->type = svp_acl_mdl_get_output_data_type(ctx->model_desc, index);
    tensor->stride_bytes = svp_acl_mdl_get_output_default_stride(ctx->model_desc, index);
    tensor->channels = static_cast<td_s32>(tensor->dims.dims[1]);
    tensor->grid_h = static_cast<td_s32>(tensor->dims.dims[2]);
    tensor->grid_w = static_cast<td_s32>(tensor->dims.dims[3]);

    return (tensor->buffer != nullptr && tensor->buffer_size > 0 && tensor->stride_bytes > 0) ?
        TD_TRUE : TD_FALSE;
}

float ai_acl_adapter_yolo11_tensor_value(const AiYolo11OutputTensor *tensor, td_s32 channel, td_s32 y, td_s32 x)
{
    size_t logical_index;
    float value = 0.0f;

    if (tensor == nullptr || tensor->buffer == nullptr || channel < 0 || y < 0 || x < 0 ||
        channel >= tensor->channels || y >= tensor->grid_h || x >= tensor->grid_w) {
        return 0.0f;
    }

    logical_index = (static_cast<size_t>(channel) * tensor->grid_h + static_cast<size_t>(y)) *
        tensor->grid_w + static_cast<size_t>(x);
    (td_void)ai_acl_adapter_output_value(tensor->buffer, tensor->buffer_size, tensor->type,
        &tensor->dims, tensor->stride_bytes, logical_index, &value);
    return value;
}

float ai_acl_adapter_yolo11_dfl_expectation(const AiYolo11OutputTensor *box_tensor,
    td_s32 side, td_s32 y, td_s32 x)
{
    float logits[AI_YOLO11_DFL_LEN];
    float max_logit = -INFINITY;
    float exp_sum = 0.0f;
    float acc = 0.0f;

    if (box_tensor == nullptr || side < 0 || side >= 4) {
        return 0.0f;
    }

    for (td_s32 i = 0; i < AI_YOLO11_DFL_LEN; ++i) {
        logits[i] = ai_acl_adapter_yolo11_tensor_value(box_tensor,
            side * AI_YOLO11_DFL_LEN + i, y, x);
        max_logit = std::max(max_logit, logits[i]);
    }

    for (td_s32 i = 0; i < AI_YOLO11_DFL_LEN; ++i) {
        logits[i] = std::exp(logits[i] - max_logit);
        exp_sum += logits[i];
    }
    if (exp_sum <= 0.0f || !std::isfinite(exp_sum)) {
        return 0.0f;
    }

    for (td_s32 i = 0; i < AI_YOLO11_DFL_LEN; ++i) {
        acc += logits[i] / exp_sum * static_cast<float>(i);
    }
    return acc;
}

td_void ai_acl_adapter_decode_yolo11_branch(AiAclAdapterContext *ctx, const AiPreprocessMeta *meta,
    td_u32 frame_w, td_u32 frame_h, const AiYolo11OutputTensor *box_tensor,
    const AiYolo11OutputTensor *score_tensor, const AiYolo11OutputTensor *score_sum_tensor,
    td_bool use_score_sum, std::vector<AiDetection> *detections)
{
    td_s32 stride;

    if (ctx == nullptr || meta == nullptr || box_tensor == nullptr || score_tensor == nullptr ||
        detections == nullptr || box_tensor->grid_h <= 0 || box_tensor->grid_w <= 0 ||
        box_tensor->channels != AI_YOLO11_DFL_LEN * 4 ||
        score_tensor->channels < AI_YOLO11_CLASS_COUNT) {
        return;
    }

    stride = AI_YOLO11_MODEL_SIZE / box_tensor->grid_h;
    if (stride <= 0) {
        stride = 1;
    }

    for (td_s32 y = 0; y < box_tensor->grid_h; ++y) {
        for (td_s32 x = 0; x < box_tensor->grid_w; ++x) {
            float best_score = 0.0f;
            td_s32 best_class = -1;

            if (use_score_sum == TD_TRUE && score_sum_tensor != nullptr &&
                ai_acl_adapter_yolo11_tensor_value(score_sum_tensor, 0, y, x) < AI_DET_SCORE_THRESHOLD) {
                continue;
            }

            for (td_s32 c = 0; c < AI_YOLO11_CLASS_COUNT; ++c) {
                float score = ai_acl_adapter_yolo11_tensor_value(score_tensor, c, y, x);
                if (score > best_score) {
                    best_score = score;
                    best_class = c;
                }
            }

            if (best_class < 0 || best_score < AI_DET_SCORE_THRESHOLD) {
                continue;
            }

            float left_d = ai_acl_adapter_yolo11_dfl_expectation(box_tensor, 0, y, x);
            float top_d = ai_acl_adapter_yolo11_dfl_expectation(box_tensor, 1, y, x);
            float right_d = ai_acl_adapter_yolo11_dfl_expectation(box_tensor, 2, y, x);
            float bottom_d = ai_acl_adapter_yolo11_dfl_expectation(box_tensor, 3, y, x);
            float cx = static_cast<float>(x) + 0.5f;
            float cy = static_cast<float>(y) + 0.5f;
            float model_x1 = (cx - left_d) * static_cast<float>(stride);
            float model_y1 = (cy - top_d) * static_cast<float>(stride);
            float model_x2 = (cx + right_d) * static_cast<float>(stride);
            float model_y2 = (cy + bottom_d) * static_cast<float>(stride);
            AiDetection det = {0};

            ai_acl_adapter_map_model_to_frame(meta, frame_w, frame_h, model_x1, model_y1, &det.x1, &det.y1);
            ai_acl_adapter_map_model_to_frame(meta, frame_w, frame_h, model_x2, model_y2, &det.x2, &det.y2);
            det.x1 = std::max(0.0f, std::min(det.x1, static_cast<float>(frame_w - 1)));
            det.y1 = std::max(0.0f, std::min(det.y1, static_cast<float>(frame_h - 1)));
            det.x2 = std::max(0.0f, std::min(det.x2, static_cast<float>(frame_w - 1)));
            det.y2 = std::max(0.0f, std::min(det.y2, static_cast<float>(frame_h - 1)));
            if (det.x2 - det.x1 < 2.0f || det.y2 - det.y1 < 2.0f) {
                continue;
            }

            det.class_id = best_class;
            det.confidence = best_score;
            if (det.class_id >= 0 && static_cast<size_t>(det.class_id) < ctx->classes.size()) {
                det.class_name = ai_acl_adapter_get_class_name(ctx, det.class_id);
            } else {
                det.class_name = "CLASS " + std::to_string(det.class_id);
            }
            detections->push_back(det);
        }
    }
}

td_s32 ai_acl_adapter_parse_yolo11_9out(AiAclAdapterContext *ctx, svp_acl_mdl_dataset *dataset,
    const AiPreprocessMeta *meta, td_u32 frame_w, td_u32 frame_h, std::vector<AiDetection> *detections)
{
    std::vector<AiDetection> parsed;
    static td_s32 yolo11_log_count = 0;

    if (ctx == nullptr || dataset == nullptr || meta == nullptr || detections == nullptr ||
        ai_acl_adapter_is_yolo11_9out(ctx) != TD_TRUE ||
        svp_acl_mdl_get_dataset_num_buffers(dataset) < 9) {
        return TD_FAILURE;
    }

    /*
     * Each scale provides DFL box, 80-class score, and score-sum tensors.
     * First prune with score-sum; retry without it when a converted model has
     * an unusable auxiliary output.
     */
    for (td_s32 pass = 0; pass < 2 && parsed.empty(); ++pass) {
        td_bool use_score_sum = (pass == 0) ? TD_TRUE : TD_FALSE;
        for (size_t branch = 0; branch < 3; ++branch) {
            AiYolo11OutputTensor box_tensor;
            AiYolo11OutputTensor score_tensor;
            AiYolo11OutputTensor score_sum_tensor;
            size_t box_idx = branch * 3;
            size_t score_idx = branch * 3 + 1;
            size_t sum_idx = branch * 3 + 2;

            if (ai_acl_adapter_get_yolo11_output_tensor(ctx, dataset, box_idx, &box_tensor) != TD_TRUE ||
                ai_acl_adapter_get_yolo11_output_tensor(ctx, dataset, score_idx, &score_tensor) != TD_TRUE ||
                ai_acl_adapter_get_yolo11_output_tensor(ctx, dataset, sum_idx, &score_sum_tensor) != TD_TRUE) {
                continue;
            }

            if (yolo11_log_count < 6) {
                std::printf("[AI] yolo11 branch=%zu grid=%dx%d stride=%d use_score_sum=%d\n",
                    branch, box_tensor.grid_w, box_tensor.grid_h,
                    box_tensor.grid_h > 0 ? AI_YOLO11_MODEL_SIZE / box_tensor.grid_h : 0,
                    use_score_sum == TD_TRUE ? 1 : 0);
            }

            ai_acl_adapter_decode_yolo11_branch(ctx, meta, frame_w, frame_h, &box_tensor,
                &score_tensor, &score_sum_tensor, use_score_sum, &parsed);
        }

        if (yolo11_log_count < 6) {
            std::printf("[AI] yolo11 raw detections=%zu use_score_sum=%d\n",
                parsed.size(), use_score_sum == TD_TRUE ? 1 : 0);
        }
    }

    ai_acl_adapter_apply_nms(&parsed, AI_DET_NMS_THRESHOLD);
    if (yolo11_log_count < 6) {
        std::printf("[AI] yolo11 detections after nms=%zu threshold=%.2f nms=%.2f\n",
            parsed.size(), AI_DET_SCORE_THRESHOLD, AI_DET_NMS_THRESHOLD);
        yolo11_log_count++;
    }
    detections->swap(parsed);
    return TD_SUCCESS;
}

td_s32 ai_acl_adapter_parse_yolo_raw(AiAclAdapterContext *ctx, const td_void *buffer, size_t buffer_size,
    svp_acl_data_type type, const svp_acl_mdl_io_dims *dims, size_t stride_bytes, const AiPreprocessMeta *meta,
    td_u32 frame_w, td_u32 frame_h, std::vector<AiDetection> *detections)
{
    size_t last_dim;
    size_t second_last_dim;
    size_t channels = 0;
    size_t boxes = 0;
    td_bool channel_first = TD_FALSE;
    td_bool has_object_score = TD_FALSE;
    size_t score_start_channel;
    size_t class_count;
    std::vector<AiDetection> parsed;

    if (ctx == nullptr || buffer == nullptr || dims == nullptr || meta == nullptr || detections == nullptr ||
        dims->dim_count < 2) {
        return TD_FAILURE;
    }

    last_dim = (size_t)dims->dims[dims->dim_count - 1];
    second_last_dim = (size_t)dims->dims[dims->dim_count - 2];
    if (second_last_dim >= 5 && second_last_dim <= 256 && last_dim > 0) {
        channels = second_last_dim;
        boxes = last_dim;
        channel_first = TD_TRUE;
    } else if (last_dim >= 5 && last_dim <= 256 && second_last_dim > 0) {
        channels = last_dim;
        boxes = second_last_dim;
        channel_first = TD_FALSE;
    } else {
        return TD_FAILURE;
    }

    if (boxes == 0 || boxes > AI_DET_MAX_RAW_BOXES || channels < 5) {
        return TD_FAILURE;
    }

    if (!ctx->classes.empty() && channels == ctx->classes.size() + 5) {
        has_object_score = TD_TRUE;
    } else if (channels == 85) {
        has_object_score = TD_TRUE;
    }

    score_start_channel = has_object_score == TD_TRUE ? 5 : 4;
    class_count = (channels > score_start_channel) ? (channels - score_start_channel) : 0;
    if (channels == 5) {
        class_count = 1;
    } else if (class_count == 0) {
        return TD_FAILURE;
    }

    auto read_value = [&](size_t box_index, size_t channel_index, float *value) -> td_bool {
        size_t logical_index;

        if (channel_index >= channels || box_index >= boxes) {
            return TD_FALSE;
        }

        logical_index = (channel_first == TD_TRUE) ?
            (channel_index * boxes + box_index) : (box_index * channels + channel_index);
        return ai_acl_adapter_output_value(buffer, buffer_size, type, dims, stride_bytes, logical_index, value);
    };

    for (size_t i = 0; i < boxes; ++i) {
        float cx;
        float cy;
        float w;
        float h;
        float obj_score = 1.0f;
        float best_score = 0.0f;
        size_t best_class = 0;
        AiDetection det = {0};

        if (read_value(i, 0, &cx) != TD_TRUE || read_value(i, 1, &cy) != TD_TRUE ||
            read_value(i, 2, &w) != TD_TRUE || read_value(i, 3, &h) != TD_TRUE) {
            continue;
        }

        if (has_object_score == TD_TRUE) {
            if (read_value(i, 4, &obj_score) != TD_TRUE) {
                continue;
            }
            obj_score = ai_acl_adapter_score_value(obj_score);
        }

        if (channels == 5) {
            if (read_value(i, 4, &best_score) != TD_TRUE) {
                continue;
            }
            best_score = ai_acl_adapter_score_value(best_score);
            best_class = 0;
        } else {
            for (size_t cls = 0; cls < class_count; ++cls) {
                float score;
                if (read_value(i, score_start_channel + cls, &score) != TD_TRUE) {
                    continue;
                }
                score = ai_acl_adapter_score_value(score);
                if (score > best_score) {
                    best_score = score;
                    best_class = cls;
                }
            }
            best_score *= obj_score;
        }

        if (!std::isfinite(best_score) || best_score < AI_DET_SCORE_THRESHOLD) {
            continue;
        }

        if (ai_acl_adapter_decode_xywh_box(meta, frame_w, frame_h, cx, cy, w, h,
            &det.x1, &det.y1, &det.x2, &det.y2) != TD_TRUE) {
            continue;
        }

        det.class_id = static_cast<td_s32>(best_class);
        det.confidence = best_score;
        if (det.class_id >= 0 && static_cast<size_t>(det.class_id) < ctx->classes.size()) {
            det.class_name = ai_acl_adapter_get_class_name(ctx, det.class_id);
        } else {
            det.class_name = "CLASS " + std::to_string(det.class_id);
        }
        parsed.push_back(det);
    }

    ai_acl_adapter_apply_nms(&parsed, AI_DET_NMS_THRESHOLD);
    detections->swap(parsed);
    return TD_SUCCESS;
}

td_void ai_acl_adapter_draw_detections(ai_acl_adapter_frame *frame, const std::vector<AiDetection> &detections)
{
    static const td_u8 k_palette[][3] = {
        {0, 255, 0},
        {255, 128, 0},
        {0, 180, 255},
        {255, 0, 128},
        {255, 255, 0},
    };
    td_u8 *buffer;

    if (frame == nullptr || frame->output_yuv420p == nullptr) {
        return;
    }

    buffer = frame->output_yuv420p;
    if (frame->output_yuv420p != frame->input_yuv420p) {
        td_u32 frame_size = frame->stride_y * frame->height + frame->stride_uv * frame->height / 2;
        std::memcpy(frame->output_yuv420p, frame->input_yuv420p, frame_size);
    }

    for (size_t i = 0; i < detections.size(); ++i) {
        const AiDetection &det = detections[i];
        const td_u8 *rgb = k_palette[det.class_id >= 0 ? (det.class_id % 5) : 0];
        td_u8 box_y;
        td_u8 box_u;
        td_u8 box_v;
        td_u8 text_y = 235;
        td_u8 text_u = 128;
        td_u8 text_v = 128;
        td_s32 left = static_cast<td_s32>(det.x1);
        td_s32 top = static_cast<td_s32>(det.y1);
        td_s32 right = static_cast<td_s32>(det.x2);
        td_s32 bottom = static_cast<td_s32>(det.y2);

        rgb_to_yuv(rgb[0], rgb[1], rgb[2], &box_y, &box_u, &box_v);
        yuv420sp_draw_rect(buffer, frame->width, frame->height, frame->stride_y, frame->stride_uv,
            frame->input_is_yvu_semiplanar_420, left, top, right, bottom, 2, box_y, box_u, box_v);

        if (AI_DRAW_LABEL_TEXT == TD_TRUE) {
            td_s32 scale = 2;
            std::string label = ai_acl_adapter_make_label(det.class_name, det.confidence);
            td_s32 label_width = static_cast<td_s32>(label.size()) * 6 * scale + 4;
            td_s32 label_height = 7 * scale + 4;
            td_s32 label_x = left;
            td_s32 label_y = (top >= label_height + 2) ? (top - label_height - 2) : (top + 2);

            if (label_x + label_width > static_cast<td_s32>(frame->width)) {
                label_x = static_cast<td_s32>(frame->width) - label_width;
            }
            if (label_x < 0) {
                label_x = 0;
            }
            if (label_y + label_height > static_cast<td_s32>(frame->height)) {
                label_y = static_cast<td_s32>(frame->height) - label_height;
            }
            if (label_y < 0) {
                label_y = 0;
            }

            yuv420sp_fill_rect(buffer, frame->width, frame->height, frame->stride_y, frame->stride_uv,
                frame->input_is_yvu_semiplanar_420, label_x, label_y,
                label_x + label_width, label_y + label_height, box_y, box_u, box_v);
            yuv420sp_draw_text(buffer, frame->width, frame->height, frame->stride_y, frame->stride_uv,
                frame->input_is_yvu_semiplanar_420, label_x + 2, label_y + 2, label, scale, text_y, text_u, text_v);
        }
    }
}

td_bool ai_acl_adapter_cached_detections_valid(AiAclAdapterContext *ctx, const ai_acl_adapter_frame *frame)
{
    if (ctx == nullptr || frame == nullptr || ctx->cached_detections.empty()) {
        return TD_FALSE;
    }

    if (ctx->cached_frame_w != frame->width || ctx->cached_frame_h != frame->height) {
        return TD_FALSE;
    }

    if (frame->frame_id < ctx->cached_frame_id ||
        frame->frame_id - ctx->cached_frame_id > AI_DET_HOLD_FRAMES) {
        return TD_FALSE;
    }

    return TD_TRUE;
}

td_s32 ai_acl_adapter_draw_cached_detections(AiAclAdapterContext *ctx, ai_acl_adapter_frame *frame)
{
    std::vector<AiDetection> detections;

    {
        std::lock_guard<std::mutex> lock(g_detection_mutex);
        if (ai_acl_adapter_cached_detections_valid(ctx, frame) == TD_TRUE) {
            detections = ctx->cached_detections;
        }
    }

    if (detections.empty()) {
        if (frame != nullptr) {
            frame->detection_count = 0;
        }
        return TD_FAILURE;
    }

    ai_acl_adapter_draw_detections(frame, detections);
    frame->detection_count = static_cast<td_s32>(detections.size());
    return TD_SUCCESS;
}

td_void ai_acl_adapter_log_detections(const std::vector<AiDetection> &detections)
{
    static td_s32 log_count = 0;

    if (detections.empty() || log_count >= 20) {
        return;
    }

    log_count++;
    for (size_t i = 0; i < detections.size() && i < 4; ++i) {
        const AiDetection &det = detections[i];
        std::printf("[AI] det[%zu] class=%d name=%s score=%.3f box=(%.0f,%.0f)-(%.0f,%.0f)\n",
            i, det.class_id, det.class_name.c_str(), det.confidence,
            det.x1, det.y1, det.x2, det.y2);
    }
}

td_void ai_acl_adapter_log_model_io(AiAclAdapterContext *ctx)
{
    size_t i;

    if (ctx == nullptr || ctx->model_desc == nullptr) {
        return;
    }

    for (i = 0; i < ctx->input_count; ++i) {
        svp_acl_mdl_io_dims dims = {0};
        size_t buffer_size = svp_acl_mdl_get_input_size_by_index(ctx->model_desc, i);
        size_t stride = svp_acl_mdl_get_input_default_stride(ctx->model_desc, i);
        svp_acl_format format = svp_acl_mdl_get_input_format(ctx->model_desc, i);
        svp_acl_data_type type = svp_acl_mdl_get_input_data_type(ctx->model_desc, i);
        std::ostringstream oss;

        (td_void)svp_acl_mdl_get_input_dims(ctx->model_desc, i, &dims);
        for (size_t j = 0; j < dims.dim_count; ++j) {
            if (j != 0) {
                oss << "x";
            }
            oss << dims.dims[j];
        }

        std::printf("[AI] input[%zu] name=%s dims=%s type=%s format=%s size=%zu stride=%zu\n",
            i,
            safe_cstr(svp_acl_mdl_get_input_name_by_index(ctx->model_desc, i)),
            oss.str().empty() ? "-" : oss.str().c_str(),
            acl_data_type_name(type),
            acl_format_name(format),
            buffer_size,
            stride);
    }

    for (i = 0; i < ctx->output_count; ++i) {
        svp_acl_mdl_io_dims dims = {0};
        size_t buffer_size = svp_acl_mdl_get_output_size_by_index(ctx->model_desc, i);
        size_t stride = svp_acl_mdl_get_output_default_stride(ctx->model_desc, i);
        svp_acl_format format = svp_acl_mdl_get_output_format(ctx->model_desc, i);
        svp_acl_data_type type = svp_acl_mdl_get_output_data_type(ctx->model_desc, i);
        std::ostringstream oss;

        (td_void)svp_acl_mdl_get_output_dims(ctx->model_desc, i, &dims);
        for (size_t j = 0; j < dims.dim_count; ++j) {
            if (j != 0) {
                oss << "x";
            }
            oss << dims.dims[j];
        }

        std::printf("[AI] output[%zu] name=%s dims=%s type=%s format=%s size=%zu stride=%zu\n",
            i,
            safe_cstr(svp_acl_mdl_get_output_name_by_index(ctx->model_desc, i)),
            oss.str().empty() ? "-" : oss.str().c_str(),
            acl_data_type_name(type),
            acl_format_name(format),
            buffer_size,
            stride);
    }
}

td_bool ai_acl_adapter_get_image_dims(AiAclAdapterContext *ctx, size_t index, td_u32 *channels, td_u32 *model_h,
    td_u32 *model_w)
{
    svp_acl_mdl_io_dims dims = {0};
    svp_acl_format format;

    if (ctx == nullptr || ctx->model_desc == nullptr || channels == nullptr || model_h == nullptr || model_w == nullptr) {
        return TD_FALSE;
    }

    if (svp_acl_mdl_get_input_dims(ctx->model_desc, index, &dims) != SVP_ACL_SUCCESS || dims.dim_count != 4) {
        return TD_FALSE;
    }

    format = svp_acl_mdl_get_input_format(ctx->model_desc, index);
    if (format == SVP_ACL_FORMAT_NCHW) {
        *channels = (td_u32)dims.dims[1];
        *model_h = (td_u32)dims.dims[2];
        *model_w = (td_u32)dims.dims[3];
        return TD_TRUE;
    }

    if (format == SVP_ACL_FORMAT_NHWC) {
        *model_h = (td_u32)dims.dims[1];
        *model_w = (td_u32)dims.dims[2];
        *channels = (td_u32)dims.dims[3];
        return TD_TRUE;
    }

    return TD_FALSE;
}

td_s32 ai_acl_adapter_fill_image_input(AiAclAdapterContext *ctx, size_t index, ai_acl_adapter_frame *frame,
    svp_acl_data_buffer *data_buffer, AiPreprocessMeta *meta)
{
    td_u32 channels = 0;
    td_u32 model_h = 0;
    td_u32 model_w = 0;
    svp_acl_format format;
    svp_acl_data_type type;
    td_void *buffer;
    size_t buffer_size;
    size_t input_alloc_size = 0;
    size_t input_stride;
    td_u32 dst_x;
    td_u32 dst_y;

    if (ctx == nullptr || frame == nullptr || data_buffer == nullptr || meta == nullptr) {
        return TD_FAILURE;
    }

    if (frame->stride_y == 0) {
        frame->stride_y = frame->width;
    }
    if (frame->stride_uv == 0) {
        frame->stride_uv = frame->width;
    }

    if (ai_acl_adapter_get_image_dims(ctx, index, &channels, &model_h, &model_w) != TD_TRUE || channels < 1) {
        set_block_reason_text("input[0] dims/format unsupported for frame preprocess");
        return TD_FAILURE;
    }

    format = svp_acl_mdl_get_input_format(ctx->model_desc, index);
    type = svp_acl_mdl_get_input_data_type(ctx->model_desc, index);
    buffer = svp_acl_get_data_buffer_addr(data_buffer);
    buffer_size = svp_acl_get_data_buffer_size(data_buffer);
    input_stride = svp_acl_mdl_get_input_default_stride(ctx->model_desc, index);
    if (ai_acl_adapter_get_input_alloc_size(ctx, index, &input_alloc_size) != TD_TRUE) {
        input_alloc_size = buffer_size;
    }
    std::memset(meta, 0, sizeof(*meta));
    meta->model_w = model_w;
    meta->model_h = model_h;
    meta->scale_x = (float)model_w / (float)frame->width;
    meta->scale_y = (float)model_h / (float)frame->height;
    /* Preserve the source aspect ratio with centered letterbox padding for square inputs. */
    if (model_w == model_h) {
        meta->use_letterbox = TD_TRUE;
        meta->letterbox_scale = std::min((float)model_w / (float)frame->width,
            (float)model_h / (float)frame->height);
        meta->resized_w = std::max(1U, (td_u32)(frame->width * meta->letterbox_scale + 0.5f));
        meta->resized_h = std::max(1U, (td_u32)(frame->height * meta->letterbox_scale + 0.5f));
        meta->pad_x = (model_w - meta->resized_w) / 2;
        meta->pad_y = (model_h - meta->resized_h) / 2;
    }

    if (type == SVP_ACL_FLOAT) {
        td_u8 *dst = static_cast<td_u8 *>(buffer);
        size_t need_size = (size_t)channels * model_h * model_w * sizeof(float);
        if (input_alloc_size < need_size || input_stride < model_w * sizeof(float)) {
            set_block_reason_text("input[0] float buffer too small");
            return TD_FAILURE;
        }

        for (dst_y = 0; dst_y < model_h; ++dst_y) {
            for (dst_x = 0; dst_x < model_w; ++dst_x) {
                td_u32 src_x;
                td_u32 src_y;
                td_u8 r;
                td_u8 g;
                td_u8 b;

                if (meta->use_letterbox == TD_TRUE) {
                    if (dst_x < meta->pad_x || dst_x >= meta->pad_x + meta->resized_w ||
                        dst_y < meta->pad_y || dst_y >= meta->pad_y + meta->resized_h) {
                        continue;
                    }
                    src_x = std::min((td_u32)(((dst_x - meta->pad_x) / meta->letterbox_scale)),
                        frame->width - 1);
                    src_y = std::min((td_u32)(((dst_y - meta->pad_y) / meta->letterbox_scale)),
                        frame->height - 1);
                } else {
                    src_x = std::min((td_u32)((uint64_t)dst_x * frame->width / model_w), frame->width - 1);
                    src_y = std::min((td_u32)((uint64_t)dst_y * frame->height / model_h), frame->height - 1);
                }
                yvu420sp_to_rgb_pixel(frame, src_x, src_y, &r, &g, &b);

                if (format == SVP_ACL_FORMAT_NCHW) {
                    size_t pixel_offset = (size_t)dst_y * input_stride + dst_x * sizeof(float);
                    float value = (float)r / 255.0f;
                    std::memcpy(dst + 0 * model_h * input_stride + pixel_offset, &value, sizeof(value));
                    if (channels > 1) {
                        value = (float)g / 255.0f;
                        std::memcpy(dst + 1 * model_h * input_stride + pixel_offset, &value, sizeof(value));
                    }
                    if (channels > 2) {
                        value = (float)b / 255.0f;
                        std::memcpy(dst + 2 * model_h * input_stride + pixel_offset, &value, sizeof(value));
                    }
                } else {
                    size_t pixel_offset = ((size_t)dst_y * model_w + dst_x) * input_stride;
                    float value = (float)r / 255.0f;
                    std::memcpy(dst + pixel_offset + 0 * sizeof(float), &value, sizeof(value));
                    if (channels > 1) {
                        value = (float)g / 255.0f;
                        std::memcpy(dst + pixel_offset + 1 * sizeof(float), &value, sizeof(value));
                    }
                    if (channels > 2) {
                        value = (float)b / 255.0f;
                        std::memcpy(dst + pixel_offset + 2 * sizeof(float), &value, sizeof(value));
                    }
                }
            }
        }
        return TD_SUCCESS;
    }

    if (type == SVP_ACL_UINT8) {
        td_u8 *dst = static_cast<td_u8 *>(buffer);
        size_t need_size = (size_t)channels * model_h * model_w;
        if (input_alloc_size < need_size || input_stride < model_w) {
            set_block_reason_text("input[0] uint8 buffer too small");
            return TD_FAILURE;
        }

        for (dst_y = 0; dst_y < model_h; ++dst_y) {
            for (dst_x = 0; dst_x < model_w; ++dst_x) {
                td_u32 src_x;
                td_u32 src_y;
                td_u8 r;
                td_u8 g;
                td_u8 b;

                if (meta->use_letterbox == TD_TRUE) {
                    if (dst_x < meta->pad_x || dst_x >= meta->pad_x + meta->resized_w ||
                        dst_y < meta->pad_y || dst_y >= meta->pad_y + meta->resized_h) {
                        continue;
                    }
                    src_x = std::min((td_u32)(((dst_x - meta->pad_x) / meta->letterbox_scale)),
                        frame->width - 1);
                    src_y = std::min((td_u32)(((dst_y - meta->pad_y) / meta->letterbox_scale)),
                        frame->height - 1);
                } else {
                    src_x = std::min((td_u32)((uint64_t)dst_x * frame->width / model_w), frame->width - 1);
                    src_y = std::min((td_u32)((uint64_t)dst_y * frame->height / model_h), frame->height - 1);
                }
                yvu420sp_to_rgb_pixel(frame, src_x, src_y, &r, &g, &b);

                if (format == SVP_ACL_FORMAT_NCHW) {
                    size_t pixel_offset = (size_t)dst_y * input_stride + dst_x;
                    dst[0 * model_h * input_stride + pixel_offset] = r;
                    if (channels > 1) {
                        dst[1 * model_h * input_stride + pixel_offset] = g;
                    }
                    if (channels > 2) {
                        dst[2 * model_h * input_stride + pixel_offset] = b;
                    }
                } else {
                    size_t pixel_offset = ((size_t)dst_y * model_w + dst_x) * input_stride;
                    dst[pixel_offset + 0] = r;
                    if (channels > 1) {
                        dst[pixel_offset + 1] = g;
                    }
                    if (channels > 2) {
                        dst[pixel_offset + 2] = b;
                    }
                }
            }
        }
        return TD_SUCCESS;
    }

    set_block_reason_text("input[0] data type unsupported for frame preprocess");
    return TD_FAILURE;
}

td_s32 ai_acl_adapter_fill_aux_input(AiAclAdapterContext *ctx, size_t index, svp_acl_data_buffer *data_buffer,
    td_u32 frame_w, td_u32 frame_h, const AiPreprocessMeta *meta)
{
    td_void *buffer;
    size_t buffer_size;
    svp_acl_data_type type;
    svp_acl_mdl_io_dims dims = {0};
    const char *name;

    if (ctx == nullptr || data_buffer == nullptr) {
        return TD_FAILURE;
    }

    buffer = svp_acl_get_data_buffer_addr(data_buffer);
    buffer_size = svp_acl_get_data_buffer_size(data_buffer);
    type = svp_acl_mdl_get_input_data_type(ctx->model_desc, index);
    name = svp_acl_mdl_get_input_name_by_index(ctx->model_desc, index);
    (td_void)svp_acl_mdl_get_input_dims(ctx->model_desc, index, &dims);

    if (ai_acl_adapter_is_yolo11_9out(ctx) == TD_TRUE) {
        std::memset(buffer, 0, buffer_size);
        return TD_SUCCESS;
    }

    if (ai_acl_adapter_is_verified_yolov8_rpn(ctx) == TD_TRUE) {
        std::memset(buffer, 0, buffer_size);
        if (index == 1 && type == SVP_ACL_FLOAT && buffer_size >= sizeof(float) * 4) {
            float *dst = static_cast<float *>(buffer);
            dst[0] = AI_RPN_NMS_THRESHOLD;
            dst[1] = AI_RPN_SCORE_THRESHOLD;
            dst[2] = AI_RPN_MIN_HEIGHT;
            dst[3] = AI_RPN_MIN_WIDTH;
        }
        return TD_SUCCESS;
    }

    if (type == SVP_ACL_FLOAT && buffer_size >= sizeof(float) * 2) {
        float *dst = static_cast<float *>(buffer);
        td_bool is_shape = (name != nullptr && std::strstr(name, "shape") != nullptr) ? TD_TRUE : TD_FALSE;
        td_bool is_scale = (name != nullptr && std::strstr(name, "scale") != nullptr) ? TD_TRUE : TD_FALSE;
        td_bool is_task_buf = (name != nullptr && std::strstr(name, "task_buf") != nullptr) ? TD_TRUE : TD_FALSE;
        td_bool is_work_buf = (name != nullptr && std::strstr(name, "work_buf") != nullptr) ? TD_TRUE : TD_FALSE;

        std::memset(buffer, 0, buffer_size);

        if (is_task_buf == TD_TRUE && buffer_size >= sizeof(float) * 4) {
            dst[0] = AI_RPN_NMS_THRESHOLD;
            dst[1] = AI_RPN_SCORE_THRESHOLD;
            dst[2] = AI_RPN_MIN_HEIGHT;
            dst[3] = AI_RPN_MIN_WIDTH;
        } else if (is_work_buf == TD_TRUE) {
            return TD_SUCCESS;
        } else if (is_shape == TD_TRUE) {
            dst[0] = (float)frame_h;
            dst[1] = (float)frame_w;
        } else if (is_scale == TD_TRUE) {
            dst[0] = (meta != nullptr && meta->use_letterbox == TD_TRUE && meta->letterbox_scale > 0.0f) ?
                meta->letterbox_scale : ((meta != nullptr) ? meta->scale_y : 1.0f);
            dst[1] = (meta != nullptr && meta->use_letterbox == TD_TRUE && meta->letterbox_scale > 0.0f) ?
                meta->letterbox_scale : ((meta != nullptr) ? meta->scale_x : 1.0f);
        } else {
            dst[0] = (float)frame_h;
            dst[1] = (float)frame_w;
            if (buffer_size >= sizeof(float) * 4) {
                dst[2] = (meta != nullptr && meta->use_letterbox == TD_TRUE && meta->letterbox_scale > 0.0f) ?
                    meta->letterbox_scale : ((meta != nullptr) ? meta->scale_y : 1.0f);
                dst[3] = (meta != nullptr && meta->use_letterbox == TD_TRUE && meta->letterbox_scale > 0.0f) ?
                    meta->letterbox_scale : ((meta != nullptr) ? meta->scale_x : 1.0f);
            }
        }
        return TD_SUCCESS;
    }

    if (type == SVP_ACL_INT32 && buffer_size >= sizeof(td_s32) * 2) {
        td_s32 *dst = static_cast<td_s32 *>(buffer);
        std::memset(buffer, 0, buffer_size);
        dst[0] = (td_s32)frame_h;
        dst[1] = (td_s32)frame_w;
        return TD_SUCCESS;
    }

    (td_void)dims;
    return TD_SUCCESS;
}

td_s32 ai_acl_adapter_fill_input_dataset(AiAclAdapterContext *ctx, svp_acl_mdl_dataset *dataset,
    ai_acl_adapter_frame *frame, AiPreprocessMeta *meta)
{
    if (ctx == nullptr || dataset == nullptr || frame == nullptr || meta == nullptr) {
        set_block_reason_text("fill input dataset precondition failed");
        return TD_FAILURE;
    }

    for (size_t i = 0; i < ctx->input_count; ++i) {
        svp_acl_data_buffer *data_buffer = svp_acl_mdl_get_dataset_buffer(dataset, i);
        if (data_buffer == nullptr) {
            set_block_reason_text("get input dataset buffer failed");
            return TD_FAILURE;
        }

        if (i == 0) {
            if (ai_acl_adapter_fill_image_input(ctx, i, frame, data_buffer, meta) != TD_SUCCESS) {
                return TD_FAILURE;
            }
        } else {
            if (ai_acl_adapter_fill_aux_input(ctx, i, data_buffer, frame->width, frame->height, meta) != TD_SUCCESS) {
                return TD_FAILURE;
            }
        }
    }

    return TD_SUCCESS;
}

td_void ai_acl_adapter_log_output_probe(const td_void *buffer, size_t buffer_size, svp_acl_data_type type,
    const svp_acl_mdl_io_dims *dims, size_t stride_bytes);

td_s32 ai_acl_adapter_parse_yolov8_rpn(AiAclAdapterContext *ctx, svp_acl_mdl_dataset *dataset,
    const AiPreprocessMeta *meta, td_u32 frame_w, td_u32 frame_h, std::vector<AiDetection> *detections)
{
    svp_acl_data_buffer *num_data_buffer;
    svp_acl_data_buffer *bbox_data_buffer;
    td_void *num_buffer;
    td_void *bbox_buffer;
    size_t num_buffer_size;
    size_t bbox_buffer_size;
    svp_acl_mdl_io_dims num_dims = {0};
    svp_acl_mdl_io_dims bbox_dims = {0};
    svp_acl_data_type num_type;
    svp_acl_data_type bbox_type;
    size_t num_stride;
    size_t bbox_stride;
    size_t rows;
    float total_f = 0.0f;
    td_s32 total;
    std::vector<AiDetection> parsed;
    static td_s32 rpn_log_count = 0;

    if (ctx == nullptr || dataset == nullptr || meta == nullptr || detections == nullptr ||
        ctx->output_count < 2 || svp_acl_mdl_get_dataset_num_buffers(dataset) < 2 ||
        ai_acl_adapter_is_verified_yolov8_rpn(ctx) != TD_TRUE) {
        return TD_FAILURE;
    }

    /* Verified OM contract: output[0] is the count; output[1] has up to 300 rows of six floats. */
    num_data_buffer = svp_acl_mdl_get_dataset_buffer(dataset, 0);
    bbox_data_buffer = svp_acl_mdl_get_dataset_buffer(dataset, 1);
    if (num_data_buffer == nullptr || bbox_data_buffer == nullptr) {
        return TD_FAILURE;
    }

    num_buffer = svp_acl_get_data_buffer_addr(num_data_buffer);
    bbox_buffer = svp_acl_get_data_buffer_addr(bbox_data_buffer);
    num_buffer_size = svp_acl_get_data_buffer_size(num_data_buffer);
    bbox_buffer_size = svp_acl_get_data_buffer_size(bbox_data_buffer);
    num_type = svp_acl_mdl_get_output_data_type(ctx->model_desc, 0);
    bbox_type = svp_acl_mdl_get_output_data_type(ctx->model_desc, 1);
    num_stride = svp_acl_mdl_get_output_default_stride(ctx->model_desc, 0);
    bbox_stride = svp_acl_mdl_get_output_default_stride(ctx->model_desc, 1);
    (td_void)svp_acl_mdl_get_output_dims(ctx->model_desc, 0, &num_dims);
    (td_void)svp_acl_mdl_get_output_dims(ctx->model_desc, 1, &bbox_dims);

    if (num_buffer == nullptr || bbox_buffer == nullptr ||
        ai_acl_adapter_output_value(num_buffer, num_buffer_size, num_type, &num_dims, num_stride, 0,
            &total_f) != TD_TRUE) {
        return TD_FAILURE;
    }

    rows = static_cast<size_t>(bbox_dims.dims[bbox_dims.dim_count - 1]);
    total = static_cast<td_s32>(total_f + 0.5f);
    if (total < 0) {
        total = 0;
    }
    if (static_cast<size_t>(total) > rows) {
        total = static_cast<td_s32>(rows);
    }

    if (rpn_log_count < 6) {
        std::printf("[AI] yolov8 rpn output total=%d rows=%zu rpn_threshold=%.2f display_threshold=%.2f\n",
            total, rows, AI_RPN_SCORE_THRESHOLD, AI_DET_LIVE_SCORE_THRESHOLD);
        rpn_log_count++;
    }

    for (td_s32 i = 0; i < total; ++i) {
        float a;
        float b;
        float c;
        float d;
        float score;
        float cls_f;
        AiDetection det = {0};

        if (ai_acl_adapter_output_value(bbox_buffer, bbox_buffer_size, bbox_type, &bbox_dims, bbox_stride,
                0 * rows + static_cast<size_t>(i), &a) != TD_TRUE ||
            ai_acl_adapter_output_value(bbox_buffer, bbox_buffer_size, bbox_type, &bbox_dims, bbox_stride,
                1 * rows + static_cast<size_t>(i), &b) != TD_TRUE ||
            ai_acl_adapter_output_value(bbox_buffer, bbox_buffer_size, bbox_type, &bbox_dims, bbox_stride,
                2 * rows + static_cast<size_t>(i), &c) != TD_TRUE ||
            ai_acl_adapter_output_value(bbox_buffer, bbox_buffer_size, bbox_type, &bbox_dims, bbox_stride,
                3 * rows + static_cast<size_t>(i), &d) != TD_TRUE ||
            ai_acl_adapter_output_value(bbox_buffer, bbox_buffer_size, bbox_type, &bbox_dims, bbox_stride,
                4 * rows + static_cast<size_t>(i), &score) != TD_TRUE ||
            ai_acl_adapter_output_value(bbox_buffer, bbox_buffer_size, bbox_type, &bbox_dims, bbox_stride,
                5 * rows + static_cast<size_t>(i), &cls_f) != TD_TRUE) {
            continue;
        }

        if (!std::isfinite(score) || score < AI_DET_LIVE_SCORE_THRESHOLD || !std::isfinite(cls_f)) {
            continue;
        }

        if (ai_acl_adapter_decode_box(meta, frame_w, frame_h, a, b, c, d,
            &det.x1, &det.y1, &det.x2, &det.y2) != TD_TRUE) {
            continue;
        }

        det.class_id = static_cast<td_s32>(cls_f + 0.5f);
        det.confidence = score;
        if (det.class_id >= 0 && static_cast<size_t>(det.class_id) < ctx->classes.size()) {
            det.class_name = ai_acl_adapter_get_class_name(ctx, det.class_id);
        } else {
            det.class_name = "CLASS " + std::to_string(det.class_id);
        }
        parsed.push_back(det);
    }

    ai_acl_adapter_apply_nms(&parsed, AI_DET_NMS_THRESHOLD);
    detections->swap(parsed);
    return TD_SUCCESS;
}

td_s32 ai_acl_adapter_parse_detections(AiAclAdapterContext *ctx, svp_acl_mdl_dataset *dataset,
    const AiPreprocessMeta *meta, td_u32 frame_w, td_u32 frame_h, std::vector<AiDetection> *detections)
{
    svp_acl_data_buffer *data_buffer;
    td_void *buffer;
    size_t buffer_size;
    svp_acl_data_type type;
    svp_acl_mdl_io_dims dims = {0};
    size_t rows = 0;
    size_t i;
    size_t logical_count;
    size_t stride_bytes;
    td_bool row_major = TD_TRUE;
    std::vector<AiDetection> parsed;

    if (ctx == nullptr || dataset == nullptr || meta == nullptr || detections == nullptr || ctx->output_count == 0) {
        return TD_FAILURE;
    }

    if (ai_acl_adapter_parse_yolo11_9out(ctx, dataset, meta, frame_w, frame_h, detections) == TD_SUCCESS) {
        return TD_SUCCESS;
    }

    if (ai_acl_adapter_parse_yolov8_rpn(ctx, dataset, meta, frame_w, frame_h, detections) == TD_SUCCESS) {
        return TD_SUCCESS;
    }

    data_buffer = svp_acl_mdl_get_dataset_buffer(dataset, 0);
    if (data_buffer == nullptr) {
        return TD_FAILURE;
    }

    buffer = svp_acl_get_data_buffer_addr(data_buffer);
    buffer_size = svp_acl_get_data_buffer_size(data_buffer);
    type = svp_acl_mdl_get_output_data_type(ctx->model_desc, 0);
    stride_bytes = svp_acl_mdl_get_output_default_stride(ctx->model_desc, 0);
    (td_void)svp_acl_mdl_get_output_dims(ctx->model_desc, 0, &dims);
    ai_acl_adapter_log_output_probe(buffer, buffer_size, type, &dims, stride_bytes);

    if ((type != SVP_ACL_FLOAT && type != SVP_ACL_FLOAT16 && type != SVP_ACL_UINT8 && type != SVP_ACL_INT8 &&
        type != SVP_ACL_INT32) || buffer == nullptr || buffer_size < acl_data_type_size_bytes(type) * 5) {
        detections->clear();
        return TD_SUCCESS;
    }

    logical_count = ai_acl_adapter_dims_count(&dims);
    if (logical_count == 0) {
        logical_count = buffer_size / acl_data_type_size_bytes(type);
    }

    if (dims.dim_count >= 2 && dims.dims[dims.dim_count - 1] == 6) {
        row_major = TD_TRUE;
        rows = logical_count / 6;
    } else if (dims.dim_count >= 2 && dims.dims[dims.dim_count - 2] == 6) {
        row_major = TD_FALSE;
        rows = (size_t)dims.dims[dims.dim_count - 1];
    } else if ((logical_count % 6) == 0 && logical_count <= 4096 * 6) {
        row_major = TD_TRUE;
        rows = logical_count / 6;
    } else {
        if (ai_acl_adapter_parse_yolo_raw(ctx, buffer, buffer_size, type, &dims, stride_bytes, meta,
            frame_w, frame_h, detections) == TD_SUCCESS) {
            return TD_SUCCESS;
        }
        detections->clear();
        return TD_SUCCESS;
    }

    for (i = 0; i < rows; ++i) {
        float a;
        float b;
        float c;
        float d;
        float score;
        float cls_f;
        AiDetection det = {0};

        if (row_major == TD_TRUE) {
            if (ai_acl_adapter_output_value(buffer, buffer_size, type, &dims, stride_bytes, i * 6 + 0, &a) != TD_TRUE ||
                ai_acl_adapter_output_value(buffer, buffer_size, type, &dims, stride_bytes, i * 6 + 1, &b) != TD_TRUE ||
                ai_acl_adapter_output_value(buffer, buffer_size, type, &dims, stride_bytes, i * 6 + 2, &c) != TD_TRUE ||
                ai_acl_adapter_output_value(buffer, buffer_size, type, &dims, stride_bytes, i * 6 + 3, &d) != TD_TRUE ||
                ai_acl_adapter_output_value(buffer, buffer_size, type, &dims, stride_bytes, i * 6 + 4, &score) != TD_TRUE ||
                ai_acl_adapter_output_value(buffer, buffer_size, type, &dims, stride_bytes, i * 6 + 5, &cls_f) != TD_TRUE) {
                continue;
            }
        } else {
            if (ai_acl_adapter_output_value(buffer, buffer_size, type, &dims, stride_bytes, 0 * rows + i, &a) != TD_TRUE ||
                ai_acl_adapter_output_value(buffer, buffer_size, type, &dims, stride_bytes, 1 * rows + i, &b) != TD_TRUE ||
                ai_acl_adapter_output_value(buffer, buffer_size, type, &dims, stride_bytes, 2 * rows + i, &c) != TD_TRUE ||
                ai_acl_adapter_output_value(buffer, buffer_size, type, &dims, stride_bytes, 3 * rows + i, &d) != TD_TRUE ||
                ai_acl_adapter_output_value(buffer, buffer_size, type, &dims, stride_bytes, 4 * rows + i, &score) != TD_TRUE ||
                ai_acl_adapter_output_value(buffer, buffer_size, type, &dims, stride_bytes, 5 * rows + i, &cls_f) != TD_TRUE) {
                continue;
            }
        }

        score = ai_acl_adapter_score_value(score);
        if (!std::isfinite(score) || score < AI_DET_SCORE_THRESHOLD || !std::isfinite(cls_f)) {
            continue;
        }
        if (ai_acl_adapter_decode_box(meta, frame_w, frame_h, a, b, c, d,
            &det.x1, &det.y1, &det.x2, &det.y2) != TD_TRUE) {
            continue;
        }

        det.class_id = static_cast<td_s32>(cls_f + 0.5f);
        det.confidence = score;
        if (det.class_id >= 0 && static_cast<size_t>(det.class_id) < ctx->classes.size()) {
            det.class_name = ai_acl_adapter_get_class_name(ctx, det.class_id);
        } else {
            det.class_name = "CLASS " + std::to_string(det.class_id);
        }
        parsed.push_back(det);
    }

    ai_acl_adapter_apply_nms(&parsed, AI_DET_NMS_THRESHOLD);
    detections->swap(parsed);
    return TD_SUCCESS;
}

td_void ai_acl_adapter_log_output_probe(const td_void *buffer, size_t buffer_size, svp_acl_data_type type,
    const svp_acl_mdl_io_dims *dims, size_t stride_bytes)
{
    static td_s32 probe_count = 0;
    size_t rows;
    float ch_min[6];
    float ch_max[6];
    size_t score_gt_001 = 0;
    size_t score_gt_015 = 0;
    size_t score_gt_025 = 0;
    float top_score[3] = {-1.0f, -1.0f, -1.0f};
    size_t top_index[3] = {0, 0, 0};
    float top_values[3][6] = {{0}};

    if (probe_count >= 6 || buffer == nullptr || dims == nullptr || dims->dim_count < 2 ||
        dims->dims[dims->dim_count - 2] != 6 || dims->dims[dims->dim_count - 1] <= 0) {
        return;
    }
    probe_count++;

    rows = (size_t)dims->dims[dims->dim_count - 1];
    for (size_t c = 0; c < 6; ++c) {
        ch_min[c] = INFINITY;
        ch_max[c] = -INFINITY;
    }

    for (size_t i = 0; i < rows; ++i) {
        float value[6] = {0.0f};
        td_bool valid = TD_TRUE;
        for (size_t c = 0; c < 6; ++c) {
            if (ai_acl_adapter_output_value(buffer, buffer_size, type, dims, stride_bytes, c * rows + i,
                &value[c]) != TD_TRUE) {
                valid = TD_FALSE;
                break;
            }
            if (std::isfinite(value[c])) {
                ch_min[c] = std::min(ch_min[c], value[c]);
                ch_max[c] = std::max(ch_max[c], value[c]);
            }
        }
        if (valid != TD_TRUE || !std::isfinite(value[4])) {
            continue;
        }

        float score = ai_acl_adapter_score_value(value[4]);
        if (score > 0.01f) {
            score_gt_001++;
        }
        if (score > AI_RPN_SCORE_THRESHOLD) {
            score_gt_015++;
        }
        if (score > AI_DET_SCORE_THRESHOLD) {
            score_gt_025++;
        }
        for (size_t rank = 0; rank < 3; ++rank) {
            if (score > top_score[rank]) {
                for (size_t move = 2; move > rank; --move) {
                    top_score[move] = top_score[move - 1];
                    top_index[move] = top_index[move - 1];
                    std::memcpy(top_values[move], top_values[move - 1], sizeof(top_values[move]));
                }
                top_score[rank] = score;
                top_index[rank] = i;
                std::memcpy(top_values[rank], value, sizeof(value));
                break;
            }
        }
    }

    std::printf("[AI] output probe #%d rows=%zu ch0=[%.3f,%.3f] ch1=[%.3f,%.3f] "
        "ch2=[%.3f,%.3f] ch3=[%.3f,%.3f] ch4=[%.3f,%.3f] ch5=[%.3f,%.3f] "
        "score_gt_0.01=%zu score_gt_0.15=%zu score_gt_0.25=%zu\n",
        probe_count, rows,
        ch_min[0], ch_max[0], ch_min[1], ch_max[1], ch_min[2], ch_max[2],
        ch_min[3], ch_max[3], ch_min[4], ch_max[4], ch_min[5], ch_max[5],
        score_gt_001, score_gt_015, score_gt_025);
    for (size_t rank = 0; rank < 3; ++rank) {
        if (top_score[rank] >= 0.0f) {
            std::printf("[AI] output top%zu idx=%zu score=%.3f raw={%.3f,%.3f,%.3f,%.3f,%.3f,%.3f}\n",
                rank + 1, top_index[rank], top_score[rank], top_values[rank][0], top_values[rank][1],
                top_values[rank][2], top_values[rank][3], top_values[rank][4], top_values[rank][5]);
        }
    }
}

td_void ai_acl_adapter_close_context(AiAclAdapterContext *ctx)
{
    if (ctx == nullptr) {
        return;
    }

    /* Release Dataset device buffers before unloading the model and ACL context. */
    ai_acl_adapter_destroy_dataset(&ctx->input_dataset);
    ai_acl_adapter_destroy_dataset(&ctx->output_dataset);

    if (ctx->model_loaded || ctx->model_desc != nullptr || ctx->model_mem_ptr != nullptr) {
        if (ctx->model_loaded) {
            (td_void)svp_acl_mdl_unload(ctx->model_id);
            ctx->model_loaded = false;
        }

        if (ctx->model_desc != nullptr) {
            (td_void)svp_acl_mdl_destroy_desc(ctx->model_desc);
            ctx->model_desc = nullptr;
        }

        if (ctx->model_mem_ptr != nullptr) {
            (td_void)svp_acl_rt_free(ctx->model_mem_ptr);
            ctx->model_mem_ptr = nullptr;
            ctx->model_mem_size = 0;
        }
    }

    if (ctx->stream_created && ctx->stream != nullptr) {
        (td_void)svp_acl_rt_destroy_stream(ctx->stream);
        ctx->stream = nullptr;
        ctx->stream_created = false;
    }

    if (ctx->context_created && ctx->context != nullptr) {
        (td_void)svp_acl_rt_destroy_context(ctx->context);
        ctx->context = nullptr;
        ctx->context_created = false;
    }

    if (ctx->device_opened) {
        (td_void)svp_acl_rt_reset_device(ctx->device_id);
        ctx->device_opened = false;
    }

    if (ctx->acl_inited) {
        (td_void)svp_acl_finalize();
        ctx->acl_inited = false;
    }
}

td_void om_model_release(AiAclAdapterContext *ctx)
{
    if (ctx == nullptr) {
        return;
    }

    ai_acl_adapter_destroy_dataset(&ctx->input_dataset);
    ai_acl_adapter_destroy_dataset(&ctx->output_dataset);

    if (ctx->model_loaded) {
        (td_void)svp_acl_mdl_unload(ctx->model_id);
        ctx->model_loaded = false;
    }

    if (ctx->model_desc != nullptr) {
        (td_void)svp_acl_mdl_destroy_desc(ctx->model_desc);
        ctx->model_desc = nullptr;
    }

    if (ctx->model_mem_ptr != nullptr) {
        (td_void)svp_acl_rt_free(ctx->model_mem_ptr);
        ctx->model_mem_ptr = nullptr;
        ctx->model_mem_size = 0;
    }

    ctx->model_id = 0;
    ctx->input_count = 0;
    ctx->output_count = 0;
}

td_s32 ai_acl_adapter_read_model(AiAclAdapterContext *ctx)
{
    FILE *fp = nullptr;
    long file_size;
    size_t read_size;
    svp_acl_error ret;

    if (ctx == nullptr || ctx->model_path.empty()) {
        set_block_reason_text("model path is empty");
        return TD_FAILURE;
    }

    fp = std::fopen(ctx->model_path.c_str(), "rb");
    if (fp == nullptr) {
        set_block_reason_errno("open model file failed", ctx->model_path);
        return TD_FAILURE;
    }

    if (std::fseek(fp, 0, SEEK_END) != 0) {
        set_block_reason_errno("seek model file failed", ctx->model_path);
        std::fclose(fp);
        return TD_FAILURE;
    }

    file_size = std::ftell(fp);
    if (file_size <= 0) {
        set_block_reason_text("model file is empty: path=\"" + ctx->model_path + "\"");
        std::fclose(fp);
        return TD_FAILURE;
    }

    if (std::fseek(fp, 0, SEEK_SET) != 0) {
        set_block_reason_errno("rewind model file failed", ctx->model_path);
        std::fclose(fp);
        return TD_FAILURE;
    }

    ret = svp_acl_rt_malloc(&ctx->model_mem_ptr, (size_t)file_size, SVP_ACL_MEM_MALLOC_NORMAL_ONLY);
    if (ret != SVP_ACL_SUCCESS) {
        set_block_reason_text("svp_acl_rt_malloc model buffer failed");
        std::fclose(fp);
        return TD_FAILURE;
    }

    read_size = std::fread(ctx->model_mem_ptr, 1, (size_t)file_size, fp);
    std::fclose(fp);
    if (read_size != (size_t)file_size) {
        set_block_reason_text("read model file failed: path=\"" + ctx->model_path + "\"");
        return TD_FAILURE;
    }

    ctx->model_mem_size = (td_ulong)file_size;
    std::printf("[AI] model file read ok: path=\"%s\", size=%ld bytes\n",
        ctx->model_path.c_str(), file_size);
    return TD_SUCCESS;
}

td_s32 ai_acl_adapter_load_model(AiAclAdapterContext *ctx)
{
    svp_acl_error ret;

    if (ai_acl_adapter_read_model(ctx) != TD_SUCCESS) {
        return TD_FAILURE;
    }

    ret = svp_acl_mdl_load_from_mem(static_cast<td_u8 *>(ctx->model_mem_ptr), ctx->model_mem_size, &ctx->model_id);
    if (ret != SVP_ACL_SUCCESS) {
        set_block_reason_text("svp_acl_mdl_load_from_mem failed");
        return TD_FAILURE;
    }
    ctx->model_loaded = true;

    ctx->model_desc = svp_acl_mdl_create_desc();
    if (ctx->model_desc == nullptr) {
        set_block_reason_text("svp_acl_mdl_create_desc failed");
        return TD_FAILURE;
    }

    ret = svp_acl_mdl_get_desc(ctx->model_desc, ctx->model_id);
    if (ret != SVP_ACL_SUCCESS) {
        set_block_reason_text("svp_acl_mdl_get_desc failed");
        return TD_FAILURE;
    }

    ctx->input_count = svp_acl_mdl_get_num_inputs(ctx->model_desc);
    ctx->output_count = svp_acl_mdl_get_num_outputs(ctx->model_desc);
    if (ctx->input_count == 0 || ctx->output_count == 0) {
        set_block_reason_text("model io description is empty");
        return TD_FAILURE;
    }

    return TD_SUCCESS;
}

td_s32 om_model_init(AiAclAdapterContext *ctx, const std::string &model_path)
{
    if (ctx == nullptr || model_path.empty()) {
        set_block_reason_text("new model path is empty");
        return TD_FAILURE;
    }

    ctx->model_path = model_path;
    ctx->model_mem_ptr = nullptr;
    ctx->model_mem_size = 0;
    ctx->model_id = 0;
    ctx->model_desc = nullptr;
    ctx->model_loaded = false;
    ctx->input_count = 0;
    ctx->output_count = 0;
    ctx->input_dataset = nullptr;
    ctx->output_dataset = nullptr;

    if (ai_acl_adapter_load_model(ctx) != TD_SUCCESS) {
        om_model_release(ctx);
        return TD_FAILURE;
    }

    return TD_SUCCESS;
}

td_void ai_acl_adapter_destroy_dataset(svp_acl_mdl_dataset **dataset)
{
    size_t i;
    size_t count;
    svp_acl_data_buffer *data_buffer;
    td_void *data;

    if (dataset == nullptr || *dataset == nullptr) {
        return;
    }

    /* Destroying a Dataset does not free device buffers added by the application. */
    count = svp_acl_mdl_get_dataset_num_buffers(*dataset);
    for (i = 0; i < count; ++i) {
        data_buffer = svp_acl_mdl_get_dataset_buffer(*dataset, i);
        if (data_buffer == nullptr) {
            continue;
        }

        data = svp_acl_get_data_buffer_addr(data_buffer);
        if (data != nullptr) {
            (td_void)svp_acl_rt_free(data);
        }
        (td_void)svp_acl_destroy_data_buffer(data_buffer);
    }

    (td_void)svp_acl_mdl_destroy_dataset(*dataset);
    *dataset = nullptr;
}

td_s32 ai_acl_adapter_create_input_dataset(AiAclAdapterContext *ctx, svp_acl_mdl_dataset **dataset)
{
    size_t i;
    size_t stride;
    size_t buffer_size;
    size_t alloc_size;
    td_void *buffer = nullptr;
    svp_acl_data_buffer *data_buffer = nullptr;
    svp_acl_error ret;

    if (ctx == nullptr || dataset == nullptr || ctx->model_desc == nullptr) {
        set_block_reason_text("input dataset precondition failed");
        return TD_FAILURE;
    }

    *dataset = svp_acl_mdl_create_dataset();
    if (*dataset == nullptr) {
        set_block_reason_text("svp_acl_mdl_create_dataset input failed");
        return TD_FAILURE;
    }

    for (i = 0; i < ctx->input_count; ++i) {
        stride = svp_acl_mdl_get_input_default_stride(ctx->model_desc, i);
        buffer_size = svp_acl_mdl_get_input_size_by_index(ctx->model_desc, i);
        if (stride == 0 || buffer_size == 0) {
            set_block_reason_text("input stride or size invalid");
            goto fail;
        }
        if (ai_acl_adapter_get_input_alloc_size(ctx, i, &alloc_size) != TD_TRUE) {
            set_block_reason_text("input alloc size invalid");
            goto fail;
        }

        ret = svp_acl_rt_malloc(&buffer, alloc_size, SVP_ACL_MEM_MALLOC_NORMAL_ONLY);
        if (ret != SVP_ACL_SUCCESS) {
            set_block_reason_text("svp_acl_rt_malloc input failed");
            goto fail;
        }
        (td_void)std::memset(buffer, 0, alloc_size);

        data_buffer = svp_acl_create_data_buffer(buffer, buffer_size, stride);
        if (data_buffer == nullptr) {
            set_block_reason_text("svp_acl_create_data_buffer input failed");
            (td_void)svp_acl_rt_free(buffer);
            buffer = nullptr;
            goto fail;
        }

        ret = svp_acl_mdl_add_dataset_buffer(*dataset, data_buffer);
        if (ret != SVP_ACL_SUCCESS) {
            set_block_reason_text("svp_acl_mdl_add_dataset_buffer input failed");
            (td_void)svp_acl_destroy_data_buffer(data_buffer);
            (td_void)svp_acl_rt_free(buffer);
            buffer = nullptr;
            goto fail;
        }
    }

    return TD_SUCCESS;

fail:
    ai_acl_adapter_destroy_dataset(dataset);
    return TD_FAILURE;
}

td_s32 ai_acl_adapter_create_output_dataset(AiAclAdapterContext *ctx, svp_acl_mdl_dataset **dataset)
{
    size_t i;
    size_t stride;
    size_t buffer_size;
    td_void *buffer = nullptr;
    svp_acl_data_buffer *data_buffer = nullptr;
    svp_acl_error ret;

    if (ctx == nullptr || dataset == nullptr || ctx->model_desc == nullptr) {
        set_block_reason_text("output dataset precondition failed");
        return TD_FAILURE;
    }

    *dataset = svp_acl_mdl_create_dataset();
    if (*dataset == nullptr) {
        set_block_reason_text("svp_acl_mdl_create_dataset output failed");
        return TD_FAILURE;
    }

    for (i = 0; i < ctx->output_count; ++i) {
        stride = svp_acl_mdl_get_output_default_stride(ctx->model_desc, i);
        buffer_size = svp_acl_mdl_get_output_size_by_index(ctx->model_desc, i);
        if (stride == 0 || buffer_size == 0) {
            set_block_reason_text("output stride or size invalid");
            goto fail;
        }

        ret = svp_acl_rt_malloc(&buffer, buffer_size, SVP_ACL_MEM_MALLOC_NORMAL_ONLY);
        if (ret != SVP_ACL_SUCCESS) {
            set_block_reason_text("svp_acl_rt_malloc output failed");
            goto fail;
        }
        (td_void)std::memset(buffer, 0, buffer_size);

        data_buffer = svp_acl_create_data_buffer(buffer, buffer_size, stride);
        if (data_buffer == nullptr) {
            set_block_reason_text("svp_acl_create_data_buffer output failed");
            (td_void)svp_acl_rt_free(buffer);
            buffer = nullptr;
            goto fail;
        }

        ret = svp_acl_mdl_add_dataset_buffer(*dataset, data_buffer);
        if (ret != SVP_ACL_SUCCESS) {
            set_block_reason_text("svp_acl_mdl_add_dataset_buffer output failed");
            (td_void)svp_acl_destroy_data_buffer(data_buffer);
            (td_void)svp_acl_rt_free(buffer);
            buffer = nullptr;
            goto fail;
        }
    }

    return TD_SUCCESS;

fail:
    ai_acl_adapter_destroy_dataset(dataset);
    return TD_FAILURE;
}

td_s32 ai_acl_adapter_prepare_runtime_datasets(AiAclAdapterContext *ctx)
{
    /* Dataset shapes are model-specific, so hot updates replace them with the model. */
    if (ctx == nullptr || ctx->input_dataset != nullptr || ctx->output_dataset != nullptr) {
        set_block_reason_text("runtime dataset state invalid");
        return TD_FAILURE;
    }
    if (ai_acl_adapter_create_input_dataset(ctx, &ctx->input_dataset) != TD_SUCCESS) {
        return TD_FAILURE;
    }
    if (ai_acl_adapter_create_output_dataset(ctx, &ctx->output_dataset) != TD_SUCCESS) {
        ai_acl_adapter_destroy_dataset(&ctx->input_dataset);
        return TD_FAILURE;
    }
    std::printf("[AI] runtime datasets ready inputs=%zu outputs=%zu (reused per inference)\n",
        ctx->input_count, ctx->output_count);
    return TD_SUCCESS;
}

td_s32 ai_acl_adapter_self_test_execute(AiAclAdapterContext *ctx)
{
    svp_acl_data_buffer *output_buffer = nullptr;
    td_void *output_addr = nullptr;
    size_t output_size = 0;
    svp_acl_error ret;

    if (ctx == nullptr || ctx->input_dataset == nullptr || ctx->output_dataset == nullptr) {
        set_block_reason_text("self-test runtime dataset is not ready");
        return TD_FAILURE;
    }

    ret = svp_acl_mdl_execute(ctx->model_id, ctx->input_dataset, ctx->output_dataset);
    if (ret != SVP_ACL_SUCCESS) {
        set_block_reason_text("svp_acl_mdl_execute failed");
        return TD_FAILURE;
    }

    if (ctx->output_count > 0) {
        output_buffer = svp_acl_mdl_get_dataset_buffer(ctx->output_dataset, 0);
        if (output_buffer != nullptr) {
            output_addr = svp_acl_get_data_buffer_addr(output_buffer);
            output_size = svp_acl_get_data_buffer_size(output_buffer);
            std::printf("[AI] acl self-test execute ok: first_output_addr=%p size=%zu\n",
                output_addr, output_size);
        }
    }

    return TD_SUCCESS;
}

} // namespace

td_s32 ai_acl_adapter_init(const ai_acl_adapter_attr *attr)
{
    std::lock_guard<std::mutex> lock(g_acl_mutex);
    svp_acl_rt_run_mode run_mode;
    svp_acl_error ret;

    if (g_acl_ctx != nullptr) {
        return TD_SUCCESS;
    }

    g_acl_ctx = new (std::nothrow) AiAclAdapterContext();
    if (g_acl_ctx == nullptr) {
        set_block_reason_text("ACL adapter alloc failed");
        return TD_FAILURE;
    }

    g_acl_ctx->context = nullptr;
    g_acl_ctx->stream = nullptr;
    g_acl_ctx->device_id = 0;
    g_acl_ctx->model_mem_ptr = nullptr;
    g_acl_ctx->model_desc = nullptr;
    g_acl_ctx->model_mem_size = 0;
    g_acl_ctx->model_id = 0;
    g_acl_ctx->input_count = 0;
    g_acl_ctx->output_count = 0;
    g_acl_ctx->input_dataset = nullptr;
    g_acl_ctx->output_dataset = nullptr;
    g_acl_ctx->cached_frame_id = -AI_DET_HOLD_FRAMES - 1;
    g_acl_ctx->cached_frame_w = 0;
    g_acl_ctx->cached_frame_h = 0;
    g_acl_ctx->empty_infer_count = 0;

    if (attr != nullptr) {
        g_acl_ctx->model_path = safe_cstr(attr->model_path);
        g_acl_ctx->config_path = safe_cstr(attr->config_path);
        g_acl_ctx->classes_path = safe_cstr(attr->classes_path);
        g_acl_ctx->inference_enabled = (attr->enable_inference == TD_TRUE);
        g_acl_ctx->reporter_enabled = (attr->enable_reporter == TD_TRUE);
        g_acl_ctx->model_receiver_enabled = (attr->enable_model_receiver == TD_TRUE);
        g_acl_ctx->device_id = attr->device_id;
    }

    ret = svp_acl_init(attr != nullptr ? attr->config_path : nullptr);
    if (ret != SVP_ACL_SUCCESS) {
        set_block_reason_text("svp_acl_init failed");
        goto fail;
    }
    g_acl_ctx->acl_inited = true;

    ret = svp_acl_rt_set_device(g_acl_ctx->device_id);
    if (ret != SVP_ACL_SUCCESS) {
        set_block_reason_text("svp_acl_rt_set_device failed");
        goto fail;
    }
    g_acl_ctx->device_opened = true;

    ret = svp_acl_rt_set_op_wait_timeout(0);
    if (ret != SVP_ACL_SUCCESS) {
        set_block_reason_text("svp_acl_rt_set_op_wait_timeout failed");
        goto fail;
    }
    g_acl_ctx->timeout_set = true;

    ret = svp_acl_rt_create_context(&g_acl_ctx->context, g_acl_ctx->device_id);
    if (ret != SVP_ACL_SUCCESS) {
        set_block_reason_text("svp_acl_rt_create_context failed");
        goto fail;
    }
    g_acl_ctx->context_created = true;

    ret = svp_acl_rt_create_stream(&g_acl_ctx->stream);
    if (ret != SVP_ACL_SUCCESS) {
        set_block_reason_text("svp_acl_rt_create_stream failed");
        goto fail;
    }
    g_acl_ctx->stream_created = true;

    ret = svp_acl_rt_get_run_mode(&run_mode);
    if (ret != SVP_ACL_SUCCESS) {
        set_block_reason_text("svp_acl_rt_get_run_mode failed");
        goto fail;
    }
    if (run_mode != SVP_ACL_DEVICE) {
        set_block_reason_text("run mode is not SVP_ACL_DEVICE");
        goto fail;
    }

    if (g_acl_ctx->inference_enabled) {
        std::printf("[AI] acl self-test start: model=\"%s\"\n", g_acl_ctx->model_path.c_str());
        if (ai_acl_adapter_load_model(g_acl_ctx) != TD_SUCCESS) {
            goto fail;
        }
        ai_acl_adapter_load_classes(g_acl_ctx);
        std::printf("[AI] acl model loaded: path=\"%s\", inputs=%zu, outputs=%zu, model_id=%u\n",
            g_acl_ctx->model_path.c_str(), g_acl_ctx->input_count, g_acl_ctx->output_count, g_acl_ctx->model_id);
        ai_acl_adapter_log_model_io(g_acl_ctx);
        if (ai_acl_adapter_prepare_runtime_datasets(g_acl_ctx) != TD_SUCCESS) {
            goto fail;
        }
        if (ai_acl_adapter_self_test_execute(g_acl_ctx) != TD_SUCCESS) {
            goto fail;
        }
    }

    g_acl_block_reason.clear();
    std::printf("[AI] acl adapter prepared (backend=%s, dev=%d, ai=%d, model=\"%s\")\n",
        ai_acl_adapter_backend_name(), g_acl_ctx->device_id,
        g_acl_ctx->inference_enabled ? 1 : 0, g_acl_ctx->model_path.c_str());
    return TD_SUCCESS;

fail:
    std::printf("[AI] acl adapter init failed: %s\n", g_acl_block_reason.c_str());
    ai_acl_adapter_close_context(g_acl_ctx);
    delete g_acl_ctx;
    g_acl_ctx = nullptr;
    return TD_FAILURE;
}

td_void ai_acl_adapter_shutdown(td_void)
{
    std::lock_guard<std::mutex> lock(g_acl_mutex);

    if (g_acl_ctx == nullptr) {
        return;
    }

    std::printf("[AI] acl adapter shutdown\n");
    ai_acl_adapter_close_context(g_acl_ctx);
    delete g_acl_ctx;
    g_acl_ctx = nullptr;
}

td_bool ai_acl_adapter_is_ready(td_void)
{
    std::lock_guard<std::mutex> lock(g_acl_mutex);
    return (g_acl_ctx != nullptr) ? TD_TRUE : TD_FALSE;
}

const td_char *ai_acl_adapter_backend_name(td_void)
{
    return "svp-acl-prep";
}

const td_char *ai_acl_adapter_block_reason(td_void)
{
    return g_acl_block_reason.c_str();
}

td_s32 ai_acl_adapter_validate_model(const td_char *model_path)
{
    std::lock_guard<std::mutex> lock(g_acl_mutex);
    AiAclAdapterContext test_model = {};
    std::string requested_path = safe_cstr(model_path);
    td_s32 ret;

    if (g_acl_ctx == nullptr || g_acl_ctx->inference_enabled == false) {
        set_block_reason_text("acl adapter is not ready");
        return TD_FAILURE;
    }

    if (requested_path.empty()) {
        set_block_reason_text("model path is empty");
        return TD_FAILURE;
    }

    if (svp_acl_rt_set_current_context(g_acl_ctx->context) != SVP_ACL_SUCCESS) {
        set_block_reason_text("set acl context failed");
        return TD_FAILURE;
    }

    /* Execute once with zero-filled input to reject unloadable or incompatible OMs before publication. */
    ret = om_model_init(&test_model, requested_path);
    if (ret == TD_SUCCESS) {
        ret = ai_acl_adapter_prepare_runtime_datasets(&test_model);
    }
    if (ret == TD_SUCCESS) {
        ret = ai_acl_adapter_self_test_execute(&test_model);
    }
    om_model_release(&test_model);
    return ret;
}

td_s32 ai_acl_adapter_switch_model(const td_char *model_path)
{
    std::lock_guard<std::mutex> lock(g_acl_mutex);
    AiAclAdapterContext new_model = {};
    std::string requested_path = safe_cstr(model_path);
    std::string old_path;

    if (g_acl_ctx == nullptr || g_acl_ctx->inference_enabled == false) {
        std::printf("[MODEL] load failed keep old model reason=acl adapter is not ready\n");
        return TD_FAILURE;
    }

    if (requested_path.empty()) {
        std::printf("[MODEL] load failed keep old model reason=model path is empty\n");
        return TD_FAILURE;
    }

    if (svp_acl_rt_set_current_context(g_acl_ctx->context) != SVP_ACL_SUCCESS) {
        std::printf("[MODEL] load failed keep old model reason=set acl context failed\n");
        return TD_FAILURE;
    }

    std::printf("[MODEL] load new model start path=%s\n", requested_path.c_str());

    /* Build all candidate resources first; failures leave active model pointers untouched. */
    if (om_model_init(&new_model, requested_path) != TD_SUCCESS) {
        std::printf("[MODEL] load failed keep old model reason=%s\n", g_acl_block_reason.c_str());
        return TD_FAILURE;
    }
    if (ai_acl_adapter_prepare_runtime_datasets(&new_model) != TD_SUCCESS ||
        ai_acl_adapter_self_test_execute(&new_model) != TD_SUCCESS) {
        std::printf("[MODEL] load failed keep old model reason=%s\n", g_acl_block_reason.c_str());
        om_model_release(&new_model);
        return TD_FAILURE;
    }

    std::printf("[MODEL] load new model ok\n");
    old_path = g_acl_ctx->model_path;

    /* Move old resources aside before installing the fully tested candidate. */
    AiAclAdapterContext old_model = {};
    old_model.model_loaded = g_acl_ctx->model_loaded;
    old_model.model_desc = g_acl_ctx->model_desc;
    old_model.model_mem_ptr = g_acl_ctx->model_mem_ptr;
    old_model.model_mem_size = g_acl_ctx->model_mem_size;
    old_model.model_id = g_acl_ctx->model_id;
    old_model.input_count = g_acl_ctx->input_count;
    old_model.output_count = g_acl_ctx->output_count;
    old_model.input_dataset = g_acl_ctx->input_dataset;
    old_model.output_dataset = g_acl_ctx->output_dataset;

    g_acl_ctx->model_path = requested_path;
    g_acl_ctx->model_loaded = new_model.model_loaded;
    g_acl_ctx->model_desc = new_model.model_desc;
    g_acl_ctx->model_mem_ptr = new_model.model_mem_ptr;
    g_acl_ctx->model_mem_size = new_model.model_mem_size;
    g_acl_ctx->model_id = new_model.model_id;
    g_acl_ctx->input_count = new_model.input_count;
    g_acl_ctx->output_count = new_model.output_count;
    g_acl_ctx->input_dataset = new_model.input_dataset;
    g_acl_ctx->output_dataset = new_model.output_dataset;

    new_model.model_loaded = false;
    new_model.model_desc = nullptr;
    new_model.model_mem_ptr = nullptr;
    new_model.model_mem_size = 0;
    new_model.model_id = 0;
    new_model.input_dataset = nullptr;
    new_model.output_dataset = nullptr;

    {
        std::lock_guard<std::mutex> det_lock(g_detection_mutex);
        g_acl_ctx->cached_detections.clear();
        g_acl_ctx->cached_frame_id = -AI_DET_HOLD_FRAMES - 1;
        g_acl_ctx->cached_frame_w = 0;
        g_acl_ctx->cached_frame_h = 0;
        g_acl_ctx->empty_infer_count = 0;
    }

    /* Release old resources only after the active context references the new model. */
    om_model_release(&old_model);
    g_acl_block_reason.clear();

    std::printf("[MODEL] switched current model=%s old=%s inputs=%zu outputs=%zu\n",
        g_acl_ctx->model_path.c_str(), old_path.c_str(), g_acl_ctx->input_count, g_acl_ctx->output_count);
    ai_acl_adapter_log_model_io(g_acl_ctx);
    return TD_SUCCESS;
}

td_s32 ai_acl_adapter_submit_frame(ai_acl_adapter_frame *frame)
{
    /* The model lock protects inference and hot switching; Dataset buffers are reused in place. */
    std::lock_guard<std::mutex> lock(g_acl_mutex);
    ai_acl_adapter_frame local_frame;
    AiPreprocessMeta preprocess_meta = {0};
    std::vector<AiDetection> detections;
    svp_acl_error ret;
    td_u64 total_start_ms = ai_acl_adapter_now_ms();
    td_u64 create_input_cost_ms;
    td_u64 create_output_cost_ms;
    td_u64 preprocess_cost_ms;
    td_u64 execute_cost_ms;
    td_u64 parse_cost_ms;
    td_u64 draw_cost_ms = 0;
    td_u64 t0_ms;

    if (frame == nullptr || frame->input_yuv420p == nullptr) {
        return TD_FAILURE;
    }

    if (g_acl_ctx == nullptr || g_acl_ctx->inference_enabled == false || g_acl_ctx->model_loaded == false) {
        return TD_FAILURE;
    }

    ret = svp_acl_rt_set_current_context(g_acl_ctx->context);
    if (ret != SVP_ACL_SUCCESS) {
        set_block_reason_text("svp_acl_rt_set_current_context realtime failed");
        return TD_FAILURE;
    }

    local_frame = *frame;
    local_frame.detection_count = 0;

    if (g_acl_ctx->input_dataset == nullptr || g_acl_ctx->output_dataset == nullptr) {
        set_block_reason_text("runtime dataset is not ready");
        return TD_FAILURE;
    }
    create_input_cost_ms = 0;
    create_output_cost_ms = 0;

    t0_ms = ai_acl_adapter_now_ms();
    if (ai_acl_adapter_fill_input_dataset(g_acl_ctx, g_acl_ctx->input_dataset,
        &local_frame, &preprocess_meta) != TD_SUCCESS) {
        return TD_FAILURE;
    }
    preprocess_cost_ms = ai_acl_adapter_now_ms() - t0_ms;

    t0_ms = ai_acl_adapter_now_ms();
    ret = svp_acl_mdl_execute(g_acl_ctx->model_id, g_acl_ctx->input_dataset, g_acl_ctx->output_dataset);
    execute_cost_ms = ai_acl_adapter_now_ms() - t0_ms;
    if (ret != SVP_ACL_SUCCESS) {
        set_block_reason_text("svp_acl_mdl_execute realtime failed");
        return TD_FAILURE;
    }

    t0_ms = ai_acl_adapter_now_ms();
    (td_void)ai_acl_adapter_parse_detections(g_acl_ctx, g_acl_ctx->output_dataset, &preprocess_meta,
        local_frame.width, local_frame.height, &detections);
    parse_cost_ms = ai_acl_adapter_now_ms() - t0_ms;
    if (!detections.empty()) {
        {
            std::lock_guard<std::mutex> det_lock(g_detection_mutex);
            g_acl_ctx->cached_detections = detections;
            g_acl_ctx->cached_frame_id = local_frame.frame_id;
            g_acl_ctx->cached_frame_w = local_frame.width;
            g_acl_ctx->cached_frame_h = local_frame.height;
            g_acl_ctx->empty_infer_count = 0;
        }
        ai_acl_adapter_log_detections(detections);
        t0_ms = ai_acl_adapter_now_ms();
        ai_acl_adapter_draw_detections(&local_frame, detections);
        draw_cost_ms = ai_acl_adapter_now_ms() - t0_ms;
        local_frame.detection_count = static_cast<td_s32>(detections.size());
    } else if (ai_acl_adapter_draw_cached_detections(g_acl_ctx, &local_frame) == TD_SUCCESS) {
        {
            std::lock_guard<std::mutex> det_lock(g_detection_mutex);
            g_acl_ctx->empty_infer_count++;
            if (g_acl_ctx->empty_infer_count >= AI_DET_CLEAR_EMPTY_INFER) {
                g_acl_ctx->cached_detections.clear();
            }
        }
    } else if (local_frame.output_yuv420p != nullptr && local_frame.output_yuv420p != local_frame.input_yuv420p) {
        td_u32 frame_size = local_frame.stride_y * local_frame.height + local_frame.stride_uv * local_frame.height / 2;
        std::memcpy(local_frame.output_yuv420p, local_frame.input_yuv420p, frame_size);
        local_frame.detection_count = 0;
    } else {
        local_frame.detection_count = 0;
    }
    frame->detection_count = local_frame.detection_count;

    {
        static td_s32 profile_count = 0;
        td_u64 total_cost_ms = ai_acl_adapter_now_ms() - total_start_ms;
        profile_count++;
        if (profile_count <= 5 || (profile_count % 10) == 0 || total_cost_ms > 500) {
            std::printf("[AI] profile frame=%d create_in=%llums create_out=%llums preprocess=%llums execute=%llums parse=%llums draw=%llums total=%llums det=%d\n",
                local_frame.frame_id,
                (unsigned long long)create_input_cost_ms,
                (unsigned long long)create_output_cost_ms,
                (unsigned long long)preprocess_cost_ms,
                (unsigned long long)execute_cost_ms,
                (unsigned long long)parse_cost_ms,
                (unsigned long long)draw_cost_ms,
                (unsigned long long)total_cost_ms,
                local_frame.detection_count);
        }
    }

    return TD_SUCCESS;
}

td_s32 ai_acl_adapter_overlay_cached_frame(ai_acl_adapter_frame *frame)
{
    if (frame == nullptr || frame->input_yuv420p == nullptr || frame->output_yuv420p == nullptr) {
        return TD_FAILURE;
    }

    if (g_acl_ctx == nullptr || g_acl_ctx->inference_enabled == false) {
        return TD_FAILURE;
    }

    if (ai_acl_adapter_draw_cached_detections(g_acl_ctx, frame) == TD_SUCCESS) {
        return TD_SUCCESS;
    }

    if (frame->output_yuv420p != frame->input_yuv420p) {
        td_u32 frame_size = frame->stride_y * frame->height + frame->stride_uv * frame->height / 2;
        std::memcpy(frame->output_yuv420p, frame->input_yuv420p, frame_size);
    }
    frame->detection_count = 0;
    return TD_SUCCESS;
}

td_s32 ai_acl_adapter_copy_cached_detections(const ai_acl_adapter_frame *frame,
    ai_acl_adapter_detection *detections, td_u32 max_detections, td_u32 *detection_count)
{
    std::vector<AiDetection> cached;

    if (detection_count != nullptr) {
        *detection_count = 0;
    }

    if (frame == nullptr || detections == nullptr || max_detections == 0 || detection_count == nullptr) {
        return TD_FAILURE;
    }

    {
        std::lock_guard<std::mutex> det_lock(g_detection_mutex);
        if (g_acl_ctx == nullptr || ai_acl_adapter_cached_detections_valid(g_acl_ctx, frame) != TD_TRUE) {
            return TD_FAILURE;
        }
        cached = g_acl_ctx->cached_detections;
    }

    td_u32 copied = 0;
    for (const AiDetection &det : cached) {
        if (copied >= max_detections) {
            break;
        }
        detections[copied].class_id = det.class_id;
        (td_void)snprintf(detections[copied].class_name, sizeof(detections[copied].class_name),
            "%s", det.class_name.c_str());
        detections[copied].confidence = det.confidence;
        detections[copied].x1 = det.x1;
        detections[copied].y1 = det.y1;
        detections[copied].x2 = det.x2;
        detections[copied].y2 = det.y2;
        copied++;
    }

    *detection_count = copied;
    return copied > 0 ? TD_SUCCESS : TD_FAILURE;
}

td_s32 ai_acl_adapter_get_frame(ai_acl_adapter_frame *frame)
{
    if (frame == nullptr || frame->output_yuv420p == nullptr) {
        return TD_FAILURE;
    }

    frame->detection_count = 0;
    return OT_ERR_VENC_NOT_SUPPORT;
}
