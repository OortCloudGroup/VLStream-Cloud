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
import time
import uuid
from datetime import datetime, timezone
from pathlib import Path

import paho.mqtt.client as mqtt
import requests


DEVICE_ID = os.environ["VLSTREAM_DEVICE_ID"]
MQTT_HOST = os.environ["VLSTREAM_MQTT_HOST"]
MQTT_PORT = int(os.getenv("VLSTREAM_MQTT_PORT", "1883"))
MQTT_USERNAME = os.getenv("VLSTREAM_MQTT_USERNAME")
MQTT_PASSWORD = os.getenv("VLSTREAM_MQTT_PASSWORD")
BUS_TOPIC = f"vlstream/v2.2/dev/{DEVICE_ID}/bus"
MODEL_DIR = Path(os.getenv("VLSTREAM_MODEL_DIR", "/mnt/models"))
ACTIVATE_COMMAND = os.getenv("VLSTREAM_MODEL_ACTIVATE_COMMAND", "")
COMPLETED_MESSAGES = {}


def utc_now():
    """Return a VLS-Protocol UTC timestamp."""
    return datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def publish_status(client, command, model, status, detail, started_at, file_sha256=""):
    """Publish one VLS-Protocol 2.2 modelDeploy acknowledgement."""
    failed = status == "FAILED"
    biz_data = {
        "requestId": model["requestId"],
        "status": status,
        "fileSha256": file_sha256,
        "costMs": int((time.monotonic() - started_at) * 1000),
    }
    reply = {
        "protocolVersion": "2.2",
        "messageId": str(uuid.uuid4()),
        "deviceId": DEVICE_ID,
        "sentAt": utc_now(),
        "msgDir": "dev2platform",
        "mainBizType": "aiBiz",
        "subBizType": "modelDeploy",
        "payload": {
            "sourceMsgId": command["messageId"],
            "code": 500 if failed else 200,
            "msg": detail,
            "errCode": 5001 if failed else 0,
            "errDetail": detail if failed else "",
            "bizData": biz_data,
        },
        "extend": {},
    }
    client.publish(BUS_TOPIC, json.dumps(reply, ensure_ascii=False), qos=1, retain=False)


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
        return Path(temp_name), actual_sha256
    except Exception:
        Path(temp_name).unlink(missing_ok=True)
        raise


def activate_model(temp_path, message):
    """Atomically install the file and restore the previous model if activation fails."""
    target = MODEL_DIR / message["fileName"]
    backup = target.with_suffix(target.suffix + ".previous")
    had_previous = target.exists()
    if had_previous:
        os.replace(str(target), str(backup))
    try:
        os.replace(str(temp_path), str(target))
        if ACTIVATE_COMMAND:
            args = [part.replace("{path}", str(target)) for part in shlex.split(ACTIVATE_COMMAND)]
            subprocess.run(args, check=True, timeout=120)
    except Exception:
        target.unlink(missing_ok=True)
        if message.get("rollbackEnable", False) and had_previous and backup.exists():
            os.replace(str(backup), str(target))
        raise
    return target


def handle_dispatch(client, payload):
    """Process one platform2dev aiBiz/modelDeploy message from this device bus."""
    command = json.loads(payload.decode("utf-8"))
    if (
        command.get("protocolVersion") != "2.2"
        or command.get("deviceId") != DEVICE_ID
        or command.get("msgDir") != "platform2dev"
        or command.get("mainBizType") != "aiBiz"
        or command.get("subBizType") != "modelDeploy"
    ):
        return
    message = command.get("payload") or {}
    required = {
        "requestId", "algorithmId", "modelType", "modelUrl", "fileName",
        "fileSize", "sha256", "expiresAt", "rollbackEnable",
    }
    missing = sorted(required.difference(message))
    if missing:
        raise ValueError("missing fields: " + ",".join(missing))
    source_message_id = command.get("messageId")
    if not source_message_id:
        raise ValueError("missing messageId")
    if source_message_id in COMPLETED_MESSAGES:
        status, detail, file_sha256, started_at = COMPLETED_MESSAGES[source_message_id]
        publish_status(client, command, message, status, detail, started_at, file_sha256)
        return

    started_at = time.monotonic()
    actual_sha256 = ""
    try:
        publish_status(client, command, message, "RECEIVED", "task accepted", started_at)
        publish_status(client, command, message, "DOWNLOADING", "model download started", started_at)
        temp_path, actual_sha256 = download_and_verify(message)
        publish_status(
            client, command, message, "DOWNLOADED", "model download completed",
            started_at, actual_sha256,
        )
        publish_status(
            client, command, message, "VERIFYING", "size and SHA-256 verified",
            started_at, actual_sha256,
        )
        publish_status(
            client, command, message, "DEPLOYING", "model activation started",
            started_at, actual_sha256,
        )
        target = activate_model(temp_path, message)
        detail = f"model activated: {target.name}"
        publish_status(
            client, command, message, "SUCCESS", detail, started_at, actual_sha256,
        )
        COMPLETED_MESSAGES[source_message_id] = (
            "SUCCESS", detail, actual_sha256, started_at,
        )
    except Exception as exc:
        detail = str(exc)[:500]
        publish_status(
            client, command, message, "FAILED", detail, started_at, actual_sha256,
        )
        COMPLETED_MESSAGES[source_message_id] = (
            "FAILED", detail, actual_sha256, started_at,
        )


def on_connect(client, _userdata, _flags, reason_code, _properties=None):
    """Restore the dispatch subscription after initial connection or reconnect."""
    if int(reason_code) != 0:
        raise RuntimeError(f"MQTT connection failed: {reason_code}")
    client.subscribe(BUS_TOPIC, qos=1)


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
        client_id=f"vlstream-{DEVICE_ID}",
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
