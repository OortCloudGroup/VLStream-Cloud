# Hi3519DV500 AI Camera Business Source

This directory exports the business source from the current Hi3519DV500 project. It contains:

1. RTSP/WebRTC video streaming and keyframe handling.
2. Asynchronous AI detection event reporting and JPEG generation.
3. OM model reception, validation, and runtime hot switching.
4. AI inference bridging, ACL model lifecycle management, and runtime configuration loading.

## Directory Layout

- `src/rtsp_streamer.c`: Media pipeline, thread orchestration, and integration entry point.
- `src/webrtc_bridge.c`: WebRTC SDK initialization, session management, keyframe gating, and video input.
- `src/http_reporter.cpp`: Event queue, JPEG encoding, and reporting to two backend platforms.
- `src/model_receiver.cpp`: Model update HTTP service, download, validation, and atomic replacement.
- `src/ai_bridge.cpp`: C-to-C++ boundary for the ACL inference backend.
- `src/ai_acl_adapter.cpp`: OM loading, inference, post-processing, and hot switching.
- `src/ai_runtime_config.cpp`: AI switch, model path, and class file configuration loading.
- `include/`: Public headers for the modules above.
- `config/`: Board-side `video.conf`, `webcam.conf`, class labels, and a WebRTC configuration example.
- `rtsp_lib/`: RTSP source used by `rtsp_streamer.c`.
- `third_party/stb_image_write.h`: JPEG encoder used for event snapshots.
- `third_party/webrtc/webrtc_streamer.h`: External WebRTC SDK API declarations.
- `docs/`: WebRTC porting notes and project debugging records.

## Excluded Files

- `.om` model files: runtime assets rather than business source.
- Compiled `rtsp_streamer` and offline verification binaries: rebuild them from the original project.
- WebRTC, MPP, ACL, and mbedTLS static libraries: large, version-specific SDK dependencies.
- HiSilicon SDK public headers and `sample_comm` sources: supplied by the original SDK tree.

## Original Project Locations

Business source directory:

```text
smp/a55_linux/source/mpp/sample/rtsp_streamer
```

Board configuration directory:

```text
smp/a55_linux/source/bsp/rootfs_scripts/rootfs/mnt/webrtc
```

External WebRTC SDK directory:

```text
/home/oort/Desktop/hi3519dv500+IMX335/webrtc-stream-gcc-20230609-aarch64-v01c01-linux-musl
```

## Build Notes

The root `Makefile` is a copy of the original project build rules. It still depends on the HiSilicon
SDK's `Makefile.param`, `smp_linux.mak`, MPP/ACL libraries, and cross toolchain. This export is intended
to consolidate and hand off the business source; it is not a standalone build outside the original SDK.
