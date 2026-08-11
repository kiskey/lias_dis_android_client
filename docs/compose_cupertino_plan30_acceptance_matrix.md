# Compose Cupertino Plan 3.0 acceptance matrix

Status: validation checklist pending CI and device review  
Version: 30.0.0  
Scope: Plan 3.0 dependency and namespace migration only

## Contract boundary

Plan 3.0 is accepted only if the Android app keeps the existing LIAS contract:

- no LIAS server REST route changes;
- no SSE framing, replay cursor, event interpretation, or authentication change;
- no repository ownership or state-flow ownership change;
- no persisted settings change;
- no navigation route change;
- no public Kotlin application API change;
- no policy, schedule, PDID, identity, or enforcement authority change;
- no new DIS, nftables, nmap, Avahi, netlink, SQLite, Pi-hole, DHCP, or Go-runtime dependency in the Android app.

## Static migration gate

Run from repository root:

```bash
scripts/verify_cupertino_plan30.sh
```

Expected result:

- old package/group `io.github.alexzhirkevich` is absent;
- maintained fork package `com.slapps.cupertino` is present in Android source;
- maintained fork Maven group `io.github.schott12521` is used for both Cupertino artifacts;
- no `cupertino-adaptive`, `cupertino-native`, or `cupertino-decompose` dependency is added;
- minSdk remains 26;
- compileSdk remains 36;
- targetSdk remains 35;
- Gradle wrapper remains 8.11.1;
- Java and Kotlin JVM target remain 17.

## Gradle gate

Run from repository root:

```bash
scripts/run_cupertino_migration_gate.sh
```

Required result:

- `:app:dependencies --configuration debugRuntimeClasspath` completes;
- `:app:dependencies --configuration releaseRuntimeClasspath` completes;
- debug and release runtime dependency reports contain `io.github.schott12521`;
- debug and release runtime dependency reports do not contain `io.github.alexzhirkevich`;
- `:app:compileDebugKotlin` passes;
- `:app:compileReleaseKotlin` passes;
- `:app:testDebugUnitTest` passes;
- `:app:lintDebug` passes;
- `:app:assembleRelease` passes.

## Runtime smoke validation

Validate on Pixel 6a or equivalent API-level emulator/device.

| Area | Required result | Status |
| --- | --- | --- |
| Cold start | App opens to the same connected/disconnected destination as before migration | Pending |
| Recreate process/activity | Existing settings and pending deep link behavior remain unchanged | Pending |
| Dark/light/system theme | LIAS colors, status bar, navigation bar, and backgrounds match previous behavior | Pending |
| Bottom navigation | Home, Devices, Schedules, Rules, and Settings still navigate correctly | Pending |
| Back behavior | Existing back-stack behavior remains unchanged | Pending |
| Network Access switch | Tap and drag behavior work without duplicate actions | Pending |
| Other switches | Device/tag/policy switches retain previous semantics | Pending |
| Sliders | Values at min, max, and midpoints behave correctly | Pending |
| Alerts | Confirm, cancel, destructive, outside tap, and back behavior remain unchanged | Pending |
| Existing sheets | Open, dismiss, rotate, keyboard display, and long content remain usable | Pending |
| Large font | Layout remains readable and touch targets remain usable | Pending |
| Landscape | No blocking layout regression in major screens | Pending |
| TalkBack | Traversal order and labels remain acceptable | Pending |
| REST payloads | No request/response shape changes observed in tests | Pending |
| SSE handling | Existing replay, malformed-event drop, and unknown-event no-op behavior remains unchanged | Pending |

## Acceptance decision

Plan 3.0 may be recorded as accepted only after all applicable items pass:

- GitHub Actions workflow `LIAS Android Cupertino Plan 3.0` is green;
- local or CI dependency reports show only the maintained fork Cupertino group;
- no old Cupertino package remains in source;
- device smoke validation has no behavior regressions;
- release shrinking and packaging succeed;
- no Plan 3.1 text/search/dialog/sheet adoption has been mixed into this migration.

## Explicitly deferred to Plan 3.1

Do not implement these in Plan 3.0:

- replacing custom field internals with `CupertinoTextField`;
- replacing search implementation with `CupertinoSearchTextField`;
- adopting new Cupertino alert presentation for short confirmations;
- centralizing bottom-sheet hosting;
- moving Identity Review flows into new focused sheets;
- adopting pickers, swipe actions, adaptive/native/decompose modules, or navigation architecture changes.
