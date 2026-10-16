"""Linux kernel-lock regression. No GPU, database, credential, or training simulation."""
import importlib.util
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

MODULE_PATH = Path(os.environ.get('VLS_PROBE_MODULE', Path(__file__).resolve().parents[2] / 'main/resources/training/check_cloud_environment.py'))
spec = importlib.util.spec_from_file_location('gpu_probe_under_test', MODULE_PATH)
probe = importlib.util.module_from_spec(spec)
spec.loader.exec_module(probe)


@unittest.skipUnless(sys.platform.startswith('linux'), 'Requires real Linux flock')
class GpuProbeLeaseTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix='vls-gpu-lease-test-')
        self.directory = Path(self.temporary.name)
        self.path_patch = patch.object(probe, '_gpu_lock_path', lambda index: self.directory / ('gpu-' + str(index)))
        self.path_patch.start()
        self.environment_patch = patch.dict(os.environ)
        self.environment_patch.start()
        os.environ.pop('VLS_GPU_LOCK_FD', None)

    def tearDown(self):
        self.environment_patch.stop()
        self.path_patch.stop()
        self.temporary.cleanup()

    def test_same_gpu_blocks_but_different_cards_remain_independent(self):
        with probe.gpu_probe_lease(0):
            with self.assertRaises(BlockingIOError):
                with probe.gpu_probe_lease(0):
                    self.fail('Duplicate GPU lease granted')
            with probe.gpu_probe_lease(1):
                pass
        with probe.gpu_probe_lease(0):
            pass

    def test_independent_platform_processes_share_the_same_inode(self):
        code = """
import importlib.util,sys
from pathlib import Path
s=importlib.util.spec_from_file_location('p',sys.argv[1]);p=importlib.util.module_from_spec(s);s.loader.exec_module(p)
p._gpu_lock_path=lambda index:Path(sys.argv[2])
with p.gpu_probe_lease(0):
 print('LOCKED',flush=True)
 sys.stdin.read(1)
"""
        child = subprocess.Popen([sys.executable, '-u', '-c', code, str(MODULE_PATH), str(self.directory / 'gpu-0')],
            stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True,
            env={'PATH': os.defpath})
        try:
            self.assertEqual('LOCKED', child.stdout.readline().strip())
            with self.assertRaises(BlockingIOError):
                with probe.gpu_probe_lease(0):
                    self.fail('Cross-process GPU lease granted')
            child.communicate('x', timeout=5)
            self.assertEqual(0, child.returncode)
            with probe.gpu_probe_lease(0):
                pass
        finally:
            if child.poll() is None:
                child.kill(); child.communicate()

    def test_bootstrap_descriptor_can_be_borrowed_without_unlocking_owner(self):
        import fcntl
        descriptor = os.open(str(self.directory / 'gpu-0'), os.O_RDWR | os.O_CREAT, 0o600)
        fcntl.flock(descriptor, fcntl.LOCK_EX | fcntl.LOCK_NB)
        code = """
import importlib.util,sys
from pathlib import Path
s=importlib.util.spec_from_file_location('p',sys.argv[1]);p=importlib.util.module_from_spec(s);s.loader.exec_module(p)
p._gpu_lock_path=lambda index:Path(sys.argv[2])
with p.gpu_probe_lease(0):print('BORROWED')
"""
        try:
            result = subprocess.run([sys.executable, '-c', code, str(MODULE_PATH), str(self.directory / 'gpu-0')],
                pass_fds=(descriptor,), env={'PATH': os.defpath, 'VLS_GPU_LOCK_FD': str(descriptor)},
                capture_output=True, text=True, timeout=5, check=True)
            self.assertEqual('BORROWED', result.stdout.strip())
            with self.assertRaises(BlockingIOError):
                with probe.gpu_probe_lease(0):
                    self.fail('Borrower released the initializer lease')
        finally:
            os.close(descriptor)

    def test_inherited_wrong_inode_or_symlink_never_bypasses_lock(self):
        (self.directory / 'gpu-0').touch(mode=0o600)
        descriptor = os.open(str(self.directory / 'wrong-file'), os.O_RDWR | os.O_CREAT, 0o600)
        try:
            os.environ['VLS_GPU_LOCK_FD'] = str(descriptor)
            with self.assertRaises(ValueError):
                with probe.gpu_probe_lease(0):
                    self.fail('Wrong inode accepted')
        finally:
            os.close(descriptor)
            os.environ.pop('VLS_GPU_LOCK_FD', None)
        (self.directory / 'gpu-1').symlink_to(self.directory / 'gpu-0')
        with self.assertRaises(OSError):
            with probe.gpu_probe_lease(1):
                self.fail('Symlink accepted as a lock')


if __name__ == '__main__':
    unittest.main(verbosity=2)
