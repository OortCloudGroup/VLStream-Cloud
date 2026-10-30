"""Receive the existing VLStream CameraRTC signalling protocol without a browser.

The caller owns authorization, source selection, reconnect policy and task lifetime.
Media frames are delivered directly to the worker; no RTSP bridge is required.
"""
import asyncio
import json
import re
import uuid
from urllib.parse import urlsplit


def ice_configuration(payload, attempt=0):
    """Use the server-provided ICE configuration, including its TURN credentials."""
    from aiortc import RTCConfiguration, RTCIceServer
    strategies = []
    for key in ("iceServers", "domainnameiceServers", "domainnameIceServers"):
        configuration = payload.get(key)
        if isinstance(configuration, str):
            try:
                configuration = json.loads(configuration)
            except (ValueError, TypeError):
                continue
        if not isinstance(configuration, dict) or not isinstance(configuration.get("iceServers"), list):
            continue
        if configuration not in strategies:
            strategies.append(configuration)
    # Match the browser's retries: retain supplied configurations first, then try TURN/TCP.
    for configuration in list(strategies):
        for server in configuration["iceServers"]:
            if not isinstance(server, dict):
                continue
            urls = server.get('urls', [])
            for url in [urls] if isinstance(urls, str) else urls:
                if isinstance(url, str) and url.startswith('turn:') and 'transport=udp' in url:
                    alternate = {'iceServers': [dict(server, urls=url.replace('transport=udp', 'transport=tcp'))]}
                    if alternate not in strategies:
                        strategies.append(alternate)
    configuration = strategies[attempt % len(strategies)] if strategies else {"iceServers": []}
    servers = []
    for value in configuration["iceServers"]:
        if not isinstance(value, dict):
            continue
        urls = value.get("urls")
        if isinstance(urls, str):
            urls = [urls]
        if not isinstance(urls, list) or not urls:
            continue
        if any(not isinstance(url, str) or not url.startswith(("stun:", "stuns:", "turn:", "turns:")) for url in urls):
            raise ValueError("Invalid CameraRTC ICE server")
        servers.append(RTCIceServer(urls=urls, username=value.get("username"), credential=value.get("credential")))
    return RTCConfiguration(iceServers=servers), configuration


class CameraRtcReceiver:
    def __init__(self, socket_url, camera_id, on_frame, on_state=None, call_options=None, timeout=25, ice_attempt=0):
        endpoint = urlsplit(socket_url)
        if endpoint.scheme not in ("ws", "wss") or not endpoint.hostname or endpoint.username or endpoint.password or endpoint.query or endpoint.fragment:
            raise ValueError("Invalid CameraRTC signalling endpoint")
        if not re.fullmatch(r"[A-Za-z0-9_-]{1,100}", camera_id or ""):
            raise ValueError("Invalid CameraRTC device identity")
        self.socket_url = socket_url.rstrip("/")
        self.camera_id = camera_id
        self.on_frame = on_frame
        self.on_state = on_state or (lambda state: None)
        self.timeout = timeout
        self.ice_attempt = ice_attempt
        self.call_options = dict(call_options or {})
        if set(self.call_options) - {"user", "pwd", "source"}:
            raise ValueError("Unsupported CameraRTC call options")
        self.session_id = "PCWEB-" + str(uuid.uuid4()).upper()
        self.client_id = str(uuid.uuid4()).upper()
        self.peer = None
        self.socket = None
        self.closed = False
        self.remote_ready = False
        self.pending_candidates = []
        self.frame_count = 0
        self._tasks = set()
        self._failure = None
        self._connected_at = None
        self._last_frame_at = None

    def _spawn(self, coroutine):
        task = asyncio.create_task(coroutine)
        self._tasks.add(task)
        task.add_done_callback(self._tasks.discard)
        return task

    async def send(self, event, data=None):
        if self.socket is None or self.socket.closed:
            return
        body = dict(sessionId=self.session_id, sessionType="IE", messageId=str(uuid.uuid4()).upper(),
                    from_=self.client_id, to=self.camera_id)
        body["from"] = body.pop("from_")
        body.update(data or {})
        await self.socket.send_json(dict(eventName=event, data=body))

    async def run(self, stop_event):
        import aiohttp
        loop = asyncio.get_running_loop()
        self._connected_at = loop.time()
        self.on_state("CONNECTING")
        try:
            async with aiohttp.ClientSession(timeout=aiohttp.ClientTimeout(total=None, sock_connect=10)) as session:
                async with session.ws_connect(self.socket_url + "/wswebclient/" + self.client_id,
                                              max_msg_size=1024 * 1024) as socket:
                    self.socket = socket
                    try:
                        await self.send("__connectto")
                        heartbeat = loop.time()
                        while not stop_event.is_set() and not self.closed:
                            if self._failure:
                                raise RuntimeError(self._failure)
                            if loop.time() - (self._last_frame_at or self._connected_at) > self.timeout:
                                raise TimeoutError("CameraRTC video frames timed out")
                            if loop.time() - heartbeat >= 20:
                                await self.send("__ping")
                                heartbeat = loop.time()
                            try:
                                message = await socket.receive(timeout=0.5)
                            except asyncio.TimeoutError:
                                continue
                            if message.type == aiohttp.WSMsgType.TEXT:
                                await asyncio.wait_for(self.handle_message(message.data), timeout=self.timeout)
                            elif message.type in (aiohttp.WSMsgType.ERROR, aiohttp.WSMsgType.CLOSED, aiohttp.WSMsgType.CLOSE):
                                raise ConnectionError("CameraRTC signalling closed")
                    finally:
                        # Disconnect the camera while signalling is still open, including cancellation.
                        await self.close()
        finally:
            await self.close()

    async def handle_message(self, raw):
        from aiortc import RTCPeerConnection, RTCSessionDescription
        try:
            message = json.loads(raw)
        except (TypeError, ValueError):
            raise ValueError("Invalid CameraRTC signalling message") from None
        data = message.get("data") if isinstance(message, dict) else None
        if not isinstance(data, dict) or data.get("sessionId") != self.session_id:
            return
        event = message.get("eventName")
        if event == "_create":
            if data.get("state") not in ("online", "sleep"):
                raise ConnectionError("CameraRTC device is offline")
            if self.peer is not None:
                raise ValueError("CameraRTC session was created more than once")
            configuration, wire_configuration = ice_configuration(data, self.ice_attempt)
            self.peer = RTCPeerConnection(configuration)
            from camera_rtc_tls import configure_camera_certificate
            configure_camera_certificate(self.peer)
            self.peer.createDataChannel("mydatachannel")

            @self.peer.on("track")
            def track_received(track):
                if track.kind == "video":
                    self._spawn(self._consume(track))

            @self.peer.on("connectionstatechange")
            async def connection_changed():
                if not self.closed and self.peer.connectionState == "failed":
                    self._failure = "CameraRTC media connection failed"

            # Keep the existing device call contract; these protocol defaults mirror CameraRtcSession.
            call = dict(datachannel="true", audio="recvonly", video="recvonly", user="admin", pwd="123456",
                        mode="live", source="MainStream", iceservers=json.dumps(wire_configuration))
            call.update(self.call_options)
            await self.send("__call", call)
            self.on_state("NEGOTIATING")
        elif event == "_offer":
            if self.peer is None or self.remote_ready or not isinstance(data.get("sdp"), str):
                raise ValueError("Unexpected CameraRTC offer")
            await self.peer.setRemoteDescription(RTCSessionDescription(sdp=data["sdp"], type="offer"))
            self.remote_ready = True
            for candidate in self.pending_candidates:
                await self._candidate(candidate)
            self.pending_candidates.clear()
            answer = await self.peer.createAnswer()
            await self.peer.setLocalDescription(answer)
            # aiortc gathers local candidates before returning; send the complete local SDP.
            await self.send("__answer", dict(type=self.peer.localDescription.type, sdp=self.peer.localDescription.sdp))
            # Existing devices also consume explicit trickle messages. Send the same gathered
            # candidates through that contract; some device builds ignore candidates in an answer.
            sections = self.peer.localDescription.sdp.split("\r\nm=")
            for index, section in enumerate(sections[1:]):
                lines = section.splitlines()
                mid = next((line[6:] for line in lines if line.startswith("a=mid:")), str(index))
                for line in lines:
                    if line.startswith("a=candidate:"):
                        await self.send("__ice_candidate", dict(candidate=json.dumps(dict(
                            candidate=line[2:], sdpMid=mid, sdpMLineIndex=index))))
        elif event == "_ice_candidate":
            candidate = data.get("candidate")
            if self.remote_ready:
                await self._candidate(candidate)
            else:
                if len(self.pending_candidates) >= 128:
                    raise ValueError("Too many pending CameraRTC candidates")
                self.pending_candidates.append(candidate)
        elif event in ("_session_failed", "_session_disconnected"):
            raise ConnectionError("CameraRTC device session ended")

    async def _candidate(self, value):
        from aiortc.sdp import candidate_from_sdp
        if isinstance(value, str):
            value = json.loads(value)
        if not value:
            return
        if not isinstance(value, dict):
            raise ValueError("Invalid CameraRTC candidate")
        text = value.get("candidate", "")
        if not text:
            await self.peer.addIceCandidate(None)
            return
        if not isinstance(text, str) or len(text) > 8192:
            raise ValueError("Invalid CameraRTC candidate")
        candidate = candidate_from_sdp(text.removeprefix("candidate:"))
        candidate.sdpMid = value.get("sdpMid")
        candidate.sdpMLineIndex = value.get("sdpMLineIndex")
        await self.peer.addIceCandidate(candidate)

    async def _consume(self, track):
        try:
            while not self.closed:
                frame = await track.recv()
                self.frame_count += 1
                self._last_frame_at = asyncio.get_running_loop().time()
                if self.frame_count == 1:
                    self.on_state("RECEIVING")
                # Keep this callback short; the inference worker consumes a bounded latest-frame queue.
                self.on_frame(frame)
        except asyncio.CancelledError:
            raise
        except Exception:
            if not self.closed:
                self._failure = "CameraRTC video track ended"

    async def close(self):
        if self.closed:
            return
        self.closed = True
        try:
            await asyncio.wait_for(self.send("__disconnected"), timeout=2)
        except (ConnectionError, TimeoutError, OSError):
            pass
        tasks = list(self._tasks)
        for task in tasks:
            task.cancel()
        if tasks:
            await asyncio.gather(*tasks, return_exceptions=True)
        if self.peer is not None:
            await self.peer.close()
        if self.socket is not None and not self.socket.closed:
            await self.socket.close()
        self.on_state("CLOSED")
