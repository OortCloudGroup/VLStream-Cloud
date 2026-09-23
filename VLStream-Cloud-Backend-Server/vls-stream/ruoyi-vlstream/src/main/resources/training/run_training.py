import argparse
import json
from pathlib import Path
from four_task_runtime import manifest, train, atomic_json

parser = argparse.ArgumentParser()
parser.add_argument('--dataset', required=True)
parser.add_argument('--model', required=True)
parser.add_argument('--output', required=True)
parser.add_argument('--epochs', type=int, default=10)
parser.add_argument('--batch', type=int, default=4)
parser.add_argument('--size', type=int, default=640)
parser.add_argument('--workers', type=int, default=2)
parser.add_argument('--device', default='0')

if __name__ == '__main__':
    args = parser.parse_args()
    _, data = manifest(args.dataset)
    result = train(data['annotationType'], args.model, args.dataset, args.output, args.epochs, args.batch, args.size, args.workers, args.device)
    if not Path(result).is_file(): raise RuntimeError('本轮训练模型文件不存在')
    print('Training complete', flush=True)
