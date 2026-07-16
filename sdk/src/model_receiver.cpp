#include "model_receiver.h"

#include <algorithm>
#include <atomic>
#include <cerrno>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <mutex>
#include <arpa/inet.h>
#include <netdb.h>
#include <sstream>
#include <string>
#include <sys/select.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/time.h>
#include <thread>
#include <unistd.h>
#include <vector>

/*
 * Device-side HTTP receiver for model updates. Downloads and model callbacks
 * run in this service thread and never block the video streaming thread.
 */
namespace {

constexpr const char *kDefaultVideoConfigPath = "/mnt/webrtc/video.conf";
constexpr const char *kDefaultWebcamConfigPath = "/mnt/webrtc/webcam.conf";
constexpr const char *kDefaultListen = "0.0.0.0:8888";
constexpr const char *kDefaultModelPath = "/mnt/ai_verify/yolov8.om";
constexpr size_t kMaxRequestBytes = 8192;

struct ReceiverConfig {
    bool enabled = false;
    std::string listen_host = "0.0.0.0";
    int listen_port = 8888;
    std::string model_path = kDefaultModelPath;
    std::string webcam_config_path = kDefaultWebcamConfigPath;
    model_receiver_model_validate_fn validate_model = nullptr;
    model_receiver_model_switch_fn switch_model = nullptr;
};

std::mutex g_cfg_mutex;
ReceiverConfig g_cfg;
std::atomic<bool> g_running(false);
std::thread g_thread;
int g_listen_fd = -1;

std::string read_text_file(const char *path)
{
    FILE *fp = std::fopen(path, "rb");
    if (fp == nullptr) {
        return std::string();
    }
    if (std::fseek(fp, 0, SEEK_END) != 0) {
        std::fclose(fp);
        return std::string();
    }
    long size = std::ftell(fp);
    if (size <= 0) {
        std::fclose(fp);
        return std::string();
    }
    std::rewind(fp);
    std::string out;
    out.resize((size_t)size);
    size_t n = std::fread(&out[0], 1, out.size(), fp);
    std::fclose(fp);
    out.resize(n);
    return out;
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

std::string url_decode(const std::string &value)
{
    std::string out;
    for (size_t i = 0; i < value.size(); ++i) {
        if (value[i] == '%' && i + 2 < value.size()) {
            char hex[3] = {value[i + 1], value[i + 2], 0};
            char *end = nullptr;
            long ch = std::strtol(hex, &end, 16);
            if (end != hex + 2) {
                out.push_back(value[i]);
            } else {
                out.push_back((char)ch);
                i += 2;
            }
        } else if (value[i] == '+') {
            out.push_back(' ');
        } else {
            out.push_back(value[i]);
        }
    }
    return out;
}

std::string extract_query_param(const std::string &target, const std::string &name)
{
    size_t q = target.find('?');
    if (q == std::string::npos) {
        return std::string();
    }

    std::string query = target.substr(q + 1);
    std::string key = name + "=";
    size_t pos = query.find(key);
    if (pos == std::string::npos) {
        return std::string();
    }

    size_t start = pos + key.size();
    size_t end = query.size();
    if (name != "modelDownloadPath") {
        size_t amp = query.find('&', start);
        if (amp != std::string::npos) {
            end = amp;
        }
    } else {
        size_t amp = query.find("&deviceId=", start);
        if (amp != std::string::npos) {
            end = amp;
        }
    }
    return url_decode(query.substr(start, end - start));
}

bool starts_with(const std::string &value, const char *prefix)
{
    size_t len = std::strlen(prefix);
    return value.size() >= len && value.compare(0, len, prefix) == 0;
}

bool ends_with_om_url(const std::string &url)
{
    size_t q = url.find('?');
    std::string path = (q == std::string::npos) ? url : url.substr(0, q);
    std::transform(path.begin(), path.end(), path.begin(), ::tolower);
    return path.size() >= 3 && path.compare(path.size() - 3, 3, ".om") == 0;
}

bool split_host_port(const std::string &listen, std::string *host, int *port)
{
    size_t colon = listen.rfind(':');
    if (colon == std::string::npos) {
        return false;
    }
    *host = listen.substr(0, colon);
    *port = std::atoi(listen.substr(colon + 1).c_str());
    return !host->empty() && *port > 0;
}

bool parse_http_url(const std::string &url, std::string *host, int *port, std::string *path)
{
    if (!starts_with(url, "http://")) {
        return false;
    }
    std::string rest = url.substr(7);
    size_t slash = rest.find('/');
    std::string host_port = (slash == std::string::npos) ? rest : rest.substr(0, slash);
    *path = (slash == std::string::npos) ? "/" : rest.substr(slash);
    size_t colon = host_port.rfind(':');
    if (colon == std::string::npos) {
        *host = host_port;
        *port = 80;
    } else {
        *host = host_port.substr(0, colon);
        *port = std::atoi(host_port.substr(colon + 1).c_str());
    }
    return !host->empty() && *port > 0;
}

std::string dirname_of(const std::string &path)
{
    size_t slash = path.find_last_of('/');
    if (slash == std::string::npos || slash == 0) {
        return slash == 0 ? "/" : ".";
    }
    return path.substr(0, slash);
}

bool ensure_dir(const std::string &path)
{
    struct stat st;
    if (stat(path.c_str(), &st) == 0) {
        return S_ISDIR(st.st_mode);
    }
    return mkdir(path.c_str(), 0755) == 0 || errno == EEXIST;
}

std::string load_device_id(const std::string &webcam_config_path)
{
    std::string json = read_text_file(webcam_config_path.c_str());
    std::string serno;
    if (!parse_json_string(json, "serno", &serno)) {
        return std::string();
    }
    return serno;
}

void send_json(int fd, int status, const char *reason, const std::string &body)
{
    std::ostringstream oss;
    oss << "HTTP/1.1 " << status << " " << reason << "\r\n";
    oss << "Content-Type: application/json\r\n";
    oss << "Content-Length: " << body.size() << "\r\n";
    oss << "Connection: close\r\n\r\n";
    oss << body;
    std::string resp = oss.str();
    (void)send(fd, resp.data(), resp.size(), 0);
}

bool download_http_to_file(const std::string &url, const std::string &tmp_path, size_t *downloaded, std::string *reason)
{
    std::string host;
    std::string path;
    int port = 0;
    if (!parse_http_url(url, &host, &port, &path)) {
        *reason = "only http download is supported in this build";
        return false;
    }

    struct addrinfo hints;
    std::memset(&hints, 0, sizeof(hints));
    hints.ai_family = AF_UNSPEC;
    hints.ai_socktype = SOCK_STREAM;
    struct addrinfo *res = nullptr;
    std::string port_str = std::to_string(port);
    if (getaddrinfo(host.c_str(), port_str.c_str(), &hints, &res) != 0 || res == nullptr) {
        *reason = "resolve host failed";
        return false;
    }

    int fd = socket(res->ai_family, res->ai_socktype, res->ai_protocol);
    if (fd < 0) {
        freeaddrinfo(res);
        *reason = "create socket failed";
        return false;
    }

    /* A stalled model server must time out rather than block the receiver forever. */
    timeval tv;
    tv.tv_sec = 15;
    tv.tv_usec = 0;
    (void)setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, &tv, sizeof(tv));
    (void)setsockopt(fd, SOL_SOCKET, SO_SNDTIMEO, &tv, sizeof(tv));

    if (connect(fd, res->ai_addr, res->ai_addrlen) != 0) {
        close(fd);
        freeaddrinfo(res);
        *reason = "connect failed";
        return false;
    }
    freeaddrinfo(res);

    std::ostringstream req;
    req << "GET " << path << " HTTP/1.1\r\n";
    req << "Host: " << host;
    if (port != 80) {
        req << ":" << port;
    }
    req << "\r\nConnection: close\r\n\r\n";
    std::string req_str = req.str();
    if (send(fd, req_str.data(), req_str.size(), 0) != (ssize_t)req_str.size()) {
        close(fd);
        *reason = "send request failed";
        return false;
    }

    FILE *fp = std::fopen(tmp_path.c_str(), "wb");
    if (fp == nullptr) {
        close(fd);
        *reason = "open tmp file failed";
        return false;
    }

    std::string header;
    bool header_done = false;
    int status_code = 0;
    *downloaded = 0;
    char buf[4096];
    ssize_t n;
    while ((n = recv(fd, buf, sizeof(buf), 0)) > 0) {
        if (!header_done) {
            header.append(buf, (size_t)n);
            size_t pos = header.find("\r\n\r\n");
            if (pos == std::string::npos) {
                if (header.size() > 65536) {
                    std::fclose(fp);
                    close(fd);
                    *reason = "response header too large";
                    return false;
                }
                continue;
            }
            std::string status_line = header.substr(0, header.find("\r\n"));
            if (std::sscanf(status_line.c_str(), "HTTP/%*s %d", &status_code) != 1 || status_code != 200) {
                std::fclose(fp);
                close(fd);
                *reason = "http status is not 200";
                return false;
            }
            size_t body_start = pos + 4;
            size_t body_size = header.size() - body_start;
            if (body_size > 0) {
                if (std::fwrite(header.data() + body_start, 1, body_size, fp) != body_size) {
                    std::fclose(fp);
                    close(fd);
                    *reason = "write tmp file failed";
                    return false;
                }
                *downloaded += body_size;
            }
            header_done = true;
        } else {
            if (std::fwrite(buf, 1, (size_t)n, fp) != (size_t)n) {
                std::fclose(fp);
                close(fd);
                *reason = "write tmp file failed";
                return false;
            }
            *downloaded += (size_t)n;
        }
    }

    std::fclose(fp);
    close(fd);
    if (!header_done) {
        *reason = "invalid http response";
        return false;
    }
    if (*downloaded == 0) {
        *reason = "downloaded file is empty";
        return false;
    }
    return true;
}

void handle_latest_model_request(int client_fd, const std::string &target)
{
    ReceiverConfig cfg;
    {
        std::lock_guard<std::mutex> lock(g_cfg_mutex);
        cfg = g_cfg;
    }

    std::string device_id = extract_query_param(target, "deviceId");
    std::string model_url = extract_query_param(target, "modelDownloadPath");
    std::printf("[MODEL] request deviceId=%s modelDownloadPath=%s\n", device_id.c_str(), model_url.c_str());

    /* Read device identity for every request so provisioning changes need no service restart. */
    std::string expected_device = load_device_id(cfg.webcam_config_path);
    if (expected_device.empty() || device_id != expected_device) {
        send_json(client_fd, 403, "Forbidden", "{\"success\":false,\"msg\":\"deviceId mismatch\"}");
        return;
    }
    if (!(starts_with(model_url, "http://") || starts_with(model_url, "https://"))) {
        send_json(client_fd, 400, "Bad Request", "{\"success\":false,\"msg\":\"invalid modelDownloadPath scheme\"}");
        return;
    }
    if (!ends_with_om_url(model_url)) {
        send_json(client_fd, 400, "Bad Request", "{\"success\":false,\"msg\":\"model suffix must be .om\"}");
        return;
    }
    if (cfg.validate_model == nullptr || cfg.switch_model == nullptr) {
        send_json(client_fd, 500, "Internal Server Error", "{\"success\":false,\"msg\":\"model callback not ready\"}");
        return;
    }

    std::string model_dir = dirname_of(cfg.model_path);
    if (!ensure_dir(model_dir)) {
        send_json(client_fd, 500, "Internal Server Error", "{\"success\":false,\"msg\":\"create model dir failed\"}");
        return;
    }

    /* Always download to a temporary path; never overwrite the active model directly. */
    std::string tmp_path = cfg.model_path + ".tmp";
    size_t size = 0;
    std::string reason;
    (void)unlink(tmp_path.c_str());
    if (!download_http_to_file(model_url, tmp_path, &size, &reason)) {
        std::printf("[MODEL] load failed keep old model reason=download failed: %s\n", reason.c_str());
        (void)unlink(tmp_path.c_str());
        send_json(client_fd, 502, "Bad Gateway", "{\"success\":false,\"msg\":\"download failed\"}");
        return;
    }
    std::printf("[MODEL] download ok path=%s size=%zu\n", tmp_path.c_str(), size);

    /* Load and self-test the temporary OM without replacing active inference resources. */
    if (cfg.validate_model(tmp_path.c_str()) != TD_SUCCESS) {
        std::printf("[MODEL] load failed keep old model reason=model validate failed\n");
        (void)unlink(tmp_path.c_str());
        send_json(client_fd, 400, "Bad Request", "{\"success\":false,\"msg\":\"model validate failed\"}");
        return;
    }
    std::printf("[MODEL] load new model ok\n");

    /* Publish the validated model atomically with rename on the same filesystem. */
    if (rename(tmp_path.c_str(), cfg.model_path.c_str()) != 0) {
        std::printf("[MODEL] load failed keep old model reason=rename failed errno=%d (%s)\n",
            errno, std::strerror(errno));
        (void)unlink(tmp_path.c_str());
        send_json(client_fd, 500, "Internal Server Error", "{\"success\":false,\"msg\":\"rename failed\"}");
        return;
    }

    /* Keep the old in-memory model and report failure if the hot switch fails. */
    if (cfg.switch_model(cfg.model_path.c_str()) != TD_SUCCESS) {
        std::printf("[MODEL] load failed keep old model reason=switch failed\n");
        send_json(client_fd, 500, "Internal Server Error", "{\"success\":false,\"msg\":\"switch failed\"}");
        return;
    }

    std::printf("[MODEL] switched current model=%s\n", cfg.model_path.c_str());
    send_json(client_fd, 200, "OK", "{\"success\":true,\"msg\":\"model updated\"}");
}

void handle_client(int client_fd)
{
    std::string req;
    char buf[1024];
    while (req.size() < kMaxRequestBytes) {
        ssize_t n = recv(client_fd, buf, sizeof(buf), 0);
        if (n <= 0) {
            break;
        }
        req.append(buf, (size_t)n);
        if (req.find("\r\n\r\n") != std::string::npos) {
            break;
        }
    }

    std::string line = req.substr(0, req.find("\r\n"));
    std::istringstream iss(line);
    std::string method;
    std::string target;
    std::string version;
    iss >> method >> target >> version;
    if (method != "GET") {
        send_json(client_fd, 405, "Method Not Allowed", "{\"success\":false,\"msg\":\"method not allowed\"}");
        return;
    }
    if (target.find("/vlsDeviceInfo/latest-training-model") != 0) {
        send_json(client_fd, 404, "Not Found", "{\"success\":false,\"msg\":\"not found\"}");
        return;
    }

    handle_latest_model_request(client_fd, target);
}

void server_thread()
{
    ReceiverConfig cfg;
    {
        std::lock_guard<std::mutex> lock(g_cfg_mutex);
        cfg = g_cfg;
    }

    g_listen_fd = socket(AF_INET, SOCK_STREAM, 0);
    if (g_listen_fd < 0) {
        std::printf("[MODEL] receiver start failed reason=socket errno=%d\n", errno);
        return;
    }

    int reuse = 1;
    (void)setsockopt(g_listen_fd, SOL_SOCKET, SO_REUSEADDR, &reuse, sizeof(reuse));

    sockaddr_in addr;
    std::memset(&addr, 0, sizeof(addr));
    addr.sin_family = AF_INET;
    addr.sin_port = htons((uint16_t)cfg.listen_port);
    if (cfg.listen_host == "0.0.0.0") {
        addr.sin_addr.s_addr = htonl(INADDR_ANY);
    } else if (inet_pton(AF_INET, cfg.listen_host.c_str(), &addr.sin_addr) != 1) {
        std::printf("[MODEL] receiver start failed reason=invalid listen host %s\n", cfg.listen_host.c_str());
        close(g_listen_fd);
        g_listen_fd = -1;
        return;
    }

    if (bind(g_listen_fd, (sockaddr *)&addr, sizeof(addr)) != 0) {
        std::printf("[MODEL] receiver start failed reason=bind errno=%d (%s)\n", errno, std::strerror(errno));
        close(g_listen_fd);
        g_listen_fd = -1;
        return;
    }
    if (listen(g_listen_fd, 4) != 0) {
        std::printf("[MODEL] receiver start failed reason=listen errno=%d\n", errno);
        close(g_listen_fd);
        g_listen_fd = -1;
        return;
    }

    std::printf("[MODEL] receiver started listen=%s:%d\n", cfg.listen_host.c_str(), cfg.listen_port);
    /* The select timeout checks for shutdown periodically even when no client connects. */
    while (g_running.load()) {
        fd_set fds;
        FD_ZERO(&fds);
        FD_SET(g_listen_fd, &fds);
        timeval tv;
        tv.tv_sec = 1;
        tv.tv_usec = 0;
        int ret = select(g_listen_fd + 1, &fds, nullptr, nullptr, &tv);
        if (ret <= 0 || !FD_ISSET(g_listen_fd, &fds)) {
            continue;
        }
        int client_fd = accept(g_listen_fd, nullptr, nullptr);
        if (client_fd < 0) {
            continue;
        }
        handle_client(client_fd);
        close(client_fd);
    }

    close(g_listen_fd);
    g_listen_fd = -1;
}

void copy_attr_string(td_char *dst, size_t dst_size, const std::string &src)
{
    if (dst == nullptr || dst_size == 0) {
        return;
    }
    std::snprintf(dst, dst_size, "%s", src.c_str());
}

} // namespace

td_void model_receiver_load_config(model_receiver_attr *attr, const td_char *video_config_path)
{
    if (attr == nullptr) {
        return;
    }

    bool enabled = false;
    std::string listen = kDefaultListen;
    std::string model_path = kDefaultModelPath;
    std::string webcam_config_path = kDefaultWebcamConfigPath;
    std::string json = read_text_file(video_config_path != nullptr ? video_config_path : kDefaultVideoConfigPath);

    (void)parse_json_bool(json, "enable_model_receiver", &enabled);
    (void)parse_json_string(json, "receiver_listen", &listen);
    (void)parse_json_string(json, "ai_model_path", &model_path);

    attr->enabled = enabled ? TD_TRUE : TD_FALSE;
    std::string listen_host;
    if (!split_host_port(listen, &listen_host, &attr->listen_port)) {
        listen_host = "0.0.0.0";
        attr->listen_port = 8888;
    }
    copy_attr_string(attr->listen_host, sizeof(attr->listen_host), listen_host);
    copy_attr_string(attr->model_path, sizeof(attr->model_path), model_path);
    copy_attr_string(attr->webcam_config_path, sizeof(attr->webcam_config_path), webcam_config_path);
}

td_s32 model_receiver_start(const model_receiver_attr *attr)
{
    if (attr == nullptr || attr->enabled != TD_TRUE) {
        return TD_SUCCESS;
    }
    if (g_running.load()) {
        return TD_SUCCESS;
    }

    {
        std::lock_guard<std::mutex> lock(g_cfg_mutex);
        g_cfg.enabled = true;
        g_cfg.listen_host = attr->listen_host[0] != '\0' ? attr->listen_host : "0.0.0.0";
        g_cfg.listen_port = attr->listen_port > 0 ? attr->listen_port : 8888;
        g_cfg.model_path = attr->model_path[0] != '\0' ? attr->model_path : kDefaultModelPath;
        g_cfg.webcam_config_path = attr->webcam_config_path[0] != '\0' ?
            attr->webcam_config_path : kDefaultWebcamConfigPath;
        g_cfg.validate_model = attr->validate_model;
        g_cfg.switch_model = attr->switch_model;
    }

    g_running.store(true);
    g_thread = std::thread(server_thread);
    return TD_SUCCESS;
}

td_void model_receiver_stop(td_void)
{
    if (!g_running.load()) {
        return;
    }
    g_running.store(false);
    if (g_listen_fd >= 0) {
        shutdown(g_listen_fd, SHUT_RDWR);
    }
    if (g_thread.joinable()) {
        g_thread.join();
    }
}
