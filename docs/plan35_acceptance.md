# Plan 3.5 — Adaptive Cupertino Sheet Presentation & Readability

## Presentation profiles

- **Compact:** Medium + Large. Used for focused tasks that are immediately
  usable at Medium. Large remains available for accessibility or additional
  context.
- **Picker:** 62% + Large. Reserved for the full-window crash-safe date/time
  picker portal where multi-column wheels need additional initial height.
- **Editor:** Large only. Used where substantial multi-section content would
  otherwise be cramped or hide important context.

## Nested-scroll interaction

LIAS owns sheet interaction semantics in `HigSheets.kt`.

- **ResizeSheet** is the default. It maps to Slanoss
  `PresentationContentInteraction.Resize`, preserving existing nested-scroll
  detent behavior for every existing sheet.
- **ScrollContent** maps to Slanoss
  `PresentationContentInteraction.Scroll`. Nested scrolling remains with the
  child control instead of changing the sheet detent.
- Slanoss `sheetSwipeEnabled` remains enabled. Direct sheet/grabber dragging
  and swipe-down dismissal therefore remain available independently of the
  nested-scroll policy.

## Focused sheet refinements

- **Pause Internet: Compact.** The initial Medium presentation contains the
  device, `1 Hour`, one concise explanatory sentence, and the destructive
  `Pause for 1 Hour` action.
- The redundant infrastructure disclaimer is not repeated in the focused
  Pause confirmation. LIAS still enforces infrastructure immunity.
- **Extend Access: Compact.** It keeps the single Slanoss
  `CupertinoWheelPicker` with 5-minute choices from 5 through 120 minutes.
- Extend Access opts into **ScrollContent**, so scrolling the duration wheel
  does not promote the sheet from Medium to Large.
- Cancel remains leading and Apply remains trailing in the header.
- Extend Access has no quick-pick row, no slider, and no duplicate bottom
  primary button.
- When an extension is active, the remaining time and replacement behavior
  remain visible. `Cancel Extended Access` remains a destructive secondary
  action.
- The crash-safe Date/Time picker portal remains unchanged.
- **Identity Candidate Review: Editor/Large** because its evidence view and
  Merge/Reject/Reopen/Bind/Split actions live in the scrollable detail body.

## Runtime acceptance

On Pixel 6a portrait:

- Pause opens at Compact/Medium.
- `Pause for 1 Hour` is visible without dragging the sheet.
- Pause keeps the fixed 60-minute server contract.
- Extend Access opens at Compact/Medium.
- Extend Access shows one wheel.
- Extend Apply remains visible in the header.
- Scrolling the Extend wheel changes the wheel only and does not promote the
  sheet to Large.
- Direct sheet/grabber dragging can still expand the Compact sheet.
- Swipe-down dismissal still works.
- Extend Access defaults to 30 minutes.
- Extend Access choices remain 5-minute increments from 5 through 120.
- Active extension status and Cancel Extended Access remain available.
- no action overlaps gesture or 3-button navigation;
- Schedule Editor opens directly usable at Large;
- date and time pickers open repeatedly without crash;
- Global Access typography remains readable.

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
