#!/usr/bin/env python3
"""Verify the complete public file inventory, file hashes and workload digest."""
import hashlib
import json
from pathlib import Path
from make_manifest import ROOT, project_files

EXPECTED_TRACE = '65417b9f1efc977296157dd4c5af9307960a022b80153ed89b06a8fc43862e1d'

def main():
    manifest = json.loads((ROOT / 'package-manifest.json').read_text(encoding='utf-8'))
    failures = []
    expected = set()
    for entry in manifest['files']:
        name = entry['path']
        rel = Path(name)
        if rel.is_absolute() or '..' in rel.parts or name in expected:
            failures.append('invalid or duplicate entry: ' + name)
            continue
        expected.add(name)
        path = ROOT / rel
        if path.is_symlink() or not path.is_file() or hashlib.sha256(path.read_bytes()).hexdigest() != entry['sha256']:
            failures.append(name)
    try:
        actual = {p.relative_to(ROOT).as_posix() for p in project_files()}
        failures += ['unlisted file: ' + p for p in sorted(actual - expected)]
    except ValueError as error:
        failures.append(str(error))
    trace = ROOT / 'experiments/data/transactions.txt'
    if not trace.is_file() or hashlib.sha256(trace.read_bytes()).hexdigest() != EXPECTED_TRACE:
        failures.append('workload checksum mismatch')
    print(json.dumps({'checked_files': len(expected), 'failures': failures}, indent=2))
    return int(bool(failures))

if __name__ == '__main__':
    raise SystemExit(main())
