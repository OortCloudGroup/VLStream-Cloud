"""Bounded video history and evidence: actual timestamps, no synthetic pre-event frames."""
from collections import deque
from fractions import Fraction
import hashlib
import json
import os
from pathlib import Path
import time


def atomic_json(path, value):
    path = Path(path)
    temporary = path.with_suffix(path.suffix + '.tmp')
    with open(temporary, 'w', encoding='utf-8') as stream:
        json.dump(value, stream, ensure_ascii=False, separators=(',', ':'))
        stream.flush()
        os.fsync(stream.fileno())
    os.replace(temporary, path)


def digest(path):
    value = hashlib.sha256()
    with open(path, 'rb') as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b''):
            value.update(chunk)
    return value.hexdigest()


def file_evidence(path):
    path = Path(path)
    return dict(name=path.name, size=path.stat().st_size, sha256=digest(path))


def save_event_images(directory, frame, detections):
    """Render evidence offline. Category names remain in event metadata for the UI.

    Ultralytics' plot() may download a Unicode font on the first Chinese label;
    that network dependency must never block capture, heartbeats or stopping.
    """
    import cv2
    annotated = frame.copy()
    height, width = frame.shape[:2]
    for detection in detections:
        x1, y1, x2, y2 = detection['box']
        x1, x2 = max(0, min(width - 1, round(x1))), max(0, min(width - 1, round(x2)))
        y1, y2 = max(0, min(height - 1, round(y1))), max(0, min(height - 1, round(y2)))
        cv2.rectangle(annotated, (x1, y1), (x2, y2), (28, 112, 255), 2)
        label = 'class {}  {:.1f}%'.format(detection['classId'], detection['confidence'] * 100)
        cv2.putText(annotated, label, (x1, max(18, y1 - 6)), cv2.FONT_HERSHEY_SIMPLEX,
                    .5, (28, 112, 255), 1, cv2.LINE_AA)
    if not cv2.imwrite(str(Path(directory) / 'original.jpg'), frame) or not cv2.imwrite(str(Path(directory) / 'annotated.jpg'), annotated):
        raise RuntimeError('EVENT_IMAGE_WRITE_FAILED')


class FrameHistory:
    def __init__(self, seconds, max_bytes=64 * 1024 * 1024, fps=10):
        self.seconds = seconds
        self.max_bytes = max_bytes
        self.fps = fps
        self.frames = deque()
        self.bytes = 0
        self.latest = None
        self.last_recorded = 0
        self.received = 0

    def receive(self, frame, at=None):
        import cv2
        at = int(time.time() * 1000) if at is None else at
        self.received += 1
        # A single decoded input feeds all of this camera's models.
        array = frame.to_ndarray(format='bgr24')
        self.latest = (at, array)
        if at - self.last_recorded < 1000 / self.fps:
            return
        self.last_recorded = at
        height, width = array.shape[:2]
        if width > 960:
            array = cv2.resize(array, (960, max(2, int(height * 960 / width) // 2 * 2)))
        ok, encoded = cv2.imencode('.jpg', array, [cv2.IMWRITE_JPEG_QUALITY, 82])
        if not ok:
            raise RuntimeError('VIDEO_BUFFER_ENCODE_FAILED')
        self.append(at, encoded.tobytes())

    def append(self, at, encoded):
        self.frames.append((at, encoded))
        self.bytes += len(encoded)
        while self.frames and (self.bytes > self.max_bytes or at - self.frames[0][0] > self.seconds * 1000):
            self.bytes -= len(self.frames.popleft()[1])

    def between(self, start, end):
        return [(at, data) for at, data in self.frames if start <= at <= end]


def encode_clip(path, frames, event_at, pre_seconds, post_seconds):
    import av
    import cv2
    import numpy as np
    if len(frames) < 2:
        raise RuntimeError('VIDEO_FRAMES_INSUFFICIENT')
    first = cv2.imdecode(np.frombuffer(frames[0][1], dtype=np.uint8), cv2.IMREAD_COLOR)
    height, width = first.shape[:2]
    width, height = width // 2 * 2, height // 2 * 2
    container = av.open(str(path), 'w', format='mp4', options={'movflags': '+faststart'})
    try:
        stream = container.add_stream('libx264', rate=10)
        stream.width, stream.height = width, height
        stream.pix_fmt = 'yuv420p'
        stream.time_base = Fraction(1, 1000)
        stream.codec_context.time_base = Fraction(1, 1000)
        stream.options = {'preset': 'veryfast', 'crf': '25'}
        previous = -1
        for at, jpeg in frames:
            image = cv2.imdecode(np.frombuffer(jpeg, dtype=np.uint8), cv2.IMREAD_COLOR)
            if image is None:
                raise RuntimeError('VIDEO_BUFFER_DECODE_FAILED')
            if image.shape[:2] != (height, width):
                image = cv2.resize(image, (width, height))
            frame = av.VideoFrame.from_ndarray(image, format='bgr24')
            frame.pts = max(previous + 1, at - frames[0][0])
            frame.time_base = Fraction(1, 1000)
            previous = frame.pts
            for packet in stream.encode(frame):
                container.mux(packet)
        for packet in stream.encode():
            container.mux(packet)
    finally:
        container.close()
    pre = max(0, min(pre_seconds, (event_at - frames[0][0]) / 1000))
    post = max(0, min(post_seconds, (frames[-1][0] - event_at) / 1000))
    gaps = [right[0] - left[0] for left, right in zip(frames, frames[1:])]
    return dict(actualPreSeconds=round(pre, 3), actualPostSeconds=round(post, 3), frameCount=len(frames),
                gapMillis=max(gaps), mediaState='PARTIAL' if pre < pre_seconds - .25 or post < post_seconds - .25 or max(gaps) > 1000 else 'READY')


class ReportIntervals:
    """Intervals are scoped to camera/model/class, so one class never suppresses another."""
    def __init__(self):
        self.last = {}

    def eligible(self, camera, model, detections, at, seconds):
        return [item for item in detections if at - self.last.get((camera, model, item['classId']), -10**18) >= seconds * 1000]

    def recorded(self, camera, model, detections, at):
        for item in detections:
            self.last[camera, model, item['classId']] = at
