"""Runner contract checks with a deterministic Ultralytics test double; no GPU claims."""
import importlib.util
import json
from pathlib import Path
import sys
import tempfile
import types
import unittest
from unittest.mock import patch

ROOT = next(parent for parent in Path(__file__).resolve().parents if (parent / 'BUSINESS_PROCESSES.md').exists())
RUNNER = ROOT / 'VLStream-Cloud-Backend-Server/vls-stream/ruoyi-vlstream/src/main/resources/smart-annotation/runner.py'
sys.path.insert(0, str(RUNNER.parent.parent / 'training'))
spec = importlib.util.spec_from_file_location('smart_runner', RUNNER)
runner = importlib.util.module_from_spec(spec)
spec.loader.exec_module(runner)


class Scalar(float):
    def cpu(self): return self


class Coordinates(list):
    def cpu(self): return self
    def tolist(self): return list(self)


class Detector:
    task = 'detect'
    names = {0: 'car'}
    trained = []

    def __init__(self, path): self.path = Path(path)

    def train(self, **kwargs):
        self.trained.append(kwargs)
        weights = Path(kwargs['project']) / 'training/weights/best.pt'
        weights.parent.mkdir(parents=True)
        weights.write_bytes(b'trained-checkpoint')

    def predict(self, **kwargs):
        detection = types.SimpleNamespace(xyxy=[Coordinates([10, 20, 50, 70])], conf=[Scalar(.8)], cls=[Scalar(0)])
        return [types.SimpleNamespace(boxes=[detection], orig_shape=(80, 100))]


class RunnerTest(unittest.TestCase):
    def setUp(self):
        directory = ROOT / 'codex/smart-annotation/python-fixtures'
        directory.mkdir(parents=True, exist_ok=True)
        self.directory = tempfile.TemporaryDirectory(dir=directory)
        self.root = Path(self.directory.name)
        self.source = self.root / 'original.pt'
        self.source.write_bytes(b'original-checkpoint')
        self.config = dict(model=str(self.source), mode='model', epochs=2, workers=0, confidence=.25,
                           images=[dict(id='2102255841364148226', path='images/predict/example.png')])
        Detector.trained = []
        Detector.task = 'detect'

    def tearDown(self): self.directory.cleanup()

    def execute(self):
        path = self.root / 'config.json'
        path.write_text(json.dumps(self.config), encoding='utf-8')
        def load(kind, source, cache):
            if Detector.task != 'detect': raise ValueError('目标检测模型类型不匹配')
            return {'network': Detector(source), 'task': 'detect', 'names': Detector.names}
        def train(kind, source, dataset, output, **kwargs):
            Detector.trained.append(kwargs)
            weights = Path(output) / 'weights/best.pt'; weights.parent.mkdir(parents=True); weights.write_bytes(b'trained-checkpoint'); return weights
        def predict(model, path, confidence, **kwargs):
            return 100, 80, [dict(className='car', x=10, y=20, width=40, height=50, confidence=.8)]
        with patch.object(runner, 'load_model', load), patch.object(runner, 'train', train), patch.object(runner, 'predict', predict): runner.run(str(path))
        return json.loads((self.root / 'results.json').read_text(encoding='utf-8'))

    def test_specified_model_preserves_source_and_exact_image_id(self):
        result = self.execute()
        self.assertEqual(b'original-checkpoint', self.source.read_bytes())
        self.assertEqual('2102255841364148226', result['predictions'][0]['imageId'])
        self.assertEqual(100, result['predictions'][0]['width'])
        self.assertEqual('car', result['predictions'][0]['boxes'][0]['className'])
        self.assertEqual(64, len(result['modelSha256']))
        self.assertEqual([], Detector.trained)

    def test_active_learning_uses_new_best_checkpoint(self):
        self.config['mode'] = 'active'
        result = self.execute()
        self.assertEqual(2, Detector.trained[0]['epochs'])
        self.assertEqual(str(self.root / 'training/weights/best.pt'), result['modelPath'])
        self.assertEqual(b'original-checkpoint', self.source.read_bytes())

    def test_rejects_non_detection_models_before_inference(self):
        Detector.task = 'classify'
        with self.assertRaisesRegex(ValueError, '目标检测'): self.execute()
        self.assertFalse((self.root / 'results.json').exists())

    def test_does_not_download_missing_weights(self):
        self.source.unlink()
        with self.assertRaisesRegex(ValueError, '不存在'): self.execute()

    def test_empty_predictions_are_prioritized_for_review(self):
        self.assertEqual(1, runner.uncertainty([]))
        self.assertAlmostEqual(.2, runner.uncertainty([dict(confidence=.8)]))


if __name__ == '__main__': unittest.main()
