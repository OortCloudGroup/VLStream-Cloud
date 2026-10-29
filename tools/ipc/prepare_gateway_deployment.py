"""Render public IPC gateway deployment inputs. Never applies or reads secrets."""
import argparse
import ipaddress
import json
from pathlib import Path
import re


def validate_suffix(value, mode):
    suffix = value.lower().strip().removeprefix('.')
    labels = suffix.split('.')
    if len(suffix) > 180 or len(labels) < 2 or any(
        not re.fullmatch(r'[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?', part)
        for part in labels
    ):
        raise ValueError('Use an ASCII DNS suffix without wildcard, port or URL.')
    try:
        ipaddress.ip_address(suffix)
    except ValueError:
        pass
    else:
        raise ValueError('An IP address cannot replace a session DNS suffix.')
    if labels[-1] == 'localhost':
        raise ValueError('localhost is unsuitable for access from another machine.')
    if mode == 'production' and (
        labels[-1] in ('test', 'invalid', 'example', 'local')
        or suffix in ('example.com', 'example.net', 'example.org')
        or suffix.endswith(('.example.com', '.example.net', '.example.org'))
    ):
        raise ValueError('Production requires an approved real DNS name.')
    return suffix


def safe_path(value):
    if not value or not re.fullmatch(r'[A-Za-z0-9_./:\\-]+', value):
        raise ValueError('Certificate reference must be a simple path without directives.')
    return value.replace('\\', '/')


def render_plan(mode, suffix, address, port, certificate, private_key, gateway_port=8088):
    if mode not in ('lan', 'production'):
        raise ValueError('Mode must be lan or production.')
    suffix = validate_suffix(suffix, mode)
    ip = ipaddress.IPv4Address(address)
    if ip.is_unspecified or ip.is_multicast or ip.is_link_local:
        raise ValueError('Bind an explicitly approved unicast IPv4 address.')
    private_lan = any(ip in ipaddress.ip_network(net) for net in (
        '10.0.0.0/8', '172.16.0.0/12', '192.168.0.0/16'))
    if mode == 'lan' and not private_lan:
        raise ValueError('LAN templates require the approved private LAN address.')
    if not 1 <= int(port) <= 65535 or not 1 <= int(gateway_port) <= 65535:
        raise ValueError('Port is outside 1..65535.')
    certificate, private_key = safe_path(certificate), safe_path(private_key)
    authority = '{sessionId}.' + suffix + ('' if int(port) == 443 else ':' + str(port))
    public_base = 'https://' + authority
    server = f'''# Include inside http{{}} AFTER approval. Preserve the existing Agent/default server.
# This file does not configure DNS, issue certificates, open ports or grant access.
server {{
    listen {ip}:{port} ssl;
    server_name ~^[a-z0-9-]+\\.{re.escape(suffix)}$;
    ssl_certificate {certificate};
    ssl_certificate_key {private_key};
    ssl_protocols TLSv1.2 TLSv1.3;
    access_log off;
    error_log stderr crit;
    location / {{
        proxy_pass http://127.0.0.1:{gateway_port};
        proxy_http_version 1.1;
        proxy_set_header Host $http_host;
        proxy_set_header X-Forwarded-Proto https;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection $ipc_gateway_connection_upgrade;
        proxy_read_timeout 36000s;
        proxy_send_timeout 36000s;
        proxy_buffering off;
    }}
}}
'''
    mapping = '''# Include ONCE inside http{}, before the server include.
map $http_upgrade $ipc_gateway_connection_upgrade {
    default upgrade;
    '' close;
}
'''
    public_env = (
        '# Merge only these public keys into existing protected configuration.\n'
        '# Do not replace the full environment or rotate existing identities.\n'
        f'VLSTREAM_TUNNEL_GATEWAY_BASE_URL={public_base}\n'
        f'VLSTREAM_TUNNEL_GATEWAY_HOST_SUFFIX=.{suffix}\n'
        f'TUNNEL_GATEWAY_HOST_SUFFIX=.{suffix}\n'
        'TUNNEL_PUBLIC_HTTPS=true\n'
    )
    backend = f"vlstream:\n  tunnel:\n    gateway-base-url: '{public_base}'\n"
    manifest = {
        'mode': mode, 'sessionDnsWildcard': '*.' + suffix,
        'listenAddress': str(ip), 'httpsPort': int(port),
        'gatewayUpstream': '127.0.0.1:' + str(gateway_port),
        'publicBase': public_base, 'gatewayHostSuffix': '.' + suffix,
        'certificateReference': certificate, 'privateKeyReference': private_key,
        'credentialsReadOrGenerated': False, 'changesApplied': False,
        'requiredApprovals': ['DNS scope', 'certificate issuance/reference and trust',
                              'configuration activation/reload window'],
        'crossMachineAcceptanceCompleted': False,
    }
    return {'nginx-ipc-map.conf': mapping, 'nginx-ipc-session.conf': server,
            'gateway-public.env': public_env, 'backend-public.yml': backend,
            'deployment-plan.json': json.dumps(manifest, indent=2) + '\n'}


def write_plan(destination, files):
    destination = Path(destination)
    destination.mkdir(parents=True, exist_ok=False)
    for name, content in files.items():
        (destination / name).write_text(content, encoding='utf-8', newline='\n')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--mode', choices=('lan', 'production'), required=True)
    parser.add_argument('--suffix', required=True)
    parser.add_argument('--listen-address', required=True)
    parser.add_argument('--https-port', type=int, default=443)
    parser.add_argument('--certificate', required=True)
    parser.add_argument('--private-key-reference', required=True)
    parser.add_argument('--gateway-port', type=int, default=8088)
    parser.add_argument('--out', required=True)
    args = parser.parse_args()
    try:
        files = render_plan(args.mode, args.suffix, args.listen_address, args.https_port,
                            args.certificate, args.private_key_reference, args.gateway_port)
        write_plan(args.out, files)
    except (ValueError, OSError) as error:
        parser.error(str(error))
    print(json.dumps({'generatedDirectory': str(Path(args.out).resolve()),
                      'files': list(files), 'changesApplied': False,
                      'certificateOrPrivateKeyRead': False}))


if __name__ == '__main__':
    main()
