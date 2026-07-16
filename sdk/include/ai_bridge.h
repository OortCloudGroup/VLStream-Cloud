#ifndef AI_BRIDGE_H
#define AI_BRIDGE_H

#include "sample_comm.h"

#ifdef __cplusplus
extern "C" {
#endif

/* Stable C-facing configuration consumed by the C++ ACL inference backend. */
typedef struct {
    const td_char *model_path;
    const td_char *config_path;
    const td_char *classes_path;
    td_bool enable_ai;
    td_bool enable_reporter;
    td_bool enable_model_receiver;
} ai_bridge_attr;

/*
 * One YUV420SP frame. The caller owns both buffers for the duration of the
 * call. input_yuv420p and output_yuv420p may point to the same memory.
 */
typedef struct {
    const td_u8 *input_yuv420p;
    td_u8 *output_yuv420p;
    td_u32 width;
    td_u32 height;
    td_u32 stride_y;
    td_u32 stride_uv;
    td_bool input_is_yvu_semiplanar_420;
    td_s32 frame_id;
    td_s32 detection_count;
} ai_bridge_frame;

typedef struct {
    td_s32 class_id;
    td_char class_name[64];
    float confidence;
    float x1;
    float y1;
    float x2;
    float y2;
} ai_bridge_detection;

/* Initialize once before any frame call; repeated initialization does not recreate resources. */
td_s32 ai_bridge_init(const ai_bridge_attr *attr);
/* Stop inference and release the model, Dataset, stream, context, and ACL runtime. */
td_void ai_bridge_shutdown(td_void);
td_bool ai_bridge_is_ready(td_void);
const td_char *ai_bridge_backend_name(td_void);
const td_char *ai_bridge_block_reason(td_void);
/* Run inference synchronously in the calling AI worker and update the detection cache. */
td_s32 ai_bridge_submit_frame(ai_bridge_frame *frame);
/* Draw the latest still-valid cached detections without running inference. */
td_s32 ai_bridge_overlay_cached_frame(ai_bridge_frame *frame);
/* Copy cached boxes in source-frame coordinates without exposing internal pointers. */
td_s32 ai_bridge_copy_cached_detections(const ai_bridge_frame *frame,
    ai_bridge_detection *detections, td_u32 max_detections, td_u32 *detection_count);
/* Validate a candidate OM without changing the active model. */
td_s32 ai_bridge_validate_model(const td_char *model_path);
/* Replace active model resources only after the candidate OM loads and passes its self-test. */
td_s32 ai_bridge_switch_model(const td_char *model_path);

#ifdef __cplusplus
}
#endif

#endif
