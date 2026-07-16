#include "ai_runtime_config.h"

#include <cstdio>
#include <cstring>
#include <string>

namespace {

/* Use verified defaults when optional fields are absent to preserve the known-good boot path. */
constexpr const char *kDefaultModelPath = "/mnt/ai_verify/yolov8.om";
constexpr const char *kDefaultClassesPath = "/mnt/webrtc/model/classes.txt";

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
    std::string text(static_cast<size_t>(size), '\0');
    size_t read_size = std::fread(&text[0], 1, text.size(), fp);
    std::fclose(fp);
    text.resize(read_size);
    return text;
}

bool parse_json_bool(const std::string &json, const char *key, bool *value)
{
    std::string marker = "\"" + std::string(key) + "\"";
    size_t pos = json.find(marker);
    if (pos == std::string::npos || (pos = json.find(':', pos + marker.size())) == std::string::npos) {
        return false;
    }
    std::string tail = json.substr(pos + 1, 8);
    if (tail.find("true") != std::string::npos) {
        *value = true;
        return true;
    }
    if (tail.find("false") != std::string::npos) {
        *value = false;
        return true;
    }
    return false;
}

bool parse_json_string(const std::string &json, const char *key, std::string *value)
{
    std::string marker = "\"" + std::string(key) + "\"";
    size_t pos = json.find(marker);
    if (pos == std::string::npos || (pos = json.find(':', pos + marker.size())) == std::string::npos ||
        (pos = json.find('"', pos + 1)) == std::string::npos) {
        return false;
    }

    std::string result;
    bool escaped = false;
    for (size_t i = pos + 1; i < json.size(); ++i) {
        char ch = json[i];
        if (escaped) {
            result.push_back(ch);
            escaped = false;
        } else if (ch == '\\') {
            escaped = true;
        } else if (ch == '"') {
            *value = result;
            return true;
        } else {
            result.push_back(ch);
        }
    }
    return false;
}

void copy_string(td_char *dst, size_t dst_size, const std::string &value)
{
    if (dst != nullptr && dst_size > 0) {
        std::snprintf(dst, dst_size, "%s", value.c_str());
    }
}

} // namespace

td_s32 ai_runtime_config_load(ai_runtime_config *config, const td_char *path)
{
    if (config == nullptr || path == nullptr) {
        return TD_FAILURE;
    }

    bool enable_ai = true;
    std::string model_path = kDefaultModelPath;
    std::string classes_path = kDefaultClassesPath;
    std::string text = read_text_file(path);
    if (text.empty()) {
        std::printf("[AI][WARN] runtime config %s not found, using defaults\n", path);
    } else {
        (void)parse_json_bool(text, "enable_ai", &enable_ai);
        (void)parse_json_string(text, "ai_model_path", &model_path);
        (void)parse_json_string(text, "ai_classes_path", &classes_path);
    }

    if (model_path.empty()) {
        std::printf("[AI][ERROR] ai_model_path is empty\n");
        return TD_FAILURE;
    }
    if (classes_path.empty()) {
        std::printf("[AI][ERROR] ai_classes_path is empty\n");
        return TD_FAILURE;
    }

    /* Parsed std::string values are local, so copy them into the persistent configuration. */
    config->enable_ai = enable_ai ? TD_TRUE : TD_FALSE;
    copy_string(config->model_path, sizeof(config->model_path), model_path);
    copy_string(config->classes_path, sizeof(config->classes_path), classes_path);
    std::printf("[AI] runtime config enable=%d model=%s classes=%s\n",
        config->enable_ai == TD_TRUE ? 1 : 0, config->model_path, config->classes_path);
    return TD_SUCCESS;
}
