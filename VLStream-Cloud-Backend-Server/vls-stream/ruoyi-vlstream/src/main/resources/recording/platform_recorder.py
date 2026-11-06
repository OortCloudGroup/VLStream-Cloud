"""Bounded, browser-independent video recorder. Credentials arrive through stdin, never argv.

Finalized MP4 + JSON sidecars form a recoverable spool. A parent EOF/heartbeat timeout stops
the receiver. Source timestamps are preserved within a segment; reception gaps split files.
"""
import asyncio
import json
import os
from pathlib import Path
import queue
import shutil
import sys
import threading
import time
import uuid
from fractions import Fraction


def emit(value):
    print(json.dumps(value), flush=True)


class SegmentWriter:
    def __init__(self, directory, seconds=60, reserve=1024 ** 3):
        self.directory = Path(directory)
        self.directory.mkdir(parents=True, exist_ok=True)
        self.seconds, self.reserve = seconds, reserve
        self.container = self.encoder = None
        self.last_clock = self.last_pts = None

    def write(self, frame, captured_ms):
        import av
        if shutil.disk_usage(self.directory).free < self.reserve:
            raise RuntimeError("STORAGE_FULL")
        # Never encode a missing interval as a held frame in a continuous recording.
        if self.container and (captured_ms - self.start_ms >= self.seconds * 1000
                               or captured_ms - self.last_clock > 2000
                               or (frame.width, frame.height) != self.dimensions):
            self.close()
        if not self.container:
            self.identity = str(uuid.uuid4())
            self.start_ms = captured_ms
            self.dimensions = (frame.width, frame.height)
            self.container = av.open(str(self.directory / (self.identity + ".part")), "w", format="mp4",
                                     options={"movflags": "+faststart"})
            self.encoder = self.container.add_stream("libx264", rate=25)
            self.encoder.width, self.encoder.height = frame.width // 2 * 2, frame.height // 2 * 2
            self.encoder.pix_fmt = "yuv420p"
            self.encoder.time_base = Fraction(1, 1000)
            # Network delivery is bursty. The codec's default 1/25 clock collapses
            # adjacent millisecond timestamps and makes MP4 DTS non-monotonic.
            self.encoder.codec_context.time_base = Fraction(1, 1000)
            self.encoder.options = {"preset": "veryfast", "crf": "23"}
            self.last_pts = -1
        output = frame.reformat(width=self.encoder.width, height=self.encoder.height, format="yuv420p")
        output.pts = max(self.last_pts + 1, captured_ms - self.start_ms)
        output.time_base = Fraction(1, 1000)
        self.last_pts = output.pts
        for packet in self.encoder.encode(output):
            self.container.mux(packet)
        self.last_clock = captured_ms

    def close(self):
        if self.container is None:
            return
        for packet in self.encoder.encode():
            self.container.mux(packet)
        self.container.close()
        self.container = None
        path = self.directory / (self.identity + ".mp4")
        os.replace(self.directory / (self.identity + ".part"), path)
        info = dict(id=self.identity, startMs=self.start_ms, endMs=self.last_clock + 40, size=path.stat().st_size)
        temporary = self.directory / (self.identity + ".json.tmp")
        temporary.write_text(json.dumps(info), encoding="utf-8")
        os.replace(temporary, self.directory / (self.identity + ".json"))
        emit(dict(segment=info))


async def record(config):
    stop = threading.Event()
    frames = queue.Queue(maxsize=8)
    failures = queue.Queue()
    last_parent = [time.monotonic()]

    def control():
        for line in sys.stdin:
            if line.strip() == "stop":
                break
            last_parent[0] = time.monotonic()
        stop.set()
    threading.Thread(target=control, daemon=True).start()

    def frame_received(frame):
        item = (frame, int(time.time() * 1000))
        try:
            frames.put_nowait(item)
        except queue.Full:
            # Bounded memory under CPU pressure, with actual capture times retained.
            try:
                frames.get_nowait()
            except queue.Empty:
                pass
            try:
                frames.put_nowait(item)
            except queue.Full:
                pass

    def direct():
        import av
        try:
            source = config["source"]
            options = {"rtsp_transport": "tcp"} if source["url"].lower().startswith("rtsp:") else {}
            with av.open(source["url"], options=options, timeout=(15, 15)) as container:
                for frame in container.decode(video=0):
                    if stop.is_set():
                        break
                    frame_received(frame)
        except Exception:
            failures.put("SOURCE_UNAVAILABLE")
        finally:
            if not stop.is_set():
                failures.put("SOURCE_ENDED")

    source = config["source"]
    task = None
    if source["type"] == "CAMERA_RTC":
        from camera_rtc_receiver import CameraRtcReceiver
        receiver = CameraRtcReceiver(source["socketUrl"], source["cameraId"], frame_received)
        task = asyncio.create_task(receiver.run(stop))
    else:
        threading.Thread(target=direct, daemon=True).start()
    writer = SegmentWriter(config["directory"], config.get("segmentSeconds", 60), config.get("reserveBytes", 1024 ** 3))
    last_frame = last_report = time.monotonic()
    try:
        while not stop.is_set():
            now = time.monotonic()
            if now - last_parent[0] > 50:
                raise RuntimeError("PARENT_LOST")
            if now - last_frame > 30:
                raise RuntimeError("NO_FRAMES")
            if not failures.empty():
                raise RuntimeError(failures.get())
            if task and task.done():
                # Do not print exceptions containing upstream URLs or SDP.
                try:
                    task.result()
                except Exception:
                    pass
                raise RuntimeError("SOURCE_UNAVAILABLE")
            if not frames.empty():
                frame, timestamp = frames.get_nowait()
                await asyncio.to_thread(writer.write, frame, timestamp)
                last_frame = time.monotonic()
                if now - last_report > 2:
                    emit(dict(state="RECORDING"))
                    last_report = now
            else:
                await asyncio.sleep(.02)
    finally:
        stop.set()
        if task:
            task.cancel()
            await asyncio.gather(task, return_exceptions=True)
        await asyncio.to_thread(writer.close)


if __name__ == "__main__":
    if "--check" in sys.argv:
        import av, aiortc, aiohttp
        av.Codec("libx264", "w")
        emit(dict(ready=True))
    else:
        try:
            asyncio.run(record(json.loads(sys.stdin.readline())))
        except Exception as failure:
            code = str(failure)
            emit(dict(error=code if code in {"STORAGE_FULL", "NO_FRAMES", "PARENT_LOST", "SOURCE_ENDED", "SOURCE_UNAVAILABLE"} else "RECORDING_FAILED"))
            sys.exit(1)
