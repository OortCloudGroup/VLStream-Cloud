#!/usr/bin/env bash
# Run on the user's new GPU instance from a checked-out VLStream release.
set -euo pipefail
script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd -- "$script_dir/../.." && pwd)"
resources="$repo_root/VLStream-Cloud-Backend-Server/vls-stream/ruoyi-vlstream/src/main/resources/training"
profile=standard
work_dir=/root/autodl-tmp/vlstream
conda_bin=/root/miniconda3/bin/conda
gpu=0
dry_run=false
while (($#)); do
    case "$1" in
        --profile) profile="${2:?missing profile}"; shift 2 ;;
        --work-dir) work_dir="${2:?missing directory}"; shift 2 ;;
        --conda) conda_bin="${2:?missing conda path}"; shift 2 ;;
        --gpu) gpu="${2:?missing GPU index}"; shift 2 ;;
        --dry-run) dry_run=true; shift ;;
        --help) printf '%s\n' 'Usage: bash tools/compute/bootstrap-autodl.sh --profile standard|blackwell [--work-dir /absolute/path] [--conda /path/to/conda] [--gpu 0] [--dry-run]'; exit 0 ;;
        *) echo "Unknown option: $1" >&2; exit 2 ;;
    esac
done
case "$profile" in
    standard) torch=2.5.1; vision=0.20.1; cuda=cu124 ;;
    blackwell) torch=2.7.1; vision=0.22.1; cuda=cu128 ;;
    *) echo 'Choose standard or blackwell; do not guess a CUDA combination.' >&2; exit 2 ;;
esac
[[ "$work_dir" =~ ^/[A-Za-z0-9_./-]+$ && "$work_dir" != / && "$work_dir" != *..* && "$work_dir" != *//* ]] || { echo 'Use a dedicated absolute work directory.' >&2; exit 2; }
[[ "$gpu" =~ ^[0-9]+$ ]] || { echo 'GPU index must be numeric.' >&2; exit 2; }
prefix="$work_dir/envs/vls-$profile"
printf 'Profile: %s | Python 3.10 | torch %s | torchvision %s | %s | ultralytics 8.3.240\nPython path: %s/bin/python\nWork directory: %s\n' "$profile" "$torch" "$vision" "$cuda" "$prefix" "$work_dir"
if "$dry_run"; then exit 0; fi
[[ "$(uname -s)" == Linux && "$(uname -m)" == x86_64 ]] || { echo 'Linux x86_64 required.' >&2; exit 2; }
[[ -x "$conda_bin" && -f "$resources/check_cloud_environment.py" ]] || { echo 'Conda or repository resources missing.' >&2; exit 2; }
for program in nvidia-smi nohup flock; do command -v "$program" >/dev/null || { echo "Missing $program" >&2; exit 2; }; done
exec 8>"/tmp/.vlstream-gpu-$gpu.lock"
flock -n 8 || { echo 'GPU is reserved by a platform task; initialize after it finishes.' >&2; exit 2; }
busy="$(nvidia-smi -i "$gpu" --query-compute-apps=pid --format=csv,noheader,nounits)"
[[ -z "${busy//[[:space:]]/}" ]] || { echo 'GPU busy; wait until existing computation has finished.' >&2; exit 2; }
mkdir -p "$work_dir"
exec 9>"$work_dir/.vls-bootstrap.lock"
flock -n 9 || { echo 'Another initialization is running.' >&2; exit 2; }
marker="$work_dir/.vls-env-$profile"
if [[ -e "$prefix" ]]; then
    [[ -f "$marker" && "$(cat "$marker")" == "$prefix" ]] || { echo 'Existing directory is not managed by this initializer; choose another work directory.' >&2; exit 2; }
else
    printf '%s' "$prefix" > "$marker"
    "$conda_bin" create -y --prefix "$prefix" python=3.10 pip
fi
[[ -x "$prefix/bin/python" ]] || { echo 'Environment creation incomplete; inspect Conda errors and choose a new work directory if needed.' >&2; exit 2; }
mkdir -p "$work_dir/models" "$work_dir/config/ultralytics"
export TORCH_HOME="$work_dir/models/torch"
export YOLO_CONFIG_DIR="$work_dir/config/ultralytics"
# Constraints preserve the torch/CUDA pair when the training tools resolve dependencies.
constraints="$work_dir/.vls-constraints-$profile.txt"
printf 'torch==%s\ntorchvision==%s\nnumpy==1.26.4\nopencv-python==4.11.0.86\nultralytics==8.3.240\n' "$torch" "$vision" > "$constraints"
# Fetch only the CUDA-specific wheels here. Resolve their dependencies in the
# next command using the instance's configured PyPI index instead of forcing
# every large NVIDIA library through the PyTorch wheel host.
"$prefix/bin/python" -m pip install --no-deps "torch==$torch+$cuda" "torchvision==$vision+$cuda" --index-url "https://download.pytorch.org/whl/$cuda"
"$prefix/bin/python" -m pip install --constraint "$constraints" "ultralytics==8.3.240" "numpy==1.26.4" "opencv-python==4.11.0.86"
"$prefix/bin/python" -m pip check
"$prefix/bin/python" "$script_dir/prepare-presets.py" --work-dir "$work_dir"
"$prefix/bin/python" "$resources/check_cloud_environment.py" --work-dir "$work_dir" --gpu "$gpu" | tee "$work_dir/environment-check.jsonl"
"$prefix/bin/python" - "$work_dir/environment-check.jsonl" <<'PY'
import json, sys
rows = [line.split('VLS_PROBE=', 1)[1] for line in open(sys.argv[1]) if line.startswith('VLS_PROBE=')]
if not rows or not json.loads(rows[-1]).get('ready'):
    raise SystemExit('Environment check did not pass; inspect the report before connecting this instance.')
PY
"$prefix/bin/python" -m pip freeze > "$work_dir/requirements-resolved-$profile.txt"
printf '\nInitialization complete. Enter Python: %s/bin/python\nEnter work directory: %s\nThen run the platform environment check and a small training task.\n' "$prefix" "$work_dir"
