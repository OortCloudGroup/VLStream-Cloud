#include "ai_bridge.h"
#include "ai_acl_adapter.h"

#include <mutex>
#include <string>
#include <cstdio>

/*
 * This file provides a thin C/C++ ABI boundary. The C streaming pipeline
 * depends only on ai_bridge.h; model and ACL details remain in ai_acl_adapter.cpp.
 */
namespace {

struct AiBridgeContext {
    std::string model_path;
    std::string config_path;
    std::string classes_path;
    bool enabled;
};

std::mutex g_ai_mutex;
AiBridgeContext *g_ai_ctx = nullptr;

const char *ai_bridge_safe_string(const td_char *value)
{
    return (value != nullptr) ? value : "";
}

} // namespace

td_s32 ai_bridge_init(const ai_bridge_attr *attr)
{
    std::lock_guard<std::mutex> lock(g_ai_mutex);
    ai_acl_adapter_attr acl_attr = {0};

    if (g_ai_ctx != nullptr) {
        return TD_SUCCESS;
    }

    g_ai_ctx = new (std::nothrow) AiBridgeContext();
    if (g_ai_ctx == nullptr) {
        return TD_FAILURE;
    }

    if (attr != nullptr) {
        g_ai_ctx->model_path = ai_bridge_safe_string(attr->model_path);
        g_ai_ctx->config_path = ai_bridge_safe_string(attr->config_path);
        g_ai_ctx->classes_path = ai_bridge_safe_string(attr->classes_path);
        g_ai_ctx->enabled = (attr->enable_ai == TD_TRUE);
        acl_attr.model_path = attr->model_path;
        acl_attr.config_path = attr->config_path;
        acl_attr.classes_path = attr->classes_path;
        acl_attr.enable_inference = attr->enable_ai;
        acl_attr.enable_reporter = attr->enable_reporter;
        acl_attr.enable_model_receiver = attr->enable_model_receiver;
        acl_attr.device_id = 0;
    } else {
        g_ai_ctx->enabled = false;
    }

    if (ai_acl_adapter_init(&acl_attr) != TD_SUCCESS) {
        delete g_ai_ctx;
        g_ai_ctx = nullptr;
        return TD_FAILURE;
    }

    return TD_SUCCESS;
}

td_void ai_bridge_shutdown(td_void)
{
    std::lock_guard<std::mutex> lock(g_ai_mutex);

    if (g_ai_ctx == nullptr) {
        return;
    }

    ai_acl_adapter_shutdown();
    delete g_ai_ctx;
    g_ai_ctx = nullptr;
}

td_bool ai_bridge_is_ready(td_void)
{
    std::lock_guard<std::mutex> lock(g_ai_mutex);
    return (g_ai_ctx != nullptr && ai_acl_adapter_is_ready() == TD_TRUE) ? TD_TRUE : TD_FALSE;
}

const td_char *ai_bridge_backend_name(td_void)
{
    return ai_acl_adapter_backend_name();
}

const td_char *ai_bridge_block_reason(td_void)
{
    return ai_acl_adapter_block_reason();
}

td_s32 ai_bridge_submit_frame(ai_bridge_frame *frame)
{
    ai_acl_adapter_frame acl_frame = {0};

    if (frame == nullptr) {
        return TD_FAILURE;
    }

    acl_frame.input_yuv420p = frame->input_yuv420p;
    acl_frame.output_yuv420p = frame->output_yuv420p;
    acl_frame.width = frame->width;
    acl_frame.height = frame->height;
    acl_frame.stride_y = frame->stride_y;
    acl_frame.stride_uv = frame->stride_uv;
    acl_frame.input_is_yvu_semiplanar_420 = frame->input_is_yvu_semiplanar_420;
    acl_frame.frame_id = frame->frame_id;
    acl_frame.detection_count = frame->detection_count;

    if (ai_acl_adapter_submit_frame(&acl_frame) != TD_SUCCESS) {
        return TD_FAILURE;
    }

    frame->detection_count = acl_frame.detection_count;
    return TD_SUCCESS;
}

td_s32 ai_bridge_overlay_cached_frame(ai_bridge_frame *frame)
{
    ai_acl_adapter_frame acl_frame = {0};

    if (frame == nullptr) {
        return TD_FAILURE;
    }

    acl_frame.input_yuv420p = frame->input_yuv420p;
    acl_frame.output_yuv420p = frame->output_yuv420p;
    acl_frame.width = frame->width;
    acl_frame.height = frame->height;
    acl_frame.stride_y = frame->stride_y;
    acl_frame.stride_uv = frame->stride_uv;
    acl_frame.input_is_yvu_semiplanar_420 = frame->input_is_yvu_semiplanar_420;
    acl_frame.frame_id = frame->frame_id;
    acl_frame.detection_count = frame->detection_count;

    if (ai_acl_adapter_overlay_cached_frame(&acl_frame) != TD_SUCCESS) {
        return TD_FAILURE;
    }

    frame->detection_count = acl_frame.detection_count;
    return TD_SUCCESS;
}

td_s32 ai_bridge_copy_cached_detections(const ai_bridge_frame *frame,
    ai_bridge_detection *detections, td_u32 max_detections, td_u32 *detection_count)
{
    ai_acl_adapter_frame acl_frame = {0};
    ai_acl_adapter_detection acl_detections[32];
    td_u32 acl_count = 0;

    if (frame == nullptr || detections == nullptr || detection_count == nullptr || max_detections == 0) {
        return TD_FAILURE;
    }

    acl_frame.input_yuv420p = frame->input_yuv420p;
    acl_frame.output_yuv420p = frame->output_yuv420p;
    acl_frame.width = frame->width;
    acl_frame.height = frame->height;
    acl_frame.stride_y = frame->stride_y;
    acl_frame.stride_uv = frame->stride_uv;
    acl_frame.input_is_yvu_semiplanar_420 = frame->input_is_yvu_semiplanar_420;
    acl_frame.frame_id = frame->frame_id;
    acl_frame.detection_count = frame->detection_count;

    td_u32 local_max = max_detections;
    if (local_max > (td_u32)(sizeof(acl_detections) / sizeof(acl_detections[0]))) {
        local_max = (td_u32)(sizeof(acl_detections) / sizeof(acl_detections[0]));
    }

    if (ai_acl_adapter_copy_cached_detections(&acl_frame, acl_detections, local_max, &acl_count) != TD_SUCCESS) {
        *detection_count = 0;
        return TD_FAILURE;
    }

    for (td_u32 i = 0; i < acl_count; ++i) {
        detections[i].class_id = acl_detections[i].class_id;
        (td_void)snprintf(detections[i].class_name, sizeof(detections[i].class_name),
            "%s", acl_detections[i].class_name);
        detections[i].confidence = acl_detections[i].confidence;
        detections[i].x1 = acl_detections[i].x1;
        detections[i].y1 = acl_detections[i].y1;
        detections[i].x2 = acl_detections[i].x2;
        detections[i].y2 = acl_detections[i].y2;
    }

    *detection_count = acl_count;
    return TD_SUCCESS;
}

td_s32 ai_bridge_switch_model(const td_char *model_path)
{
    return ai_acl_adapter_switch_model(model_path);
}

td_s32 ai_bridge_validate_model(const td_char *model_path)
{
    return ai_acl_adapter_validate_model(model_path);
}
