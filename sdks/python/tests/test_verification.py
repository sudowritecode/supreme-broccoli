from __future__ import annotations

import base64
import unittest
from datetime import datetime, timedelta, timezone

from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey

from plinth_miniapps import (
    SignedLaunchTicket,
    SignedManifest,
    manifest_payload,
    ticket_payload,
    verify_launch_ticket,
    verify_manifest,
)


class SignatureVerificationTest(unittest.TestCase):
    def setUp(self) -> None:
        self.private_key = Ed25519PrivateKey.generate()
        self.public_key_base64 = base64.b64encode(
            self.private_key.public_key().public_bytes(
                serialization.Encoding.DER,
                serialization.PublicFormat.SubjectPublicKeyInfo,
            )
        ).decode()

    def test_manifest_and_ticket_verification(self) -> None:
        unsigned_manifest = SignedManifest(
            app_id="word-tool",
            app_version="1.0.0",
            issuer="Plinth Test Issuer",
            origin="https://apps.example.test/word-tool",
            public_key_base64=self.public_key_base64,
            signature_base64="",
            permissions=("PROFILE_BASIC", "CONTEXT_LAUNCH"),
        )
        manifest = SignedManifest(
            **{
                **unsigned_manifest.__dict__,
                "signature_base64": base64.b64encode(self.private_key.sign(manifest_payload(unsigned_manifest))).decode(),
            }
        )
        self.assertTrue(verify_manifest(manifest))
        self.assertFalse(verify_manifest(SignedManifest(**{**manifest.__dict__, "origin": "https://evil.example.test"})))

        expires_at = (datetime.now(timezone.utc) + timedelta(minutes=5)).isoformat().replace("+00:00", "Z")
        unsigned_ticket = SignedLaunchTicket(
            ticket_id="4f8ccd4f-47e1-48a8-972d-8484fe09fca5",
            app_id="word-tool",
            app_version="1.0.0",
            origin="https://apps.example.test/word-tool",
            account_id="ecdb7dc9-8087-4e7a-b4a8-d1d3a509c978",
            device_id="ebefea05-1d36-4bca-b688-d13e3a1dd3f4",
            permissions=("PROFILE_BASIC", "CONTEXT_LAUNCH"),
            nonce="uYpCcwE5rS1H0R64aWhS4A",
            expires_at=expires_at,
            ticket_signature_base64="",
            platform_public_key_base64=self.public_key_base64,
        )
        ticket = SignedLaunchTicket(
            **{
                **unsigned_ticket.__dict__,
                "ticket_signature_base64": base64.b64encode(self.private_key.sign(ticket_payload(unsigned_ticket))).decode(),
            }
        )
        self.assertTrue(verify_launch_ticket(ticket))
        self.assertFalse(verify_launch_ticket(SignedLaunchTicket(**{**ticket.__dict__, "consumed": True})))


if __name__ == "__main__":
    unittest.main()
