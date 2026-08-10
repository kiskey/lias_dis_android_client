# Compose Cupertino Plan 3.1 — text fields, search, and alerts

Status: implementation/audit slice  
Date: 2026-08-10

## Contract boundary

This is a UI adapter/presentation slice only. It must not change LIAS REST, SSE,
PDID, repository ownership, persistence, authentication, policy semantics,
schedule semantics, or server-authoritative enforcement.

## Approved Plan 3.1 scope

1. Text fields:
   - keep the public `HigField(String, ...)` API;
   - internally use the maintained fork's `CupertinoTextField(TextFieldValue, ...)`
     through LIAS-owned adapter code;
   - preserve caller-owned `String`, cursor selection, IME composition, keyboard
     options/actions, disabled/read-only behavior, and accessibility.

2. Search:
   - keep `HigSearchField(query: String, ...)` as the public app adapter;
   - use maintained-fork Cupertino icons for search/clear affordances;
   - do not use Canvas-drawn search or clear glyphs;
   - use `CursorSafeTextField` unless `CupertinoSearchTextField` is separately
     proven against long-query cursor selection, cancellation, restoration,
     accessibility, and CI.

3. Alerts:
   - only short non-editing confirmations are candidates for Cupertino alert adoption;
   - crowded multi-field forms remain sheets/forms;
   - alert adoption must not change mutation semantics, destructive-operation
     safeguards, or confirmation text meaning;
   - existing LIAS-owned `HigAlertDialog` with an internal Dialog host remains
     acceptable for editable/adaptive dialogs.

## Corrected verifier notes

- `androidx.compose.ui.draw.clip` is not Canvas drawing.
- `DetailedWeekGrid.kt` Canvas is schedule-grid rendering, not search/clear glyph drawing.
- Platform Dialog/Popup references are audited but are not automatic failures.
