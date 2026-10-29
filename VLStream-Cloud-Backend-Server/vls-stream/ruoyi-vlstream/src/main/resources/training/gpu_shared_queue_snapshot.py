"""Read an atomic shared FIFO snapshot. No registry creation, locks or task operations."""
import base64
import hashlib
import json
import os
from pathlib import Path
import re
import stat
import sys
import time
import uuid

ROOT = Path('/home/oort/.local/state/vlstream-shared/v1')


def read_json_file(path):
    path = Path(path)
    if path.resolve() != path:
        raise ValueError('Unsafe metadata path')
    descriptor = os.open(str(path), os.O_RDONLY | os.O_NOFOLLOW)
    with os.fdopen(descriptor, 'r', encoding='utf-8') as source:
        info = os.fstat(source.fileno())
        if not stat.S_ISREG(info.st_mode) or info.st_uid != os.geteuid() or info.st_mode & 0o022 or info.st_size > 16 * 1024 * 1024:
            raise ValueError('Unsafe metadata file')
        text = source.read(16 * 1024 * 1024 + 1)
        if len(text) > 16 * 1024 * 1024:
            raise ValueError('Oversized metadata')
        return json.loads(text)


def workload_profile(row, gpu=None):
    """Hash exact frozen workload metadata; never return dataset/model paths or IDs."""
    try:
        run = row.get('runDir')
        bases = ('/data/work/vlstream-gpu/', '/home/oort/vlstream/')
        if not isinstance(run, str) or not re.fullmatch('/[A-Za-z0-9_./-]+', run) or '..' in run or '//' in run:
            return None
        if not any(run.startswith(base + row['environment'] + '/') for base in bases) or not run.endswith('/' + row['jobId']):
            return None
        config = read_json_file(Path(run) / 'job.json')
        shared = config.get('sharedQueue', {})
        if shared.get('jobId') != row['jobId'] or shared.get('environment') != row['environment'] or shared.get('owner') != row['owner'] or shared.get('mode') != 'SHARED_GPU_QUEUE':
            return None
        if gpu is not None and (shared.get('gpuUuid') != gpu['gpuUuid'] or shared.get('gpuIndex') != gpu['gpuIndex'] or config.get('gpuIndex') != gpu['gpuIndex']):
            return None
        # Only stable official presets and immutable dataset artifacts can be matched.
        model, artifact = config.get('model'), config.get('datasetArtifactRef')
        if not isinstance(model, str) or not re.fullmatch('@preset/[A-Za-z0-9_.-]+', model):
            return None
        if not isinstance(artifact, str) or not re.fullmatch('vls-dataset://[a-f0-9-]{36}', artifact):
            return None
        if config.get('annotationType') not in ('object_detection', 'image_classification', 'instance_segmentation', 'semantic_segmentation'):
            return None
        for field, maximum in (('epochs', 10000), ('batchSize', 8192), ('imgSize', 8192)):
            if type(config.get(field)) is not int or not 0 < config[field] <= maximum:
                return None
        runtime = shared.get('runtime')
        fields = ('python', 'torch', 'torchvision', 'cudaVersion', 'ultralytics')
        if not isinstance(runtime, dict) or any(not isinstance(runtime.get(key), str) or not runtime[key] for key in fields):
            return None
        values = {key: config[key] for key in ('model', 'datasetArtifactRef', 'annotationType', 'epochs', 'batchSize', 'imgSize')}
        values['runtime'] = {key: runtime[key] for key in fields}
        return hashlib.sha256(json.dumps(values, sort_keys=True, separators=(',', ':')).encode()).hexdigest()
    except (OSError, ValueError, TypeError, KeyError):
        return None


def read_snapshot(gpu, environment, viewer, root=ROOT, previous_ticket_key=None):
    if not re.fullmatch('GPU-[a-fA-F0-9-]{36}', gpu.get('gpuUuid', '')) or type(gpu.get('gpuIndex')) is not int or not 0 <= gpu['gpuIndex'] <= 15:
        raise ValueError('Invalid physical GPU')
    if environment not in ('localhost', 'vm') or not re.fullmatch('[a-f0-9]{64}', viewer):
        raise ValueError('Missing authenticated viewer')
    path = Path(root) / gpu['gpuUuid'] / 'registry.json'
    # Registry does atomic replaces. O_RDONLY + O_NOFOLLOW reads one complete generation,
    # without creating directories, acquiring the task lock, or uploading a script.
    state = read_json_file(path)
    return summarize(state, gpu, environment, viewer, previous_ticket_key)


def summarize(state, gpu, environment, viewer, previous_ticket_key=None):
    if state.get('version') != 1 or state.get('gpu') != gpu or not isinstance(state.get('tickets'), dict) or len(state['tickets']) > 50000:
        raise ValueError('Invalid registry identity')
    sequence = state.get('sequence')
    if type(sequence) is not int or sequence < 0:
        raise ValueError('Invalid sequence')
    active, seen, waiting = state.get('active'), set(), 0
    for key, row in state['tickets'].items():
        if not isinstance(row, dict):
            raise ValueError('Invalid ticket')
        env, job, owner = row.get('environment'), row.get('jobId'), row.get('owner')
        if env not in ('localhost', 'vm') or not isinstance(job, str) or str(uuid.UUID(job)) != job or not isinstance(owner, str) or not re.fullmatch('[a-f0-9]{64}', owner):
            raise ValueError('Invalid ticket identity')
        if key != hashlib.sha256((env + '\n' + job).encode()).hexdigest():
            raise ValueError('Invalid ticket key')
        order = row.get('sequence')
        phase = row.get('state')
        if type(order) is not int or not 0 < order <= sequence or order in seen or phase not in ('WAITING', 'RUNNING', 'ARCHIVING', 'DONE'):
            raise ValueError('Invalid ticket state')
        seen.add(order)
        if phase in ('RUNNING', 'ARCHIVING') and key != active:
            raise ValueError('Invalid active lease')
        waiting += phase == 'WAITING'
    row = state['tickets'].get(active) if active else None
    if active is not None and (row is None or row['state'] not in ('RUNNING', 'ARCHIVING')):
        raise ValueError('Invalid active task')
    own = bool(row and row['environment'] == environment and row['owner'] == viewer)
    previous = state['tickets'].get(previous_ticket_key, {}) if isinstance(previous_ticket_key, str) else {}
    acknowledgement = previous.get('acknowledgement', {})
    archived = previous.get('state') == 'DONE' and acknowledgement.get('state') == 'ARCHIVED' and all(
        re.fullmatch('[a-f0-9]{64}', acknowledgement.get(field, '')) for field in ('ptSha256', 'classesSha256')) and type(acknowledgement.get('size')) is int and acknowledgement['size'] > 0
    waiting_rows = [value for value in state['tickets'].values() if value['state'] == 'WAITING']
    return dict(state='KNOWN', waitingCount=waiting, activePhase=row['state'] if row else None,
                activeJobId=row['jobId'] if own else None, checkedAt=int(time.time() * 1000), gpuUuid=gpu['gpuUuid'],
                activeTicketKey=active, activeProfile=workload_profile(row, gpu) if row else None,
                waitingProfiles=[workload_profile(value, gpu) for value in waiting_rows] if waiting <= 100 else None,
                previousTicketKey=previous_ticket_key, previousOutcome='ARCHIVED' if archived else None)


if __name__ == '__main__':
    payload = json.loads(base64.b64decode(sys.argv[1]))
    print('VLS_QUEUE_SNAPSHOT=' + json.dumps(read_snapshot(payload['gpu'], payload['environment'], payload['viewer'], previous_ticket_key=payload.get('previousTicketKey'))))
