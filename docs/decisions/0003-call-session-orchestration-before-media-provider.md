# ADR-0003: Implement Call-Session Orchestration Before Selecting a Media Provider

**Status:** Accepted  
**Date:** 18 August 2026  
**Decision owner:** Manus AI, per user instruction to proceed autonomously and record decisions for review.

## Context

Private text and private groups are now established in the target repository. The next approved capability is voice/video calling. A call experience requires both platform authorisation/signalling and an SFU/TURN media path, but no provider account, deployment environment, region/cost requirements, mobile WebRTC stack, or data-processing agreement has been supplied.

Hard-coding a media provider or attempting to relay media through the Spring Boot application would create unnecessary lock-in, operational risk, and privacy uncertainty.

## Decision

Implement an **authorised call-session orchestration layer** now and isolate media-provider integration behind a `CallMediaPort`. The platform will create/end call sessions, enforce direct/group active membership, issue a provider-neutral short-lived participant credential only through the port, and remove participants when group membership or device status becomes invalid. The first implementation will use a non-networking disabled/stub adapter; no real media credential, recording, call analytics content, TURN configuration, or SFU provider is enabled.

## Consequences

| Decision | Consequence |
|---|---|
| Calls reference existing direct/group conversations | There is one membership/authorisation source of truth. |
| Media is provider-neutral | A managed SFU/TURN provider can be selected later without controller/schema rewrites. |
| Call state is durable | Start, join, leave, end, and removal events can be audited and tested independent of media. |
| No provider token exists by default | A call API cannot accidentally become a live calling feature merely by deployment. |
| Group removal intersects calls | Future membership removal will terminate associated participation before a media adapter issues/reuses credentials. |

## Deferred Work

Select a managed SFU/TURN provider after evaluating regional coverage, privacy/data processing, mobile SDK compatibility, operational support, and cost. Then implement short-lived provider tokens, mobile camera/microphone UX, network change handling, active speaker state, call quality telemetry without media content, room-call integration, and user-facing ringing/push wake-ups.
