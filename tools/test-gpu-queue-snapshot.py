"""Offline protocol tests. No SSH, NVIDIA query, task execution or registry mutation."""
import copy
import hashlib
import importlib.util
from pathlib import Path
import unittest
from unittest.mock import patch
import uuid

SOURCE = Path(__file__).resolve().parents[1] / 'VLStream-Cloud-Backend-Server/vls-stream/ruoyi-vlstream/src/main/resources/training/gpu_shared_queue_snapshot.py'
spec = importlib.util.spec_from_file_location('queue_snapshot', SOURCE)
snapshot = importlib.util.module_from_spec(spec)
spec.loader.exec_module(snapshot)
GPU = dict(gpuIndex=0, gpuUuid='GPU-24fb011a-dd51-d9e1-ed6d-59f9266869ac')
OWNER = 'a' * 64


class QueueSnapshotTest(unittest.TestCase):
    def registry(self, active_phase='RUNNING', foreign=False):
        rows, active = {}, None
        for index, phase in enumerate((active_phase, 'WAITING', 'WAITING', 'DONE'), 1):
            job = str(uuid.uuid4())
            env = 'vm' if foreign and index == 1 else 'localhost'
            key = hashlib.sha256((env + '\n' + job).encode()).hexdigest()
            rows[key] = dict(environment=env, jobId=job, owner=OWNER, sequence=index, state=phase)
            if index == 1:
                active = key
        return dict(version=1, gpu=GPU, sequence=4, active=active, tickets=rows)

    def view(self, state):
        return snapshot.summarize(state, GPU, 'localhost', OWNER)

    def test_waiting_excludes_active_and_done(self):
        state = self.registry()
        before = copy.deepcopy(state)
        view = self.view(state)
        self.assertEqual(view['waitingCount'], 2)
        self.assertEqual(view['activePhase'], 'RUNNING')
        self.assertEqual(state, before)

    def test_archiving_keeps_lease_but_is_not_waiting(self):
        view = self.view(self.registry('ARCHIVING'))
        self.assertEqual(view['activePhase'], 'ARCHIVING')
        self.assertEqual(view['waitingCount'], 2)

    def test_own_current_environment_id_only(self):
        own = self.registry()
        self.assertEqual(self.view(own)['activeJobId'], own['tickets'][own['active']]['jobId'])
        self.assertIsNone(self.view(self.registry(foreign=True))['activeJobId'])
        own['tickets'][own['active']]['owner'] = 'b' * 64
        self.assertIsNone(self.view(own)['activeJobId'])

    def test_empty_registry_is_known_but_has_no_eta(self):
        view = self.view(dict(version=1, gpu=GPU, sequence=0, active=None, tickets={}))
        self.assertEqual(view['waitingCount'], 0)
        self.assertIsNone(view['activePhase'])
        self.assertNotIn('estimatedWaitSeconds', view)

    def test_other_environment_counts_but_does_not_expose_identity(self):
        view = self.view(self.registry(foreign=True))
        self.assertEqual(view['waitingCount'], 2)
        self.assertNotIn('runDir', view)
        self.assertNotIn('owner', view)

    def test_inconsistent_lease_rejected(self):
        state = self.registry()
        state['active'] = 'missing'
        with self.assertRaises(ValueError): self.view(state)

    def test_duplicate_sequence_bad_state_or_wrong_gpu_rejected(self):
        for mutate in (
            lambda state: state['tickets'][state['active']].update(sequence=2),
            lambda state: state['tickets'][state['active']].update(state='UNKNOWN'),
            lambda state: state.update(gpu=dict(GPU, gpuIndex=1)),
            lambda state: state.update(version=2),
            lambda state: state.update(sequence=True),
        ):
            state = self.registry(); mutate(state)
            with self.assertRaises(ValueError): self.view(state)

    def test_forged_ticket_key_rejected(self):
        state = self.registry()
        state['tickets'][state['active']]['jobId'] = str(uuid.uuid4())
        with self.assertRaises(ValueError): self.view(state)

    def test_missing_registry_does_not_create_files(self):
        # Input validation precedes any attempted filesystem access.
        with self.assertRaises(ValueError): snapshot.read_snapshot(dict(GPU, gpuIndex=True), 'localhost', OWNER)
        with self.assertRaises(ValueError): snapshot.read_snapshot(GPU, 'unknown', OWNER)

    def config(self, row):
        runtime = {key: 'verified' for key in ('python', 'torch', 'torchvision', 'cudaVersion', 'ultralytics')}
        return dict(model='@preset/detect', datasetArtifactRef='vls-dataset://12345678-1234-1234-1234-123456789abc', annotationType='object_detection', epochs=100, batchSize=16, imgSize=640, gpuIndex=0,
                    sharedQueue=dict(mode='SHARED_GPU_QUEUE', jobId=row['jobId'], owner=row['owner'], environment=row['environment'], runtime=runtime, gpuUuid=GPU['gpuUuid'], gpuIndex=0))

    def test_exact_workload_profile_changes_with_dataset_model_parameters_or_runtime(self):
        registry = self.registry(); row = registry['tickets'][registry['active']]
        row['runDir'] = '/home/oort/vlstream/localhost/' + row['jobId']
        config = self.config(row)
        with patch.object(snapshot, 'read_json_file', return_value=config):
            first = snapshot.workload_profile(row)
            self.assertRegex(first, '^[a-f0-9]{64}$')
            self.assertEqual(first, snapshot.workload_profile(row))
            for key, value in (('epochs', 101), ('batchSize', 32), ('imgSize', 1280), ('datasetArtifactRef', 'vls-dataset://22345678-1234-1234-1234-123456789abc')):
                changed = dict(config, **{key: value})
                with patch.object(snapshot, 'read_json_file', return_value=changed): self.assertNotEqual(first, snapshot.workload_profile(row))
            changed = copy.deepcopy(config); changed['sharedQueue']['runtime']['torch'] = 'changed'
            with patch.object(snapshot, 'read_json_file', return_value=changed): self.assertNotEqual(first, snapshot.workload_profile(row))

    def test_unprepared_unsafe_paths_custom_weights_and_owner_mismatch_have_no_profile(self):
        registry = self.registry(); row = registry['tickets'][registry['active']]
        row['runDir'] = '/home/oort/vlstream/localhost/' + row['jobId']
        with patch.object(snapshot, 'read_json_file', side_effect=FileNotFoundError): self.assertIsNone(snapshot.workload_profile(row))
        for change in (dict(model='/private/base.pt'), dict(datasetArtifactRef=None), dict(epochs=True)):
            config = dict(self.config(row), **change)
            with patch.object(snapshot, 'read_json_file', return_value=config): self.assertIsNone(snapshot.workload_profile(row))
        config = self.config(row); config['sharedQueue']['owner'] = 'b' * 64
        with patch.object(snapshot, 'read_json_file', return_value=config): self.assertIsNone(snapshot.workload_profile(row))
        row['runDir'] = '/home/oort/vlstream/localhost/../secret/' + row['jobId']
        with patch.object(snapshot, 'read_json_file') as reader:
            self.assertIsNone(snapshot.workload_profile(row)); reader.assert_not_called()

    def test_profile_requires_the_same_verified_physical_gpu(self):
        state = self.registry(); row = state['tickets'][state['active']]
        row['runDir'] = '/home/oort/vlstream/localhost/' + row['jobId']; config = self.config(row)
        with patch.object(snapshot, 'read_json_file', return_value=config):
            self.assertIsNotNone(snapshot.workload_profile(row, GPU))
            self.assertIsNone(snapshot.workload_profile(row, dict(GPU, gpuIndex=1)))

    def test_only_verified_archived_acknowledgement_can_close_a_timing_sample(self):
        state = self.registry(); key = state['active']; row = state['tickets'][key]
        state['active'] = None; row['state'] = 'DONE'
        row['acknowledgement'] = dict(state='ARCHIVED', ptSha256='c' * 64, classesSha256='d' * 64, size=100)
        view = snapshot.summarize(state, GPU, 'localhost', OWNER, key)
        self.assertEqual(view['previousOutcome'], 'ARCHIVED')
        row['acknowledgement']['state'] = 'FAILED'
        self.assertIsNone(snapshot.summarize(state, GPU, 'localhost', OWNER, key)['previousOutcome'])


if __name__ == '__main__': unittest.main(verbosity=2)
