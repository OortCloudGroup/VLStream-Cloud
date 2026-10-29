import tempfile
from pathlib import Path
import unittest
from unittest.mock import patch
from prepare_gateway_deployment import render_plan, write_plan
from verify_gateway_public import check


class DeploymentPlanTest(unittest.TestCase):
    def plan(self, **overrides):
        values = dict(mode='lan', suffix='ipc.vls.test', address='10.0.0.10',
                      port=8444, certificate='private/lan-session-cert.pem',
                      private_key='private/lan-session-key.pem')
        values.update(overrides)
        return render_plan(**values)

    def test_lan_names_are_consistent_and_no_secrets_are_requested(self):
        p = self.plan()
        self.assertIn('https://{sessionId}.ipc.vls.test:8444', p['backend-public.yml'])
        self.assertIn('TUNNEL_GATEWAY_HOST_SUFFIX=.ipc.vls.test', p['gateway-public.env'])
        self.assertIn(r'\.ipc\.vls\.test$', p['nginx-ipc-session.conf'])
        self.assertIn('"changesApplied": false', p['deployment-plan.json'])
        self.assertNotIn('API_TOKEN', p['gateway-public.env'])

    def test_host_websocket_and_loopback_boundary_preserved(self):
        p = self.plan()['nginx-ipc-session.conf']
        for value in ('listen 10.0.0.10:8444 ssl;', 'Host $http_host;',
                      'proxy_http_version 1.1;', 'Upgrade $http_upgrade;',
                      'proxy_pass http://127.0.0.1:8088;', 'access_log off;'):
            self.assertIn(value, p)
        self.assertNotIn('default_server', p)
        self.assertNotIn('vlsTunnel/internal', p)

    def test_localhost_and_ip_are_not_lan_suffixes(self):
        for suffix in ('ipc.localhost', 'localhost', '10.0.0.10'):
            with self.subTest(suffix=suffix), self.assertRaises(ValueError):
                self.plan(suffix=suffix)

    def test_suffix_rejects_directive_and_url_injection(self):
        for suffix in ('*.ipc.vls.test', 'https://ipc.vls.test', 'ipc.vls.test:8444',
                       'ipc.vls.test;return 200', 'ipc..test', '-ipc.vls.test'):
            with self.subTest(suffix=suffix), self.assertRaises(ValueError):
                self.plan(suffix=suffix)

    def test_bind_scope_does_not_silently_widen(self):
        for address in ('0.0.0.0', '127.0.0.1', '203.0.113.10', '224.0.0.1'):
            with self.subTest(address=address), self.assertRaises(ValueError):
                self.plan(address=address)

    def test_certificate_reference_is_not_a_directive(self):
        for value in ('key.pem; return 200', 'key.pem\nlisten 80;', '"key.pem"'):
            with self.subTest(value=value), self.assertRaises(ValueError):
                self.plan(private_key=value)

    def test_production_requires_real_name_and_normal_https(self):
        for suffix in ('ipc.vls.test', 'ipc.example.com', 'ipc.invalid'):
            with self.subTest(suffix=suffix), self.assertRaises(ValueError):
                self.plan(mode='production', suffix=suffix)
        p = self.plan(mode='production', suffix='ipc.approved-company.net', port=443)
        self.assertIn('https://{sessionId}.ipc.approved-company.net\n', p['gateway-public.env'])
        self.assertNotIn(':443', p['gateway-public.env'])

    def test_output_never_overwrites_existing_configuration(self):
        with tempfile.TemporaryDirectory() as directory:
            target = Path(directory) / 'plan'
            write_plan(target, self.plan())
            before = (target / 'gateway-public.env').read_text()
            with self.assertRaises(FileExistsError):
                write_plan(target, self.plan(suffix='ipc.other.test'))
            self.assertEqual(before, (target / 'gateway-public.env').read_text())

    def test_invalid_ports_are_rejected(self):
        for port in (0, -1, 65536):
            with self.subTest(port=port), self.assertRaises(ValueError):
                self.plan(port=port)

    def test_public_probe_rejects_credentials_or_startup_paths_before_dns(self):
        with patch('socket.getaddrinfo') as dns:
            for url in ('http://session.ipc.vls.test', 'https://user:secret@ipc.vls.test',
                        'https://session.ipc.vls.test/s/fixture-token',
                        'https://session.ipc.vls.test/?token=fixture',
                        'https://session.ipc.vls.test/#/login'):
                with self.subTest(url=url), self.assertRaises(ValueError):
                    check(url)
            dns.assert_not_called()


if __name__ == '__main__':
    unittest.main()
