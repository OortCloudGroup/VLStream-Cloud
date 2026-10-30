"""Linux filesystem/process tests for the shared queue; no GPU training is simulated."""
import fcntl
import hashlib
import importlib.util
import json
import multiprocessing
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
import uuid

SOURCE = Path(__file__).resolve().parents[2] / 'main/resources/training/gpu_shared_queue.py'
if not SOURCE.exists():
    SOURCE = Path(__file__).with_name('gpu_shared_queue.py')
spec = importlib.util.spec_from_file_location('gpu_shared_queue', SOURCE)
queue = importlib.util.module_from_spec(spec)
spec.loader.exec_module(queue)
runner_path=SOURCE.with_name('cloud_training_runner.py')
runner_spec=importlib.util.spec_from_file_location('cloud_training_runner',runner_path)
runner=importlib.util.module_from_spec(runner_spec)
runner_spec.loader.exec_module(runner)


def register_in_process(gpu, root, ticket):
    queue.Registry(gpu, root).register([ticket])


class SharedQueueTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory(prefix='vls-shared-queue-test-')
        self.root = Path(self.directory.name)
        self.previous_bases = queue.RUN_BASES
        queue.RUN_BASES = (str(self.root / 'jobs'),)
        self.gpu = dict(gpuIndex=0, gpuUuid='GPU-24fb011a-dd51-d9e1-ed6d-59f9266869ac')
        self.registry = queue.Registry(self.gpu, self.root / 'registry')

    def tearDown(self):
        queue.RUN_BASES = self.previous_bases
        self.directory.cleanup()

    def ticket(self, environment='localhost', owner='a'):
        identity = str(uuid.uuid4())
        run = self.root / 'jobs' / environment / (owner * 16) / identity
        return dict(environment=environment, jobId=identity, owner=owner * 64, runDir=str(run))

    def result(self, ticket, state='SUCCEEDED'):
        root = Path(ticket['runDir']); root.mkdir(parents=True)
        value = dict(state=state, sha256='b' * 64, size=12)
        (root / 'result.json').write_text(json.dumps(value))
        return dict(state='ARCHIVED', ptSha256=value['sha256'], size=12, classesSha256='c' * 64)

    def test_local_and_vm_share_fifo_and_archive_lease(self):
        local, vm = self.ticket(), self.ticket('vm', 'd')
        self.registry.register([local]); self.registry.register([vm])
        self.assertFalse(self.registry.claim(vm))
        self.assertTrue(self.registry.claim(local))
        self.registry.archiving(local)
        self.assertFalse(self.registry.claim(vm))
        self.assertTrue(self.registry.inspect()['active'])
        self.registry.release(local, self.result(local))
        self.assertTrue(self.registry.claim(vm))

    def detection_result(self, ticket, state='COMPLETED', acknowledged=True):
        root=Path(ticket['runDir']);root.mkdir(parents=True,exist_ok=True)
        manifest=dict(events=[dict(id='event-1')])
        (root/'manifest.json').write_text(json.dumps(manifest))
        digest=hashlib.sha256((root/'manifest.json').read_bytes()).hexdigest()
        (root/'result.json').write_text(json.dumps(dict(kind='CLOUD_AI',state=state,manifestSha256=digest,eventCount=1)))
        (root/'acknowledged.json').write_text(json.dumps(dict(events=['event-1'] if acknowledged else [])))
        return dict(state='DETECTION_ARCHIVED',manifestSha256=digest,eventCount=1)

    def test_detection_blocks_training_until_every_event_is_archived(self):
        detection,training=self.ticket(),self.ticket('vm','d')
        self.registry.register([detection,training]);self.assertTrue(self.registry.claim(detection))
        self.registry.archiving(detection)
        proof=self.detection_result(detection,acknowledged=False)
        with self.assertRaises(ValueError):self.registry.release(detection,proof)
        self.assertFalse(self.registry.claim(training))
        self.detection_result(detection)
        self.registry.release(detection,proof)
        self.assertTrue(self.registry.claim(training))

    def test_detection_release_requires_matching_manifest_and_proven_exit(self):
        ticket=self.ticket();self.registry.register([ticket]);self.registry.claim(ticket)
        proof=self.detection_result(ticket);root=Path(ticket['runDir'])
        with self.assertRaises(ValueError):self.registry.release(ticket,dict(proof,eventCount=2))
        with self.assertRaises(ValueError):self.registry.release(ticket,dict(proof,manifestSha256='a'*64))
        start=Path('/proc/self/stat').read_text().rsplit(')',1)[1].split()[19]
        (root/'state.json').write_text(json.dumps(dict(supervisorPid=os.getpid(),supervisorStart=start)))
        with self.assertRaises(ValueError):self.registry.release(ticket,proof)
        (root/'state.json').write_text('{}')
        self.registry.release(ticket,proof)
        self.registry.release(ticket,proof)
        self.assertFalse(self.registry.inspect()['active'])

    def test_expired_detection_does_not_release_active_training(self):
        training,waiting=self.ticket(),self.ticket('vm')
        self.registry.register([training,waiting]);self.registry.claim(training)
        self.registry.release(waiting,self.detection_result(waiting,state='SKIPPED'))
        self.assertTrue(self.registry.inspect(training)['ownsLease'])
        self.assertEqual(self.registry.inspect(waiting)['state'],'DONE')

    def test_recovery_before_queue_registration_requires_detection_terminal_proof(self):
        ticket=self.ticket();proof=self.detection_result(ticket,state='CANCELLED')
        self.assertEqual(self.registry.release(ticket,proof)['state'],'DONE')
        (Path(ticket['runDir'])/'acknowledged.json').write_text('{}')
        with self.assertRaises(ValueError):self.registry.release(ticket,proof)

    def test_atomic_concurrent_registration_survives_process_restart(self):
        tickets = [self.ticket('localhost' if index % 2 else 'vm') for index in range(12)]
        processes = [multiprocessing.Process(target=register_in_process, args=(self.gpu, self.root / 'registry', ticket)) for ticket in tickets]
        for process in processes: process.start()
        for process in processes:
            process.join(15); self.assertEqual(process.exitcode, 0)
        restarted = queue.Registry(self.gpu, self.root / 'registry')
        with restarted.locked() as state:
            self.assertEqual(len(state['tickets']), 12)
            self.assertEqual({row['sequence'] for row in state['tickets'].values()}, set(range(1, 13)))

    def test_archive_requires_matching_pt_and_classes_acknowledgement(self):
        ticket = self.ticket(); self.registry.register([ticket]); self.registry.claim(ticket)
        acknowledgement = self.result(ticket)
        for change in ({'ptSha256': 'd' * 64}, {'size': 13}, {'classesSha256': ''}):
            with self.assertRaises(ValueError): self.registry.release(ticket, dict(acknowledgement, **change))
            self.assertTrue(self.registry.inspect()['active'])
        self.registry.release(ticket, acknowledgement)
        self.registry.release(ticket, acknowledgement)
        with self.assertRaises(ValueError): self.registry.release(ticket, dict(acknowledgement, classesSha256='d' * 64))

    def test_live_process_and_inherited_run_lock_prevent_release(self):
        ticket = self.ticket(); self.registry.register([ticket]); self.registry.claim(ticket)
        acknowledgement = self.result(ticket)
        root = Path(ticket['runDir'])
        start = Path('/proc/self/stat').read_text().rsplit(')', 1)[1].split()[19]
        (root / 'state.json').write_text(json.dumps(dict(childPid=os.getpid(), childStart=start)))
        with self.assertRaises(ValueError): self.registry.release(ticket, acknowledgement)
        (root / 'state.json').write_text('{}')
        descriptor = os.open(str(root / 'run.lock'), os.O_RDWR | os.O_CREAT, 0o600)
        fcntl.flock(descriptor, fcntl.LOCK_EX)
        child = subprocess.Popen([sys.executable, '-c', 'import time; time.sleep(30)'], pass_fds=(descriptor,))
        os.close(descriptor)
        try:
            with self.assertRaises(BlockingIOError): self.registry.release(ticket, acknowledgement)
            self.assertTrue(self.registry.inspect()['active'])
        finally:
            child.terminate(); child.wait(15)
        self.registry.release(ticket, acknowledgement)

    def test_waiting_cancellation_does_not_release_another_task(self):
        active, waiting = self.ticket(), self.ticket('vm')
        self.registry.register([active, waiting]); self.registry.claim(active)
        self.registry.release(waiting, dict(state='CANCELLED'))
        self.assertTrue(self.registry.inspect(active)['ownsLease'])
        self.assertEqual(self.registry.inspect(waiting)['state'], 'DONE')

    def test_failed_active_task_needs_matching_terminal_result(self):
        ticket = self.ticket(); self.registry.register([ticket]); self.registry.claim(ticket)
        with self.assertRaises(ValueError): self.registry.release(ticket, dict(state='FAILED'))
        self.result(ticket, 'FAILED')
        self.registry.release(ticket, dict(state='FAILED'))
        self.assertFalse(self.registry.inspect()['active'])

    def test_identity_cannot_change_environment_owner_or_directory(self):
        ticket = self.ticket(); self.registry.register([ticket])
        with self.assertRaises(ValueError): self.registry.register([dict(ticket, owner='b' * 64)])
        for changed in (dict(ticket, environment='prod'), dict(ticket, runDir='/tmp/' + ticket['jobId']), dict(ticket, owner='')):
            with self.assertRaises(ValueError): queue.validate_ticket(changed)

    def test_registry_corruption_and_physical_replacement_fail_closed(self):
        ticket = self.ticket(); self.registry.register([ticket]); self.registry.claim(ticket)
        state = json.loads(self.registry.path.read_text()); state['active'] = None
        self.registry.path.write_text(json.dumps(state))
        with self.assertRaises(ValueError): self.registry.inspect()
        replacement = dict(self.gpu, gpuIndex=1)
        with self.assertRaises(ValueError): queue.Registry(replacement, self.root / 'registry').inspect()

    def test_symlink_lock_and_symlink_run_directory_are_rejected(self):
        target = self.root / 'unrelated'; target.write_text('preserved')
        (self.registry.base / 'registry.lock').symlink_to(target)
        with self.assertRaises(OSError): self.registry.register([self.ticket()])
        self.assertEqual(target.read_text(), 'preserved')
        ticket = self.ticket(); root = Path(ticket['runDir']); root.parent.mkdir(parents=True)
        root.symlink_to(self.root)
        with self.assertRaises(ValueError):
            with queue.confirmed_exit(root): pass

    def test_gpu_queues_are_independent_and_done_task_cannot_restart(self):
        first = self.ticket(); self.registry.register([first]); self.registry.claim(first)
        second_gpu = dict(gpuIndex=1, gpuUuid='GPU-11111111-1111-1111-1111-111111111111')
        second = queue.Registry(second_gpu, self.root / 'registry'); other = self.ticket('vm')
        second.register([other]); self.assertTrue(second.claim(other))
        self.registry.release(first, self.result(first))
        with self.assertRaises(ValueError): self.registry.claim(first)

    def test_runner_rejects_symlink_and_shared_inode_locks(self):
        target=self.root/'preserved';target.write_text('existing')
        symbolic=self.root/'symbolic';symbolic.symlink_to(target)
        with self.assertRaises(OSError):
            with runner.lock_file(symbolic):pass
        linked=self.root/'linked';os.link(target,linked)
        with self.assertRaises(ValueError):
            with runner.lock_file(linked):pass
        self.assertEqual(target.read_text(),'existing')

    def test_runner_parent_close_does_not_unlock_inherited_child_lease(self):
        path=self.root/'runner.lock'
        with runner.lock_file(path) as lock:
            fcntl.flock(lock,fcntl.LOCK_EX)
            child=subprocess.Popen([sys.executable,'-c','import time; time.sleep(30)'],pass_fds=(lock.fileno(),))
        try:
            with runner.lock_file(path) as competing:
                with self.assertRaises(BlockingIOError):fcntl.flock(competing,fcntl.LOCK_EX|fcntl.LOCK_NB)
        finally:
            child.terminate();child.wait(15)
        with runner.lock_file(path) as competing:fcntl.flock(competing,fcntl.LOCK_EX|fcntl.LOCK_NB)

    def test_interrupted_process_is_persisted_failed_only_after_its_lock_exits(self):
        ticket=self.ticket();self.registry.register([ticket]);self.registry.claim(ticket)
        root=Path(ticket['runDir']);root.mkdir(parents=True)
        (root/'state.json').write_text(json.dumps(dict(state='RUNNING',childPid=99999999,childStart='1')))
        with runner.lock_file(root/'run.lock') as lock:
            fcntl.flock(lock,fcntl.LOCK_EX)
            self.assertEqual(runner.status(root)['state'],'RUNNING')
            self.assertFalse((root/'result.json').exists())
        self.assertEqual(runner.status(root)['state'],'FAILED')
        self.assertEqual(json.loads((root/'result.json').read_text())['state'],'FAILED')
        self.registry.release(ticket,dict(state='FAILED'))
        self.assertFalse(self.registry.inspect()['active'])


if __name__ == '__main__':
    unittest.main(verbosity=2)
