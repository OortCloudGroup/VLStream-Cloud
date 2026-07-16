#ifndef HTTP_REPORTER_H
#define HTTP_REPORTER_H

#include <stdbool.h>
#include "sample_comm.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef struct {
    td_s32 class_id;
    td_char class_name[64];
    float confidence;
    float x1;
    float y1;
    float x2;
    float y2;
} http_reporter_detection;

/* Load both reporting endpoint configurations from /mnt/webrtc/video.conf. */
void http_reporter_load_config(void);
void http_reporter_set_enable(bool enable);
bool http_reporter_is_enabled(void);
/* start/stop are idempotent; stop joins the network worker before returning. */
void http_reporter_start(void);
void http_reporter_stop(void);

/*
 * Copy a detection frame into the bounded queue. JPEG encoding and HTTP I/O
 * run in the reporter worker, so this call never waits for network completion.
 */
void http_reporter_submit(td_s32 frame_id, td_u32 width, td_u32 height,
    td_u32 stride_y, td_u32 stride_uv, td_bool yvu_semiplanar,
    const td_u8 *yuv420sp, const http_reporter_detection *detections,
    td_u32 detection_count);

#ifdef __cplusplus
}
#endif

#endif
