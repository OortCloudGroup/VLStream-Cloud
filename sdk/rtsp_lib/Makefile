# RTSP Library Makefile for Hi3519DV500 (64-bit ARM)

# Use aarch64 toolchain
CC ?= aarch64-v01c01-linux-musl-gcc
AR ?= aarch64-v01c01-linux-musl-ar
RANLIB ?= aarch64-v01c01-linux-musl-ranlib

TARGET = librtsp.a

CFLAGS += -g -Wall -D__LINUX__ -fPIC

OBJS = rtsp_demo.o rtsp_msg.o rtp_enc.o stream_queue.o utils.o

all: $(TARGET)

$(TARGET): $(OBJS)
	$(AR) -r $(TARGET) $(OBJS)
	$(RANLIB) $(TARGET)

%.o: %.c
	$(CC) -c $(CFLAGS) -o $@ $<

clean:
	/bin/rm -f *.o
	/bin/rm -f $(TARGET)

.PHONY: all clean
