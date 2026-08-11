#!/usr/bin/env python3
from pathlib import Path
ROOT = Path.cwd()
P = ROOT / "app/src/main/java/com/lias/remote/core/policy/PolicyPresentation.kt"
W = ROOT / "app/src/main/java/com/lias/remote/ui/screens/rules/PolicyWizardSheet.kt"
OUT = ROOT / "build/plan32/batch004_conflict_ux.md"
for f in [P, W]:
    if not f.exists():
        raise SystemExit(f"ERROR: missing {f.relative_to(ROOT)}")
pt = P.read_text(encoding="utf-8")
wt = W.read_text(encoding="utf-8")

if "fun serverValidationMessage(" not in pt:
    addition = r'''
    /**
     * Translate verified LIAS 2.0 validation failures into admin-facing text.
     * LIAS remains authoritative; Android does not resolve conflicts locally.
     */
    fun serverValidationMessage(
        message: String
    ): String {
        val normalized =
            message.trim()

        return when {
            normalized.contains(
                "mixed timezones",
                ignoreCase = true
            ) ->
                "LIAS cannot combine schedules that use different timezones. Set the selected schedules to one timezone, then validate again."

            normalized.contains(
                "schedule conflict",
                ignoreCase = true
            ) ||
                normalized.contains(
                    "contradictory",
                    ignoreCase = true
                ) ->
                "LIAS found overlapping schedule windows with opposite Allow/Block actions. Resolve the listed windows before saving; conflicted bundles fail closed to Block."

            normalized.contains(
                "missing",
                ignoreCase = true
            ) &&
                normalized.contains(
                    "schedule",
                    ignoreCase = true
                ) ->
                "One of the schedules in this rule no longer exists on LIAS. Refresh the rule and select only existing schedules."

            else ->
                normalized
        }
    }
'''
    pos = pt.rfind("\n}")
    if pos < 0:
        raise SystemExit("ERROR: PolicyPresentation object end not found")
    pt = pt[:pos] + "\n" + addition + pt[pos:]
    P.write_text(pt, encoding="utf-8")

wt = wt.replace(
    '"Choose one or more reusable schedules. LIAS combines them into one effective weekly bundle."',
    '"Choose one or more reusable schedules. LIAS combines them into one effective schedule bundle and validates weekly and calendar-date conflicts."'
)

old_tz = '''        WarningText(
            title =
                "Mixed Timezones",
            text =
                "Selected schedules use ${timezones.joinToString()}. This is difficult to reason about; aligning their timezones is recommended."
        )'''
new_tz = '''        WarningText(
            title =
                "LIAS Cannot Merge Mixed Timezones",
            text =
                "LIAS 2.0 rejects schedule bundles whose schedules use different timezones. Selected: ${timezones.joinToString()}. Change them to one timezone before saving."
        )'''
if old_tz in wt:
    wt = wt.replace(old_tz, new_tz, 1)

wt = wt.replace(
'''                validationError =
                    result.message''',
'''                validationError =
                    PolicyPresentation
                        .serverValidationMessage(
                            result.message
                        )'''
)

old_conflict = '''        ConflictList(
            title =
                "LIAS Rejected This Bundle",
            conflicts =
                serverConflicts
        )'''
new_conflict = '''        ConflictList(
            title =
                "LIAS Rejected This Bundle",
            conflicts =
                serverConflicts
        )

        CupertinoText(
            text =
                "LIAS does not silently choose between contradictory Allow and Block windows. A conflicted bundle fails closed to Block until the overlap is resolved.",
            style =
                HigTypography.caption,
            color =
                LiasThemeColors.red
        )'''
if old_conflict in wt and "does not silently choose between contradictory" not in wt:
    wt = wt.replace(old_conflict, new_conflict, 1)

old_guard = '''                isValidating ||
                    localConflicts.isNotEmpty() ||
                    serverConflicts.isNotEmpty()
                )'''
new_guard = '''                isValidating ||
                    validationError != null ||
                    localConflicts.isNotEmpty() ||
                    serverConflicts.isNotEmpty()
                )'''
if old_guard in wt:
    wt = wt.replace(old_guard, new_guard, 1)

old_enabled = '''                                !isValidating &&
                                    localConflicts
                                        .isEmpty() &&
                                    serverConflicts
                                        .isEmpty(),'''
new_enabled = '''                                !isValidating &&
                                    validationError == null &&
                                    localConflicts
                                        .isEmpty() &&
                                    serverConflicts
                                        .isEmpty(),'''
if old_enabled in wt:
    wt = wt.replace(old_enabled, new_enabled, 1)

W.write_text(wt, encoding="utf-8")
pt = P.read_text(encoding="utf-8")
wt = W.read_text(encoding="utf-8")
checks = {
    "humanizer": "fun serverValidationMessage(" in pt,
    "mixed_timezone": "LIAS Cannot Merge Mixed Timezones" in wt,
    "calendar_conflicts": "weekly and calendar-date conflicts" in wt,
    "fail_closed": "fails closed to Block" in wt,
    "guard": "validationError != null" in wt,
    "button": "validationError == null" in wt,
}
bad = [k for k,v in checks.items() if not v]
OUT.parent.mkdir(parents=True, exist_ok=True)
if bad:
    OUT.write_text("# Batch 004 failed\n\n" + "\n".join(f"- {x}" for x in bad), encoding="utf-8")
    raise SystemExit("ERROR: " + ", ".join(bad))
OUT.write_text(
    "# Batch 004 passed\n\n"
    "- Mixed timezone is treated as a LIAS rejection.\n"
    "- Weekly + calendar-date conflict wording is synchronized.\n"
    "- Fail-closed Block behavior is explained.\n"
    "- Authoritative validationError blocks Save Rule.\n",
    encoding="utf-8",
)
print(OUT.read_text())
