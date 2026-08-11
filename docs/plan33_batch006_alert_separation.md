# Plan 3.3 Batch 006 — Alert separation

Plan 3.3 does not convert compact confirmations into bottom sheets.

`HigAlertDialog` remains a centered Compose `Dialog` with Cupertino controls.
This applies to destructive confirmations, identity decisions, editable
confirmation prompts, and similar compact yes/no admin decisions.

This preserves the distinction:
- long-form/action/picker/detail -> bottom sheet;
- confirmation/alert -> centered dialog.
