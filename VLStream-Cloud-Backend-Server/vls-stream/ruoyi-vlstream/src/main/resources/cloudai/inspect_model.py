"""Inspect only operator-enrolled legacy weights after matching their exact digest."""
import hashlib, json, os, sys
from pathlib import Path
path, expected = sys.argv[1:3]
actual = hashlib.sha256()
with open(path, 'rb') as stream:
    for chunk in iter(lambda: stream.read(1024 * 1024), b''):
        actual.update(chunk)
if actual.hexdigest() != expected:
    raise ValueError('MODEL_CHECKSUM_MISMATCH')
os.environ['CUDA_VISIBLE_DEVICES'] = ''
settings = Path(__file__).parent / 'yolo-config'
settings.mkdir(exist_ok=True)
os.environ['YOLO_CONFIG_DIR'] = str(settings)
from ultralytics import YOLO
model = YOLO(path)
if model.task != 'detect':
    raise ValueError('MODEL_IS_NOT_DETECTION')
print('VLS_MODEL=' + json.dumps(dict(task=model.task, sha256=expected, names=model.names), ensure_ascii=False))
