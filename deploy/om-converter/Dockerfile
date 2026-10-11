FROM python:3.10-slim-bookworm
RUN apt-get update && apt-get install -y --no-install-recommends libgl1 libglib2.0-0 libgomp1 && rm -rf /var/lib/apt/lists/*
RUN pip install --no-cache-dir torch==2.5.1+cpu torchvision==0.20.1+cpu --extra-index-url https://download.pytorch.org/whl/cpu
RUN pip install --no-cache-dir ultralytics==8.3.240 onnx==1.17.0 numpy==1.26.4 PyYAML==6.0.2 opencv-python==4.11.0.86
WORKDIR /opt/converter
COPY deploy/om-converter/worker.py .
COPY VLStream-Cloud-Backend-Server/vls-stream/script/model-conversion/export_hisilicon_yolov8.py ./export_hisilicon_yolov8.py
ENV PYTHONUNBUFFERED=1 OMP_NUM_THREADS=2 YOLO_CONFIG_DIR=/tmp/ultralytics TORCH_FORCE_WEIGHTS_ONLY_LOAD=1
CMD ["python", "/opt/converter/worker.py"]
