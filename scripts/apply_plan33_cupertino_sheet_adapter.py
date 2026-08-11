#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path.cwd()
FILE = ROOT / "app/src/main/java/com/lias/remote/ui/components/HigSheets.kt"
OUT = ROOT / "build/plan33/batch002_cupertino_sheet_adapter.md"

text = FILE.read_text(encoding="utf-8")

if "Plan 3.3 CupertinoSheetState adapter" in text:
    print("Batch 002 already applied.")
    sys.exit(0)

required_current = [
    "fun HigModalSheet(",
    "AnimatedVisibility(",
    "visible =\n            true",
    "slideInVertically",
    "slideOutVertically",
    "fun HigSheetHeader(",
    "com.slapps.cupertino.CupertinoText",
]
missing = [x for x in required_current if x not in text]
if missing:
    print("ERROR: HigSheets.kt does not match verified luna baseline:")
    for x in missing:
        print(" - missing:", repr(x))
    sys.exit(1)

anchor = "package com.lias.remote.ui.components\n\n"
pkg_pos = text.find(anchor)
if pkg_pos < 0:
    raise SystemExit("ERROR: package declaration anchor not found")
after_pkg = pkg_pos + len(anchor)
first_fun = text.find("@Composable", after_pkg)
if first_fun < 0:
    raise SystemExit("ERROR: first composable anchor not found")

imports = '''import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lias.remote.ui.theme.HigSpec
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import com.slapps.cupertino.CupertinoBottomSheetScaffold
import com.slapps.cupertino.CupertinoBottomSheetScaffoldDefaults
import com.slapps.cupertino.CupertinoSheetValue
import com.slapps.cupertino.CupertinoText
import com.slapps.cupertino.ExperimentalCupertinoApi
import com.slapps.cupertino.PresentationDetent
import com.slapps.cupertino.PresentationStyle
import com.slapps.cupertino.rememberCupertinoBottomSheetScaffoldState
import com.slapps.cupertino.rememberCupertinoSheetState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
'''

text = text[:after_pkg] + imports + "\n" + text[first_fun:]

start = text.find("@Composable\nfun HigModalSheet(")
end = text.find("\n@Composable\nfun HigSheetHeader(", start)
if start < 0 or end < 0:
    raise SystemExit("ERROR: could not bound HigModalSheet")

new_modal = '''private val LocalHigAnimatedDismiss =
    staticCompositionLocalOf<
        (() -> Unit)?
    > {
        null
    }

@Composable
fun rememberHigAnimatedDismiss(
    fallback: () -> Unit
): () -> Unit =
    LocalHigAnimatedDismiss.current
        ?: fallback

@OptIn(ExperimentalCupertinoApi::class)
@Composable
fun HigModalSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    accessibilityLabel: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    // Plan 3.3 CupertinoSheetState adapter:
    // Slanoss owns sheet motion, swipe and scrim interpolation.
    val coroutineScope =
        rememberCoroutineScope()

    var presentationStarted by
        remember {
            mutableStateOf(false)
        }

    var parentDismissDelivered by
        remember {
            mutableStateOf(false)
        }

    val sheetState =
        rememberCupertinoSheetState(
            initialValue =
                CupertinoSheetValue.Hidden,
            presentationStyle =
                PresentationStyle.Modal(
                    detents =
                        setOf(
                            PresentationDetent.Large
                        ),
                    dismissOnClickOutside =
                        true
                )
        )

    val scaffoldState =
        rememberCupertinoBottomSheetScaffoldState(
            bottomSheetState =
                sheetState
        )

    fun deliverParentDismissOnce() {
        if (!parentDismissDelivered) {
            parentDismissDelivered = true
            onDismiss()
        }
    }

    fun requestAnimatedDismiss() {
        if (parentDismissDelivered) {
            return
        }

        coroutineScope.launch {
            runCatching {
                sheetState.hide()
            }
            deliverParentDismissOnce()
        }
    }

    LaunchedEffect(sheetState) {
        presentationStarted = true
        sheetState.show()
    }

    LaunchedEffect(
        sheetState,
        presentationStarted
    ) {
        snapshotFlow {
            sheetState.currentValue
        }
            .distinctUntilChanged()
            .collect { value ->
                if (
                    presentationStarted &&
                    value is CupertinoSheetValue.Hidden &&
                    sheetState.targetValue is CupertinoSheetValue.Hidden
                ) {
                    deliverParentDismissOnce()
                }
            }
    }

    BackHandler(
        enabled =
            !parentDismissDelivered
    ) {
        requestAnimatedDismiss()
    }

    CompositionLocalProvider(
        LocalHigAnimatedDismiss provides
            ::requestAnimatedDismiss
    ) {
        CupertinoBottomSheetScaffold(
            modifier =
                Modifier.fillMaxSize(),
            scaffoldState =
                scaffoldState,
            colors =
                CupertinoBottomSheetScaffoldDefaults.colors(
                    sheetContainerColor =
                        LiasThemeColors.secondaryBackground,
                    containerColor =
                        Color.Transparent,
                    contentColor =
                        LiasThemeColors.label,
                    scrimColor =
                        Color.Black.copy(
                            alpha = 0.40f
                        ),
                    scaledScaffoldBackgroundColor =
                        Color.Transparent
                ),
            sheetShape =
                RoundedCornerShape(
                    topStart =
                        HigSpec.SheetCorner,
                    topEnd =
                        HigSpec.SheetCorner
                ),
            sheetSwipeEnabled =
                true,
            applyContentScaling =
                false,
            sheetContent = {
                Column(
                    modifier =
                        modifier
                            .fillMaxWidth()
                            .imePadding()
                            .then(
                                if (
                                    accessibilityLabel.isNullOrBlank()
                                ) {
                                    Modifier
                                } else {
                                    Modifier.semantics {
                                        paneTitle =
                                            accessibilityLabel
                                    }
                                }
                            )
                            .padding(
                                bottom = 24.dp
                            )
                ) {
                    content()
                }
            }
        ) {
            Spacer(
                modifier =
                    Modifier.fillMaxSize()
            )
        }
    }
}
'''

text = text[:start] + new_modal + text[end:]

header_marker = '''fun HigSheetHeader(
    title: String,
    onCancel: () -> Unit,
    trailingAction:
        (@Composable () -> Unit)? =
        null
) {
'''
header_replacement = header_marker + '''
    val animatedCancel =
        rememberHigAnimatedDismiss(
            fallback =
                onCancel
        )

'''
if header_marker not in text:
    raise SystemExit("ERROR: current HigSheetHeader signature differs")
text = text.replace(header_marker, header_replacement, 1)

old_cancel = '''        HigTextButton(
            text =
                "Cancel",
            onClick =
                onCancel
        )'''
new_cancel = '''        HigTextButton(
            text =
                "Cancel",
            onClick =
                animatedCancel
        )'''
if old_cancel not in text:
    raise SystemExit("ERROR: current HigSheetHeader Cancel block differs")
text = text.replace(old_cancel, new_cancel, 1)

text = text.replace("// Version: 27.0.0", "// Version: 33.2.0", 1)
text = text.replace(
    "//   - Sheet body consumes pointer taps to prevent accidental dismissal.",
    "//   - Sheet body consumes pointer taps to prevent accidental dismissal.\n"
    "//\n"
    "// Plan 3.3 Batch 002:\n"
    "//   - Uses Slanoss 2.3.1 CupertinoSheetState / CupertinoBottomSheetScaffold.\n"
    "//   - Back, outside tap, swipe-down and header Cancel animate before parent removal.",
    1,
)

FILE.write_text(text, encoding="utf-8")

text = FILE.read_text(encoding="utf-8")
checks = {
    "uses_cupertino_scaffold": "CupertinoBottomSheetScaffold(" in text,
    "uses_sheet_state": "rememberCupertinoSheetState(" in text,
    "initial_hidden": "CupertinoSheetValue.Hidden" in text,
    "calls_show": "sheetState.show()" in text,
    "calls_hide": "sheetState.hide()" in text,
    "observes_hidden": "snapshotFlow" in text and "sheetState.currentValue" in text,
    "back_animated": "BackHandler(" in text and "requestAnimatedDismiss()" in text,
    "header_cancel_animated": "animatedCancel" in text,
    "no_old_animated_visibility": "AnimatedVisibility(" not in text,
    "no_old_slide_animation": "slideInVertically" not in text and "slideOutVertically" not in text,
    "no_content_scaling": "applyContentScaling =\n                false" in text,
    "keeps_accessibility": "paneTitle" in text,
    "keeps_ime_padding": ".imePadding()" in text,
}
bad = [k for k,v in checks.items() if not v]

OUT.parent.mkdir(parents=True, exist_ok=True)
if bad:
    OUT.write_text(
        "# Batch 002 failed\n\n" +
        "\n".join(f"- {x}" for x in bad) + "\n",
        encoding="utf-8"
    )
    print("ERROR: Batch 002 verification failed:")
    for x in bad:
        print(" -", x)
    sys.exit(1)

OUT.write_text(
    "# Plan 3.3 Batch 002 passed\n\n"
    "- HigModalSheet uses Slanoss CupertinoBottomSheetScaffold.\n"
    "- Initial state is Hidden and entrance calls show().\n"
    "- Back/outside/swipe dismissal reaches Hidden before parent removal.\n"
    "- HigSheetHeader Cancel uses the animated close action.\n"
    "- IME padding and pane accessibility semantics are retained.\n"
    "- Arbitrary Done/Save call sites remain for Batch 003.\n",
    encoding="utf-8"
)
print(OUT.read_text())
