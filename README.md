# LIAS Schedule / Policy Conflict Patch Set

Target: `kiskey/lias_dis` branch `enhancement2.0`.

## Batches

1. **Calendar runtime + validation**
   - Fixes date-range overnight windows continuing after midnight.
   - Rejects partial/reversed date ranges, bad days, invalid rule actions, invalid schedule modes.

2. **Date-aware conflict detection**
   - Detects date-vs-weekly and date-vs-date contradictory overlaps in LIAS core.
   - Prevents finite calendar rules being projected as weekly forever.
   - De-duplicates schedule IDs in bundles.
   - Dashboard local checker stops falsely projecting calendar rules; LIAS core/API remains authoritative.

3. **Policy precedence + tag priority**
   - Deterministic same-priority tie breaking: priority, then Block > Schedule > Allow, then updated time, then ID.
   - Applies priority within each tag so high-priority Tag Extend Access actually overrides that tag's base policy.
   - Preserves cross-tag fail-closed combination.
   - Empty non-global schedule policies fail closed in the engine.
   - Tag effective-status API/dashboard path is aligned with actual core enforcement.

4. **API validation + acceptance**
   - Propagates merge errors such as mixed timezones instead of silently accepting them.
   - Rejects non-global schedule policies without schedules.
   - Locks mixed whitelist+downtime semantics: outside all windows, the composite whitelist bundle blocks.
   - Runs targeted LIAS tests and the complete Go workspace acceptance suite.

## Run all

```bash
unzip lias_schedule_policy_conflict_patches.zip
chmod +x lias_schedule_policy_conflict_patches/*.patch
cd /path/to/lias_dis
/path/to/lias_schedule_policy_conflict_patches/run_all_lias_conflict_batches.patch "$PWD"
```

The scripts are designed for the existing Go workspace (`go.work`) and use:

```bash
go test ./apps/discovery-service/... ./apps/lias/... ./pkg/oui/... ./shared/...
```

Core LIAS remains the authority. No DIS API or shared Device JSON contract is changed.
