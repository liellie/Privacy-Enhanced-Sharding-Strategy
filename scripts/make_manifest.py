#!/usr/bin/env python3
"""Record SHA-256 hashes for the distributable project files."""
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SKIP = {'.git', '.venv', 'node_modules', '__pycache__', 'target', 'build', 'outputs'}

def project_files():
    for path in sorted(ROOT.rglob('*')):
        rel = path.relative_to(ROOT)
        if any(part in SKIP for part in rel.parts) or rel.as_posix() == 'package-manifest.json':
            continue
        if path.is_symlink():
            raise ValueError(f'Symlink is not distributable: {rel}')
        if path.is_file():
            yield path

def main():
    files = [{'path': p.relative_to(ROOT).as_posix(),
              'sha256': hashlib.sha256(p.read_bytes()).hexdigest(),
              'size': p.stat().st_size} for p in project_files()]
    (ROOT / 'package-manifest.json').write_text(
        json.dumps({'project': 'shardchain', 'algorithm': 'sha256', 'files': files}, indent=2) + '\n', encoding='utf-8')
    print(f'Manifest: {len(files)} files')

if __name__ == '__main__':
    main()
