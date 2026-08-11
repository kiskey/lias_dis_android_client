# Plan 3.4 Batch 003 — Back controls and compact alerts

Hierarchical navigation chrome now uses Slanoss 2.3.1
CupertinoNavigateBackButton.

Compact HigAlertDialog calls use Slanoss CupertinoAlertDialog with native
Default / Cancel / Destructive action styling.

Editable admin confirmations retain the existing wider custom portal because
the upstream Cupertino alert is fixed-width and has no arbitrary form-content
slot; this preserves Identity Review and other form-based confirmations.
