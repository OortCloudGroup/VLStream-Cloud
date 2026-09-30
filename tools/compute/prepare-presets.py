"""Download official preset weights once and record an integrity manifest on the user's instance."""
import argparse
import hashlib
import json
import os
from pathlib import Path


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--work-dir', required=True)
    args = parser.parse_args()
    root = Path(args.work_dir).resolve()
    cache = root / 'models'
    cache.mkdir(parents=True, exist_ok=True)
    os.environ['TORCH_HOME'] = str(cache / 'torch')
    os.environ['YOLO_CONFIG_DIR'] = str(root / 'config/ultralytics')
    from ultralytics import YOLO
    from torchvision.models.segmentation import deeplabv3_resnet50, DeepLabV3_ResNet50_Weights
    files = {}
    for task, name in [('detect', 'yolov8m.pt'), ('classify', 'yolov8n-cls.pt'), ('segment', 'yolov8n-seg.pt')]:
        model = YOLO(str(cache / name))
        if model.task != task:
            raise ValueError('Preset task mismatch: ' + name)
        del model
        files[name] = None
    model = deeplabv3_resnet50(weights=DeepLabV3_ResNet50_Weights.COCO_WITH_VOC_LABELS_V1)
    del model
    files['torch/hub/checkpoints/deeplabv3_resnet50_coco-cd0a2569.pth'] = None
    for name in files:
        digest = hashlib.sha256()
        with (cache / name).open('rb') as stream:
            for block in iter(lambda: stream.read(1024 * 1024), b''):
                digest.update(block)
        files[name] = digest.hexdigest()
    temporary = cache / 'presets.json.tmp'
    temporary.write_text(json.dumps(dict(format=1, ultralytics='8.3.240', files=files), indent=2), encoding='utf-8')
    temporary.replace(cache / 'presets.json')
    print('Four preset models downloaded and verified locally.')


if __name__ == '__main__':
    main()
