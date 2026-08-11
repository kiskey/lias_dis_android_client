# Batch 017 field/search verification passed

- HigField uses maintained-fork CupertinoTextField(TextFieldValue).
- CursorSafeTextField preserves cursor/selection and IME composition through TextFieldValue reconciliation.
- HigSearchField uses CursorSafeTextField, Cupertino MagnifyingGlass, Cupertino XmarkCircle, and ImeAction.Search.
- No Canvas or Material icon use detected in field/search adapters.
