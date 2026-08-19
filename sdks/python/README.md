# Plinth Mini-App SDK for Python

This package provides **verification primitives only** for Plinth signed mini-app manifests and short-lived launch tickets. It does not expose platform API clients, device tokens, messages/ciphertext, user contacts, payment actions, arbitrary network execution, or an app sandbox.

```python
from plinth_miniapps import SignedManifest, verify_manifest

manifest = SignedManifest(
    app_id="word-tool",
    app_version="1.0.0",
    issuer="Example Studio",
    origin="https://apps.example.test/word-tool",
    public_key_base64="...",
    signature_base64="...",
    permissions=("PROFILE_BASIC", "CONTEXT_LAUNCH"),
)
assert verify_manifest(manifest)
```

`verify_launch_ticket` additionally rejects expired or already-consumed tickets before verifying the platform Ed25519 signature. The platform must still consume a ticket through the backend before launching a client bridge.

The canonical payload uses UTF-8 `key=value` lines terminated by `\n`; permission names are sorted and comma-separated. The package exports `manifest_payload` and `ticket_payload` so partners can reproduce the exact signed bytes during integration tests.
