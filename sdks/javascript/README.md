# Plinth Mini-App SDK for JavaScript/TypeScript

This package verifies Plinth's signed mini-app manifests and short-lived launch tickets before exposing a deliberately narrow host bridge. It does **not** provide arbitrary network access, device-token access, message/ciphertext access, contact enumeration, payment calls, or executable mini-app hosting.

```ts
import { MiniAppBridge, verifyLaunchTicket, verifyManifest } from "@plinth/miniapp-sdk";

if (!(await verifyManifest(manifest))) throw new Error("Invalid manifest");
if (!(await verifyLaunchTicket(ticket))) throw new Error("Invalid ticket");

const bridge = new MiniAppBridge(ticket, hostTransport);
await bridge.invoke("context.launch");
```

The host application must consume the ticket through the Plinth API before actual launch and must provide a transport that dispatches only `profile.getBasic` and `context.launch`. The bridge checks that the signed ticket includes the corresponding declared permission.

The signing payloads are UTF-8 `key=value` lines, terminated by `\n`, with permission names sorted and comma-separated. The exact canonical payload is exported as `manifestPayload` and `ticketPayload` for test vectors and auditing.
