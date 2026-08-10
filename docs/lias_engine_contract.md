# LIAS Android engine contract

Status: contract baseline with Android LIAS 2.0 implementation record  
Android baseline: `357b15e866e4c7716e25388ede4ee769dee55105`  
Reference baseline: `lias_dis` at `6b42a898e1121baa316a44784e2802214cd85007`

## Purpose and scope

LIAS Remote for Android is a client of the LIAS server. It is not a local port
of the Go DIS discovery, correlation, identity, storage, policy-enforcement, or
nftables engines. The server remains authoritative for device identity,
inventory, effective access, persistence, policy enforcement, and event replay.

This document separates:

1. the current Android contract that existing deployments may depend on; and
2. optional future capabilities already present in the reference repository.

Reference functionality is not an implicit Android requirement. No item in the
future or gap sections authorizes a new dependency, endpoint dependency,
architecture, public Kotlin API, wire field, or user-visible behavior.

## Android LIAS 2.0 implementation status

The gap list below records the comparison baseline. The Android LIAS 2.0
upgrade subsequently implemented the following additive capabilities without
editing the reference repository:

| Capability | Android implementation |
| --- | --- |
| Capability negotiation | Validates v1, additive responses, and public `pdid`; older/malformed/unsupported servers retain the legacy path |
| Consolidated snapshot | Capability-gated `/api/v1/snapshot` synchronization with revision state, ETag/304 handling, and automatic fallback to the existing refresh path |
| System status | Displays LIAS API/schema and upstream current, legacy, or degraded state on Home |
| Device identity additions | Decodes opaque `device_id` without using it as a key; decodes assurance, probability, ambiguity, `l7`/`bia` tiers, and richer event facts |
| Identity review | Paged pending/confirmed/rejected queues, detail/profile/evidence comparison, guarded confirm/reject/reopen, verified bindings/revocation, and typed-confirmation split |
| Identity events | Explicit candidate/binding event handling that refetches authoritative identity state |
| Home protections and restrictions | Home shows only effective global overrides, blocked policy-backed groups, and server-identified temporary pauses; restricted devices remain separate, global-source duplicates stay suppressed, and tag-based icons are presentation-only |
| Transport hardening | Structured server messages, encoded dynamic path segments, and a 1 MiB SSE event ceiling |
| Verification | New engine contract tests plus existing regression suite; final compile/test/lint/assemble verification is performed by remote Android CI |

Direct DIS device refresh remains intentionally absent because the Android app
connects to LIAS and the reference LIAS router does not proxy that DIS-only
route. Browser session/CSRF behavior and on-device discovery/correlation remain
intentionally out of scope.

## Change-control gate

Backward compatibility is the default. Before implementation, obtain explicit
approval for any of the following:

- changing repository ownership, state flow, synchronization, transport, or
  dependency-injection architecture;
- adding, removing, renaming, moving, or changing the signature/visibility of a
  public Kotlin declaration;
- changing an existing REST path, method, request shape, result classification,
  SSE cursor rule, event interpretation, or model default;
- changing PDID keying, policy/schedule semantics, destructive-operation
  safeguards, or the displayed identity fallback order;
- making an optional reference capability mandatory;
- adding or replacing a production library or raising the supported Android,
  Java, Kotlin, Gradle, or server baseline.

Purely additive internal work may proceed only when it preserves all current
fallback behavior and is protected by regression tests. A breaking server wire
change belongs in a new API namespace such as `/api/v2`; an increase in the v1
`schema_version` does not make a breaking change acceptable.


## Compose Cupertino Plan 3.0 accepted baseline

Status: accepted dependency and namespace migration record  
Recorded version: 30.0.0

The Android app has completed the Plan 3.0 maintained-fork migration with the following stable UI dependency baseline:

| Area | Accepted baseline |
| --- | --- |
| AGP | 8.10.1 |
| Kotlin / Compose compiler plugin | 2.2.0 |
| Gradle wrapper | 8.11.1 |
| Java/JVM | 17 |
| Android SDKs | minSdk 26, compileSdk 36, targetSdk 35 |
| Compose BOM | 2025.06.00 |
| Cupertino artifacts | `io.github.schott12521:cupertino:2.3.1` and `io.github.schott12521:cupertino-icons-extended:2.3.1` |
| Cupertino Kotlin package | `com.slapps.cupertino` |
| kotlinx.serialization JSON | 1.7.3 |

This record documents the accepted dependency baseline only. It does not authorize or record any LIAS server API change, Android repository ownership change, REST/SSE behavior change, navigation route change, persisted setting change, PDID keying change, identity workflow change, policy/schedule semantic change, or user-visible behavior change. Plan 3.1 UX adoption remains separate and requires explicit approval.

## Current stable Android contract

### Platform and build dependencies

These are the current build inputs, not upgrade recommendations.

| Area | Stable baseline |
| --- | --- |
| Android | minSdk 26, compileSdk 36, targetSdk 35 |
| Toolchain | Gradle 8.11.1, AGP 8.9.3, Kotlin 2.0.20, Java/JVM 17 |
| AndroidX | Core KTX 1.13.1; Lifecycle 2.8.3; Activity Compose 1.9.0; Biometric 1.1.0; Navigation Compose 2.7.7; DataStore 1.1.1 |
| UI | Compose BOM 2024.09.00; Compose UI/foundation; Cupertino and extended icons 0.1.0-alpha04 |
| Transport | OkHttp and MockWebServer 4.12.0 |
| Kotlin runtime libraries | kotlinx.serialization JSON 1.6.3; coroutines 1.8.1 |
| Tests | JUnit 4.13.2; Robolectric 4.16.1; AndroidX Arch Core Testing 2.2.0 |

The current Android engine has no Go, SQLite, nmap, Avahi, NetBIOS, SSDP,
netlink, Pi-hole, DHCP, TLS-fingerprinting, OUI, or nftables runtime dependency.
Those are server/reference implementation details and must not be pulled into
the app merely for feature parity.

### Ownership and authority

- `EventRepository` is the single live application repository and exposes
  server-derived `StateFlow`/`SharedFlow` state.
- `LiasApiClient` owns authenticated REST transport; `LiasSseClient` owns the
  event stream and replay cursor.
- `SettingsRepository` is the persisted authority for server URL and bearer
  token. A connection probe must not mutate the live repository configuration.
- Focused mutation modules use `MutationCoordinator`. Operations on the same
  logical resource are serialized, and REST snapshots are revision-guarded so
  stale reads cannot overwrite a mutation or SSE update.
- Android may validate and explain a draft, but LIAS remains authoritative for
  validation, persistence, effective status, and enforcement.
- `pdid` is the public and canonical device key. Android must not invent a
  replacement identity or use the reference-only opaque `device_id` as a policy
  target.

Changing any of these ownership boundaries is an architectural change and is
subject to the approval gate.

### Stable LIAS REST dependency

All `/api/v1` calls use JSON and an optional `Authorization: Bearer <token>`
header. The connection health check is `GET /health`. Android currently relies
on the following authenticated LIAS routes:

| Resource | Stable operations |
| --- | --- |
| Devices | `GET /api/v1/devices`, `GET /api/v1/devices/{pdid}`, tag assignment, pause/resume, extend/cancel, effective status, rename, user assignment, and logs |
| Tags | list, create, update, delete, extend/cancel, and effective status under `/api/v1/tags` |
| Policies | list/create/update/delete, validate, export, and import under `/api/v1/policies` |
| Schedules | list/create/update/delete under `/api/v1/schedules` |
| Users | list and create under `/api/v1/users` |
| Controls | `POST /api/v1/vacation`, `GET /api/v1/stats`, and `POST /api/v1/nftables/flush` |
| Events | `GET /api/v1/events` as `text/event-stream` |

Dynamic path segments continue to represent server IDs/PDIDs. Existing callers
and routes must remain compatible; a future implementation should URL-encode
dynamic segments without changing the logical endpoint surface.

### Stable wire behavior

- Response decoding ignores unknown JSON keys. Additive optional response fields
  and unknown SSE event types must therefore remain harmless.
- Request DTOs are intentionally bounded. Server-owned policy fields (`id`,
  timestamps, expiry, reason tag, and legacy `schedule_id`) are not sent in a
  `PolicyMutationRequest`.
- `DeviceListResponse` remains `{ "devices": [...], "total": number }`.
- Nullable server collections are exposed through safe empty-list/map accessors.
  Missing optional enrichment data is not fabricated.
- Times remain server-provided strings at the wire boundary. Android does not
  rewrite server timestamps or confidence/provenance.
- Successful `204` or empty responses are valid only for `Unit`; an empty typed
  response is a serialization error.
- HTTP `401` and `403` map to `AuthenticationError`; `409` preserves schedule
  conflicts in `ConflictError`; other non-2xx responses map to `HttpError`;
  transport failures map to `NetworkError`; and typed JSON failures map to
  `SerializationError`.
- Server-computed `EffectiveStatus` is authoritative. Failure to fetch it must
  not be converted into a fabricated allow or block decision.

The current Android `Device` decoder depends on these established fields when
present: public identity (`pdid`, `identity_tier`, `identity_anchor`), host/MAC/IP
history, names, vendor/model/type/services, online and tentative state,
first/last seen, confidence, tags/user, source provenance, and enrichment/nmap
state. Defaults preserve decoding against older LIAS versions.

### Stable SSE behavior

- Framing is standard `event:`, numeric `id:`, JSON `data:`, and blank-line
  separation. Comment/heartbeat lines are ignored.
- A same-server reconnect sends the greatest observed `Last-Event-ID`.
  Disconnect/reconnect preserves it; changing the logical server resets it.
- Replacing the bearer token cancels the active request so the reconnect uses
  the replacement credential.
- One malformed event is dropped without terminating subsequent valid events.
- Android explicitly handles the existing device events, security alerts,
  `effective.status_changed`, and `ping`. Unrecognized event types do not
  mutate inventory, enforcement state, or storage.
- Replayed events suppress transient notifications during the reconnect quiet
  period, while still reconciling authoritative state.
- `device.reidentified` removes the old PDID, fetches the new PDID, and never
  rewrites identity locally.

### Stable Android behavior and safety rules

- Device display name order remains friendly name, hostname, canonical hostname,
  vendor/model, manufacturer, current MAC, PDID, then `Unknown Device`.
- Infrastructure devices/tags cannot be policy targets; the infrastructure tag
  is immutable. Built-in tags are not deletable.
- The generic tag is a fallback: it is removed when a meaningful tag is
  assigned and restored when no meaningful tag remains.
- Tag deletion is blocked while devices or policies depend on the tag. Schedule
  deletion is blocked while policies depend on the schedule.
- `global_default` cannot be deleted, and Android does not create additional
  global policies.
- Vacation mode must not silently transform a previous global `allow` override
  into `schedule`; server state is refreshed after the mutation.
- Schedule semantics retain downtime/whitelist modes, recurring and calendar
  windows, inclusive calendar end dates, valid overnight windows, invalid
  equal start/end times, IANA timezones, and server-authoritative enforcement.
- Policy and schedule IDs remain server-owned. Client-side optimistic updates
  must roll back on failure and reconcile from LIAS on success.
- Credentials remain redacted from diagnostics and stored through the existing
  secure token/settings boundary.

## Gap list

The classification is deliberate: **compatibility gap** means the reference has
already evolved in a way Android should account for defensively; **future
enhancement** means Android does not currently depend on it.

| Gap | Classification | Current Android behavior | Reference behavior / risk |
| --- | --- | --- | --- |
| Capability negotiation | Future enhancement | No `/api/v1/capabilities` model or request | LIAS advertises API/schema versions, `pdid`, additive responses, snapshot, SSE, and optional identity features. Android cannot selectively enable them yet. |
| Consolidated snapshot | Future enhancement | `refreshAll()` makes separate list calls and then per-device/per-tag effective-status calls | `GET /api/v1/snapshot` returns revision, inventory, configuration, users, and status maps with ETag support. It could reduce requests but would change synchronization behavior. |
| System/upstream status | Future enhancement | `/health` only | `/api/v1/system/status` exposes API/schema and DIS reachability/legacy/sync/SSE state. |
| Device `device_id` | Intentional non-dependency | Ignored | Reference exposes an opaque immutable internal ID. Android must continue keying public behavior by `pdid`. |
| Identity tier vocabulary | Compatibility gap | Recognizes `tentative`, `stable`, `verified`; other values present as `UNKNOWN` | Current reference uses `tentative`, `l7`, `bia`. Mapping or changing the Android enum is a public/presentation decision requiring approval. |
| Identity assurance summary | Future enhancement | Does not decode `identity_assurance`, `identity_probability`, or `identity_ambiguous` | Reference adds `unverified`, `candidate`, `strong`, and `verified` assurance plus probability/ambiguity. These are not equivalent to the legacy Android tier labels. |
| Identity review | Future enhancement | No models, endpoints, state, or UI for candidate review | Reference supports candidate queue/detail, confirm/reject/reopen, device profile, bindings/revocation, and split. These are administrator/destructive workflows. |
| Identity SSE events | Future enhancement | Unknown identity events are safely ignored | Reference adds `identity.candidate.changed`, `identity.binding.changed`, and `identity.candidate.decided`. Supporting them must remain capability-gated. |
| Rich device event payload | Compatibility gap | Decodes the current PDID/MAC/IP/hostname/confirmation subset and ignores additions | Reference includes canonical/old hostname, old MAC/IP, and payload timestamp. Existing behavior is safe but does not expose them. |
| Explicit event PDID | Compatibility gap | Resolves a device key from payload `pdid`, `target_id`, `new_pdid`, or legacy `device_id` | Reference treats explicit `pdid` as primary and `device_id` as a deprecated PDID alias. Contract tests should protect priority and unknown/global-event no-op behavior. |
| Structured errors | Future enhancement | Preserves a message and coarse error taxonomy | Reference errors can include `error`, `details`, `code`, and `retryable`; adopting them must not alter current classifications or user messaging without approval. |
| Device refresh | Future enhancement | No `POST /api/v1/devices/{pdid}/refresh` dependency | DIS exposes an accepted background-refresh operation. LIAS Android currently talks to the LIAS surface and must not assume this DIS route is proxied. |
| SSE/request resource ceiling | Compatibility hardening | No explicit 1 MiB per-event limit in the Android parser | Reference bounds authenticated request bodies and individual SSE events to 1 MiB. Android should add a tested bound before consuming untrusted oversized frames. |
| URL path encoding | Compatibility hardening | Endpoint helpers interpolate IDs directly | Reference web client encodes path components. Hardening must preserve all existing paths and signatures. |
| Server session/CSRF flow | Not applicable now | Native client uses bearer authentication | Reference browser dashboard uses same-origin session cookies and CSRF. It should not replace native bearer auth without an approved authentication architecture change. |
| Discovery/correlation engine | Not an Android dependency | Server-derived inventory only | Reference runs provider orchestration, debouncing, enrichment, probabilistic identity, SQLite, and resource budgets. Porting it on-device would be a new product architecture. |

## Migration path for future engine enhancements

Every phase is optional, additive, independently releasable, and must retain the
legacy path. Stop for approval where a phase crosses the change-control gate.

### Phase 0 — Freeze and test the current contract

1. Keep the current endpoint and DTO tests as golden gates.
2. Add fixtures proving that an older device payload still decodes, future
   unknown fields/events are ignored, PDID wins over internal `device_id`, and
   unknown/global events perform no inventory, enforcement, storage, or network
   work.
3. Add integration coverage for existing REST refresh plus SSE replay before
   optimizing synchronization.

This phase must not change production behavior or public declarations.

### Phase 1 — Add internal capability discovery

Add internal serializable capability/upstream DTOs and an internal, cached
capability read. Absence, `401`, `404`, malformed data, or an unknown feature
must preserve today's legacy behavior. Feature strings are opt-in flags, not a
server-version ordering scheme. Require approval if the new types or methods
would be public or if startup/connection ownership changes.

### Phase 2 — Adopt snapshot as an optional synchronization path

Only use `/api/v1/snapshot` when `snapshot_v1` is advertised. Decode into a
temporary object, validate it completely, and publish one revision-consistent
state update. Preserve the existing multi-request `refreshAll()` path for older
servers and as fallback. Support ETag/304 without treating an empty 304 as a
typed serialization failure. Because this changes synchronization architecture
and request patterns, obtain approval before implementation.

### Phase 3 — Decode additive identity facts without changing meaning

Internally decode assurance, probability, ambiguity, and richer event payloads
with safe defaults. Do not map `l7`/`bia` to `stable`/`verified` or equate a
probability with proof unless product semantics are explicitly approved. Keep
PDID as the public key and retain all display fallbacks. Public model additions
or UI label changes require approval.

### Phase 4 — Add read-only identity review

When the matching capabilities are advertised, add paged candidate/profile
reads behind a disabled-by-default feature boundary. Treat correlation scores
as evidence, not proof. Unknown or absent capabilities hide the feature and
leave the rest of the app unchanged. State ownership or navigation/public-route
changes require approval.

### Phase 5 — Add identity mutations with destructive safeguards

Add binding, revoke, confirm, reject, reopen, and split one operation at a time.
Send the reference concurrency guards (`expected_source_pdid`,
`expected_target_pdid`, and `expected_updated_at`) and handle stale/conflicting
decisions without optimistic identity rewrites. Merge and split require explicit
destructive confirmation, clear impact text, server reconciliation, and
regression coverage for cancellation/failure. This phase always requires
product/API approval before implementation.

### Phase 6 — Subscribe to optional identity events

Register explicit event names only after their corresponding capability is
available. Events invalidate/refetch authoritative identity state; they do not
perform local merges, splits, or policy-key rewrites. Preserve the unknown-event
no-op rule and current replay cursor semantics.

### Phase 7 — Resource and transport hardening

Add bounded SSE frame handling, encoded dynamic path segments, structured error
metadata, and retry hints as behavior-preserving internals. Maintain the current
`ApiResult` taxonomy and user-facing recovery behavior unless separately
approved. Test malformed, oversized, replayed, reordered, and additive inputs.

## Acceptance gates for any migration

A future enhancement is ready only when all applicable statements are true:

- current Android unit/contract tests still pass on the legacy path;
- the app operates against a server with no capabilities endpoint;
- unknown fields, feature strings, and events are harmless;
- no optional endpoint is called before its feature is advertised;
- PDID remains the public policy/navigation/cache key;
- server identity and effective access remain authoritative;
- authentication, conflict, transport, HTTP, and serialization failures remain
  distinguishable;
- replay state persists only for the same logical server and never crosses to a
  different server;
- destructive identity and configuration operations are explicit, reversible
  where the server permits, and never inferred from an event;
- performance/resource gains are measured without removing the stable fallback;
- any architecture or public API change has recorded explicit approval.

## Reference sources used for this contract

Android sources: version catalog and app build file; `Endpoints`, `ApiTypes`,
`ApiResult`, `LiasApiClient`, `LiasSseClient`, `EventConstants`; canonical models
and identity presentation; repository/mutation modules; policy, schedule,
configuration, navigation, diagnostics, and regression tests.

Reference sources: `docs/API_COMPATIBILITY.md`,
`docs/DIS_VALIDATION_AND_RESOURCE_BUDGETS.md`, shared API/models and golden
fixtures, LIAS/DIS route registration, synchronization and identity gateways,
SSE brokers, and compatibility/identity/resource tests.
