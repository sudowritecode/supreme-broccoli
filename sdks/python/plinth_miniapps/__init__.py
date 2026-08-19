"""Verification primitives for Plinth signed mini-app manifests and launch tickets.

This package intentionally does not contain platform API clients, payment actions,
message access, device token handling, arbitrary network methods, or sandboxing.
"""

from __future__ import annotations

import base64
from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Iterable, Sequence

from cryptography.exceptions import InvalidSignature
from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PublicKey
from cryptography.hazmat.primitives.serialization import load_der_public_key


_ALLOWED_PERMISSIONS = frozenset({"PROFILE_BASIC", "CONTEXT_LAUNCH"})


@dataclass(frozen=True)
class SignedManifest:
    app_id: str
    app_version: str
    issuer: str
    origin: str
    public_key_base64: str
    signature_base64: str
    permissions: Sequence[str]


@dataclass(frozen=True)
class SignedLaunchTicket:
    ticket_id: str
    app_id: str
    app_version: str
    origin: str
    account_id: str
    device_id: str
    permissions: Sequence[str]
    nonce: str
    expires_at: str
    ticket_signature_base64: str
    platform_public_key_base64: str
    consumed: bool = False


def canonical_permissions(permissions: Iterable[str]) -> str:
    normalized = tuple(sorted(set(permissions)))
    if not normalized or not set(normalized).issubset(_ALLOWED_PERMISSIONS):
        raise ValueError("Permissions must be a non-empty subset of the supported mini-app scopes.")
    return ",".join(normalized)


def manifest_payload(manifest: SignedManifest) -> bytes:
    return (
        f"appId={manifest.app_id}\n"
        f"appVersion={manifest.app_version}\n"
        f"issuer={manifest.issuer}\n"
        f"origin={manifest.origin}\n"
        f"permissions={canonical_permissions(manifest.permissions)}\n"
    ).encode("utf-8")


def ticket_payload(ticket: SignedLaunchTicket) -> bytes:
    expires_at = _parse_expiry(ticket.expires_at)
    return (
        f"ticketId={ticket.ticket_id}\n"
        f"appId={ticket.app_id}\n"
        f"appVersion={ticket.app_version}\n"
        f"accountId={ticket.account_id}\n"
        f"deviceId={ticket.device_id}\n"
        f"nonce={ticket.nonce}\n"
        f"expiresAt={int(expires_at.timestamp())}\n"
        f"permissions={canonical_permissions(ticket.permissions)}\n"
    ).encode("utf-8")


def verify_manifest(manifest: SignedManifest) -> bool:
    if not _valid_origin(manifest.origin):
        return False
    try:
        return _verify(manifest.public_key_base64, manifest.signature_base64, manifest_payload(manifest))
    except (TypeError, ValueError):
        return False


def verify_launch_ticket(ticket: SignedLaunchTicket, *, now: datetime | None = None) -> bool:
    if ticket.consumed or not _valid_origin(ticket.origin) or not ticket.nonce:
        return False
    now = now or datetime.now(timezone.utc)
    try:
        if _parse_expiry(ticket.expires_at) <= now:
            return False
        return _verify(ticket.platform_public_key_base64, ticket.ticket_signature_base64, ticket_payload(ticket))
    except (TypeError, ValueError):
        return False


def _verify(public_key_base64: str, signature_base64: str, payload: bytes) -> bool:
    public_key = load_der_public_key(base64.b64decode(public_key_base64, validate=True))
    if not isinstance(public_key, Ed25519PublicKey):
        return False
    try:
        public_key.verify(base64.b64decode(signature_base64, validate=True), payload)
        return True
    except InvalidSignature:
        return False


def _parse_expiry(value: str) -> datetime:
    parsed = datetime.fromisoformat(value.replace("Z", "+00:00"))
    return parsed if parsed.tzinfo else parsed.replace(tzinfo=timezone.utc)


def _valid_origin(origin: str) -> bool:
    return origin.startswith("https://") and len(origin) <= 255


__all__ = [
    "SignedManifest",
    "SignedLaunchTicket",
    "canonical_permissions",
    "manifest_payload",
    "ticket_payload",
    "verify_manifest",
    "verify_launch_ticket",
]
