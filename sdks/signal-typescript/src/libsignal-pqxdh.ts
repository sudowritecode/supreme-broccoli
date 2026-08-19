import {
  Fingerprint,
  IdentityKeyStore,
  KEMPublicKey,
  KyberPreKeyStore,
  PreKeyBundle,
  PreKeySignalMessage,
  PreKeyStore,
  ProtocolAddress,
  PublicKey,
  SessionStore,
  SignedPreKeyStore,
  SignalMessage,
  processPreKeyBundle,
  signalDecrypt,
  signalDecryptPreKey,
  signalEncrypt,
} from "@signalapp/libsignal-client";

import {
  SIGNAL_PROTOCOL_PROFILE,
  SignalMessageContext,
  SignalPrekeyBundle,
  SignalProtocolProfile,
} from "./index.js";

export interface LibsignalPqxdhStores {
  sessionStore: SessionStore;
  identityStore: IdentityKeyStore;
  preKeyStore: PreKeyStore;
  signedPreKeyStore: SignedPreKeyStore;
  kyberPreKeyStore: KyberPreKeyStore;
}

export interface SafetyNumberParameters {
  iterations: number;
  version: number;
  localIdentifier: Uint8Array;
  localIdentityKey: PublicKey;
  remoteIdentifier: Uint8Array;
  remoteIdentityKey: PublicKey;
}

/**
 * Direct binding to the official @signalapp/libsignal-client APIs observed in
 * version 0.101.0. It must be reviewed whenever the dependency changes. The
 * caller owns secure persistent store implementations and application-payload
 * context binding. This class neither transmits keys nor stores them itself.
 */
export class LibsignalPqxdhSessionBootstrapper {
  public readonly protocolProfile: SignalProtocolProfile = SIGNAL_PROTOCOL_PROFILE;

  public constructor(
    private readonly localAddress: ProtocolAddress,
    private readonly stores: LibsignalPqxdhStores,
  ) {}

  public async establish(bundle: SignalPrekeyBundle): Promise<ProtocolAddress> {
    this.assertProfile(bundle.protocolProfile);
    const identityKey = PublicKey.deserialize(base64ToBuffer(bundle.identityKey));
    const signedPrekey = PublicKey.deserialize(base64ToBuffer(bundle.signedPrekeyPublic));
    const signedPrekeySignature = base64ToBuffer(bundle.signedPrekeySignature);
    const kyberPrekey = KEMPublicKey.deserialize(base64ToBuffer(bundle.kyberPrekey.publicKey));
    const kyberSignature = base64ToBuffer(bundle.kyberPrekey.signature);

    if (!identityKey.verify(signedPrekey.serialize(), signedPrekeySignature)) {
      throw new Error("The peer signed prekey signature is invalid.");
    }
    if (!identityKey.verify(kyberPrekey.serialize(), kyberSignature)) {
      throw new Error("The peer signed Kyber prekey signature is invalid.");
    }

    const oneTimePrekey = bundle.oneTimePrekey;
    const nativeBundle = PreKeyBundle.new(
      bundle.registrationId,
      bundle.protocolDeviceId,
      oneTimePrekey?.prekeyId ?? null,
      oneTimePrekey === null ? null : PublicKey.deserialize(base64ToBuffer(oneTimePrekey.publicKey)),
      bundle.signedPrekeyId,
      signedPrekey,
      signedPrekeySignature,
      identityKey,
      bundle.kyberPrekey.prekeyId,
      kyberPrekey,
      kyberSignature,
    );
    const peerAddress = ProtocolAddress.new(bundle.protocolAddressName, bundle.protocolDeviceId);
    await processPreKeyBundle(nativeBundle, peerAddress, this.localAddress, this.stores.sessionStore, this.stores.identityStore);
    return peerAddress;
  }

  /** Produces an official libsignal ciphertext message. The caller serializes it into a PREKEY or RATCHET envelope. */
  public encrypt(peerAddress: ProtocolAddress, plaintext: Uint8Array): Promise<Uint8Array> {
    return signalEncrypt(toArrayBuffer(plaintext), peerAddress, this.localAddress, this.stores.sessionStore, this.stores.identityStore)
      .then((message) => new Uint8Array(message.serialize()));
  }

  public decryptRatchet(peerAddress: ProtocolAddress, serialized: Uint8Array): Promise<Uint8Array> {
    return signalDecrypt(
      SignalMessage.deserialize(toArrayBuffer(serialized)), peerAddress, this.localAddress,
      this.stores.sessionStore, this.stores.identityStore,
    ).then((plaintext) => new Uint8Array(plaintext));
  }

  public decryptPrekey(peerAddress: ProtocolAddress, serialized: Uint8Array): Promise<Uint8Array> {
    return signalDecryptPreKey(
      PreKeySignalMessage.deserialize(toArrayBuffer(serialized)), peerAddress, this.localAddress,
      this.stores.sessionStore, this.stores.identityStore, this.stores.preKeyStore,
      this.stores.signedPreKeyStore, this.stores.kyberPreKeyStore,
    ).then((plaintext) => new Uint8Array(plaintext));
  }

  public safetyNumber(parameters: SafetyNumberParameters): string {
    return Fingerprint.new(
      parameters.iterations,
      parameters.version,
      toArrayBuffer(parameters.localIdentifier),
      parameters.localIdentityKey,
      toArrayBuffer(parameters.remoteIdentifier),
      parameters.remoteIdentityKey,
    ).displayableFingerprint().toString();
  }

  public assertContextMatches(context: SignalMessageContext, payloadContext: SignalMessageContext): void {
    if (
      context.conversationId !== payloadContext.conversationId ||
      context.senderDeviceId !== payloadContext.senderDeviceId ||
      context.recipientDeviceId !== payloadContext.recipientDeviceId ||
      context.messageId !== payloadContext.messageId
    ) {
      throw new Error("The decrypted application payload context does not match the expected routing context.");
    }
  }

  private assertProfile(profile: SignalProtocolProfile): void {
    if (profile !== SIGNAL_PROTOCOL_PROFILE) {
      throw new Error("The server returned an unsupported Signal protocol profile.");
    }
  }
}

function base64ToBuffer(value: string): Uint8Array<ArrayBuffer> {
  const decoded = atob(value);
  const bytes = Uint8Array.from(decoded, (character) => character.charCodeAt(0));
  return toArrayBuffer(bytes);
}

function toArrayBuffer(value: Uint8Array): Uint8Array<ArrayBuffer> {
  const output = new Uint8Array(value.byteLength);
  output.set(value);
  return output;
}
