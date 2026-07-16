#ifndef WEBRTC_BRIDGE_H
#define WEBRTC_BRIDGE_H

#include "sample_comm.h"
#include "webrtc_streamer.h"

/*
 * WebRTC SDK bridge lifecycle: init -> set_codec_header/input_video -> shutdown.
 * request_idr_chn requests a VENC keyframe when a new session or PLI arrives.
 */
td_s32 webrtc_bridge_init(ot_venc_chn request_idr_chn, webrtc_video_code_type_t codec_type,
    webrtc_stream_type_t stream_type);
/* Copy and retain the codec header; the caller may release its source buffer on return. */
td_s32 webrtc_bridge_set_codec_header(const td_u8 *data, td_u32 len);
td_void webrtc_bridge_shutdown(td_void);
/* Submit one complete encoded frame; non-keyframes are dropped when a session first starts. */
td_s32 webrtc_bridge_input_video(const td_u8 *data, td_u32 len);
td_bool webrtc_bridge_is_enabled(td_void);
td_bool webrtc_bridge_has_active_session(td_void);

#endif
