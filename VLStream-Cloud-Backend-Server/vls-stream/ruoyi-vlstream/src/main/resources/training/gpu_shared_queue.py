"""Company GPU task queue shared by local/VM backends through SSH, never a network server.

The Linux GPU flock protects live compute. This durable FIFO additionally keeps
the logical task lease until verified archive acknowledgement, without TTL steals.
"""
from contextlib import contextmanager
import base64
import fcntl
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import stat
import subprocess
import sys
import uuid

MODE = 'SHARED_GPU_QUEUE'
ENVIRONMENTS = {'localhost', 'vm', 'prod-multi'}
HOME = Path('/home/oort')
ROOT = HOME / '.local/state/vlstream-shared/v1'
RUN_BASES = ('/data/work/vlstream-gpu', '/home/oort/vlstream')


def validate_ticket(value):
    fields = {'environment', 'jobId', 'owner', 'runDir'}
    if not isinstance(value, dict) or set(value) not in (fields, fields | {'runRoot'}):
        raise ValueError('Invalid shared task fields')
    environment = value['environment']
    approved_environment = isinstance(environment, str) and re.fullmatch('[A-Za-z0-9_-]{1,64}', environment)
    if (not approved_environment
            or ('runRoot' not in value and environment not in ENVIRONMENTS)
            or str(uuid.UUID(value['jobId'])) != value['jobId']):
        raise ValueError('Invalid shared task identity')
    if not isinstance(value['owner'], str) or not re.fullmatch('[a-f0-9]{64}', value['owner']):
        raise ValueError('Missing authenticated task owner')
    run = value['runDir']
    if not isinstance(run, str) or not re.fullmatch('/[A-Za-z0-9_./-]+', run) or '..' in run or '//' in run:
        raise ValueError('Invalid task directory')
    if 'runRoot' in value:
        root = validated_queue_root(value['runRoot'])
        prefix = str(root) + '/'
        suffix = run[len(prefix):] if run.startswith(prefix) else ''
        if not re.fullmatch('[a-f0-9]{16}/' + re.escape(value['jobId']), suffix):
            raise ValueError('Task directory does not match its frozen root and tenant namespace')
    elif not run.startswith(tuple(base + '/' + environment + '/' for base in RUN_BASES)) or not run.endswith('/' + value['jobId']):
        raise ValueError('Task directory is outside its platform namespace')
    return hashlib.sha256((value['environment'] + '\n' + value['jobId']).encode()).hexdigest()


def validated_queue_root(value):
    if not isinstance(value, str) or value == '/' or not re.fullmatch('/[A-Za-z0-9_./-]+', value) or '..' in value or '//' in value:
        raise ValueError('Invalid frozen queue or task root')
    return Path(value)


def validate_gpu(value):
    if not isinstance(value, dict) or set(value) != {'gpuIndex', 'gpuUuid'}:
        raise ValueError('Invalid physical GPU fields')
    index, identity = value['gpuIndex'], value['gpuUuid']
    if type(index) is not int or not 0 <= index <= 15 or not isinstance(identity, str) or not re.fullmatch('GPU-[a-fA-F0-9-]{36}', identity):
        raise ValueError('Invalid physical GPU identity')


def verify_physical_gpu(gpu):
    validate_gpu(gpu)
    actual = subprocess.run(['nvidia-smi', '-i', str(gpu['gpuIndex']), '--query-gpu=uuid', '--format=csv,noheader,nounits'],
        capture_output=True, text=True, timeout=15, check=True).stdout.strip()
    if actual != gpu['gpuUuid']:
        raise ValueError('Physical GPU identity changed; no task was admitted')


def read_json(path):
    path = Path(path)
    if not path.exists():
        return {}
    if path.is_symlink() or not path.is_file() or path.stat().st_size > 16 * 1024 * 1024:
        raise ValueError('Unsafe or oversized queue/task metadata')
    return json.loads(path.read_text(encoding='utf-8'))


def process_alive(pid, start):
    if type(pid) is not int or pid <= 0 or not start:
        return False
    try:
        fields = Path('/proc/{}/stat'.format(pid)).read_text().rsplit(')', 1)[1].split()
        return fields[0] != 'Z' and fields[19] == str(start)
    except (OSError, IndexError):
        return False


@contextmanager
def confirmed_exit(root):
    descriptor = None
    try:
        if root.exists():
            if root.is_symlink() or root.resolve() != root:
                raise ValueError('Unsafe task directory; lease retained')
            descriptor = os.open(str(root / 'run.lock'), os.O_RDWR | os.O_CREAT | os.O_NOFOLLOW, 0o600)
            info = os.fstat(descriptor)
            if not stat.S_ISREG(info.st_mode) or info.st_uid != os.geteuid() or info.st_nlink != 1:
                raise ValueError('Task process lock identity invalid; lease retained')
            fcntl.flock(descriptor, fcntl.LOCK_EX | fcntl.LOCK_NB)
        state_file = read_json(root / 'state.json')
        if process_alive(state_file.get('childPid'), state_file.get('childStart')) or process_alive(state_file.get('supervisorPid'), state_file.get('supervisorStart')):
            raise ValueError('Training process still alive; lease retained')
        yield
    finally:
        if descriptor is not None:
            os.close(descriptor)


def cleanup_paths(ticket):
    root = Path(ticket['runDir'])
    if (str(root) != ticket['runDir'] or not re.fullmatch('[a-f0-9]{16}', root.parent.name)
            or ('runRoot' not in ticket and root.parent.parent not in
                {Path(base) / ticket['environment'] for base in RUN_BASES})
            or root.parent.resolve() != root.parent):
        raise ValueError('Invalid workspace cleanup scope')
    return root, root.with_name('.vls-cleanup-' + ticket['jobId'])


def workspace_identity(path, *, require_safe=True):
    try:
        info = path.lstat()
    except FileNotFoundError:
        return None
    unsafe_permissions = info.st_mode & 0o002 or (info.st_mode & 0o020 and info.st_gid != os.getegid())
    if not stat.S_ISDIR(info.st_mode) or info.st_uid != os.geteuid() or unsafe_permissions:
        if require_safe:
            raise ValueError('Workspace directory identity is unsafe')
        return None
    return [info.st_dev, info.st_ino]


def reject_workspace_mounts(path):
    for line in Path('/proc/self/mountinfo').read_text().splitlines():
        mount = re.sub(r'\\([0-7]{3})', lambda part: chr(int(part[1], 8)), line.split()[4])
        if mount == str(path) or mount.startswith(str(path) + '/'):
            raise ValueError('Workspace contains a mount; cleanup deferred')


class Registry:
    def __init__(self, gpu, root=None):
        validate_gpu(gpu)
        self.gpu = dict(gpu)
        self.base = (ROOT if root is None else Path(root)) / gpu['gpuUuid']
        self.base.mkdir(mode=0o700, parents=True, exist_ok=True)
        info = self.base.stat()
        if self.base.is_symlink() or self.base.resolve() != self.base or info.st_uid != os.geteuid() or info.st_mode & 0o022:
            raise ValueError('Shared queue path/owner permissions are invalid')
        self.path = self.base / 'registry.json'

    @contextmanager
    def locked(self):
        descriptor = os.open(str(self.base / 'registry.lock'), os.O_RDWR | os.O_CREAT | os.O_NOFOLLOW, 0o600)
        try:
            info = os.fstat(descriptor)
            if not stat.S_ISREG(info.st_mode) or info.st_uid != os.geteuid() or info.st_nlink != 1:
                raise ValueError('Shared queue lock identity is invalid')
            fcntl.flock(descriptor, fcntl.LOCK_EX)
            state = read_json(self.path)
            if not state:
                state = dict(version=1, gpu=self.gpu, sequence=0, active=None, tickets={})
            if state.get('version') != 1 or state.get('gpu') != self.gpu or not isinstance(state.get('tickets'), dict):
                raise ValueError('Shared queue protocol/physical identity mismatch')
            if type(state.get('sequence')) is not int or state['sequence'] < 0 or len(state['tickets']) > 50000:
                raise ValueError('Shared queue sequence/history is invalid')
            sequences = set()
            for key, row in state['tickets'].items():
                ticket = {name: row.get(name) for name in ('environment', 'jobId', 'owner', 'runDir')} if isinstance(row, dict) else {}
                if isinstance(row, dict) and 'runRoot' in row:
                    ticket['runRoot'] = row['runRoot']
                if not isinstance(row, dict) or key != validate_ticket(ticket):
                    raise ValueError('Shared queue ticket identity is invalid')
                sequence = row.get('sequence')
                if type(sequence) is not int or not 0 < sequence <= state['sequence'] or sequence in sequences or row.get('state') not in ('WAITING', 'RUNNING', 'ARCHIVING', 'DONE'):
                    raise ValueError('Shared queue ticket state is invalid')
                sequences.add(sequence)
                if row['state'] in ('RUNNING', 'ARCHIVING') and state.get('active') != key:
                    raise ValueError('Shared queue has an unowned active task')
            active = state.get('active')
            if active is not None and (active not in state['tickets'] or state['tickets'][active]['state'] not in ('RUNNING', 'ARCHIVING')):
                raise ValueError('Shared queue active lease is invalid')
            yield state
        finally:
            os.close(descriptor)

    def persist(self, state):
        temporary = self.path.with_name('registry.' + uuid.uuid4().hex + '.tmp')
        with temporary.open('x', encoding='utf-8') as output:
            json.dump(state, output, ensure_ascii=False)
            output.flush(); os.fsync(output.fileno())
        os.chmod(temporary, 0o600)
        os.replace(temporary, self.path)
        descriptor = os.open(str(self.base), os.O_RDONLY | os.O_DIRECTORY)
        try:
            os.fsync(descriptor)
        finally:
            os.close(descriptor)

    def register(self, tickets):
        if not isinstance(tickets, list) or not tickets or len(tickets) > 500:
            raise ValueError('Invalid admission batch')
        checked = [(validate_ticket(ticket), dict(ticket)) for ticket in tickets]
        with self.locked() as state:
            for key, ticket in checked:
                before = state['tickets'].get(key)
                if before is not None:
                    if any(before.get(field) != ticket[field] for field in ticket):
                        raise ValueError('Shared task identity cannot be rebound')
                    continue
                if len(state['tickets']) >= 50000:
                    raise ValueError('Shared queue history limit reached; administrator review required')
                state['sequence'] += 1
                state['tickets'][key] = dict(ticket, sequence=state['sequence'], state='WAITING')
            self.persist(state)
            return [self.view(state, key) for key, _ in checked]

    @staticmethod
    def view(state, key):
        ticket = state['tickets'].get(key)
        if ticket is None:
            return dict(state='NOT_REGISTERED', position=None)
        waiting = sorted((item for item in state['tickets'].items() if item[1]['state'] != 'DONE'), key=lambda item: item[1]['sequence'])
        position = next((index + 1 for index, item in enumerate(waiting) if item[0] == key), 0)
        return dict(state=ticket['state'], position=position, ownsLease=state['active'] == key)

    def inspect(self, ticket=None):
        with self.locked() as state:
            if ticket is None:
                return dict(active=state['active'] is not None,
                    waiting=sum(row['state'] != 'DONE' for row in state['tickets'].values()))
            key = self.require_ticket(state, ticket)
            return self.view(state, key)

    @staticmethod
    def require_ticket(state, ticket):
        key = validate_ticket(ticket)
        stored = state['tickets'].get(key)
        if stored is None or any(stored.get(field) != value for field, value in ticket.items()):
            raise ValueError('Shared task was not registered with this identity')
        return key

    def claim(self, ticket):
        """Caller must already hold the host GPU flock and have checked external GPU processes."""
        with self.locked() as state:
            key = self.require_ticket(state, ticket)
            if state['tickets'][key]['state'] == 'DONE':
                raise ValueError('Finished task cannot reclaim a GPU')
            if state['tickets'][key]['state'] == 'ARCHIVING':
                raise ValueError('Archive acknowledgement is required before another run')
            if state['active'] not in (None, key):
                return False
            waiting = [item for item in state['tickets'].items() if item[1]['state'] != 'DONE']
            head = min(waiting, key=lambda item: item[1]['sequence'])[0]
            if head != key:
                return False
            state['active'] = key
            state['tickets'][key]['state'] = 'RUNNING'
            self.persist(state)
            return True

    def archiving(self, ticket):
        with self.locked() as state:
            key = self.require_ticket(state, ticket)
            if state['active'] != key:
                raise ValueError('Only the current task lease may enter archiving')
            state['tickets'][key]['state'] = 'ARCHIVING'
            self.persist(state)

    @staticmethod
    def verify_detection_archive(root, acknowledgement):
        result = read_json(root / 'result.json')
        manifest = read_json(root / 'manifest.json')
        acknowledged = read_json(root / 'acknowledged.json').get('events', [])
        events = manifest.get('events', [])
        manifest_path = root / 'manifest.json'
        sha = hashlib.sha256(manifest_path.read_bytes()).hexdigest() if manifest_path.is_file() else ''
        if result.get('kind') != 'CLOUD_AI' or result.get('state') not in ('COMPLETED', 'CANCELLED', 'SKIPPED', 'FAILED'):
            raise ValueError('Detection terminal proof missing; lease retained')
        if not re.fullmatch('[a-f0-9]{64}', sha) or sha != result.get('manifestSha256') or sha != acknowledgement.get('manifestSha256'):
            raise ValueError('Detection manifest acknowledgement mismatch; lease retained')
        if result.get('eventCount') != len(events) or acknowledgement.get('eventCount') != len(events) or set(acknowledged) != {event['id'] for event in events}:
            raise ValueError('Detection evidence has not been archived; lease retained')

    @staticmethod
    def verify_training_archive(root, acknowledgement):
        if not re.fullmatch('[a-f0-9]{64}', acknowledgement.get('ptSha256', '')) or type(acknowledgement.get('size')) is not int or acknowledgement['size'] < 1:
            raise ValueError('Verified PT archive acknowledgement missing')
        result = read_json(root / 'result.json')
        if result.get('state') != 'SUCCEEDED' or result.get('sha256') != acknowledgement.get('ptSha256') or result.get('size') != acknowledgement.get('size'):
            raise ValueError('Successful task archive acknowledgement does not match its result')
        if not re.fullmatch('[a-f0-9]{64}', acknowledgement.get('classesSha256', '')):
            raise ValueError('Verified classes archive acknowledgement missing')

    def release(self, ticket, acknowledgement):
        with self.locked() as state:
            requested = validate_ticket(ticket)
            if requested not in state['tickets'] and acknowledgement.get('state') == 'DETECTION_ARCHIVED':
                root = Path(ticket['runDir'])
                with confirmed_exit(root):
                    self.verify_detection_archive(root, acknowledgement)
                    return dict(state='DONE', position=0, ownsLease=False)
            if requested not in state['tickets'] and acknowledgement.get('state') == 'CANCELLED':
                # No admission ever took place, and no process state may be present.
                if read_json(Path(ticket['runDir']) / 'state.json'):
                    raise ValueError('Unregistered task has process state; manual reconciliation required')
                return dict(state='DONE', position=0, ownsLease=False)
            key = self.require_ticket(state, ticket)
            row = state['tickets'][key]
            if row['state'] == 'DONE':
                if row.get('acknowledgement') != acknowledgement:
                    raise ValueError('Archive/terminal acknowledgement changed')
                return self.view(state, key)
            root = Path(row['runDir'])
            with confirmed_exit(root):
                if acknowledgement.get('state') == 'ARCHIVED':
                    self.verify_training_archive(root, acknowledgement)
                elif acknowledgement.get('state') == 'DETECTION_ARCHIVED':
                    self.verify_detection_archive(root, acknowledgement)
                elif acknowledgement.get('state') in ('FAILED', 'CANCELLED'):
                    result = read_json(root / 'result.json')
                    if state['active'] == key and result.get('state') != acknowledgement['state']:
                        raise ValueError('Active task has no matching terminal result; lease retained')
                    if result and result.get('state') != acknowledgement['state']:
                        raise ValueError('Task terminal result conflicts; lease retained')
                else:
                    raise ValueError('Unsupported shared task acknowledgement')
                row.update(state='DONE', acknowledgement=dict(acknowledgement))
                if acknowledgement['state'] in ('ARCHIVED', 'FAILED', 'CANCELLED'):
                    row['workspaceIdentity'] = workspace_identity(root, require_safe=False)
                if state['active'] == key:
                    state['active'] = None
                self.persist(state)
                return self.view(state, key)

    def verify_historical_workspace(self, root, row):
        """Older DONE tickets do not record the released directory inode."""
        config = read_json(root / 'job.json')
        shared = config.get('sharedQueue', {})
        ticket_fields = ('environment', 'jobId', 'owner')
        if (shared.get('mode') != MODE or config.get('gpuIndex') != self.gpu['gpuIndex']
                or shared.get('gpuUuid') != self.gpu['gpuUuid']
                or any(shared.get(field) != row[field] for field in ticket_fields)):
            raise ValueError('Historical workspace identity is not verifiable')
        acknowledgement = row['acknowledgement']
        if acknowledgement['state'] == 'ARCHIVED':
            self.verify_training_archive(root, acknowledgement)
        elif read_json(root / 'result.json').get('state') != acknowledgement['state']:
            raise ValueError('Historical workspace terminal result does not match its acknowledgement')

    def cleanup(self, ticket, acknowledgement):
        if not isinstance(acknowledgement, dict) or acknowledgement.get('state') not in ('ARCHIVED', 'FAILED', 'CANCELLED'):
            raise ValueError('Unsupported workspace cleanup acknowledgement')
        key = validate_ticket(ticket)
        root, quarantine = cleanup_paths(ticket)
        with self.locked() as state:
            original = workspace_identity(root)
            pending = workspace_identity(quarantine)
            if key not in state['tickets'] and acknowledgement == {'state': 'CANCELLED'} and original is None and pending is None:
                return dict(state='CLEANED')
            self.require_ticket(state, ticket)
            row = state['tickets'][key]
            if (row.get('runRoot') != ticket.get('runRoot') or row['state'] != 'DONE'
                    or row.get('acknowledgement') != acknowledgement):
                raise ValueError('Workspace cleanup requires the released terminal acknowledgement')
            progress = row.get('workspaceCleanup')
            if progress not in (None, 'PENDING', 'CLEANED'):
                raise ValueError('Invalid workspace cleanup progress')
            if original is None and pending is None:
                if progress != 'CLEANED':
                    if root.parent.exists():
                        descriptor = os.open(str(root.parent), os.O_RDONLY | os.O_DIRECTORY | os.O_NOFOLLOW)
                        try:
                            os.fsync(descriptor)
                        finally:
                            os.close(descriptor)
                    self.finish_cleanup(state, row)
                return dict(state='CLEANED')
            if progress == 'CLEANED' or (pending is not None and (progress != 'PENDING' or original is not None)):
                raise ValueError('Released workspace was recreated or its quarantine is inconsistent')
            path, identity = (root, original) if pending is None else (quarantine, pending)
            with confirmed_exit(path):
                if workspace_identity(path) != identity:
                    raise ValueError('Workspace identity changed during cleanup')
                if 'workspaceIdentity' not in row and progress is None:
                    self.verify_historical_workspace(root, row)
                    row['workspaceIdentity'] = identity
                if row.get('workspaceIdentity') != identity:
                    raise ValueError('Released workspace identity changed')
                reject_workspace_mounts(path)
                if not shutil.rmtree.avoids_symlink_attacks:
                    raise ValueError('Symlink-safe recursive cleanup is unavailable')
                descriptor = os.open(str(root.parent), os.O_RDONLY | os.O_DIRECTORY | os.O_NOFOLLOW)
                try:
                    parent = Path('/proc/self/fd') / str(descriptor)
                    if workspace_identity(parent / path.name) != identity:
                        raise ValueError('Workspace parent identity changed')
                    if progress is None:
                        row['workspaceCleanup'] = 'PENDING'
                        self.persist(state)
                    if pending is None:
                        if workspace_identity(parent / quarantine.name) is not None:
                            raise ValueError('Workspace quarantine already exists')
                        os.rename(root.name, quarantine.name, src_dir_fd=descriptor, dst_dir_fd=descriptor)
                        os.fsync(descriptor)
                    if workspace_identity(parent / quarantine.name) != identity:
                        raise ValueError('Workspace quarantine identity changed')
                    reject_workspace_mounts(quarantine)
                    shutil.rmtree(str(parent / quarantine.name))
                    os.fsync(descriptor)
                finally:
                    os.close(descriptor)
            self.finish_cleanup(state, row)
            return dict(state='CLEANED')

    def finish_cleanup(self, state, row):
        row.pop('workspaceIdentity', None)
        row['workspaceCleanup'] = 'CLEANED'
        self.persist(state)


def context(config, run_dir):
    shared = config.get('sharedQueue')
    if not isinstance(shared, dict) or shared.get('mode') != MODE:
        raise ValueError('Missing company shared queue configuration')
    gpu = {'gpuIndex': config['gpuIndex'], 'gpuUuid': shared['gpuUuid']}
    ticket = dict(environment=shared['environment'], jobId=shared['jobId'], owner=shared['owner'], runDir=str(run_dir))
    if 'runRoot' in shared:
        ticket['runRoot'] = shared['runRoot']
    validate_gpu(gpu); validate_ticket(ticket)
    root = validated_queue_root(shared['queueRoot']) if 'queueRoot' in shared else None
    return Registry(gpu, root), ticket


def main():
    action, encoded = sys.argv[1:3]
    payload = json.loads(base64.b64decode(encoded))
    if 'queueRoot' not in payload and (str(Path.home()) != str(HOME) or os.geteuid() != HOME.stat().st_uid):
        raise ValueError('Legacy shared queue requires the approved existing oort account')
    gpu = payload['gpu']
    root = validated_queue_root(payload['queueRoot']) if 'queueRoot' in payload else None
    registry = Registry(gpu, root)
    if action == 'register':
        verify_physical_gpu(gpu)
        value = registry.register(payload['tickets'])
    elif action == 'inspect':
        value = registry.inspect(payload.get('ticket'))
    elif action == 'release':
        value = registry.release(payload['ticket'], payload['acknowledgement'])
    elif action == 'cleanup':
        value = registry.cleanup(payload['ticket'], payload['acknowledgement'])
    else:
        raise ValueError('Unknown shared queue action')
    print('VLS_SHARED=' + json.dumps(value, ensure_ascii=False))


if __name__ == '__main__':
    main()
