# LIAS Android Compose Cupertino Plan 3.0 full patch scripts

Use this pack when the local `scripts/` folder was accidentally removed or when you want to re-apply the full migration support cleanly.

## Recommended command

Copy this folder's contents into the repo root, then run:

```bash
chmod +x apply_all_plan30_preacceptance.sh apply_batch_*.sh
./apply_all_plan30_preacceptance.sh
```

This applies Batches 001, 002, 003, 004, 005, 007, 008, 009, and 010. It intentionally skips Batch 006 because Batch 006 is post-acceptance only.

## After the local static gate passes

```bash
git status
git add .
git commit -m "Migrate Compose Cupertino Plan 3.0"
git push origin luna
```

Then check the GitHub Actions workflow:

```text
LIAS Android Cupertino Plan 3.0
```

## Post-acceptance only

After local static validation, GitHub Actions validation, and runtime acceptance matrix review all pass:

```bash
./apply_batch_006_post_acceptance_contract_record.sh --accepted
git add docs/lias_engine_contract.md
git commit -m "Record accepted Compose Cupertino Plan 3.0 baseline"
git push origin luna
```
