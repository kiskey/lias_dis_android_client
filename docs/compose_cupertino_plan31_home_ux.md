# Compose Cupertino Plan 3.1 — Home UX extension

Status: implementation patch plan  
Scope: Plan 3.1 UX adoption plus explicitly requested Home-screen refinements  
Date: 2026-08-10

## Contract boundary

This is a UI-only Android change. It must not change LIAS REST paths, JSON
payloads, SSE behavior, PDID keying, repository ownership, authentication,
persistence, policy/schedule semantics, or server-authoritative enforcement.

Android remains a LIAS server client. The server remains authoritative for
identity, inventory, effective access, persistence, policy enforcement, and
event replay.

## User-approved Home-screen changes

1. Restricted device cards:
   - remove distinct visible per-card action buttons for `Extend access` and
     `Details`;
   - tapping the card opens the existing extend-access modal for that device;
   - tapping the trailing right-side disclosure opens the existing device details
     flow.

2. Active Protections group-policy cards:
   - tapping a group/tag policy card navigates to Devices already scoped to that
     specific group/tag instead of opening the generic Devices screen.

3. Cupertino/HIG visual boundary:
   - continue using the maintained `com.slapps.cupertino` package;
   - do not introduce Material icons into Home cards;
   - prefer Cupertino icons already present in the maintained fork;
   - if a disclosure/chevron icon cannot be verified, use a text disclosure glyph
     as a safe fallback and record the gap.

## Non-goals

- No new LIAS/DIS endpoint.
- No identity mutation behavior change.
- No device-policy semantics change.
- No local discovery/correlation.
- No bottom-sheet host migration unless separately approved.
- No replacement of REST/SSE models.
- No new public Kotlin contract unless a patch explicitly records it.

## Acceptance criteria

- Debug and release builds pass in GitHub Actions.
- Existing unit/contract tests pass.
- Restricted device card tap opens the same extend-access modal formerly opened
  by the `Extend access` button.
- Restricted device trailing disclosure opens the same details flow formerly
  opened by the `Details` button.
- Active Protections group-policy card opens Devices scoped to that group/tag.
- Home cards do not import Material icons.
- Old Cupertino package `io.github.alexzhirkevich` remains absent.
- Existing routes continue to work.
- No server/API/PDID/persistence/policy behavior changes.
