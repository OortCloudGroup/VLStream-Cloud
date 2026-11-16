"""Node supervisor/annotation adapter contract; no claim of real GPU inference."""
import importlib.util
import json
import os
from pathlib import Path
import sys
import tempfile
import types
import unittest
from unittest.mock import patch

ROOT = next(p for p in Path(__file__).resolve().parents if (p / 'BUSINESS_PROCESSES.md').exists())
SOURCE = ROOT / 'VLStream-Cloud-Backend-Server/vls-stream/ruoyi-vlstream/src/main/resources/training/cloud_training_runner.py'
if os.name == 'nt':
    sys.modules.setdefault('fcntl', types.SimpleNamespace())
spec = importlib.util.spec_from_file_location('cloud_runner_contract', SOURCE)
runner = importlib.util.module_from_spec(spec)
spec.loader.exec_module(runner)


class SmartComputeRunnerTest(unittest.TestCase):
    def setUp(self):
        directory = ROOT / 'codex/smart-compute-20261116/python-fixtures'
        directory.mkdir(parents=True, exist_ok=True)
        self.directory = tempfile.TemporaryDirectory(dir=directory)
        self.root = Path(self.directory.name)
        self.config = dict(workload='smart_annotation', annotationType='object_detection', datasetId='1',
            workDir=str(self.root), gpuIndex=0, mode='model', model=str(self.root / 'base.pt'))
        self.model = self.root / 'base.pt'
        self.model.write_bytes(b'original-model')
        self.invocations = []

    def tearDown(self):
        self.directory.cleanup()

    def execute(self, corrupt=False, version='8.3.240'):
        def annotate(path):
            config = json.loads(Path(path).read_text())
            self.invocations.append(config)
            target = self.root / ('training/weights/best.pt' if config['mode'] == 'active' else 'source.pt')
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(b'annotation-model')
            runner.write_json(self.root / 'results.json', dict(annotationType=config['annotationType'],
                modelPath=str(target), modelSha256='corrupt' if corrupt else runner.sha256(target), predictions=[]))
        modules = dict(torch=types.SimpleNamespace(cuda=types.SimpleNamespace(is_available=lambda: True)),
            ultralytics=types.SimpleNamespace(__version__=version),
            four_task_runtime=types.SimpleNamespace(train=lambda *args: self.fail('formal training path used')),
            smart_annotation_runner=types.SimpleNamespace(run=annotate))
        with patch.dict(sys.modules, modules), patch.dict(os.environ):
            runner.train(self.root / 'job.json', self.config)
        return runner.read_json(self.root / 'result.json')

    def test_all_four_tasks_and_both_modes_return_verifiable_model_and_prediction_artifacts(self):
        for kind in ('image_classification', 'object_detection', 'instance_segmentation', 'semantic_segmentation'):
            for mode in ('active', 'model'):
                with self.subTest(kind=kind, mode=mode):
                    self.config.update(annotationType=kind, mode=mode)
                    result = self.execute()
                    self.assertEqual('SUCCEEDED', result['state'])
                    self.assertEqual(runner.sha256(self.root / 'results.json'), result['predictionsSha256'])
                    self.assertEqual(runner.sha256(self.root / 'output/weights/best.pt'), result['sha256'])
                    self.assertEqual('dataset/dataset.yaml', self.invocations[-1]['dataset'])
                    self.assertEqual(b'original-model', self.model.read_bytes())

    def test_inconsistent_prediction_model_digest_fails_without_success_receipt(self):
        self.assertEqual('FAILED', self.execute(corrupt=True)['state'])

    def test_incompatible_runtime_fails_before_prediction(self):
        self.assertEqual('FAILED', self.execute(version='0.0.0')['state'])
        self.assertEqual([], self.invocations)

    def test_status_preserves_annotation_stage_and_counts(self):
        runner.write_json(self.root / 'result.json', dict(state='SUCCEEDED'))
        runner.write_json(self.root / 'progress.json', dict(stage='PREDICTING', current=7, total=10))
        result = runner.status(self.root)
        self.assertEqual(('PREDICTING', 7, 10), (result['stage'], result['current'], result['total']))


if __name__ == '__main__':
    unittest.main()
