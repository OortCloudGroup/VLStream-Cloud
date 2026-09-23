"""One isolated classification/detection/instance/semantic annotation round."""
import hashlib
import json
import math
import os
from pathlib import Path
import shutil
import sys
from four_task_runtime import load_model, train, predict


def atomic_json(path, value):
    temporary = path.with_suffix('.tmp')
    temporary.write_text(json.dumps(value, ensure_ascii=False, allow_nan=False), encoding='utf-8')
    os.replace(temporary, path)


def uncertainty(boxes):
    # A prioritization heuristic, not an estimate of annotation accuracy.
    return 1.0 if not boxes else 1.0 - sum(b['confidence'] for b in boxes) / len(boxes)


def run(config_path):
    config = json.loads(Path(config_path).read_text(encoding='utf-8'))
    root = Path(config_path).resolve().parent
    kind = config.get('annotationType', 'object_detection')
    source = config['model']
    cache = Path(config.get('modelCache', root.parent / 'vls-model-cache'))
    copied = root / 'source.pt'
    if source.startswith('@preset/'):
        model = load_model(kind, source, cache)
        if model['task'] == 'semanticSeg':
            import torch
            torch.save(dict(vls_task='semanticSeg', architecture='deeplabv3_resnet50', names=model['names'],
                            aux_loss=model['network'].aux_classifier is not None, state_dict=model['network'].state_dict()), copied)
        else:
            model['network'].save(str(copied))
        del model
    else:
        if not Path(source).is_file(): raise ValueError('所选模型文件不存在')
        shutil.copyfile(source, copied)

    def progress(stage, current, total):
        atomic_json(root / 'progress.json', dict(stage=stage, current=current, total=total))

    model_path = copied
    if config['mode'] == 'active':
        print('VLS_STAGE=TRAINING', flush=True)
        progress('TRAINING', 0, config['epochs'])
        model_path = train(kind, str(copied), root / 'dataset.yaml', root / 'training',
                           epochs=config['epochs'], batch=4, size=640, workers=config['workers'], device=config.get('device', '0'), progress=progress)
    model = load_model(kind, str(model_path), cache)
    print('VLS_STAGE=PREDICTING', flush=True)
    predictions = []
    for index, item in enumerate(config['images']):
        width, height, boxes = predict(model, root / item['path'], config['confidence'], device=config.get('device', '0'))
        predictions.append(dict(imageId=str(item['id']), width=int(width), height=int(height), boxes=boxes, uncertainty=uncertainty(boxes)))
        progress('PREDICTING', index + 1, len(config['images']))
        print('VLS_PROGRESS={}/{}'.format(index + 1, len(config['images'])), flush=True)
    digest = hashlib.sha256()
    with model_path.open('rb') as weights:
        for chunk in iter(lambda: weights.read(1024 * 1024), b''):
            digest.update(chunk)
    atomic_json(root / 'results.json', dict(modelPath=str(model_path), modelSha256=digest.hexdigest(), annotationType=kind, predictions=predictions))
    print('VLS_STAGE=COMPLETED', flush=True)


if __name__ == '__main__':
    run(sys.argv[1])
