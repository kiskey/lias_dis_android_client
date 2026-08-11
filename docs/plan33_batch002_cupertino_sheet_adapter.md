# Plan 3.3 Batch 002 — Cupertino sheet adapter

Verified against:
- LIAS Remote luna `HigSheets.kt`;
- `io.github.schott12521:cupertino:2.3.1`;
- Slanoss 2.3.1 `CupertinoBottomSheet.kt`;
- Slanoss 2.3.1 `CupertinoBottomSheetScaffold.kt`.

This batch fixes the previous motion bug where `AnimatedVisibility` was
permanently `visible = true`.

Slanoss state now owns:
- hidden -> shown animation;
- shown -> hidden animation;
- swipe motion;
- outside-tap hide;
- scrim interpolation.

LIAS owns:
- parent modal removal only after hide;
- Android Back behavior;
- accessibility label;
- app theme integration.

Header Cancel is routed through the animated close path.

Batch 003 will migrate non-header completion actions such as Done/Save/Confirm
that may still invoke parent state removal directly.
