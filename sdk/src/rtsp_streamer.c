/*
 * Hi3519DV500 + IMX335 RTSP streaming application
 *
 * Main pipeline:
 * - VI -> ISP -> VPSS -> VENC(H.264) -> RTSP server
 * - Dual VPSS channels: CHN0 for the main stream, CHN1 for AI overlays
 * - RTSP URL: rtsp://[board_ip]:554/live
 *
 * Thread model:
 * - The VENC thread drains H.264 and forwards it to RTSP/WebRTC.
 * - The AI sender owns VPSS CHN1 frames, overlays cached boxes, and feeds VENC1.
 * - The AI inference thread consumes a copied latest frame and never retains a VPSS frame.
 * - HTTP reporting and model reception run in separate background threads.
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <pthread.h>
#include <signal.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <errno.h>
#include <sys/time.h>

#include "sample_comm.h"
#include "ai_bridge.h"
#include "ai_runtime_config.h"
#include "http_reporter.h"
#include "model_receiver.h"
#include "rtsp_demo.h"
#include "webrtc_bridge.h"

#define RTSP_LOG_ERROR(fmt, ...) printf("[ERROR] " fmt "\n", ##__VA_ARGS__)
#define RTSP_LOG_WARN(fmt, ...)  printf("[WARN] " fmt "\n", ##__VA_ARGS__)
#define RTSP_LOG_INFO(fmt, ...)  printf("[INFO] " fmt "\n", ##__VA_ARGS__)

/*
 * Channel topology follows sample_venc.c: index 0 is the full-resolution main
 * channel, and index 1 is the external 1280x720 H.264 channel. RTSP and WebRTC
 * share the encoded output from VENC1.
 */
#define CHN_NUM_MAX 2
#define MAIN_STREAM_IDX 0
#define RTSP_STREAM_IDX 1
#define MAIN_VENC_CHN 0
#define RTSP_VENC_CHN 1
#define RTSP_PIC_SIZE PIC_720P
#define RTSP_FRAME_RATE 15
#define RTSP_GOP 30
#define IMX335_FULL_WIDTH 2592
#define IMX335_FULL_HEIGHT 1944

/* AI defaults apply only when video.conf omits the corresponding fields. */
#define AI_DEFAULT_MODEL_PATH "/mnt/ai_verify/yolov8.om"
#define AI_DEFAULT_CLASSES_PATH "/mnt/webrtc/model/classes.txt"
#define AI_VIDEO_CONFIG_PATH "/mnt/webrtc/video.conf"
#define AI_WEBCAM_CONFIG_PATH "/mnt/webrtc/webcam.conf"
#define AI_ENABLE_REALTIME_OVERLAY TD_TRUE
#define AI_INFER_FRAME_INTERVAL 10
#define AI_REPORT_MAX_DETECTIONS 32

/*
 * A single latest-frame slot connects the VPSS/VENC path to inference.
 * When inference falls behind capture, overwrite pending stale frames instead
 * of accumulating latency.
 */
typedef struct {
    ot_vpss_chn vpss_chn[CHN_NUM_MAX];
    ot_venc_chn venc_chn[CHN_NUM_MAX];
} sample_venc_vpss_chn;

/*
 * Global handles and started flags support unified cleanup. Join or stop a
 * resource only when its flag is set, because initialization may fail midway.
 */
static td_bool g_running = TD_TRUE;
static rtsp_demo_handle g_rtsp_server = NULL;
static rtsp_session_handle g_rtsp_session = NULL;
static pthread_t g_venc_thread;
static pthread_t g_dummy_venc_thread;
static pthread_t g_ai_probe_thread;
static pthread_t g_ai_infer_thread;
static td_bool g_venc_thread_started = TD_FALSE;
static td_bool g_dummy_venc_thread_started = TD_FALSE;
static td_bool g_ai_probe_thread_started = TD_FALSE;
static td_bool g_ai_infer_thread_started = TD_FALSE;
static td_bool g_ai_bridge_started = TD_FALSE;
static td_bool g_ai_manual_venc_path = TD_FALSE;
static td_bool g_webrtc_bridge_started = TD_FALSE;
static td_bool g_http_reporter_started = TD_FALSE;
static td_bool g_model_receiver_started = TD_FALSE;

#define VI_VB_YUV_CNT 6
#define VPSS_VB_YUV_CNT 8

/* Collect VB pool requirements for all sizes and formats before initializing MPP. */
typedef struct {
    td_u32 valid_num;
    td_u64 blk_size[OT_VB_MAX_COMMON_POOLS];
    td_u32 blk_cnt[OT_VB_MAX_COMMON_POOLS];
    td_u32 supplement_config;
} rtsp_streamer_vb_attr;

typedef struct {
    /* This mutex protects the buffer, frame metadata, and stop state together. */
    pthread_mutex_t mutex;
    pthread_cond_t cond;
    td_u8 *buffer;
    td_u32 buffer_size;
    td_u32 data_size;
    td_u32 width;
    td_u32 height;
    td_u32 stride_y;
    td_u32 stride_uv;
    td_bool input_is_yvu_semiplanar_420;
    td_s32 frame_id;
    td_bool has_frame;
    td_bool stop;
    td_u32 queued_count;
    td_u32 dropped_count;
} ai_async_frame_queue;

static ai_async_frame_queue g_ai_async_queue = {
    .mutex = PTHREAD_MUTEX_INITIALIZER,
    .cond = PTHREAD_COND_INITIALIZER,
    .buffer = NULL,
    .buffer_size = 0,
    .data_size = 0,
    .width = 0,
    .height = 0,
    .stride_y = 0,
    .stride_uv = 0,
    .input_is_yvu_semiplanar_420 = TD_TRUE,
    .frame_id = 0,
    .has_frame = TD_FALSE,
    .stop = TD_FALSE,
    .queued_count = 0,
    .dropped_count = 0,
};

static td_u64 rtsp_streamer_now_ms(td_void)
{
    struct timeval tv;
    gettimeofday(&tv, NULL);
    return (td_u64)tv.tv_sec * 1000 + (td_u64)tv.tv_usec / 1000;
}

static td_void rtsp_streamer_init_mem_share(td_void)
{
    td_u32 i;
    ot_vb_common_pools_id pools_id = {0};

    if (ss_mpi_vb_get_common_pool_id(&pools_id) != TD_SUCCESS) {
        RTSP_LOG_WARN("get common pool_id failed");
        return;
    }

    /* Allow MPP modules in this process to share physical blocks from common VB pools. */
    for (i = 0; i < pools_id.pool_cnt; ++i) {
        ss_mpi_vb_pool_share_all(pools_id.pool[i]);
    }
}

static td_void rtsp_streamer_update_vb_attr(rtsp_streamer_vb_attr *vb_attr, const ot_size *size,
    ot_pixel_format format, ot_compress_mode compress_mode, td_u32 blk_cnt)
{
    ot_pic_buf_attr pic_buf_attr = {0};

    if (vb_attr->valid_num >= OT_VB_MAX_COMMON_POOLS) {
        return;
    }

    pic_buf_attr.width = size->width;
    pic_buf_attr.height = size->height;
    pic_buf_attr.align = OT_DEFAULT_ALIGN;
    pic_buf_attr.bit_width = OT_DATA_BIT_WIDTH_8;
    pic_buf_attr.pixel_format = format;
    pic_buf_attr.compress_mode = compress_mode;

    vb_attr->blk_size[vb_attr->valid_num] = ot_common_get_pic_buf_size(&pic_buf_attr);
    vb_attr->blk_cnt[vb_attr->valid_num] = blk_cnt;
    vb_attr->valid_num++;
}

static td_void rtsp_streamer_get_vb_attr(const ot_size *vi_size, const ot_size *stream_size,
    rtsp_streamer_vb_attr *vb_attr)
{
    /* Use separate pools for VI 422, compressed full-size VPSS 420, and output-size uncompressed 420. */
    rtsp_streamer_update_vb_attr(vb_attr, vi_size, OT_PIXEL_FORMAT_YUV_SEMIPLANAR_422,
        OT_COMPRESS_MODE_NONE, VI_VB_YUV_CNT);
    rtsp_streamer_update_vb_attr(vb_attr, vi_size, OT_PIXEL_FORMAT_YVU_SEMIPLANAR_420,
        OT_COMPRESS_MODE_SEG_COMPACT, VPSS_VB_YUV_CNT);
    rtsp_streamer_update_vb_attr(vb_attr, stream_size, OT_PIXEL_FORMAT_YVU_SEMIPLANAR_420,
        OT_COMPRESS_MODE_NONE, VPSS_VB_YUV_CNT);
    vb_attr->supplement_config = OT_VB_SUPPLEMENT_JPEG_MASK | OT_VB_SUPPLEMENT_BNR_MOT_MASK;
}

static td_s32 rtsp_streamer_sys_init(const ot_size *vi_size, const ot_size *stream_size)
{
    td_u32 i;
    ot_vb_cfg vb_cfg = {0};
    rtsp_streamer_vb_attr vb_attr = {0};

    rtsp_streamer_get_vb_attr(vi_size, stream_size, &vb_attr);
    vb_cfg.max_pool_cnt = vb_attr.valid_num;
    for (i = 0; i < vb_attr.valid_num; ++i) {
        vb_cfg.common_pool[i].blk_size = vb_attr.blk_size[i];
        vb_cfg.common_pool[i].blk_cnt = vb_attr.blk_cnt[i];
    }

    /* VB must be ready before VI/VPSS/VENC because all later modules allocate from these pools. */
    if (sample_comm_sys_init_with_vb_supplement(&vb_cfg, vb_attr.supplement_config) != TD_SUCCESS) {
        return TD_FAILURE;
    }

    rtsp_streamer_init_mem_share();

    return TD_SUCCESS;
}

/* Process termination signals. */
static td_void sig_handler(td_s32 signo)
{
    if (SIGINT == signo || SIGTERM == signo) {
        RTSP_LOG_INFO("received signal %d, exiting", signo);
        g_running = TD_FALSE;
    }
}

#define IRCUT_GPIO_CLOSE_CHIP 2
#define IRCUT_GPIO_CLOSE_OFFSET 0
#define IRCUT_GPIO_OPEN_CHIP 5
#define IRCUT_GPIO_OPEN_OFFSET 1
#define IRCUT_GPIO_PINS_PER_CHIP 8
#define IRCUT_PULSE_US 1000000

/* GPIO sysfs helper; IR-CUT failures are logged but never block the video pipeline. */
static td_s32 rtsp_streamer_write_text_file(const char *path, const char *text)
{
    FILE *fp = fopen(path, "w");
    if (fp == NULL) {
        return TD_FAILURE;
    }

    if (fprintf(fp, "%s", text) < 0) {
        fclose(fp);
        return TD_FAILURE;
    }

    if (fclose(fp) != 0) {
        return TD_FAILURE;
    }

    return TD_SUCCESS;
}

static td_s32 rtsp_streamer_gpio_export(td_u32 gpio_num)
{
    char path[64];
    char gpio_text[16];

    snprintf(path, sizeof(path), "/sys/class/gpio/gpio%u/value", gpio_num);
    if (access(path, F_OK) == 0) {
        return TD_SUCCESS;
    }

    snprintf(gpio_text, sizeof(gpio_text), "%u", gpio_num);
    if (rtsp_streamer_write_text_file("/sys/class/gpio/export", gpio_text) != TD_SUCCESS) {
        RTSP_LOG_WARN("IR-CUT export gpio%u failed, errno=%d", gpio_num, errno);
        return TD_FAILURE;
    }

    return TD_SUCCESS;
}

static td_s32 rtsp_streamer_gpio_set_output(td_u32 gpio_num)
{
    char path[64];

    snprintf(path, sizeof(path), "/sys/class/gpio/gpio%u/direction", gpio_num);
    if (rtsp_streamer_write_text_file(path, "out") != TD_SUCCESS) {
        RTSP_LOG_WARN("IR-CUT set gpio%u direction failed, errno=%d", gpio_num, errno);
        return TD_FAILURE;
    }

    return TD_SUCCESS;
}

static td_s32 rtsp_streamer_gpio_write(td_u32 gpio_num, td_u32 value)
{
    char path[64];

    snprintf(path, sizeof(path), "/sys/class/gpio/gpio%u/value", gpio_num);
    if (rtsp_streamer_write_text_file(path, value ? "1" : "0") != TD_SUCCESS) {
        RTSP_LOG_WARN("IR-CUT write gpio%u=%u failed, errno=%d", gpio_num, value, errno);
        return TD_FAILURE;
    }

    return TD_SUCCESS;
}

static td_void rtsp_streamer_close_ircut(td_void)
{
    td_u32 close_gpio = IRCUT_GPIO_CLOSE_CHIP * IRCUT_GPIO_PINS_PER_CHIP + IRCUT_GPIO_CLOSE_OFFSET;
    td_u32 open_gpio = IRCUT_GPIO_OPEN_CHIP * IRCUT_GPIO_PINS_PER_CHIP + IRCUT_GPIO_OPEN_OFFSET;

    if (rtsp_streamer_gpio_export(close_gpio) != TD_SUCCESS ||
        rtsp_streamer_gpio_export(open_gpio) != TD_SUCCESS ||
        rtsp_streamer_gpio_set_output(close_gpio) != TD_SUCCESS ||
        rtsp_streamer_gpio_set_output(open_gpio) != TD_SUCCESS) {
        RTSP_LOG_WARN("IR-CUT close skipped; check sys_config.ko ir_auto pinmux and gpio sysfs");
        return;
    }

    /* The dual-coil actuator needs only a pulse; drive both lines low afterward. */
    rtsp_streamer_gpio_write(open_gpio, 0);
    rtsp_streamer_gpio_write(close_gpio, 1);
    usleep(IRCUT_PULSE_US);
    rtsp_streamer_gpio_write(open_gpio, 0);
    rtsp_streamer_gpio_write(close_gpio, 0);
}

static td_void rtsp_streamer_log_ai_file_status(const ai_bridge_attr *ai_attr)
{
    /* Diagnose resource files before ACL startup to distinguish configuration and load errors. */
    if (ai_attr == NULL || ai_attr->enable_ai != TD_TRUE) {
        RTSP_LOG_INFO("AI minimal verify disabled");
        return;
    }

    if (ai_attr->model_path == NULL || access(ai_attr->model_path, R_OK) != 0) {
        RTSP_LOG_WARN("AI model not ready: path=%s errno=%d",
            ai_attr != NULL && ai_attr->model_path != NULL ? ai_attr->model_path : "(null)", errno);
    } else {
        RTSP_LOG_INFO("AI model path ready: %s", ai_attr->model_path);
    }

    if (ai_attr->classes_path != NULL) {
        if (access(ai_attr->classes_path, R_OK) != 0) {
            RTSP_LOG_WARN("AI classes file missing: path=%s errno=%d",
                ai_attr->classes_path, errno);
        } else {
            RTSP_LOG_INFO("AI classes path ready: %s", ai_attr->classes_path);
        }
    }
}

/*
 * Extract H.264 SPS/PPS from VENC. On success, return a malloc-owned buffer;
 * the caller frees it after configuring the RTSP/WebRTC codec headers.
 */
static td_s32 get_sps_pps(ot_venc_chn venc_chn, td_u8 **sps_pps_buf, td_u32 *len)
{
    td_s32 ret;
    ot_venc_chn_status stat = {0};
    ot_venc_stream stream = {0};
    ot_venc_pack *pack = NULL;
    td_u8 *buf = NULL;
    td_u32 total_len = 0;
    td_u32 offset = 0;

    ret = ss_mpi_venc_query_status(venc_chn, &stat);
    if (ret != TD_SUCCESS) {
        RTSP_LOG_ERROR("ss_mpi_venc_query_status failed: 0x%x", ret);
        return ret;
    }

    if (stat.cur_packs == 0) {
        return TD_FAILURE;
    }

    stream.pack = (ot_venc_pack *)malloc(sizeof(ot_venc_pack) * stat.cur_packs);
    if (stream.pack == NULL) {
        return TD_FAILURE;
    }
    stream.pack_cnt = stat.cur_packs;

    /* Packs returned by get_stream remain VENC-owned and require release_stream after copying. */
    ret = ss_mpi_venc_get_stream(venc_chn, &stream, 1000);
    if (ret != TD_SUCCESS) {
        RTSP_LOG_ERROR("ss_mpi_venc_get_stream failed: 0x%x", ret);
        free(stream.pack);
        return ret;
    }

    /* Calculate the total SPS/PPS length. */
    for (td_u32 i = 0; i < stream.pack_cnt; i++) {
        pack = &stream.pack[i];
        if (pack->data_type.h264_type == OT_VENC_H264_NALU_SPS ||
            pack->data_type.h264_type == OT_VENC_H264_NALU_PPS) {
            total_len += pack->len - pack->offset;
        }
    }

    if (total_len == 0) {
        ss_mpi_venc_release_stream(venc_chn, &stream);
        free(stream.pack);
        return TD_FAILURE;
    }

    /* Allocate a contiguous output buffer. */
    buf = (td_u8 *)malloc(total_len);
    if (buf == NULL) {
        ss_mpi_venc_release_stream(venc_chn, &stream);
        free(stream.pack);
        return TD_FAILURE;
    }

    /* Coalesce separate stream packs into the contiguous buffer. */
    for (td_u32 i = 0; i < stream.pack_cnt; i++) {
        pack = &stream.pack[i];
        if (pack->data_type.h264_type == OT_VENC_H264_NALU_SPS ||
            pack->data_type.h264_type == OT_VENC_H264_NALU_PPS) {
            td_u32 pack_len = pack->len - pack->offset;
            memcpy(buf + offset, pack->addr + pack->offset, pack_len);
            offset += pack_len;
        }
    }

    ss_mpi_venc_release_stream(venc_chn, &stream);
    free(stream.pack);

    *sps_pps_buf = buf;
    *len = total_len;
    return TD_SUCCESS;
}

/* Drain VENC0 so VPSS CHN0 has a consumer, matching the sample_venc dual-stream model. */
static td_void *venc_dummy_drain_thread(td_void *arg)
{
    td_s32 ret;
    ot_venc_chn venc_chn = MAIN_VENC_CHN;
    ot_venc_chn_status stat;
    ot_venc_stream stream;
    fd_set read_fds;
    td_s32 venc_fd;
    struct timeval timeout;
    ot_unused(arg);

    venc_fd = ss_mpi_venc_get_fd(venc_chn);
    if (venc_fd < 0) {
        RTSP_LOG_ERROR("ss_mpi_venc_get_fd for VENC0 failed");
        return NULL;
    }

    /* Use a select timeout so the thread observes shutdown within one second. */
    while (g_running) {
        FD_ZERO(&read_fds);
        FD_SET(venc_fd, &read_fds);

        timeout.tv_sec = 1;
        timeout.tv_usec = 0;

        ret = select(venc_fd + 1, &read_fds, NULL, NULL, &timeout);
        if (ret < 0) {
            RTSP_LOG_ERROR("VENC0 dummy select failed");
            break;
        } else if (ret == 0 || !FD_ISSET(venc_fd, &read_fds)) {
            continue;
        }

        memset(&stat, 0, sizeof(stat));
        ret = ss_mpi_venc_query_status(venc_chn, &stat);
        if (ret != TD_SUCCESS || stat.cur_packs == 0) {
            continue;
        }

        memset(&stream, 0, sizeof(stream));
        stream.pack = (ot_venc_pack *)malloc(sizeof(ot_venc_pack) * stat.cur_packs);
        if (stream.pack == NULL) {
            continue;
        }
        stream.pack_cnt = stat.cur_packs;

        /* This channel is not published; drain and release it to prevent VENC0 backpressure. */
        ret = ss_mpi_venc_get_stream(venc_chn, &stream, 200);
        if (ret == TD_SUCCESS) {
            ss_mpi_venc_release_stream(venc_chn, &stream);
        }
        free(stream.pack);
    }
    return NULL;
}

/* Drain VENC1 and send frames to RTSP/WebRTC. */
static td_void *venc_rtsp_thread(td_void *arg)
{
    td_s32 ret;
    ot_venc_chn venc_chn = RTSP_VENC_CHN;
    ot_venc_chn_status stat;
    ot_venc_stream stream;
    ot_venc_pack *pack;
    td_u8 *frame_buf = NULL;
    td_u32 frame_len;
    td_u64 timestamp;
    fd_set read_fds;
    td_s32 venc_fd;
    struct timeval timeout;
    td_u32 frame_count = 0;
    venc_fd = ss_mpi_venc_get_fd(venc_chn);
    if (venc_fd < 0) {
        RTSP_LOG_ERROR("ss_mpi_venc_get_fd failed");
        return NULL;
    }

    /* This is the only VENC1 consumer; each encoded frame fans out to RTSP and WebRTC. */
    while (g_running) {
        FD_ZERO(&read_fds);
        FD_SET(venc_fd, &read_fds);

        timeout.tv_sec = 1;
        timeout.tv_usec = 0;

        ret = select(venc_fd + 1, &read_fds, NULL, NULL, &timeout);
        if (ret < 0) {
            RTSP_LOG_ERROR("select failed");
            break;
        } else if (ret == 0) {
            continue;
        }

        if (!FD_ISSET(venc_fd, &read_fds)) {
            continue;
        }

        memset(&stat, 0, sizeof(stat));
        ret = ss_mpi_venc_query_status(venc_chn, &stat);
        if (ret != TD_SUCCESS || stat.cur_packs == 0) {
            continue;
        }

        memset(&stream, 0, sizeof(stream));
        stream.pack = (ot_venc_pack *)malloc(sizeof(ot_venc_pack) * stat.cur_packs);
        if (stream.pack == NULL) {
            continue;
        }
        stream.pack_cnt = stat.cur_packs;

        /* Acquire one encoded frame. */
        ret = ss_mpi_venc_get_stream(venc_chn, &stream, 200);
        if (ret != TD_SUCCESS) {
            free(stream.pack);
            continue;
        }

        /* Calculate the combined length of all packs in this frame. */
        frame_len = 0;
        for (td_u32 i = 0; i < stream.pack_cnt; i++) {
            pack = &stream.pack[i];
            frame_len += (pack->len - pack->offset);
        }

        /* Allocate a contiguous frame buffer. */
        frame_buf = (td_u8 *)malloc(frame_len);
        if (frame_buf == NULL) {
            ss_mpi_venc_release_stream(venc_chn, &stream);
            free(stream.pack);
            continue;
        }

        /* Coalesce all packs for this frame. */
        td_u32 offset = 0;
        for (td_u32 i = 0; i < stream.pack_cnt; i++) {
            pack = &stream.pack[i];
            td_u32 pack_len = pack->len - pack->offset;
            memcpy(frame_buf + offset, pack->addr + pack->offset, pack_len);
            offset += pack_len;
        }

        /* Use the encoded-frame PTS as the transmission timestamp. */
        timestamp = stream.pack[0].pts;

        /* Send to RTSP. */
        if (g_rtsp_session) {
            ret = rtsp_tx_video(g_rtsp_session, frame_buf, frame_len, timestamp);
            if (ret < 0) {
                RTSP_LOG_WARN("rtsp_tx_video failed: %d", ret);
            }
        }

        /* WebRTC failures are logged without interrupting RTSP delivery. */
        if (g_webrtc_bridge_started == TD_TRUE) {
            ret = webrtc_bridge_input_video(frame_buf, frame_len);
            if (ret < 0) {
                printf("[WEBRTC] input video failed: %d\n", ret);
            }
        }

        frame_count++;
        if (frame_count % 30 == 0) {
            RTSP_LOG_INFO("Streamed %u frames", frame_count);
        }

        /* Free the contiguous copy after both sends, then return the original stream to VENC. */
        free(frame_buf);
        ss_mpi_venc_release_stream(venc_chn, &stream);
        free(stream.pack);
    }
    return NULL;
}

static td_u32 rtsp_streamer_get_frame_map_size(const ot_video_frame *video_frame)
{
    td_u32 y_size;
    td_u32 uv_size;

    /* CHN1 is YUV420SP: Y has full height and the interleaved UV/VU plane has half height. */
    y_size = video_frame->stride[0] * video_frame->height;
    uv_size = video_frame->stride[1] * video_frame->height / 2;
    return y_size + uv_size;
}

static td_void rtsp_streamer_ai_async_reset(td_void)
{
    /* Reset queue statistics and stop state before starting inference; allocate lazily. */
    pthread_mutex_lock(&g_ai_async_queue.mutex);
    g_ai_async_queue.has_frame = TD_FALSE;
    g_ai_async_queue.stop = TD_FALSE;
    g_ai_async_queue.queued_count = 0;
    g_ai_async_queue.dropped_count = 0;
    pthread_mutex_unlock(&g_ai_async_queue.mutex);
}

static td_void rtsp_streamer_ai_async_stop(td_void)
{
    /* Wake the inference worker so it can finish any pending frame and exit. */
    pthread_mutex_lock(&g_ai_async_queue.mutex);
    g_ai_async_queue.stop = TD_TRUE;
    pthread_cond_signal(&g_ai_async_queue.cond);
    pthread_mutex_unlock(&g_ai_async_queue.mutex);
}

static td_void rtsp_streamer_ai_async_release(td_void)
{
    /* Call only after joining inference so no consumer still reads the shared buffer. */
    pthread_mutex_lock(&g_ai_async_queue.mutex);
    free(g_ai_async_queue.buffer);
    g_ai_async_queue.buffer = NULL;
    g_ai_async_queue.buffer_size = 0;
    g_ai_async_queue.data_size = 0;
    g_ai_async_queue.has_frame = TD_FALSE;
    pthread_mutex_unlock(&g_ai_async_queue.mutex);
}

static td_s32 rtsp_streamer_ai_async_queue_frame(const td_u8 *src, const ot_video_frame *video_frame,
    td_s32 frame_id, td_u32 map_size)
{
    td_u8 *new_buffer;

    if (src == NULL || video_frame == NULL || map_size == 0) {
        return TD_FAILURE;
    }

    pthread_mutex_lock(&g_ai_async_queue.mutex);

    /* Grow but never shrink the buffer to avoid repeated allocation at a stable resolution. */
    if (g_ai_async_queue.buffer_size < map_size) {
        new_buffer = (td_u8 *)realloc(g_ai_async_queue.buffer, map_size);
        if (new_buffer == NULL) {
            pthread_mutex_unlock(&g_ai_async_queue.mutex);
            RTSP_LOG_WARN("AI async queue realloc failed: size=%u", map_size);
            return TD_FAILURE;
        }
        g_ai_async_queue.buffer = new_buffer;
        g_ai_async_queue.buffer_size = map_size;
    }

    /* Keep only the latest pending frame to prevent unbounded inference latency. */
    if (g_ai_async_queue.has_frame == TD_TRUE) {
        g_ai_async_queue.dropped_count++;
    }

    /* After copying, inference no longer depends on the VPSS frame. */
    memcpy(g_ai_async_queue.buffer, src, map_size);
    g_ai_async_queue.data_size = map_size;
    g_ai_async_queue.width = video_frame->width;
    g_ai_async_queue.height = video_frame->height;
    g_ai_async_queue.stride_y = video_frame->stride[0];
    g_ai_async_queue.stride_uv = video_frame->stride[1];
    g_ai_async_queue.input_is_yvu_semiplanar_420 = TD_TRUE;
    g_ai_async_queue.frame_id = frame_id;
    g_ai_async_queue.has_frame = TD_TRUE;
    g_ai_async_queue.queued_count++;
    pthread_cond_signal(&g_ai_async_queue.cond);

    if (g_ai_async_queue.queued_count <= 3 || (g_ai_async_queue.queued_count % 30) == 0) {
        RTSP_LOG_INFO("AI async queued frame_id=%d, queued=%u, overwritten=%u",
            frame_id, g_ai_async_queue.queued_count, g_ai_async_queue.dropped_count);
    }

    pthread_mutex_unlock(&g_ai_async_queue.mutex);
    return TD_SUCCESS;
}

static td_void *ai_infer_worker_thread(td_void *arg)
{
    td_u8 *local_buffer = NULL;
    td_u32 local_buffer_size = 0;
    td_u32 infer_count = 0;

    ot_unused(arg);

    /* This worker only runs inference and updates the adapter cache; it never touches VPSS/VENC. */
    while (TD_TRUE) {
        ai_bridge_frame ai_frame = {0};
        td_u32 data_size;
        td_s32 ret;

        pthread_mutex_lock(&g_ai_async_queue.mutex);
        while (g_ai_async_queue.has_frame != TD_TRUE && g_ai_async_queue.stop != TD_TRUE) {
            pthread_cond_wait(&g_ai_async_queue.cond, &g_ai_async_queue.mutex);
        }

        if (g_ai_async_queue.stop == TD_TRUE && g_ai_async_queue.has_frame != TD_TRUE) {
            pthread_mutex_unlock(&g_ai_async_queue.mutex);
            break;
        }

        data_size = g_ai_async_queue.data_size;
        if (local_buffer_size < data_size) {
            td_u8 *new_buffer = (td_u8 *)realloc(local_buffer, data_size);
            if (new_buffer == NULL) {
                g_ai_async_queue.has_frame = TD_FALSE;
                pthread_mutex_unlock(&g_ai_async_queue.mutex);
                RTSP_LOG_WARN("AI async worker realloc failed: size=%u", data_size);
                usleep(100000);
                continue;
            }
            local_buffer = new_buffer;
            local_buffer_size = data_size;
        }

        /* Copy under the lock, then unlock before inference to avoid blocking the producer. */
        memcpy(local_buffer, g_ai_async_queue.buffer, data_size);
        ai_frame.input_yuv420p = local_buffer;
        ai_frame.output_yuv420p = local_buffer;
        ai_frame.width = g_ai_async_queue.width;
        ai_frame.height = g_ai_async_queue.height;
        ai_frame.stride_y = g_ai_async_queue.stride_y;
        ai_frame.stride_uv = g_ai_async_queue.stride_uv;
        ai_frame.input_is_yvu_semiplanar_420 = g_ai_async_queue.input_is_yvu_semiplanar_420;
        ai_frame.frame_id = g_ai_async_queue.frame_id;
        ai_frame.detection_count = 0;
        g_ai_async_queue.has_frame = TD_FALSE;
        pthread_mutex_unlock(&g_ai_async_queue.mutex);

        td_u64 infer_start_ms = rtsp_streamer_now_ms();
        /* submit_frame runs ACL synchronously but blocks only this worker, not video delivery. */
        ret = ai_bridge_submit_frame(&ai_frame);
        td_u64 infer_cost_ms = rtsp_streamer_now_ms() - infer_start_ms;
        infer_count++;
        if (ret == TD_SUCCESS) {
            if (infer_count <= 3 || (infer_count % 10) == 0 || infer_cost_ms > 300) {
                RTSP_LOG_INFO("AI async infer frame_id=%d, detections=%d, total=%u, cost=%llums",
                    ai_frame.frame_id, ai_frame.detection_count, infer_count,
                    (unsigned long long)infer_cost_ms);
            }
        } else if (infer_count <= 3) {
            RTSP_LOG_WARN("AI async infer failed: frame_id=%d, reason=%s",
                ai_frame.frame_id, ai_bridge_block_reason());
        }
    }

    free(local_buffer);
    return NULL;
}

static td_void *ai_venc_send_thread(td_void *arg)
{
    ot_vpss_grp vpss_grp = 0;
    ot_vpss_chn vpss_chn = RTSP_STREAM_IDX;
    ot_venc_chn venc_chn = RTSP_VENC_CHN;
    td_s32 frame_id = 0;
    td_s32 ret;
    ot_video_frame_info frame_info;

    ot_unused(arg);

    /*
     * With AI enabled, this thread performs VPSS1 -> overlay -> VENC1 manually.
     * Do not also bind VPSS1 to VENC1, which would create two producers.
     */
    while (g_running == TD_TRUE) {
        td_u8 *virt_addr;
        td_u32 map_size;
        ai_bridge_frame ai_frame = {0};
        td_bool ai_ok = TD_FALSE;
        td_bool ai_due = TD_FALSE;

        if (g_ai_bridge_started != TD_TRUE || ai_bridge_is_ready() != TD_TRUE) {
            usleep(200000);
            continue;
        }

        memset(&frame_info, 0, sizeof(frame_info));
        td_u64 loop_start_ms = rtsp_streamer_now_ms();
        td_u64 t0_ms = loop_start_ms;
        td_u64 get_cost_ms;
        td_u64 map_cost_ms;
        td_u64 queue_cost_ms = 0;
        td_u64 overlay_cost_ms;
        td_u64 venc_cost_ms;

        /* Frame acquisition and release must pair; VPSS owns the physical frame until release. */
        ret = ss_mpi_vpss_get_chn_frame(vpss_grp, vpss_chn, &frame_info, 1000);
        get_cost_ms = rtsp_streamer_now_ms() - t0_ms;
        if (ret != TD_SUCCESS) {
            usleep(100000);
            continue;
        }

        map_size = rtsp_streamer_get_frame_map_size(&frame_info.video_frame);
        t0_ms = rtsp_streamer_now_ms();
        /* Map the physical frame before the CPU copies input or draws in place on YUV. */
        virt_addr = (td_u8 *)ss_mpi_sys_mmap(frame_info.video_frame.phys_addr[0], map_size);
        map_cost_ms = rtsp_streamer_now_ms() - t0_ms;
        if (virt_addr == NULL) {
            RTSP_LOG_WARN("AI probe mmap failed for frame %d", frame_id);
            (td_void)ss_mpi_vpss_release_chn_frame(vpss_grp, vpss_chn, &frame_info);
            usleep(100000);
            continue;
        }

        ai_frame.input_yuv420p = virt_addr;
        ai_frame.output_yuv420p = virt_addr;
        ai_frame.width = frame_info.video_frame.width;
        ai_frame.height = frame_info.video_frame.height;
        ai_frame.stride_y = frame_info.video_frame.stride[0];
        ai_frame.stride_uv = frame_info.video_frame.stride[1];
        ai_frame.input_is_yvu_semiplanar_420 = TD_TRUE;
        ai_frame.frame_id = frame_id++;
        ai_frame.detection_count = 0;

        /* Sample inference by interval and overlay still-valid cached boxes on intermediate frames. */
        ai_due = ((AI_INFER_FRAME_INTERVAL <= 1) ||
            ((ai_frame.frame_id % AI_INFER_FRAME_INTERVAL) == 0)) ? TD_TRUE : TD_FALSE;
        if (ai_due == TD_TRUE) {
            t0_ms = rtsp_streamer_now_ms();
            ret = rtsp_streamer_ai_async_queue_frame(virt_addr, &frame_info.video_frame,
                ai_frame.frame_id, map_size);
            queue_cost_ms = rtsp_streamer_now_ms() - t0_ms;
            if (ret != TD_SUCCESS && (ai_frame.frame_id % 30) == 0) {
                RTSP_LOG_WARN("AI async queue failed, raw frame will still be encoded");
            }
        }

        t0_ms = rtsp_streamer_now_ms();
        /* Draw in place on the mapped VPSS frame before feeding it to VENC1. */
        ret = ai_bridge_overlay_cached_frame(&ai_frame);
        overlay_cost_ms = rtsp_streamer_now_ms() - t0_ms;
        if (ret == TD_SUCCESS && ai_frame.detection_count > 0) {
            ai_ok = TD_TRUE;
        }

        /* Report only valid overlays; the snapshot comes from the already annotated YUV frame. */
        if (ai_ok == TD_TRUE) {
            ai_bridge_detection bridge_detections[AI_REPORT_MAX_DETECTIONS];
            http_reporter_detection report_detections[AI_REPORT_MAX_DETECTIONS];
            td_u32 detection_count = 0;

            ret = ai_bridge_copy_cached_detections(&ai_frame, bridge_detections,
                AI_REPORT_MAX_DETECTIONS, &detection_count);
            if (ret == TD_SUCCESS && detection_count > 0) {
                td_u32 i;
                for (i = 0; i < detection_count; ++i) {
                    report_detections[i].class_id = bridge_detections[i].class_id;
                    snprintf(report_detections[i].class_name, sizeof(report_detections[i].class_name),
                        "%s", bridge_detections[i].class_name);
                    report_detections[i].confidence = bridge_detections[i].confidence;
                    report_detections[i].x1 = bridge_detections[i].x1;
                    report_detections[i].y1 = bridge_detections[i].y1;
                    report_detections[i].x2 = bridge_detections[i].x2;
                    report_detections[i].y2 = bridge_detections[i].y2;
                }
                /* submit() copies the image, so this loop may unmap and release the VPSS frame. */
                http_reporter_submit(ai_frame.frame_id, ai_frame.width, ai_frame.height,
                    ai_frame.stride_y, ai_frame.stride_uv, ai_frame.input_is_yvu_semiplanar_420,
                    virt_addr, report_detections, detection_count);
            }
        }

        t0_ms = rtsp_streamer_now_ms();
        /* Unmap and return the physical frame only after VENC accepts it. */
        ret = ss_mpi_venc_send_frame(venc_chn, &frame_info, 1000);
        venc_cost_ms = rtsp_streamer_now_ms() - t0_ms;
        if (ret != TD_SUCCESS) {
            RTSP_LOG_WARN("ss_mpi_venc_send_frame failed: 0x%x", ret);
        } else if (ai_due == TD_TRUE && ai_ok == TD_FALSE && (frame_id % 30) == 0) {
            RTSP_LOG_INFO("async raw frame sent to VENC1, frame_id=%d", ai_frame.frame_id);
        }

        if ((ai_frame.frame_id % 30) == 0 || overlay_cost_ms > 80 || venc_cost_ms > 80 ||
            (rtsp_streamer_now_ms() - loop_start_ms) > 120) {
            RTSP_LOG_INFO("AI send profile frame=%d get=%llums mmap=%llums queue=%llums overlay=%llums venc=%llums total=%llums det=%d",
                ai_frame.frame_id,
                (unsigned long long)get_cost_ms,
                (unsigned long long)map_cost_ms,
                (unsigned long long)queue_cost_ms,
                (unsigned long long)overlay_cost_ms,
                (unsigned long long)venc_cost_ms,
                (unsigned long long)(rtsp_streamer_now_ms() - loop_start_ms),
                ai_frame.detection_count);
        }

        /* Unmap and release in the exact reverse order of map and acquisition. */
        (td_void)ss_mpi_sys_munmap(virt_addr, map_size);
        (td_void)ss_mpi_vpss_release_chn_frame(vpss_grp, vpss_chn, &frame_info);
    }

    return NULL;
}

/* Application entry point. */
td_s32 main(td_s32 argc, td_char *argv[])
{
    td_s32 ret;
    sample_vi_cfg vi_cfg;
    sample_vpss_cfg vpss_cfg;
    ot_vpss_grp vpss_grp = 0;
    ot_venc_chn main_venc_chn = MAIN_VENC_CHN;
    ot_venc_chn rtsp_venc_chn = RTSP_VENC_CHN;
    const td_s32 stream_idx = RTSP_STREAM_IDX; /* The external H.264 stream uses sample_venc channel 1. */
    ot_size vi_size;
    ot_size enc_size;
    sample_venc_vpss_chn venc_vpss_chn;
    sample_comm_venc_chn_param main_chn_param;
    sample_comm_venc_chn_param rtsp_chn_param;
    ot_venc_gop_attr gop_attr;
    sample_sns_type sns_type = SENSOR0_TYPE;
    td_u8 *sps_pps_buf = NULL;
    td_u32 sps_pps_len = 0;
    td_u16 rtsp_port = 554;
    ot_pic_size pic_size = RTSP_PIC_SIZE;
    ot_vi_pipe active_vi_pipe = 0;
    /* Start from bootable defaults, then override AI and model-receiver options from video.conf. */
    ai_bridge_attr ai_attr = {
        .model_path = AI_DEFAULT_MODEL_PATH,
        .config_path = NULL,
        .classes_path = AI_DEFAULT_CLASSES_PATH,
        .enable_ai = TD_TRUE,
        .enable_reporter = TD_TRUE,
        .enable_model_receiver = TD_FALSE,
    };
    model_receiver_attr model_rx_attr = {
        .enabled = TD_FALSE,
        .listen_host = "0.0.0.0",
        .listen_port = 8888,
        .model_path = AI_DEFAULT_MODEL_PATH,
        .webcam_config_path = AI_WEBCAM_CONFIG_PATH,
        .validate_model = ai_bridge_validate_model,
        .switch_model = ai_bridge_switch_model,
    };
    ai_runtime_config runtime_cfg = {
        .enable_ai = TD_TRUE,
        .model_path = AI_DEFAULT_MODEL_PATH,
        .classes_path = AI_DEFAULT_CLASSES_PATH,
    };

    /* Disable AI on invalid configuration while allowing the base video and RTSP path to start. */
    if (ai_runtime_config_load(&runtime_cfg, AI_VIDEO_CONFIG_PATH) != TD_SUCCESS) {
        RTSP_LOG_WARN("AI runtime config invalid, AI disabled");
        runtime_cfg.enable_ai = TD_FALSE;
    }
    model_receiver_load_config(&model_rx_attr, AI_VIDEO_CONFIG_PATH);
    ai_attr.model_path = runtime_cfg.model_path;
    ai_attr.classes_path = runtime_cfg.classes_path;
    ai_attr.enable_ai = runtime_cfg.enable_ai;
    ai_attr.enable_model_receiver = model_rx_attr.enabled;

    RTSP_LOG_INFO("starting rtsp_streamer, H.264 %dx%d@%dfps, RTSP port %u",
        1280, 720, RTSP_FRAME_RATE, rtsp_port);

    /* Register termination signals. */
    signal(SIGINT, sig_handler);
    signal(SIGTERM, sig_handler);

    /* Initialize the MPP system. */
    ret = sample_comm_sys_get_pic_size(pic_size, &enc_size);
    if (ret != TD_SUCCESS) {
        RTSP_LOG_ERROR("sample_comm_sys_get_pic_size failed: 0x%x", ret);
        goto exit;
    }

    /* Force full IMX335 resolution to avoid a mismatch with sample defaults. */
    sample_comm_vi_get_size_by_sns_type(sns_type, &vi_size);
    vi_size.width = IMX335_FULL_WIDTH;
    vi_size.height = IMX335_FULL_HEIGHT;

    ret = rtsp_streamer_sys_init(&vi_size, &enc_size);
    if (ret != TD_SUCCESS) {
        RTSP_LOG_ERROR("rtsp_streamer_sys_init failed: 0x%x", ret);
        goto exit;
    }

    /* Move the physical IR-CUT filter into the optical path before starting the sensor. */
    rtsp_streamer_close_ircut();

    /* Start VI once and keep it running; debug probes must not gate pipeline startup. */
    sample_comm_vi_get_default_vi_cfg(sns_type, &vi_cfg);

    /* VI, MIPI, pipe, channel, and ISP windows must use the same input size. */
    vi_cfg.dev_info.dev_attr.in_size.width = IMX335_FULL_WIDTH;
    vi_cfg.dev_info.dev_attr.in_size.height = IMX335_FULL_HEIGHT;
    vi_cfg.mipi_info.combo_dev_attr.img_rect.x = 0;
    vi_cfg.mipi_info.combo_dev_attr.img_rect.y = 0;
    vi_cfg.mipi_info.combo_dev_attr.img_rect.width = IMX335_FULL_WIDTH;
    vi_cfg.mipi_info.combo_dev_attr.img_rect.height = IMX335_FULL_HEIGHT;
    vi_cfg.pipe_info[0].pipe_attr.size.width = IMX335_FULL_WIDTH;
    vi_cfg.pipe_info[0].pipe_attr.size.height = IMX335_FULL_HEIGHT;
    vi_cfg.pipe_info[0].chn_info[0].chn_attr.size.width = IMX335_FULL_WIDTH;
    vi_cfg.pipe_info[0].chn_info[0].chn_attr.size.height = IMX335_FULL_HEIGHT;
    vi_cfg.grp_info.fusion_grp_attr[0].cache_line = IMX335_FULL_HEIGHT;
    vi_cfg.pipe_info[0].isp_info.isp_pub_attr.wnd_rect.x = 0;
    vi_cfg.pipe_info[0].isp_info.isp_pub_attr.wnd_rect.y = 0;
    vi_cfg.pipe_info[0].isp_info.isp_pub_attr.wnd_rect.width = IMX335_FULL_WIDTH;
    vi_cfg.pipe_info[0].isp_info.isp_pub_attr.wnd_rect.height = IMX335_FULL_HEIGHT;
    vi_cfg.pipe_info[0].isp_info.isp_pub_attr.sns_size.width = IMX335_FULL_WIDTH;
    vi_cfg.pipe_info[0].isp_info.isp_pub_attr.sns_size.height = IMX335_FULL_HEIGHT;
    vi_cfg.pipe_info[0].isp_info.isp_pub_attr.mipi_crop_attr.mipi_crop_en = TD_FALSE;

    ret = sample_comm_vi_start_vi(&vi_cfg);
    if (ret != TD_SUCCESS) {
        RTSP_LOG_ERROR("sample_comm_vi_start_vi failed: 0x%x", ret);
        goto sys_exit;
    }

    active_vi_pipe = vi_cfg.bind_pipe.pipe_id[0];

    /* Start dual-channel VPSS using attributes aligned with sample_venc defaults. */
    sample_comm_vpss_get_default_vpss_cfg(&vpss_cfg, OT_FMU_MODE_OFF);
    vpss_cfg.grp_attr.max_width = vi_size.width;
    vpss_cfg.grp_attr.max_height = vi_size.height;
    vpss_cfg.grp_attr.dei_mode = OT_VPSS_DEI_MODE_OFF;
    vpss_cfg.grp_attr.pixel_format = OT_PIXEL_FORMAT_YVU_SEMIPLANAR_420;
    vpss_cfg.grp_attr.frame_rate.src_frame_rate = -1;
    vpss_cfg.grp_attr.frame_rate.dst_frame_rate = -1;

    /*
     * CHN0 is the full-size compressed main channel from sample_venc.
     * depth=0 means the application does not acquire frames; binding feeds VENC0.
     */
    vpss_cfg.chn_en[0] = TD_TRUE;
    vpss_cfg.chn_attr[0].width = vi_size.width;
    vpss_cfg.chn_attr[0].height = vi_size.height;
    vpss_cfg.chn_attr[0].chn_mode = OT_VPSS_CHN_MODE_USER;
    vpss_cfg.chn_attr[0].compress_mode = OT_COMPRESS_MODE_SEG_COMPACT;
    vpss_cfg.chn_attr[0].pixel_format = OT_PIXEL_FORMAT_YVU_SEMIPLANAR_420;
    vpss_cfg.chn_attr[0].frame_rate.src_frame_rate = -1;
    vpss_cfg.chn_attr[0].frame_rate.dst_frame_rate = -1;
    vpss_cfg.chn_attr[0].depth = 0;
    vpss_cfg.chn_attr[0].mirror_en = TD_FALSE;
    vpss_cfg.chn_attr[0].flip_en = TD_FALSE;
    vpss_cfg.chn_attr[0].aspect_ratio.mode = OT_ASPECT_RATIO_NONE;

    /*
     * CHN1 is the uncompressed 1280x720 AI overlay channel. depth=1 allows
     * application frame acquisition; without AI, direct binding feeds VENC1.
     */
    vpss_cfg.chn_en[1] = TD_TRUE;
    vpss_cfg.chn_attr[1].width = enc_size.width;
    vpss_cfg.chn_attr[1].height = enc_size.height;
    vpss_cfg.chn_attr[1].chn_mode = OT_VPSS_CHN_MODE_USER;
    vpss_cfg.chn_attr[1].compress_mode = OT_COMPRESS_MODE_NONE;
    vpss_cfg.chn_attr[1].pixel_format = OT_PIXEL_FORMAT_YVU_SEMIPLANAR_420;
    vpss_cfg.chn_attr[1].frame_rate.src_frame_rate = -1;
    vpss_cfg.chn_attr[1].frame_rate.dst_frame_rate = -1;
    vpss_cfg.chn_attr[1].depth = 1;
    vpss_cfg.chn_attr[1].mirror_en = TD_FALSE;
    vpss_cfg.chn_attr[1].flip_en = TD_FALSE;
    vpss_cfg.chn_attr[1].aspect_ratio.mode = OT_ASPECT_RATIO_NONE;

    /* sample_common_vpss_start uses a separate channel attribute structure. */
    sample_vpss_chn_attr vpss_chn_attr = {0};
    memcpy_s(&vpss_chn_attr.chn_attr[0], sizeof(ot_vpss_chn_attr) * OT_VPSS_MAX_PHYS_CHN_NUM,
        vpss_cfg.chn_attr, sizeof(ot_vpss_chn_attr) * OT_VPSS_MAX_PHYS_CHN_NUM);
    memcpy_s(vpss_chn_attr.chn_enable, sizeof(vpss_chn_attr.chn_enable),
        vpss_cfg.chn_en, sizeof(vpss_chn_attr.chn_enable));
    vpss_chn_attr.chn_array_size = OT_VPSS_MAX_PHYS_CHN_NUM;

    ret = sample_common_vpss_start(vpss_grp, &vpss_cfg.grp_attr, &vpss_chn_attr);
    if (ret != TD_SUCCESS) {
        RTSP_LOG_ERROR("sample_common_vpss_start failed: 0x%x", ret);
        goto vi_stop;
    }

    /* Bind VI to VPSS. */
    ret = sample_comm_vi_bind_vpss(active_vi_pipe, 0, vpss_grp, 0);
    if (ret != TD_SUCCESS) {
        RTSP_LOG_ERROR("sample_comm_vi_bind_vpss failed: 0x%x", ret);
        goto vpss_stop;
    }

    /* Start VENC0 and VENC1 to match the sample_venc dual-stream consumer model. */
    venc_vpss_chn.vpss_chn[MAIN_STREAM_IDX] = MAIN_STREAM_IDX;
    venc_vpss_chn.venc_chn[MAIN_STREAM_IDX] = main_venc_chn;
    venc_vpss_chn.vpss_chn[stream_idx] = stream_idx;
    venc_vpss_chn.venc_chn[stream_idx] = rtsp_venc_chn;

    ret = sample_comm_venc_get_gop_attr(OT_VENC_GOP_MODE_NORMAL_P, &gop_attr);
    if (ret != TD_SUCCESS) {
        RTSP_LOG_ERROR("sample_comm_venc_get_gop_attr failed: 0x%x", ret);
        goto vpss_unbind;
    }

    /* VENC0 encodes full-size H.265 only to consume the main channel; RTSP does not use it. */
    memset(&main_chn_param, 0, sizeof(main_chn_param));
    main_chn_param.frame_rate = 30;
    main_chn_param.gop = 60;
    main_chn_param.stats_time = 2;
    main_chn_param.gop_attr = gop_attr;
    main_chn_param.type = OT_PT_H265;
    main_chn_param.size = sample_comm_sys_get_pic_enum(&vi_size);
    main_chn_param.rc_mode = SAMPLE_RC_CBR;
    main_chn_param.profile = 0;
    main_chn_param.is_rcn_ref_share_buf = TD_TRUE;

    ret = sample_comm_venc_start(main_venc_chn, &main_chn_param);
    if (ret != TD_SUCCESS) {
        RTSP_LOG_ERROR("sample_comm_venc_start VENC0 failed: 0x%x", ret);
        goto vpss_unbind;
    }

    ret = sample_comm_vpss_bind_venc(vpss_grp, venc_vpss_chn.vpss_chn[MAIN_STREAM_IDX],
        venc_vpss_chn.venc_chn[MAIN_STREAM_IDX]);
    if (ret != TD_SUCCESS) {
        RTSP_LOG_ERROR("sample_comm_vpss_bind_venc VENC0 failed: 0x%x", ret);
        goto main_venc_stop;
    }

    /* Failing to drain VENC0 creates backpressure that eventually stalls shared VI/VPSS. */
    ret = pthread_create(&g_dummy_venc_thread, NULL, venc_dummy_drain_thread, NULL);
    if (ret != 0) {
        RTSP_LOG_ERROR("pthread_create VENC0 dummy drain failed: %d", ret);
        goto main_venc_unbind;
    }
    g_dummy_venc_thread_started = TD_TRUE;

    /* VENC1 produces 720p H.264 for RTSP, WebRTC, and client keyframe requests. */
    memset(&rtsp_chn_param, 0, sizeof(rtsp_chn_param));
    rtsp_chn_param.frame_rate = RTSP_FRAME_RATE;
    rtsp_chn_param.gop = RTSP_GOP;
    rtsp_chn_param.stats_time = 2;
    rtsp_chn_param.gop_attr = gop_attr;
    rtsp_chn_param.type = OT_PT_H264;
    rtsp_chn_param.size = sample_comm_sys_get_pic_enum(&enc_size);
    rtsp_chn_param.rc_mode = SAMPLE_RC_CBR;
    rtsp_chn_param.profile = 0;
    rtsp_chn_param.is_rcn_ref_share_buf = TD_TRUE;

    ret = sample_comm_venc_start(rtsp_venc_chn, &rtsp_chn_param);
    if (ret != TD_SUCCESS) {
        RTSP_LOG_ERROR("sample_comm_venc_start VENC1 failed: 0x%x", ret);
        goto dummy_join;
    }

    /* Reporting owns a background worker; startup or network failure must not block media. */
    http_reporter_load_config();
    ai_attr.enable_reporter = http_reporter_is_enabled() ? TD_TRUE : TD_FALSE;
    if (http_reporter_is_enabled()) {
        http_reporter_start();
        g_http_reporter_started = TD_TRUE;
    }

    /* Switch to manual application frame delivery only after model and worker startup succeed. */
    if (AI_ENABLE_REALTIME_OVERLAY == TD_TRUE && ai_attr.enable_ai == TD_TRUE) {
        rtsp_streamer_log_ai_file_status(&ai_attr);

        ret = ai_bridge_init(&ai_attr);
        if (ret == TD_SUCCESS) {
            g_ai_bridge_started = TD_TRUE;
            RTSP_LOG_INFO("AI bridge lifecycle initialized, backend=%s, ready=%d",
                ai_bridge_backend_name(), ai_bridge_is_ready() == TD_TRUE ? 1 : 0);
            if (ai_bridge_block_reason()[0] != '\0') {
                RTSP_LOG_WARN("AI bridge inactive: %s", ai_bridge_block_reason());
            }

            /* Model reception depends on AI validate/switch callbacks, so start it after the bridge. */
            if (model_rx_attr.enabled == TD_TRUE) {
                if (model_receiver_start(&model_rx_attr) == TD_SUCCESS) {
                    g_model_receiver_started = TD_TRUE;
                } else {
                    RTSP_LOG_WARN("model receiver start failed, stream path remains unchanged");
                }
            }
        } else {
            RTSP_LOG_WARN("AI bridge init failed, stream path remains unchanged: %s",
                ai_bridge_block_reason());
        }

        if (g_ai_bridge_started == TD_TRUE && ai_bridge_is_ready() == TD_TRUE) {
            rtsp_streamer_ai_async_reset();
            ret = pthread_create(&g_ai_infer_thread, NULL, ai_infer_worker_thread, NULL);
            if (ret == 0) {
                g_ai_infer_thread_started = TD_TRUE;
                RTSP_LOG_INFO("AI async infer thread started");

                /* Start the consumer before the video thread that queues inference and feeds VENC. */
                ret = pthread_create(&g_ai_probe_thread, NULL, ai_venc_send_thread, NULL);
                if (ret == 0) {
                    g_ai_probe_thread_started = TD_TRUE;
                    g_ai_manual_venc_path = TD_TRUE;
                    RTSP_LOG_INFO("AI async draw+send thread started on VPSS chn%d -> VENC1", RTSP_STREAM_IDX);
                } else {
                    rtsp_streamer_ai_async_stop();
                    pthread_join(g_ai_infer_thread, NULL);
                    g_ai_infer_thread_started = TD_FALSE;
                    RTSP_LOG_WARN("AI draw+send thread create failed: %d, fallback to direct VPSS->VENC bind", ret);
                }
            } else {
                RTSP_LOG_WARN("AI async infer thread create failed: %d, fallback to direct VPSS->VENC bind", ret);
            }
        }
    } else {
        RTSP_LOG_INFO("AI realtime overlay disabled by build or video.conf, using direct VPSS->VENC path");
    }

    /*
     * Fall back to direct hardware binding when AI is disabled or worker startup fails.
     * This path has no CPU overlay but keeps VENC1 and base streaming operational.
     */
    if (g_ai_manual_venc_path != TD_TRUE) {
        ret = sample_comm_vpss_bind_venc(vpss_grp, venc_vpss_chn.vpss_chn[stream_idx],
            venc_vpss_chn.venc_chn[stream_idx]);
        if (ret != TD_SUCCESS) {
            RTSP_LOG_ERROR("sample_comm_vpss_bind_venc VENC1 failed: 0x%x", ret);
            goto rtsp_venc_stop;
        }
    }

    /* Wait for the first I-frame and extract SPS/PPS with bounded retries. */
    td_s32 retry_count = 0;
    td_s32 max_retry = 10;
    ret = TD_FAILURE;

    while (g_running && retry_count < max_retry && ret != TD_SUCCESS) {
        usleep(500000);  /* Wait for initial encoded data containing SPS/PPS. */
        ret = get_sps_pps(rtsp_venc_chn, &sps_pps_buf, &sps_pps_len);
        if (ret != TD_SUCCESS) {
            retry_count++;
        }
    }

    if (ret != TD_SUCCESS) {
        RTSP_LOG_ERROR("get_sps_pps failed after %d retries", max_retry);
        RTSP_LOG_INFO("keeping pipeline alive for dump tools");
        while (g_running) {
            sleep(1);
        }
        goto webrtc_cleanup;
    }

    /* Initialize the RTSP server. */
    g_rtsp_server = rtsp_new_demo(rtsp_port);
    if (g_rtsp_server == NULL) {
        RTSP_LOG_ERROR("rtsp_new_demo failed");
        goto webrtc_cleanup;
    }

    g_rtsp_session = rtsp_new_session(g_rtsp_server, "/live");
    if (g_rtsp_session == NULL) {
        RTSP_LOG_ERROR("rtsp_new_session failed");
        goto webrtc_cleanup;
    }

    /* RTSP needs the codec header for SDP; WebRTC caches it for new clients. */
    ret = rtsp_set_video(g_rtsp_session, RTSP_CODEC_ID_VIDEO_H264, sps_pps_buf, sps_pps_len);
    if (ret != 0) {
        RTSP_LOG_ERROR("rtsp_set_video failed: %d", ret);
        goto webrtc_cleanup;
    }

    RTSP_LOG_INFO("RTSP ready: rtsp://[board_ip]:%u/live", rtsp_port);

    ret = webrtc_bridge_set_codec_header(sps_pps_buf, sps_pps_len);
    if (ret != TD_SUCCESS) {
        RTSP_LOG_WARN("WebRTC codec header cache failed: 0x%x", ret);
    }

    /* WebRTC is optional; RTSP continues independently if initialization fails. */
    ret = webrtc_bridge_init(rtsp_venc_chn, WEBRTC_VIDEO_H264, WEBRTC_STREAM_MAIN);
    if (ret == TD_SUCCESS) {
        g_webrtc_bridge_started = TD_TRUE;
        RTSP_LOG_INFO("WebRTC bridge initialized");
    } else {
        RTSP_LOG_WARN("WebRTC bridge disabled, RTSP will continue alone");
    }

    /* Start the VENC1 drain and RTSP sender thread. */
    ret = pthread_create(&g_venc_thread, NULL, venc_rtsp_thread, NULL);
    if (ret != 0) {
        RTSP_LOG_ERROR("pthread_create failed: %d", ret);
        goto webrtc_cleanup;
    }
    g_venc_thread_started = TD_TRUE;

    RTSP_LOG_INFO("streaming started");

    /* The main loop only processes RTSP events. */
    while (g_running) {
        rtsp_do_event(g_rtsp_server);
        usleep(10000);  /* Avoid spinning at full CPU when no event is pending. */
    }

    /* Release resources in reverse initialization order. */
    RTSP_LOG_INFO("stopping");

    if (g_venc_thread_started == TD_TRUE) {
        g_running = TD_FALSE;
        pthread_join(g_venc_thread, NULL);
        g_venc_thread_started = TD_FALSE;
    }

webrtc_cleanup:
    /* Stop threads that access AI or network modules before releasing those modules. */
    if (g_webrtc_bridge_started == TD_TRUE) {
        webrtc_bridge_shutdown();
        g_webrtc_bridge_started = TD_FALSE;
    }

    if (g_ai_probe_thread_started == TD_TRUE) {
        g_running = TD_FALSE;
        pthread_join(g_ai_probe_thread, NULL);
        g_ai_probe_thread_started = TD_FALSE;
    }

    if (g_ai_infer_thread_started == TD_TRUE) {
        rtsp_streamer_ai_async_stop();
        pthread_join(g_ai_infer_thread, NULL);
        g_ai_infer_thread_started = TD_FALSE;
    }
    rtsp_streamer_ai_async_release();

    /* The model receiver may call bridge callbacks, so stop it before ai_bridge. */
    if (g_model_receiver_started == TD_TRUE) {
        model_receiver_stop();
        g_model_receiver_started = TD_FALSE;
    }

    if (g_ai_bridge_started == TD_TRUE) {
        ai_bridge_shutdown();
        g_ai_bridge_started = TD_FALSE;
    }

    /* Reporter stop joins its queue worker before media teardown continues. */
    if (g_http_reporter_started == TD_TRUE) {
        http_reporter_stop();
        g_http_reporter_started = TD_FALSE;
    }

    if (g_rtsp_session) {
        rtsp_del_session(g_rtsp_session);
        g_rtsp_session = NULL;
    }

    if (g_rtsp_server) {
        rtsp_del_demo(g_rtsp_server);
        g_rtsp_server = NULL;
    }

    if (sps_pps_buf) {
        free(sps_pps_buf);
    }

    /* Only the direct path needs unbinding; the manual path never created this bind. */
    if (g_ai_manual_venc_path != TD_TRUE) {
        sample_comm_vpss_un_bind_venc(vpss_grp, venc_vpss_chn.vpss_chn[stream_idx],
            venc_vpss_chn.venc_chn[stream_idx]);
    }

rtsp_venc_stop:
    /* The labels below unwind VENC1 -> VENC0 -> VPSS -> VI -> SYS in reverse order. */
    sample_comm_venc_stop(rtsp_venc_chn);

dummy_join:
    if (g_dummy_venc_thread_started == TD_TRUE) {
        g_running = TD_FALSE;
        pthread_join(g_dummy_venc_thread, NULL);
        g_dummy_venc_thread_started = TD_FALSE;
    }

main_venc_unbind:
    sample_comm_vpss_un_bind_venc(vpss_grp, venc_vpss_chn.vpss_chn[MAIN_STREAM_IDX],
        venc_vpss_chn.venc_chn[MAIN_STREAM_IDX]);

main_venc_stop:
    sample_comm_venc_stop(main_venc_chn);

vpss_unbind:
    sample_comm_vi_un_bind_vpss(active_vi_pipe, 0, vpss_grp, 0);

vpss_stop:
    sample_common_vpss_stop(vpss_grp, vpss_cfg.chn_en, OT_VPSS_MAX_PHYS_CHN_NUM);

vi_stop:
    sample_comm_vi_stop_vi(&vi_cfg);

sys_exit:
    sample_comm_sys_exit();

exit:
    RTSP_LOG_INFO("program exited");
    return ret;
}
