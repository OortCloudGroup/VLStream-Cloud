override PWD := $(CURDIR)
include ../Makefile.param

# Main executable and shared directories
TARGET := rtsp_streamer
TARGET_PATH := $(CURDIR)
COMMON_DIR := $(CURDIR)/../common
AUDIO_ADP_DIR := $(CURDIR)/../audio/adp

# In-tree RTSP static library
RTSP_LIB_DIR := $(CURDIR)/rtsp_lib
SENSOR_LIBS += $(RTSP_LIB_DIR)/librtsp.a

# External WebRTC Streamer SDK; use the board-side musl + mbedTLS build by default.
WEBRTC_SDK_ROOT ?= /home/oort/Desktop/hi3519dv500+IMX335/webrtc-stream-gcc-20230609-aarch64-v01c01-linux-musl
WEBRTC_MBEDTLS_DIR := $(WEBRTC_SDK_ROOT)/lib/pcmu/mbedtls
WEBRTC_INC_DIR := $(WEBRTC_SDK_ROOT)/include

SENSOR_LIBS += $(WEBRTC_MBEDTLS_DIR)/libwebrtc-stream.a
SENSOR_LIBS += $(WEBRTC_MBEDTLS_DIR)/libmbedx509.a
SENSOR_LIBS += $(WEBRTC_MBEDTLS_DIR)/libmbedtls.a
SENSOR_LIBS += $(WEBRTC_MBEDTLS_DIR)/libmbedcrypto.a

# Additional project include directories
CFLAGS += -I$(RTSP_LIB_DIR)
CFLAGS += -I$(WEBRTC_INC_DIR)

# Main sources: C owns the media pipeline; C++ owns AI, reporting, and model updates.
SMP_SRCS := rtsp_streamer.c webrtc_bridge.c
SMP_CPP_SRCS := ai_bridge.cpp ai_acl_adapter.cpp ai_runtime_config.cpp http_reporter.cpp model_receiver.cpp

# Reuse the SDK sample build and link rules to produce rtsp_streamer.
include $(CURDIR)/../smp_linux.mak

AI_VERIFY_TARGETS := ai_yolov8_verify ai_yolo11_verify
# The two offline verification tools are linked separately and are not part of rtsp_streamer.
AI_YOLOV8_VERIFY_OBJS := ai_yolov8_verify.o
AI_YOLO11_VERIFY_OBJS := ai_yolo11_verify.o

all: $(AI_VERIFY_TARGETS)

ai_yolov8_verify: $(AI_YOLOV8_VERIFY_OBJS)
	@$(CXX) $(CFLAGS) -o $(TARGET_PATH)/$@ $^ -Wl,--start-group $(MPI_LIBS) $(REL_LIB)/libsecurec.a -Wl,--end-group

ai_yolo11_verify: $(AI_YOLO11_VERIFY_OBJS)
	@$(CXX) $(CFLAGS) -o $(TARGET_PATH)/$@ $^ -Wl,--start-group $(MPI_LIBS) $(REL_LIB)/libsecurec.a -Wl,--end-group

clean: clean_ai_verify

.PHONY: clean_ai_verify
clean_ai_verify:
	@rm -f $(addprefix $(TARGET_PATH)/,$(AI_VERIFY_TARGETS)) $(AI_YOLOV8_VERIFY_OBJS) $(AI_YOLO11_VERIFY_OBJS)
