# Compose Cupertino Plan 3.1 — date wheel picker

Status: implementation slice  
Date: 2026-08-10

## Verified library support

The maintained Slanoss/Schott Cupertino fork supports `CupertinoDatePicker`
with `DatePickerStyle.Wheel()`.

The library implementation defines separate wheel components:
- `DatePickerComponent.Day`
- `DatePickerComponent.Month`
- `DatePickerComponent.Year`

Those components each render through `CupertinoWheelPicker`. The state backing
them is also separated:
- `dayState`
- `monthState`
- `yearState`

Therefore the day, month, and year columns are independently scrollable. This is
different from LIAS' previous implementation, which generated one sequential
list of complete dates such as `Mon, Aug 10, 2026`.

## LIAS goal

Replace only the date picker sheet's sequential complete-date wheel with
Slanoss `CupertinoDatePicker(style = DatePickerStyle.Wheel())`.

## Contract boundary

This is UI-only. It must not change schedule REST payloads, schedule model fields,
schedule validation semantics, policy/schedule engine semantics, persistence,
SSE, or PDID/device identity. LIAS wire format remains `YYYY-MM-DD`.
