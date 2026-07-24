#!/usr/bin/env python3
"""Reference device client for VLStream MQTT model dispatch.

Dependencies:
    pip install paho-mqtt requests
"""

import hashlib
import json
import os
import shlex
import subprocess
import tempfile
from pathlib import Path

import paho.mqtt.client as mqtt
import requests


DEVICE_ID = os.environ["VLSTREAM_DEVICE_ID"]
MQTT_HOST = os.environ["VLSTREAM_MQTT_HOST"]
MQTT_PORT = int(os.getenv("VLSTREAM_MQTT_PORT", "1883"))
MQTT_USERNAME = os.getenv("VLSTREAM_MQTT_USERNAME")
MQTT_PASSWORD = os.getenv("VLSTREAM_MQTT_PASSWORD")
DISPATCH_TOPIC = os.getenv("VLSTREAM_MODEL_DISPATCH_TOPIC", "oortcloud/dispatchAlgorithms")
MODEL_DIR = Path(os.getenv("VLSTREAM_MODEL_DIR", "/mnt/models"))
ACTIVATE_COMMAND = os.getenv("VLSTREAM_MODEL_ACTIVATE_COMMAND", "")


def publish_status(client, message, status, detail):
    """Publish one deployment progress event to the reply topic from the task."""
    payload = {
        "requestId": message["requestId"],
        "deviceId": DEVICE_ID,
        "status": status,
        "message": detail,
    }
    client.publish(message["replyTopic"], json.dumps(payload), qos=1)


def download_and_verify(message):
    """Stream to a temporary file and verify byte length plus SHA-256."""
    MODEL_DIR.mkdir(parents=True, exist_ok=True)
    suffix = "." + message["modelType"].replace("int8-", "")
    digest = hashlib.sha256()
    downloaded = 0
    fd, temp_name = tempfile.mkstemp(prefix="vlstream-", suffix=suffix, dir=str(MODEL_DIR))
    try:
        with os.fdopen(fd, "wb") as output:
            with requests.get(message["modelUrl"], stream=True, timeout=(15, 300)) as response:
                response.raise_for_status()
                for chunk in response.iter_content(chunk_size=1024 * 1024):
                    if not chunk:
                        continue
                    output.write(chunk)
                    digest.update(chunk)
                    downloaded += len(chunk)
        if downloaded != int(message["fileSize"]):
            raise ValueError(f"size mismatch: expected={message['fileSize']} actual={downloaded}")
        actual_sha256 = digest.hexdigest()
        if actual_sha256.lower() != str(message["sha256"]).lower():
            raise ValueError(
                f"sha256 mismatch: expected={message['sha256']} actual={actual_sha256}"
            )
        return Path(temp_name)
    except Exception:
        Path(temp_name).unlink(missing_ok=True)
        raise


def activate_model(temp_path, message):
    """Atomically install the file and invoke an optional hardware-specific hook."""
    target = MODEL_DIR / message["fileName"]
    os.replace(str(temp_path), str(target))
    if ACTIVATE_COMMAND:
        args = [part.replace("{path}", str(target)) for part in shlex.split(ACTIVATE_COMMAND)]
        subprocess.run(args, check=True, timeout=120)
    return target


def handle_dispatch(client, payload):
    """Process one task only when its hardware device number matches this client."""
    message = json.loads(payload.decode("utf-8"))
    if message.get("deviceId") != DEVICE_ID:
        return
    required = {
        "requestId", "deviceId", "modelType", "modelUrl", "fileName",
        "fileSize", "sha256", "replyTopic",
    }
    missing = sorted(required.difference(message))
    if missing:
        raise ValueError("missing fields: " + ",".join(missing))

    try:
        publish_status(client, message, "RECEIVED", "task accepted")
        publish_status(client, message, "DOWNLOADING", "model download started")
        temp_path = download_and_verify(message)
        publish_status(client, message, "DOWNLOADED", "model download completed")
        publish_status(client, message, "VERIFYING", "size and SHA-256 verified")
        publish_status(client, message, "DEPLOYING", "model activation started")
        target = activate_model(temp_path, message)
        publish_status(client, message, "SUCCESS", f"model activated: {target.name}")
    except Exception as exc:
        publish_status(client, message, "FAILED", str(exc)[:500])


def on_connect(client, _userdata, _flags, reason_code, _properties=None):
    """Restore the dispatch subscription after initial connection or reconnect."""
    if int(reason_code) != 0:
        raise RuntimeError(f"MQTT connection failed: {reason_code}")
    client.subscribe(DISPATCH_TOPIC, qos=1)


def on_message(client, _userdata, mqtt_message):
    """Keep the MQTT network loop alive if one malformed task is received."""
    try:
        handle_dispatch(client, mqtt_message.payload)
    except Exception as exc:
        print(f"invalid dispatch message: {exc}", flush=True)


def main():
    """Connect to the broker and run until the process is stopped."""
    client = mqtt.Client(
        mqtt.CallbackAPIVersion.VERSION2,
        client_id=f"vlstream-device-{DEVICE_ID}",
        clean_session=False,
        protocol=mqtt.MQTTv311,
    )
    if MQTT_USERNAME:
        client.username_pw_set(MQTT_USERNAME, MQTT_PASSWORD)
    client.on_connect = on_connect
    client.on_message = on_message
    client.connect(MQTT_HOST, MQTT_PORT, keepalive=60)
    client.loop_forever()


if __name__ == "__main__":
    main()
