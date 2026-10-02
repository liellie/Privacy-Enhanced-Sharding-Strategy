# Research artifact security

This software is an experimental prototype. Known protocol and integration limitations are documented in `docs/IMPLEMENTATION_SCOPE.md`.

No original deployment secrets are distributed. Fresh chain initialization creates private keys, `chain/.env` and deployment identity files locally. Those files, real Java configurations, logs and node data are ignored by Git. Never use the test accounts or protocol prototype to hold real assets.

Before publishing changes, run `python scripts/check_publication.py` and review the Git diff. The scanner checks common credential patterns and runtime-file names; public addresses, transaction hashes, metadata hashes and contract bytecode are legitimate research/code data and are not private keys merely because they are hexadecimal.

Report security issues through the maintainer contact associated with the paper or hosting repository. Do not include credentials or working private keys in a public issue.
