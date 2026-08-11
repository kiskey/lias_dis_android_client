#!/usr/bin/env python3

from pathlib import Path
import sys

IDENTITY = Path(
    "app/src/main/java/com/lias/remote/ui/screens/identity/"
    "IdentityReviewScreen.kt"
).read_text(encoding="utf-8")

B2 = Path(
    "scripts/verify_plan35_batch002.py"
).read_text(encoding="utf-8")

FINAL = Path(
    "scripts/verify_plan35_adaptive_sheet_presentation.py"
).read_text(encoding="utf-8")

fn = "private fun IdentityCandidateSheet("
start = IDENTITY.index(fn)
next_comp = IDENTITY.find("\n@Composable\n", start + len(fn))
segment = (
    IDENTITY[start:next_comp]
    if next_comp != -1
    else IDENTITY[start:]
)

checks = {
    "presentation_import":
        "import com.lias.remote.ui.components.HigSheetPresentation"
        in IDENTITY,

    "identity_candidate_editor":
        "HigSheetPresentation.Editor" in segment,

    "hig_sheet_retained":
        "HigModalSheet(" in segment,

    "scroll_retained":
        ".verticalScroll(" in segment,

    "merge_retained":
        '"Continue to Merge"' in segment,

    "reject_retained":
        '"Reject"' in segment,

    "binding_retained":
        '"Add Verified Binding"' in segment,

    "split_retained":
        '"Split Device Identity"' in segment,

    "batch002_identity_entry":
        "IdentityReviewScreen.kt" in B2
        and '"IdentityCandidateSheet"' in B2,

    "final_classification_identity_entry":
        "classification = {" in FINAL
        and "IdentityReviewScreen.kt" in FINAL
        and '"IdentityCandidateSheet"' in FINAL,

    "final_inventory_identity_entry":
        "expected_sheet_files = {" in FINAL
        and (
            '"app/src/main/java/com/lias/remote/ui/screens/identity/'
            'IdentityReviewScreen.kt",'
        ) in FINAL,
}

bad = [
    name
    for name, ok in checks.items()
    if not ok
]

for name, ok in checks.items():
    print(
        f"{'PASS' if ok else 'FAIL'}: {name}"
    )

if bad:
    print(
        "ERROR: Plan 3.5 Identity Review recovery failed: "
        + ", ".join(bad),
        file=sys.stderr,
    )
    sys.exit(1)

print(
    "PASS: Plan 3.5 Identity Review recovery gate passed."
)
