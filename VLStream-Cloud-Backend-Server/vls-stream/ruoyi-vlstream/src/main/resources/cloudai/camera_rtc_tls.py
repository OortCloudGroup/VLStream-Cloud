"""CameraRTC RSA server certificate support for the pinned aiortc 1.14 transport.

Retains DTLS fingerprint verification and ECDHE forward secrecy. No legacy RSA key
exchange, anonymous suites, or certificate-verification bypass is introduced.
"""
from aiortc import RTCCertificate


class CameraRtcCertificate(RTCCertificate):
    def getFingerprints(self):
        # Existing firmware negotiates SHA-256 like the browser SDK.
        return [item for item in super().getFingerprints() if item.algorithm == 'sha-256']

    def _create_ssl_context(self, srtp_profiles):
        context = super()._create_ssl_context(srtp_profiles)
        context.set_cipher_list(b'ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:'
            b'ECDHE-ECDSA-CHACHA20-POLY1305:ECDHE-RSA-CHACHA20-POLY1305:'
            b'ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384')
        return context


def configure_camera_certificate(peer):
    # aiortc has no public certificate argument. Keep this one adapter pinned/tested.
    if not hasattr(peer, '_RTCPeerConnection__certificates'):
        raise RuntimeError('CAMERA_RTC_RUNTIME_VERSION_MISMATCH')
    peer._RTCPeerConnection__certificates = [CameraRtcCertificate.generateCertificate()]
