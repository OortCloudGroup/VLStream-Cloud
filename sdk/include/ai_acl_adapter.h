#ifndef AI_ACL_ADAPTER_H
#define AI_ACL_ADAPTER_H

#include "sample_comm.h"

#ifdef __cplusplus
extern "C" {
#endif

/* ACL runtime and model options are copied into the module during initialization. */
typedef struct {
    const td_char *model_path;
    const td_char *config_path;
    const td_char *classes_path;
    td_bool enable_inference;
    td_bool enable_reporter;
    td_bool enable_model_receiver;
    td_s32 device_id;
} ai_acl_adapter_attr;

/*
 * YUV420SP frame descriptor. A stride is measured in bytes per row and may
 * exceed the image width. Set input_is_yvu_semiplanar_420 for NV21/VU input;
 * clear it for NV12/UV input.
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
} ai_acl_adapter_frame;

typedef struct {
    td_s32 class_id;
    td_char class_name[64];
    float confidence;
    float x1;
    float y1;
    float x2;
    float y2;
} ai_acl_adapter_detection;

/* Lifecycle calls are serialized internally; all frame calls must occur between init and shutdown. */
td_s32 ai_acl_adapter_init(const ai_acl_adapter_attr *attr);
td_void ai_acl_adapter_shutdown(td_void);
td_bool ai_acl_adapter_is_ready(td_void);
const td_char *ai_acl_adapter_backend_name(td_void);
const td_char *ai_acl_adapter_block_reason(td_void);
td_s32 ai_acl_adapter_submit_frame(ai_acl_adapter_frame *frame);
td_s32 ai_acl_adapter_overlay_cached_frame(ai_acl_adapter_frame *frame);
td_s32 ai_acl_adapter_get_frame(ai_acl_adapter_frame *frame);
/* Returned boxes use source-frame coordinates, not the 640x640 model coordinate system. */
td_s32 ai_acl_adapter_copy_cached_detections(const ai_acl_adapter_frame *frame,
    ai_acl_adapter_detection *detections, td_u32 max_detections, td_u32 *detection_count);
td_s32 ai_acl_adapter_validate_model(const td_char *model_path);
td_s32 ai_acl_adapter_switch_model(const td_char *model_path);

#ifdef __cplusplus
}
#endif

#endif
