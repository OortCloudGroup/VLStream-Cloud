"""Typed image training/inference on the existing worker image (Ultralytics 8.3.240)."""
import base64
from copy import copy
import io
import json
import os
from pathlib import Path

TASKS = {'image_classification': 'classify', 'object_detection': 'detect',
         'instance_segmentation': 'segment', 'semantic_segmentation': 'semanticSeg'}


def atomic_json(path, value):
    path = Path(path)
    temporary = path.with_suffix('.tmp')
    temporary.write_text(json.dumps(value, ensure_ascii=False, allow_nan=False), encoding='utf-8')
    os.replace(temporary, path)


def child(root, relative):
    root = Path(root).resolve()
    result = (root / relative).resolve()
    if result != root and root not in result.parents:
        raise ValueError('数据文件超出数据集目录')
    return result


def manifest(dataset):
    root = Path(dataset).resolve().parent
    value = json.loads((root / 'vls-dataset.json').read_text(encoding='utf-8'))
    if value.get('format') != 2 or value.get('annotationType') not in TASKS:
        raise ValueError('请重新生成当前类型的训练数据集')
    return root, value


def mask_png(mask):
    import numpy as np
    from PIL import Image
    output = io.BytesIO()
    Image.fromarray(np.asarray(mask, dtype=np.uint8) * 255).save(output, format='PNG')
    return base64.b64encode(output.getvalue()).decode('ascii')


def binary_mask(region):
    import numpy as np
    from PIL import Image
    raw = base64.b64decode(region['maskData'], validate=True)
    with Image.open(io.BytesIO(raw)) as bitmap:
        if bitmap.format != 'PNG' or bitmap.size != (int(region['width']), int(region['height'])):
            raise ValueError('掩膜尺寸不匹配')
        rgba = np.asarray(bitmap.convert('RGBA'))
        return (rgba[:, :, 0] > 0) & (rgba[:, :, 3] > 0)


def crop_region(mask, name, confidence):
    import numpy as np
    ys, xs = np.nonzero(mask)
    if not len(xs):
        return None
    x, y, right, bottom = int(xs.min()), int(ys.min()), int(xs.max()) + 1, int(ys.max()) + 1
    return dict(className=str(name), confidence=float(confidence), x=x, y=y,
                width=right - x, height=bottom - y, maskData=mask_png(mask[y:bottom, x:right]))


def load_model(kind, source, cache):
    task = TASKS[kind]
    preset = source == '@preset/' + task
    if source.startswith('@preset/') and not preset:
        raise ValueError('系统模型与数据集类型不匹配')
    if not preset and not Path(source).is_file():
        raise ValueError('所选模型文件不存在')
    if task == 'semanticSeg':
        import torch
        from torchvision.models.segmentation import deeplabv3_resnet50, DeepLabV3_ResNet50_Weights
        if preset:
            os.environ.setdefault('TORCH_HOME', str(Path(cache) / 'torch'))
            weights = DeepLabV3_ResNet50_Weights.COCO_WITH_VOC_LABELS_V1
            network = deeplabv3_resnet50(weights=weights)
            return {'network': network, 'names': list(weights.meta['categories']), 'task': task}
        try: checkpoint = torch.load(source, map_location='cpu', weights_only=True)
        except Exception as error: raise ValueError('所选权重不是兼容的语义分割模型，请选择系统基础模型或平台生成的语义分割模型') from error
        if not isinstance(checkpoint, dict) or checkpoint.get('vls_task') != task or checkpoint.get('architecture') != 'deeplabv3_resnet50':
            raise ValueError('语义分割需要平台生成的 DeepLabV3 PT 模型，可选择系统基础模型开始训练')
        names = checkpoint.get('names')
        if not isinstance(names, list) or not names or any(not isinstance(name, str) or not name for name in names):
            raise ValueError('语义分割模型缺少类别信息')
        network = deeplabv3_resnet50(weights=None, weights_backbone=None, num_classes=len(names), aux_loss=checkpoint.get('aux_loss', False))
        network.load_state_dict(checkpoint['state_dict'], strict=True)
        return {'network': network, 'names': names, 'task': task}
    from ultralytics import YOLO
    if preset:
        filename = {'detect': 'yolov8m.pt', 'classify': 'yolov8n-cls.pt', 'segment': 'yolov8n-seg.pt'}[task]
        Path(cache).mkdir(parents=True, exist_ok=True)
        source = str(Path(cache) / filename)
    network = YOLO(source)
    if network.task != task:
        raise ValueError('所选模型任务类型为 {}，当前数据集需要 {}'.format(network.task, task))
    return {'network': network, 'names': network.names, 'task': task}


def instance_trainer():
    """Feed binary masks directly to YOLO's segmentation loss, including holes and separate regions."""
    import numpy as np
    import torch
    from PIL import Image
    from ultralytics.models.yolo.segment import SegmentationTrainer

    class MaskDataset(torch.utils.data.Dataset):
        def __init__(self, root, data, subset, size):
            self.root, self.imgsz, self.rect, self.mosaic = root, size, False, False
            self.items = [item for item in data['samples'] if item['split'] == subset]
            self.im_files = [str(child(root, item['path'])) for item in self.items]
            self.labels = []
            for item in self.items:
                regions = json.loads(child(root, item['target']).read_text(encoding='utf-8'))
                self.labels.append({'cls': np.asarray([[region['classIndex']] for region in regions], dtype=np.float32),
                                    'bboxes': np.asarray([[(region['x'] + region['width'] / 2) / item['width'],
                                                          (region['y'] + region['height'] / 2) / item['height'],
                                                          region['width'] / item['width'], region['height'] / item['height']]
                                                         for region in regions], dtype=np.float32)})

        def __len__(self): return len(self.items)

        def close_mosaic(self, hyp=None): self.mosaic = False

        def __getitem__(self, index):
            item = self.items[index]
            regions = json.loads(child(self.root, item['target']).read_text(encoding='utf-8'))
            with Image.open(child(self.root, item['path'])) as opened:
                source = opened.convert('RGB')
            width, height = source.size
            ratio = min(self.imgsz / width, self.imgsz / height)
            resized = (int(round(width * ratio)), int(round(height * ratio)))
            left, top = (self.imgsz - resized[0]) // 2, (self.imgsz - resized[1]) // 2
            image = Image.new('RGB', (self.imgsz, self.imgsz), (114, 114, 114))
            image.paste(source.resize(resized, Image.Resampling.BILINEAR), (left, top))
            masks, boxes, classes = [], [], []
            for region in regions:
                full = np.zeros((height, width), dtype=np.uint8)
                x, y, w, h = [int(region[key]) for key in ('x', 'y', 'width', 'height')]
                full[y:y+h, x:x+w] = binary_mask(region)
                transformed = Image.new('L', (self.imgsz, self.imgsz), 0)
                transformed.paste(Image.fromarray(full).resize(resized, Image.Resampling.NEAREST), (left, top))
                mask = np.asarray(transformed).copy()
                ys, xs = np.nonzero(mask)
                if not len(xs): continue
                x1, y1, x2, y2 = xs.min(), ys.min(), xs.max() + 1, ys.max() + 1
                masks.append(mask)
                boxes.append([(x1 + x2) / 2 / self.imgsz, (y1 + y2) / 2 / self.imgsz,
                              (x2 - x1) / self.imgsz, (y2 - y1) / self.imgsz])
                classes.append([region['classIndex']])
            return dict(img=torch.from_numpy(np.asarray(image).copy().transpose(2, 0, 1)),
                        cls=torch.tensor(classes, dtype=torch.float32).reshape(-1, 1),
                        bboxes=torch.tensor(boxes, dtype=torch.float32).reshape(-1, 4),
                        masks=torch.from_numpy(np.stack(masks) if masks else np.zeros((0, self.imgsz, self.imgsz), dtype=np.uint8)),
                        batch_idx=torch.zeros(len(classes)), im_file=str(child(self.root, item['path'])),
                        ori_shape=(height, width), resized_shape=(self.imgsz, self.imgsz), ratio_pad=((ratio, ratio), (left, top)))

        @staticmethod
        def collate_fn(samples):
            result = {'img': torch.stack([sample['img'] for sample in samples])}
            for key in ('cls', 'bboxes', 'masks'):
                result[key] = torch.cat([sample[key] for sample in samples], dim=0)
            result['batch_idx'] = torch.cat([sample['batch_idx'] + index for index, sample in enumerate(samples)])
            for key in ('im_file', 'ori_shape', 'resized_shape', 'ratio_pad'):
                result[key] = tuple(sample[key] for sample in samples)
            return result

    class PixelMaskTrainer(SegmentationTrainer):
        def build_dataset(self, img_path, mode='train', batch=None):
            root, data = manifest(Path(self.data['path']) / 'dataset.yaml')
            return MaskDataset(root, data, mode, int(self.args.imgsz))

    return PixelMaskTrainer


def train(kind, source, dataset, output, epochs=10, batch=4, size=640, workers=2, device='0', progress=None):
    import yaml
    root, data = manifest(dataset)
    if data['annotationType'] != kind: raise ValueError('数据集格式与训练任务不匹配')
    output = Path(output)
    model = load_model(kind, source, os.environ.get('VLS_MODEL_CACHE', root.parent / 'vls-model-cache'))
    if model['task'] == 'semanticSeg':
        return train_semantic(model, root, data, output, epochs, batch, size, device, progress)
    network = model['network']
    if progress:
        network.add_callback('on_train_epoch_end', lambda trainer: progress('TRAINING', trainer.epoch + 1, epochs))
    arguments = dict(data=str(root / 'classification') if model['task'] == 'classify' else str(dataset),
                     epochs=epochs, imgsz=size, batch=batch, workers=workers, device=device,
                     project=str(output.parent), name=output.name, exist_ok=False, plots=False,
                     seed=42, save=True, amp=False)
    if model['task'] == 'segment': arguments.update(trainer=instance_trainer(), overlap_mask=False, mask_ratio=1, close_mosaic=0)
    metrics = network.train(**arguments)
    best = output / 'weights/best.pt'
    if not best.is_file(): raise ValueError('训练没有生成本轮 best.pt')
    if model['task'] == 'classify':
        from ultralytics import YOLO
        labeled = YOLO(str(best))
        labeled.model.names = {index: name for index, name in enumerate(data['names'])}
        labeled.save(str(best))
    args_path = output / 'args.yaml'
    args = yaml.safe_load(args_path.read_text(encoding='utf-8')) or {}
    args['vls_dataset_yaml'] = str(Path(dataset).resolve())
    args_path.write_text(yaml.safe_dump(args, allow_unicode=True), encoding='utf-8')
    values = {key: float(value) for key, value in getattr(metrics, 'results_dict', {}).items()}
    atomic_json(output / 'vls-metrics.json', dict(annotationType=kind, metrics=values))
    return best


def train_semantic(model, root, data, output, epochs, batch, size, device, progress):
    import numpy as np
    import torch
    import torch.nn.functional as functional
    from PIL import Image
    import yaml
    network, names = model['network'], data['names']
    if model['names'] != names:
        for head in (network.classifier, network.aux_classifier):
            if head is not None: head[-1] = torch.nn.Conv2d(head[-1].in_channels, len(names), 1)
    target_device = torch.device('cpu' if str(device) == 'cpu' else 'cuda:0')
    network.to(target_device)
    optimizer = torch.optim.SGD(network.parameters(), lr=0.001, momentum=0.9, weight_decay=0.0001)
    mean = torch.tensor([.485, .456, .406], device=target_device).view(1, 3, 1, 1)
    std = torch.tensor([.229, .224, .225], device=target_device).view(1, 3, 1, 1)
    training = [sample for sample in data['samples'] if sample['split'] == 'train']
    validation = [sample for sample in data['samples'] if sample['split'] == 'val']
    if not training or not validation: raise ValueError('语义分割训练和验证集合均不能为空')
    output.mkdir(parents=True, exist_ok=False)
    (output / 'weights').mkdir()
    (output / 'args.yaml').write_text(yaml.safe_dump(dict(data=str(root / 'dataset.yaml'), task='semanticSeg', epochs=epochs, imgsz=size), allow_unicode=True), encoding='utf-8')
    best_score = -1.0
    generator = np.random.default_rng(42)

    def load(samples):
        images, labels = [], []
        for sample in samples:
            with Image.open(child(root, sample['path'])) as image:
                images.append(np.asarray(image.convert('RGB').resize((size, size), Image.Resampling.BILINEAR)).copy())
            with Image.open(child(root, sample['target'])) as label:
                labels.append(np.asarray(label.resize((size, size), Image.Resampling.NEAREST), dtype=np.int64).copy())
        image = torch.from_numpy(np.stack(images).transpose(0, 3, 1, 2)).to(target_device).float() / 255
        return (image - mean) / std, torch.from_numpy(np.stack(labels)).to(target_device)

    for epoch in range(epochs):
        network.train()
        # Small user datasets may have a final batch of one; use pretrained normalization statistics.
        for layer in network.modules():
            if isinstance(layer, torch.nn.modules.batchnorm._BatchNorm): layer.eval()
        order = generator.permutation(len(training))
        loss_total = 0.0
        for start in range(0, len(order), batch):
            images, labels = load([training[index] for index in order[start:start+batch]])
            optimizer.zero_grad(set_to_none=True)
            outputs = network(images)
            loss = functional.cross_entropy(outputs['out'], labels)
            if 'aux' in outputs: loss = loss + .4 * functional.cross_entropy(outputs['aux'], labels)
            if not torch.isfinite(loss): raise ValueError('语义分割训练损失不是有限数值')
            loss.backward(); optimizer.step(); loss_total += float(loss.detach())
        network.eval()
        confusion = torch.zeros((len(names), len(names)), dtype=torch.int64, device=target_device)
        with torch.inference_mode():
            for start in range(0, len(validation), batch):
                images, labels = load(validation[start:start+batch])
                prediction = network(images)['out'].argmax(1)
                confusion += torch.bincount((labels * len(names) + prediction).flatten(), minlength=len(names) ** 2).reshape(len(names), len(names))
        intersection = confusion.diag().float()
        union = confusion.sum(0) + confusion.sum(1) - intersection
        score = float((intersection[union > 0] / union[union > 0]).mean())
        print('Epoch {}/{} loss={:.6f} mIoU={:.6f}'.format(epoch + 1, epochs, loss_total, score), flush=True)
        if score > best_score:
            best_score = score
            torch.save(dict(vls_task='semanticSeg', architecture='deeplabv3_resnet50', names=names,
                            aux_loss=network.aux_classifier is not None, state_dict=network.state_dict()), output / 'weights/best.pt')
        if progress: progress('TRAINING', epoch + 1, epochs)
        atomic_json(output / 'vls-metrics.json', dict(annotationType='semantic_segmentation', metrics={'mIoU': score, 'best_mIoU': best_score}))
    return output / 'weights/best.pt'


def predict(model, path, confidence=.25, size=640, device='0'):
    import numpy as np
    from PIL import Image
    if model['task'] == 'semanticSeg':
        import torch
        import torch.nn.functional as functional
        target = torch.device('cpu' if str(device) == 'cpu' else 'cuda:0')
        network = model['network'].to(target).eval()
        with Image.open(path) as opened:
            image = opened.convert('RGB'); width, height = image.size
            resized = np.asarray(image.resize((size, size), Image.Resampling.BILINEAR)).copy()
        value = torch.from_numpy(resized.transpose(2, 0, 1)).to(target).float().unsqueeze(0) / 255
        mean = torch.tensor([.485, .456, .406], device=target).view(1, 3, 1, 1)
        std = torch.tensor([.229, .224, .225], device=target).view(1, 3, 1, 1)
        with torch.inference_mode():
            probabilities = network((value - mean) / std)['out'].softmax(1)
            scores, classes = probabilities.max(1)
            classes = functional.interpolate(classes.float().unsqueeze(1), (height, width), mode='nearest')[0, 0].cpu().numpy().astype(np.int32)
            scores = functional.interpolate(scores.unsqueeze(1), (height, width), mode='bilinear', align_corners=False)[0, 0].cpu().numpy()
        regions = [crop_region(classes == index, model['names'][index], scores[classes == index].mean()) for index in np.unique(classes)]
        return width, height, regions
    result = model['network'].predict(source=str(path), conf=confidence, iou=.7, imgsz=size, device=device,
                                       max_det=1000, retina_masks=True, save=False, verbose=False)[0]
    height, width = result.orig_shape
    if model['task'] == 'classify':
        index = result.probs.top1
        return width, height, [dict(className=str(result.names[index]), confidence=float(result.probs.top1conf.cpu()))]
    regions = []
    for index, box in enumerate(result.boxes):
        score, category = float(box.conf[0].cpu()), int(box.cls[0].cpu())
        if model['task'] == 'segment':
            if result.masks is None: raise ValueError('实例分割模型没有返回像素掩膜')
            mask = result.masks.data[index].cpu().numpy() > .5
            if mask.shape != (height, width): raise ValueError('实例掩膜与原图尺寸不一致')
            region = crop_region(mask, result.names[category], score)
            if region: regions.append(region)
        else:
            x1, y1, x2, y2 = box.xyxy[0].cpu().tolist()
            x1, y1, x2, y2 = max(0., x1), max(0., y1), min(float(width), x2), min(float(height), y2)
            if x2 > x1 and y2 > y1: regions.append(dict(className=str(result.names[category]), confidence=score, x=x1, y=y1, width=x2-x1, height=y2-y1))
    return width, height, regions
