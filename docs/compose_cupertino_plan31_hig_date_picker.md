# Compose Cupertino Plan 3.1 — HigDatePicker

Status: implementation slice  
Date: 2026-08-10

## Goal

Use a LIAS-owned wrapper for Slanoss `CupertinoDatePicker`.

Default mode is `Wheel`, because Slanoss wheel date picker provides independent
day, month, and year columns. `Pager` remains available for future date-only
calendar screens, but is not the default for LIAS schedule date entry.

## Contract boundary

This wrapper owns UI date selection only. It must not change LIAS schedule wire
format, DTOs, REST, SSE, persistence, PDID, or policy/schedule semantics.
