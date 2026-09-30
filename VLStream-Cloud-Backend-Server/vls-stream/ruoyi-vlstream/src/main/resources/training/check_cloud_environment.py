"""Standalone new-node check. No dependency on the development GPU, no package installation."""
import argparse
import hashlib
import importlib
import json
import os
from pathlib import Path
import platform
import shutil
import subprocess
import sys

PROFILES = {
    'standard': {'torch': '2.5.1', 'torchvision': '0.20.1', 'cuda': '12.4'},
    'blackwell': {'torch': '2.7.1', 'torchvision': '0.22.1', 'cuda': '12.8'},
}
PRESETS = {'detect': 'yolov8m.pt', 'classify': 'yolov8n-cls.pt', 'segment': 'yolov8n-seg.pt'}


def checksum(path):
    value = hashlib.sha256()
    with Path(path).open('rb') as source:
        for block in iter(lambda: source.read(1024 * 1024), b''):
            value.update(block)
    return value.hexdigest()


def cached_weights(root):
    manifest_path = root / 'models/presets.json'
    value = json.loads(manifest_path.read_text(encoding='utf-8'))
    expected = list(PRESETS.values()) + ['torch/hub/checkpoints/deeplabv3_resnet50_coco-cd0a2569.pth']
    if value.get('format') != 1 or value.get('ultralytics') != '8.3.240':
        raise ValueError('基础模型缓存版本无效，请重新执行初始化')
    for name in expected:
        path = root / 'models' / name
        if not path.is_file() or path.is_symlink() or path.stat().st_size < 1:
            raise ValueError('缺少预下载基础权重：' + name)
        if value.get('files', {}).get(name) != checksum(path):
            raise ValueError('基础权重校验失败：' + name)
    return expected


def inspect_environment(work_dir, gpu_index=0):
    root = Path(work_dir)
    result = dict(ready=False, contractVersion=1, python=platform.python_version(), checks=[], warnings=[])

    def check(name, action):
        try:
            action()
            result['checks'].append(dict(name=name, passed=True, message='通过'))
            return True
        except Exception as error:
            result['checks'].append(dict(name=name, passed=False, message=str(error)[:300]))
            return False

    def runtime():
        if platform.system() != 'Linux' or platform.machine() not in ('x86_64', 'AMD64'):
            raise ValueError('当前标准初始化配置要求 Linux x86_64')
        if sys.version_info[:2] != (3, 10):
            raise ValueError('请使用初始化脚本创建的 Python 3.10 环境')
        for program in ('nvidia-smi', 'nohup'):
            if shutil.which(program) is None:
                raise ValueError('缺少命令：' + program)

    def directory():
        if not root.is_absolute() or not root.is_dir() or not os.access(root, os.W_OK):
            raise ValueError('工作目录不存在或不可写，请先执行初始化脚本')
        free = shutil.disk_usage(root).free / 1024 ** 3
        result['diskFreeGb'] = round(free, 2)
        if free < 5:
            raise ValueError('可用磁盘不足 5 GiB；还需为实际数据集和训练输出预留空间')

    check('系统与 Python', runtime)
    check('工作目录与空间', directory)
    os.environ['TORCH_HOME'] = str(root / 'models/torch')
    os.environ['YOLO_CONFIG_DIR'] = str(root / 'config/ultralytics')
    modules = {}

    def dependencies():
        for name in ('torch', 'torchvision', 'ultralytics', 'numpy', 'PIL', 'yaml', 'cv2', 'scipy'):
            modules[name] = importlib.import_module(name)
        for name in ('torch', 'torchvision', 'ultralytics'):
            result[name] = modules[name].__version__
        result['cudaVersion'] = modules['torch'].version.cuda
        profile = next((name for name, versions in PROFILES.items()
                        if all(result[key].split('+')[0] == versions[key] for key in ('torch', 'torchvision'))
                        and result['cudaVersion'] == versions['cuda']), None)
        if profile is None or result['ultralytics'] != '8.3.240':
            raise ValueError('版本组合不在标准配置内，请按接入指南重新执行初始化')
        result['profile'] = profile

    dependencies_ok = check('训练依赖与版本组合', dependencies)
    if dependencies_ok:
        def gpu():
            torch = modules['torch']
            if not torch.cuda.is_available() or gpu_index < 0 or gpu_index >= torch.cuda.device_count():
                raise ValueError('未发现所选 GPU，请确认有卡开机及 CUDA 版 PyTorch')
            result['gpu'] = torch.cuda.get_device_name(gpu_index)
            result['capability'] = list(torch.cuda.get_device_capability(gpu_index))
            busy = subprocess.run(['nvidia-smi', '-i', str(gpu_index), '--query-compute-apps=pid', '--format=csv,noheader,nounits'], capture_output=True, text=True, timeout=15, check=True).stdout.strip()
            if busy:
                raise ValueError('GPU 正被计算任务使用，请空闲后进行运算检查')
            device = 'cuda:' + str(gpu_index)
            with torch.cuda.device(gpu_index):
                network = torch.nn.Conv2d(3, 4, 3).to(device)
                value = torch.ones((1, 3, 16, 16), device=device, requires_grad=True)
                network(value).square().mean().backward()
                boxes = torch.tensor([[0., 0., 10., 10.], [1., 1., 9., 9.]], device=device)
                scores = torch.tensor([0.9, 0.8], device=device)
                modules['torchvision'].ops.nms(boxes, scores, 0.5)
                torch.cuda.synchronize()
            result['cuda'] = True
        check('GPU 前后向运算与 Torchvision 算子', gpu)
        if check('四类基础权重完整性', lambda: cached_weights(root)):
            def models():
                for task, filename in PRESETS.items():
                    model = modules['ultralytics'].YOLO(str(root / 'models' / filename))
                    if model.task != task:
                        raise ValueError('基础权重任务类型不匹配：' + filename)
                    del model
                segmentation = modules['torchvision'].models.segmentation
                model = segmentation.deeplabv3_resnet50(weights=segmentation.DeepLabV3_ResNet50_Weights.COCO_WITH_VOC_LABELS_V1)
                del model
            check('四类基础模型加载', models)
    result['ready'] = bool(result['checks']) and all(item['passed'] for item in result['checks'])
    result['message'] = '环境检查通过；真实四类训练仍需小样本验收' if result['ready'] else '；'.join(item['message'] for item in result['checks'] if not item['passed'])[:450]
    return result


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--work-dir', required=True)
    parser.add_argument('--gpu', type=int, default=0)
    args = parser.parse_args()
    print('VLS_PROBE=' + json.dumps(inspect_environment(args.work_dir, args.gpu), ensure_ascii=False))


if __name__ == '__main__':
    main()
