"""Linux GPU inference process. Status is based on frames/inference and verified process exit."""
import asyncio
import fcntl
import json
import os
from pathlib import Path
import re
import signal
import stat
import subprocess
import sys
import threading
import time
import uuid
from cloud_ai_media import atomic_json, digest, file_evidence, FrameHistory, ReportIntervals, encode_clip, save_event_images
from gpu_shared_queue import context, confirmed_exit, process_alive, read_json, verify_physical_gpu

FINAL = {'COMPLETED', 'FAILED', 'CANCELLED', 'SKIPPED'}


def now():
    return int(time.time() * 1000)


def process_start():
    return Path('/proc/self/stat').read_text().rsplit(')', 1)[1].split()[19]


def load_config(path):
    path = Path(path).resolve()
    value = read_json(path)
    if path.name != 'job.json' or value.get('kind') != 'CLOUD_AI' or str(uuid.UUID(value['runId'])) != path.parent.name:
        raise ValueError('INVALID_RUN_IDENTITY')
    if type(value.get('gpuIndex')) is not int or not 0 <= value['gpuIndex'] <= 15:
        raise ValueError('INVALID_GPU_INDEX')
    return path.parent, value


def cancelled(root):
    return (root / 'cancel.json').exists()


def expired(config):
    return config.get('plannedEnd') is not None and now() >= config['plannedEnd']


def manifest(root):
    return read_json(root / 'manifest.json') or {'events': []}


def pending_manifest(root):
    acknowledged = set(read_json(root / 'acknowledged.json').get('events', []))
    return {'events': [entry for entry in manifest(root)['events'] if entry['id'] not in acknowledged]}


def final_result(root, state, message):
    current = manifest(root)
    for entry in current['events']:
        metadata = root / 'events' / entry['id'] / 'event.json'
        event = read_json(metadata)
        if event and event.get('mediaState') == 'PENDING':
            event.update(mediaState='FAILED', mediaError='执行结束前未完成告警录像')
            atomic_json(metadata, event)
    atomic_json(root / 'manifest.json', current)
    atomic_json(root / 'result.json', dict(kind='CLOUD_AI', state=state, message=message, endedAt=now(),
        eventCount=len(current['events']), manifestSha256=digest(root / 'manifest.json')))


def status(root):
    state = read_json(root / 'state.json') or read_json(root / 'launch.json')
    alive = process_alive(state.get('supervisorPid'), state.get('supervisorStart'))
    result = read_json(root / 'result.json')
    if result:
        try:
            with confirmed_exit(root):
                return dict(result, exited=True, detail=state, manifest=pending_manifest(root))
        except (BlockingIOError, ValueError):
            return dict(state='STOPPING', message='等待进程退出和资源释放', exited=False, detail=state, manifest=pending_manifest(root))
    if alive:
        stale = now() - state.get('heartbeatAt', 0) > 30000
        return dict(state=state.get('state', 'PREPARING') if not stale else 'UNKNOWN', message=state.get('message', '') if not stale else '执行器心跳中断，等待恢复或确认退出',
                    exited=False, detail=state, manifest=pending_manifest(root))
    # Never restart a process that was already admitted. Recover a durable failed terminal proof.
    if state.get('supervisorPid'):
        with confirmed_exit(root):
            for entry in manifest(root)['events']:
                metadata = root / 'events' / entry['id'] / 'event.json'
                event = read_json(metadata)
                if event and event.get('mediaState') == 'PENDING':
                    event.update(mediaState='FAILED', mediaError='执行器异常退出，录像未完成')
                    atomic_json(metadata, event)
            final_result(root, 'FAILED', '执行器异常退出，已确认进程结束')
        return status(root)
    return dict(state='NEW', exited=True, detail=state, manifest=pending_manifest(root))


def refreshed_source(root, original):
    try:
        sources = read_json(root / 'sources.json')
        if isinstance(sources, list):
            return next((source for source in sources if source.get('grantId') == original['grantId']), original)
    except (OSError, ValueError):
        pass
    return original


async def source_loop(root, original, history, detail, stop):
    from camera_rtc_receiver import CameraRtcReceiver
    from aiortc.contrib.media import MediaPlayer
    while not stop.is_set():
        player = None
        try:
            source = refreshed_source(root, original)
            detail.update(state='CONNECTING', message='正在连接视频源')
            if source['type'] == 'CAMERA_RTC':
                receiver = CameraRtcReceiver(source['socketUrl'], source['cameraId'], history.receive,
                    lambda value: detail.update(sourceState=value), ice_attempt=detail.get('failures', 0))
                await receiver.run(stop)
            elif source['type'] in {'RTSP', 'RTMP', 'HTTP', 'MEDIA'}:
                # open and teardown can block up to the configured FFmpeg network timeout.
                options = {'rw_timeout': '10000000', 'protocol_whitelist': 'http,https,tcp,tls,udp,rtsp,rtp,rtmp,rtmps,crypto,srt'}
                if source['type'] == 'RTSP':
                    options['rtsp_transport'] = 'tcp'
                opening = asyncio.create_task(asyncio.to_thread(MediaPlayer, source['url'],
                    format='rtsp' if source['type'] == 'RTSP' else None, options=options, timeout=10))
                try:
                    player = await asyncio.shield(opening)
                except asyncio.CancelledError:
                    # Wait for the bounded native open, then finally closes the resulting decoder.
                    try:
                        player = await opening
                    except Exception:
                        pass
                    raise
                if player.video is None:
                    raise RuntimeError('NO_VIDEO_TRACK')
                check_source_at = time.monotonic() + 2
                while not stop.is_set():
                    frame = await asyncio.wait_for(player.video.recv(), 20)
                    history.receive(frame)
                    if time.monotonic() >= check_source_at:
                        check_source_at = time.monotonic() + 2
                        if refreshed_source(root, original).get('url') != source['url']:
                            break
            else:
                raise ValueError('UNSUPPORTED_SOURCE')
        except asyncio.CancelledError:
            raise
        except Exception:
            # Never persist library exception messages that can contain source credentials.
            detail.update(state='RECONNECTING', message='视频连接中断，正在重连', failures=detail.get('failures', 0) + 1)
            try:
                await asyncio.wait_for(stop.wait(), 3)
            except asyncio.TimeoutError:
                pass
        finally:
            if player:
                if player.audio:
                    await asyncio.to_thread(player.audio.stop)
                if player.video:
                    await asyncio.to_thread(player.video.stop)


async def detect(root, config, base_state):
    import torch
    import ultralytics
    from ultralytics import YOLO
    if not torch.cuda.is_available() or torch.cuda.device_count() != 1:
        raise RuntimeError('CUDA_NOT_AVAILABLE')
    if ultralytics.__version__ != '8.3.240':
        raise RuntimeError('YOLO_RUNTIME_VERSION_MISMATCH')
    # Explicit CUDA use; no CPU fallback that could falsely report a GPU task as healthy.
    models = []
    for fixed in config['models']:
        path = root / 'models' / (fixed['modelId'] + '.pt')
        if not path.is_file() or digest(path) != fixed['sha256']:
            raise RuntimeError('MODEL_CHECKSUM_MISMATCH')
        model = YOLO(str(path), task='detect')
        if model.task != 'detect' or {str(k): v for k, v in model.names.items()} != fixed['labels']:
            raise RuntimeError('MODEL_CLASS_CONTRACT_MISMATCH')
        model.to('cuda:0')
        models.append((fixed, model))
    captures = {source['grantId']: FrameHistory(config['preSeconds'] + config['postSeconds'] + 15) for source in config['cameras']}
    details = {source['grantId']: dict(name=source.get('name'), state='CONNECTING', inferences=0, failures=0) for source in config['cameras']}
    stop = asyncio.Event()
    sources = [asyncio.create_task(source_loop(root, source, captures[source['grantId']], details[source['grantId']], stop)) for source in config['cameras']]
    intervals, pending, last_inference, pairs = ReportIntervals(), [], {}, {}
    start, deadline = None, config.get('plannedEnd')
    preparing_at = now()
    manifest_value = manifest(root)

    async def heartbeat():
        while not stop.is_set():
            instant = now()
            for key, history in captures.items():
                details[key]['receivedFrames'] = history.received
                details[key]['lastFrameAt'] = history.latest[0] if history.latest else None
            live = [key for key, history in captures.items() if history.latest and instant - history.latest[0] < 15000]
            state = 'PREPARING' if start is None else 'RUNNING' if len(live) == len(captures) else 'DEGRADED'
            if cancelled(root) or deadline is not None and instant >= deadline:
                state = 'STOPPING'
            atomic_json(root / 'state.json', dict(base_state, state=state, message='检测执行中' if state == 'RUNNING' else '等待画面或证据收尾',
                heartbeatAt=instant, startedAt=start, deadline=deadline, cameras=list(details.values()), pairs=list(pairs.values()), eventCount=len(manifest_value['events'])))
            bound = deadline if deadline is not None else preparing_at + 60000 if start is None else None
            stop_request = read_json(root / 'cancel.json').get('at')
            if stop_request:
                bound = min(bound, stop_request) if bound else stop_request
            if bound is not None and instant > bound + (config['postSeconds'] + 90) * 1000:
                # A stuck GPU call must not hold this task forever. CLI status proves exit before release.
                os._exit(74)
            await asyncio.sleep(2)

    async def finish_clips(force=False):
        for event in list(pending):
            if not force and now() < event['detectedAt'] + config['postSeconds'] * 1000:
                continue
            directory = root / 'events' / event['id']
            frames = captures[event['cameraKey']].between(event['detectedAt'] - config['preSeconds'] * 1000,
                                                        event['detectedAt'] + config['postSeconds'] * 1000)
            try:
                video = directory / 'video.mp4'
                event.update(await asyncio.to_thread(encode_clip, video, frames, event['detectedAt'], config['preSeconds'], config['postSeconds']))
                event['files']['video'] = file_evidence(video)
            except Exception:
                event.update(mediaState='FAILED', mediaError='告警录像生成失败或可用帧不足')
            atomic_json(directory / 'event.json', event)
            pending.remove(event)

    heart = asyncio.create_task(heartbeat())
    final = 'FAILED'
    try:
        while True:
            instant = now()
            if cancelled(root):
                final = 'CANCELLED'
                break
            if deadline is not None and instant >= deadline:
                final = 'COMPLETED' if start is not None else 'SKIPPED'
                break
            if start is None and instant - preparing_at > 60000:
                raise RuntimeError('NO_CAMERA_FRAMES')
            if len(manifest_value['events']) >= 10000:
                raise RuntimeError('EVENT_STORAGE_LIMIT')
            await finish_clips()
            acknowledged = read_json(root / 'acknowledged.json').get('events', [])
            if len(manifest_value['events']) - len(acknowledged) >= 200:
                raise RuntimeError('EVIDENCE_BACKLOG_LIMIT')
            for key, history in captures.items():
                latest = history.latest
                if not latest or instant - latest[0] > 15000 or instant - last_inference.get(key, 0) < 1000 / config['sampleFps']:
                    continue
                frame_at, frame = latest
                last_inference[key] = instant
                for fixed, model in models:
                    if cancelled(root) or deadline is not None and now() >= deadline:
                        break
                    pair_key = key + ':' + fixed['modelId']
                    result = await asyncio.to_thread(model.predict, source=frame, device=0, conf=fixed['confidence'],
                        classes=fixed['classes'] or None, verbose=False, max_det=300)
                    if start is None:
                        start = now()
                        if config.get('durationSeconds'):
                            deadline = start + config['durationSeconds'] * 1000
                    details[key].update(state='RUNNING', message='画面与检测正常', inferences=details[key]['inferences'] + 1)
                    pairs[pair_key] = dict(cameraKey=key, modelId=fixed['modelId'], name=fixed['name'], lastInferenceAt=now(),
                        count=pairs.get(pair_key, {}).get('count', 0) + 1)
                    boxes = result[0].boxes
                    detected = [dict(classId=int(box.cls.item()), confidence=round(float(box.conf.item()), 5),
                        box=[round(float(number), 2) for number in box.xyxy[0].tolist()]) for box in boxes]
                    eligible = intervals.eligible(key, fixed['modelId'], detected, frame_at, fixed['reportInterval'])
                    if not eligible:
                        continue
                    event_id = str(uuid.uuid4())
                    directory = root / 'events' / event_id
                    directory.mkdir(parents=True, mode=0o700)
                    await asyncio.to_thread(save_event_images, directory, frame, eligible)
                    event = dict(id=event_id, cameraKey=key, modelId=fixed['modelId'], detectedAt=frame_at, detections=eligible,
                        cameraName=details[key]['name'], modelName=fixed['name'], labels=fixed['labels'], mediaState='PENDING',
                        requestedPreSeconds=config['preSeconds'], requestedPostSeconds=config['postSeconds'],
                        files={role: file_evidence(directory / (role + '.jpg')) for role in ('original', 'annotated')})
                    atomic_json(directory / 'event.json', event)
                    manifest_value['events'].append(dict(id=event_id))
                    atomic_json(root / 'manifest.json', manifest_value)
                    pending.append(event)
                    intervals.recorded(key, fixed['modelId'], eligible, frame_at)
            await asyncio.sleep(.02)
        if final == 'COMPLETED' and (len(pairs) != len(captures) * len(models) or any(not item.latest or now() - item.latest[0] > 15000 for item in captures.values())):
            final = 'FAILED'
    finally:
        # Keep only capture alive for the already reported alerts' post-event evidence.
        tail_until = min(now() + config['postSeconds'] * 1000,
            max([event['detectedAt'] + config['postSeconds'] * 1000 for event in pending] or [now()]))
        while pending and now() < tail_until:
            await finish_clips()
            await asyncio.sleep(.2)
        await finish_clips(force=True)
        stop.set()
        for task in sources:
            task.cancel()
        await asyncio.gather(*sources, return_exceptions=True)
        heart.cancel()
        await asyncio.gather(heart, return_exceptions=True)
    return final


def watchdog(root, config, stopped):
    # Independent of the event loop: a native codec or plotting call can block it.
    while not stopped.wait(2):
        state = read_json(root / 'state.json')
        bound = state.get('deadline') or config.get('plannedEnd')
        requested = read_json(root / 'cancel.json').get('at')
        if requested:
            bound = min(bound, requested) if bound else requested
        if now() - state.get('heartbeatAt', now()) > 120000 or bound is not None and now() > bound + (config['postSeconds'] + 90) * 1000:
            os._exit(74)


def serve(root, config):
    os.umask(0o077)
    with os.fdopen(os.open(root / 'run.lock', os.O_CREAT | os.O_RDWR | os.O_NOFOLLOW, 0o600), 'a') as run_lock:
        try:
            fcntl.flock(run_lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError:
            return
        if (root / 'result.json').exists():
            return
        base = dict(supervisorPid=os.getpid(), supervisorStart=process_start(), heartbeatAt=now(), state='QUEUED', message='等待GPU资源')
        atomic_json(root / 'state.json', base)
        queue = ticket = None
        claimed = False
        gpu_descriptor = None
        watchdog_stopped = threading.Event()
        try:
            if abs(config['dispatchedAt'] - now()) > 120000:
                raise RuntimeError('GPU_CLOCK_SKEW_OR_STALE_DISPATCH')
            if config.get('sharedQueue'):
                queue, ticket = context(config, root)
                verify_physical_gpu({'gpuIndex': config['gpuIndex'], 'gpuUuid': config['sharedQueue']['gpuUuid']})
                queue.register([ticket])
            gpu_path = '/tmp/.vlstream-gpu-{}.lock'.format(config['gpuIndex'])
            gpu_descriptor = os.open(gpu_path, os.O_CREAT | os.O_RDWR | os.O_NOFOLLOW, 0o600)
            info = os.fstat(gpu_descriptor)
            if not stat.S_ISREG(info.st_mode) or info.st_uid != os.geteuid() or info.st_nlink != 1:
                raise RuntimeError('GPU_LOCK_IDENTITY_INVALID')
            while True:
                if cancelled(root) or expired(config):
                    final_result(root, 'CANCELLED' if cancelled(root) else 'SKIPPED', '等待期间停止或窗口已结束')
                    return
                held = False
                try:
                    fcntl.flock(gpu_descriptor, fcntl.LOCK_EX | fcntl.LOCK_NB)
                    held = True
                    busy = subprocess.run(['nvidia-smi', '-i', str(config['gpuIndex']), '--query-compute-apps=pid', '--format=csv,noheader,nounits'],
                        capture_output=True, text=True, timeout=15, check=True).stdout.strip()
                    if not busy and (queue is None or queue.claim(ticket)):
                        claimed = True
                        break
                except BlockingIOError:
                    pass
                if held:
                    fcntl.flock(gpu_descriptor, fcntl.LOCK_UN)
                atomic_json(root / 'state.json', dict(base, heartbeatAt=now()))
                time.sleep(2)
            os.environ['CUDA_VISIBLE_DEVICES'] = str(config['gpuIndex'])
            (root / 'yolo-config').mkdir(exist_ok=True)
            os.environ['YOLO_CONFIG_DIR'] = str(root / 'yolo-config')
            if config.get('sourceHandshake'):
                source_deadline = now() + 180000
                base['needsSources'] = True
                while True:
                    if cancelled(root) or expired(config):
                        final_result(root, 'CANCELLED' if cancelled(root) else 'SKIPPED', '准备视频源期间停止或窗口结束')
                        return
                    prepared = read_json(root / 'sources.json')
                    if isinstance(prepared, list) and {item.get('grantId') for item in prepared} == {item['grantId'] for item in config['cameras']}:
                        config['cameras'] = prepared
                        break
                    if now() >= source_deadline:
                        raise RuntimeError('MEDIA_SOURCE_PREPARATION_TIMEOUT')
                    atomic_json(root / 'state.json', dict(base, state='PREPARING', heartbeatAt=now(), message='GPU 已就绪，正在准备设备视频源'))
                    time.sleep(2)
            atomic_json(root / 'state.json', dict(base, state='PREPARING', heartbeatAt=now(), message='加载固定模型并连接摄像机'))
            threading.Thread(target=watchdog, args=(root, config, watchdog_stopped), daemon=True).start()
            final = asyncio.run(detect(root, config, base))
            final_result(root, final, {'COMPLETED': '按计划完成检测', 'CANCELLED': '已停止检测', 'SKIPPED': '窗口结束前未进入检测', 'FAILED': '部分摄像机或模型未正常完成检测'}[final])
        except Exception as error:
            # Only our enumerated codes are persisted; third-party messages may expose source secrets.
            code = str(error) if re.fullmatch('[A-Z_]{3,80}', str(error)) else type(error).__name__
            final_result(root, 'FAILED', '检测执行失败：' + code)
        finally:
            watchdog_stopped.set()
            try:
                if queue is not None and claimed:
                    queue.archiving(ticket)
            finally:
                if gpu_descriptor is not None:
                    os.close(gpu_descriptor)


def main():
    action, path = sys.argv[1:3]
    root, config = load_config(path)
    if action == 'serve':
        serve(root, config)
        return
    if action == 'start':
        with open(root / 'start.lock', 'a') as lock:
            fcntl.flock(lock, fcntl.LOCK_EX)
            current = status(root)
            if current['state'] == 'NEW':
                with open(root / 'runner.log', 'ab', buffering=0) as log:
                    child = subprocess.Popen([sys.executable, str(root / 'cloud_ai_runner.py'), 'serve', str(root / 'job.json')],
                        cwd=str(root), stdin=subprocess.DEVNULL, stdout=log, stderr=log, start_new_session=True, close_fds=True)
                    # Parent admission marker closes the race before a second scanner can start the same task.
                    start_id = None
                    for _ in range(50):
                        try:
                            start_id = Path('/proc/{}/stat'.format(child.pid)).read_text().rsplit(')', 1)[1].split()[19]
                            break
                        except OSError:
                            time.sleep(.01)
                    atomic_json(root / 'launch.json', dict(supervisorPid=child.pid, supervisorStart=start_id, heartbeatAt=now(), state='QUEUED'))
    elif action == 'cancel':
        if not cancelled(root):
            atomic_json(root / 'cancel.json', dict(at=now()))
        with open(root / 'start.lock', 'a') as lock:
            fcntl.flock(lock, fcntl.LOCK_EX)
            if status(root)['state'] == 'NEW':
                with confirmed_exit(root):
                    final_result(root, 'CANCELLED', '在GPU执行前停止')
        state = read_json(root / 'state.json')
        requested = read_json(root / 'cancel.json').get('at', now())
        if now() - requested > (config['postSeconds'] + 60) * 1000 and process_alive(state.get('supervisorPid'), state.get('supervisorStart')):
            os.killpg(state['supervisorPid'], signal.SIGKILL)
    elif action == 'ack':
        event_id = sys.argv[3]
        if event_id not in {item['id'] for item in manifest(root)['events']}:
            raise ValueError('UNKNOWN_EVENT_ACK')
        with open(root / 'acknowledged.lock', 'a') as lock:
            fcntl.flock(lock, fcntl.LOCK_EX)
            acknowledged = set(read_json(root / 'acknowledged.json').get('events', []))
            acknowledged.add(event_id)
            atomic_json(root / 'acknowledged.json', dict(events=sorted(acknowledged)))
        print('VLS_CLOUD_AI=' + json.dumps(dict(acknowledged=event_id)))
        return
    elif action == 'release':
        result = status(root)
        if not result.get('exited') or result['state'] not in FINAL:
            raise ValueError('EXIT_NOT_CONFIRMED')
        queue, ticket = context(config, root)
        queue.release(ticket, dict(state='DETECTION_ARCHIVED', manifestSha256=result['manifestSha256'], eventCount=result['eventCount']))
    elif action != 'status':
        raise ValueError('UNKNOWN_ACTION')
    print('VLS_CLOUD_AI=' + json.dumps(status(root), ensure_ascii=False, separators=(',', ':')))


if __name__ == '__main__':
    main()
