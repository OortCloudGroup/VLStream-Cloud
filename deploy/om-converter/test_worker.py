import hashlib
import json
import tempfile
import subprocess
import unittest
from pathlib import Path
from unittest.mock import patch
import worker


class WorkerTest(unittest.TestCase):
    def test_process_failure_exposes_cause_instead_of_long_command(self):
        base = Path(__file__).resolve().parents[2] / 'codex/om-converter/tests'
        base.mkdir(parents=True, exist_ok=True)
        with tempfile.TemporaryDirectory(dir=base) as temp:
            folder=Path(temp)
            (folder/'conversion.log').write_text('traceback\nValueError: PT classes mismatch\n')
            result=worker.failure_result(folder,subprocess.CalledProcessError(1,['python','/long/path']))
            self.assertIn('PT classes mismatch',result['message'])
            self.assertNotIn('/long/path',result['message'])

    def test_corrupt_checkpoint_is_rejected_before_any_process(self):
        base = Path(__file__).resolve().parents[2] / 'codex/om-converter/tests'
        base.mkdir(parents=True, exist_ok=True)
        with tempfile.TemporaryDirectory(dir=base) as temp:
            folder = Path(temp)
            (folder/'model.pt').write_bytes(b'bad')
            worker.atomic_json(folder/'request.json', {'protocol':1,'id':folder.name,'soc':'Hi3519DV500','sha256':{'model.pt':'0'*64}})
            with patch('worker.subprocess.run') as run:
                with self.assertRaisesRegex(ValueError,'checksum'):
                    worker.convert(folder)
                run.assert_not_called()

    def test_symlink_input_cannot_escape_job_directory(self):
        base = Path(__file__).resolve().parents[2] / 'codex/om-converter/tests'
        base.mkdir(parents=True, exist_ok=True)
        with tempfile.TemporaryDirectory(dir=base) as temp:
            folder = Path(temp)
            outside=folder/'other';outside.write_bytes(b'data')
            link=folder/'model.pt'
            try: link.symlink_to(outside)
            except OSError: self.skipTest('Symlinks unavailable')
            with self.assertRaises(ValueError): worker.regular(link,folder)

    def test_digest_matches_and_json_is_atomic(self):
        base = Path(__file__).resolve().parents[2] / 'codex/om-converter/tests'
        base.mkdir(parents=True, exist_ok=True)
        with tempfile.TemporaryDirectory(dir=base) as temp:
            path=Path(temp)/'result.json'
            worker.atomic_json(path,{'state':'COMPLETED'})
            self.assertEqual('COMPLETED',json.loads(path.read_text())['state'])
            self.assertEqual(hashlib.sha256(path.read_bytes()).hexdigest(),worker.digest(path))
            self.assertFalse(path.with_suffix('.tmp').exists())


if __name__ == '__main__': unittest.main()
