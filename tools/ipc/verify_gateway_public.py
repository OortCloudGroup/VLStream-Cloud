"""Run on the actual client PC: DNS -> strict TLS -> anonymous gateway denial.

No login, startup token, Cookie, enrollment, configuration write or redirect.
"""
import argparse
from datetime import datetime, timezone
import http.client
import json
from pathlib import Path
import platform
import socket
import ssl
from urllib.parse import urlsplit


def check(url, ca_file=None, vantage='unspecified', expected_address=None):
    parsed = urlsplit(url)
    if (parsed.scheme != 'https' or not parsed.hostname or parsed.username is not None
            or parsed.password is not None or parsed.path not in ('', '/')
            or parsed.query or parsed.fragment):
        raise ValueError('Use an HTTPS session-host root URL without credentials or token path.')
    hostname, port = parsed.hostname, parsed.port or 443
    report = {'observedAtUtc': datetime.now(timezone.utc).isoformat(),
              'clientComputer': platform.node(), 'vantageLabel': vantage,
              'hostname': hostname, 'port': port, 'credentialsSent': False,
              'tlsVerificationDisabled': False, 'sessionCreated': False}
    connection = None
    try:
        addresses = sorted({r[4][0] for r in socket.getaddrinfo(hostname, port, type=socket.SOCK_STREAM)})
        report['dnsAddresses'] = addresses
        if expected_address:
            report['expectedAddress'] = expected_address
            report['expectedAddressPresent'] = expected_address in addresses
        context = ssl.create_default_context(cafile=ca_file)
        context.minimum_version = ssl.TLSVersion.TLSv1_2
        connection = http.client.HTTPSConnection(hostname, port, timeout=8, context=context)
        connection.request('GET', '/', headers={'Connection': 'close'})
        report['clientSocketAddress'] = connection.sock.getsockname()[0]
        report['tlsVersion'] = connection.sock.version()
        cert = connection.sock.getpeercert()
        report['certificateSans'] = cert.get('subjectAltName')
        report['certificateExpires'] = cert.get('notAfter')
        report['strictTlsVerified'] = True
        response = connection.getresponse()
        body = response.read(2049)
        report['httpStatus'] = response.status
        report['expectedAnonymousSessionDenial'] = (
            response.status == 401 and b'remote-management browser session is required' in body)
        report['publicPathCheckPassed'] = report['expectedAnonymousSessionDenial']
    except ssl.SSLCertVerificationError as error:
        report.update(failedLayer='TLS', errorType=type(error).__name__,
                      verifyCode=error.verify_code, verifyMessage=error.verify_message)
    except socket.gaierror as error:
        report.update(failedLayer='DNS', errorCode=error.errno)
    except ssl.SSLError as error:
        report.update(failedLayer='TLS', errorType=type(error).__name__)
    except http.client.HTTPException as error:
        report.update(failedLayer='HTTP', errorType=type(error).__name__)
    except (ConnectionError, TimeoutError, OSError) as error:
        report.update(failedLayer='connection', errorType=type(error).__name__)
    finally:
        if connection:
            connection.close()
    report['authenticatedCameraAcceptanceCompleted'] = False
    return report


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--url', required=True)
    parser.add_argument('--ca-file', help='Existing approved PUBLIC CA file; no trust-store installation.')
    parser.add_argument('--vantage', required=True, help='Label the actual client PC where this runs.')
    parser.add_argument('--expected-address')
    parser.add_argument('--out', required=True)
    args = parser.parse_args()
    try:
        result = check(args.url, args.ca_file, args.vantage, args.expected_address)
        with Path(args.out).open('x', encoding='utf-8') as handle:
            json.dump(result, handle, ensure_ascii=False, indent=2)
            handle.write('\n')
    except (ValueError, OSError) as error:
        parser.error(str(error))
    print(json.dumps(result, ensure_ascii=False))
    raise SystemExit(0 if result.get('publicPathCheckPassed') else 1)


if __name__ == '__main__':
    main()
