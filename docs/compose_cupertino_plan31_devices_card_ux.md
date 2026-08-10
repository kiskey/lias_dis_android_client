# Compose Cupertino Plan 3.1 — Devices card UX

Status: implementation slice  
Date: 2026-08-10

## Goal

Bring Devices screen card behavior in line with Home screen:
- no visible per-card `Extend Access`, `Manage Access`, `Pause`, or `Details` button row for ordinary device cards;
- tapping the device card opens the correct current-state modal:
  - extend/manage extension sheet when extension is available or active;
  - pause sheet when pause is available;
- if a paused device exposes resume, keep explicit `Resume` button because it is an immediate mutation, not a modal;
- trailing `›` disclosure opens device details;
- Active Protections tag navigation from Home must show the selected tag group correctly without requiring the user to clear search text.

## Root-cause diagnosis

Home -> Active Protections tag navigation passes `initialTagId` to Devices, and Devices scopes the device list by that tag. But Devices also initialized `searchQuery` to the tag name. The scoped list was then filtered a second time by free-text search. If device names/IP/MAC/hostnames did not contain the tag name, the scoped result looked empty. Clearing search showed the correct tag-scoped devices.

## Contract boundary

This is UI/navigation-state only. It must not change REST, SSE, PDID, persistence/settings keys, policy/schedule/extension/pause semantics, repository ownership, or server-authoritative evaluation.
