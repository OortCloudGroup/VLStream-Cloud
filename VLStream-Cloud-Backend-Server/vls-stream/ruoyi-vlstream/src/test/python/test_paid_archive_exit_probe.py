"""Read-only paid archive proof tests; portable POSIX fixture plus Linux kernel-lock checks."""
import contextlib
import errno
import hashlib
import importlib.util
import io
import json
import os
from pathlib import Path
import stat
import subprocess
import sys
import tempfile
import types
import unittest
from unittest.mock import patch

MODULE_PATH = Path(__file__).resolve().parents[2] / 'main/resources/training/paid_archive_exit_probe.py'
spec = importlib.util.spec_from_file_location('paid_archive_probe_under_test', MODULE_PATH)
probe = importlib.util.module_from_spec(spec)
spec.loader.exec_module(probe)


def proc_stat(pid, start='100', state='S'):
    fields = [state] + ['0'] * 18 + [start]
    return '{} (worker (with spaces)) {}'.format(pid, ' '.join(fields))


class ProcReadClassificationTest(unittest.TestCase):
    def test_only_missing_process_file_is_absent(self):
        with patch('builtins.open', side_effect=FileNotFoundError(errno.ENOENT, 'missing')):
            self.assertIsNone(probe._read_proc(101))
        for failure in (PermissionError(errno.EACCES, 'private'), OSError(errno.EIO, 'unreadable')):
            with self.subTest(error=type(failure).__name__), patch('builtins.open', side_effect=failure):
                with self.assertRaises(OSError):
                    probe._read_proc(101)

    def test_oversized_empty_and_non_utf8_process_stat_are_not_absence(self):
        for raw in (b'', b'x' * (probe.MAX_PROC_BYTES + 1), b'\xff'):
            with self.subTest(size=len(raw)), patch('builtins.open', return_value=io.BytesIO(raw)):
                with self.assertRaises((probe.Unverified, UnicodeError)):
                    probe._read_proc(101)


class PosixFixture:
    """Exercise descriptor-relative code using real temp files on Windows without claiming kernel flock."""
    O_RDONLY, O_RDWR = os.O_RDONLY, os.O_RDWR
    O_DIRECTORY, O_NOFOLLOW, O_CLOEXEC, O_NONBLOCK = 1 << 25, 1 << 26, 1 << 27, 1 << 28

    def __init__(self, root):
        self.root = root
        self.directories = {}
        self.files = {}
        self.next_fd = 100000
        self.links = set()
        self.denied = set()
        self.metadata = {}
        self.opens = []
        self.read_hook = None

    def geteuid(self):
        return 0

    def getpid(self):
        return os.getpid()

    def _path(self, path, dir_fd=None):
        return self.root if path == '/' else self.directories[dir_fd] / path

    def _stat(self, path, follow=True):
        original = path.stat() if follow else path.lstat()
        values = {field: getattr(original, field) for field in ('st_mode', 'st_dev', 'st_ino', 'st_uid', 'st_nlink',
                                                              'st_size', 'st_mtime_ns', 'st_ctime_ns')}
        values['st_uid'] = 0
        values.update(self.metadata.get(path, {}))
        if path in self.links:
            values['st_mode'] = stat.S_IFLNK | 0o777
        return types.SimpleNamespace(**values)

    def open(self, path, flags, dir_fd=None):
        actual = self._path(path, dir_fd)
        self.opens.append((actual, flags))
        if actual in self.denied:
            raise PermissionError('private contents must not appear')
        if actual in self.links:
            raise OSError(errno.ELOOP, 'symlink')
        if flags & self.O_DIRECTORY:
            if not actual.is_dir():
                raise FileNotFoundError(str(actual))
            descriptor = self.next_fd
            self.next_fd += 1
            self.directories[descriptor] = actual
            return descriptor
        if not flags & self.O_NOFOLLOW:
            raise AssertionError('Missing no-follow flag')
        if flags & (os.O_CREAT | os.O_TRUNC | os.O_APPEND):
            raise AssertionError('Probe must not create, truncate or append')
        descriptor = os.open(str(actual), (flags & os.O_RDWR) | getattr(os, 'O_BINARY', 0))
        self.files[descriptor] = actual
        return descriptor

    def close(self, descriptor):
        if descriptor in self.directories:
            del self.directories[descriptor]
        else:
            self.files.pop(descriptor)
            os.close(descriptor)

    def stat(self, path, dir_fd=None, follow_symlinks=True):
        return self._stat(self._path(path, dir_fd), follow_symlinks)

    def fstat(self, descriptor):
        if descriptor in self.directories:
            return self._stat(self.directories[descriptor])
        return self._stat(self.files[descriptor])

    def read(self, descriptor, count):
        result = os.read(descriptor, count)
        if self.read_hook is not None:
            hook, self.read_hook = self.read_hook, None
            hook(self.files[descriptor])
        return result


class FakeFlock:
    LOCK_EX, LOCK_NB = 1, 2

    def __init__(self):
        self.busy = False
        self.calls = []

    def flock(self, descriptor, flags):
        self.calls.append((descriptor, flags))
        if flags != self.LOCK_EX | self.LOCK_NB:
            raise AssertionError('Must be nonblocking exclusive')
        if self.busy:
            raise BlockingIOError(errno.EWOULDBLOCK, 'inherited lock remains held')


class PaidArchiveExitPortableTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix='vls-paid-exit-')
        self.root = Path(self.temp.name)
        self.run = self.root / 'run'
        (self.run / 'output/weights').mkdir(parents=True)
        (self.run / 'run.lock').write_bytes(b'')
        self.model = b'original model bytes'
        (self.run / 'output/weights/best.pt').write_bytes(self.model)
        self.state = dict(state='RUNNING', childPid=101, childStart='100', supervisorPid=102, supervisorStart='200')
        self.result = dict(state='SUCCEEDED', sha256=hashlib.sha256(self.model).hexdigest(), size=len(self.model))
        self.write_state()
        self.write_result()
        self.posix = PosixFixture(self.root)
        self.lock = FakeFlock()
        self.stack = contextlib.ExitStack()
        self.stack.enter_context(patch.object(probe, 'os', self.posix))
        self.stack.enter_context(patch.object(probe, 'fcntl', self.lock))
        self.processes = {os.getpid(): proc_stat(os.getpid(), '999')}
        self.stack.enter_context(patch.object(probe, '_read_proc', side_effect=lambda pid: self.processes.get(pid)))

    def tearDown(self):
        self.stack.close()
        self.assertEqual({}, self.posix.files)
        self.assertEqual({}, self.posix.directories)
        self.temp.cleanup()

    def write_state(self):
        (self.run / 'state.json').write_text(json.dumps(self.state), encoding='utf-8')

    def write_result(self):
        (self.run / 'result.json').write_text(json.dumps(self.result), encoding='utf-8')

    def assert_unknown(self):
        self.assertEqual(dict(version=1, state='UNKNOWN', processExited=False), probe.probe('/run'))

    def test_success_uses_original_result_digest_and_changes_no_file(self):
        before = {str(path.relative_to(self.root)): (path.read_bytes(), path.stat().st_mtime_ns)
                  for path in self.root.rglob('*') if path.is_file()}
        result = probe.probe('/run')
        self.assertEqual('SUCCEEDED', result['state'])
        self.assertTrue(result['processExited'])
        self.assertEqual(hashlib.sha256((self.run / 'result.json').read_bytes()).hexdigest(), result['resultSha256'])
        self.assertEqual(self.result['sha256'], result['sha256'])
        self.assertEqual(self.state['childStart'], result['childStart'])
        self.assertEqual(self.state['supervisorPid'], result['supervisorPid'])
        after = {str(path.relative_to(self.root)): (path.read_bytes(), path.stat().st_mtime_ns)
                 for path in self.root.rglob('*') if path.is_file()}
        self.assertEqual(before, after)
        self.assertEqual(1, len(self.lock.calls))

    def test_live_child_or_supervisor_blocks_success_even_with_success_result(self):
        for pid, start in ((101, '100'), (102, '200')):
            with self.subTest(pid=pid):
                self.processes[pid] = proc_stat(pid, start)
                self.assert_unknown()
                self.processes.pop(pid)

    def test_reused_pid_and_zombie_prove_old_process_no_longer_runs(self):
        self.processes[101] = proc_stat(101, '101')
        self.processes[102] = proc_stat(102, '200', 'Z')
        self.assertEqual('SUCCEEDED', probe.probe('/run')['state'])

    def test_inherited_lock_blocks_even_when_recorded_processes_are_absent(self):
        self.lock.busy = True
        self.assert_unknown()

    def test_missing_lock_is_not_created(self):
        (self.run / 'run.lock').unlink()
        self.assert_unknown()
        self.assertFalse((self.run / 'run.lock').exists())

    def test_missing_or_malformed_result_is_unknown(self):
        path = self.run / 'result.json'
        path.unlink()
        self.assert_unknown()
        for raw in (b'bad json', b'[]', b'{"state":"SUCCEEDED","state":"FAILED"}', b' ' * 65537):
            with self.subTest(raw_size=len(raw)):
                path.write_bytes(raw)
                self.assert_unknown()

    def test_non_success_and_invalid_hash_or_size_are_unknown(self):
        for key, value in (('state', 'RUNNING'), ('sha256', 'F' * 64), ('sha256', '1' * 63),
                           ('size', True), ('size', 0), ('size', 1.2), ('size', '20'), ('size', 9223372036854775808)):
            original = self.result[key]
            self.result[key] = value
            self.write_result()
            self.assert_unknown()
            self.result[key] = original

    def test_missing_mismatched_or_replaced_model_is_unknown(self):
        path = self.run / 'output/weights/best.pt'
        path.unlink()
        self.assert_unknown()
        path.write_bytes(b'different length')
        self.assert_unknown()

    def test_symlink_in_root_lock_json_or_artifact_path_is_rejected(self):
        for path in (self.run, self.run / 'run.lock', self.run / 'state.json', self.run / 'result.json',
                     self.run / 'output', self.run / 'output/weights', self.run / 'output/weights/best.pt'):
            with self.subTest(path=path.name):
                self.posix.links.add(path)
                self.assert_unknown()
                self.posix.links.clear()

    def test_foreign_owner_and_hard_link_are_rejected(self):
        for path in (self.run / 'run.lock', self.run / 'state.json', self.run / 'result.json', self.run / 'output/weights/best.pt'):
            for field, value in (('st_uid', 123), ('st_nlink', 2), ('st_mode', stat.S_IFIFO | 0o600)):
                with self.subTest(path=path.name, field=field):
                    self.posix.metadata[path] = {field: value}
                    self.assert_unknown()
                    self.posix.metadata.clear()

    def test_permission_failure_is_unknown_not_process_exit(self):
        self.posix.denied.add(self.run / 'state.json')
        self.assert_unknown()
        self.posix.denied.clear()
        with patch.object(probe, '_read_proc', side_effect=PermissionError('secret proc contents')):
            self.assert_unknown()
        def child_permission_denied(pid):
            if pid == 101:
                raise PermissionError('child proc denied')
            return self.processes.get(pid)
        with patch.object(probe, '_read_proc', side_effect=child_permission_denied):
            self.assert_unknown()

    def test_absent_proc_mount_or_malformed_proc_stat_is_unknown(self):
        self.processes.clear()
        self.assert_unknown()
        self.processes[os.getpid()] = 'malformed'
        self.assert_unknown()
        self.processes[os.getpid()] = proc_stat(os.getpid(), '999')
        self.processes[101] = '101 (partial) S 1'
        self.assert_unknown()

    def test_missing_pid_or_start_never_counts_as_exit(self):
        for field in ('childPid', 'childStart', 'supervisorPid', 'supervisorStart'):
            original = self.state.pop(field)
            self.write_state()
            self.assert_unknown()
            self.state[field] = original
        self.state['childPid'] = True
        self.write_state()
        self.assert_unknown()

    def test_changed_state_during_read_is_unknown(self):
        self.posix.read_hook = lambda path: path.write_bytes(path.read_bytes() + b' ')
        self.assert_unknown()

    def test_relative_or_parent_traversal_path_is_unknown(self):
        for path in ('run', '/run/../run', '/run/./', '/', '/run\x00'):
            self.assertFalse(probe.probe(path)['processExited'])

    def test_marker_and_failures_never_print_arbitrary_exception_details(self):
        self.posix.denied.add(self.run / 'result.json')
        out, err = io.StringIO(), io.StringIO()
        with patch.object(sys, 'argv', ['-c', '/run']), contextlib.redirect_stdout(out), contextlib.redirect_stderr(err):
            probe.main()
        self.assertEqual('', err.getvalue())
        self.assertTrue(out.getvalue().startswith('VLS_ARCHIVE_EXIT='))
        self.assertNotIn('private', out.getvalue())
        self.assertFalse(json.loads(out.getvalue().split('=', 1)[1])['processExited'])

    def test_unsupported_lock_platform_fails_closed(self):
        with patch.object(probe, 'fcntl', None):
            self.assert_unknown()


@unittest.skipUnless(sys.platform.startswith('linux'), 'Requires actual Linux /proc and flock; portable fixtures run separately')
class PaidArchiveExitLinuxLockTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix='vls-paid-lock-')
        self.run = Path(self.temp.name)
        (self.run / 'run.lock').write_bytes(b'')
        (self.run / 'output/weights').mkdir(parents=True)
        (self.run / 'output/weights/best.pt').write_bytes(b'weights')
        self.children = []
        state = dict(state='RUNNING')
        for role in ('child', 'supervisor'):
            child = subprocess.Popen([sys.executable, '-c', 'import sys; print("READY",flush=True); sys.stdin.read()'],
                                     stdin=subprocess.PIPE, stdout=subprocess.PIPE, text=True)
            self.children.append(child)
            self.assertEqual('READY', child.stdout.readline().strip())
            state[role + 'Pid'] = child.pid
            state[role + 'Start'] = probe._parse_proc(child.pid, probe._read_proc(child.pid))[1]
        self.state = state
        (self.run / 'state.json').write_text(json.dumps(state))
        (self.run / 'result.json').write_text(json.dumps(dict(state='SUCCEEDED', sha256=hashlib.sha256(b'weights').hexdigest(), size=7)))

    def tearDown(self):
        for child in self.children:
            if child.poll() is None:
                child.communicate(timeout=5)
        self.temp.cleanup()

    def stop_recorded(self):
        for child in self.children:
            child.communicate(timeout=5)

    def test_actual_running_process_rejected_then_exit_proven_without_file_writes(self):
        self.assertFalse(probe.probe(str(self.run))['processExited'])
        self.stop_recorded()
        before = {str(path): path.read_bytes() for path in self.run.rglob('*') if path.is_file()}
        self.assertTrue(probe.probe(str(self.run))['processExited'])
        self.assertEqual(before, {str(path): path.read_bytes() for path in self.run.rglob('*') if path.is_file()})

    def test_kernel_lock_inherited_by_child_still_blocks_after_parent_closes(self):
        self.stop_recorded()
        descriptor = os.open(str(self.run / 'run.lock'), os.O_RDWR | os.O_NOFOLLOW)
        probe.fcntl.flock(descriptor, probe.fcntl.LOCK_EX | probe.fcntl.LOCK_NB)
        child = subprocess.Popen([sys.executable, '-c', 'import sys; print("READY",flush=True); sys.stdin.read()'],
                                 pass_fds=(descriptor,), stdin=subprocess.PIPE, stdout=subprocess.PIPE, text=True)
        self.children.append(child)
        try:
            self.assertEqual('READY', child.stdout.readline().strip())
        finally:
            os.close(descriptor)
        self.assertFalse(probe.probe(str(self.run))['processExited'])
        child.communicate(timeout=5)
        self.assertTrue(probe.probe(str(self.run))['processExited'])

    def test_real_model_symlink_is_rejected(self):
        self.stop_recorded()
        model = self.run / 'output/weights/best.pt'
        model.rename(model.with_name('original.pt'))
        model.symlink_to('original.pt')
        self.assertFalse(probe.probe(str(self.run))['processExited'])


if __name__ == '__main__':
    unittest.main()
