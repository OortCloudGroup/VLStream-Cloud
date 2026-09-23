"""Read-only validation before a classification or pixel segmentation training job is queued."""
import base64
import hashlib
import io
import json
from pathlib import Path
import sys
import numpy as np
from PIL import Image
import yaml


def validate(dataset, model, kind, dataset_id=None):
    root = Path(dataset).resolve().parent
    meta = yaml.safe_load(Path(dataset).read_text(encoding='utf-8'))
    data = json.loads((root / 'vls-dataset.json').read_text(encoding='utf-8'))
    task = {'image_classification': 'classify', 'instance_segmentation': 'segment', 'semantic_segmentation': 'semanticSeg'}[kind]
    if data.get('format') != 2 or data.get('annotationType') != kind or meta.get('annotation_type') != kind:
        raise ValueError('数据集类型不匹配，请重新生成训练目录')
    if dataset_id and str(data.get('datasetId')) != str(dataset_id): raise ValueError('训练目录不属于当前数据集')
    if model != '@preset/' + task and (not Path(model).is_file() or Path(model).suffix != '.pt'):
        raise ValueError('基础模型不是可用的PT文件')
    names = data.get('names')
    if not isinstance(names, list) or not names or len(set(names)) != len(names) or names != meta.get('names') or meta.get('nc') != len(names):
        raise ValueError('类别名称与编号不一致')
    samples = data.get('samples')
    if not isinstance(samples, list) or len(samples) > 10000: raise ValueError('样本清单无效')
    hashes, identifiers = {}, set()
    counts = {'train': 0, 'val': 0}
    categories = {'train': set(), 'val': set()}

    def file(relative):
        target = (root / relative).resolve()
        if root not in target.parents or not target.is_file(): raise ValueError('样本文件缺失或超出数据集目录')
        return target

    for sample in samples:
        split = sample.get('split')
        if split not in counts: continue
        if sample['id'] in identifiers: raise ValueError('样本ID重复')
        identifiers.add(sample['id'])
        image_path = file(sample['path'])
        if image_path.stat().st_size > 25 * 1024 * 1024: raise ValueError('图片超过25 MiB')
        digest = hashlib.sha256(image_path.read_bytes()).hexdigest()
        if sample.get('sha256') and sample['sha256'] != digest: raise ValueError('图片校验和与冻结版本不一致')
        if digest in hashes and hashes[digest] != split: raise ValueError('训练集与验证集存在重复图片内容')
        hashes[digest] = split
        with Image.open(image_path) as image:
            width, height = image.size
            if width * height > 40000000 or width != sample['width'] or height != sample['height']: raise ValueError('图片尺寸无效')
            image.verify()
        target = file(sample['target'])
        if kind == 'semantic_segmentation':
            with Image.open(target) as mask:
                if mask.format != 'PNG' or mask.size != (width, height) or mask.mode not in ('L', 'I', 'I;16'):
                    raise ValueError('语义分割需要与图片同尺寸的类别索引PNG')
                values = np.asarray(mask)
                if values.min() < 0 or values.max() >= len(names): raise ValueError('语义像素引用了不存在的类别')
                categories[split].update(int(value) for value in np.unique(values))
        else:
            regions = json.loads(target.read_text(encoding='utf-8'))
            if not regions or len(regions) > 1000: raise ValueError('样本缺少有效标注')
            if kind == 'image_classification' and len(regions) != 1: raise ValueError('单标签分类样本必须只有一个类别')
            for region in regions:
                index = region.get('classIndex')
                if not isinstance(index, int) or index < 0 or index >= len(names): raise ValueError('标注类别编号无效')
                categories[split].add(index)
                if kind == 'image_classification':
                    if image_path.parent.name != 'c{:06d}'.format(index): raise ValueError('分类目录与类别编号不一致')
                    continue
                values = [region.get(key) for key in ('x', 'y', 'width', 'height')]
                if any(not isinstance(value, (int, float)) or not float(value).is_integer() for value in values): raise ValueError('实例掩膜坐标无效')
                x, y, w, h = map(int, values)
                if x < 0 or y < 0 or w <= 0 or h <= 0 or x + w > width or y + h > height: raise ValueError('实例掩膜越界')
                raw = base64.b64decode(region['maskData'], validate=True)
                if len(raw) > 8 * 1024 * 1024: raise ValueError('实例掩膜过大')
                with Image.open(io.BytesIO(raw)) as mask:
                    if mask.format != 'PNG' or mask.size != (w, h): raise ValueError('实例掩膜尺寸不一致')
                    pixels = np.asarray(mask.convert('RGBA'))
                    visible = pixels[:, :, 3] > 0
                    rgb = pixels[:, :, :3]
                    valid = (pixels[:, :, 3] == 0) | ((pixels[:, :, 3] == 255) & ((rgb == 0).all(2) | (rgb == 255).all(2)))
                    if not valid.all() or not (visible & (rgb[:, :, 0] == 255)).any(): raise ValueError('实例掩膜必须是非空二值像素区域')
        counts[split] += 1
    if not counts['train'] or not counts['val']: raise ValueError('训练和验证集合均需有效样本')
    if kind == 'image_classification' and any(values != set(range(len(names))) for values in categories.values()):
        raise ValueError('分类任务的训练集和验证集必须分别包含每个类别，请按类别重新划分')
    return dict(valid=True, annotationType=kind, train=counts['train'], val=counts['val'], classes=len(names))


if __name__ == '__main__':
    try:
        result = validate(sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4] if len(sys.argv) > 4 else None)
    except Exception as error:
        result = dict(valid=False, message=str(error))
    print('VLS_DATASET_CHECK=' + json.dumps(result, ensure_ascii=False))
