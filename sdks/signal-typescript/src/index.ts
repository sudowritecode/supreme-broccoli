export const SIGNAL_PROTOCOL_PROFILE = "SIGNAL_PQXDH_DOUBLE_RATCHET_V1" as const;

export type SignalProtocolProfile = typeof SIGNAL_PROTOCOL_PROFILE;
export type SignalEnvelopeKind = "PREKEY" | "RATCHET" | "GROUP";

/** Public material returned by Plinth. It must be signature-validated by the cryptographic adapter before session creation. */
export interface SignalPrekeyBundle {
  deviceId: string;
  protocolAddressName: string;
  protocolProfile: SignalProtocolProfile;
  protocolDeviceId: number;
  registrationId: number;
  identityKey: string;
  signedPrekeyId: number;
  signedPrekeyPublic: string;
  signedPrekeySignature: string;
  signedPrekeyExpiresAt: string;
  oneTimePrekey: { prekeyId: number; publicKey: string } | null;
  oneTimePrekeyClaimId: string | null;
  kyberPrekey: { prekeyId: number; publicKey: string; signature: string; lastResort: boolean };
  kyberPrekeyClaimId: string | null;
  remainingOneTimePrekeys: number;
  remainingKyberOneTimePrekeys: number;
}

/** Client-produced opaque message data. Plinth routes this data but never parses or decrypts it. */
export interface SignalProtocolEnvelope {
  protocolProfile: SignalProtocolProfile;
  kind: SignalEnvelopeKind;
  bodyBase64: string;
  associatedDataBase64: string;
}

export interface SignalMessageContext {
  conversationId: string;
  senderDeviceId: string;
  recipientDeviceId: string;
  messageId: string;
}

/**
 * Secure persistence belongs on the client platform (for example Android Keystore,
 * iOS Keychain, or a reviewed encrypted desktop store). Never implement this store
 * with web localStorage, a server API, or an in-memory production substitute.
 */
export interface SignalSecureStateStore {
  read(name: string): Promise<Uint8Array | undefined>;
  write(name: string, value: Uint8Array): Promise<void>;
  remove(name: string): Promise<void>;
}

/**
 * This is the only cryptographic integration point. An implementation must use a
 * separately reviewed, licensed, maintained Signal-compatible library. It must
 * verify signed EC and Kyber prekeys before initialization, securely persist ratchet state,
 * delete consumed one-time private keys, and reject malformed/unauthenticated data.
 */
export interface ReviewedSignalCryptoAdapter {
  readonly protocolProfile: SignalProtocolProfile;
  initializeSession(bundle: SignalPrekeyBundle): Promise<void>;
  encrypt(context: SignalMessageContext, plaintext: Uint8Array): Promise<SignalProtocolEnvelope>;
  decrypt(context: SignalMessageContext, envelope: SignalProtocolEnvelope): Promise<Uint8Array>;
  safetyNumber(peerIdentityKey: string, localIdentityKey: string): Promise<string>;
}

export interface SignalDirectoryApi {
  claimPrekeyBundle(deviceId: string): Promise<SignalPrekeyBundle>;
  recordIdentityVerification(deviceId: string, safetyNumberFingerprint: string): Promise<void>;
}

/**
 * Orchestrates server-bundle retrieval and client-only protocol work. It contains
 * no PQXDH, Double Ratchet, AEAD, KDF, signature, or group-key implementation.
 */
export class SignalProtocolClient {
  public constructor(
    private readonly directoryApi: SignalDirectoryApi,
    private readonly crypto: ReviewedSignalCryptoAdapter,
  ) {
    if (crypto.protocolProfile !== SIGNAL_PROTOCOL_PROFILE) {
      throw new Error("The cryptographic adapter does not support the required Signal protocol profile.");
    }
  }

  public async establishSession(peerDeviceId: string): Promise<SignalPrekeyBundle> {
    const bundle = await this.directoryApi.claimPrekeyBundle(peerDeviceId);
    this.assertProfile(bundle.protocolProfile);
    await this.crypto.initializeSession(bundle);
    return bundle;
  }

  public async encrypt(context: SignalMessageContext, plaintext: Uint8Array): Promise<SignalProtocolEnvelope> {
    const envelope = await this.crypto.encrypt(context, plaintext);
    this.assertProfile(envelope.protocolProfile);
    if (envelope.kind === "GROUP") {
      throw new Error("Group encryption requires the separately versioned group protocol integration.");
    }
    return envelope;
  }

  public async decrypt(context: SignalMessageContext, envelope: SignalProtocolEnvelope): Promise<Uint8Array> {
    this.assertProfile(envelope.protocolProfile);
    if (envelope.kind === "GROUP") {
      throw new Error("Group encryption requires the separately versioned group protocol integration.");
    }
    return this.crypto.decrypt(context, envelope);
  }

  public async verifySafetyNumber(
    peerDeviceId: string,
    peerIdentityKey: string,
    localIdentityKey: string,
  ): Promise<string> {
    const safetyNumberFingerprint = await this.crypto.safetyNumber(peerIdentityKey, localIdentityKey);
    if (!safetyNumberFingerprint || safetyNumberFingerprint.length > 512) {
      throw new Error("The reviewed adapter returned an invalid safety-number fingerprint.");
    }
    await this.directoryApi.recordIdentityVerification(peerDeviceId, safetyNumberFingerprint);
    return safetyNumberFingerprint;
  }

  private assertProfile(protocolProfile: SignalProtocolProfile): void {
    if (protocolProfile !== SIGNAL_PROTOCOL_PROFILE) {
      throw new Error("Unsupported Signal protocol profile.");
    }
  }
}

export { LibsignalPqxdhSessionBootstrapper } from "./libsignal-pqxdh.js";
export type { LibsignalPqxdhStores, SafetyNumberParameters } from "./libsignal-pqxdh.js";
