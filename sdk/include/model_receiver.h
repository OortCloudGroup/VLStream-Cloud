#ifndef MODEL_RECEIVER_H
#define MODEL_RECEIVER_H

#include "sample_comm.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef td_s32 (*model_receiver_model_validate_fn)(const td_char *model_path);
typedef td_s32 (*model_receiver_model_switch_fn)(const td_char *model_path);

/* model_receiver_start() copies the receiver configuration and model callbacks. */
typedef struct {
    td_bool enabled;
    td_char listen_host[64];
    td_s32 listen_port;
    td_char model_path[256];
    td_char webcam_config_path[256];
    model_receiver_model_validate_fn validate_model;
    model_receiver_model_switch_fn switch_model;
} model_receiver_attr;

/* Read enable_model_receiver, receiver_listen, and ai_model_path. */
td_void model_receiver_load_config(model_receiver_attr *attr, const td_char *video_config_path);
/* Start the HTTP receiver thread; a disabled receiver succeeds without creating a thread. */
td_s32 model_receiver_start(const model_receiver_attr *attr);
/* Close the listening socket, wake the service thread, and join it. */
td_void model_receiver_stop(td_void);

#ifdef __cplusplus
}
#endif

#endif
