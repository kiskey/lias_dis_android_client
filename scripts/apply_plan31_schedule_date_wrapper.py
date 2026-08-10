#!/usr/bin/env python3
from __future__ import annotations
from pathlib import Path
import re

ROOT = Path.cwd()
FILE = ROOT / "app/src/main/java/com/lias/remote/ui/screens/schedules/SchedulePickerSheets.kt"
REPORT = ROOT / "build/plan31/batch036_schedule_date_wrapper_report.md"

def read() -> str:
    if not FILE.exists():
        raise SystemExit(f"ERROR: missing {FILE.relative_to(ROOT)}")
    return FILE.read_text(encoding="utf-8")

def write(text: str) -> None:
    FILE.write_text(text, encoding="utf-8")

def add_import(text: str, imp: str) -> str:
    line = f"import {imp}\n"
    if line in text:
        return text
    imports = list(re.finditer(r"^import .+$", text, flags=re.MULTILINE))
    if not imports:
        raise SystemExit("ERROR: no import block found")
    last = imports[-1]
    return text[:last.end()] + "\n" + line.rstrip("\n") + text[last.end():]

text = read()

if "Plan 3.1 Schedule date wrapper integration" not in text:
    text = text.replace(
        "// - Confirm still emits YYYY-MM-DD.\n//",
        "// - Confirm still emits YYYY-MM-DD.\n//\n"
        "// Plan 3.1 Schedule date wrapper integration:\n"
        "// - ScheduleDatePickerSheet calls HigDatePicker instead of raw CupertinoDatePicker.\n//",
        1,
    )

for imp in [
    "import com.slapps.cupertino.CupertinoDatePicker\n",
    "import com.slapps.cupertino.DatePickerStyle\n",
    "import com.slapps.cupertino.ExperimentalCupertinoApi\n",
    "import com.slapps.cupertino.rememberCupertinoDatePickerState\n",
]:
    text = text.replace(imp, "")

text = add_import(text, "com.lias.remote.ui.components.HigDatePicker")
text = add_import(text, "com.lias.remote.ui.components.HigDatePickerMode")

text = text.replace("@OptIn(ExperimentalCupertinoApi::class)\n@Composable\nfun ScheduleDatePickerSheet(", "@Composable\nfun ScheduleDatePickerSheet(", 1)

start = text.find("@Composable\nfun ScheduleDatePickerSheet(")
end = text.find("\n@Composable\nprivate fun FocusedPickerDialog", start)
if start < 0 or end < 0:
    raise SystemExit("ERROR: could not locate ScheduleDatePickerSheet block")

new_block = '''@Composable
fun ScheduleDatePickerSheet(
    title: String,
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val today =
        remember {
            LocalDate.now()
        }

    val initial =
        remember(
            initialValue
        ) {
            runCatching {
                LocalDate.parse(
                    initialValue
                )
            }
                .getOrDefault(
                    today
                )
        }

    val firstYear =
        min(
            today.year - 2,
            initial.year - 1
        )

    val lastYear =
        max(
            today.year + 10,
            initial.year + 1
        )

    val initialMillis =
        remember(
            initial
        ) {
            initial
                .atStartOfDay()
                .toInstant(
                    ZoneOffset.UTC
                )
                .toEpochMilli()
        }

    var selectedDateMillis by
        remember(
            initialMillis
        ) {
            mutableStateOf(
                initialMillis
            )
        }

    FocusedPickerDialog(
        title = title,
        onDismiss = onDismiss
    ) {
        HigDatePicker(
            selectedDateMillis =
                initialMillis,
            onDateSelected = {
                selectedDateMillis =
                    it
            },
            yearRange =
                firstYear..lastYear,
            mode =
                HigDatePickerMode.Wheel,
            modifier =
                Modifier.fillMaxWidth()
        )

        val selectedDate =
            remember(
                selectedDateMillis
            ) {
                Instant
                    .ofEpochMilli(
                        selectedDateMillis
                    )
                    .atZone(
                        ZoneOffset.UTC
                    )
                    .toLocalDate()
            }

        CupertinoText(
            text =
                selectedDate.toString(),
            style =
                HigTypography.headline,
            color =
                LiasThemeColors.secondaryLabel,
            textAlign =
                TextAlign.Center,
            modifier =
                Modifier.fillMaxWidth()
        )

        HigButton(
            text = "Done",
            onClick = {
                onConfirm(
                    Instant
                        .ofEpochMilli(
                            selectedDateMillis
                        )
                        .atZone(
                            ZoneOffset.UTC
                        )
                        .toLocalDate().toString()
                )
            },
            style =
                HigButtonStyle.Primary,
            modifier =
                Modifier.fillMaxWidth()
        )
    }
}
'''

text = text[:start] + new_block + text[end:]
write(text)

text = read()
start = text.find("fun ScheduleDatePickerSheet(")
end = text.find("@Composable\nprivate fun FocusedPickerDialog", start)
date_block = text[start:end] if start >= 0 and end > start else ""

problems = []
for token in [
    "Plan 3.1 Schedule date wrapper integration",
    "import com.lias.remote.ui.components.HigDatePicker",
    "import com.lias.remote.ui.components.HigDatePickerMode",
    "HigDatePicker(",
    "HigDatePickerMode.Wheel",
    ".toLocalDate().toString()",
]:
    if token not in text:
        problems.append(f"missing {token}")

for forbidden in [
    "CupertinoDatePicker(",
    "DatePickerStyle.Wheel()",
    "rememberCupertinoDatePickerState(",
    "ExperimentalCupertinoApi",
    "buildList",
    "cursor.plusDays(1)",
    "TextWheel(",
]:
    if forbidden in date_block or (forbidden == "ExperimentalCupertinoApi" and forbidden in text):
        problems.append(f"forbidden raw/sequential token remains: {forbidden}")

REPORT.parent.mkdir(parents=True, exist_ok=True)
if problems:
    REPORT.write_text("# Batch 036 failed\n\n" + "\n".join(f"- {p}" for p in problems) + "\n", encoding="utf-8")
    raise SystemExit(f"ERROR: Batch 036 failed. See {REPORT}")

REPORT.write_text(
    "# Batch 036 passed\n\n"
    "- ScheduleDatePickerSheet now uses HigDatePicker.\n"
    "- Wheel mode preserved through HigDatePickerMode.Wheel.\n"
    "- Confirm output remains YYYY-MM-DD.\n",
    encoding="utf-8",
)
print(f"PASS: Batch 036 applied. Report: {REPORT}")
