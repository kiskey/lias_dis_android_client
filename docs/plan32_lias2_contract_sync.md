# Plan 3.2 — LIAS 2.0 Android contract synchronization

Reference engine: LIAS/DIS enhancement2.0.

Verified LIAS behavior:
- identity candidates are provided through LIAS list/detail/confirm/reject/reopen endpoints;
- identity decisions support decision_note plus expected_source_pdid,
  expected_target_pdid and expected_updated_at;
- stale/changed candidates and simultaneous-online merge attempts return HTTP 409;
- contradictory schedule windows are rejected;
- calendar-date rules participate in conflict detection;
- mixed-timezone schedule bundles are rejected;
- conflicted bundles fail closed to Block at evaluation;
- Android remains presentation/admin control only and must not duplicate engine authority.
