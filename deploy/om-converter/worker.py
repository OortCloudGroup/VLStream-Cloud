"""Private-volume Hi3519DV500 worker. No HTTP listener, GPU, database or MinIO credentials."""
import fcntl
import hashlib
import json
import os
import re
from pathlib import Path
import subprocess
import time
import uuid

ROOT = Path(os.environ.get('OM_WORK_DIR', '/work'))
TOOLKIT = Path(os.environ.get('SVP_ROOT', '/opt/svp'))


def atomic_json(path, value):
    stage = path.with_suffix('.tmp')
    stage.write_text(json.dumps(value, ensure_ascii=False), encoding='utf-8')
    stage.replace(path)


def digest(path):
    h = hashlib.sha256()
    with path.open('rb') as f:
        for block in iter(lambda: f.read(1024 * 1024), b''):
            h.update(block)
    return h.hexdigest()


def regular(path, root):
    if path.is_symlink() or not path.is_file() or not path.resolve().is_relative_to(root.resolve()):
        raise ValueError('Invalid conversion input file')
    return path


def failure_result(folder, error):
    message = str(error)
    if isinstance(error, subprocess.TimeoutExpired):
        message = '模型转换超时'
    elif isinstance(error, subprocess.CalledProcessError):
        message = 'ONNX导出失败' if error.cmd[0] == 'python' else 'Hi3519DV500 ATC编译失败'
    result = {'state': 'FAILED', 'id': folder.name, 'message': message[:500]}
    log = folder / 'conversion.log'
    if log.is_file():
        with log.open('rb') as f:
            f.seek(max(0, log.stat().st_size - 3500))
            detail = re.sub(r'\x1b\[[0-9;]*m', '', f.read().decode('utf-8', errors='replace'))
        result['detail'] = detail
        reasons = [line.strip() for line in detail.splitlines() if re.search(r'error|failed|invalid|unsupported|mismatch', line, re.I)]
        if reasons:
            result['message'] = (message + '：' + reasons[-1][-320:])[:450]
    return result


def convert(folder):
    request = json.loads(regular(folder / 'request.json', folder).read_text())
    if request.get('protocol') != 1 or request.get('id') != folder.name or request.get('soc') != 'Hi3519DV500':
        raise ValueError('Unsupported conversion request')
    for name in ('model.pt', 'classes.yaml'):
        if digest(regular(folder / name, folder)) != request['sha256'][name]:
            raise ValueError('Conversion input checksum mismatch: ' + name)
    calibration = []
    for name, expected in request['calibration'].items():
        if Path(name).name != name:
            raise ValueError('Invalid calibration name')
        path = regular(folder / 'calibration' / name, folder)
        if digest(path) != expected:
            raise ValueError('Calibration checksum mismatch')
        calibration.append(str(path))
    if not 1 <= len(calibration) <= 20:
        raise ValueError('Expected 1-20 training calibration images')
    (folder / 'images.txt').write_text('\n'.join(calibration) + '\n')
    env_script = TOOLKIT / 'Ascend/ascend-toolkit/svp_latest/x86_64-linux/script/setenv.sh'
    config = TOOLKIT / 'V3.0.0.12/sample/yolo/insert_op.cfg'
    if not env_script.is_file() or not config.is_file():
        raise RuntimeError('Hi3519DV500 SVP toolkit is not installed')
    with (folder / 'conversion.log').open('w') as log:
        subprocess.run(['python', '/opt/converter/export_hisilicon_yolov8.py', '--model', str(folder / 'model.pt'),
                        '--output', str(folder / 'model.onnx'), '--imgsz', '640', '--classes', str(folder / 'classes.yaml'), '--safe-load'],
                       cwd=folder, stdout=log, stderr=subprocess.STDOUT, check=True, timeout=900)
        command = ['atc', '--dump_data=0', '--input_shape=images:1,3,640,640', '--input_type=images:UINT8',
                   '--log_level=0', '--online_model_type=0', '--batch_num=1', '--input_format=NCHW',
                   '--output=' + str(folder / 'model'), '--soc_version=Hi3519DV500', '--insert_op_conf=' + str(config),
                   '--framework=5', '--compile_mode=0', '--save_original_model=true', '--model=' + str(folder / 'model.onnx'),
                   '--image_list=images:' + str(folder / 'images.txt')]
        subprocess.run(['bash', '-c', 'source "$1" >/dev/null; shift; exec "$@"', 'svp', str(env_script)] + command,
                       cwd=folder, stdout=log, stderr=subprocess.STDOUT, check=True, timeout=1800)
    output = regular(folder / 'model_original.om', folder)
    if output.stat().st_size < 1024:
        raise ValueError('ATC output is empty or invalid')
    return {'state': 'COMPLETED', 'id': folder.name, 'soc': 'Hi3519DV500', 'size': output.stat().st_size, 'sha256': digest(output)}


def main():
    ROOT.mkdir(parents=True, exist_ok=True)
    jobs = ROOT / 'jobs'
    jobs.mkdir(exist_ok=True)
    # All replicas share this volume. A crash releases the lock; requests/results remain durable.
    with (ROOT / 'worker.lock').open('a') as lock:
        fcntl.flock(lock, fcntl.LOCK_EX)
        while True:
            atomic_json(ROOT / 'heartbeat.json', {'time': time.time(), 'soc': 'Hi3519DV500'})
            for folder in sorted(jobs.iterdir(), key=lambda p: p.stat().st_mtime):
                try:
                    if str(uuid.UUID(folder.name)) != folder.name or folder.is_symlink():
                        continue
                except ValueError:
                    continue
                if not (folder / 'request.json').is_file() or (folder / 'result.json').exists():
                    continue
                atomic_json(folder / 'state.json', {'state': 'CONVERTING', 'time': time.time()})
                try:
                    result = convert(folder)
                except Exception as error:
                    result = failure_result(folder, error)
                atomic_json(folder / 'result.json', result)
            time.sleep(3)


if __name__ == '__main__':
    main()
