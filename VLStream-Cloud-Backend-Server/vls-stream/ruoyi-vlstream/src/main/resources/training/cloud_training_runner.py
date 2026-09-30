"""AutoDL process runner: durable files, exact process identity, no Docker or listening port."""
import fcntl
import hashlib
import json
import os
from pathlib import Path
import signal
import subprocess
import sys
import time
import uuid


def write_json(path, value):
    path = Path(path)
    temp = path.with_name(path.name + '.' + uuid.uuid4().hex + '.tmp')
    temp.write_text(json.dumps(value, ensure_ascii=False), encoding='utf-8')
    os.replace(temp, path)


def read_json(path):
    try:
        return json.loads(Path(path).read_text(encoding='utf-8'))
    except FileNotFoundError:
        return {}


def identity(pid):
    try:
        # Linux field 22; account for spaces in the parenthesized process name.
        fields = Path('/proc/{}/stat'.format(pid)).read_text().rsplit(')', 1)[1].split()
        return None if fields[0] == 'Z' else fields[19]
    except (OSError, IndexError):
        return None


def alive(pid, start):
    return bool(pid and start and identity(pid) == start)


def sha256(path):
    digest = hashlib.sha256()
    with Path(path).open('rb') as source:
        for block in iter(lambda: source.read(1024 * 1024), b''):
            digest.update(block)
    return digest.hexdigest()


def status(root):
    result = read_json(root / 'result.json')
    state = read_json(root / 'state.json')
    if not result:
        if alive(state.get('childPid'), state.get('childStart')) or alive(state.get('supervisorPid'), state.get('supervisorStart')):
            result = dict(state='RUNNING', message='实例训练中')
        elif state.get('state') == 'WAITING':
            result = dict(state='WAITING', message=state.get('message'))
        elif state:
            result = dict(state='FAILED', message='训练进程已中断，未生成成功结果；请查看日志后重新训练')
        else:
            result = dict(state='NOT_STARTED', message='准备实例训练环境')
    progress = read_json(root / 'progress.json')
    result.update(epoch=progress.get('epoch', 0), progress=progress.get('progress', 0))
    try:
        with (root / 'training.log').open('rb') as source:
            source.seek(0, 2)
            source.seek(max(0, source.tell() - 32768))
            result['logs'] = source.read().decode('utf-8', errors='replace')
    except FileNotFoundError:
        result['logs'] = ''
    return result


def terminate_owned(state):
    pid, start = state.get('childPid'), state.get('childStart')
    if not alive(pid, start):
        return
    # Only the new session created for this job is eligible for a group signal.
    if os.getpgid(pid) != pid:
        raise RuntimeError('训练进程组身份不匹配，未执行停止')
    os.killpg(pid, signal.SIGTERM)
    deadline = time.monotonic() + 15
    while alive(pid, start) and time.monotonic() < deadline:
        time.sleep(0.2)
    if alive(pid, start):
        os.killpg(pid, signal.SIGKILL)


def cancel(root):
    if read_json(root / 'result.json'):
        return
    (root / 'cancel.request').touch()
    state = read_json(root / 'state.json')
    if not alive(state.get('supervisorPid'), state.get('supervisorStart')):
        terminate_owned(state)
        if not alive(state.get('childPid'), state.get('childStart')):
            write_json(root / 'result.json', dict(state='CANCELLED', message='训练已停止'))


def supervise(config_path, config):
    root = config_path.parent
    with (root / 'run.lock').open('a') as job_lock:
        try:
            fcntl.flock(job_lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError:
            return
        current = status(root)
        if current['state'] not in ('NOT_STARTED', 'WAITING'):
            return
        if (root / 'cancel.request').exists():
            write_json(root / 'result.json', dict(state='CANCELLED', message='训练已停止'))
            return
        lock_path = Path('/tmp') / ('.vlstream-gpu-{}.lock'.format(config['gpuIndex']))
        with lock_path.open('a') as gpu_lock:
            try:
                fcntl.flock(gpu_lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
                occupied = subprocess.run(['nvidia-smi', '-i', str(config['gpuIndex']), '--query-compute-apps=pid', '--format=csv,noheader,nounits'], check=True, capture_output=True, text=True, timeout=20).stdout.strip()
                if occupied:
                    raise BlockingIOError()
            except BlockingIOError:
                write_json(root / 'state.json', dict(state='WAITING', message='GPU 有其他计算任务，等待空闲'))
                return
            except Exception:
                write_json(root / 'result.json', dict(state='FAILED', message='GPU 状态读取失败，请检查实例环境'))
                return
            state = dict(state='RUNNING', supervisorPid=os.getpid(), supervisorStart=identity(os.getpid()))
            write_json(root / 'state.json', state)
            with (root / 'training.log').open('ab', buffering=0) as log:
                child = subprocess.Popen([sys.executable, '-u', str(Path(__file__).resolve()), 'train', str(config_path)], cwd=str(root), stdout=log, stderr=subprocess.STDOUT, start_new_session=True)
                state.update(childPid=child.pid, childStart=identity(child.pid))
                write_json(root / 'state.json', state)
                while child.poll() is None:
                    if (root / 'cancel.request').exists():
                        terminate_owned(state)
                        child.wait()
                        write_json(root / 'result.json', dict(state='CANCELLED', message='训练已停止'))
                        return
                    time.sleep(1)
                if not (root / 'result.json').exists():
                    write_json(root / 'result.json', dict(state='FAILED', message='训练进程异常退出：{}'.format(child.returncode)))


def train(config_path, config):
    root = config_path.parent
    try:
        os.environ['VLS_MODEL_CACHE'] = str(Path(config['workDir']) / 'models')
        os.environ['TORCH_HOME'] = str(Path(config['workDir']) / 'models/torch')
        os.environ['YOLO_CONFIG_DIR'] = str(Path(config['workDir']) / 'config/ultralytics')
        # Expose only the selected GPU so all four training implementations address the same device.
        os.environ['CUDA_VISIBLE_DEVICES'] = str(config['gpuIndex'])
        import torch
        import ultralytics
        from four_task_runtime import train as run_training
        if ultralytics.__version__ != '8.3.240' or not torch.cuda.is_available():
            raise ValueError('需要可用 CUDA 及 Ultralytics 8.3.240')
        dataset = str(root / 'dataset/dataset.yaml')
        kind, model = config['annotationType'], config['model']
        if model.startswith('@preset/'):
            from check_cloud_environment import cached_weights
            cached_weights(Path(config['workDir']))
        if kind == 'object_detection':
            from validate_detection_dataset import validate
            validate(dataset, model)
        else:
            from validate_typed_dataset import validate
            validate(dataset, model, kind, str(config['datasetId']))

        def progress(_stage, epoch, total):
            write_json(root / 'progress.json', dict(epoch=epoch, progress=min(98, int(epoch * 98 / max(1, total)))))

        output = root / 'output'
        model_path = Path(run_training(kind, model, dataset, str(output), config['epochs'], config['batchSize'], config['imgSize'], 0, '0', progress)).resolve()
        expected = (output / 'weights/best.pt').resolve()
        if model_path != expected or not model_path.is_file() or not model_path.stat().st_size:
            raise ValueError('本轮 PT 产物不存在或路径不匹配')
        write_json(root / 'result.json', dict(state='SUCCEEDED', message='训练完成，等待模型回存', sha256=sha256(model_path), size=model_path.stat().st_size))
        print('Training complete', flush=True)
    except Exception as error:
        import traceback
        traceback.print_exc()
        write_json(root / 'result.json', dict(state='FAILED', message=str(error)[:400]))


def main():
    action, config_path = sys.argv[1], Path(sys.argv[2]).resolve()
    root = config_path.parent
    if action == 'status':
        print('VLS_CLOUD=' + json.dumps(status(root), ensure_ascii=False))
    elif action == 'cancel':
        cancel(root)
        print('VLS_CLOUD=' + json.dumps(status(root), ensure_ascii=False))
    elif action in ('run', 'train'):
        config = read_json(config_path)
        (supervise if action == 'run' else train)(config_path, config)
    else:
        raise ValueError('未知操作')


if __name__ == '__main__':
    main()
