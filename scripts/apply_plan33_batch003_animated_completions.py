#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path.cwd()
SRC = ROOT / "app/src/main/java"
OUT = ROOT / "build/plan33/batch003_animated_completions.md"

def read(path):
    return path.read_text(encoding="utf-8")

def write(path, text):
    path.write_text(text, encoding="utf-8")

def require_file(rel):
    p = ROOT / rel
    if not p.exists():
        raise SystemExit(f"ERROR: missing {rel}")
    return p

def replace_once(text, old, new, label):
    if new in text:
        return text
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"ERROR: {label}: expected exactly one baseline block, found {count}")
    return text.replace(old, new, 1)

def ensure_import(text, import_line, anchor):
    if import_line in text:
        return text
    if anchor not in text:
        raise SystemExit(f"ERROR: import anchor missing for {import_line}")
    return text.replace(anchor, anchor + import_line, 1)

# ---------------------------------------------------------------------
# 1. Harden Batch 002 wrapper with completionInFlight and completion hook.
# ---------------------------------------------------------------------
hig = require_file("app/src/main/java/com/lias/remote/ui/components/HigSheets.kt")
text = read(hig)

if "Plan 3.3 CupertinoSheetState adapter" not in text:
    raise SystemExit("ERROR: Batch 002 marker missing from HigSheets.kt")

if "fun rememberHigAnimatedCompletion(" not in text:
    anchor = '''@Composable
fun rememberHigAnimatedDismiss(
    fallback: () -> Unit
): () -> Unit =
    LocalHigAnimatedDismiss.current
        ?: fallback
'''
    addition = anchor + '''
private val LocalHigAnimatedCompletion =
    staticCompositionLocalOf<
        (((() -> Unit) -> Unit))?
    > {
        null
    }

@Composable
fun rememberHigAnimatedCompletion(
    fallbackDismiss: () -> Unit
): ((() -> Unit) -> Unit) =
    LocalHigAnimatedCompletion.current
        ?: { action ->
            action()
            fallbackDismiss()
        }
'''
    if anchor not in text:
        raise SystemExit("ERROR: Batch 002 dismiss helper shape differs")
    text = text.replace(anchor, addition, 1)

if "var completionInFlight by" not in text:
    anchor = '''    var parentDismissDelivered by
        remember {
            mutableStateOf(false)
        }
'''
    addition = anchor + '''
    var completionInFlight by
        remember {
            mutableStateOf(false)
        }
'''
    if anchor not in text:
        raise SystemExit("ERROR: parentDismissDelivered baseline missing")
    text = text.replace(anchor, addition, 1)

if "fun requestAnimatedCompletion(" not in text:
    anchor = '''    fun requestAnimatedDismiss() {
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
'''
    addition = anchor + '''
    fun requestAnimatedCompletion(
        action: () -> Unit
    ) {
        if (
            parentDismissDelivered ||
            completionInFlight
        ) {
            return
        }

        completionInFlight =
            true

        coroutineScope.launch {
            runCatching {
                sheetState.hide()
            }

            /*
             * Strict ordering:
             *   hide animation -> existing action -> parent cleanup.
             *
             * This prevents the Hidden observer from removing composition
             * before Save/Confirm logic runs.
             */
            action()

            completionInFlight =
                false

            deliverParentDismissOnce()
        }
    }
'''
    if anchor not in text:
        raise SystemExit("ERROR: requestAnimatedDismiss baseline differs")
    text = text.replace(anchor, addition, 1)

old_observer = '''                    presentationStarted &&
                    value is CupertinoSheetValue.Hidden &&
                    sheetState.targetValue is CupertinoSheetValue.Hidden
'''
new_observer = '''                    presentationStarted &&
                    !completionInFlight &&
                    value is CupertinoSheetValue.Hidden &&
                    sheetState.targetValue is CupertinoSheetValue.Hidden
'''
if new_observer not in text:
    if old_observer not in text:
        raise SystemExit("ERROR: Hidden observer baseline differs")
    text = text.replace(old_observer, new_observer, 1)

old_provider = '''    CompositionLocalProvider(
        LocalHigAnimatedDismiss provides
            ::requestAnimatedDismiss
    ) {
'''
new_provider = '''    CompositionLocalProvider(
        LocalHigAnimatedDismiss provides
            ::requestAnimatedDismiss,
        LocalHigAnimatedCompletion provides
            ::requestAnimatedCompletion
    ) {
'''
if new_provider not in text:
    if old_provider not in text:
        raise SystemExit("ERROR: Batch 002 CompositionLocalProvider baseline differs")
    text = text.replace(old_provider, new_provider, 1)

text = text.replace("// Version: 33.2.0", "// Version: 33.3.0", 1)
if "Plan 3.3 Batch 003:" not in text:
    text = text.replace(
        "//   - Back, outside tap, swipe-down and header Cancel animate before parent removal.",
        "//   - Back, outside tap, swipe-down and header Cancel animate before parent removal.\n"
        "//\n"
        "// Plan 3.3 Batch 003:\n"
        "//   - Adds animated completion ordering for Save/Done/Confirm actions.\n"
        "//   - Prevents Hidden observer from racing completion callbacks.",
        1
    )

write(hig, text)

# ---------------------------------------------------------------------
# Helper used in normal HigModalSheet files.
# ---------------------------------------------------------------------
def add_completion_import(path, text):
    return ensure_import(
        text,
        "import com.lias.remote.ui.components.rememberHigAnimatedCompletion\n",
        "import com.lias.remote.ui.components.HigModalSheet\n"
    )

# ---------------------------------------------------------------------
# 2. ExtendAccessSheet
# ---------------------------------------------------------------------
p = require_file("app/src/main/java/com/lias/remote/ui/screens/ExtendAccessSheet.kt")
t = add_completion_import(p, read(p))
if "val animatedComplete =" not in t:
    t = replace_once(
        t,
        '''    HigModalSheet(
        onDismiss =
            onDismiss
    ) {

        Column(
''',
        '''    HigModalSheet(
        onDismiss =
            onDismiss
    ) {

        val animatedComplete =
            rememberHigAnimatedCompletion(
                fallbackDismiss =
                    onDismiss
            )

        Column(
''',
        "ExtendAccessSheet helper"
    )

old = '''                onClick = {
                    onConfirm(
                        selectedMinutes
                            .toInt()
                            .coerceIn(
                                1,
                                120
                            )
                    )
                },
'''
new = '''                onClick = {
                    val minutes =
                        selectedMinutes
                            .toInt()
                            .coerceIn(
                                1,
                                120
                            )

                    animatedComplete {
                        onConfirm(
                            minutes
                        )
                    }
                },
'''
if new not in t:
    if old not in t:
        raise SystemExit("ERROR: ExtendAccessSheet confirm block differs")
    t = t.replace(old, new, 1)

old = '''                    onClick =
                        onCancelExtension,
'''
new = '''                    onClick = {
                        animatedComplete {
                            onCancelExtension()
                        }
                    },
'''
if new not in t:
    if old not in t:
        raise SystemExit("ERROR: ExtendAccessSheet cancel-extension block differs")
    t = t.replace(old, new, 1)

write(p, t)

# ---------------------------------------------------------------------
# 3. PauseSheet
# ---------------------------------------------------------------------
p = require_file("app/src/main/java/com/lias/remote/ui/screens/PauseSheet.kt")
t = add_completion_import(p, read(p))
if "val animatedComplete =" not in t:
    t = replace_once(
        t,
        '''    ) {

        Column(
''',
        '''    ) {

        val animatedComplete =
            rememberHigAnimatedCompletion(
                fallbackDismiss =
                    onDismiss
            )

        Column(
''',
        "PauseSheet helper"
    )
old = '''                onClick = {

                    onConfirm(
                        60
                    )
                },
'''
new = '''                onClick = {

                    animatedComplete {
                        onConfirm(
                            60
                        )
                    }
                },
'''
if new not in t:
    if old not in t:
        raise SystemExit("ERROR: PauseSheet confirm block differs")
    t = t.replace(old, new, 1)
write(p, t)

# ---------------------------------------------------------------------
# 4. ActionSheets — Onboarding, Security Alert, Global Switch.
# ---------------------------------------------------------------------
p = require_file("app/src/main/java/com/lias/remote/ui/screens/ActionSheets.kt")
t = read(p)
t = add_completion_import(p, t)
t = ensure_import(
    t,
    "import com.lias.remote.ui.components.rememberHigAnimatedDismiss\n",
    "import com.lias.remote.ui.components.rememberHigAnimatedCompletion\n"
)

# Onboarding
old = '''    ) {
        Column(
'''
new = '''    ) {
        val animatedComplete =
            rememberHigAnimatedCompletion(
                fallbackDismiss =
                    onComplete
            )

        Column(
'''
# first occurrence belongs to Onboarding.
if "fallbackDismiss =\n                    onComplete" not in t:
    if old not in t:
        raise SystemExit("ERROR: Onboarding HigModalSheet body baseline differs")
    t = t.replace(old, new, 1)

t = replace_once(
    t,
    '''            HigButton(
                text = "Continue",
                onClick = onComplete,
''',
    '''            HigButton(
                text = "Continue",
                onClick = {
                    animatedComplete {
                        onComplete()
                    }
                },
''',
    "Onboarding Continue"
)

# SecurityAlert: insert helpers in second matching sheet.
security_anchor = '''    HigModalSheet(
        onDismiss = onDismiss,
        accessibilityLabel = "Security Alert"
    ) {
'''
if "val animatedDismiss =" not in t[t.find(security_anchor):t.find(security_anchor)+700]:
    repl = security_anchor + '''        val animatedComplete =
            rememberHigAnimatedCompletion(
                fallbackDismiss =
                    onDismiss
            )

        val animatedDismiss =
            rememberHigAnimatedDismiss(
                fallback =
                    onDismiss
            )

'''
    if security_anchor not in t:
        raise SystemExit("ERROR: SecurityAlertSheet baseline differs")
    t = t.replace(security_anchor, repl, 1)

t = replace_once(
    t,
    '''            HigButton(
                text = "Block Device",
                onClick = onBlock,
''',
    '''            HigButton(
                text = "Block Device",
                onClick = {
                    animatedComplete {
                        onBlock()
                    }
                },
''',
    "Security Block"
)
t = replace_once(
    t,
    '''            HigButton(
                text = "Mark as Trusted",
                onClick = onTrust,
''',
    '''            HigButton(
                text = "Mark as Trusted",
                onClick = {
                    animatedComplete {
                        onTrust()
                    }
                },
''',
    "Security Trust"
)
t = replace_once(
    t,
    '''            HigTextButton(
                text = "Investigate Later",
                onClick = onDismiss
''',
    '''            HigTextButton(
                text = "Investigate Later",
                onClick = animatedDismiss
''',
    "Security dismiss"
)

# Global switch
global_anchor = '''    HigModalSheet(
        onDismiss = onDismiss,
        accessibilityLabel = "Global Access Switch"
    ) {
'''
if "fallbackDismiss =\n                    onDismiss" not in t[t.find(global_anchor):t.find(global_anchor)+500]:
    repl = global_anchor + '''        val animatedComplete =
            rememberHigAnimatedCompletion(
                fallbackDismiss =
                    onDismiss
            )

'''
    if global_anchor not in t:
        raise SystemExit("ERROR: GlobalSwitchSheet baseline differs")
    t = t.replace(global_anchor, repl, 1)

old = '''                onClick = {
                    onSave(
                        currentPolicy.copy(
                            action = selectedAction,
                            enabled = true
                        )
                    )
                },
'''
new = '''                onClick = {
                    val updatedPolicy =
                        currentPolicy.copy(
                            action = selectedAction,
                            enabled = true
                        )

                    animatedComplete {
                        onSave(
                            updatedPolicy
                        )
                    }
                },
'''
if new not in t:
    if old not in t:
        raise SystemExit("ERROR: GlobalSwitch Save baseline differs")
    t = t.replace(old, new, 1)

write(p, t)

# ---------------------------------------------------------------------
# 5. ScheduleEditorSheet — both Save paths.
# ---------------------------------------------------------------------
p = require_file("app/src/main/java/com/lias/remote/ui/screens/schedules/ScheduleEditorSheet.kt")
t = add_completion_import(p, read(p))
if "val animatedComplete =" not in t:
    t = replace_once(
        t,
        '''    HigModalSheet(
        onDismiss =
            onDismiss
    ) {

        Column(
''',
        '''    HigModalSheet(
        onDismiss =
            onDismiss
    ) {

        val animatedComplete =
            rememberHigAnimatedCompletion(
                fallbackDismiss =
                    onDismiss
            )

        Column(
''',
        "ScheduleEditor helper"
    )

old_save = '''                                onSave(
                                    draft.toSchedule(
                                        initialSchedule
                                    )
                                )
'''
new_save = '''                                val schedule =
                                    draft.toSchedule(
                                        initialSchedule
                                    )

                                animatedComplete {
                                    onSave(
                                        schedule
                                    )
                                }
'''
count = t.count(old_save)
if count:
    # Exact current source has two save paths.
    if count != 2:
        raise SystemExit(f"ERROR: ScheduleEditor expected 2 save blocks, found {count}")
    t = t.replace(old_save, new_save, 2)
elif t.count("animatedComplete {") < 2:
    raise SystemExit("ERROR: ScheduleEditor Save blocks not recognized")

write(p, t)

# ---------------------------------------------------------------------
# 6. TagEditorSheet — Save closes after motion.
# ---------------------------------------------------------------------
p = require_file("app/src/main/java/com/lias/remote/ui/screens/devices/TagEditorSheet.kt")
t = add_completion_import(p, read(p))
if "val animatedComplete =" not in t:
    t = replace_once(
        t,
        '''    HigModalSheet(
        onDismiss =
            onDismiss
    ) {

        Column(
''',
        '''    HigModalSheet(
        onDismiss =
            onDismiss
    ) {

        val animatedComplete =
            rememberHigAnimatedCompletion(
                fallbackDismiss =
                    onDismiss
            )

        Column(
''',
        "TagEditor helper"
    )

old = '''                                onSave(
                                    Tag(
'''
new = '''                                val updatedTag =
                                    Tag(
'''
if "val updatedTag =" not in t:
    if old not in t:
        raise SystemExit("ERROR: TagEditor Save baseline start differs")
    t = t.replace(old, new, 1)
    tail = '''                                    )
                                )
'''
    repl = '''                                    )

                                animatedComplete {
                                    onSave(
                                        updatedTag
                                    )
                                }
'''
    # only replace first tail following val updatedTag.
    pos = t.find("val updatedTag =")
    idx = t.find(tail, pos)
    if idx < 0:
        raise SystemExit("ERROR: TagEditor Save baseline tail differs")
    t = t[:idx] + repl + t[idx+len(tail):]

write(p, t)

# ---------------------------------------------------------------------
# 7. MoveTagSheet — custom Cancel + Done.
# ---------------------------------------------------------------------
p = require_file("app/src/main/java/com/lias/remote/ui/screens/devices/MoveTagSheet.kt")
t = read(p)
t = add_completion_import(p, t)
t = ensure_import(
    t,
    "import com.lias.remote.ui.components.rememberHigAnimatedDismiss\n",
    "import com.lias.remote.ui.components.rememberHigAnimatedCompletion\n"
)
if "val animatedComplete =" not in t:
    t = replace_once(
        t,
        '''    HigModalSheet(
        onDismiss =
            onDismiss
    ) {

        Column(
''',
        '''    HigModalSheet(
        onDismiss =
            onDismiss
    ) {

        val animatedComplete =
            rememberHigAnimatedCompletion(
                fallbackDismiss =
                    onDismiss
            )

        val animatedDismiss =
            rememberHigAnimatedDismiss(
                fallback =
                    onDismiss
            )

        Column(
''',
        "MoveTag helpers"
    )

t = replace_once(
    t,
    '''                HigTextButton(
                    text =
                        "Cancel",
                    onClick =
                        onDismiss
                )
''',
    '''                HigTextButton(
                    text =
                        "Cancel",
                    onClick =
                        animatedDismiss
                )
''',
    "MoveTag Cancel"
)

old = '''                        onConfirm(
                            finalTags
                        )
'''
new = '''                        animatedComplete {
                            onConfirm(
                                finalTags
                            )
                        }
'''
if new not in t:
    if old not in t:
        raise SystemExit("ERROR: MoveTag Done baseline differs")
    t = t.replace(old, new, 1)

write(p, t)

# ---------------------------------------------------------------------
# 8. UserAssignmentSheet — selecting existing user closes; create stays open.
# ---------------------------------------------------------------------
p = require_file("app/src/main/java/com/lias/remote/ui/screens/devices/UserAssignmentSheet.kt")
t = add_completion_import(p, read(p))
if "val animatedComplete =" not in t:
    t = replace_once(
        t,
        '''    HigModalSheet(
        onDismiss = onDismiss
    ) {
        Column(
''',
        '''    HigModalSheet(
        onDismiss = onDismiss
    ) {
        val animatedComplete =
            rememberHigAnimatedCompletion(
                fallbackDismiss =
                    onDismiss
            )

        Column(
''',
        "UserAssignment helper"
    )

old = '''                                if (!selected) {
                                    onSelectUser(
                                        user.id
                                    )
                                }
'''
new = '''                                if (!selected) {
                                    animatedComplete {
                                        onSelectUser(
                                            user.id
                                        )
                                    }
                                }
'''
if new not in t:
    if old not in t:
        raise SystemExit("ERROR: UserAssignment select baseline differs")
    t = t.replace(old, new, 1)

# Explicitly verify Create User remains direct/non-dismissive.
if "onCreateUser(" not in t or "animatedComplete {\n                                onCreateUser" in t:
    raise SystemExit("ERROR: User creation flow must remain open/non-dismissive")

write(p, t)

# ---------------------------------------------------------------------
# 9. Policy Wizard — wait for server success; then animate dismiss.
# ---------------------------------------------------------------------
p = require_file("app/src/main/java/com/lias/remote/ui/screens/rules/PolicyWizardSheet.kt")
t = read(p)
t = ensure_import(
    t,
    "import com.lias.remote.ui.components.rememberHigAnimatedDismiss\n",
    "import com.lias.remote.ui.components.HigModalSheet\n"
)

old_sig = '''    onSave: (Policy) -> Unit
'''
new_sig = '''    onSave: suspend (Policy) -> Boolean
'''
if new_sig not in t:
    if old_sig not in t:
        raise SystemExit("ERROR: PolicyWizard onSave signature baseline differs")
    t = t.replace(old_sig, new_sig, 1)

if "suspend fun save(" not in t:
    t = t.replace(
        '''    fun save() {
''',
        '''    suspend fun save(
        animatedDismiss: () -> Unit
    ) {
''',
        1
    )

old_submit = '''        onSave(
            draft.toPolicy(
                initialPolicy
            )
        )
'''
new_submit = '''        val saved =
            onSave(
                draft.toPolicy(
                    initialPolicy
                )
            )

        if (
            saved
        ) {
            animatedDismiss()
        }
'''
if new_submit not in t:
    if old_submit not in t:
        raise SystemExit("ERROR: PolicyWizard save submit baseline differs")
    t = t.replace(old_submit, new_submit, 1)

# Add animatedDismiss inside modal provider scope.
if "val animatedDismiss =" not in t[t.find("HigModalSheet("):t.find("HigModalSheet(")+600]:
    t = replace_once(
        t,
        '''    HigModalSheet(
        onDismiss =
            onDismiss
    ) {

        Column(
''',
        '''    HigModalSheet(
        onDismiss =
            onDismiss
    ) {

        val animatedDismiss =
            rememberHigAnimatedDismiss(
                fallback =
                    onDismiss
            )

        Column(
''',
        "PolicyWizard animated dismiss"
    )

# Direct Step2 save must launch suspend.
old = '''                                } else {
                                    save()
                                }
'''
new = '''                                } else {
                                    scope.launch {
                                        save(
                                            animatedDismiss
                                        )
                                    }
                                }
'''
if new not in t:
    if old not in t:
        raise SystemExit("ERROR: PolicyWizard Step2 save call baseline differs")
    t = t.replace(old, new, 1)

# Step3 already has scope.launch { save() }.
old = '''                                scope.launch {
                                    save()
                                }
'''
new = '''                                scope.launch {
                                    save(
                                        animatedDismiss
                                    )
                                }
'''
if new not in t:
    if old not in t:
        raise SystemExit("ERROR: PolicyWizard Step3 save call baseline differs")
    t = t.replace(old, new, 1)

write(p, t)

# RulesScreen callback returns Boolean, does not close directly.
p = require_file("app/src/main/java/com/lias/remote/ui/screens/rules/RulesScreen.kt")
t = read(p)
old = '''            onSave = {
                policy ->

                if (
                    !policySaveInFlight
                ) {

                    policySaveInFlight =
                        true

                    scope.launch {

                        val result =
                            viewModel
                                .savePolicyAwait(
                                    policy
                                )

                        policySaveInFlight =
                            false

                        if (
                            result is
                            ApiResult.Success
                        ) {

                            showWizard =
                                false

                            editingPolicy =
                                null
                        }
                    }
                }
            }
'''
new = '''            onSave = {
                policy ->

                if (
                    policySaveInFlight
                ) {
                    false

                } else {

                    policySaveInFlight =
                        true

                    val result =
                        viewModel
                            .savePolicyAwait(
                                policy
                            )

                    policySaveInFlight =
                        false

                    result is
                        ApiResult.Success
                }
            }
'''
if new not in t:
    if old not in t:
        raise SystemExit("ERROR: RulesScreen PolicyWizard onSave baseline differs")
    t = t.replace(old, new, 1)

# Remove no-longer-used scope if not used elsewhere.
if "scope.launch" not in t:
    t = t.replace("import androidx.compose.runtime.rememberCoroutineScope\n", "")
    t = t.replace("import kotlinx.coroutines.launch\n", "")
    scope_block = '''    val scope =
        rememberCoroutineScope()

'''
    t = t.replace(scope_block, "")

write(p, t)

# ---------------------------------------------------------------------
# 10. SchedulePickerSheets — migrate focused custom Dialog to HigModalSheet.
# ---------------------------------------------------------------------
p = require_file("app/src/main/java/com/lias/remote/ui/screens/schedules/SchedulePickerSheets.kt")
t = read(p)

t = ensure_import(
    t,
    "import com.lias.remote.ui.components.HigModalSheet\n",
    "import com.lias.remote.ui.components.HigButtonStyle\n"
)
t = ensure_import(
    t,
    "import com.lias.remote.ui.components.rememberHigAnimatedCompletion\n",
    "import com.lias.remote.ui.components.HigModalSheet\n"
)

if "private typealias AnimatedCompletion" not in t:
    marker = "private const val WHEEL_VISIBLE_ROWS = 5\n"
    if marker not in t:
        raise SystemExit("ERROR: schedule picker constants anchor missing")
    t = t.replace(
        marker,
        "private typealias AnimatedCompletion = (() -> Unit) -> Unit\n\n" + marker,
        1
    )

# Both call sites get completion parameter.
old = '''    ) {
        Row(
'''
new = '''    ) { animatedComplete ->
        Row(
'''
if "{ animatedComplete ->" not in t:
    # first FocusedPickerDialog occurrence only
    idx = t.find("FocusedPickerDialog(")
    block_end = t.find("Row(", idx)
    prefix = t[idx:block_end]
    if old not in prefix:
        raise SystemExit("ERROR: time picker FocusedPickerDialog lambda baseline differs")
    prefix = prefix.replace(old, new, 1)
    t = t[:idx] + prefix + t[block_end:]

# Patch time Done.
old = '''                onConfirm(
                    String.format(
                        Locale.US,
                        "%02d:%02d",
                        selectedHour,
                        selectedMinute
                    )
                )
'''
new = '''                val value =
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
'''
if new not in t:
    if old not in t:
        raise SystemExit("ERROR: time Done baseline differs")
    t = t.replace(old, new, 1)

# Date call site.
date_start = t.find("fun ScheduleDatePickerSheet(")
idx = t.find("FocusedPickerDialog(", date_start)
brace = t.find(") {", idx)
if brace < 0:
    raise SystemExit("ERROR: date FocusedPickerDialog lambda not found")
if "{ animatedComplete ->" not in t[brace:brace+40]:
    t = t[:brace] + ") { animatedComplete ->" + t[brace+3:]

old = '''                onConfirm(
                    Instant
                        .ofEpochMilli(
                            selectedDateMillis
                        )
                        .atZone(
                            ZoneOffset.UTC
                        )
                        .toLocalDate().toString()
                )
'''
new = '''                val value =
                    Instant
                        .ofEpochMilli(
                            selectedDateMillis
                        )
                        .atZone(
                            ZoneOffset.UTC
                        )
                        .toLocalDate().toString()

                animatedComplete {
                    onConfirm(
                        value
                    )
                }
'''
if new not in t:
    if old not in t:
        raise SystemExit("ERROR: date Done baseline differs")
    t = t.replace(old, new, 1)

# Replace whole FocusedPickerDialog.
start = t.find("@Composable\nprivate fun FocusedPickerDialog(")
end = t.find("\n@Composable\nprivate fun NumberWheel(", start)
if start < 0 or end < 0:
    raise SystemExit("ERROR: could not bound FocusedPickerDialog")

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
t = t[:start] + new_picker + t[end:]

# Remove obsolete direct Dialog/animation imports.
for imp in [
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
    "import androidx.compose.ui.draw.clip\n",
    "import com.lias.remote.ui.theme.HigSpec\n",
]:
    t = t.replace(imp, "")

t = t.replace("// Version: 28.6.1", "// Version: 33.3.0", 1)
if "Plan 3.3 animated completion:" not in t:
    t = t.replace(
        "// - ScheduleDatePickerSheet calls HigDatePicker instead of raw CupertinoDatePicker.",
        "// - ScheduleDatePickerSheet calls HigDatePicker instead of raw CupertinoDatePicker.\n"
        "//\n"
        "// Plan 3.3 animated completion:\n"
        "// - Focused date/time picker uses HigModalSheet instead of a custom Dialog.\n"
        "// - Done waits for sheet hide before emitting its unchanged wire value.",
        1
    )

write(p, t)

# ---------------------------------------------------------------------
# Static verification.
# ---------------------------------------------------------------------
checks = {}
all_text = "\n".join(read(x) for x in SRC.rglob("*.kt"))

hig_text = read(hig)
checks["completion_helper"] = "fun rememberHigAnimatedCompletion(" in hig_text
checks["completion_in_flight"] = "var completionInFlight by" in hig_text
checks["observer_guard"] = "!completionInFlight" in hig_text
checks["completion_order"] = (
    "sheetState.hide()" in hig_text
    and "action()" in hig_text
    and hig_text.find("sheetState.hide()", hig_text.find("fun requestAnimatedCompletion")) <
        hig_text.find("action()", hig_text.find("fun requestAnimatedCompletion"))
)

expected_files = [
    "app/src/main/java/com/lias/remote/ui/screens/ExtendAccessSheet.kt",
    "app/src/main/java/com/lias/remote/ui/screens/PauseSheet.kt",
    "app/src/main/java/com/lias/remote/ui/screens/ActionSheets.kt",
    "app/src/main/java/com/lias/remote/ui/screens/schedules/ScheduleEditorSheet.kt",
    "app/src/main/java/com/lias/remote/ui/screens/devices/TagEditorSheet.kt",
    "app/src/main/java/com/lias/remote/ui/screens/devices/MoveTagSheet.kt",
    "app/src/main/java/com/lias/remote/ui/screens/devices/UserAssignmentSheet.kt",
]
for rel in expected_files:
    checks["completion_" + Path(rel).stem] = "rememberHigAnimatedCompletion" in read(ROOT / rel)

picker_text = read(ROOT / "app/src/main/java/com/lias/remote/ui/screens/schedules/SchedulePickerSheets.kt")
checks["picker_no_dialog"] = "Dialog(" not in picker_text
checks["picker_uses_hig_sheet"] = "HigModalSheet(" in picker_text
checks["picker_time_wire"] = '"%02d:%02d"' in picker_text
checks["picker_date_wire"] = ".toLocalDate().toString()" in picker_text

policy_text = read(ROOT / "app/src/main/java/com/lias/remote/ui/screens/rules/PolicyWizardSheet.kt")
rules_text = read(ROOT / "app/src/main/java/com/lias/remote/ui/screens/rules/RulesScreen.kt")
checks["policy_suspend_save"] = "onSave: suspend (Policy) -> Boolean" in policy_text
checks["policy_dismiss_only_after_success"] = "if (\n            saved\n        ) {\n            animatedDismiss()" in policy_text
checks["rules_returns_success"] = "result is\n                        ApiResult.Success" in rules_text
checks["user_create_stays_open"] = "onCreateUser(" in read(ROOT / "app/src/main/java/com/lias/remote/ui/screens/devices/UserAssignmentSheet.kt")

bad = [k for k,v in checks.items() if not v]
OUT.parent.mkdir(parents=True, exist_ok=True)
if bad:
    OUT.write_text("# Batch 003 failed\n\n" + "\n".join(f"- {x}" for x in bad) + "\n", encoding="utf-8")
    print("ERROR: Batch 003 static verification failed:")
    for x in bad:
        print(" -", x)
    sys.exit(1)

OUT.write_text(
    "# Plan 3.3 Batch 003 passed\n\n"
    "- Added completionInFlight race protection.\n"
    "- Save/Done/Confirm paths hide before existing parent callbacks run.\n"
    "- Policy wizard still closes only after LIAS save success.\n"
    "- Schedule date/time picker now uses HigModalSheet and preserves YYYY-MM-DD / HH:mm.\n"
    "- User creation intentionally stays open for subsequent selection.\n",
    encoding="utf-8"
)
print(OUT.read_text())
