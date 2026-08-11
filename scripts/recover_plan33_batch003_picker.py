#!/usr/bin/env python3
from pathlib import Path
import re
import sys

ROOT = Path.cwd()
P = ROOT / "app/src/main/java/com/lias/remote/ui/screens/schedules/SchedulePickerSheets.kt"
OUT = ROOT / "build/plan33/batch003_picker_recovery.md"

if not P.exists():
    raise SystemExit("ERROR: SchedulePickerSheets.kt missing")

text = P.read_text(encoding="utf-8")

# ------------------------------------------------------------------
# Imports / type alias: safe for both untouched and partially modified file.
# ------------------------------------------------------------------
def ensure_import(source, line, after):
    if line in source:
        return source
    if after not in source:
        raise SystemExit(f"ERROR: import anchor missing: {after.strip()}")
    return source.replace(after, after + line, 1)

text = ensure_import(
    text,
    "import com.lias.remote.ui.components.HigModalSheet\n",
    "import com.lias.remote.ui.components.HigButtonStyle\n",
)
text = ensure_import(
    text,
    "import com.lias.remote.ui.components.rememberHigAnimatedCompletion\n",
    "import com.lias.remote.ui.components.HigModalSheet\n",
)

if "private typealias AnimatedCompletion" not in text:
    marker = "private const val WHEEL_VISIBLE_ROWS = 5\n"
    if marker not in text:
        raise SystemExit("ERROR: picker constants anchor missing")
    text = text.replace(
        marker,
        "private typealias AnimatedCompletion = (() -> Unit) -> Unit\n\n" + marker,
        1,
    )

# ------------------------------------------------------------------
# Convert BOTH FocusedPickerDialog call-site lambdas structurally.
# Verified luna has exactly two calls: time and date.
# ------------------------------------------------------------------
call_pattern = re.compile(
    r'(FocusedPickerDialog\(\s*'
    r'title\s*=\s*title\s*,\s*'
    r'onDismiss\s*=\s*onDismiss\s*'
    r'\)\s*)\{\s*(?!animatedComplete\s*->)',
    re.MULTILINE,
)

text, converted_calls = call_pattern.subn(
    r'\1{ animatedComplete ->\n',
    text,
)

# If already partially recovered, zero replacements is valid only when both exist.
animated_call_count = len(
    re.findall(
        r'FocusedPickerDialog\(\s*'
        r'title\s*=\s*title\s*,\s*'
        r'onDismiss\s*=\s*onDismiss\s*'
        r'\)\s*\{\s*animatedComplete\s*->',
        text,
        re.MULTILINE,
    )
)

if animated_call_count != 2:
    raise SystemExit(
        f"ERROR: expected 2 animated FocusedPickerDialog call sites, found {animated_call_count}"
    )

# ------------------------------------------------------------------
# Time picker Done: preserve HH:mm wire output.
# ------------------------------------------------------------------
time_old = re.compile(
    r'onConfirm\(\s*'
    r'String\.format\(\s*'
    r'Locale\.US\s*,\s*'
    r'"%02d:%02d"\s*,\s*'
    r'selectedHour\s*,\s*'
    r'selectedMinute\s*'
    r'\)\s*'
    r'\)',
    re.MULTILINE,
)

if "animatedComplete {\n                    onConfirm(\n                        value" not in text:
    def time_repl(_):
        return '''val value =
                    String.format(
                        Locale.US,
                        "%02d:%02d",
                        selectedHour,
                        selectedMinute
                    )

                animatedComplete {
                    onConfirm(
                        value
                    )
                }'''
    text, time_patched = time_old.subn(time_repl, text, count=1)
else:
    time_patched = 0

# ------------------------------------------------------------------
# Date picker Done: preserve YYYY-MM-DD wire output.
# ------------------------------------------------------------------
date_old = re.compile(
    r'onConfirm\(\s*'
    r'Instant\s*'
    r'\.ofEpochMilli\(\s*selectedDateMillis\s*\)\s*'
    r'\.atZone\(\s*ZoneOffset\.UTC\s*\)\s*'
    r'\.toLocalDate\(\)\.toString\(\)\s*'
    r'\)',
    re.MULTILINE,
)

# Distinguish from time value block by looking for selectedDateMillis nearby.
if not re.search(
    r'val value\s*=\s*Instant\s*\.ofEpochMilli\(\s*selectedDateMillis',
    text,
    re.MULTILINE,
):
    def date_repl(_):
        return '''val value =
                    Instant
                        .ofEpochMilli(
                            selectedDateMillis
                        )
                        .atZone(
                            ZoneOffset.UTC
                        )
                        .toLocalDate()
                        .toString()

                animatedComplete {
                    onConfirm(
                        value
                    )
                }'''
    text, date_patched = date_old.subn(date_repl, text, count=1)
else:
    date_patched = 0

# ------------------------------------------------------------------
# Replace custom direct Dialog implementation wholesale.
# ------------------------------------------------------------------
start = text.find("@Composable\nprivate fun FocusedPickerDialog(")
end = text.find("\n@Composable\nprivate fun NumberWheel(", start)
if start < 0 or end < 0:
    raise SystemExit("ERROR: could not bound FocusedPickerDialog function")

current_picker = text[start:end]

if "HigModalSheet(" not in current_picker:
    new_picker = '''@Composable
private fun FocusedPickerDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable (AnimatedCompletion) -> Unit
) {
    val configuration =
        LocalConfiguration.current

    val maxPickerHeight =
        (
            configuration.screenHeightDp *
                0.72f
            )
            .dp
            .coerceAtLeast(
                420.dp
            )

    HigModalSheet(
        onDismiss =
            onDismiss,
        modifier =
            Modifier.widthIn(
                max =
                    600.dp
            ),
        accessibilityLabel =
            title
    ) {
        val animatedComplete =
            rememberHigAnimatedCompletion(
                fallbackDismiss =
                    onDismiss
            )

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(
                        max =
                            maxPickerHeight
                    )
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(
                        horizontal =
                            20.dp,
                        vertical =
                            16.dp
                    ),
            verticalArrangement =
                Arrangement.spacedBy(
                    14.dp
                )
        ) {
            HigSheetHeader(
                title =
                    title,
                onCancel =
                    onDismiss
            )

            content(
                animatedComplete
            )
        }
    }
}
'''
    text = text[:start] + new_picker + text[end:]

# ------------------------------------------------------------------
# Remove imports belonging only to the retired custom Dialog animation.
# ------------------------------------------------------------------
obsolete = [
    "import androidx.compose.animation.AnimatedVisibility\n",
    "import androidx.compose.animation.fadeIn\n",
    "import androidx.compose.animation.fadeOut\n",
    "import androidx.compose.animation.slideInVertically\n",
    "import androidx.compose.animation.slideOutVertically\n",
    "import androidx.compose.ui.window.Dialog\n",
    "import androidx.compose.ui.window.DialogProperties\n",
    "import androidx.compose.foundation.layout.WindowInsets\n",
    "import androidx.compose.foundation.layout.systemBars\n",
    "import androidx.compose.foundation.layout.windowInsetsPadding\n",
    "import com.lias.remote.ui.theme.HigSpec\n",
]
for line in obsolete:
    text = text.replace(line, "")

text = text.replace("// Version: 28.6.1", "// Version: 33.3.1", 1)

if "Plan 3.3 animated completion:" not in text:
    marker = "// - ScheduleDatePickerSheet calls HigDatePicker instead of raw CupertinoDatePicker."
    if marker in text:
        text = text.replace(
            marker,
            marker + "\n"
            "//\n"
            "// Plan 3.3 animated completion:\n"
            "// - Focused date/time picker uses HigModalSheet instead of a custom Dialog.\n"
            "// - Done hides the sheet before emitting unchanged YYYY-MM-DD / HH:mm values.",
            1,
        )

P.write_text(text, encoding="utf-8")
text = P.read_text(encoding="utf-8")

# ------------------------------------------------------------------
# Verification
# ------------------------------------------------------------------
focused_start = text.find("@Composable\nprivate fun FocusedPickerDialog(")
focused_end = text.find("\n@Composable\nprivate fun NumberWheel(", focused_start)
focused = text[focused_start:focused_end]

checks = {
    "two_animated_calls":
        len(re.findall(r'FocusedPickerDialog\([\s\S]*?\)\s*\{\s*animatedComplete\s*->', text)) >= 2,
    "time_value_preserved":
        '"%02d:%02d"' in text
        and re.search(r'animatedComplete\s*\{\s*onConfirm\(\s*value', text, re.MULTILINE) is not None,
    "date_value_preserved":
        re.search(
            r'val value\s*=\s*Instant\s*\.ofEpochMilli\(\s*selectedDateMillis',
            text,
            re.MULTILINE,
        ) is not None,
    "focused_uses_hig_modal":
        "HigModalSheet(" in focused,
    "focused_has_completion":
        "rememberHigAnimatedCompletion(" in focused,
    "focused_no_direct_dialog":
        "Dialog(" not in focused,
    "file_no_direct_dialog":
        "Dialog(" not in text,
    "no_old_animated_visibility":
        "AnimatedVisibility(" not in text,
    "time_wire":
        '"%02d:%02d"' in text,
    "date_wire":
        ".toLocalDate()" in text and ".toString()" in text,
}

bad = [k for k,v in checks.items() if not v]
OUT.parent.mkdir(parents=True, exist_ok=True)

if bad:
    OUT.write_text(
        "# Picker recovery failed\n\n"
        f"- converted call sites this run: {converted_calls}\n"
        f"- time callback patched this run: {time_patched}\n"
        f"- date callback patched this run: {date_patched}\n\n"
        + "\n".join(f"- FAIL: {x}" for x in bad)
        + "\n",
        encoding="utf-8",
    )
    print(OUT.read_text())
    sys.exit(1)

OUT.write_text(
    "# Plan 3.3 Batch 003 picker recovery passed\n\n"
    f"- FocusedPickerDialog call sites converted this run: {converted_calls}\n"
    f"- Time Done callback patched this run: {time_patched}\n"
    f"- Date Done callback patched this run: {date_patched}\n"
    "- Both picker call sites receive `animatedComplete`.\n"
    "- Custom Compose Dialog presentation removed.\n"
    "- Picker now inherits Slanoss HigModalSheet entrance/dismiss lifecycle.\n"
    "- HH:mm and YYYY-MM-DD output contracts are preserved.\n",
    encoding="utf-8",
)
print(OUT.read_text())
