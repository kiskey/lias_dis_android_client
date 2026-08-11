# Plan 3.5 — Adaptive Cupertino Sheet Presentation & Readability

## Presentation profiles

- **Compact:** Medium + Large. Used for focused tasks that are immediately
  usable at Medium. Large remains available for accessibility or additional
  context.
- **Picker:** 62% + Large. Reserved for the full-window crash-safe date/time
  picker portal where multi-column wheels need additional initial height.
- **Editor:** Large only. Used where substantial multi-section content would
  otherwise be cramped or hide important context.

## Approved focused-sheet refinements

- **Pause Internet: Compact.** It is a short one-hour confirmation with one
  destructive action. It opens at Medium and can still expand to Large.
- **Extend Access: Compact.** It uses a single Slanoss `CupertinoWheelPicker`
  with 5-minute choices from 5 through 120 minutes. Cancel is the leading
  header action and Apply is the trailing header action.
- Extend Access deliberately has no quick-pick button row, no slider, and no
  duplicate bottom primary button.
- When an extension is active, the remaining time and replacement behavior
  remain visible. `Cancel Extended Access` remains a destructive secondary
  action.
- **Identity Candidate Review: Editor/Large** because its evidence view and
  Merge/Reject/Reopen/Bind/Split actions live in the scrollable detail body.

## Runtime acceptance

On Pixel 6a portrait:

- Pause opens at Compact/Medium rather than Large.
- Pause Internet action is visible without dragging the sheet.
- Extend Access opens at Compact/Medium.
- Extend Access shows one wheel, not presets plus slider.
- Extend Access Apply is always visible in the header.
- Extend Access defaults to 30 minutes.
- Extend Access choices are 5-minute increments from 5 through 120.
- Active extension status remains visible and applying replaces the current
  server-managed extension.
- Cancel Extended Access remains available for an active extension.
- no action overlaps gesture or 3-button navigation;
- Schedule Editor opens directly usable at Large;
- date and time pickers open repeatedly without crash;
- picker Done is visible at initial presentation;
- Global Access title/description/segmented labels remain readable;
- swipe-down, Back, outside-tap and animated completion remain intact.

Repeat with:

- gesture navigation;
- 3-button navigation;
- font scales 1.0, 1.15, 1.30;
- light and dark modes.

## Project boundary

No REST/SSE/DTO/repository/policy/schedule/identity/enforcement semantics change.

The LIAS temporary-access API remains authoritative for its `1..120` minute
contract. The Android Extend Access UI intentionally exposes the approved
5-minute-step subset `5, 10, ... 120`.
