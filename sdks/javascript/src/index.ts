export type MiniAppPermission = "PROFILE_BASIC" | "CONTEXT_LAUNCH";

export interface SignedMiniAppManifest {
  manifestId: string;
  appId: string;
  appVersion: string;
  issuer: string;
  origin: string;
  publicKeyBase64: string;
  signatureBase64: string;
  permissions: MiniAppPermission[];
  createdAt: string;
}

export interface SignedMiniAppLaunchTicket {
  ticketId: string;
  appId: string;
  appVersion: string;
  origin: string;
  accountId: string;
  deviceId: string;
  permissions: MiniAppPermission[];
  nonce: string;
  expiresAt: string;
  ticketSignatureBase64: string;
  platformPublicKeyBase64: string;
  consumed: boolean;
}

export interface HostBridgeTransport {
  invoke(method: "profile.getBasic" | "context.launch", payload: Readonly<Record<string, never>>): Promise<unknown>;
}

const METHOD_PERMISSION: Readonly<Record<"profile.getBasic" | "context.launch", MiniAppPermission>> = {
  "profile.getBasic": "PROFILE_BASIC",
  "context.launch": "CONTEXT_LAUNCH"
};

/**
 * Verifies a manifest against its Ed25519 signing key. The canonical payload mirrors
 * the backend contract and intentionally excludes its own signature/public-key fields.
 */
export async function verifyManifest(manifest: SignedMiniAppManifest): Promise<boolean> {
  if (!isValidManifestShape(manifest)) return false;
  return verifyEd25519(
    manifest.publicKeyBase64,
    manifest.signatureBase64,
    manifestPayload(manifest)
  );
}

/**
 * Verifies a caller-bound, unconsumed launch ticket before any host bridge call.
 * Server-side one-time consumption remains mandatory before actual client launch.
 */
export async function verifyLaunchTicket(ticket: SignedMiniAppLaunchTicket, now = new Date()): Promise<boolean> {
  if (!isValidTicketShape(ticket) || ticket.consumed || new Date(ticket.expiresAt).getTime() <= now.getTime()) {
    return false;
  }
  return verifyEd25519(
    ticket.platformPublicKeyBase64,
    ticket.ticketSignatureBase64,
    ticketPayload(ticket)
  );
}

/**
 * A permission-checked bridge facade. It has no generic network, storage, messaging,
 * device-token, payment, or arbitrary-method API.
 */
export class MiniAppBridge {
  public constructor(
    private readonly ticket: SignedMiniAppLaunchTicket,
    private readonly transport: HostBridgeTransport
  ) {}

  public async invoke(method: "profile.getBasic" | "context.launch"): Promise<unknown> {
    const requiredPermission = METHOD_PERMISSION[method];
    if (!this.ticket.permissions.includes(requiredPermission)) {
      throw new Error(`Mini app lacks the ${requiredPermission} permission.`);
    }
    return this.transport.invoke(method, {});
  }
}

export function manifestPayload(manifest: Pick<SignedMiniAppManifest, "appId" | "appVersion" | "issuer" | "origin" | "permissions">): Uint8Array {
  return textBytes(
    `appId=${manifest.appId}\n` +
      `appVersion=${manifest.appVersion}\n` +
      `issuer=${manifest.issuer}\n` +
      `origin=${manifest.origin}\n` +
      `permissions=${canonicalPermissions(manifest.permissions)}\n`
  );
}

export function ticketPayload(ticket: Pick<SignedMiniAppLaunchTicket, "ticketId" | "appId" | "appVersion" | "accountId" | "deviceId" | "nonce" | "expiresAt" | "permissions">): Uint8Array {
  const expiresAtEpochSecond = Math.floor(new Date(ticket.expiresAt).getTime() / 1000);
  return textBytes(
    `ticketId=${ticket.ticketId}\n` +
      `appId=${ticket.appId}\n` +
      `appVersion=${ticket.appVersion}\n` +
      `accountId=${ticket.accountId}\n` +
      `deviceId=${ticket.deviceId}\n` +
      `nonce=${ticket.nonce}\n` +
      `expiresAt=${expiresAtEpochSecond}\n` +
      `permissions=${canonicalPermissions(ticket.permissions)}\n`
  );
}

function isValidManifestShape(manifest: SignedMiniAppManifest): boolean {
  return Boolean(
    manifest.appId && manifest.appVersion && manifest.issuer && manifest.origin.startsWith("https://") &&
      manifest.publicKeyBase64 && manifest.signatureBase64 && manifest.permissions.length > 0
  );
}

function isValidTicketShape(ticket: SignedMiniAppLaunchTicket): boolean {
  return Boolean(
    ticket.ticketId && ticket.appId && ticket.appVersion && ticket.origin.startsWith("https://") &&
      ticket.accountId && ticket.deviceId && ticket.nonce && ticket.platformPublicKeyBase64 &&
      ticket.ticketSignatureBase64 && ticket.permissions.length > 0 && !Number.isNaN(new Date(ticket.expiresAt).getTime())
  );
}

async function verifyEd25519(publicKeyBase64: string, signatureBase64: string, payload: Uint8Array): Promise<boolean> {
  try {
    const key = await crypto.subtle.importKey("spki", toArrayBuffer(base64Bytes(publicKeyBase64)), "Ed25519", false, ["verify"]);
    return crypto.subtle.verify("Ed25519", key, toArrayBuffer(base64Bytes(signatureBase64)), toArrayBuffer(payload));
  } catch {
    return false;
  }
}

function canonicalPermissions(permissions: readonly MiniAppPermission[]): string {
  return [...new Set(permissions)].sort().join(",");
}

function textBytes(value: string): Uint8Array {
  return new TextEncoder().encode(value);
}

function base64Bytes(value: string): Uint8Array {
  const decoded = atob(value);
  return Uint8Array.from(decoded, (character) => character.charCodeAt(0));
}

function toArrayBuffer(value: Uint8Array): ArrayBuffer {
  const copy = new Uint8Array(value.byteLength);
  copy.set(value);
  return copy.buffer as ArrayBuffer;
}
