"""Linux cross-process queue protocol simulation, with no GPU/model/archive work.

Run directly with Python and a new --evidence-dir. --verify-physical only reads
nvidia-smi; the filesystem queue and GPU-shaped lock are always test-owned.
Every stdout record states the simulation boundary, including worker records.
"""
import argparse
import fcntl
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import select
import subprocess
import sys
import time
import uuid

GPU = dict(gpuIndex=0, gpuUuid='GPU-24fb011a-dd51-d9e1-ed6d-59f9266869ac')
BOUNDARY = dict(simulation=True, gpuTrainingRan=False, platformArchiveVerified=False,
    lockScope='isolated-test-owned-os-lock', archiveAcknowledgement='synthetic-protocol-fixture')
SOURCE = Path(__file__).resolve().parents[2] / 'main/resources/training/gpu_shared_queue.py'
if not SOURCE.is_file():
    SOURCE = Path(__file__).with_name('gpu_shared_queue.py')


def load_module(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    value = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(value)
    return value


queue = load_module('gpu_shared_queue', SOURCE)
runner = load_module('cloud_training_runner', SOURCE.with_name('cloud_training_runner.py'))


def record(event, **values):
    return dict(BOUNDARY, event=event, utc=time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime()),
        monotonicNs=time.monotonic_ns(), pid=os.getpid(), **values)


def emit(event, **values):
    value = record(event, **values)
    print(json.dumps(value, ensure_ascii=False), flush=True)
    return value


def test_context(path):
    value = json.loads(Path(path).read_text(encoding='utf-8'))
    base = Path(value['evidenceDir'])
    if base.resolve() != base or base.is_symlink() or base.stat().st_uid != os.geteuid():
        raise ValueError('Simulation evidence path is unsafe')
    marker = json.loads((base / '.vls-shared-simulation.json').read_text())
    if marker != dict(BOUNDARY, gpu=GPU):
        raise ValueError('Missing test-owned simulation marker')
    if value['registryRoot'] != str(base / 'registry') or value['gpuLock'] != str(base / 'gpu-0.lock'):
        raise ValueError('Simulation cannot use a production registry or GPU lock')
    queue.RUN_BASES = (str(base / 'jobs'),)
    return value, base, queue.Registry(GPU, base / 'registry')


def change_compute_count(base, amount, environment):
    with runner.lock_file(base / 'metrics.lock') as lock:
        fcntl.flock(lock, fcntl.LOCK_EX)
        path = base / 'metrics.json'
        value = json.loads(path.read_text())
        value['activeCompute'] += amount
        if not 0 <= value['activeCompute'] <= 2:
            raise AssertionError('Invalid simulation counter')
        value['peakCompute'] = max(value['peakCompute'], value['activeCompute'])
        value['steps'].append(dict(environment=environment, amount=amount, monotonicNs=time.monotonic_ns()))
        runner.write_json(path, value)
        return value


def worker(context_path, name):
    value, base, registry = test_context(context_path)
    ticket = value['tickets'][name]
    run = Path(ticket['runDir'])
    job_context = gpu_context = None
    job_lock = gpu_lock = None
    compute = False

    def close_locks():
        nonlocal job_context, gpu_context, job_lock, gpu_lock
        if gpu_context is not None:
            gpu_context.__exit__(None, None, None)
            gpu_context = gpu_lock = None
        if job_context is not None:
            job_context.__exit__(None, None, None)
            job_context = job_lock = None

    emit('worker-ready', worker=name, environment=ticket['environment'])
    try:
        for line in sys.stdin:
            request = json.loads(line)
            action = request['action']
            detail = {}
            if action == 'register':
                detail['registration'] = registry.register([ticket])[0]
            elif action == 'register-c':
                detail['registration'] = registry.register([value['tickets']['C']])[0]
            elif action == 'claim':
                if compute:
                    raise AssertionError('Worker already has a simulation compute step')
                run.mkdir(parents=True, exist_ok=True)
                job_context = runner.lock_file(run / 'run.lock')
                job_lock = job_context.__enter__()
                gpu_context = runner.lock_file(Path(value['gpuLock']))
                gpu_lock = gpu_context.__enter__()
                try:
                    fcntl.flock(job_lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
                    fcntl.flock(gpu_lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
                except BlockingIOError:
                    close_locks()
                    detail.update(claimed=False, blockedBy='physical-test-flock')
                else:
                    if not registry.claim(ticket):
                        detail.update(claimed=False, blockedBy='durable-queue-lease', ticket=registry.inspect(ticket))
                        close_locks()
                    else:
                        compute = True
                        detail.update(claimed=True, metrics=change_compute_count(base, 1, ticket['environment']))
                        # Small CPU-only control step. No tensor/GPU/model code is imported.
                        detail['cpuControlDigest'] = hashlib.sha256((name * 1024).encode()).hexdigest()
            elif action == 'archive':
                if not compute:
                    raise AssertionError('No simulation compute lease to archive')
                # Required protocol shape only: no PT/classes objects are created.
                runner.write_json(run / 'result.json', dict(BOUNDARY, state='SUCCEEDED',
                    sha256='b' * 64, size=12, fixtureOnly=True, modelFileCreated=False))
                registry.archiving(ticket)
                detail.update(ticket=registry.inspect(ticket), metrics=change_compute_count(base, -1, ticket['environment']))
                compute = False
                close_locks()
                detail['physicalTestLockReleased'] = True
            elif action == 'ack':
                try:
                    detail['ticket'] = registry.release(ticket, request['acknowledgement'])
                    detail['accepted'] = True
                except (ValueError, BlockingIOError) as error:
                    detail.update(accepted=False, reason=str(error), ticket=registry.inspect(ticket))
            elif action == 'inspect':
                detail.update(ticket=registry.inspect(ticket), aggregate=registry.inspect(), computeStepHeld=compute)
            elif action == 'stop':
                if compute:
                    change_compute_count(base, -1, ticket['environment'])
                    compute = False
                close_locks()
                emit('worker-stopped', worker=name, environment=ticket['environment'])
                return
            else:
                raise ValueError('Unsupported simulation action')
            emit('worker-action', worker=name, environment=ticket['environment'], action=action, **detail)
    finally:
        close_locks()


class Worker:
    def __init__(self, name, context, timeline):
        self.name, self.timeline = name, timeline
        self.process = subprocess.Popen([sys.executable, '-I', str(Path(__file__).resolve()),
            '--worker', name, '--context', str(context)], stdin=subprocess.PIPE,
            stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, bufsize=1)
        self.receive()

    def receive(self):
        readable, _, _ = select.select([self.process.stdout], [], [], 10)
        if not readable:
            raise AssertionError('Worker response timeout: ' + self.name)
        line = self.process.stdout.readline()
        if not line:
            raise AssertionError('Worker exited: ' + self.name + ' ' + self.process.stderr.read())
        value = json.loads(line)
        if any(value.get(key) != expected for key, expected in BOUNDARY.items()):
            raise AssertionError('Worker did not disclose the simulation boundary')
        self.timeline.append(value)
        print(json.dumps(value), flush=True)
        return value

    def action(self, action, **values):
        self.process.stdin.write(json.dumps(dict(BOUNDARY, action=action, **values)) + '\n')
        self.process.stdin.flush()
        return self.receive()

    def close(self):
        if self.process.poll() is None:
            try:
                self.action('stop')
                self.process.wait(5)
            except Exception:
                # This PID was created by this isolated simulation. No signal is
                # sent to a process group, GPU task, or another worker's PID.
                self.process.terminate()
                self.process.wait(5)
        if self.process.returncode:
            raise AssertionError('Worker failed: ' + self.name + ' ' + self.process.stderr.read())


def simulate(evidence, verify_physical):
    evidence = evidence.absolute()
    if evidence.exists() or evidence.resolve() != evidence:
        raise ValueError('Use a new canonical test-owned --evidence-dir')
    evidence.mkdir(mode=0o700, parents=True)
    (evidence / '.vls-shared-simulation.json').write_text(json.dumps(dict(BOUNDARY, gpu=GPU)))
    timeline, checks, workers = [], [], []
    summary = dict(BOUNDARY, gpu=GPU, evidenceDir=str(evidence), physicalIdentityVerified=False,
        productionRegistryTouched=False, productionGpuLockTouched=False, newModelProduced=False,
        passed=False, checks=checks)

    def note(event, **values):
        entry = emit(event, **values)
        timeline.append(entry)

    def require(condition, name):
        if not condition:
            raise AssertionError(name)
        checks.append(name)
        note('assertion-passed', name=name)

    try:
        if verify_physical:
            queue.verify_physical_gpu(GPU)
            processes = subprocess.run(['nvidia-smi', '-i', '0', '--query-compute-apps=pid',
                '--format=csv,noheader,nounits'], capture_output=True, text=True, timeout=15, check=True).stdout.strip()
            summary.update(physicalIdentityVerified=True, observedGpuProcessCount=len(processes.splitlines()) if processes else 0)
        note('simulation-start', gpu=GPU, physicalIdentityVerified=summary['physicalIdentityVerified'])
        tickets = {}
        for name, environment in (('A', 'localhost'), ('B', 'vm'), ('C', 'localhost')):
            job_id = str(uuid.uuid4())
            owner = hashlib.sha256(('simulation-fixture-owner-' + name).encode()).hexdigest()
            tickets[name] = dict(environment=environment, jobId=job_id, owner=owner,
                runDir=str(evidence / 'jobs' / environment / owner[:16] / job_id))
        context = evidence / 'context.json'
        runner.write_json(context, dict(BOUNDARY, evidenceDir=str(evidence), gpu=GPU,
            registryRoot=str(evidence / 'registry'), gpuLock=str(evidence / 'gpu-0.lock'), tickets=tickets))
        runner.write_json(evidence / 'metrics.json', dict(BOUNDARY, activeCompute=0, peakCompute=0, steps=[]))
        _, _, registry = test_context(context)
        # Preserve the original registry schema, adding evidence-only boundary
        # metadata to this test registry. The production default ROOT is unused.
        with registry.locked() as state:
            state.update(BOUNDARY)
            registry.persist(state)
        a = Worker('A', context, timeline); workers.append(a)
        b = Worker('B', context, timeline); workers.append(b)
        require(a.process.pid != b.process.pid, 'two-independent-worker-processes')
        a.action('register'); b.action('register'); b.action('register-c')
        with registry.locked() as state:
            order = [row['jobId'] for row in sorted(state['tickets'].values(), key=lambda item: item['sequence'])]
        require(order == [tickets[name]['jobId'] for name in ('A', 'B', 'C')], 'cross-process-registration-fifo')
        require(not b.action('claim')['claimed'], 'vm-cannot-overtake-localhost-fifo-head')
        require(a.action('claim')['claimed'], 'localhost-fifo-head-starts-cpu-control-step')
        blocked = b.action('claim')
        require(not blocked['claimed'] and blocked['blockedBy'] == 'physical-test-flock', 'physical-flock-excludes-concurrent-worker')
        registry.release(tickets['C'], dict(BOUNDARY, state='CANCELLED'))
        note('waiting-cancelled', worker='C', acknowledgementFixture=True, aggregate=registry.inspect())
        require(a.process.poll() is None and b.process.poll() is None and
            a.action('inspect')['computeStepHeld'], 'cancel-waiting-c-does-not-kill-a-or-b')
        require(registry.inspect(tickets['C'])['state'] == 'DONE', 'cancelled-c-removed-from-active-fifo')
        require(a.action('archive')['physicalTestLockReleased'], 'a-archiving-releases-physical-test-lock')
        blocked = b.action('claim')
        require(not blocked['claimed'] and blocked['blockedBy'] == 'durable-queue-lease', 'archiving-a-keeps-b-waiting-after-physical-lock-release')
        ack = dict(BOUNDARY, state='ARCHIVED', ptSha256='b' * 64, size=12, classesSha256='c' * 64)
        for name, change in (('pt-digest', dict(ptSha256='d' * 64)), ('pt-size', dict(size=13)), ('missing-classes-digest', dict(classesSha256=''))):
            require(not a.action('ack', acknowledgement=dict(ack, **change))['accepted'], 'mismatched-fixture-ack-rejected-' + name)
            blocked = b.action('claim')
            require(not blocked['claimed'] and blocked['blockedBy'] == 'durable-queue-lease', 'b-still-waits-after-invalid-ack-' + name)
        require(a.action('ack', acknowledgement=ack)['accepted'], 'matching-synthetic-fixture-ack-ends-a-lease')
        require(b.action('claim')['claimed'], 'vm-b-starts-only-after-matching-fixture-ack')
        registry.release(tickets['C'], dict(BOUNDARY, state='CANCELLED'))
        require(a.process.poll() is None and b.process.poll() is None and
            b.action('inspect')['computeStepHeld'], 'repeat-cancellation-does-not-kill-active-b')
        b.action('archive')
        require(b.action('ack', acknowledgement=ack)['accepted'], 'matching-synthetic-fixture-ack-ends-b-lease')
        aggregate = registry.inspect()
        require(aggregate == dict(active=False, waiting=0), 'all-simulation-tickets-terminal-queue-idle')
        with runner.lock_file(evidence / 'gpu-0.lock') as lock:
            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        require(True, 'physical-test-lock-reacquired-after-b-completion')
        metrics = json.loads((evidence / 'metrics.json').read_text())
        require(metrics['peakCompute'] == 1 and metrics['activeCompute'] == 0, 'cross-process-compute-peak-one-and-final-zero')
        require([item['environment'] for item in metrics['steps'] if item['amount'] == 1] == ['localhost', 'vm'], 'cpu-control-steps-start-in-global-fifo-order')
        summary.update(passed=True, aggregate=aggregate, peakCompute=metrics['peakCompute'], metrics=metrics)
    except Exception as error:
        summary['error'] = str(error)
        note('simulation-failed', reason=str(error))
        raise
    finally:
        for current in workers:
            try:
                current.close()
            except Exception as error:
                summary.update(passed=False, workerCloseError=str(error))
        summary['workerPids'] = [current.process.pid for current in workers]
        summary['workerExitCodes'] = [current.process.returncode for current in workers]
        summary['limitations'] = [
            'CPU-only protocol simulation; no GPU operations or training ran.',
            'The real GPU UUID may be verified read-only, but the registry and OS lock are isolated test-owned files.',
            'ARCHIVED acknowledgements and SUCCEEDED result shapes are synthetic fixtures; no PT or classes file was produced.',
            'No VLS submission, database, SSO, MinIO upload/download, or production archive was verified.'
        ]
        summary['durationSeconds'] = round((time.monotonic_ns() - timeline[0]['monotonicNs']) / 1e9, 3) if timeline else None
        runner.write_json(evidence / 'summary.json', summary)
        with (evidence / 'timeline.jsonl').open('x', encoding='utf-8') as output:
            for entry in timeline:
                output.write(json.dumps(entry, ensure_ascii=False) + '\n')
        emit('simulation-summary', summary=summary)
    return summary


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--evidence-dir', type=Path)
    parser.add_argument('--verify-physical', action='store_true')
    parser.add_argument('--worker', choices=('A', 'B'))
    parser.add_argument('--context', type=Path)
    args = parser.parse_args()
    if args.worker:
        worker(args.context, args.worker)
    elif args.evidence_dir:
        if not simulate(args.evidence_dir, args.verify_physical)['passed']:
            raise AssertionError('Simulation did not finish cleanly')
    else:
        parser.error('--evidence-dir is required for an isolated simulation')


if __name__ == '__main__':
    try:
        main()
    except Exception as error:
        emit('simulation-error', reason=str(error))
        sys.exit(1)
