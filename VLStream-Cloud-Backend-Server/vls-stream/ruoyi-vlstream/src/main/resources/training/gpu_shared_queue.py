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
import stat
import subprocess
import sys
import uuid

MODE = 'SHARED_GPU_QUEUE'
ENVIRONMENTS = {'localhost', 'vm'}
HOME = Path('/home/oort')
ROOT = HOME / '.local/state/vlstream-shared/v1'
RUN_BASES = ('/data/work/vlstream-gpu', '/home/oort/vlstream')


def validate_ticket(value):
    if not isinstance(value, dict) or set(value) != {'environment', 'jobId', 'owner', 'runDir'}:
        raise ValueError('Invalid shared task fields')
    if value['environment'] not in ENVIRONMENTS or str(uuid.UUID(value['jobId'])) != value['jobId']:
        raise ValueError('Invalid shared task identity')
    if not isinstance(value['owner'], str) or not re.fullmatch('[a-f0-9]{64}', value['owner']):
        raise ValueError('Missing authenticated task owner')
    run = value['runDir']
    if not isinstance(run, str) or not re.fullmatch('/[A-Za-z0-9_./-]+', run) or '..' in run or '//' in run:
        raise ValueError('Invalid task directory')
    expected = tuple(base + '/' + value['environment'] + '/' for base in RUN_BASES)
    if not run.startswith(expected) or not run.endswith('/' + value['jobId']):
        raise ValueError('Task directory is outside its platform namespace')
    return hashlib.sha256((value['environment'] + '\n' + value['jobId']).encode()).hexdigest()


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
                if not isinstance(row, dict) or key != validate_ticket({name: row.get(name) for name in ('environment', 'jobId', 'owner', 'runDir')}):
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

    def release(self, ticket, acknowledgement):
        with self.locked() as state:
            requested = validate_ticket(ticket)
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
                    if not re.fullmatch('[a-f0-9]{64}', acknowledgement.get('ptSha256', '')) or type(acknowledgement.get('size')) is not int or acknowledgement['size'] < 1:
                        raise ValueError('Verified PT archive acknowledgement missing')
                    result = read_json(root / 'result.json')
                    if result.get('state') != 'SUCCEEDED' or result.get('sha256') != acknowledgement.get('ptSha256') or result.get('size') != acknowledgement.get('size'):
                        raise ValueError('Successful task archive acknowledgement does not match its result')
                    if not re.fullmatch('[a-f0-9]{64}', acknowledgement.get('classesSha256', '')):
                        raise ValueError('Verified classes archive acknowledgement missing')
                elif acknowledgement.get('state') in ('FAILED', 'CANCELLED'):
                    result = read_json(root / 'result.json')
                    if state['active'] == key and result.get('state') != acknowledgement['state']:
                        raise ValueError('Active task has no matching terminal result; lease retained')
                    if result and result.get('state') != acknowledgement['state']:
                        raise ValueError('Task terminal result conflicts; lease retained')
                else:
                    raise ValueError('Unsupported shared task acknowledgement')
                row.update(state='DONE', acknowledgement=dict(acknowledgement))
                if state['active'] == key:
                    state['active'] = None
                self.persist(state)
                return self.view(state, key)


def context(config, run_dir):
    shared = config.get('sharedQueue')
    if not isinstance(shared, dict) or shared.get('mode') != MODE:
        raise ValueError('Missing company shared queue configuration')
    gpu = {'gpuIndex': config['gpuIndex'], 'gpuUuid': shared['gpuUuid']}
    ticket = dict(environment=shared['environment'], jobId=shared['jobId'], owner=shared['owner'], runDir=str(run_dir))
    validate_gpu(gpu); validate_ticket(ticket)
    return Registry(gpu), ticket


def main():
    if str(Path.home()) != str(HOME) or os.geteuid() != HOME.stat().st_uid:
        raise ValueError('Shared queue requires the approved existing oort account')
    action, encoded = sys.argv[1:3]
    payload = json.loads(base64.b64decode(encoded))
    gpu = payload['gpu']
    registry = Registry(gpu)
    if action == 'register':
        verify_physical_gpu(gpu)
        value = registry.register(payload['tickets'])
    elif action == 'inspect':
        value = registry.inspect(payload.get('ticket'))
    elif action == 'release':
        value = registry.release(payload['ticket'], payload['acknowledgement'])
    else:
        raise ValueError('Unknown shared queue action')
    print('VLS_SHARED=' + json.dumps(value, ensure_ascii=False))


if __name__ == '__main__':
    main()
