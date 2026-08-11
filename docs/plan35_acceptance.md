# Plan 3.5 — Adaptive Cupertino Sheet Presentation & Readability

## Presentation profiles

- Compact: Medium + Large. Used only where the primary action is in the
  header and the body is scrollable.
- Picker: 62% + Large. Used by the full-window crash-safe date/time portal.
- Editor: Large only. Used where critical actions or substantial content
  would otherwise be hidden at Medium.
- Identity Candidate Review: Editor/Large because its evidence view and
  Merge/Reject/Reopen/Bind/Split actions live in the scrollable detail body.

## Runtime acceptance

On Pixel 6a portrait:
- no primary action requires dragging the sheet upward;
- no action overlaps gesture or 3-button navigation;
- Schedule Editor opens directly usable at Large;
- date and time pickers open repeatedly without crash;
- picker Done is visible at initial presentation;
- Global Access title/description/segmented labels are comfortably readable;
- swipe-down, Back, outside-tap and animated completion remain intact.

Repeat with:
- gesture navigation;
- 3-button navigation;
- font scales 1.0, 1.15, 1.30;
- light and dark modes.

## Project boundary

No REST/SSE/DTO/repository/policy/schedule/identity/enforcement semantics change.
