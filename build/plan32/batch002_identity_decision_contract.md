# Batch 002 passed — no source rewrite required

Current luna already sends the complete LIAS 2.0 optimistic-concurrency contract:

- expected_source_pdid
- expected_target_pdid
- expected_updated_at
- decision_note

IdentityRepositoryActions.kt centralizes these fields in IdentityCandidateDetail.decisionRequest(note), and confirm/reject/reopen use that helper.
