# Plan 3.5 — Adaptive Cupertino Sheet Presentation & Readability

## Presentation profiles

- **Compact:** Medium + Large. Used for focused tasks that are immediately
  usable at Medium. Large remains available for accessibility or additional
  context.
- **Picker:** 62% + Large. Used for the full-window crash-safe date/time
  picker portal where multi-column wheels need additional initial height.
- **Editor:** Large only. Used where substantial multi-section content would
  otherwise be cramped or hide important context.

## Nested-scroll interaction

LIAS owns sheet interaction semantics in `HigSheets.kt`.

- **ResizeSheet** maps to Slanoss
  `PresentationContentInteraction.Resize`. It remains the default for both
  `HigModalSheet` and `HigModalSheetPortal`.
- **ScrollContent** maps to Slanoss
  `PresentationContentInteraction.Scroll`. Nested wheel/list scrolling stays
  with content instead of changing the sheet detent.
- Slanoss `sheetSwipeEnabled` remains enabled. Direct sheet/grabber dragging
  and swipe-down dismissal therefore remain available independently of the
  nested-scroll policy.

## Focused sheet refinements

- **Pause Internet: Compact.** The initial Medium presentation contains the
  device, `1 Hour`, one concise explanatory sentence, and the destructive
  `Pause for 1 Hour` action.
- **Extend Access: Compact.** It keeps the single Slanoss
  `CupertinoWheelPicker` with 5-minute choices from 5 through 120 minutes.
- Extend Access opts into **ScrollContent**, so scrolling the duration wheel
  does not promote the sheet from Medium to Large.
- **Schedule Date/Time Picker: Picker.** The full-window
  `HigModalSheetPortal` remains the presentation boundary and continues to
  use the 62% + Large Picker detents.
- The schedule picker portal call now opts into **ScrollContent**. Scrolling
  the hour/minute wheels or Slanoss wheel-style date picker must not promote
  or demote the sheet.
- The time picker still uses its bounded snapping `LazyColumn` wheels.
- The date picker still uses the LIAS `HigDatePicker` adapter over Slanoss
  `CupertinoDatePicker` with `DatePickerStyle.Wheel()`.
- The focused picker body's `verticalScroll` remains as overflow/accessibility
  fallback; it is not used to resize sheet detents.
- Direct sheet/grabber drag remains available to deliberately expand the
  Picker sheet to Large.
- The full-window Dialog portal, animated completion ordering, navigation-bar
  insets, and `YYYY-MM-DD` / `HH:mm` wire formats remain unchanged.
- **Identity Candidate Review: Editor/Large** remains unchanged.

## Dismissal responsiveness

- Shared `HigSheetHeader` **Cancel** is an immediate, guarded parent-dismiss
  action. It does not wait for the Slanoss bottom-sheet hide tween.
- The immediate Cancel path is blocked while an animated completion is already
  in flight, preventing Cancel from racing Save/Done/Apply/Confirm.
- Back, outside-tap and swipe-down continue to use the Slanoss sheet lifecycle.
- Non-header uses of `rememberHigAnimatedDismiss` remain animated.
- Save/Done/Apply/Confirm keep strict ordering:
  `hide animation -> action -> parent cleanup`.
- Global Access remains a native `CupertinoActionSheet` and retains its
  existing 150 ms exit handling.
- No artificial Cancel delay, debounce, network work, or hardware-specific
  workaround is introduced.

## Runtime acceptance

- Header Cancel removes a standard LIAS sheet immediately without waiting for
  the full Slanoss bottom-sheet hide animation.
- Rapid repeated Cancel taps deliver parent dismissal at most once.
- Cancel does not interrupt an already-started Save/Done/Apply completion.
- Back and swipe-down still visibly animate the sheet.
- Save/Done/Apply still complete only after their existing hide animation.

On Pixel 6a portrait:

- Pause opens at Compact/Medium and `Pause for 1 Hour` is immediately visible.
- Extend Access opens at Compact/Medium.
- Scrolling the Extend wheel changes the wheel only and does not change the
  sheet detent.
- Schedule date picker opens at Picker/62%.
- Scrolling day/month/year wheels changes the wheel selection without moving
  the sheet to another detent.
- Schedule time picker opens at Picker/62%.
- Scrolling hour/minute wheels changes the wheel selection without moving the
  sheet to another detent.
- Direct sheet/grabber dragging can still expand Picker to Large.
- Swipe-down dismissal still works.
- Date/time Done actions remain visible and complete using animated ordering.
- Date/time pickers can be opened repeatedly without the nested-sheet crash.
- no action overlaps gesture or 3-button navigation.

Repeat with:

- gesture navigation;
- 3-button navigation;
- font scales 1.0, 1.15, 1.30;
- light and dark modes.

## Project boundary

No REST/SSE/DTO/repository/policy/schedule/identity/enforcement semantics change.

The schedule wire formats remain `YYYY-MM-DD` and `HH:mm`.
