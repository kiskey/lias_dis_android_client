#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path

ROOT = Path.cwd()
FILE = ROOT / "app/src/main/java/com/lias/remote/ui/screens/schedules/SchedulePickerSheets.kt"
REPORT = ROOT / "build" / "plan31" / "batch032_cupertino_date_wheel_report.md"

def read() -> str:
    if not FILE.exists():
        raise SystemExit(f"ERROR: missing {FILE.relative_to(ROOT)}")
    return FILE.read_text(encoding="utf-8")

def write(text: str) -> None:
    FILE.write_text(text, encoding="utf-8")

text = read()

if "Plan 3.1 Cupertino date wheel" not in text:
    text = text.replace(
        "// - LIAS wire formats remain YYYY-MM-DD and HH:mm.\n//",
        "// - LIAS wire formats remain YYYY-MM-DD and HH:mm.\n//\n"
        "// Plan 3.1 Cupertino date wheel:\n"
        "// - Date sheet uses Slanoss CupertinoDatePicker Wheel style.\n"
        "// - Day, month, and year scroll as separate picker columns.\n"
        "// - Confirm still emits YYYY-MM-DD.\n//",
        1,
    )

anchor = "import com.slapps.cupertino.CupertinoText\n"
if anchor not in text:
    raise SystemExit("ERROR: expected CupertinoText import anchor not found")

imports_to_add = [
    "import com.slapps.cupertino.CupertinoDatePicker\n",
    "import com.slapps.cupertino.DatePickerStyle\n",
    "import com.slapps.cupertino.ExperimentalCupertinoApi\n",
    "import com.slapps.cupertino.rememberCupertinoDatePickerState\n",
    "import java.time.Instant\n",
    "import java.time.ZoneOffset\n",
]
for imp in imports_to_add:
    if imp not in text:
        text = text.replace(anchor, anchor + imp, 1)

text = text.replace("import java.time.format.DateTimeFormatter\n", "")

start = text.find("@Composable\nfun ScheduleDatePickerSheet(")
end = text.find("\n@Composable\nprivate fun FocusedPickerDialog", start)
if start < 0 or end < 0:
    raise SystemExit("ERROR: could not locate ScheduleDatePickerSheet block")

new_block = """@OptIn(ExperimentalCupertinoApi::class)
@Composable
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

    val pickerState =
        rememberCupertinoDatePickerState(
            initialSelectedDateMillis =
                initialMillis,
            yearRange =
                firstYear..lastYear
        )

    FocusedPickerDialog(
        title = title,
        onDismiss = onDismiss
    ) {
        CupertinoDatePicker(
            state =
                pickerState,
            style =
                DatePickerStyle.Wheel(),
            modifier =
                Modifier.fillMaxWidth()
        )

        val selectedDate =
            remember(
                pickerState.selectedDateMillis
            ) {
                Instant
                    .ofEpochMilli(
                        pickerState.selectedDateMillis
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
                            pickerState.selectedDateMillis
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
"""

text = text[:start] + new_block + text[end:]
write(text)

text = read()
start = text.find("fun ScheduleDatePickerSheet(")
end = text.find("@Composable\nprivate fun FocusedPickerDialog", start)
date_block = text[start:end] if start >= 0 and end > start else ""

problems = []
for token in [
    "Plan 3.1 Cupertino date wheel",
    "import com.slapps.cupertino.CupertinoDatePicker",
    "import com.slapps.cupertino.DatePickerStyle",
    "import com.slapps.cupertino.ExperimentalCupertinoApi",
    "import com.slapps.cupertino.rememberCupertinoDatePickerState",
    "CupertinoDatePicker(",
    "DatePickerStyle.Wheel()",
    "rememberCupertinoDatePickerState(",
    ".toLocalDate().toString()",
]:
    if token not in text:
        problems.append(f"missing {token}")

for forbidden in [
    "DateTimeFormatter",
    "buildList",
    "cursor.plusDays(1)",
    "TextWheel(",
    "CupertinoDateTimePicker",
]:
    if forbidden in date_block:
        problems.append(f"forbidden sequential/date-time token remains in date sheet: {forbidden}")

REPORT.parent.mkdir(parents=True, exist_ok=True)
if problems:
    REPORT.write_text("# Batch 032 failed\n\n" + "\n".join(f"- {p}" for p in problems) + "\n", encoding="utf-8")
    raise SystemExit(f"ERROR: Batch 032 failed. See {REPORT}")

REPORT.write_text(
    "# Batch 032 passed\n\n"
    "- ScheduleDatePickerSheet now uses Slanoss CupertinoDatePicker.\n"
    "- DatePickerStyle.Wheel() is used, giving separate day/month/year columns.\n"
    "- Old sequential full-date list was removed from the date sheet.\n"
    "- Confirm output remains YYYY-MM-DD.\n"
    "- ScheduleTimePickerSheet remains unchanged.\n",
    encoding="utf-8",
)
print(f"PASS: Batch 032 applied. Report: {REPORT}")
