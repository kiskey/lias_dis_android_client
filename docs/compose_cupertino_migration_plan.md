# Compose Cupertino maintained-fork migration plan

Status: analysis and proposed migration only  
Plan identifier: 3.0  
Follow-on UX plan: 3.1 (Cupertino text fields, search, focused forms, and accessibility)  
Audit date: 2026-08-10

## Decision summary

LIAS Remote should evaluate and migrate to the maintained Compose Cupertino
fork, pinned to `io.github.schott12521:*:2.3.1`, before implementing Plan 3.1.
The migration is technically promising because the fork supplies native
Cupertino-styled text-field, search-field, dialog, icon, and bottom-sheet
primitives that can replace parts of the app's custom presentation code.

This is not a two-line dependency replacement. Version 2.3.1 changes the
Kotlin package namespace and was published against a newer Kotlin, Compose,
and Android build baseline. It also introduces transitive runtime-version
changes. Migration must therefore be staged, remotely built, and regression
tested before any screen adopts new behavior.

No LIAS server API, engine contract, repository ownership boundary, public
Android API, navigation route, persisted setting, or user-visible behavior is
changed by Plan 3.0.

## Sources and evidence

- Current Android version catalog: `gradle/libs.versions.toml`
- Current dependency use: `app/build.gradle.kts`
- Stable application contract: `docs/lias_engine_contract.md`
- Maintained fork source and usage: <https://github.com/slanos/compose-cupertino>
- Fork releases: <https://github.com/slanos/compose-cupertino/releases>
- Maven artifact metadata: <https://central.sonatype.com/artifact/io.github.schott12521/cupertino/2.3.1>
- Android Kotlin/AGP requirements: <https://developer.android.com/build/kotlin-support>
- Android Compose BOM guidance: <https://developer.android.com/develop/ui/compose/bom>
- Android Compose BOM mapping: <https://developer.android.com/develop/ui/compose/bom/bom-mapping>

The audit compared the exact original `0.1.0-alpha04` source tag with the
fork's `2.3.1` source tag. The fork README describes it as compatible with
Compose Multiplatform 1.7 and newer, but the actual 2.3.1 publication is built
with Compose Multiplatform 1.8.2 and Kotlin 2.2.0. Published Maven metadata,
not the general README wording, is the compatibility authority for this plan.

## Maintenance assessment

The fork is materially newer than the original alpha dependency and has a
repeatable Maven Central release process. It includes fixes released through
2.3.1. However, it must not be treated as an officially supported Apple or
JetBrains library:

- the repository explicitly says it is not officially supported;
- the latest tag and source commit are 2.3.1 from 2025-08-19;
- as of this audit, that is roughly one year without a newer tagged release;
- the API still contains experimental components.

Recommendation: use an exact version pin, retain LIAS-owned adapter
components, and never expose fork types through LIAS public interfaces. This
makes a future fork update or replacement containable.

## Baseline comparison

| Area | Current LIAS Android | Fork 2.3.1 publication | Migration consequence |
| --- | --- | --- | --- |
| Coordinates | `io.github.schott12521` | `io.github.schott12521` | Replace both Cupertino catalog entries atomically |
| Kotlin package | `io.github.schott12521.cupertino` | `com.slapps.cupertino` | Rewrite imports in 43 Android source files |
| Cupertino version | `0.1.0-alpha04` | `2.3.1` | Large version jump; no dynamic version |
| Kotlin | 2.0.20 | 2.2.0 | Upgrade Kotlin and Compose compiler plugin together |
| AGP | 8.9.3 | Kotlin 2.2 requires AGP 8.10+ | Upgrade AGP before consuming the fork |
| Gradle | 8.11.1 | Fork was built with Gradle/AGP 8.12 | Keep only if compatible with selected AGP; otherwise raise minimally |
| Compose | Android BOM 2024.09.00 | Compose Multiplatform 1.8.2 | Align direct AndroidX Compose libraries to compatible 1.8.x versions |
| Serialization runtime | 1.6.3 | 1.7.3 transitive | Align to 1.7.3 and rerun all wire/JSON contract tests |
| Date/time | no direct dependency | `kotlinx-datetime` 0.7.1 transitive | Accept only as a transitive UI-library dependency; do not use in engine code |
| Atomicfu | no direct dependency | 0.23.2 transitive | Dependency-graph and packaging validation required |
| Minimum Android | 26 | fork minimum 21 | LIAS minimum remains 26; no device-support regression |
| Java target | 17 | fork bytecode target 11 | Compatible; LIAS remains Java/JVM 17 |

Android's compatibility table requires AGP 8.10 or newer for Kotlin 2.2.
Consequently, keeping AGP 8.9.3 while moving to the fork is not an approved
configuration. The fork repository's AGP 8.12 is a producer build choice, not
automatically a consumer requirement; LIAS should select the smallest stable
AGP/Gradle pair that supports Kotlin 2.2 and compileSdk 36.

The exact Android Compose BOM version must be selected from the official BOM
mapping during implementation. It must resolve the app's direct Compose
runtime, UI, Foundation, graphics, tooling, and test artifacts compatibly with
the fork's Compose Multiplatform 1.8.2 publication. The BOM version must not be
guessed or allowed to drift independently.

## Current LIAS Cupertino usage audit

The Android application currently imports Compose Cupertino from 43 Kotlin
files. The dependency is broad but concentrated around a small API surface:

| Current element | Approximate app usage | Fork status | Migration disposition |
| --- | ---: | --- | --- |
| `CupertinoText` | 40 importing files / 238 references | Source-compatible after package change | Mechanical import migration |
| `CupertinoIcon` and `CupertinoIcons` | 7 and 9 importing files | Available | Mechanical import migration plus icon validation |
| Extended outlined icons | Checkmark, Clock, warning, Gear, House, iPhone, Lock, Pencil, Shield, Trash | All exact names exist in 2.3.1 | Mechanical import migration |
| `CupertinoButton` and defaults | 3 importing files | Available; fork includes indication fix | Migrate, then validate press/disabled/destructive states |
| `CupertinoSwitch` | 3 importing files | Available; drag, hover, and haptic implementation changed | Migrate with interaction regression tests |
| `CupertinoSlider` | 2 importing files | Available; internal interaction implementation changed | Migrate with range/value/gesture tests |
| `CupertinoScaffold` | 3 importing files | Available | Mechanical migration; verify insets and bars visually |
| `CupertinoTopAppBar` | 1 importing file | Available | Mechanical migration; verify title/inset behavior |
| Segmented control and tab | 1 importing file | Available | Mechanical migration; verify selection semantics |
| `CupertinoSection` | 1 importing file | Available | Mechanical migration; verify grouped-list geometry |
| Theme and color schemes | central `Theme.kt` | Available under new package | Preserve LIAS color tokens and dark/light behavior |

The fork retains the primary source-level signatures used by LIAS. Most direct
call sites should require only namespace changes. That is not equivalent to
behavioral compatibility: switch dragging/haptics, slider gestures, dialog
windowing, scaffold insets, and bottom-sheet presentation changed internally
and require explicit validation.

## Fork capabilities relevant to LIAS

### Text fields

The fork supplies both:

- `CupertinoTextField(String, ...)`;
- `CupertinoTextField(TextFieldValue, ...)`;
- `CupertinoBorderedTextField(String, ...)`;
- `CupertinoBorderedTextField(TextFieldValue, ...)`.

The `TextFieldValue` overload preserves caller-controlled selection, cursor,
and IME composition. It also supports placeholders, leading/trailing content,
error state, visual transformation, keyboard options/actions, single-line and
multiline modes, minimum/maximum lines, alignment, colors, shape, padding,
and border width. This directly addresses the technical core of Plan 3.1 and
means LIAS should not maintain its own `BasicTextField` editing engine after
the fork is proven.

LIAS should retain the public `HigField(String, ...)` interface. Internally,
the adapter can reconcile the screen-owned `String` with `TextFieldValue` and
delegate rendering/editing to `CupertinoTextField` or
`CupertinoBorderedTextField`. This preserves call sites and prevents a fork
type from becoming a public application contract.

### Search field and official library symbols

`CupertinoSearchTextField` provides iOS-style search presentation, the
`MagnifyingGlass` library icon, focus-aware Cancel behavior, keyboard Search
action, and optional leading/trailing slots. It is currently marked
`ExperimentalCupertinoApi` and exposes a `String` overload, not a public
`TextFieldValue` overload in 2.3.1.

Plan 3.1 must therefore compare two implementations on device:

1. use `CupertinoSearchTextField` if long-query cursor selection, restoration,
   focus, cancellation, list-scroll collapse, and accessibility all pass;
2. otherwise use `CupertinoTextField(TextFieldValue)` with the fork's
   `MagnifyingGlass` and `XmarkCircle` vectors and LIAS-owned search layout.

In both cases, remove Canvas-drawn search and clear glyphs. Version 2.3.1
contains `MagnifyingGlass`, outlined/filled `XmarkCircle`, and every icon
currently referenced by LIAS.

### Alerts and action sheets

The fork changed Cupertino alerts/action sheets from Popup-based presentation
to Compose `Dialog`, adding system-bar and IME handling. It supplies
`CupertinoAlertDialog` and `CupertinoActionSheet` with default, cancel, and
destructive action styles.

Use these only for short decisions and confirmations. The library does not
turn a crowded multi-field alert into an HIG-appropriate form. Identity
Decision, Binding, Split, and other long editing remain candidates for a
focused sheet under Plan 3.1.

### Bottom sheets

The fork supplies `CupertinoBottomSheetScaffold`, `CupertinoBottomSheetContent`,
`CupertinoSheetState`, modal/fullscreen presentation styles, fractional,
height, medium, and large detents, background-interaction control, outside-tap
dismissal, and nested-scroll interaction. Releases also added control over
background content scaling and an Android bottom-sheet example/fix.

This is more capable than the current `HigModalSheet`, but adoption is not a
drop-in replacement. The fork's sheet is scaffold/state-hosted, while current
LIAS sheets are direct overlay composables. Replacing the host is a structural
UI change. Plan 3.0 migrates the dependency without changing sheet ownership;
Plan 3.1 can separately approve and introduce a centralized Cupertino sheet
host for focused Identity Review forms.

### Other capabilities

The fork also contains updated swipe-box interactions, corrected date-picker
scrolling, pickers, date/time pickers, dropdown menus, checkboxes, navigation
bars, activity indicators, and adaptive/native/Decompose modules.

For this Android application:

- do not add `cupertino-adaptive`; LIAS intentionally presents a consistent
  Cupertino/HIG experience on Android rather than switching to Material;
- do not add `cupertino-native`; UIKit wrappers provide no Android benefit;
- do not add `cupertino-decompose`; LIAS uses Navigation Compose and changing
  navigation architecture is out of scope;
- do not adopt new date/time pickers, swipe actions, or dropdowns during the
  dependency migration; consider each later as a separate UX enhancement.

Only `cupertino` and `cupertino-icons-extended` are in migration scope.

## Migration risks and controls

| Risk | Impact | Control |
| --- | --- | --- |
| Kotlin 2.2 metadata with Kotlin 2.0 compiler | Compilation failure | Upgrade Kotlin and Compose compiler plugin first |
| Kotlin 2.2 with AGP 8.9.3 | Unsupported D8/R8 combination | Move to AGP 8.10+ using official compatibility table |
| Mixed AndroidX and JetBrains Compose versions | Resolution or runtime linkage failure | Inspect Gradle dependency graph and align all Compose artifacts to 1.8.x |
| Serialization 1.6.3 versus 1.7.3 | Silent runtime uplift affecting JSON | Align deliberately and run existing REST/SSE/serialization contract tests |
| Package namespace replacement | Broad compile break | Atomic mechanical import change; prohibit mixed old/new packages |
| Switch/slider gesture changes | Duplicate actions or changed feel | Interaction tests and physical-device validation |
| Dialog implementation change | Insets, focus, or dismissal regression | Test back, outside tap, rotation, IME, and process restoration |
| Experimental search API | Future churn or cursor/accessibility defect | Hide behind `HigSearchField`; retain nonexperimental fallback |
| Bottom-sheet host requirement | Navigation/presentation regression | Defer host adoption to separately approved Plan 3.1 |
| Unofficial, low-cadence dependency | Maintenance/supply risk | Exact pin, dependency verification, adapter boundary, documented rollback |
| SF Symbols licensing | Distribution restriction | Preserve current usage scope and Apple license review; add no unrelated symbol redistribution |

## Proposed migration sequence

Each phase has a remote GitHub Actions gate because the project has no local
Android build environment. A failed phase is repaired before proceeding; no
later UX adoption is mixed into migration fixes.

### Phase 0: freeze the compatibility baseline

1. Record the last known passing Android commit and Actions workflow run.
2. Preserve screenshots for Home, Devices, Schedules, Rules, Settings,
   Connection, Identity Review, alerts, and sheets in light/dark modes.
3. Run the existing unit/contract suite unchanged.
4. Capture `debugRuntimeClasspath` and `releaseRuntimeClasspath` dependency
   reports from CI for before/after comparison.
5. Confirm the working tree contains no unrelated user changes before edits.

Exit gate: current main branch builds and tests successfully.

### Phase 1: toolchain alignment without Cupertino replacement

1. Upgrade Kotlin and the Compose compiler plugin together to 2.2.0.
2. Upgrade AGP from 8.9.3 to a stable 8.10-or-newer version compatible with
   Kotlin 2.2 and compileSdk 36.
3. Keep Java/JVM 17, minSdk 26, targetSdk 35, and compileSdk 36 unchanged.
4. Keep Gradle 8.11.1 if supported by the chosen AGP; otherwise raise it only
   to that AGP's documented minimum.
5. Select an official Android Compose BOM whose UI/Foundation/runtime set is
   compatible with Compose 1.8.2.
6. Align `kotlinx-serialization-json` to 1.7.3 because the fork will introduce
   that runtime transitively.
7. Run compilation, lint, unit/contract tests, and dependency reports.

Exit gate: the current original Cupertino dependency and existing UI build and
behave unchanged on the new toolchain. This isolates toolchain failures from
fork API failures.

### Phase 2: dependency and namespace migration only

1. Change the two catalog coordinates to `io.github.schott12521` and pin
   version `2.3.1`.
2. Replace every `io.github.schott12521.cupertino...` import with the
   corresponding `com.slapps.cupertino...` import.
3. Confirm no old Cupertino group or package remains in source, lockfiles, or
   resolved runtime dependencies.
4. Make only the minimum source adjustments demanded by changed signatures.
5. Do not adopt new fields, search, alerts, sheets, pickers, swipe actions, or
   navigation behavior in this phase.
6. Run dependency resolution reports and check for duplicate Compose classes,
   forced versions, dependency downgrades, and unexpected modules.
7. Run debug/release compilation, lint, unit/contract tests, and release
   shrinking/packaging.

Exit gate: same UI and behavior, new dependency only, complete remote build.

### Phase 3: behavioral compatibility validation

Validate on Pixel 6a or an equivalent API-level device/emulator:

- app startup, configuration restore, and dark/light theme;
- all bottom navigation destinations and back behavior;
- Network Access switch and every other switch, including tap and drag;
- sliders and schedule controls at limits and intermediate values;
- Home active protections and restricted-device cards;
- alerts: confirm, cancel, destructive action, outside tap, system back;
- existing sheets: open, dismiss, rotate, keyboard display, and long content;
- large font, landscape, TalkBack traversal, and touch target behavior;
- no changes to REST payloads, SSE handling, policy authority, or persistence.

Exit gate: migration acceptance matrix passes without a LIAS behavior change.

### Phase 4: approve and implement Plan 3.1

Only after Phase 3 passes:

1. replace the custom `BasicTextField` rendering inside `HigField` with the
   fork's `CupertinoTextField(TextFieldValue)` or bordered variant;
2. replace Canvas search/clear symbols with fork vectors;
3. evaluate the experimental `CupertinoSearchTextField` against the stable
   `CupertinoTextField`-based fallback;
4. adopt `CupertinoAlertDialog` for short non-editable confirmations;
5. seek explicit structural approval before centralizing bottom-sheet hosting
   and moving Identity Review forms into focused Cupertino sheets;
6. complete the Plan 3.1 accessibility and physical-device validation matrix.

## Expected file scope during implementation

Plan 3.0 is expected to modify only Android build configuration, Cupertino
imports, tests, and this documentation set. Likely files include:

- `gradle/libs.versions.toml`
- root and app Gradle configuration if required by the selected toolchain
- the 43 Kotlin files importing the original Cupertino package
- dependency/build verification tests or CI reporting configuration
- `docs/lias_engine_contract.md` only after the migration is accepted, to
  record the new stable dependency baseline

It must not modify the reference LIAS/DIS repository or any server code.

## Acceptance criteria

Plan 3.0 is complete only when all of the following are true:

- GitHub Actions compiles debug and release variants successfully;
- lint and all existing unit/contract tests pass;
- release shrinking/packaging succeeds;
- runtime dependency reports contain only the fork's Cupertino group;
- all Compose dependencies resolve to a reviewed compatible set;
- no engine API, payload, persistence, navigation, or public UI API changed;
- all current Cupertino icons resolve and render;
- switches, sliders, scaffolds, alerts, and existing sheets pass regression
  checks;
- minSdk remains 26 and Java/JVM remains 17;
- the stable baseline in `lias_engine_contract.md` is updated only after the
  migration has passed and is accepted;
- Plan 3.1 remains a separate, reviewable UX adoption change.

## Rollback

Rollback is the complete reversal of the migration commits: restore Kotlin,
AGP, Gradle/BOM/serialization versions as a coherent prior set; restore the
two original Cupertino coordinates; and restore the original import namespace.
Do not attempt a mixed rollback in which old and new Cupertino artifacts or
packages coexist.

Because LIAS-owned `Hig*` adapters remain the application-facing boundary,
screen call sites and engine behavior should not require rollback changes.

## Approval gates

This document authorizes no implementation by itself. Separate explicit
approval is required for:

1. Plan 3.0 Phase 1: Kotlin/AGP/Compose/runtime toolchain changes;
2. Plan 3.0 Phase 2: replacing the Cupertino dependency and namespace;
3. Plan 3.1: replacing text/search implementations and alert presentation;
4. Plan 3.1 structural sheet hosting and Identity Review form migration.

