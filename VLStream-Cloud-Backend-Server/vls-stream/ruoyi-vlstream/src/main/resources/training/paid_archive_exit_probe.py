"""Read-only proof for an official paid job whose original training process has exited."""
from contextlib import contextmanager
import hashlib
import json
import os
import re
import stat
import sys

try:
    import fcntl
except ImportError:
    fcntl = None

MAX_JSON_BYTES = 65536
MAX_PROC_BYTES = 8192


class Unverified(Exception):
    pass


def _file_identity(info):
    return info.st_dev, info.st_ino


def _snapshot(info):
    return (_file_identity(info), info.st_size, info.st_mtime_ns, info.st_ctime_ns)


def _regular(info):
    if not stat.S_ISREG(info.st_mode) or info.st_uid != os.geteuid() or info.st_nlink != 1:
        raise Unverified('FILE_IDENTITY')


def _named_identity(parent, name, descriptor, directory=False):
    named = os.stat(name, dir_fd=parent, follow_symlinks=False)
    opened = os.fstat(descriptor)
    if stat.S_ISLNK(named.st_mode) or _file_identity(named) != _file_identity(opened):
        raise Unverified('PATH_CHANGED')
    if directory:
        if not stat.S_ISDIR(named.st_mode):
            raise Unverified('DIRECTORY_REQUIRED')
    else:
        _regular(named)
        _regular(opened)


@contextmanager
def _directory_tree(path):
    if not isinstance(path, str) or not path.startswith('/') or '\x00' in path:
        raise Unverified('ABSOLUTE_PATH_REQUIRED')
    parts = [part for part in path.split('/') if part]
    if not parts or any(part in ('.', '..') for part in parts):
        raise Unverified('INVALID_PATH')
    descriptors = []
    links = []
    flags = os.O_RDONLY | os.O_DIRECTORY | os.O_NOFOLLOW | os.O_CLOEXEC
    try:
        parent = os.open('/', flags)
        descriptors.append(parent)
        for part in parts:
            child = os.open(part, flags, dir_fd=parent)
            descriptors.append(child)
            _named_identity(parent, part, child, directory=True)
            links.append((parent, part, child))
            parent = child
        yield parent
        for parent, name, child in reversed(links):
            _named_identity(parent, name, child, directory=True)
    finally:
        for descriptor in reversed(descriptors):
            os.close(descriptor)


@contextmanager
def _regular_file(parent, name, writable=False):
    flags = (os.O_RDWR if writable else os.O_RDONLY) | os.O_NOFOLLOW | os.O_CLOEXEC | os.O_NONBLOCK
    descriptor = os.open(name, flags, dir_fd=parent)
    try:
        _regular(os.fstat(descriptor))
        _named_identity(parent, name, descriptor)
        yield descriptor
        _named_identity(parent, name, descriptor)
    finally:
        os.close(descriptor)


def _unique_object(pairs):
    result = {}
    for name, value in pairs:
        if name in result:
            raise Unverified('DUPLICATE_JSON_FIELD')
        result[name] = value
    return result


def _read_json(parent, name):
    with _regular_file(parent, name) as descriptor:
        before = os.fstat(descriptor)
        if before.st_size <= 0 or before.st_size > MAX_JSON_BYTES:
            raise Unverified('JSON_SIZE')
        blocks = []
        length = 0
        while True:
            block = os.read(descriptor, min(8192, MAX_JSON_BYTES + 1 - length))
            if not block:
                break
            blocks.append(block)
            length += len(block)
            if length > MAX_JSON_BYTES:
                raise Unverified('JSON_SIZE')
        if _snapshot(before) != _snapshot(os.fstat(descriptor)) or length != before.st_size:
            raise Unverified('FILE_CHANGED')
        raw = b''.join(blocks)
        data = json.loads(raw.decode('utf-8'), object_pairs_hook=_unique_object)
        if not isinstance(data, dict):
            raise Unverified('JSON_OBJECT_REQUIRED')
        return data, raw, _snapshot(before)


def _read_proc(pid):
    # Only ENOENT means the recorded PID is absent. EACCES, EIO and malformed stat are unknown.
    try:
        with open('/proc/{}/stat'.format(pid), 'rb') as source:
            raw = source.read(MAX_PROC_BYTES + 1)
    except FileNotFoundError:
        return None
    if not raw or len(raw) > MAX_PROC_BYTES:
        raise Unverified('PROC_STAT_SIZE')
    return raw.decode('utf-8')


def _process_exited(pid, start):
    if type(pid) is not int or pid <= 0 or pid > 2147483647:
        raise Unverified('PID_REQUIRED')
    if not isinstance(start, str) or re.fullmatch(r'[1-9][0-9]{0,19}', start) is None:
        raise Unverified('PROCESS_START_REQUIRED')
    value = _read_proc(pid)
    if value is None:
        return True
    process_state, actual_start = _parse_proc(pid, value)
    return actual_start != start or process_state == 'Z'


def _parse_proc(pid, value):
    if not isinstance(value, str):
        raise Unverified('PROC_UNAVAILABLE')
    prefix, separator, tail = value.rpartition(')')
    if not separator or not prefix.startswith(str(pid) + ' ('):
        raise Unverified('PROC_STAT_INVALID')
    fields = tail.split()
    if len(fields) < 20 or re.fullmatch(r'[1-9][0-9]{0,19}', fields[19]) is None:
        raise Unverified('PROC_STAT_INVALID')
    if fields[0] not in ('R', 'S', 'D', 'Z', 'T', 't', 'X', 'x', 'K', 'W', 'P', 'I'):
        raise Unverified('PROC_STATE_UNKNOWN')
    return fields[0], fields[19]


def _check_processes(state):
    for role in ('child', 'supervisor'):
        if not _process_exited(state.get(role + 'Pid'), state.get(role + 'Start')):
            raise Unverified('PROCESS_STILL_RUNNING')
    if state['childPid'] == state['supervisorPid']:
        raise Unverified('PROCESS_IDENTITY_CONFLICT')


def _read_model(root, expected_size):
    directories = []
    links = []
    parent = root
    try:
        for name in ('output', 'weights'):
            child = os.open(name, os.O_RDONLY | os.O_DIRECTORY | os.O_NOFOLLOW | os.O_CLOEXEC, dir_fd=parent)
            directories.append(child)
            _named_identity(parent, name, child, directory=True)
            links.append((parent, name, child))
            parent = child
        with _regular_file(parent, 'best.pt') as model:
            before = _snapshot(os.fstat(model))
            if before[1] != expected_size:
                raise Unverified('MODEL_SIZE_MISMATCH')
            if before != _snapshot(os.fstat(model)):
                raise Unverified('MODEL_CHANGED')
        for parent, name, child in reversed(links):
            _named_identity(parent, name, child, directory=True)
    finally:
        for descriptor in reversed(directories):
            os.close(descriptor)


def probe(run_dir):
    unknown = dict(version=1, state='UNKNOWN', processExited=False)
    try:
        if fcntl is None or any(not hasattr(os, name) for name in ('O_NOFOLLOW', 'O_DIRECTORY', 'O_CLOEXEC', 'geteuid')):
            raise Unverified('UNSUPPORTED_PLATFORM')
        # Missing /proc itself must not turn every recorded PID into a false exit proof.
        if _parse_proc(os.getpid(), _read_proc(os.getpid()))[0] == 'Z':
            raise Unverified('PROC_UNAVAILABLE')
        with _directory_tree(run_dir) as root:
            with _regular_file(root, 'run.lock', writable=True) as lock:
                # This is the existing inherited job lock. Never create or replace it.
                fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
                state, state_raw, _ = _read_json(root, 'state.json')
                result, result_raw, _ = _read_json(root, 'result.json')
                _check_processes(state)
                if result.get('state') != 'SUCCEEDED' or not isinstance(result.get('sha256'), str) \
                    or re.fullmatch(r'[0-9a-f]{64}', result['sha256']) is None \
                    or type(result.get('size')) is not int or not 0 < result['size'] <= 9223372036854775807:
                    raise Unverified('SUCCESSFUL_RESULT_REQUIRED')
                _read_model(root, result['size'])
                _check_processes(state)
                # Atomic result replacement or another state update invalidates this observation.
                if _read_json(root, 'state.json')[1] != state_raw or _read_json(root, 'result.json')[1] != result_raw:
                    raise Unverified('EVIDENCE_CHANGED')
                proof = dict(version=1, state='SUCCEEDED', processExited=True, sha256=result['sha256'], size=result['size'],
                             resultSha256=hashlib.sha256(result_raw).hexdigest())
                for field in ('childPid', 'childStart', 'supervisorPid', 'supervisorStart'):
                    proof[field] = state[field]
        return proof
    except Exception:
        # Do not print file contents, arbitrary exception text, paths, or a false exit assertion.
        return unknown


def main():
    result = probe(sys.argv[1]) if len(sys.argv) == 2 else dict(version=1, state='UNKNOWN', processExited=False)
    print('VLS_ARCHIVE_EXIT=' + json.dumps(result, separators=(',', ':'), sort_keys=True))


if __name__ == '__main__':
    main()
