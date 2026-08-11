# Compose Cupertino Plan 3.1 — ChevronForward icon adoption

Status: implementation slice  
Date: 2026-08-10

## Goal

Replace temporary text disclosure `›` with real Slanoss Cupertino icon:

```kotlin
CupertinoIcons.Outlined.ChevronForward
```

## Contract boundary

UI-only. It must not change REST/SSE/API contracts, PDID, policy/schedule/extension/pause semantics, persistence, navigation route grammar, or card tap behavior.

## Acceptance criteria

- `HomeScreen.kt` imports `CupertinoIcon`, `CupertinoIcons`, and `ChevronForward`.
- `DevicesScreen.kt` imports `CupertinoIcon`, `CupertinoIcons`, and `ChevronForward`.
- `text = "›"` does not remain in Home/Devices source.
- No Material icons are introduced.
