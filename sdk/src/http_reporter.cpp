#include "http_reporter.h"

#include <algorithm>
#include <atomic>
#include <cerrno>
#include <condition_variable>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <ctime>
#include <dirent.h>
#include <fcntl.h>
#include <iomanip>
#include <mutex>
#include <netdb.h>
#include <queue>
#include <sstream>
#include <string>
#include <sys/select.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/time.h>
#include <thread>
#include <unistd.h>
#include <utility>
#include <vector>

#define STB_IMAGE_WRITE_IMPLEMENTATION
#define STBI_WRITE_NO_STDIO
#include "stb_image_write.h"

/*
 * A bounded queue decouples event reporting from streaming and inference.
 * The reporter worker owns JPEG conversion, image publication, DNS, sockets,
 * and HTTP requests.
 */
namespace {

constexpr const char *kDefaultConfigPath = "/mnt/webrtc/video.conf";
constexpr const char *kDefaultWebcamConfigPath = "/mnt/webrtc/webcam.conf";
constexpr const char *kDefaultReportUrl = "http://192.168.60.76:32557/vlsEventManagement/report";
constexpr const char *kDefaultSecondaryReportUrl =
    "http://192.168.88.31:8080/task/v1/event_report_camera";
constexpr const char *kDefaultSecondaryImageDir = "/mnt/webrtc/www/event_images";
constexpr const char *kDefaultSecondaryImageBaseUrl = "http://192.168.88.98:6888/event_images";
constexpr const char *kDefaultLocation = "测试位置";
constexpr td_u32 kMaxDetections = 32;
constexpr size_t kMaxQueueSize = 4;
constexpr size_t kDefaultSecondaryImageKeepCount = 100;
constexpr td_u64 kDefaultMinIntervalMs = 10000;
constexpr td_s32 kStableFrameThreshold = 30;

struct Detection {
    td_s32 class_id;
    std::string class_name;
    float confidence;
    float x1;
    float y1;
    float x2;
    float y2;
};

struct ReportItem {
    td_s32 frame_id;
    td_u32 width;
    td_u32 height;
    td_u32 stride_y;
    td_u32 stride_uv;
    td_bool yvu_semiplanar;
    std::vector<td_u8> yuv420sp;
    std::vector<Detection> detections;
};

struct HttpTarget {
    std::string url;
    std::string host;
    int port;
    std::string path;
    std::string authorization;
};

struct ReporterConfig {
    ReporterConfig()
    {
        primary.url = kDefaultReportUrl;
        primary.host = "192.168.60.76";
        primary.port = 32557;
        primary.path = "/vlsEventManagement/report";
        secondary.url = kDefaultSecondaryReportUrl;
        secondary.host = "192.168.88.31";
        secondary.port = 8080;
        secondary.path = "/task/v1/event_report_camera";
    }

    bool enabled = true;
    HttpTarget primary;
    std::string report_location = kDefaultLocation;
    std::string report_device;
    td_u64 min_interval_ms = kDefaultMinIntervalMs;

    bool secondary_enabled = false;
    HttpTarget secondary;
    std::string secondary_image_dir = kDefaultSecondaryImageDir;
    std::string secondary_image_base_url = kDefaultSecondaryImageBaseUrl;
    size_t secondary_image_keep_count = kDefaultSecondaryImageKeepCount;
    std::string secondary_device_id;
    std::string secondary_device_name = "Hi3519DV500摄像头";
    std::string secondary_device_tag = "AI监控";
    std::string secondary_device_tenant_id;
    std::string secondary_event_item = "目标检测";
    std::string secondary_event_name = "YOLOv8";
    std::string secondary_address;
    int secondary_coord_system_type = 1;
    int secondary_coord_system_type_change = 1;
    double secondary_lat = 22.71991;
    double secondary_lat_change = 22.71991;
    double secondary_lng = 114.24779;
    double secondary_lng_change = 114.24779;
};

/* Configuration and queue state use separate locks; neither is held during network I/O. */
std::mutex g_cfg_mutex;
ReporterConfig g_cfg;
std::atomic<bool> g_running(false);
std::atomic<bool> g_enabled(true);
std::mutex g_queue_mutex;
std::condition_variable g_queue_cv;
std::queue<ReportItem> g_queue;
std::thread g_worker;
td_u64 g_last_submit_ms = 0;
td_u64 g_dropped_items = 0;

td_u64 now_ms()
{
    timeval tv;
    gettimeofday(&tv, nullptr);
    return (td_u64)tv.tv_sec * 1000 + (td_u64)tv.tv_usec / 1000;
}

std::string read_text_file(const char *path)
{
    FILE *fp = fopen(path, "rb");
    if (fp == nullptr) {
        return std::string();
    }
    if (fseek(fp, 0, SEEK_END) != 0) {
        fclose(fp);
        return std::string();
    }
    long size = ftell(fp);
    if (size <= 0) {
        fclose(fp);
        return std::string();
    }
    rewind(fp);
    std::string data;
    data.resize((size_t)size);
    size_t n = fread(&data[0], 1, data.size(), fp);
    fclose(fp);
    data.resize(n);
    return data;
}

bool parse_json_string(const std::string &json, const char *key, std::string *out)
{
    std::string marker = "\"" + std::string(key) + "\"";
    size_t pos = json.find(marker);
    if (pos == std::string::npos) {
        return false;
    }
    pos = json.find(':', pos + marker.size());
    if (pos == std::string::npos) {
        return false;
    }
    pos = json.find('"', pos + 1);
    if (pos == std::string::npos) {
        return false;
    }

    std::string value;
    bool escaped = false;
    for (size_t i = pos + 1; i < json.size(); ++i) {
        char c = json[i];
        if (escaped) {
            value.push_back(c);
            escaped = false;
            continue;
        }
        if (c == '\\') {
            escaped = true;
            continue;
        }
        if (c == '"') {
            *out = value;
            return true;
        }
        value.push_back(c);
    }
    return false;
}

bool parse_json_bool(const std::string &json, const char *key, bool *out)
{
    std::string marker = "\"" + std::string(key) + "\"";
    size_t pos = json.find(marker);
    if (pos == std::string::npos) {
        return false;
    }
    pos = json.find(':', pos + marker.size());
    if (pos == std::string::npos) {
        return false;
    }
    std::string tail = json.substr(pos + 1, 8);
    if (tail.find("true") != std::string::npos) {
        *out = true;
        return true;
    }
    if (tail.find("false") != std::string::npos) {
        *out = false;
        return true;
    }
    return false;
}

bool parse_json_u64(const std::string &json, const char *key, td_u64 *out)
{
    std::string marker = "\"" + std::string(key) + "\"";
    size_t pos = json.find(marker);
    if (pos == std::string::npos) {
        return false;
    }
    pos = json.find(':', pos + marker.size());
    if (pos == std::string::npos) {
        return false;
    }
    char *end = nullptr;
    unsigned long long value = strtoull(json.c_str() + pos + 1, &end, 10);
    if (end == json.c_str() + pos + 1) {
        return false;
    }
    *out = (td_u64)value;
    return true;
}

bool parse_json_double(const std::string &json, const char *key, double *out)
{
    std::string marker = "\"" + std::string(key) + "\"";
    size_t pos = json.find(marker);
    if (pos == std::string::npos) {
        return false;
    }
    pos = json.find(':', pos + marker.size());
    if (pos == std::string::npos) {
        return false;
    }
    char *end = nullptr;
    double value = strtod(json.c_str() + pos + 1, &end);
    if (end == json.c_str() + pos + 1) {
        return false;
    }
    *out = value;
    return true;
}

bool parse_http_url(const std::string &url, std::string *host, int *port, std::string *path)
{
    const std::string scheme = "http://";
    if (url.compare(0, scheme.size(), scheme) != 0) {
        return false;
    }

    std::string rest = url.substr(scheme.size());
    size_t slash = rest.find('/');
    std::string host_port = (slash == std::string::npos) ? rest : rest.substr(0, slash);
    *path = (slash == std::string::npos) ? "/" : rest.substr(slash);

    size_t colon = host_port.rfind(':');
    if (colon == std::string::npos) {
        *host = host_port;
        *port = 80;
    } else {
        *host = host_port.substr(0, colon);
        *port = atoi(host_port.substr(colon + 1).c_str());
        if (*port <= 0) {
            *port = 80;
        }
    }
    return !host->empty() && !path->empty();
}

bool configure_http_target(HttpTarget *target)
{
    return target != nullptr &&
        parse_http_url(target->url, &target->host, &target->port, &target->path);
}

std::string json_escape(const std::string &value)
{
    std::string out;
    out.reserve(value.size() + 16);
    for (char c : value) {
        switch (c) {
            case '\\':
                out += "\\\\";
                break;
            case '"':
                out += "\\\"";
                break;
            case '\n':
                out += "\\n";
                break;
            case '\r':
                out += "\\r";
                break;
            case '\t':
                out += "\\t";
                break;
            default:
                out.push_back(c);
                break;
        }
    }
    return out;
}

int clamp_int(int value, int low, int high)
{
    return std::max(low, std::min(high, value));
}

std::string current_time_string()
{
    char buf[32] = {0};
    time_t now = time(nullptr);
    struct tm tm_now;
    localtime_r(&now, &tm_now);
    strftime(buf, sizeof(buf), "%Y-%m-%d %H:%M:%S", &tm_now);
    return std::string(buf);
}

void stbi_vector_write(void *context, void *data, int size)
{
    std::vector<td_u8> *out = static_cast<std::vector<td_u8> *>(context);
    const td_u8 *bytes = static_cast<const td_u8 *>(data);
    out->insert(out->end(), bytes, bytes + size);
}

bool encode_yuv420sp_to_jpeg(const ReportItem &item, std::vector<td_u8> *jpeg)
{
    if (item.yuv420sp.empty() || item.width == 0 || item.height == 0 ||
        item.stride_y < item.width || item.stride_uv < item.width || jpeg == nullptr) {
        return false;
    }

    std::vector<td_u8> rgb;
    rgb.resize((size_t)item.width * item.height * 3);
    const td_u8 *y_plane = item.yuv420sp.data();
    const td_u8 *uv_plane = item.yuv420sp.data() + (size_t)item.stride_y * item.height;

    for (td_u32 y = 0; y < item.height; ++y) {
        for (td_u32 x = 0; x < item.width; ++x) {
            int y_value = y_plane[(size_t)y * item.stride_y + x];
            size_t uv_index = (size_t)(y / 2) * item.stride_uv + (x & ~1U);
            int first = uv_plane[uv_index];
            int second = uv_plane[uv_index + 1];
            int u_value = (item.yvu_semiplanar == TD_TRUE) ? second : first;
            int v_value = (item.yvu_semiplanar == TD_TRUE) ? first : second;

            int c = y_value - 16;
            int d = u_value - 128;
            int e = v_value - 128;
            int r = (298 * c + 409 * e + 128) >> 8;
            int g = (298 * c - 100 * d - 208 * e + 128) >> 8;
            int b = (298 * c + 516 * d + 128) >> 8;
            size_t rgb_index = ((size_t)y * item.width + x) * 3;
            rgb[rgb_index + 0] = (td_u8)clamp_int(r, 0, 255);
            rgb[rgb_index + 1] = (td_u8)clamp_int(g, 0, 255);
            rgb[rgb_index + 2] = (td_u8)clamp_int(b, 0, 255);
        }
    }

    jpeg->clear();
    int ok = stbi_write_jpg_to_func(stbi_vector_write, jpeg, (int)item.width, (int)item.height, 3, rgb.data(), 80);
    return ok != 0 && !jpeg->empty();
}

std::string build_event_data_json(const ReportItem &item)
{
    std::ostringstream oss;
    oss << "[";
    for (size_t i = 0; i < item.detections.size(); ++i) {
        const Detection &det = item.detections[i];
        int x1 = clamp_int((int)(det.x1 + 0.5f), 0, (int)item.width - 1);
        int y1 = clamp_int((int)(det.y1 + 0.5f), 0, (int)item.height - 1);
        int x2 = clamp_int((int)(det.x2 + 0.5f), 0, (int)item.width - 1);
        int y2 = clamp_int((int)(det.y2 + 0.5f), 0, (int)item.height - 1);
        if (i > 0) {
            oss << ",";
        }
        oss << "{\"score\":" << det.confidence
            << ",\"objectDetInfo\":{\"className\":\"" << json_escape(det.class_name) << "\"}"
            << ",\"detectionRectangle\":{\"x\":" << x1
            << ",\"y\":" << y1
            << ",\"width\":" << std::max(1, x2 - x1)
            << ",\"height\":" << std::max(1, y2 - y1)
            << "}}";
    }
    oss << "]";
    return oss.str();
}

std::string build_event_json(const ReportItem &item, const ReporterConfig &cfg)
{
    std::string event_data = build_event_data_json(item);
    std::string event_type = item.detections.empty() ? "person" : item.detections[0].class_name;
    std::ostringstream desc;
    desc << "设备 " << cfg.report_device << " detected " << item.detections.size() << " target(s).";

    std::ostringstream ev;
    ev << "{";
    ev << "\"eventDesc\":\"" << json_escape(desc.str()) << "\",";
    ev << "\"eventType\":\"" << json_escape(event_type) << "\",";
    ev << "\"reportLocation\":\"" << json_escape(cfg.report_location) << "\",";
    ev << "\"reportDevice\":\"" << json_escape(cfg.report_device) << "\",";
    ev << "\"reportTime\":\"" << current_time_string() << "\",";
    ev << "\"eventLevel\":\"low\",";
    ev << "\"eventStatus\":\"pending\",";
    ev << "\"executor\":\"\",";
    ev << "\"executorIds\":\"\",";
    ev << "\"eventData\":\"" << json_escape(event_data) << "\",";
    ev << "\"handleResult\":\"\",";
    ev << "\"feedbackInfo\":\"{}\",";
    ev << "\"feedbackStatus\":0";
    ev << "}";
    return ev.str();
}

std::string build_multipart_body(const ReportItem &item, const ReporterConfig &cfg,
    const std::vector<td_u8> &jpeg, std::string *content_type)
{
    const std::string boundary = "----rtsp-streamer-event-boundary-7f3d9a01";
    std::string event_json = build_event_json(item, cfg);
    std::ostringstream filename;
    filename << "frame_" << item.frame_id << ".jpg";

    *content_type = "multipart/form-data; boundary=" + boundary;

    std::string body;
    body.reserve(event_json.size() + jpeg.size() + 512);
    /* Primary backend contract: an event JSON part followed by a file JPEG part. */
    body += "--" + boundary + "\r\n";
    body += "Content-Disposition: form-data; name=\"event\"\r\n";
    body += "Content-Type: application/json\r\n\r\n";
    body += event_json;
    body += "\r\n";

    body += "--" + boundary + "\r\n";
    body += "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename.str() + "\"\r\n";
    body += "Content-Type: image/jpeg\r\n\r\n";
    body.append(reinterpret_cast<const char *>(jpeg.data()), jpeg.size());
    body += "\r\n";
    body += "--" + boundary + "--\r\n";
    return body;
}

std::string build_secondary_event_json(const ReportItem &item, const ReporterConfig &cfg,
    const std::string &image_url)
{
    /* The secondary backend receives JSON only; pics references a separately published JPEG. */
    std::ostringstream desc;
    desc << "AI检测到" << item.detections.size() << "个目标";

    std::ostringstream body;
    body << std::setprecision(10);
    body << "{";
    body << "\"describe\":\"" << json_escape(desc.str()) << "\",";
    body << "\"device_id\":\"" << json_escape(cfg.secondary_device_id) << "\",";
    body << "\"device_name\":\"" << json_escape(cfg.secondary_device_name) << "\",";
    body << "\"device_tag\":\"" << json_escape(cfg.secondary_device_tag) << "\",";
    body << "\"device_tenant_id\":\"" << json_escape(cfg.secondary_device_tenant_id) << "\",";
    body << "\"item\":\"" << json_escape(cfg.secondary_event_item) << "\",";
    body << "\"name\":\"" << json_escape(cfg.secondary_event_name) << "\",";
    body << "\"pics\":[\"" << json_escape(image_url) << "\"],";
    body << "\"point\":{";
    body << "\"address\":\"" << json_escape(cfg.secondary_address) << "\",";
    body << "\"coord_system_type\":" << cfg.secondary_coord_system_type << ",";
    body << "\"coord_system_type_change\":" << cfg.secondary_coord_system_type_change << ",";
    body << "\"lat\":" << cfg.secondary_lat << ",";
    body << "\"lat_change\":" << cfg.secondary_lat_change << ",";
    body << "\"lng\":" << cfg.secondary_lng << ",";
    body << "\"lng_change\":" << cfg.secondary_lng_change;
    body << "},";
    body << "\"video\":[]";
    body << "}";
    return body.str();
}

bool ensure_directory(const std::string &path)
{
    if (path.empty() || path[0] != '/') {
        return false;
    }

    for (size_t pos = 1; pos <= path.size(); ++pos) {
        if (pos != path.size() && path[pos] != '/') {
            continue;
        }
        std::string part = path.substr(0, pos);
        if (part.empty()) {
            continue;
        }
        struct stat st;
        if (stat(part.c_str(), &st) == 0) {
            if (!S_ISDIR(st.st_mode)) {
                return false;
            }
            continue;
        }
        if (mkdir(part.c_str(), 0755) != 0 && errno != EEXIST) {
            return false;
        }
    }
    return true;
}

bool write_binary_file_atomic(const std::string &path, const std::vector<td_u8> &data,
    std::string *failure_reason)
{
    std::string tmp_path = path + ".tmp";
    FILE *fp = fopen(tmp_path.c_str(), "wb");
    if (fp == nullptr) {
        *failure_reason = "open image temp file failed: " + std::string(strerror(errno));
        return false;
    }

    size_t written = fwrite(data.data(), 1, data.size(), fp);
    bool ok = written == data.size() && fflush(fp) == 0 && fsync(fileno(fp)) == 0;
    int saved_errno = errno;
    if (fclose(fp) != 0) {
        ok = false;
        saved_errno = errno;
    }
    if (!ok) {
        (void)unlink(tmp_path.c_str());
        *failure_reason = "write image temp file failed: " + std::string(strerror(saved_errno));
        return false;
    }
    if (rename(tmp_path.c_str(), path.c_str()) != 0) {
        saved_errno = errno;
        (void)unlink(tmp_path.c_str());
        *failure_reason = "publish image failed: " + std::string(strerror(saved_errno));
        return false;
    }
    return true;
}

void cleanup_published_images(const std::string &directory, size_t keep_count)
{
    DIR *dir = opendir(directory.c_str());
    if (dir == nullptr) {
        return;
    }

    std::vector<std::pair<time_t, std::string> > files;
    dirent *entry = nullptr;
    while ((entry = readdir(dir)) != nullptr) {
        std::string name = entry->d_name;
        if (name.compare(0, 6, "frame_") != 0 || name.size() < 10 ||
            name.compare(name.size() - 4, 4, ".jpg") != 0) {
            continue;
        }
        std::string path = directory + "/" + name;
        struct stat st;
        if (stat(path.c_str(), &st) == 0 && S_ISREG(st.st_mode)) {
            files.push_back(std::make_pair(st.st_mtime, path));
        }
    }
    closedir(dir);

    std::sort(files.begin(), files.end());
    size_t remove_count = files.size() > keep_count ? files.size() - keep_count : 0;
    for (size_t i = 0; i < remove_count; ++i) {
        (void)unlink(files[i].second.c_str());
    }
}

bool publish_secondary_image(const ReportItem &item, const ReporterConfig &cfg,
    const std::vector<td_u8> &jpeg, std::string *image_url, std::string *failure_reason)
{
    if (!ensure_directory(cfg.secondary_image_dir)) {
        *failure_reason = "create image directory failed: " + cfg.secondary_image_dir;
        return false;
    }

    std::ostringstream filename;
    filename << "frame_" << item.frame_id << "_" << now_ms() << ".jpg";
    std::string image_path = cfg.secondary_image_dir + "/" + filename.str();
    if (!write_binary_file_atomic(image_path, jpeg, failure_reason)) {
        return false;
    }

    std::string base_url = cfg.secondary_image_base_url;
    while (!base_url.empty() && base_url[base_url.size() - 1] == '/') {
        base_url.resize(base_url.size() - 1);
    }
    if (base_url.empty()) {
        *failure_reason = "secondary image base URL is empty";
        (void)unlink(image_path.c_str());
        return false;
    }

    *image_url = base_url + "/" + filename.str();
    cleanup_published_images(cfg.secondary_image_dir, cfg.secondary_image_keep_count);
    std::printf("[REPORT] endpoint=secondary image published path=%s url=%s jpeg=%zu\n",
        image_path.c_str(), image_url->c_str(), jpeg.size());
    return true;
}

bool connect_with_timeout(int sock, const sockaddr *addr, socklen_t addr_len, int timeout_ms)
{
    int flags = fcntl(sock, F_GETFL, 0);
    if (flags >= 0) {
        (void)fcntl(sock, F_SETFL, flags | O_NONBLOCK);
    }

    int ret = connect(sock, addr, addr_len);
    if (ret == 0) {
        if (flags >= 0) {
            (void)fcntl(sock, F_SETFL, flags);
        }
        return true;
    }
    if (errno != EINPROGRESS) {
        return false;
    }

    fd_set wfds;
    FD_ZERO(&wfds);
    FD_SET(sock, &wfds);
    timeval tv;
    tv.tv_sec = timeout_ms / 1000;
    tv.tv_usec = (timeout_ms % 1000) * 1000;
    ret = select(sock + 1, nullptr, &wfds, nullptr, &tv);
    if (ret <= 0) {
        return false;
    }

    int err = 0;
    socklen_t err_len = sizeof(err);
    if (getsockopt(sock, SOL_SOCKET, SO_ERROR, &err, &err_len) != 0 || err != 0) {
        errno = err;
        return false;
    }
    if (flags >= 0) {
        (void)fcntl(sock, F_SETFL, flags);
    }
    return true;
}

bool send_all(int sock, const char *data, size_t size)
{
    size_t sent = 0;
    while (sent < size) {
        ssize_t n = send(sock, data + sent, size - sent, 0);
        if (n <= 0) {
            return false;
        }
        sent += (size_t)n;
    }
    return true;
}

bool response_success_field_ok(const std::string &body)
{
    size_t pos = body.find("\"success\"");
    if (pos == std::string::npos) {
        return true;
    }
    size_t colon = body.find(':', pos);
    if (colon == std::string::npos) {
        return false;
    }
    size_t true_pos = body.find("true", colon + 1);
    size_t false_pos = body.find("false", colon + 1);
    return true_pos != std::string::npos && (false_pos == std::string::npos || true_pos < false_pos);
}

bool response_code_200(const std::string &body)
{
    size_t pos = body.find("\"code\"");
    if (pos == std::string::npos) {
        return false;
    }
    pos = body.find(':', pos);
    if (pos == std::string::npos) {
        return false;
    }
    char *end = nullptr;
    long code = strtol(body.c_str() + pos + 1, &end, 10);
    return end != body.c_str() + pos + 1 && code == 200;
}

enum class ResponsePolicy {
    OPTIONAL_SUCCESS_FIELD,
    REQUIRE_CODE_200
};

bool http_post(const HttpTarget &target, const std::string &body, const std::string &content_type,
    ResponsePolicy response_policy, int *status_code, std::string *response_preview,
    std::string *failure_reason)
{
    *status_code = -1;
    response_preview->clear();
    failure_reason->clear();

    addrinfo hints;
    std::memset(&hints, 0, sizeof(hints));
    hints.ai_family = AF_UNSPEC;
    hints.ai_socktype = SOCK_STREAM;

    addrinfo *res = nullptr;
    std::string port_str = std::to_string(target.port);
    if (getaddrinfo(target.host.c_str(), port_str.c_str(), &hints, &res) != 0 || res == nullptr) {
        *failure_reason = "resolve host failed";
        return false;
    }

    int sock = -1;
    bool connected = false;
    for (addrinfo *p = res; p != nullptr; p = p->ai_next) {
        sock = socket(p->ai_family, p->ai_socktype, p->ai_protocol);
        if (sock < 0) {
            continue;
        }
        if (connect_with_timeout(sock, p->ai_addr, p->ai_addrlen, 3000)) {
            connected = true;
            break;
        }
        close(sock);
        sock = -1;
    }
    freeaddrinfo(res);

    if (!connected || sock < 0) {
        *failure_reason = "connect failed";
        return false;
    }

    timeval tv;
    tv.tv_sec = 3;
    tv.tv_usec = 0;
    (void)setsockopt(sock, SOL_SOCKET, SO_SNDTIMEO, &tv, sizeof(tv));
    (void)setsockopt(sock, SOL_SOCKET, SO_RCVTIMEO, &tv, sizeof(tv));

    std::ostringstream req;
    req << "POST " << target.path << " HTTP/1.1\r\n";
    req << "Host: " << target.host << ":" << target.port << "\r\n";
    req << "Content-Type: " << content_type << "\r\n";
    if (!target.authorization.empty() && target.authorization.find('\r') == std::string::npos &&
        target.authorization.find('\n') == std::string::npos) {
        req << "Authorization: " << target.authorization << "\r\n";
    }
    req << "Content-Length: " << body.size() << "\r\n";
    req << "Connection: close\r\n\r\n";
    std::string header = req.str();

    if (!send_all(sock, header.data(), header.size()) || !send_all(sock, body.data(), body.size())) {
        *failure_reason = "send failed";
        close(sock);
        return false;
    }

    std::string response;
    char buf[512];
    while (true) {
        ssize_t n = recv(sock, buf, sizeof(buf), 0);
        if (n <= 0) {
            break;
        }
        response.append(buf, buf + n);
        if (response.size() > 8192) {
            break;
        }
    }
    close(sock);

    if (response.empty()) {
        *failure_reason = "empty response";
        return false;
    }

    std::istringstream status_line(response);
    std::string http_version;
    status_line >> http_version >> *status_code;

    size_t body_pos = response.find("\r\n\r\n");
    std::string response_body = (body_pos == std::string::npos) ? response : response.substr(body_pos + 4);
    *response_preview = response_body.substr(0, 200);

    if (*status_code != 200) {
        *failure_reason = "http status is not 200";
        return false;
    }
    if (response_policy == ResponsePolicy::REQUIRE_CODE_200) {
        if (!response_code_200(response_body)) {
            *failure_reason = "backend code is not 200";
            return false;
        }
    } else {
        if (!response_success_field_ok(response_body)) {
            *failure_reason = "backend success field is false";
            return false;
        }
    }
    return true;
}

void post_event_to_endpoint(const char *endpoint_name, const HttpTarget &target, const ReportItem &item,
    const std::string &body, const std::string &content_type, const std::vector<td_u8> &jpeg,
    ResponsePolicy response_policy)
{
    std::string failure;
    int status = -1;
    std::string response_preview;
    bool ok = http_post(target, body, content_type, response_policy, &status, &response_preview, &failure);

    std::printf("[REPORT] endpoint=%s url=%s targets=%zu jpeg=%zu http=%d resp=\"%s\"%s%s\n",
        endpoint_name, target.url.c_str(), item.detections.size(), jpeg.size(), status,
        response_preview.c_str(), ok ? "" : " reason=", ok ? "" : failure.c_str());
}

void worker_loop()
{
    while (true) {
        ReportItem item;
        {
            std::unique_lock<std::mutex> lock(g_queue_mutex);
            g_queue_cv.wait(lock, [] { return !g_running.load() || !g_queue.empty(); });
            if (!g_running.load() && g_queue.empty()) {
                break;
            }
            item = std::move(g_queue.front());
            g_queue.pop();
        }

        ReporterConfig cfg;
        {
            std::lock_guard<std::mutex> lock(g_cfg_mutex);
            cfg = g_cfg;
        }

        /* Keep expensive image conversion and both endpoint requests off the video and AI threads. */
        std::vector<td_u8> jpeg;
        std::string failure;
        if (!encode_yuv420sp_to_jpeg(item, &jpeg)) {
            failure = "JPEG encode failed";
            std::printf("[REPORT][ERROR] targets=%zu jpeg=0 reason=%s\n",
                item.detections.size(), failure.c_str());
            continue;
        }

        if (cfg.enabled) {
            std::string content_type;
            std::string body = build_multipart_body(item, cfg, jpeg, &content_type);
            post_event_to_endpoint("primary", cfg.primary, item, body, content_type, jpeg,
                ResponsePolicy::OPTIONAL_SUCCESS_FIELD);
        }

        if (cfg.secondary_enabled) {
            std::string image_url;
            if (!publish_secondary_image(item, cfg, jpeg, &image_url, &failure)) {
                std::printf("[REPORT][ERROR] endpoint=secondary url=%s targets=%zu jpeg=%zu reason=%s\n",
                    cfg.secondary.url.c_str(), item.detections.size(), jpeg.size(), failure.c_str());
                continue;
            }
            std::string body = build_secondary_event_json(item, cfg, image_url);
            post_event_to_endpoint("secondary", cfg.secondary, item, body, "application/json", jpeg,
                ResponsePolicy::REQUIRE_CODE_200);
        }
    }
}

void apply_config_text(const std::string &text)
{
    ReporterConfig cfg;
    bool enabled = cfg.enabled;
    bool secondary_enabled = cfg.secondary_enabled;
    std::string value;
    td_u64 min_interval = cfg.min_interval_ms;
    td_u64 numeric_value = 0;

    if (parse_json_bool(text, "enable_event_reporting", &enabled)) {
        cfg.enabled = enabled;
    }
    if (parse_json_string(text, "event_reporting_url", &value)) {
        cfg.primary.url = value;
    }
    if (parse_json_string(text, "report_location", &value)) {
        cfg.report_location = value;
    }
    if (parse_json_string(text, "report_device", &value)) {
        cfg.report_device = value;
    }
    if (parse_json_u64(text, "event_report_min_interval_ms", &min_interval)) {
        cfg.min_interval_ms = min_interval;
    }

    if (parse_json_bool(text, "enable_secondary_event_reporting", &secondary_enabled)) {
        cfg.secondary_enabled = secondary_enabled;
    }
    if (parse_json_string(text, "secondary_event_reporting_url", &value)) {
        cfg.secondary.url = value;
    }
    if (parse_json_string(text, "secondary_event_authorization", &value)) {
        cfg.secondary.authorization = value;
    }
    if (parse_json_string(text, "secondary_image_dir", &value)) {
        cfg.secondary_image_dir = value;
    }
    if (parse_json_string(text, "secondary_image_base_url", &value)) {
        cfg.secondary_image_base_url = value;
    }
    if (parse_json_u64(text, "secondary_image_keep_count", &numeric_value)) {
        cfg.secondary_image_keep_count = (size_t)std::max<td_u64>(1, numeric_value);
    }
    if (parse_json_string(text, "secondary_device_id", &value)) {
        cfg.secondary_device_id = value;
    }
    if (parse_json_string(text, "secondary_device_name", &value)) {
        cfg.secondary_device_name = value;
    }
    if (parse_json_string(text, "secondary_device_tag", &value)) {
        cfg.secondary_device_tag = value;
    }
    if (parse_json_string(text, "secondary_device_tenant_id", &value)) {
        cfg.secondary_device_tenant_id = value;
    }
    if (parse_json_string(text, "secondary_event_item", &value)) {
        cfg.secondary_event_item = value;
    }
    if (parse_json_string(text, "secondary_event_name", &value)) {
        cfg.secondary_event_name = value;
    }
    if (parse_json_string(text, "secondary_report_address", &value)) {
        cfg.secondary_address = value;
    }
    if (parse_json_u64(text, "secondary_coord_system_type", &numeric_value)) {
        cfg.secondary_coord_system_type = (int)numeric_value;
    }
    if (parse_json_u64(text, "secondary_coord_system_type_change", &numeric_value)) {
        cfg.secondary_coord_system_type_change = (int)numeric_value;
    }
    (void)parse_json_double(text, "secondary_lat", &cfg.secondary_lat);
    (void)parse_json_double(text, "secondary_lat_change", &cfg.secondary_lat_change);
    (void)parse_json_double(text, "secondary_lng", &cfg.secondary_lng);
    (void)parse_json_double(text, "secondary_lng_change", &cfg.secondary_lng_change);

    if (!configure_http_target(&cfg.primary)) {
        cfg.primary.url = kDefaultReportUrl;
        (void)configure_http_target(&cfg.primary);
        std::printf("[REPORT][WARN] invalid primary URL, using default=%s\n", cfg.primary.url.c_str());
    }
    if (!configure_http_target(&cfg.secondary)) {
        cfg.secondary_enabled = false;
        std::printf("[REPORT][WARN] invalid secondary URL, secondary reporting disabled url=%s\n",
            cfg.secondary.url.c_str());
    }

    if (cfg.report_device.empty()) {
        std::string webcam_conf = read_text_file(kDefaultWebcamConfigPath);
        if (!webcam_conf.empty() && parse_json_string(webcam_conf, "serno", &value)) {
            cfg.report_device = value;
        }
    }
    if (cfg.report_device.empty()) {
        cfg.report_device = "unknown-device";
    }
    if (cfg.secondary_device_id.empty()) {
        cfg.secondary_device_id = cfg.report_device;
    }
    if (cfg.secondary_address.empty()) {
        cfg.secondary_address = cfg.report_location;
    }
    if (cfg.secondary_enabled && cfg.secondary_device_tenant_id.empty()) {
        std::printf("[REPORT][WARN] secondary_device_tenant_id is empty\n");
    }

    {
        std::lock_guard<std::mutex> lock(g_cfg_mutex);
        g_cfg = cfg;
    }
    g_enabled.store(cfg.enabled || cfg.secondary_enabled);
    std::printf("[REPORT] config endpoint=primary enable=%d url=%s location=%s device=%s interval=%llums\n",
        cfg.enabled ? 1 : 0, cfg.primary.url.c_str(), cfg.report_location.c_str(), cfg.report_device.c_str(),
        (unsigned long long)cfg.min_interval_ms);
    std::printf("[REPORT] config endpoint=secondary enable=%d url=%s image_base=%s image_dir=%s "
        "device=%s tenant=%s keep=%zu\n",
        cfg.secondary_enabled ? 1 : 0, cfg.secondary.url.c_str(), cfg.secondary_image_base_url.c_str(),
        cfg.secondary_image_dir.c_str(), cfg.secondary_device_id.c_str(),
        cfg.secondary_device_tenant_id.c_str(), cfg.secondary_image_keep_count);
}

} // namespace

extern "C" void http_reporter_load_config(void)
{
    std::string text = read_text_file(kDefaultConfigPath);
    if (text.empty()) {
        std::printf("[REPORT][WARN] config %s not found, using defaults\n", kDefaultConfigPath);
    }
    apply_config_text(text);
}

extern "C" void http_reporter_set_enable(bool enable)
{
    g_enabled.store(enable);
    std::printf("[REPORT] set enable=%d\n", enable ? 1 : 0);
}

extern "C" bool http_reporter_is_enabled(void)
{
    return g_enabled.load();
}

extern "C" void http_reporter_start(void)
{
    if (!g_enabled.load()) {
        std::printf("[REPORT] reporter disabled by config\n");
        return;
    }
    bool expected = false;
    if (!g_running.compare_exchange_strong(expected, true)) {
        return;
    }
    {
        std::lock_guard<std::mutex> lock(g_queue_mutex);
        g_last_submit_ms = 0;
        g_dropped_items = 0;
    }
    g_worker = std::thread(worker_loop);
    std::printf("[REPORT] reporter thread started\n");
}

extern "C" void http_reporter_stop(void)
{
    bool was_running = g_running.exchange(false);
    if (was_running) {
        g_queue_cv.notify_all();
        if (g_worker.joinable()) {
            g_worker.join();
        }
    }
    {
        std::lock_guard<std::mutex> lock(g_queue_mutex);
        std::queue<ReportItem> empty;
        std::swap(g_queue, empty);
    }
    std::printf("[REPORT] reporter stopped dropped=%llu\n", (unsigned long long)g_dropped_items);
}

extern "C" void http_reporter_submit(td_s32 frame_id, td_u32 width, td_u32 height,
    td_u32 stride_y, td_u32 stride_uv, td_bool yvu_semiplanar,
    const td_u8 *yuv420sp, const http_reporter_detection *detections,
    td_u32 detection_count)
{
    if (!g_enabled.load() || !g_running.load() || yuv420sp == nullptr ||
        detections == nullptr || detection_count == 0 || width == 0 || height == 0 ||
        frame_id < kStableFrameThreshold) {
        return;
    }

    ReporterConfig cfg;
    {
        std::lock_guard<std::mutex> cfg_lock(g_cfg_mutex);
        cfg = g_cfg;
    }

    /* Both endpoints share one interval so detection bursts cannot flood either backend. */
    td_u64 now = now_ms();
    {
        std::lock_guard<std::mutex> lock(g_queue_mutex);
        if (g_last_submit_ms != 0 && now - g_last_submit_ms < cfg.min_interval_ms) {
            return;
        }
        g_last_submit_ms = now;
    }

    size_t y_size = (size_t)stride_y * height;
    size_t uv_size = (size_t)stride_uv * height / 2;
    if (stride_y < width || stride_uv < width || y_size == 0 || uv_size == 0) {
        std::printf("[REPORT][ERROR] invalid yuv layout frame=%d width=%u height=%u stride=(%u,%u)\n",
            frame_id, width, height, stride_y, stride_uv);
        return;
    }

    ReportItem item;
    item.frame_id = frame_id;
    item.width = width;
    item.height = height;
    item.stride_y = stride_y;
    item.stride_uv = stride_uv;
    item.yvu_semiplanar = yvu_semiplanar;
    /* The source is a mapped VPSS frame that becomes invalid when the caller releases it. */
    item.yuv420sp.assign(yuv420sp, yuv420sp + y_size + uv_size);

    td_u32 copy_count = std::min(detection_count, kMaxDetections);
    item.detections.reserve(copy_count);
    for (td_u32 i = 0; i < copy_count; ++i) {
        Detection det;
        det.class_id = detections[i].class_id;
        det.class_name = detections[i].class_name[0] != '\0' ? detections[i].class_name : "unknown";
        det.confidence = detections[i].confidence;
        det.x1 = detections[i].x1;
        det.y1 = detections[i].y1;
        det.x2 = detections[i].x2;
        det.y2 = detections[i].y2;
        item.detections.push_back(det);
    }

    {
        std::lock_guard<std::mutex> lock(g_queue_mutex);
        if (!g_running.load()) {
            return;
        }
        /* Drop the oldest event when the network is slow or unavailable to keep memory bounded. */
        while (g_queue.size() >= kMaxQueueSize) {
            g_queue.pop();
            g_dropped_items++;
            if (g_dropped_items <= 3 || (g_dropped_items % 10) == 0) {
                std::printf("[REPORT][WARN] queue full, dropped oldest total=%llu\n",
                    (unsigned long long)g_dropped_items);
            }
        }
        g_queue.push(std::move(item));
    }
    g_queue_cv.notify_one();
}
