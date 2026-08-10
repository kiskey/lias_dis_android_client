# Compose Cupertino Plan 3.1 — text-field polish

Status: implementation slice  
Date: 2026-08-10

## Goal

Make LIAS text-entry surfaces feel closer to Cupertino/iOS grouped forms while
preserving the already-correct cursor-following behavior.

## Contract boundary

This is UI-only. It must not change:

- LIAS REST paths, DTOs, request/response fields, SSE behavior, or error taxonomy;
- PDID identity logic, discovery logic, persistence, settings keys, policy or schedule semantics;
- navigation route grammar;
- LIAS/DIS server/reference code.

## Required technical shape

Screens continue to call LIAS-owned adapters:

```text
screens -> HigField / HigSearchField -> CursorSafeTextField -> CupertinoTextField(TextFieldValue)
```

Do not replace screen call sites with direct raw text-field implementations.

## Required behavior

- `HigField` keeps its existing public `String` API.
- `CursorSafeTextField` keeps `TextFieldValue`, selection, and reconciliation.
- Long single-line values keep cursor/selection stable.
- External state updates clamp selection safely.
- Read-only/clickable fields expose button semantics.
- Disabled fields remain visually and semantically disabled.
- Multiline fields remain supported.

## Visual direction

- Replace uppercase custom label chrome with softer iOS-style form labeling.
- Use grouped form row surfaces.
- Keep minimum 48dp interaction target.
- Use maintained-fork `CupertinoTextField`.
- Search keeps Cupertino `MagnifyingGlass` and `XmarkCircle`.
- No Canvas search/clear glyphs.
- No Material text fields or Material icons.

## Acceptance criteria

- `HigField.kt` imports `com.slapps.cupertino.CupertinoTextField`.
- `CursorSafeTextField` uses `TextFieldValue`.
- `reconcileEditorValue` is present and tested/verified by static gate.
- No `BasicTextField` exists in `HigField.kt` or `HigLargeTitleScaffold.kt`.
- No Material `TextField`, `OutlinedTextField`, or Material icons exist in LIAS app source.
- `HigSearchField` uses `CursorSafeTextField`.
- Search/clear icons come from maintained-fork Cupertino icons.
- No app source under `core/`, `network/`, `repositories/`, or `navigation/` is modified by this slice.
- GitHub Actions compile/test/lint/release package passes.
