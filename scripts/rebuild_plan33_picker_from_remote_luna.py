#!/usr/bin/env python3
from pathlib import Path
import re
import sys

ROOT = Path.cwd()
REMOTE = ROOT / "build/plan33/SchedulePickerSheets.remote_luna.kt"
DEST = ROOT / "app/src/main/java/com/lias/remote/ui/screens/schedules/SchedulePickerSheets.kt"
OUT = ROOT / "build/plan33/remote_exact_picker_recovery.md"

text = REMOTE.read_text(encoding="utf-8")

guards = {
    "version": "// Version: 28.6.1" in text,
    "focused_count": text.count("FocusedPickerDialog(") == 3,
    "dialog_import": "import androidx.compose.ui.window.Dialog\n" in text,
    "dialog_props": "import androidx.compose.ui.window.DialogProperties\n" in text,
    "animated_visibility": "AnimatedVisibility(" in text,
    "time_wire": '"%02d:%02d"' in text,
    "date_wire": ".toLocalDate().toString()" in text,
}
bad = [k for k,v in guards.items() if not v]
if bad:
    raise SystemExit("ERROR: audited remote picker structure mismatch: " + ", ".join(bad))

for line in [
    "import androidx.compose.animation.AnimatedVisibility\n",
    "import androidx.compose.animation.fadeIn\n",
    "import androidx.compose.animation.fadeOut\n",
    "import androidx.compose.animation.slideInVertically\n",
    "import androidx.compose.animation.slideOutVertically\n",
    "import androidx.compose.foundation.layout.WindowInsets\n",
    "import androidx.compose.foundation.layout.systemBars\n",
    "import androidx.compose.foundation.layout.windowInsetsPadding\n",
    "import androidx.compose.runtime.LaunchedEffect\n",
    "import androidx.compose.ui.window.Dialog\n",
    "import androidx.compose.ui.window.DialogProperties\n",
    "import com.lias.remote.ui.theme.HigSpec\n",
]:
    text = text.replace(line, "")

anchor = "import com.lias.remote.ui.components.HigButtonStyle\n"
if anchor not in text:
    raise SystemExit("ERROR: HigButtonStyle import anchor missing")
text = text.replace(
    anchor,
    anchor
    + "import com.lias.remote.ui.components.HigModalSheet\n"
    + "import com.lias.remote.ui.components.rememberHigAnimatedCompletion\n",
    1,
)

marker = "private const val WHEEL_VISIBLE_ROWS = 5\n"
if marker not in text:
    raise SystemExit("ERROR: wheel constants anchor missing")
text = text.replace(
    marker,
    "private typealias AnimatedCompletion = (() -> Unit) -> Unit\n\n" + marker,
    1,
)

call = '''    FocusedPickerDialog(
        title = title,
        onDismiss = onDismiss
    ) {
'''
replacement = '''    FocusedPickerDialog(
        title = title,
        onDismiss = onDismiss
    ) { animatedComplete ->
'''
if text.count(call) != 2:
    raise SystemExit(f"ERROR: expected 2 exact picker call blocks, found {text.count(call)}")
text = text.replace(call, replacement, 2)

time_old = '''            onClick = {
                onConfirm(
                    String.format(
                        Locale.US,
                        "%02d:%02d",
                        selectedHour,
                        selectedMinute
                    )
                )
            },
'''
time_new = '''            onClick = {
                val value =
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
                }
            },
'''
if text.count(time_old) != 1:
    raise SystemExit(f"ERROR: expected one exact time Done block, found {text.count(time_old)}")
text = text.replace(time_old, time_new, 1)

date_old = '''            onClick = {
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
'''
date_new = '''            onClick = {
                val value =
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
                }
            },
'''
if text.count(date_old) != 1:
    raise SystemExit(f"ERROR: expected one exact date Done block, found {text.count(date_old)}")
text = text.replace(date_old, date_new, 1)

start = text.find("@Composable\nprivate fun FocusedPickerDialog(")
end = text.find("\n@Composable\nprivate fun NumberWheel(", start)
if start < 0 or end < 0:
    raise SystemExit("ERROR: cannot bound FocusedPickerDialog")

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

text = text.replace("// Version: 28.6.1", "// Version: 33.3.2", 1)

def standalone_dialog(source: str) -> bool:
    return re.search(r'(?<![A-Za-z0-9_])Dialog\s*\(', source) is not None

checks = {
    "two_animated_calls":
        len(re.findall(
            r'FocusedPickerDialog\(\s*title\s*=\s*title,\s*onDismiss\s*=\s*onDismiss\s*\)\s*\{\s*animatedComplete\s*->',
            text,
            re.MULTILINE,
        )) == 2,
    "hig_modal": "HigModalSheet(" in text,
    "completion": "rememberHigAnimatedCompletion(" in text,
    "no_dialog_import": "import androidx.compose.ui.window.Dialog" not in text,
    "no_standalone_dialog": not standalone_dialog(text),
    "no_animated_visibility": "AnimatedVisibility(" not in text,
    "time_contract": '"%02d:%02d"' in text,
    "date_contract": ".toLocalDate()" in text and ".toString()" in text,
    "wheel_logic": "private fun TextWheel(" in text and "rememberSnapFlingBehavior" in text,
    "date_picker": "HigDatePicker(" in text and "HigDatePickerMode.Wheel" in text,
}
bad = [k for k,v in checks.items() if not v]
if bad:
    raise SystemExit("ERROR: rebuilt picker validation failed: " + ", ".join(bad))

DEST.write_text(text, encoding="utf-8")

OUT.write_text(
    "# Remote-exact SchedulePicker recovery passed\n\n"
    "- Baseline: exact origin/luna blob 40d13cc91d5064d0ce4035b51d6485cdeec38423.\n"
    "- Rebuilt from remote source, not local partial text.\n"
    "- Both picker call sites use animated completion.\n"
    "- Direct Compose Dialog and AnimatedVisibility portal removed.\n"
    "- FocusedPickerDialog name retained without false-positive Dialog detection.\n"
    "- HH:mm and YYYY-MM-DD contracts preserved.\n",
    encoding="utf-8",
)
print(OUT.read_text())
