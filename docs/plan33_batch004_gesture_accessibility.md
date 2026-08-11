# Plan 3.3 Batch 004 — Gesture and accessibility

Verified sheet lifecycle:
- swipe-down: Slanoss state;
- outside tap: Slanoss modal style;
- Android Back: LIAS wrapper -> `hide()`;
- Cancel: LIAS animated dismiss provider;
- Done/Save/Confirm: LIAS animated completion provider;
- navigation bar + IME padding retained;
- accessibility pane title retained.

No networking or engine contracts are involved.
