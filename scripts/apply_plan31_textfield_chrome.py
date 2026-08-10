#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path

ROOT = Path.cwd()
FILE = ROOT / "app/src/main/java/com/lias/remote/ui/components/HigField.kt"
REPORT = ROOT / "build/plan31/batch023_textfield_chrome_report.md"

def read() -> str:
    if not FILE.exists():
        raise SystemExit(f"ERROR: missing {FILE.relative_to(ROOT)}")
    return FILE.read_text(encoding="utf-8")

def write(text: str) -> None:
    FILE.write_text(text, encoding="utf-8")

def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"ERROR: expected one match for {label}, found {count}")
    return text.replace(old, new, 1)

text = read()

if "Plan 3.1 text-field polish" in text:
    print("Batch 023 already applied.")
else:
    text = text.replace(
        "//   Unified LIAS form-field surface.\n//",
        "//   Unified LIAS form-field surface.\n//\n// Plan 3.1 text-field polish:\n//   - Uses LIAS-owned iOS grouped-form chrome.\n//   - Keeps CursorSafeTextField -> CupertinoTextField(TextFieldValue).\n//   - Preserves cursor, selection, IME composition, and public String API.\n//",
        1,
    )

    old_column = """    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(
                    min =
                        56.dp
                )
                .clip(
                    RoundedCornerShape(
                        10.dp
                    )
                )
                .background(
                    LiasThemeColors
                        .tertiaryBackground
                )
                .then(
                    accessibilityModifier
                )
                .padding(
                    horizontal =
                        14.dp,
                    vertical =
                        8.dp
                )
    ) {

        CupertinoText(
            text =
                label.uppercase(),
            style =
                HigTypography.caption,
            color =
                LiasThemeColors
                    .tertiaryLabel
        )

        Spacer(
            modifier =
                Modifier.height(
                    2.dp
                )
        )

        CursorSafeTextField(
            value =
                value,
            onValueChange =
                onValueChange,
            placeholder = {

                if (
                    placeholder
                        .isNotBlank()
                ) {

                    CupertinoText(
                        text =
                            placeholder,
                        style =
                            HigTypography.body,
                        color =
                            LiasThemeColors
                                .tertiaryLabel
                    )
                }
            },
            enabled =
                enabled &&
                    onClick == null,
            visualTransformation =
                visualTransformation,
            keyboardOptions =
                keyboardOptions,
            keyboardActions =
                keyboardActions,
            singleLine =
                singleLine,
            minLines =
                minLines,
            maxLines =
                maxLines,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(
                        min =
                            44.dp
                    )
        )
    }
"""

    new_column = """    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(
                    min =
                        if (singleLine) {
                            52.dp
                        } else {
                            84.dp
                        }
                )
                .clip(
                    RoundedCornerShape(
                        14.dp
                    )
                )
                .background(
                    if (enabled) {
                        LiasThemeColors
                            .secondaryBackground
                    } else {
                        LiasThemeColors
                            .tertiaryBackground
                    }
                )
                .then(
                    accessibilityModifier
                )
                .padding(
                    horizontal =
                        14.dp,
                    vertical =
                        if (singleLine) {
                            9.dp
                        } else {
                            11.dp
                        }
                )
    ) {

        CupertinoText(
            text =
                label,
            style =
                HigTypography.subheadline,
            color =
                if (enabled) {
                    LiasThemeColors
                        .secondaryLabel
                } else {
                    LiasThemeColors
                        .tertiaryLabel
                }
        )

        Spacer(
            modifier =
                Modifier.height(
                    if (singleLine) {
                        3.dp
                    } else {
                        6.dp
                    }
                )
        )

        CursorSafeTextField(
            value =
                value,
            onValueChange =
                onValueChange,
            placeholder = {

                if (
                    placeholder
                        .isNotBlank()
                ) {

                    CupertinoText(
                        text =
                            placeholder,
                        style =
                            HigTypography.body,
                        color =
                            LiasThemeColors
                                .tertiaryLabel
                    )
                }
            },
            enabled =
                enabled &&
                    onClick == null,
            visualTransformation =
                visualTransformation,
            keyboardOptions =
                keyboardOptions,
            keyboardActions =
                keyboardActions,
            singleLine =
                singleLine,
            minLines =
                minLines,
            maxLines =
                maxLines,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(
                        min =
                            if (singleLine) {
                                34.dp
                            } else {
                                48.dp
                            }
                    )
        )
    }
"""

    text = replace_once(text, old_column, new_column, "HigConfiguredField visual chrome")
    write(text)

# Static post-checks
text = read()
problems = []
for token in [
    "import com.slapps.cupertino.CupertinoTextField",
    "CursorSafeTextField(",
    "TextFieldValue(",
    "reconcileEditorValue",
    "Plan 3.1 text-field polish",
]:
    if token not in text:
        problems.append(f"missing {token}")

for forbidden in [
    "label.uppercase()",
    "BasicTextField",
    "androidx.compose.material",
]:
    if forbidden in text:
        problems.append(f"forbidden token remains: {forbidden}")

REPORT.parent.mkdir(parents=True, exist_ok=True)
if problems:
    REPORT.write_text(
        "# Batch 023 failed\n\n" + "\n".join(f"- {p}" for p in problems) + "\n",
        encoding="utf-8",
    )
    raise SystemExit(f"ERROR: Batch 023 failed. See {REPORT}")

REPORT.write_text(
    "# Batch 023 passed\n\n"
    "- HigField visual chrome refactored to softer Cupertino grouped-form style.\n"
    "- Public HigField API unchanged.\n"
    "- CursorSafeTextField retained.\n"
    "- CupertinoTextField(TextFieldValue) retained.\n"
    "- reconcileEditorValue retained.\n"
    "- Uppercase custom label chrome removed.\n",
    encoding="utf-8",
)

print(f"PASS: Batch 023 applied. Report: {REPORT}")
