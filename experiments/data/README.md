# Public Ethereum trace

`transactions.txt` is the unchanged public-mainnet trace used by the archived experiments.

- 47,482 parsed transaction records; 30,085 distinct addresses.
- Blocks 21688226–21690401 inclusive (2,176 block positions; 2,148 represented blocks).
- Recorded timestamps span 2025-01-23 23:55:59 through 2025-01-24 07:12:11, as stored in the original trace.
- Fields: block, transaction hash, sender, recipient, value and timestamp.
- SHA-256: `65417b9f1efc977296157dd4c5af9307960a022b80153ed89b06a8fc43862e1d`.

Addresses and transaction hashes are public blockchain identifiers. They have not been replaced with pseudonyms because that would change the evaluated address-based baselines and the file digest. No private signing keys or service credentials belong in this file.

The data is supplied as the original research input, with public Ethereum mainnet provenance. The repository's MIT license covers software; it does not assert exclusive rights over the underlying blockchain records. The original acquisition/export script was not available in the extracted core, so this release does not claim an independently regenerated export.

The file is a mainnet-derived extract, not a complete census of those blocks. It contains 1–153 records per represented block (median 18, mean about 22.1); 42.6% of records have zero value. The acquisition script and its filtering rules are unavailable. Addresses are organizational proxies, not verified institutional identities. Stored timestamps retain their original +08:00 offset for checksum stability.
