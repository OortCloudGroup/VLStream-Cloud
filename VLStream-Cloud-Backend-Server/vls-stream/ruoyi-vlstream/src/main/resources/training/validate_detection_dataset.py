"""Read-only validation of the exact generated dataset before GPU enqueue."""
import hashlib
import json
import math
import sys
from pathlib import Path

import yaml
from PIL import Image


def validate(dataset, model):
    yaml_path = Path(dataset).resolve(strict=True)
    model_path = Path(model)
    if not model_path.is_absolute() or not model_path.is_file() or model_path.stat().st_size == 0:
        raise ValueError('基础模型文件不存在、为空或不是绝对路径')
    config = yaml.safe_load(yaml_path.read_text(encoding='utf-8'))
    if not isinstance(config, dict):
        raise ValueError('数据集 YAML 必须为对象')
    names = config.get('names')
    if isinstance(names, dict):
        if set(names) != set(range(len(names))):
            raise ValueError('类别编号必须从 0 开始连续排列')
        names = [names[i] for i in range(len(names))]
    if not isinstance(names, list) or not names or any(not isinstance(n, str) or not n.strip() for n in names):
        raise ValueError('数据集类别为空或格式不正确')
    if len(set(names)) != len(names) or config.get('nc', len(names)) != len(names):
        raise ValueError('类别名称重复或 nc 与 names 不一致')
    root = Path(config.get('path') or yaml_path.parent)
    if not root.is_absolute():
        root = yaml_path.parent / root
    root = root.resolve(strict=True)
    hashes = {}
    counts = {}
    for split in ('train', 'val'):
        location = config.get(split)
        if not isinstance(location, str) or not location:
            raise ValueError('训练集和验证集必须各指定一个图片目录')
        directory = Path(location)
        directory = (directory if directory.is_absolute() else root / directory).resolve(strict=True)
        if root not in directory.parents or not directory.is_dir():
            raise ValueError('训练集或验证集路径超出数据集目录')
        images = sorted(p for p in directory.rglob('*') if p.suffix.lower() in {'.jpg', '.jpeg', '.png', '.bmp', '.webp'})
        if not images or len(images) > 10000:
            raise ValueError('每个集合需包含 1 至 10000 张图片')
        boxes = 0
        for image in images:
            if not image.is_file() or root not in image.resolve().parents:
                raise ValueError('图片路径无效或超出数据集目录')
            if image.stat().st_size > 25 * 1024 * 1024:
                raise ValueError('单张图片超过 25 MiB')
            with Image.open(image) as content:
                content.verify()
            digest = hashlib.sha256(image.read_bytes()).hexdigest()
            if digest in hashes and hashes[digest] != split:
                raise ValueError('训练集和验证集含相同图片内容，请重新划分并生成数据集')
            hashes[digest] = split
            relative = image.relative_to(root)
            parts = list(relative.parts)
            if not parts or parts[0] != 'images':
                raise ValueError('图片须位于 images 目录')
            label = root.joinpath('labels', *parts[1:]).with_suffix('.txt')
            if not label.is_file() or root not in label.resolve().parents:
                raise ValueError('图片缺少对应标签文件：' + image.name)
            if label.stat().st_size > 1024 * 1024:
                raise ValueError('标签文件过大')
            for line in label.read_text(encoding='utf-8-sig').splitlines():
                if not line.strip():
                    continue
                fields = line.split()
                if len(fields) != 5 or not fields[0].isdigit():
                    raise ValueError('目标检测标签必须为类别编号和四个坐标：' + label.name)
                category = int(fields[0])
                x, y, w, h = map(float, fields[1:])
                if category >= len(names) or not all(math.isfinite(v) for v in (x, y, w, h)):
                    raise ValueError('标签类别越界或坐标不是有效数值')
                if not (0 <= x <= 1 and 0 <= y <= 1 and 0 < w <= 1 and 0 < h <= 1):
                    raise ValueError('标签坐标必须归一化到 0 至 1，宽高必须大于 0')
                if x-w/2 < -1e-5 or y-h/2 < -1e-5 or x+w/2 > 1.00001 or y+h/2 > 1.00001:
                    raise ValueError('标注框超出图片范围')
                boxes += 1
        if not boxes:
            raise ValueError('训练集和验证集都必须包含有效目标标注')
        counts[split] = {'images': len(images), 'boxes': boxes}
    return {'valid': True, 'classes': len(names), 'splits': counts}


if __name__ == '__main__':
    try:
        result = validate(sys.argv[1], sys.argv[2])
    except Exception as error:
        result = {'valid': False, 'message': str(error)[:300]}
    print('VLS_DATASET_CHECK=' + json.dumps(result, ensure_ascii=True))
