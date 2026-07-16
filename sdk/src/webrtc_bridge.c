#include <ctype.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <pthread.h>
#include <unistd.h>
#include <errno.h>
#include <arpa/inet.h>
#include <net/if.h>
#include <sys/ioctl.h>
#include <sys/socket.h>

#include "sample_comm.h"
#include "ss_mpi_venc.h"
#include "webrtc_streamer.h"
#include "webrtc_bridge.h"

/*
 * Connect the board-side VENC stream and device configuration to the external
 * WebRTC Streamer SDK. SDK callbacks, the video sender, and the main thread may
 * access session state concurrently. g_webrtc_bridge_lock protects callback-
 * updated shared fields, and expensive SDK calls run without holding the lock.
 */
#define WEBRTC_LOG_ERROR(fmt, ...) printf("[WEBRTC][ERROR] " fmt "\n", ##__VA_ARGS__)
#define WEBRTC_LOG_WARN(fmt, ...)  printf("[WEBRTC][WARN] " fmt "\n", ##__VA_ARGS__)
#define WEBRTC_LOG_INFO(fmt, ...)  printf("[WEBRTC] " fmt "\n", ##__VA_ARGS__)

#define WEBRTC_CONFIG_PATH "/mnt/webrtc/webcam.conf"
#define WEBRTC_RUNTIME_CONFIG_PATH "/opt/alexa"
#define WEBRTC_CACHE_PATH "/mnt/webrtc/webrtc.configuration"
#define WEBRTC_WWW_PATH "/mnt/webrtc/www"
#define WEBRTC_CERT_PATH "/mnt/webrtc/cert.pem"
#define WEBRTC_KEY_PATH "/mnt/webrtc/priv.key"

/* The bridge tracks one active session and caches recent state for diagnostics. */
typedef struct {
    td_bool enabled;
    td_bool initialized;
    td_bool webserver_started;
    ot_venc_chn request_idr_chn;
    webrtc_video_code_type_t codec_type;
    webrtc_stream_type_t stream_type;
    char serial_number[128];
    char customerserno[64];
    char active_session_id[64];
    td_bool waiting_keyframe;
    td_bool session_video_ready;
    td_u32 dropped_non_keyframes;
    td_s32 last_signaling_error;
    char last_signaling_error_text[128];
    webrtc_network_quality_type_t last_network_quality;
    char last_call_state[64];
    td_s32 last_send_queue;
    td_s32 last_send_queue_bufsize;
    td_u8 *codec_header;
    td_u32 codec_header_len;
} webrtc_bridge_state;

static webrtc_bridge_state g_webrtc_bridge = {0};
static pthread_mutex_t g_webrtc_bridge_lock = PTHREAD_MUTEX_INITIALIZER;

static td_void webrtc_bridge_request_idr(const char *reason);
static td_void webrtc_bridge_close_session(const char *session_id, const char *reason);

static td_bool webrtc_file_exists(const char *path)
{
    return (path != NULL && access(path, R_OK) == 0) ? TD_TRUE : TD_FALSE;
}

static td_void webrtc_bridge_copy_string(char *dst, size_t dst_len, const char *src)
{
    if (dst == NULL || dst_len == 0) {
        return;
    }

    if (src == NULL) {
        dst[0] = '\0';
        return;
    }

    (td_void)snprintf(dst, dst_len, "%s", src);
}

static char *webrtc_read_text_file(const char *path)
{
    FILE *fp = NULL;
    long file_size;
    char *buffer = NULL;

    fp = fopen(path, "rb");
    if (fp == NULL) {
        return NULL;
    }

    if (fseek(fp, 0, SEEK_END) != 0) {
        fclose(fp);
        return NULL;
    }

    file_size = ftell(fp);
    if (file_size < 0) {
        fclose(fp);
        return NULL;
    }

    if (fseek(fp, 0, SEEK_SET) != 0) {
        fclose(fp);
        return NULL;
    }

    buffer = (char *)calloc((size_t)file_size + 1, 1);
    if (buffer == NULL) {
        fclose(fp);
        return NULL;
    }

    if (file_size > 0 && fread(buffer, 1, (size_t)file_size, fp) != (size_t)file_size) {
        free(buffer);
        fclose(fp);
        return NULL;
    }

    fclose(fp);
    return buffer;
}

static char *webrtc_bridge_read_runtime_configuration(const char **path_used)
{
    char *text = NULL;

    if (path_used != NULL) {
        *path_used = NULL;
    }

    /* Platform-provided configuration takes precedence over the persistent cache. */
    text = webrtc_read_text_file(WEBRTC_RUNTIME_CONFIG_PATH);
    if (text != NULL) {
        if (path_used != NULL) {
            *path_used = WEBRTC_RUNTIME_CONFIG_PATH;
        }
        return text;
    }

    text = webrtc_read_text_file(WEBRTC_CACHE_PATH);
    if (text != NULL && path_used != NULL) {
        *path_used = WEBRTC_CACHE_PATH;
    }

    return text;
}

static td_void webrtc_bridge_normalize_serveraddr(char *serveraddr, size_t serveraddr_len)
{
    char *port_sep;
    char normalized[128];
    size_t host_len;

    if (serveraddr == NULL || serveraddr_len == 0 || serveraddr[0] == '\0') {
        return;
    }

    /* The SDK initialization expects a host name; accept the common host:port form. */
    port_sep = strchr(serveraddr, ':');
    if (port_sep == NULL || strchr(port_sep + 1, ':') != NULL) {
        return;
    }

    host_len = (size_t)(port_sep - serveraddr);
    if (host_len == 0 || host_len >= sizeof(normalized)) {
        return;
    }

    memcpy(normalized, serveraddr, host_len);
    normalized[host_len] = '\0';
    WEBRTC_LOG_INFO("normalize serveraddr: %s -> %s", serveraddr, normalized);
    webrtc_bridge_copy_string(serveraddr, serveraddr_len, normalized);
}

static td_s32 webrtc_write_binary_file(const char *path, const char *data, size_t len)
{
    FILE *fp = NULL;

    fp = fopen(path, "wb");
    if (fp == NULL) {
        return TD_FAILURE;
    }

    if (len > 0 && fwrite(data, 1, len, fp) != len) {
        fclose(fp);
        return TD_FAILURE;
    }

    fclose(fp);
    return TD_SUCCESS;
}

static td_bool webrtc_extract_json_string(const char *json, const char *key, char *out, size_t out_len)
{
    char pattern[64];
    const char *pos;
    const char *start;
    const char *end;
    size_t copy_len;

    if (json == NULL || key == NULL || out == NULL || out_len == 0) {
        return TD_FALSE;
    }

    snprintf(pattern, sizeof(pattern), "\"%s\"", key);
    pos = strstr(json, pattern);
    if (pos == NULL) {
        return TD_FALSE;
    }

    pos = strchr(pos + strlen(pattern), ':');
    if (pos == NULL) {
        return TD_FALSE;
    }

    pos++;
    while (*pos != '\0' && isspace((unsigned char)*pos)) {
        pos++;
    }

    if (*pos != '"') {
        return TD_FALSE;
    }

    start = pos + 1;
    end = strchr(start, '"');
    if (end == NULL) {
        return TD_FALSE;
    }

    copy_len = (size_t)(end - start);
    if (copy_len >= out_len) {
        copy_len = out_len - 1;
    }

    memcpy(out, start, copy_len);
    out[copy_len] = '\0';
    return TD_TRUE;
}

static td_s32 webrtc_extract_json_int(const char *json, const char *key, td_s32 default_value)
{
    char pattern[64];
    const char *pos;

    if (json == NULL || key == NULL) {
        return default_value;
    }

    snprintf(pattern, sizeof(pattern), "\"%s\"", key);
    pos = strstr(json, pattern);
    if (pos == NULL) {
        return default_value;
    }

    pos = strchr(pos + strlen(pattern), ':');
    if (pos == NULL) {
        return default_value;
    }

    pos++;
    while (*pos != '\0' && isspace((unsigned char)*pos)) {
        pos++;
    }

    return (td_s32)strtol(pos, NULL, 10);
}

static td_bool webrtc_bridge_get_ifconfig_value(const char *ifname, int request, char *out, size_t out_len)
{
    int fd;
    struct ifreq ifr;
    struct sockaddr_in *addr;
    td_bool ok = TD_FALSE;

    if (ifname == NULL || out == NULL || out_len == 0) {
        return TD_FALSE;
    }

    fd = socket(AF_INET, SOCK_DGRAM, 0);
    if (fd < 0) {
        return TD_FALSE;
    }

    memset(&ifr, 0, sizeof(ifr));
    (void)snprintf(ifr.ifr_name, sizeof(ifr.ifr_name), "%s", ifname);

    if (ioctl(fd, request, &ifr) == 0) {
        addr = (struct sockaddr_in *)&ifr.ifr_addr;
        if (inet_ntop(AF_INET, &addr->sin_addr, out, out_len) != NULL) {
            ok = TD_TRUE;
        }
    }

    close(fd);
    return ok;
}

static td_void webrtc_bridge_fill_network_info(char *ip, size_t ip_len, char *gw, size_t gw_len,
    char *mask, size_t mask_len)
{
    static const char *ifnames[] = {"eth0", "eth1", "end0", "enp0s0", "wlan0"};
    char runtime_ifname[IFNAMSIZ] = {0};
    FILE *runtime_iface;
    size_t i;

    if (ip != NULL && ip_len > 0) {
        ip[0] = '\0';
    }
    if (gw != NULL && gw_len > 0) {
        gw[0] = '\0';
    }
    if (mask != NULL && mask_len > 0) {
        mask[0] = '\0';
    }

    /* Prefer the interface selected by the boot script, then scan common names. */
    runtime_iface = fopen("/tmp/current_iface", "r");
    if (runtime_iface != NULL) {
        if (fgets(runtime_ifname, sizeof(runtime_ifname), runtime_iface) != NULL) {
            runtime_ifname[strcspn(runtime_ifname, "\r\n")] = '\0';
        }
        fclose(runtime_iface);
    }

    if (runtime_ifname[0] != '\0') {
        td_bool have_ip = TD_FALSE;
        td_bool have_mask = TD_FALSE;

        if (ip != NULL && ip_len > 0) {
            have_ip = webrtc_bridge_get_ifconfig_value(runtime_ifname, SIOCGIFADDR, ip, ip_len);
        }
        if (mask != NULL && mask_len > 0) {
            have_mask = webrtc_bridge_get_ifconfig_value(runtime_ifname, SIOCGIFNETMASK, mask, mask_len);
        }
        if (have_ip == TD_TRUE || have_mask == TD_TRUE) {
            return;
        }
    }

    for (i = 0; i < sizeof(ifnames) / sizeof(ifnames[0]); ++i) {
        td_bool have_ip = TD_FALSE;
        td_bool have_mask = TD_FALSE;

        if (ip != NULL && ip_len > 0) {
            have_ip = webrtc_bridge_get_ifconfig_value(ifnames[i], SIOCGIFADDR, ip, ip_len);
        }
        if (mask != NULL && mask_len > 0) {
            have_mask = webrtc_bridge_get_ifconfig_value(ifnames[i], SIOCGIFNETMASK, mask, mask_len);
        }

        if (have_ip == TD_TRUE || have_mask == TD_TRUE) {
            break;
        }
    }
}

static td_void webrtc_bridge_get_network_info_callback(char *ip, char *gw, char *mask, void *user)
{
    ot_unused(user);
    webrtc_bridge_fill_network_info(ip, 64, gw, 64, mask, 64);
}

static td_bool webrtc_bridge_parse_named_value(const char *text, const char *key, char *out, size_t out_len)
{
    const char *pos;
    size_t key_len;

    if (text == NULL || key == NULL || out == NULL || out_len == 0) {
        return TD_FALSE;
    }

    key_len = strlen(key);
    pos = strstr(text, key);
    if (pos == NULL) {
        return TD_FALSE;
    }

    pos += key_len;
    while (*pos != '\0' && (*pos == ' ' || *pos == '\t' || *pos == '"' || *pos == ':' || *pos == '=')) {
        pos++;
    }

    {
        const char *end = pos;
        size_t copy_len;

        while (*end != '\0' && *end != '"' && *end != '\n' && *end != '\r' && *end != ',' && *end != '}') {
            end++;
        }

        copy_len = (size_t)(end - pos);
        if (copy_len == 0) {
            return TD_FALSE;
        }
        if (copy_len >= out_len) {
            copy_len = out_len - 1;
        }

        memcpy(out, pos, copy_len);
        out[copy_len] = '\0';
    }

    return TD_TRUE;
}

static td_s32 webrtc_bridge_get_active_session_copy(char *session_id, size_t session_id_len)
{
    if (session_id == NULL || session_id_len == 0) {
        return TD_FAILURE;
    }

    pthread_mutex_lock(&g_webrtc_bridge_lock);
    webrtc_bridge_copy_string(session_id, session_id_len, g_webrtc_bridge.active_session_id);
    pthread_mutex_unlock(&g_webrtc_bridge_lock);

    return (session_id[0] != '\0') ? TD_SUCCESS : TD_FAILURE;
}

static size_t webrtc_bridge_format_status_json(char *buf, size_t buf_len)
{
    char session_id[64];
    char ip[64];
    char gw[64];
    char mask[64];
    td_bool enabled;
    td_bool waiting_keyframe;
    td_bool session_video_ready;
    td_u32 dropped_non_keyframes;
    td_s32 last_signaling_error;
    webrtc_network_quality_type_t last_network_quality;
    td_s32 last_send_queue;
    td_s32 last_send_queue_bufsize;
    char last_signaling_error_text[128];
    char last_call_state[64];

    if (buf == NULL || buf_len == 0) {
        return 0;
    }

    /* Copy a snapshot under the lock; format JSON and query interfaces outside it. */
    pthread_mutex_lock(&g_webrtc_bridge_lock);
    enabled = g_webrtc_bridge.enabled;
    waiting_keyframe = g_webrtc_bridge.waiting_keyframe;
    session_video_ready = g_webrtc_bridge.session_video_ready;
    dropped_non_keyframes = g_webrtc_bridge.dropped_non_keyframes;
    last_signaling_error = g_webrtc_bridge.last_signaling_error;
    last_network_quality = g_webrtc_bridge.last_network_quality;
    last_send_queue = g_webrtc_bridge.last_send_queue;
    last_send_queue_bufsize = g_webrtc_bridge.last_send_queue_bufsize;
    webrtc_bridge_copy_string(session_id, sizeof(session_id), g_webrtc_bridge.active_session_id);
    webrtc_bridge_copy_string(last_signaling_error_text, sizeof(last_signaling_error_text),
        g_webrtc_bridge.last_signaling_error_text);
    webrtc_bridge_copy_string(last_call_state, sizeof(last_call_state), g_webrtc_bridge.last_call_state);
    pthread_mutex_unlock(&g_webrtc_bridge_lock);

    webrtc_bridge_fill_network_info(ip, sizeof(ip), gw, sizeof(gw), mask, sizeof(mask));

    return (size_t)snprintf(buf, buf_len,
        "{\"enabled\":%s,\"sessionId\":\"%s\",\"waitingKeyframe\":%s,"
        "\"sessionVideoReady\":%s,\"droppedNonKeyframes\":%u,\"codecType\":%d,\"streamType\":%d,"
        "\"lastCallState\":\"%s\",\"lastSignalingError\":%d,\"lastSignalingErrorText\":\"%s\","
        "\"lastNetworkQuality\":%d,\"lastSendQueue\":%d,\"lastSendQueueBufsize\":%d,"
        "\"ip\":\"%s\",\"gateway\":\"%s\",\"netmask\":\"%s\"}",
        enabled == TD_TRUE ? "true" : "false",
        session_id,
        waiting_keyframe == TD_TRUE ? "true" : "false",
        session_video_ready == TD_TRUE ? "true" : "false",
        dropped_non_keyframes, g_webrtc_bridge.codec_type, g_webrtc_bridge.stream_type,
        last_call_state, last_signaling_error, last_signaling_error_text,
        (int)last_network_quality, last_send_queue, last_send_queue_bufsize,
        ip, gw, mask);
}

static td_s32 webrtc_bridge_get_session_info_json(const char *session_id, char *buf, size_t buf_len)
{
    webrtc_streamer_session_info info;
    int ret;

    if (session_id == NULL || session_id[0] == '\0' || buf == NULL || buf_len == 0) {
        return TD_FAILURE;
    }

    memset(&info, 0, sizeof(info));
    ret = webrtc_streamer_get_session_info((char *)session_id, &info);
    if (ret != 0) {
        return TD_FAILURE;
    }

    (void)snprintf(buf, buf_len,
        "{\"sessionId\":\"%s\",\"startStreamType\":%d,\"streamType\":%d,"
        "\"mode\":\"%s\",\"connectTime\":%d,\"sessionTime\":%d,"
        "\"videoSendBitrate\":%d,\"audioSendBitrate\":%d,\"sendPackets\":%d,"
        "\"resendPackets\":%d,\"currentResendPackets\":%d,\"videoRecvBitrate\":%d,"
        "\"audioRecvBitrate\":%d,\"transmissionMode\":%d,\"audioPacketLoss\":%d,"
        "\"videoPacketLoss\":%d,\"audioCurrentPacketLost\":%d,\"videoCurrentPacketLost\":%d,"
        "\"currentBandwidthKps\":%d,\"currentPliCount\":%d,\"sendDataBitrate\":%d,"
        "\"recvDataBitrate\":%d,\"userSendVideo\":%d,\"userSendAudio\":%d}",
        session_id, info.start_stream_type, info.stream_type,
        info.szmode, info.connect_time, info.session_time,
        info.video_send_bitrate, info.audio_send_bitrate, info.send_packets,
        info.resend_packets, info.current_resend_packets, info.video_recv_bitrate,
        info.audio_recv_bitrate, info.transmission_mode, info.audio_packet_loss,
        info.video_packet_loss, info.audio_current_packet_lost, info.video_current_packet_lost,
        info.current_bandwidth_kps, info.current_pli_count, info.send_data_bitrate,
        info.recv_data_bitrate, info.user_send_video, info.user_send_audio);
    return TD_SUCCESS;
}

static td_s32 webrtc_bridge_send_json_datachannel(const char *session_id, int stream_id, const char *json_text)
{
    if (session_id == NULL || session_id[0] == '\0' || json_text == NULL) {
        return TD_FAILURE;
    }

    return webrtc_streamer_datachannel_send_message((char *)session_id, WEBRTC_DMT_TEXT, stream_id,
        (char *)json_text, strlen(json_text));
}

static td_s32 webrtc_bridge_event_call_by_name(const char *name)
{
    if (name == NULL) {
        return TD_FAILURE;
    }

    if (strcmp(name, "doorbell") == 0) {
        return webrtc_streamer_event_call(WEBRTC_CALL_EVENT_DOORBELL_PRESS);
    }
    if (strcmp(name, "pir") == 0) {
        return webrtc_streamer_event_call(WEBRTC_CALL_EVENT_PIR);
    }
    if (strcmp(name, "custom") == 0) {
        return webrtc_streamer_event_call(WEBRTC_CALL_EVENT_CUSTOM);
    }

    return TD_FAILURE;
}

static td_s32 webrtc_bridge_execute_command(const char *session_id, const char *req_msg,
    char *rsp_msg, size_t rsp_msg_capacity, size_t *rsp_msg_len)
{
    char active_session_id[64];
    char command[64];
    char value[128];
    const char *target_session;
    td_s32 ret;

    if (rsp_msg == NULL || rsp_msg_len == NULL || rsp_msg_capacity == 0) {
        return TD_FAILURE;
    }

    rsp_msg[0] = '\0';
    *rsp_msg_len = 0;

    /* DataChannel, WebSocket, and HTTP API requests share this command dispatcher. */
    target_session = session_id;
    if (target_session == NULL || target_session[0] == '\0') {
        if (webrtc_bridge_get_active_session_copy(active_session_id, sizeof(active_session_id)) == TD_SUCCESS) {
            target_session = active_session_id;
        } else {
            target_session = "";
        }
    }

    if (req_msg == NULL || req_msg[0] == '\0') {
        *rsp_msg_len = (size_t)snprintf(rsp_msg, rsp_msg_capacity,
            "{\"ok\":true,\"message\":\"empty request\"}");
        return TD_SUCCESS;
    }

    if (strcmp(req_msg, "ping") == 0) {
        *rsp_msg_len = (size_t)snprintf(rsp_msg, rsp_msg_capacity,
            "{\"ok\":true,\"event\":\"pong\"}");
        return TD_SUCCESS;
    }

    if (strcmp(req_msg, "get_status") == 0) {
        *rsp_msg_len = webrtc_bridge_format_status_json(rsp_msg, rsp_msg_capacity);
        return TD_SUCCESS;
    }

    if (strcmp(req_msg, "get_session_info") == 0) {
        ret = webrtc_bridge_get_session_info_json(target_session, rsp_msg, rsp_msg_capacity);
        if (ret == TD_SUCCESS) {
            *rsp_msg_len = strlen(rsp_msg);
            return TD_SUCCESS;
        }
        *rsp_msg_len = (size_t)snprintf(rsp_msg, rsp_msg_capacity,
            "{\"ok\":false,\"error\":\"session info unavailable\"}");
        return TD_FAILURE;
    }

    if (strcmp(req_msg, "request_iframe") == 0) {
        webrtc_bridge_request_idr("command");
        *rsp_msg_len = (size_t)snprintf(rsp_msg, rsp_msg_capacity,
            "{\"ok\":true,\"event\":\"request_iframe\"}");
        return TD_SUCCESS;
    }

    if (strcmp(req_msg, "close_session") == 0) {
        if (target_session[0] == '\0') {
            *rsp_msg_len = (size_t)snprintf(rsp_msg, rsp_msg_capacity,
                "{\"ok\":false,\"error\":\"no active session\"}");
            return TD_FAILURE;
        }
        webrtc_bridge_close_session(target_session, "command");
        *rsp_msg_len = (size_t)snprintf(rsp_msg, rsp_msg_capacity,
            "{\"ok\":true,\"event\":\"close_session\",\"sessionId\":\"%s\"}", target_session);
        return TD_SUCCESS;
    }

    if (strncmp(req_msg, "event:", 6) == 0) {
        ret = webrtc_bridge_event_call_by_name(req_msg + 6);
        *rsp_msg_len = (size_t)snprintf(rsp_msg, rsp_msg_capacity,
            "{\"ok\":%s,\"event\":\"%s\"}", ret == 0 ? "true" : "false", req_msg + 6);
        return ret == 0 ? TD_SUCCESS : TD_FAILURE;
    }

    if (strncmp(req_msg, "stream:", 7) == 0) {
        if (target_session[0] == '\0') {
            *rsp_msg_len = (size_t)snprintf(rsp_msg, rsp_msg_capacity,
                "{\"ok\":false,\"error\":\"no active session\"}");
            return TD_FAILURE;
        }

        if (strcmp(req_msg + 7, "main") == 0) {
            ret = webrtc_streamer_set_session_streamtype((char *)target_session, WEBRTC_STREAM_MAIN);
        } else if (strcmp(req_msg + 7, "sub") == 0) {
            ret = webrtc_streamer_set_session_streamtype((char *)target_session, WEBRTC_STREAM_SUB);
        } else {
            ret = -1;
        }

        *rsp_msg_len = (size_t)snprintf(rsp_msg, rsp_msg_capacity,
            "{\"ok\":%s,\"stream\":\"%s\",\"sessionId\":\"%s\"}",
            ret == 0 ? "true" : "false", req_msg + 7, target_session);
        return ret == 0 ? TD_SUCCESS : TD_FAILURE;
    }

    memset(command, 0, sizeof(command));
    memset(value, 0, sizeof(value));

    if (webrtc_bridge_parse_named_value(req_msg, "\"cmd\"", command, sizeof(command)) == TD_FALSE &&
        webrtc_bridge_parse_named_value(req_msg, "cmd", command, sizeof(command)) == TD_FALSE) {
        *rsp_msg_len = (size_t)snprintf(rsp_msg, rsp_msg_capacity,
            "{\"ok\":false,\"error\":\"unknown command\"}");
        return TD_FAILURE;
    }

    if (strcmp(command, "ping") == 0) {
        *rsp_msg_len = (size_t)snprintf(rsp_msg, rsp_msg_capacity,
            "{\"ok\":true,\"event\":\"pong\"}");
        return TD_SUCCESS;
    }

    if (strcmp(command, "get_status") == 0) {
        *rsp_msg_len = webrtc_bridge_format_status_json(rsp_msg, rsp_msg_capacity);
        return TD_SUCCESS;
    }

    if (strcmp(command, "get_session_info") == 0) {
        ret = webrtc_bridge_get_session_info_json(target_session, rsp_msg, rsp_msg_capacity);
        if (ret == TD_SUCCESS) {
            *rsp_msg_len = strlen(rsp_msg);
            return TD_SUCCESS;
        }
        *rsp_msg_len = (size_t)snprintf(rsp_msg, rsp_msg_capacity,
            "{\"ok\":false,\"error\":\"session info unavailable\"}");
        return TD_FAILURE;
    }

    if (strcmp(command, "request_iframe") == 0) {
        webrtc_bridge_request_idr("command_json");
        *rsp_msg_len = (size_t)snprintf(rsp_msg, rsp_msg_capacity,
            "{\"ok\":true,\"event\":\"request_iframe\"}");
        return TD_SUCCESS;
    }

    if (strcmp(command, "close_session") == 0) {
        if (target_session[0] == '\0') {
            *rsp_msg_len = (size_t)snprintf(rsp_msg, rsp_msg_capacity,
                "{\"ok\":false,\"error\":\"no active session\"}");
            return TD_FAILURE;
        }
        webrtc_bridge_close_session(target_session, "command_json");
        *rsp_msg_len = (size_t)snprintf(rsp_msg, rsp_msg_capacity,
            "{\"ok\":true,\"event\":\"close_session\",\"sessionId\":\"%s\"}", target_session);
        return TD_SUCCESS;
    }

    if (strcmp(command, "event_call") == 0) {
        if (webrtc_bridge_parse_named_value(req_msg, "\"event\"", value, sizeof(value)) == TD_FALSE &&
            webrtc_bridge_parse_named_value(req_msg, "event", value, sizeof(value)) == TD_FALSE) {
            *rsp_msg_len = (size_t)snprintf(rsp_msg, rsp_msg_capacity,
                "{\"ok\":false,\"error\":\"missing event\"}");
            return TD_FAILURE;
        }

        ret = webrtc_bridge_event_call_by_name(value);
        *rsp_msg_len = (size_t)snprintf(rsp_msg, rsp_msg_capacity,
            "{\"ok\":%s,\"event\":\"%s\"}", ret == 0 ? "true" : "false", value);
        return ret == 0 ? TD_SUCCESS : TD_FAILURE;
    }

    if (strcmp(command, "switch_stream") == 0) {
        if (target_session[0] == '\0') {
            *rsp_msg_len = (size_t)snprintf(rsp_msg, rsp_msg_capacity,
                "{\"ok\":false,\"error\":\"no active session\"}");
            return TD_FAILURE;
        }

        if (webrtc_bridge_parse_named_value(req_msg, "\"stream\"", value, sizeof(value)) == TD_FALSE &&
            webrtc_bridge_parse_named_value(req_msg, "stream", value, sizeof(value)) == TD_FALSE) {
            *rsp_msg_len = (size_t)snprintf(rsp_msg, rsp_msg_capacity,
                "{\"ok\":false,\"error\":\"missing stream\"}");
            return TD_FAILURE;
        }

        if (strcmp(value, "main") == 0) {
            ret = webrtc_streamer_set_session_streamtype((char *)target_session, WEBRTC_STREAM_MAIN);
        } else if (strcmp(value, "sub") == 0) {
            ret = webrtc_streamer_set_session_streamtype((char *)target_session, WEBRTC_STREAM_SUB);
        } else {
            ret = -1;
        }

        *rsp_msg_len = (size_t)snprintf(rsp_msg, rsp_msg_capacity,
            "{\"ok\":%s,\"stream\":\"%s\",\"sessionId\":\"%s\"}",
            ret == 0 ? "true" : "false", value, target_session);
        return ret == 0 ? TD_SUCCESS : TD_FAILURE;
    }

    *rsp_msg_len = (size_t)snprintf(rsp_msg, rsp_msg_capacity,
        "{\"ok\":false,\"error\":\"unsupported command\",\"cmd\":\"%s\"}", command);
    return TD_FAILURE;
}

static td_void webrtc_bridge_request_idr(const char *reason)
{
    td_s32 ret;

    ret = ss_mpi_venc_request_idr(g_webrtc_bridge.request_idr_chn, TD_TRUE);
    if (ret != TD_SUCCESS) {
        WEBRTC_LOG_ERROR("request IDR failed on chn %d (%s): 0x%x",
            g_webrtc_bridge.request_idr_chn, reason, ret);
    }
}

static td_bool webrtc_bridge_h264_has_nal_type(const td_u8 *data, td_u32 len, td_u8 target_type)
{
    td_u32 i = 0;
    td_bool has_start_code = TD_FALSE;

    /* Support both three-byte and four-byte Annex-B start codes. */
    while (i + 3 < len) {
        td_u32 nal_pos;

        if (data[i] == 0x00 && data[i + 1] == 0x00 && data[i + 2] == 0x01) {
            nal_pos = i + 3;
        } else if (i + 4 < len && data[i] == 0x00 && data[i + 1] == 0x00 &&
            data[i + 2] == 0x00 && data[i + 3] == 0x01) {
            nal_pos = i + 4;
        } else {
            i++;
            continue;
        }

        has_start_code = TD_TRUE;
        if (nal_pos < len && (data[nal_pos] & 0x1f) == target_type) {
            return TD_TRUE;
        }

        i = nal_pos + 1;
    }

    if (has_start_code == TD_FALSE && len > 0 && (data[0] & 0x1f) == target_type) {
        return TD_TRUE;
    }

    return TD_FALSE;
}

static td_bool webrtc_bridge_h264_has_idr(const td_u8 *data, td_u32 len)
{
    return webrtc_bridge_h264_has_nal_type(data, len, 5);
}

static td_bool webrtc_bridge_frame_has_keyframe(const td_u8 *data, td_u32 len)
{
    if (g_webrtc_bridge.codec_type == WEBRTC_VIDEO_H264) {
        return webrtc_bridge_h264_has_idr(data, len);
    }

    return TD_TRUE;
}

static td_s32 webrtc_bridge_input_h264_with_header_if_needed(const td_u8 *data, td_u32 len, td_bool has_keyframe)
{
    td_s32 ret;
    td_u8 *frame_with_header = NULL;
    td_u32 frame_with_header_len;
    td_u8 *codec_header = NULL;
    td_u32 codec_header_len;
    if (has_keyframe == TD_FALSE) {
        return webrtc_streamer_input_video_data(g_webrtc_bridge.stream_type,
            g_webrtc_bridge.codec_type, (unsigned char *)data, len);
    }

    /* Copy a codec-header snapshot to avoid racing set_codec_header() replacement. */
    pthread_mutex_lock(&g_webrtc_bridge_lock);
    codec_header_len = g_webrtc_bridge.codec_header_len;
    if (g_webrtc_bridge.codec_header != NULL && codec_header_len > 0) {
        codec_header = (td_u8 *)malloc(codec_header_len);
        if (codec_header != NULL) {
            memcpy(codec_header, g_webrtc_bridge.codec_header, codec_header_len);
        }
    }
    pthread_mutex_unlock(&g_webrtc_bridge_lock);

    if (codec_header == NULL || codec_header_len == 0) {
        free(codec_header);
        return webrtc_streamer_input_video_data(g_webrtc_bridge.stream_type,
            g_webrtc_bridge.codec_type, (unsigned char *)data, len);
    }

    frame_with_header_len = codec_header_len + len;
    frame_with_header = (td_u8 *)malloc(frame_with_header_len);
    if (frame_with_header == NULL) {
        free(codec_header);
        return webrtc_streamer_input_video_data(g_webrtc_bridge.stream_type,
            g_webrtc_bridge.codec_type, (unsigned char *)data, len);
    }

    /* Prefix keyframes with SPS/PPS so a newly joined client can decode immediately. */
    memcpy(frame_with_header, codec_header, codec_header_len);
    memcpy(frame_with_header + codec_header_len, data, len);
    ret = webrtc_streamer_input_video_data(g_webrtc_bridge.stream_type,
        g_webrtc_bridge.codec_type, frame_with_header, frame_with_header_len);

    free(frame_with_header);
    free(codec_header);
    return ret;
}

static td_void webrtc_bridge_wait_next_keyframe_locked(const char *reason)
{
    /* The caller must hold g_webrtc_bridge_lock. */
    g_webrtc_bridge.waiting_keyframe = TD_TRUE;
    g_webrtc_bridge.session_video_ready = TD_FALSE;
    g_webrtc_bridge.dropped_non_keyframes = 0;
    WEBRTC_LOG_INFO("wait next keyframe: %s", reason != NULL ? reason : "");
}

static void webrtc_bridge_send_queue_full_callback(char *session_id, size_t session_id_len,
    int queue, int bufsize, void *user)
{
    ot_unused(session_id_len);
    ot_unused(user);

    pthread_mutex_lock(&g_webrtc_bridge_lock);
    g_webrtc_bridge.last_send_queue = queue;
    g_webrtc_bridge.last_send_queue_bufsize = bufsize;
    pthread_mutex_unlock(&g_webrtc_bridge_lock);

    WEBRTC_LOG_WARN("send queue full: session=%s queue=%d bufsize=%d",
        session_id != NULL ? session_id : "", queue, bufsize);
}

static void webrtc_bridge_video_callback(webrtc_stream_type_t stream_type, webrtc_video_code_type_t type,
    char *data, size_t len, char *session_id, size_t session_id_len, void *user)
{
    ot_unused(stream_type);
    ot_unused(type);
    ot_unused(data);
    ot_unused(len);
    ot_unused(session_id);
    ot_unused(session_id_len);
    ot_unused(user);
}

static td_void webrtc_bridge_clear_active_session(const char *session_id)
{
    if (session_id == NULL || session_id[0] == '\0') {
        return;
    }

    if (strcmp(session_id, g_webrtc_bridge.active_session_id) == 0) {
        pthread_mutex_lock(&g_webrtc_bridge_lock);
        g_webrtc_bridge.active_session_id[0] = '\0';
        g_webrtc_bridge.waiting_keyframe = TD_FALSE;
        g_webrtc_bridge.session_video_ready = TD_FALSE;
        g_webrtc_bridge.dropped_non_keyframes = 0;
        g_webrtc_bridge.last_call_state[0] = '\0';
        pthread_mutex_unlock(&g_webrtc_bridge_lock);
        WEBRTC_LOG_INFO("active session cleared: %s", session_id);
    }
}

static td_void webrtc_bridge_set_active_session(const char *session_id, size_t session_id_len)
{
    size_t copy_len;

    if (session_id == NULL || session_id_len == 0) {
        return;
    }

    copy_len = session_id_len;
    if (copy_len >= sizeof(g_webrtc_bridge.active_session_id)) {
        copy_len = sizeof(g_webrtc_bridge.active_session_id) - 1;
    }

    /* A new session waits for a keyframe instead of starting on a P/B frame. */
    pthread_mutex_lock(&g_webrtc_bridge_lock);
    memcpy(g_webrtc_bridge.active_session_id, session_id, copy_len);
    g_webrtc_bridge.active_session_id[copy_len] = '\0';
    g_webrtc_bridge.waiting_keyframe = TD_TRUE;
    g_webrtc_bridge.session_video_ready = TD_FALSE;
    g_webrtc_bridge.dropped_non_keyframes = 0;
    webrtc_bridge_copy_string(g_webrtc_bridge.last_call_state, sizeof(g_webrtc_bridge.last_call_state), "incoming");
    pthread_mutex_unlock(&g_webrtc_bridge_lock);
    WEBRTC_LOG_INFO("active session set: %s", g_webrtc_bridge.active_session_id);
}

static td_void webrtc_bridge_close_session(const char *session_id, const char *reason)
{
    td_s32 ret;
    size_t session_len;

    if (session_id == NULL || session_id[0] == '\0') {
        return;
    }

    session_len = strlen(session_id);
    ret = webrtc_streamer_close_session((char *)session_id, session_len);
    WEBRTC_LOG_INFO("close session: session=%s reason=%s ret=%d",
        session_id, reason != NULL ? reason : "", ret);
}

static td_void webrtc_bridge_request_session_idr(const char *reason)
{
    /* Gate non-keyframes before asking VENC to generate an IDR. */
    pthread_mutex_lock(&g_webrtc_bridge_lock);
    webrtc_bridge_wait_next_keyframe_locked(reason);
    pthread_mutex_unlock(&g_webrtc_bridge_lock);
    webrtc_bridge_request_idr(reason != NULL ? reason : "session_request_idr");
}

static td_void webrtc_bridge_configuration_callback(char *data, size_t len, int reboot)
{
    if (data == NULL) {
        return;
    }

    /* Persist SDK-provided runtime configuration for use as the next startup fallback. */
    if (webrtc_write_binary_file(WEBRTC_CACHE_PATH, data, len) != TD_SUCCESS) {
        WEBRTC_LOG_WARN("failed to persist runtime configuration to %s", WEBRTC_CACHE_PATH);
        return;
    }
    ot_unused(reboot);
}

static int webrtc_bridge_auth_callback(char *authdata, size_t authlen, char *password, size_t pwdlen)
{
    ot_unused(authdata);
    ot_unused(authlen);
    ot_unused(password);
    ot_unused(pwdlen);

    return 1;
}

static void webrtc_bridge_event_callback(webrtc_event_type_t event, void *user, int *result)
{
    ot_unused(user);
    ot_unused(result);

    switch (event) {
        case WEBRTC_EVENT_ASK_IFRAME:
            webrtc_bridge_request_idr("event");
            break;
        case WEBRTC_EVENT_ONLINE:
            WEBRTC_LOG_INFO("online");
            break;
        case WEBRTC_EVENT_OFFLINE:
            WEBRTC_LOG_INFO("offline");
            break;
        case WEBRTC_EVENT_CALL_START:
            WEBRTC_LOG_INFO("call start");
            break;
        case WEBRTC_EVENT_CALL_LINK:
            WEBRTC_LOG_INFO("call link");
            webrtc_bridge_request_session_idr("call_link");
            break;
        case WEBRTC_EVENT_CALL_DISCONNECT:
            WEBRTC_LOG_INFO("call disconnect");
            break;
        default:
            break;
    }
}

static void webrtc_bridge_call_income_callback(char *session_id, size_t session_id_len,
    char *mode, size_t mode_len, char *source, size_t source_len, void *user)
{
    ot_unused(session_id_len);
    ot_unused(mode_len);
    ot_unused(source_len);
    ot_unused(user);

    WEBRTC_LOG_INFO("incoming session=%s mode=%s source=%s",
        session_id != NULL ? session_id : "",
        mode != NULL ? mode : "",
        source != NULL ? source : "");

    if (session_id != NULL && session_id_len > 0) {
        webrtc_bridge_set_active_session(session_id, session_id_len);
        webrtc_bridge_request_session_idr("call_income");
    }
}

static void webrtc_bridge_call_destroy_callback(char *session_id, size_t session_id_len, void *user)
{
    ot_unused(session_id_len);
    ot_unused(user);
    WEBRTC_LOG_INFO("session destroyed: %s", session_id != NULL ? session_id : "");
    webrtc_bridge_clear_active_session(session_id);
}

static void webrtc_bridge_call_disconnect_callback(char *session_id, size_t session_id_len,
    char *message, void *user)
{
    ot_unused(session_id_len);
    ot_unused(user);
    WEBRTC_LOG_INFO("session disconnect: %s reason=%s",
        session_id != NULL ? session_id : "",
        message != NULL ? message : "");
    webrtc_bridge_clear_active_session(session_id);
}

static void webrtc_bridge_call_failed_callback(char *session_id, size_t session_id_len,
    char *message, void *user)
{
    ot_unused(session_id_len);
    ot_unused(user);
    WEBRTC_LOG_WARN("call failed: %s reason=%s",
        session_id != NULL ? session_id : "",
        message != NULL ? message : "");
    webrtc_bridge_clear_active_session(session_id);
}

static void webrtc_bridge_session_ask_iframe_callback(char *session_id, size_t session_id_len, void *user)
{
    ot_unused(session_id);
    ot_unused(session_id_len);
    ot_unused(user);

    webrtc_bridge_request_session_idr("session_ask_iframe");
}

static void webrtc_bridge_session_pli_callback(char *session_id, size_t session_id_len, void *user)
{
    ot_unused(session_id);
    ot_unused(session_id_len);
    ot_unused(user);

    webrtc_bridge_request_session_idr("pli");
}

static void webrtc_bridge_remote_play_start_callback(char *session_id, size_t session_id_len, void *user)
{
    ot_unused(session_id_len);
    ot_unused(user);

    WEBRTC_LOG_INFO("remote play start: %s", session_id != NULL ? session_id : "");
    webrtc_bridge_set_active_session(session_id, session_id_len);
    webrtc_bridge_request_session_idr("remote_play_start");
}

static void webrtc_bridge_callstate_callback(char *session_id, size_t session_id_len, char *state, void *user)
{
    ot_unused(session_id_len);
    ot_unused(user);

    pthread_mutex_lock(&g_webrtc_bridge_lock);
    webrtc_bridge_copy_string(g_webrtc_bridge.last_call_state, sizeof(g_webrtc_bridge.last_call_state), state);
    pthread_mutex_unlock(&g_webrtc_bridge_lock);

    ot_unused(session_id);
}

static int webrtc_bridge_check_videocode_callback(webrtc_stream_type_t stream_type,
    char *session_id, size_t session_id_len, char *session_type, size_t session_type_len,
    char *mode, size_t mode_len, char *source, size_t source_len, void *user)
{
    ot_unused(stream_type);
    ot_unused(session_id);
    ot_unused(session_id_len);
    ot_unused(session_type);
    ot_unused(session_type_len);
    ot_unused(mode);
    ot_unused(mode_len);
    ot_unused(source);
    ot_unused(source_len);
    ot_unused(user);

    return (int)g_webrtc_bridge.codec_type;
}

static void webrtc_bridge_signaling_socket_error_callback(int error, char *szerror, void *user)
{
    ot_unused(user);

    pthread_mutex_lock(&g_webrtc_bridge_lock);
    g_webrtc_bridge.last_signaling_error = error;
    webrtc_bridge_copy_string(g_webrtc_bridge.last_signaling_error_text,
        sizeof(g_webrtc_bridge.last_signaling_error_text), szerror);
    pthread_mutex_unlock(&g_webrtc_bridge_lock);

    WEBRTC_LOG_WARN("signaling socket error: type=%d message=%s", error, szerror != NULL ? szerror : "");
}

static void webrtc_bridge_network_quality_callback(char *session_id, size_t session_id_len,
    webrtc_network_quality_type_t quality, void *user)
{
    ot_unused(session_id_len);
    ot_unused(user);

    pthread_mutex_lock(&g_webrtc_bridge_lock);
    g_webrtc_bridge.last_network_quality = quality;
    pthread_mutex_unlock(&g_webrtc_bridge_lock);

    ot_unused(session_id);
}

static void webrtc_bridge_message_callback(char *session_id, size_t session_id_len,
    char *req_msg, size_t req_msg_len, char *rsp_msg, size_t *rsp_msg_len, void *user)
{
    char request[1024];
    size_t copy_len;

    ot_unused(session_id_len);
    ot_unused(user);

    if (req_msg == NULL || rsp_msg == NULL || rsp_msg_len == NULL) {
        return;
    }

    copy_len = req_msg_len;
    if (copy_len >= sizeof(request)) {
        copy_len = sizeof(request) - 1;
    }
    memcpy(request, req_msg, copy_len);
    request[copy_len] = '\0';

    (td_void)webrtc_bridge_execute_command(session_id, request, rsp_msg, *rsp_msg_len, rsp_msg_len);
    ot_unused(request);
}

static void webrtc_bridge_datachannel_open_callback(char *session_id, size_t session_id_len, int stream_id, void *user)
{
    char status_json[1024];

    ot_unused(session_id_len);
    ot_unused(user);

    if (session_id != NULL && session_id[0] != '\0') {
        (td_void)webrtc_bridge_format_status_json(status_json, sizeof(status_json));
        (td_void)webrtc_bridge_send_json_datachannel(session_id, stream_id, status_json);
    }
}

static void webrtc_bridge_can_add_datachannel_callback(char *session_id, size_t session_id_len,
    int is_create_offer, void *user)
{
    ot_unused(session_id_len);
    ot_unused(user);

    ot_unused(session_id);
    ot_unused(is_create_offer);
}

static void webrtc_bridge_datachannel_message_callback(char *session_id, size_t session_id_len,
    webrtc_data_message_type_t type, int stream_id, char *msg, size_t msg_len, void *user)
{
    char request[1024];
    char response[2048];
    size_t copy_len;
    size_t response_len = 0;
    td_s32 ret;

    ot_unused(session_id_len);
    ot_unused(user);

    if (session_id == NULL || msg == NULL) {
        return;
    }

    copy_len = msg_len;
    if (copy_len >= sizeof(request)) {
        copy_len = sizeof(request) - 1;
    }
    memcpy(request, msg, copy_len);
    request[copy_len] = '\0';

    /* Treat text as control commands and echo binary messages as in the SDK sample. */
    if (type != WEBRTC_DMT_TEXT) {
        ret = webrtc_streamer_datachannel_send_message(session_id, type, stream_id, msg, msg_len);
        if (ret != 0) {
            WEBRTC_LOG_WARN("datachannel binary echo failed: ret=%d", ret);
        }
        return;
    }

    ret = webrtc_bridge_execute_command(session_id, request, response, sizeof(response), &response_len);
    if (response_len > 0) {
        (td_void)webrtc_bridge_send_json_datachannel(session_id, stream_id, response);
    }
    if (ret != TD_SUCCESS) {
        WEBRTC_LOG_WARN("datachannel command failed: session=%s ret=%d", session_id, ret);
    }
}

static void webrtc_bridge_webserver_api_callback(const char *url, size_t url_len,
    const char *req_msg, size_t req_msg_len, char *rsp_msg, size_t *rsp_msg_len, void *user)
{
    ot_unused(url_len);
    ot_unused(req_msg_len);
    ot_unused(user);

    if (url == NULL || rsp_msg == NULL || rsp_msg_len == NULL) {
        return;
    }

    /* The built-in web service exposes identity, status, session data, commands, and Wi-Fi passthrough. */
    if (strcmp(url, "/api/getdevicenumber") == 0) {
        (void)snprintf(rsp_msg, *rsp_msg_len, "{\"devicenumber\":\"%s\"}",
            g_webrtc_bridge.serial_number[0] != '\0' ? g_webrtc_bridge.serial_number : "hi3519dv500-imx335");
        *rsp_msg_len = strlen(rsp_msg);
    } else if (strcmp(url, "/api/webrtc/status") == 0) {
        *rsp_msg_len = webrtc_bridge_format_status_json(rsp_msg, *rsp_msg_len);
    } else if (strcmp(url, "/api/webrtc/sessioninfo") == 0) {
        char session_id[64];

        if (webrtc_bridge_get_active_session_copy(session_id, sizeof(session_id)) == TD_SUCCESS &&
            webrtc_bridge_get_session_info_json(session_id, rsp_msg, *rsp_msg_len) == TD_SUCCESS) {
            *rsp_msg_len = strlen(rsp_msg);
        } else {
            *rsp_msg_len = (size_t)snprintf(rsp_msg, *rsp_msg_len,
                "{\"ok\":false,\"error\":\"session info unavailable\"}");
        }
    } else if (strcmp(url, "/api/webrtc/command") == 0 && req_msg != NULL) {
        char request[1024];
        size_t copy_len = req_msg_len;

        if (copy_len >= sizeof(request)) {
            copy_len = sizeof(request) - 1;
        }
        memcpy(request, req_msg, copy_len);
        request[copy_len] = '\0';
        (td_void)webrtc_bridge_execute_command(NULL, request, rsp_msg, *rsp_msg_len, rsp_msg_len);
    } else if (strcmp(url, "/api/wifi/config") == 0 && req_msg != NULL) {
        size_t copy_len = req_msg_len;
        if (copy_len >= *rsp_msg_len) {
            copy_len = *rsp_msg_len - 1;
        }
        memcpy(rsp_msg, req_msg, copy_len);
        rsp_msg[copy_len] = '\0';
        *rsp_msg_len = copy_len;
    } else {
        *rsp_msg_len = 0;
    }
}

static void webrtc_bridge_webserver_websocket_callback(char *session_id, size_t session_id_len,
    char *req_msg, size_t req_msg_len, char *rsp_msg, size_t *rsp_msg_len, void *user)
{
    char request[1024];

    ot_unused(session_id);
    ot_unused(session_id_len);
    ot_unused(user);

    if (req_msg == NULL || rsp_msg == NULL || rsp_msg_len == NULL) {
        return;
    }

    if (req_msg_len >= *rsp_msg_len) {
        req_msg_len = *rsp_msg_len - 1;
    }

    if (req_msg_len >= sizeof(request)) {
        req_msg_len = sizeof(request) - 1;
    }

    memcpy(request, req_msg, req_msg_len);
    request[req_msg_len] = '\0';
    (td_void)webrtc_bridge_execute_command(session_id, request, rsp_msg, *rsp_msg_len, rsp_msg_len);
}

static td_s32 webrtc_bridge_start_webserver(const char *conf_text)
{
    char web_root[256] = WEBRTC_WWW_PATH;
    char cert_path[256] = WEBRTC_CERT_PATH;
    char key_path[256] = WEBRTC_KEY_PATH;
    td_s32 https_port = 8443;
    td_s32 http_port = 6888;
    td_s32 ret;

    if (conf_text != NULL) {
        (td_void)webrtc_extract_json_string(conf_text, "web_root", web_root, sizeof(web_root));
        (td_void)webrtc_extract_json_string(conf_text, "web_cert", cert_path, sizeof(cert_path));
        (td_void)webrtc_extract_json_string(conf_text, "web_key", key_path, sizeof(key_path));
        https_port = webrtc_extract_json_int(conf_text, "https_port", https_port);
        http_port = webrtc_extract_json_int(conf_text, "http_port", http_port);
    }

    /* Missing web assets or certificates disable only the optional web service. */
    if (webrtc_file_exists(web_root) == TD_FALSE ||
        webrtc_file_exists(cert_path) == TD_FALSE ||
        webrtc_file_exists(key_path) == TD_FALSE) {
        return TD_SUCCESS;
    }

    ret = webrtc_streamer_webserver_start(web_root, cert_path, key_path, https_port, http_port);
    if (ret != 0) {
        WEBRTC_LOG_WARN("webserver start failed: %d", ret);
        return TD_FAILURE;
    }

    g_webrtc_bridge.webserver_started = TD_TRUE;
    WEBRTC_LOG_INFO("built-in webserver started: https=%d http=%d root=%s",
        https_port, http_port, web_root);
    return TD_SUCCESS;
}

td_s32 webrtc_bridge_init(ot_venc_chn request_idr_chn, webrtc_video_code_type_t codec_type,
    webrtc_stream_type_t stream_type)
{
    char *conf_text = NULL;
    char *runtime_conf_text = NULL;
    const char *runtime_conf_path = NULL;
    char initstring[512] = {0};
    char serno[128] = {0};
    char serveraddr[128] = {0};
    td_s32 ret;

    if (g_webrtc_bridge.initialized == TD_TRUE) {
        return TD_SUCCESS;
    }

    /* webcam.conf supplies device identity and signaling address and is required. */
    conf_text = webrtc_read_text_file(WEBRTC_CONFIG_PATH);
    if (conf_text == NULL) {
        WEBRTC_LOG_WARN("config %s not found, bridge disabled", WEBRTC_CONFIG_PATH);
        return TD_FAILURE;
    }

    if (webrtc_extract_json_string(conf_text, "initstring", initstring, sizeof(initstring)) == TD_FALSE ||
        webrtc_extract_json_string(conf_text, "serno", serno, sizeof(serno)) == TD_FALSE ||
        webrtc_extract_json_string(conf_text, "serveraddr", serveraddr, sizeof(serveraddr)) == TD_FALSE) {
        WEBRTC_LOG_ERROR("invalid config %s, require initstring/serno/serveraddr", WEBRTC_CONFIG_PATH);
        free(conf_text);
        return TD_FAILURE;
    }

    runtime_conf_text = webrtc_bridge_read_runtime_configuration(&runtime_conf_path);
    (td_void)webrtc_extract_json_string(conf_text, "customerserno",
        g_webrtc_bridge.customerserno, sizeof(g_webrtc_bridge.customerserno));
    webrtc_bridge_normalize_serveraddr(serveraddr, sizeof(serveraddr));
    (void)snprintf(g_webrtc_bridge.serial_number, sizeof(g_webrtc_bridge.serial_number), "%s", serno);

    g_webrtc_bridge.request_idr_chn = request_idr_chn;
    g_webrtc_bridge.codec_type = codec_type;
    g_webrtc_bridge.stream_type = stream_type;

    /* Set enabled only after SDK initialization succeeds; failure does not stop RTSP. */
    ret = webrtc_streamer_init(initstring, runtime_conf_text, serno, serveraddr,
        g_webrtc_bridge.customerserno[0] != '\0' ? g_webrtc_bridge.customerserno : NULL);
    if (ret < 0) {
        WEBRTC_LOG_ERROR("init failed: %d", ret);
        free(runtime_conf_text);
        free(conf_text);
        return TD_FAILURE;
    }

    g_webrtc_bridge.initialized = TD_TRUE;
    g_webrtc_bridge.enabled = TD_TRUE;

    webrtc_streamer_set_log_level_mask(WEBRTC_STREAM_WARNING | WEBRTC_STREAM_ERROR | WEBRTC_STREAM_FATAL);
    webrtc_streamer_set_mem_info(8 * 1024 * 1024, 2 * 1024 * 1024, 256);
    webrtc_streamer_set_device_discovery_info("hi3519dv500-imx335", "Hi3519DV500 Camera", "Camera", "1.0.0");

    /* SDK callbacks update bridge state or dispatch commands and never retain VENC/VPSS frames. */
    webrtc_streamer_register_call_income_callback_fun(webrtc_bridge_call_income_callback, NULL);
    webrtc_streamer_register_call_destory_callback_fun(webrtc_bridge_call_destroy_callback, NULL);
    webrtc_streamer_register_call_disconnect_callback_fun(webrtc_bridge_call_disconnect_callback, NULL);
    webrtc_streamer_register_call_failed_callback_fun(webrtc_bridge_call_failed_callback, NULL);
    webrtc_streamer_register_event_callback_fun(webrtc_bridge_event_callback, NULL);
    webrtc_streamer_register_configuration_callback_fun(webrtc_bridge_configuration_callback, NULL);
    webrtc_streamer_register_authentication_callback_fun(webrtc_bridge_auth_callback, NULL);
    webrtc_streamer_register_message_callback_fun(webrtc_bridge_message_callback, 4 * 1024, NULL);
    webrtc_streamer_register_datachannel_open_callback_fun(webrtc_bridge_datachannel_open_callback, NULL);
    webrtc_streamer_register_can_add_datachannel_callback_fun(webrtc_bridge_can_add_datachannel_callback, NULL);
    webrtc_streamer_register_datachannel_message_callback_fun(webrtc_bridge_datachannel_message_callback, NULL);
    webrtc_streamer_register_remote_play_start_callback_fun(webrtc_bridge_remote_play_start_callback, NULL);
    webrtc_streamer_register_session_ask_iframe_callback_fun(webrtc_bridge_session_ask_iframe_callback, NULL);
    webrtc_streamer_register_session_pli_callback_fun(webrtc_bridge_session_pli_callback, NULL);
    webrtc_streamer_register_video_callback_fun(webrtc_bridge_video_callback, NULL);
    webrtc_streamer_register_check_videocode_callback_fun(webrtc_bridge_check_videocode_callback, NULL);
    webrtc_streamer_register_callstate_callback_fun(webrtc_bridge_callstate_callback, NULL);
    webrtc_streamer_register_signaling_socket_error_callback_fun(webrtc_bridge_signaling_socket_error_callback, NULL);
    webrtc_streamer_register_network_quality_callback_fun(webrtc_bridge_network_quality_callback, NULL);
    webrtc_streamer_register_get_network_info_callback_fun(webrtc_bridge_get_network_info_callback, NULL);
    webrtc_streamer_register_send_queue_full_callback_fun(webrtc_bridge_send_queue_full_callback, NULL);
    webrtc_streamer_register_webserver_api_callback_fun(webrtc_bridge_webserver_api_callback, 4 * 1024, NULL);
    webrtc_streamer_register_webserver_websocket_messaeg_callback_fun(webrtc_bridge_webserver_websocket_callback,
        4 * 1024, NULL);

    (td_void)webrtc_bridge_start_webserver(conf_text);

    if (runtime_conf_path != NULL) {
        WEBRTC_LOG_INFO("runtime configuration loaded from %s", runtime_conf_path);
    } else {
        WEBRTC_LOG_INFO("runtime configuration not found, start with empty configuration");
    }
    WEBRTC_LOG_INFO("initialized: serno=%s server=%s codec=%d stream=%d",
        serno, serveraddr, codec_type, stream_type);

    free(runtime_conf_text);
    free(conf_text);
    return TD_SUCCESS;
}

td_void webrtc_bridge_shutdown(td_void)
{
    td_u8 *codec_header = NULL;

    if (g_webrtc_bridge.initialized == TD_FALSE) {
        return;
    }

    /* Stop the SDK-dependent web service, uninitialize the SDK, then free the codec header. */
    if (g_webrtc_bridge.webserver_started == TD_TRUE) {
        (td_void)webrtc_streamer_webserver_stop();
        g_webrtc_bridge.webserver_started = TD_FALSE;
    }

    (td_void)webrtc_streamer_uninit();
    codec_header = g_webrtc_bridge.codec_header;
    g_webrtc_bridge.codec_header = NULL;
    g_webrtc_bridge.codec_header_len = 0;
    memset(&g_webrtc_bridge, 0, sizeof(g_webrtc_bridge));
    free(codec_header);
    WEBRTC_LOG_INFO("shutdown complete");
}

td_s32 webrtc_bridge_set_codec_header(const td_u8 *data, td_u32 len)
{
    td_u8 *new_header = NULL;
    td_u8 *old_header = NULL;

    if (data == NULL || len == 0) {
        return TD_FAILURE;
    }

    new_header = (td_u8 *)malloc(len);
    if (new_header == NULL) {
        return TD_FAILURE;
    }

    /* Allocate and copy outside the lock to minimize video-sender lock contention. */
    memcpy(new_header, data, len);

    pthread_mutex_lock(&g_webrtc_bridge_lock);
    old_header = g_webrtc_bridge.codec_header;
    g_webrtc_bridge.codec_header = new_header;
    g_webrtc_bridge.codec_header_len = len;
    pthread_mutex_unlock(&g_webrtc_bridge_lock);

    free(old_header);
    return TD_SUCCESS;
}

td_s32 webrtc_bridge_input_video(const td_u8 *data, td_u32 len)
{
    td_bool waiting_keyframe;
    td_bool has_keyframe;
    td_u32 dropped_non_keyframes;

    if (g_webrtc_bridge.enabled == TD_FALSE || data == NULL || len == 0) {
        return TD_FAILURE;
    }

    pthread_mutex_lock(&g_webrtc_bridge_lock);
    waiting_keyframe = g_webrtc_bridge.waiting_keyframe;
    pthread_mutex_unlock(&g_webrtc_bridge_lock);

    /* After a session switch, accept only the first IDR before forwarding other frames. */
    if (waiting_keyframe == TD_TRUE) {
        has_keyframe = webrtc_bridge_frame_has_keyframe(data, len);
        if (has_keyframe == TD_FALSE) {
            pthread_mutex_lock(&g_webrtc_bridge_lock);
            g_webrtc_bridge.dropped_non_keyframes++;
            dropped_non_keyframes = g_webrtc_bridge.dropped_non_keyframes;
            pthread_mutex_unlock(&g_webrtc_bridge_lock);

            if (dropped_non_keyframes == 1) {
                WEBRTC_LOG_INFO("drop non-IDR while waiting keyframe");
            }
            return TD_SUCCESS;
        }

        pthread_mutex_lock(&g_webrtc_bridge_lock);
        g_webrtc_bridge.waiting_keyframe = TD_FALSE;
        g_webrtc_bridge.session_video_ready = TD_TRUE;
        g_webrtc_bridge.dropped_non_keyframes = 0;
        WEBRTC_LOG_INFO("keyframe accepted for active session: %s", g_webrtc_bridge.active_session_id);
        pthread_mutex_unlock(&g_webrtc_bridge_lock);
    }

    if (g_webrtc_bridge.codec_type == WEBRTC_VIDEO_H264) {
        return webrtc_bridge_input_h264_with_header_if_needed(data, len,
            webrtc_bridge_h264_has_idr(data, len));
    }

    return webrtc_streamer_input_video_data(g_webrtc_bridge.stream_type,
        g_webrtc_bridge.codec_type, (unsigned char *)data, len);
}

td_bool webrtc_bridge_is_enabled(td_void)
{
    return g_webrtc_bridge.enabled;
}

td_bool webrtc_bridge_has_active_session(td_void)
{
    td_bool has_active_session;

    pthread_mutex_lock(&g_webrtc_bridge_lock);
    has_active_session = (g_webrtc_bridge.enabled == TD_TRUE && g_webrtc_bridge.active_session_id[0] != '\0') ?
        TD_TRUE : TD_FALSE;
    pthread_mutex_unlock(&g_webrtc_bridge_lock);
    return has_active_session;
}
