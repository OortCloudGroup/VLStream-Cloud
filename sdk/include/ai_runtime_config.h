#ifndef AI_RUNTIME_CONFIG_H
#define AI_RUNTIME_CONFIG_H

#include "sample_comm.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef struct {
    td_bool enable_ai;
    td_char model_path[256];
    td_char classes_path[256];
} ai_runtime_config;

/*
 * Load the AI switch and resource paths from video.conf. Values are copied
 * into fixed buffers and remain valid after this function returns.
 */
td_s32 ai_runtime_config_load(ai_runtime_config *config, const td_char *path);

#ifdef __cplusplus
}
#endif

#endif
