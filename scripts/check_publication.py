#!/usr/bin/env python3
"""Heuristic checks for accidental credentials and local runtime files; prints locations, not values."""
from pathlib import Path
import json
import gzip
import re

ROOT = Path(__file__).resolve().parents[1]
SKIP = {".git", ".venv", "node_modules", "target", "__pycache__", "outputs", "build"}
RULES = {
    "private-key block": re.compile(r"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----"),
    "literal signing key": re.compile(r'''(?i)(?:private[_-]?key|DEPLOYER_PRIVATE_KEY|COORDINATOR_PRIVATE_KEY)["']?\s*[:=]\s*["']?(?:0x)?[0-9a-f]{64}(?![0-9a-f])'''),
    "access token": re.compile(r"(?:gh[pousr]_[A-Za-z0-9]{30,}|github_pat_[A-Za-z0-9_]{40,}|AKIA[0-9A-Z]{16})"),
    "credential URL": re.compile(r"https?://[^\s/@:$]+:[^\s/@$]+@"),
    "local Unix home path": re.compile(r"/(?:Users|home)/[A-Za-z0-9_.-]+/"),
    "local Windows path": re.compile(r"(?<![A-Za-z])[A-Za-z]:[\\/](?:Users|Documents and Settings)(?:[\\/]|\b)", re.I),
}

def main():
    findings = []
    files = 0
    for p in sorted(ROOT.rglob("*")):
        rel = p.relative_to(ROOT)
        if p.is_symlink():
            findings.append({"file": rel.as_posix(), "rule": "symbolic link; inspect target"})
            continue
        if not p.is_file() or any(part in SKIP for part in rel.parts):
            continue
        files += 1
        if p.name in {".env", "key", "key.pub", "deployed.json", "static-nodes.json", "genesis.json", ".DS_Store"} or p.suffix in {".log", ".pem", ".jks", ".p12"}:
            findings.append({"file": rel.as_posix(), "rule": "runtime/private file"})
        try:
            if p.name.endswith(".json.gz"):
                with gzip.open(p, "rt", encoding="utf-8") as handle:
                    text = handle.read()
                json.loads(text)
            else:
                text = p.read_text(encoding="utf-8")
        except (UnicodeError, OSError, ValueError):
            findings.append({"file": rel.as_posix(), "rule": "unexpected binary; inspect manually"})
            continue
        for name, pattern in RULES.items():
            for match in pattern.finditer(text):
                findings.append({"file": rel.as_posix(), "line": text.count("\n", 0, match.start()) + 1, "rule": name})
    print(json.dumps({"files_scanned": files, "findings": findings}, indent=2))
    return int(bool(findings))

if __name__ == "__main__":
    raise SystemExit(main())
