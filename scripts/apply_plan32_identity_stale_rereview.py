#!/usr/bin/env python3
from pathlib import Path
ROOT = Path.cwd()
FILE = ROOT / "app/src/main/java/com/lias/remote/repositories/IdentityRepositoryActions.kt"
OUT = ROOT / "build/plan32/batch003_identity_stale_rereview.md"
if not FILE.exists():
    raise SystemExit("ERROR: IdentityRepositoryActions.kt not found")
text = FILE.read_text(encoding="utf-8")

old_common = '''        } else if (result is ApiResult.ConflictError) {
            clearSelectedIdentityCandidate()
            refreshIdentityCandidates()
        }

        result
'''
new_confirm = '''        } else if (result is ApiResult.ConflictError) {
            // Plan 3.2 stale candidate refresh:
            refreshIdentityCandidates(
                _state.value.identityReview.status
            )
            loadIdentityCandidate(
                candidate.id
            )
            return@identityMutation ApiResult.ConflictError(
                conflicts = result.conflicts,
                message =
                    "This identity review changed on LIAS or is currently unsafe to merge. The latest evidence has been reloaded; review it again before deciding."
            )
        }

        result
'''
new_reject = '''        } else if (result is ApiResult.ConflictError) {
            // Plan 3.2 stale candidate refresh:
            refreshIdentityCandidates(
                _state.value.identityReview.status
            )
            loadIdentityCandidate(
                candidate.id
            )
            return@identityMutation ApiResult.ConflictError(
                conflicts = result.conflicts,
                message =
                    "This identity review changed on LIAS. The latest evidence has been reloaded; review it again before deciding."
            )
        }

        result
'''
old_reopen = '''        } else if (result is ApiResult.ConflictError) {
            clearSelectedIdentityCandidate()
            refreshIdentityCandidates("rejected")
        }

        result
'''
new_reopen = '''        } else if (result is ApiResult.ConflictError) {
            // Plan 3.2 stale candidate refresh:
            refreshIdentityCandidates("rejected")
            loadIdentityCandidate(
                candidate.id
            )
            return@identityMutation ApiResult.ConflictError(
                conflicts = result.conflicts,
                message =
                    "This identity review changed on LIAS. The latest evidence has been reloaded; review it again before reopening."
            )
        }

        result
'''
if "Plan 3.2 stale candidate refresh" not in text:
    if text.count(old_common) < 2:
        raise SystemExit("ERROR: current confirm/reject 409 block shape not found twice")
    text = text.replace(old_common, new_confirm, 1)
    text = text.replace(old_common, new_reject, 1)
    if old_reopen not in text:
        raise SystemExit("ERROR: current reopen 409 block shape not found")
    text = text.replace(old_reopen, new_reopen, 1)
FILE.write_text(text, encoding="utf-8")
checks = {
    "markers": text.count("Plan 3.2 stale candidate refresh") >= 3,
    "review_again": text.count("review it again") >= 3,
    "reloads": text.count("loadIdentityCandidate(") >= 4,
}
bad = [k for k,v in checks.items() if not v]
OUT.parent.mkdir(parents=True, exist_ok=True)
if bad:
    OUT.write_text("# Batch 003 failed\n\n" + "\n".join(f"- {x}" for x in bad), encoding="utf-8")
    raise SystemExit("ERROR: " + ", ".join(bad))
OUT.write_text(
    "# Batch 003 passed\n\n"
    "- HTTP 409 no longer discards the reviewed candidate.\n"
    "- Queue refreshes from LIAS.\n"
    "- Candidate detail reloads by ID.\n"
    "- Admin receives explicit review-again messaging.\n",
    encoding="utf-8",
)
print(OUT.read_text())
