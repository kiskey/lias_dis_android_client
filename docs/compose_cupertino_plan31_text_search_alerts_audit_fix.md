# Plan 3.1 text/search/alert audit fix

Status: verifier correction  
Date: 2026-08-10

## Diagnosis

The previous static audit was intentionally strict but too broad.

False positives:

1. `HigLargeTitleScaffold.kt` imports `androidx.compose.ui.draw.clip`.
   The substring `draw` is not a Canvas-drawn search glyph.

2. `DetailedWeekGrid.kt` uses Canvas for schedule-grid rendering. That is
   unrelated to the Plan 3.1 requirement to remove Canvas-drawn search and clear
   glyphs from the LIAS-owned search adapter.

3. `HigAlertDialog.kt` is already a LIAS-owned Apple-style adapter using
   Cupertino text/buttons. Its internal `Dialog` host is acceptable for editable
   or adaptive dialogs unless a source-specific short confirmation migration to
   `CupertinoAlertDialog` is approved.

## Corrected gate

The corrected gate fails only when:

- `HigField.kt` stops using the maintained fork `CupertinoTextField`;
- `HigField.kt` stops preserving `TextFieldValue` / reconciliation;
- `HigLargeTitleScaffold.kt` search stops using `CursorSafeTextField`;
- search/clear icons stop using maintained-fork Cupertino icons;
- actual `Canvas(...)` calls appear inside `HigLargeTitleScaffold.kt` search;
- old `io.github.alexzhirkevich` references return;
- Material AlertDialog is introduced;
- `BasicTextField` returns to `HigField.kt` or `HigLargeTitleScaffold.kt`.

## Contract boundary

This is a verifier/docs correction only. It does not change LIAS REST, SSE,
PDID, repository ownership, persistence, policy/schedule semantics, or runtime
UI behavior.
