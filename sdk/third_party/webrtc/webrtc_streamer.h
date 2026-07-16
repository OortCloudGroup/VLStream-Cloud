
#ifndef WEBRTC_STREAMER_H_3060898B846849FF9F88F5DB59B5950C
#define WEBRTC_STREAMER_H_3060898B846849FF9F88F5DB59B5950C

/*
 * webrtc_streamer.h
 * ---------------------------------------------------------------------------
 *
 * Public header for the WebRTC Streamer SDK. It provides event definitions,
 *   callback types, media input APIs, session control, Matter, WebServer,
 *   cloud publishing and WHIP related interfaces.
 */

#ifdef __cplusplus
#include <cstddef>
#include <cstdarg>
#include <cstdint>
#include <sys/types.h>
extern "C" {
#else
#include <stdarg.h>
#include <sys/types.h>
#include <stdint.h>
#endif

/* ---------------------------------------------------------------------------
 * Basic enums and data types
 * ---------------------------------------------------------------------------
 */

/*
 * SDK event types.
 */
typedef enum {
    WEBRTC_EVENT_NULL             = 0,
    WEBRTC_EVENT_ASK_IFRAME       = 1,
    WEBRTC_EVENT_LUCK             = 2,
    WEBRTC_EVENT_UNLUCK           = 3,
    WEBRTC_EVENT_LUCK_STATE       = 4,
    WEBRTC_EVENT_CALL_START       = 5,
    WEBRTC_EVENT_CALL_LINK        = 6,
    WEBRTC_EVENT_CALL_DISCONNECT  = 7,
    WEBRTC_EVENT_CALL_DESTORY     = 8,
    WEBRTC_EVENT_ONLINE           = 9,
    WEBRTC_EVENT_OFFLINE          = 10,
    WEBRTC_EVENT_DATACHANNEL_OPEN = 11,
    WEBRTC_EVENT_LOW_POWER_LEVEL  = 12,
    WEBRTC_EVENT_MAX_CHANNEL      = 13,
    WEBRTC_EVENT_CONNECT_FAILD    = 14,
    WEBRTC_EVENT_CONNECT_RECONNECT= 15,
    WEBRTC_EVENT_CALL_FAILD       = 16,
    WEBRTC_EVENT_PLI              = 17,
    WEBRTC_EVENT_SLI              = 18,
} webrtc_event_type_t;

/*
 * Device-originated call event types.
 */
typedef enum {
    WEBRTC_CALL_EVENT_DOORBELL_PRESS = 0,
    WEBRTC_CALL_EVENT_PIR            = 1,
    WEBRTC_CALL_EVENT_CUSTOM         = 2,
} webrtc_call_event_type_t;

/*
 * Stream channel identifiers.
 */
typedef enum {
    WEBRTC_STREAM_MAIN = 0,
    WEBRTC_STREAM_SUB  = 1,
    WEBRTC_STREAM_2    = 2,
    WEBRTC_STREAM_3    = 3,
    WEBRTC_STREAM_4    = 4,
    WEBRTC_STREAM_5    = 5,
    WEBRTC_STREAM_6    = 6,
    WEBRTC_STREAM_7    = 7,
    WEBRTC_STREAM_8    = 8,
    WEBRTC_STREAM_9    = 9,
    WEBRTC_STREAM_10   = 10,
    WEBRTC_STREAM_11   = 11,
    WEBRTC_STREAM_12   = 12,
    WEBRTC_STREAM_13   = 13,
    WEBRTC_STREAM_14   = 14,
    WEBRTC_STREAM_15   = 15,
    WEBRTC_STREAM_PLAY = 256,
} webrtc_stream_type_t;

/*
 * Network quality levels.
 */
typedef enum {
    WEBRTC_NETWORK_QUALITY_GOOD   = 0,
    WEBRTC_NETWORK_QUALITY_MIDDLE = 1,
    WEBRTC_NETWORK_QUALITY_LOW    = 2,
} webrtc_network_quality_type_t;

/*
 * Generic error codes.
 */
typedef enum {
    WEBRTC_ERR_SUCCESS    = 0,
    WEBRTC_ERR_INITSTRING = -1,
    WEBRTC_ERR_SERNO      = -2,
} webrtc_error_t;

/*
 * DataChannel message types.
 */
typedef enum {
    WEBRTC_DMT_BINARY = 0,
    WEBRTC_DMT_TEXT   = 1,
} webrtc_data_message_type_t;

/*
 * Log level bit mask, combinable with bitwise OR.
 */
typedef enum {
    WEBRTC_STREAM_DEBUG      = 1,
    WEBRTC_STREAM_MESSAGE    = 1 << 1,
    WEBRTC_STREAM_WARNING    = 1 << 2,
    WEBRTC_STREAM_ERROR      = 1 << 3,
    WEBRTC_STREAM_FATAL      = 1 << 4,
    WEBRTC_STREAM_TRACE      = 1 << 5,
    WEBRTC_STREAM_LOGLEV_END = 1 << 6
} webrtc_LogLevel_t;

/*
 * Cloud upload speed modes.
 */
typedef enum {
    WEBRTC_CLOUD_UPLOADSPEED_NORMAL = 0,
    WEBRTC_CLOUD_UPLOADSPEED_FAST   = 1,
} webrtc_clouduploadspeed_t;

/*
 * WHIP publish state/error event types.
 */
typedef enum {
    WEBRTC_HTTP_POST_CONNECT = 0,
    WEBRTC_HTTP_POST_ERROR   = 1,
    WEBRTC_HTTP_POST_CLOSE   = 2,
    WEBRTC_HTTP_POST_MSG     = 3,
    WEBRTC_HTTP_POST_STATE   = 4,
    WEBRTC_HTTP_POST_TIMEOUT = 5,
} webrtc_whip_error_type;

/*
 * Video codec types.
 */
typedef enum {
    WEBRTC_VIDEO_H264 = 0,
    WEBRTC_VIDEO_H265 = 1,
    WEBRTC_VIDEO_VP8  = 2,
    WEBRTC_VIDEO_VP9  = 3,
    WEBRTC_VIDEO_AV1  = 4,
} webrtc_video_code_type_t;

/*
 * Cloud publishing error codes.
 */
typedef enum {
    WEBRTC_CLOULD_PUB_SUCCESS            = 0,
    WEBRTC_CLOULD_PUB_FILE_ERR_NETWORK   = -1,
    WEBRTC_CLOULD_PUB_FILE_ERR_OPEN_FILE = -2,
    WEBRTC_CLOULD_PUB_NO_SERVER_ADDR     = -3,
    WEBRTC_CLOULD_PUB_NO_SERVER_SPACE    = -4,
    WEBRTC_CLOULD_PUB_NO_PERSONAL_SPACE  = -5,
} webrtc_cloud_publish_error_t;

/*
 * Signaling socket error types.
 */
typedef enum {
    WEBRTC_SIG_ERROR_NULL              = 0,
    WEBRTC_SIG_ERROR_GETHOSTBYNAME     = 1,
    WEBRTC_SIG_ERROR_CONNECT_TIMEOUT   = 2,
    WEBRTC_SIG_ERROR_KEEPALIVE_TIMEOUT = 3,
    WEBRTC_SIG_ERROR_REGISTER_TIMEOUT  = 4,
} webrtc_signaling_socket_error_t;

/*
 * Runtime statistics of a session.
 */
typedef struct _webrtc_streamer_session_info {
    webrtc_stream_type_t start_stream_type;
    webrtc_stream_type_t stream_type;
    char szmode[64];
    int connect_time;
    int session_time;
    int video_send_bitrate;
    int audio_send_bitrate;
    int send_packets;
    int resend_packets;
    int current_resend_packets;
    int video_recv_bitrate;
    int audio_recv_bitrate;
    int transmission_mode;
    int audio_packet_loss;
    int video_packet_loss;
    int audio_current_packet_lost;
    int video_current_packet_lost;
    int current_bandwidth_kps;
    int current_pli_count;
    int send_data_bitrate;
    int recv_data_bitrate;
    int user_send_video;
    int user_send_audio;
} webrtc_streamer_session_info;

/* ---------------------------------------------------------------------------
 * Callback type definitions
 * ---------------------------------------------------------------------------
 */

/* ---------------------------------------------------------------------------
 * Session and media callbacks
 * ---------------------------------------------------------------------------
 */
/*
 * Called when a new incoming session arrives.
 * Params: sessionId session ID; sessionId_len session ID length; szmode mode string; mode_len mode length; szsource source string; source_len source length; user opaque user pointer.
 */
typedef void(*webrtc_streamer_call_income_callback)(char *sessionId,size_t sessionId_len,char *szmode,size_t mode_len,char *szsource,size_t source_len,void *user);
/*
 * Session destroy callback. Params: sessionId session ID; sessionId_len length; user opaque user pointer.
 */
typedef void(*webrtc_streamer_call_destory_callback)(char *sessionId,size_t sessionId_len,void *user);
/*
 * Call failure callback. Params: sessionId session ID; sessionId_len length; message error string; user opaque user pointer.
 */
typedef void(*webrtc_streamer_call_failed_callback)(char *sessionId,size_t sessionId_len,char *message,void *user);
/*
 * Call disconnect callback. Params: sessionId session ID; sessionId_len length; message disconnect reason; user opaque user pointer.
 */
typedef void(*webrtc_streamer_call_disconnect_callback)(char *sessionId,size_t sessionId_len,char *message,void *user);
/*
 * Generic event callback. Params: event event type; user opaque user pointer; result optional output value for some events.
 */
typedef void(*webrtc_streamer_event_callback)(webrtc_event_type_t event,void *user,int *result);
/*
 * Audio receive callback. Params: data audio payload; len payload length; sessionId session ID; sessionId_len length; user opaque user pointer.
 */
typedef void(*webrtc_streamer_audio_callback)(char *data,size_t len,char *sessionId,size_t sessionId_len,void *user);
/*
 * Configuration callback. Params: data configuration payload; len payload length; reboot whether reboot is required.
 */
typedef void(*webrtc_streamer_configuration_callback)(char *data,size_t len,int reboot);
/*
 * Basic authentication callback. Params: authdata auth payload; authlen length; password output password buffer; pwdlen password buffer length. Return semantics are implementation-defined.
 */
typedef int(*webrtc_streamer_authentication_callback)(char *authdata,size_t authlen,char *password,size_t pwdlen);
/*
 * Signaling message callback. Params: ReqMsg request message; RspMsg response buffer; RspMsg_len in/out response length; user opaque user pointer.
 */
typedef void(*webrtc_streamer_message_callback)(char *sessionId,size_t sessionId_len,char *ReqMsg,size_t ReqMsg_len,char *RspMsg,size_t *RspMsg_len,void *user);
/*
 * DataChannel message callback. Params: type message type; streamid channel stream ID; Msg payload; Msg_len payload length.
 */
typedef void(*webrtc_streamer_datachannel_message_callback)(char *sessionId,size_t sessionId_len,webrtc_data_message_type_t type,int streamid,char *Msg,size_t Msg_len,void *user);
/*
 * DataChannel open callback. Params: sessionId session ID; streamid channel stream ID; user opaque user pointer.
 */
typedef void(*webrtc_streamer_datachnanle_open_callback)(char *sessionId,size_t sessionId_len,int streamid,void *user);
/*
 * Callback indicating whether a DataChannel can be created. Param is_create_offer indicates whether a new offer is needed.
 */
typedef void(*webrtc_streamer_can_add_datachnanle_callback)(char *sessionId,size_t sessionId_len,int is_create_offer,void *user);
/*
 * Remote playback start callback.
 */
typedef void(*webrtc_streamer_remote_play_start_callback)(char *sessionId,size_t sessionId_len,void *user);
/*
 * I-frame request callback.
 */
typedef void(*webrtc_streamer_session_ask_iframe_callback)(char *sessionId,size_t sessionId_len,void *user);
/*
 * PLI callback.
 */
typedef void(*webrtc_streamer_session_pli_callback)(char *sessionId,size_t sessionId_len,void *user);
/*
 * Mixed-audio output callback. Params: data audio payload; len length; user opaque user pointer.
 */
typedef void(*webrtc_streamer_mixer_audio_callback)(char *data,size_t len,void *user);
/*
 * Log output callback. Params: data log text; len length; user opaque user pointer.
 */
typedef void(*webrtc_streamer_log_callback)(char *data,size_t len,void *user);
/*
 * Send-queue-full callback. Params: queue queue index; bufsize current buffered size.
 */
typedef void(*webrtc_streamer_send_queue_full_callback)(char *sessionId,size_t sessionId_len,int queue,int bufsize,void *user);
/*
 * Default media send policy callback. Params: audio/video are in-out enable flags that may be modified by the callback.
 */
typedef void(*webrtc_streamer_default_mediasend_callback)(char *sessionId,size_t sessionId_len,char *sessionType,size_t sessionType_len,int *audio,int *video,void *user);
/*
 * Signaling socket error callback. Params: error error code; szerror error description; user opaque user pointer.
 */
typedef void(*webrtc_streamer_signaling_socket_error_callback)(int error,char *szerror,void *user);

/* ---------------------------------------------------------------------------
 * Cloud publish callbacks
 * ---------------------------------------------------------------------------
 */
/*
 * Cloud file upload start callback. Parameters include local and cloud file names.
 */
typedef void(*webrtc_streamer_cloud_publish_file_start_callback)(char *sessionId,size_t sessionId_len,char *filename,size_t filename_len,char *clouldfilename,size_t clouldfilename_len,void *user);
/*
 * Cloud file upload completion callback.
 */
typedef void(*webrtc_streamer_cloud_publish_file_end_callback)(char *sessionId,size_t sessionId_len,char *filename,size_t filename_len,void *user);
/*
 * Cloud file upload error callback. Param: error error code.
 */
typedef void(*webrtc_streamer_cloud_publish_file_error_callback)(char *sessionId,size_t sessionId_len,int error,void *user);
/*
 * Cloud file upload progress callback. Param: step stage or progress value.
 */
typedef void(*webrtc_streamer_cloud_publish_file_step_callback)(char *sessionId,size_t sessionId_len,int step,void *user);
/*
 * Cloud realtime-stream publish error callback.
 */
typedef void(*webrtc_streamer_cloud_publish_realtime_stream_error_callback)(char *sessionId,size_t sessionId_len,int error,void *user);
/*
 * Cloud publish enable-state callback. Param: enable 1 enabled, 0 disabled.
 */
typedef void(*webrtc_streamer_cloud_publish_enable)(int enable,void *user);
/*
 * WHIP publish state/error callback. Params: event event type; state state code.
 */
typedef void(*webrtc_streamer_whip_publish_error_callback)(webrtc_whip_error_type event,int state,void *user);

/* ---------------------------------------------------------------------------
 * Extended media callbacks
 * ---------------------------------------------------------------------------
 */
/*
 * Video callback. Params: stream_type stream type; type codec type; data payload; len length; sessionId session ID.
 */
typedef void(*webrtc_streamer_video_callback)(webrtc_stream_type_t stream_type,webrtc_video_code_type_t type,char *data,size_t len,char *sessionId,size_t sessionId_len,void *user);
/*
 * Callback to query the video codec type of a session. Return value should be one of webrtc_video_code_type_t.
 */
typedef int(*webrtc_streamer_check_videocode_callback)(webrtc_stream_type_t stream_type,char *sessionId,size_t sessionId_len,char *sessionType,size_t sessionType_len,char *szmode,size_t mode_len,char *szsource,size_t source_len,void *user);
/*
 * Extended incoming-session callback with sessionType.
 */
typedef void(*webrtc_streamer_call_income_callback_ex)(char *sessionId,size_t sessionId_len,char *sessionType,size_t sessionType_len,char *szmode,size_t mode_len,char *szsource,size_t source_len,void *user);

/* ---------------------------------------------------------------------------
 * Platform integration callbacks
 * ---------------------------------------------------------------------------
 */
/*
 * Alexa custom directive callback. Parameters include namespace, instance, name, directive message and output result buffer.
 */
typedef void(*webrtc_streamer_alexa_customer_message_callback)(char *pnamespace,size_t namespace_len,char *pinstance,size_t instance_len,char *name,size_t name_len,char *alexadirectivemsg,size_t alexadirectivemsg_len,char *resvalue,void *user);
/*
 * Network quality callback. Param: quality network quality level.
 */
typedef void(*webrtc_streamer_network_quality_callback)(char *sessionId,size_t sessionId_len,webrtc_network_quality_type_t quality,void *user);
/*
 * Local network information callback. Params: ip/gw/mask are output buffers.
 */
typedef void(*webrtc_streamer_get_network_info_callback)(char *ip,char* gw,char*mask,void *user);

/* ---------------------------------------------------------------------------
 * ---------------------------------------------------------------------------
 */
/*
 * Matter offer SDP callback.
 */
typedef void(*webrtc_streamer_matter_offer_sdp_callback)(uint16_t sessionId,char* sdp,void *user);
/*
 * Matter answer SDP callback.
 */
typedef void(*webrtc_streamer_matter_answer_sdp_callback)(uint16_t sessionId,char* sdp,void *user);
/*
 * Matter ICE candidate callback.
 */
typedef void(*webrtc_streamer_matter_ice_candidate_callback)(uint16_t sessionId,char* icecandidate,void *user);
/*
 * Matter session destroy callback.
 */
typedef void(*webrtc_streamer_matter_destroy_callback)(uint16_t sessionId,void *user);

/* ---------------------------------------------------------------------------
 * Extended authentication and call-state callbacks
 * ---------------------------------------------------------------------------
 */
/*
 * Extended authentication callback. Parameters include session ID, session type, auth payload and password output buffer. Return semantics are implementation-defined.
 */
typedef int(*webrtc_streamer_authentication_ex_callback)(char *sessionId,size_t sessionId_len,char *sessionType,size_t sessionType_len,char *authdata,size_t authlen,char *password,size_t pwdlen);
/*
 * Outgoing-call state callback. Param: szstate state string.
 */
typedef void(*webrtc_streamer_callstate_callback)(char *sessionId,size_t sessionId_len,char *szstate,void *user);

/* ---------------------------------------------------------------------------
 * Built-in WebServer callbacks
 * ---------------------------------------------------------------------------
 */
/*
 * Built-in WebServer API callback. Params: url request path; ReqMsg request body; RspMsg response buffer; RspMsg_len in/out response length.
 */
typedef void(*webrtc_streamer_webserver_api_callback)(const char *url,size_t url_len,const char *ReqMsg,size_t ReqMsg_len,char *RspMsg,size_t *RspMsg_len,void *user);
/*
 * Built-in WebSocket message callback. Params: sessionId session ID; ReqMsg incoming message; RspMsg response buffer.
 */
typedef void(*webrtc_streamer_webserver_websocket_message_callback)(char *sessionId,size_t sessionId_len,char *ReqMsg,size_t ReqMsg_len,char *RspMsg,size_t *RspMsg_len,void *user);

/* ---------------------------------------------------------------------------
 * Basic control APIs
 * ---------------------------------------------------------------------------
 */

/*
 * Set the log output level bit mask.
 * Param: lev log level bit mask, combinable with bitwise OR.
 * Return: 0 on success, <0 on failure.
 */
int webrtc_streamer_set_log_level_mask(int lev);

/*
 * Set the maximum number of concurrent sessions.
 * Param: max_channel maximum session count, typically 32 by default.
 * Return: 0 on success, <0 on failure.
 */
int webrtc_streamer_set_max_channel(int max_channel);

/*
 * Initialize the SDK.
 * Params:
 *   initstring     initialization string.
 *   configuration  configuration string.
 *   serno          device serial number.
 *   servers        server configuration string.
 *   customerserno  customer-defined serial number or identifier.
 * Return: 0 on success, <0 on failure.
 */
int webrtc_streamer_init(char *initstring,char *configuration,char *serno,char *servers,char *customerserno);

/*
 * Uninitialize the SDK and release internal resources.
 * Return: 0 on success, <0 on failure.
 */
int webrtc_streamer_uninit(void);

/*
 * Get the current number of active sessions.
 * Return: current active session count.
 */
int webrtc_streamer_current_session_count(void);

/*
 * Configure mDNS device discovery information.
 * Params:
 *   mdnsservername service name, usually exposed as _XXXXXXXX._tcp.local.
 *   name           TXT field "name".
 *   type           TXT field "type".
 *   version        TXT field "version".
 * Return: 0 on success, <0 on failure.
 */
int webrtc_streamer_set_device_discovery_info(char *mdnsservername,char *name,char*type,char *version);

/*
 * Enable or disable mDNS TXT payload.
 * Param: enable 1 to send TXT records, 0 to disable.
 * Return: 0 on success, <0 on failure.
 */
int webrtc_streamer_enable_device_discovery_info(int enable);

/*
 * Send one mDNS discovery message immediately.
 * Return: 0 on success, <0 on failure.
 */
int webrtc_streamer_send_device_discovery_message(void);

/*
 * Configure SDK memory limits.
 * Params:
 *   max_sdk_use_mem          total SDK memory limit.
 *   max_ssession_use_mem     per-session memory limit.
 *   max_ssession_buffer_size cached RTP packet count per session for retransmission.
 * Return: 0 on success, <0 on failure.
 */
int webrtc_streamer_set_mem_info(int max_sdk_use_mem,int max_ssession_use_mem,int max_ssession_buffer_size);

/*
 * Set the signaling reconnect timeout.
 * Param: timeout reconnect timeout, typically in milliseconds.
 * Return: 0 on success, <0 on failure.
 */
int webrtc_streamer_set_signal_reconnect_timeout(int timeout);

/* ---------------------------------------------------------------------------
 * Callback registration APIs
 * ---------------------------------------------------------------------------
 */

/*
 * Common rules for the callback registration APIs below:
 *   callback is the application-provided callback function.
 *   user is an opaque user pointer passed back unchanged by the SDK.
 *   Return value is 0 on success and <0 on failure.
 */
int webrtc_streamer_register_call_income_callback_fun(webrtc_streamer_call_income_callback callback,void *user);
int webrtc_streamer_register_call_destory_callback_fun(webrtc_streamer_call_destory_callback callback,void *user);
int webrtc_streamer_register_call_failed_callback_fun(webrtc_streamer_call_failed_callback callback,void *user);
int webrtc_streamer_register_call_disconnect_callback_fun(webrtc_streamer_call_disconnect_callback callback,void *user);
int webrtc_streamer_register_event_callback_fun(webrtc_streamer_event_callback callback,void *user);
int webrtc_streamer_register_audio_callback_fun(webrtc_streamer_audio_callback callback,void *user);
int webrtc_streamer_register_configuration_callback_fun(webrtc_streamer_configuration_callback callback,void *user);
int webrtc_streamer_register_authentication_callback_fun(webrtc_streamer_authentication_callback callback,void *user);
int webrtc_streamer_register_authentication_ex_callback_fun(webrtc_streamer_authentication_ex_callback callback,void *user);

/*
 * Register the signaling message callback.
 * Param: MaxRspBufSize is the maximum writable response buffer size.
 */
int webrtc_streamer_register_message_callback_fun(webrtc_streamer_message_callback callback,int MaxRspBufSize,void *user);
int webrtc_streamer_register_datachannel_open_callback_fun(webrtc_streamer_datachnanle_open_callback callback,void *user);
int webrtc_streamer_register_can_add_datachannel_callback_fun(webrtc_streamer_can_add_datachnanle_callback callback,void *user);
int webrtc_streamer_register_datachannel_message_callback_fun(webrtc_streamer_datachannel_message_callback callback,void *user);
int webrtc_streamer_register_remote_play_start_callback_fun(webrtc_streamer_remote_play_start_callback callback,void *user);
int webrtc_streamer_register_session_ask_iframe_callback_fun(webrtc_streamer_session_ask_iframe_callback callback,void *user);
int webrtc_streamer_register_session_pli_callback_fun(webrtc_streamer_session_pli_callback callback,void *user);
int webrtc_streamer_register_mixer_audio_callback_fun(webrtc_streamer_mixer_audio_callback callback,void *user);
int webrtc_streamer_register_log_callback_fun(webrtc_streamer_log_callback callback,void *user);
int webrtc_streamer_register_send_queue_full_callback_fun(webrtc_streamer_send_queue_full_callback callback,void *user);
int webrtc_streamer_register_default_mediasend_callback_fun(webrtc_streamer_default_mediasend_callback callback,void *user);
int webrtc_streamer_register_signaling_socket_error_callback_fun(webrtc_streamer_signaling_socket_error_callback callback,void *user);
int webrtc_streamer_register_call_income_callback_ex_fun(webrtc_streamer_call_income_callback_ex callback,void *user);
int webrtc_streamer_register_video_callback_fun(webrtc_streamer_video_callback callback,void *user);
int webrtc_streamer_register_check_videocode_callback_fun(webrtc_streamer_check_videocode_callback callback,void *user);
int webrtc_streamer_register_callstate_callback_fun(webrtc_streamer_callstate_callback callback,void *user);
int webrtc_streamer_register_alexa_customer_message_callback_fun(webrtc_streamer_alexa_customer_message_callback callback,void *user);
int webrtc_streamer_register_network_quality_callback_fun(webrtc_streamer_network_quality_callback callback,void *user);
int webrtc_streamer_register_get_network_info_callback_fun(webrtc_streamer_get_network_info_callback callback,void *user);
int webrtc_streamer_register_matter_offer_sdp_callback_fun(webrtc_streamer_matter_offer_sdp_callback callback,void *user);
int webrtc_streamer_register_matter_answer_sdp_callback_fun(webrtc_streamer_matter_answer_sdp_callback callback,void *user);
int webrtc_streamer_register_matter_ice_candidate_callback_fun(webrtc_streamer_matter_ice_candidate_callback callback,void *user);
int webrtc_streamer_register_matter_destroy_callback_fun(webrtc_streamer_matter_destroy_callback callback,void *user);
int webrtc_streamer_register_cloud_publish_file_start_callback_fun(webrtc_streamer_cloud_publish_file_start_callback callback,void *user);
int webrtc_streamer_register_cloud_publish_file_end_callback_fun(webrtc_streamer_cloud_publish_file_end_callback callback,void *user);
int webrtc_streamer_register_cloud_publish_file_error_callback_fun(webrtc_streamer_cloud_publish_file_error_callback callback,void *user);
int webrtc_streamer_register_cloud_publish_file_step_callback_fun(webrtc_streamer_cloud_publish_file_step_callback callback,void *user);
int webrtc_streamer_register_cloud_publish_realtime_stream_error_callback_fun(webrtc_streamer_cloud_publish_realtime_stream_error_callback callback,void *user);
int webrtc_streamer_register_cloud_publish_enable_callback_fun(webrtc_streamer_cloud_publish_enable callback,void *user);
int webrtc_streamer_register_whip_publish_error_callback_fun(webrtc_streamer_whip_publish_error_callback callback,void *user);

/*
 * Register the built-in WebServer API callback.
 * Param: MaxRspBufSize is the maximum writable response buffer size.
 */
int webrtc_streamer_register_webserver_api_callback_fun(webrtc_streamer_webserver_api_callback callback,int MaxRspBufSize,void *user);

/*
 * Register the built-in WebSocket message callback.
 * Param: MaxRspBufSize is the maximum writable response buffer size.
 */
int webrtc_streamer_register_webserver_websocket_messaeg_callback_fun(webrtc_streamer_webserver_websocket_message_callback callback,int MaxRspBufSize,void *user);

/* ---------------------------------------------------------------------------
 * Realtime media input APIs
 * ---------------------------------------------------------------------------
 */

/*
 * Input realtime audio data.
 * Params:
 *   data pointer to audio payload.
 *   len  audio payload length in bytes.
 * Return: 0 on success, <0 on failure.
 * Note: Intended for single-audio-source devices and should not be mixed with
 * webrtc_streamer_input_audio_data_ex().
 */
int webrtc_streamer_input_audio_data(unsigned char *data,size_t len);

/*
 * Input realtime audio data by stream type.
 * Params:
 *   type stream channel type.
 *   data pointer to audio payload.
 *   len  audio payload length in bytes.
 * Return: 0 on success, <0 on failure.
 * Note: Intended for multi-audio-source scenarios and should not be mixed with
 * webrtc_streamer_input_audio_data().
 */
int webrtc_streamer_input_audio_data_ex(webrtc_stream_type_t type,unsigned char *data,size_t len);

/*
 * Input realtime video data.
 * Params:
 *   type      stream channel type.
 *   code_type video codec type.
 *   data      pointer to encoded video bitstream.
 *   len       encoded video size in bytes.
 * Return: 0 on success, <0 on failure.
 */
int webrtc_streamer_input_video_data(webrtc_stream_type_t type,webrtc_video_code_type_t code_type,unsigned char *data,size_t len);

/* ---------------------------------------------------------------------------
 * Session control and signaling APIs
 * ---------------------------------------------------------------------------
 */

/*
 * Dynamically change the realtime stream type of a session.
 * Params: sessionId session ID; type new stream channel type.
 * Return: 0 on success, <0 on failure.
 */
int webrtc_streamer_set_session_streamtype(char *sessionId,webrtc_stream_type_t type);

/*
 * Enable or disable audio/video sending for a session.
 * Params: sessionId session ID; sessionId_len session ID length; audio_enable audio flag; video_enable video flag.
 * Return: 0 on success, <0 on failure.
 */
int webrtc_streamer_set_session_realstream(char *sessionId,size_t sessionId_len,int audio_enable,int video_enable);

/*
 * Report a device event such as doorbell, PIR or custom event.
 * Param: event event type.
 * Return: 0 on success, <0 on failure.
 */
int webrtc_streamer_event_call(webrtc_call_event_type_t event);

/*
 * Close a session.
 * Params: sessionId session ID; sessionId_len session ID length.
 * Return: 0 on success, <0 on failure.
 */
int webrtc_streamer_close_session(char *sessionId,size_t sessionId_len);

/*
 * Send a message over signaling for an existing session.
 * Params: sessionId session ID; data message pointer; len message length.
 * Return: 0 on success, <0 on failure.
 */
int webrtc_streamer_send_message(char *sessionId,char *data,size_t len);

/*
 * Quickly reset signaling when the network changes.
 * Return: 0 on success, <0 on failure.
 */
int webrtc_streamer_network_reset(void);

/*
 * Force-close the signaling socket.
 * Return: 0 on success, <0 on failure.
 * Note: This affects automatic reconnect and should be used carefully.
 */
int webrtc_streamer_signal_socket_close(void);

/* ---------------------------------------------------------------------------
 * ---------------------------------------------------------------------------
 */

/*
 * Handle Matter SolicitOffer and generate a local offer.
 * Params: sessionId Matter session ID; iceServers ICE server description string.
 * Return: 0 on success, <0 on failure.
 */
int webrtc_streamer_matter_handlesSolicitOffer(uint16_t sessionId,char* iceServers);

/*
 * Handle Matter ProvideOffer.
 * Params: sessionId Matter session ID; sdp remote offer SDP; iceServers ICE server description string.
 * Return: 0 on success, <0 on failure.
 */
int webrtc_streamer_matter_handleProvideOffer(uint16_t sessionId,char* sdp,char* iceServers);

/*
 * Handle Matter ProvideAnswer.
 * Params: sessionId Matter session ID; sdp remote answer SDP.
 * Return: 0 on success, <0 on failure.
 */
int webrtc_streamer_matter_handleProvideAnswer(uint16_t sessionId,char* sdp);

/*
 * Handle a Matter ICE candidate.
 * Params: sessionId Matter session ID; icecandidate ICE candidate string.
 * Return: 0 on success, <0 on failure.
 */
int webrtc_streamer_matter_handleProvideICECandidate(uint16_t sessionId,char* icecandidate);

/*
 * Close a Matter session.
 * Param: sessionId Matter session ID.
 * Return: 0 on success, <0 on failure.
 */
int webrtc_streamer_matter_close_session(uint16_t sessionId);

/* ---------------------------------------------------------------------------
 * Playback and DataChannel APIs
 * ---------------------------------------------------------------------------
 */

/*
 * Input playback audio data.
 * Params: sessionId session ID; data audio data pointer; len audio length.
 * Return: 0 on success, <0 on failure.
 */
int webrtc_streamer_player_input_audio_data(char *sessionId,unsigned char *data,size_t len);

/*
 * Input playback video data.
 * Params: code_type video codec type; sessionId session ID; data video pointer; len video length.
 * Return: 0 on success, <0 on failure.
 */
int webrtc_streamer_player_input_video_data(webrtc_video_code_type_t code_type,char *sessionId,unsigned char *data,size_t len);

/*
 * Clear the send buffer of a playback session.
 * Params: sessionId session ID; sessionId_len session ID length.
 * Return: 0 on success, <0 on failure.
 */
int webrtc_streamer_player_clean_send_buffer(char *sessionId,size_t sessionId_len);

/*
 * Create a DataChannel.
 * Params: sessionId session ID; streamid DataChannel stream ID; dcname label; dclen label length.
 * Return: 0 on success, <0 on failure.
 */
int webrtc_streamer_add_datachannel(char *sessionId,int streamid,char *dcname,size_t dclen);

/*
 * Send a message through a DataChannel.
 * Params: sessionId session ID; type message type; streamid DataChannel stream ID; data payload; len payload length.
 * Return: 0 on success, <0 on failure.
 */
int webrtc_streamer_datachannel_send_message(char *sessionId,webrtc_data_message_type_t type,int streamid,char *data,size_t len);

/*
 * Get runtime information of a session.
 * Params: sessionId session ID; sessioninfo output structure pointer.
 * Return: 0 on success, <0 on failure.
 */
int webrtc_streamer_get_session_info(char *sessionId,webrtc_streamer_session_info *sessioninfo);

/* ---------------------------------------------------------------------------
 * Audio mixer and publish APIs
 * ---------------------------------------------------------------------------
 */

/*
 * Set the mute state of a session in the audio mixer.
 * Params: sessionId session ID; mute 1 to mute, 0 to unmute.
 * Return: 0 on success, <0 on failure.
 */
int webrtc_streamer_set_audio_mixer_session_mute(char *sessionId,int mute);

/*
 * Get the mute state of a session in the audio mixer.
 * Param: sessionId session ID.
 * Return: mute state or an error code.
 */
int webrtc_streamer_get_audio_mixer_session_mute(char *sessionId);

/*
 * Set the global mixer mute state.
 * Param: mute 1 to mute, 0 to unmute.
 * Return: 0 on success, <0 on failure.
 */
int webrtc_streamer_set_audio_mixer_mute(int mute);

/*
 * Get the global mixer mute state.
 * Return: mute state or an error code.
 */
int webrtc_streamer_get_audio_mixer_mute(void);

/*
 * Publish a message to subscribed clients.
 * Params: data message pointer; len message length.
 * Return: 0 on success, <0 on failure.
 */
int webrtc_streamer_publish_message(char *data,size_t len);

/* ---------------------------------------------------------------------------
 * Built-in WebServer APIs
 * ---------------------------------------------------------------------------
 */

/*
 * Start the built-in WebServer.
 * Params:
 *   rootpath    root directory for static resources.
 *   certpath    certificate file path.
 *   certkeypath private key file path.
 *   https_port  HTTPS port.
 *   http_port   HTTP port.
 * Return: 0 on success, <0 on failure.
 */
int webrtc_streamer_webserver_start(char *rootpath,char *certpath,char *certkeypath,int https_port,int http_port);

/*
 * Stop the built-in WebServer.
 * Return: 0 on success, <0 on failure.
 */
int webrtc_streamer_webserver_stop(void);

/* ---------------------------------------------------------------------------
 * Outgoing call and extended signaling APIs
 * ---------------------------------------------------------------------------
 */

/*
 * Start an outgoing call.
 * Params:
 *   type          stream channel type.
 *   sessionId     session ID.
 *   sessionId_len session ID length.
 *   to            remote peer identifier.
 *   to_len        remote peer identifier length.
 *   audio         audio capability or configuration string.
 *   video         video capability or configuration string.
 *   datachennel   whether to enable DataChannel.
 *   user          user name or extra user parameter.
 *   pwd           password or authentication parameter.
 * Return: 0 on success, <0 on failure.
 */
int webrtc_streamer_call(webrtc_stream_type_t type,char *sessionId,size_t sessionId_len,char * to,size_t to_len,char *audio,char *video,int datachennel,char *user,char *pwd);

/*
 * Send an extended signaling message, even when the local session is not yet created.
 * Params: sessionId session ID; sessionType session type; to remote peer identifier; data message pointer; len message length.
 * Return: 0 on success, <0 on failure.
 */
int webrtc_streamer_send_message_ex(char *sessionId,char* sessionType,char*to,char *data,size_t len);

/* ---------------------------------------------------------------------------
 * Cloud publish APIs
 * ---------------------------------------------------------------------------
 */

/*
 * Publish a file to cloud storage.
 * Params:
 *   sessionId          session ID.
 *   sessionId_len      session ID length.
 *   filepath           local directory path.
 *   filepath_len       local directory path length.
 *   filename           local file name.
 *   filename_len       local file name length.
 *   cloud_filename     destination cloud file name.
 *   cloud_filename_len destination cloud file name length.
 *   speed              upload speed mode.
 *   event              business event type or extra tag.
 * Return: 0 on success, <0 on failure.
 */
int webrtc_streamer_cloud_publish_file(char *sessionId,size_t sessionId_len,char *filepath,size_t filepath_len,char *filename,size_t filename_len,char *cloud_filename,size_t cloud_filename_len,webrtc_clouduploadspeed_t speed,int event);

/*
 * Publish a realtime stream to the cloud.
 * Params: sessionId session ID; sessionId_len session ID length; type stream channel to publish; event business event type or extra tag.
 * Return: 0 on success, <0 on failure.
 */
int webrtc_streamer_cloud_publish_realtime_stream(char *sessionId,size_t sessionId_len,webrtc_stream_type_t type,int event);

/* ---------------------------------------------------------------------------
 * ---------------------------------------------------------------------------
 */

/*
 * Publish a realtime stream using WHIP.
 * Params:
 *   sessionId     session ID.
 *   sessionId_len session ID length.
 *   type          stream channel to publish.
 *   url           WHIP service URL.
 *   url_len       URL length.
 *   headers       extra HTTP headers.
 *   headers_len   extra HTTP headers length.
 *   iceServer     ICE server description string.
 * Return: 0 on success, <0 on failure.
 */
int webrtc_streamer_whip_publish_realtime_stream(char *sessionId,size_t sessionId_len,webrtc_stream_type_t type,char *url,size_t url_len,char *headers,size_t headers_len,char *iceServer);

#ifdef __cplusplus
}
#endif

#endif
