# Plan 3.4 — Cupertino Navigation & Motion Parity acceptance

Accepted only when static gates and the full Gradle gate pass.

## Navigation
- Root tabs are peers, not hierarchical pushes.
- Detail/settings/admin destinations use spatial push/pop transitions.
- Navigation Compose 2.8.9 supplies predictive in-app back progress.
- Predictive back is explicitly enabled in the manifest.
- Connect -> configured app has an explicit root transition.

## Slanoss 2.3.1 utilization
- CupertinoNavigationBar / CupertinoNavigationBarItem.
- CupertinoTopAppBar / CupertinoNavigationTitle.
- CupertinoScaffold translucency coordination.
- CupertinoNavigateBackButton.
- CupertinoAlertDialog for compact alerts.
- CupertinoActionSheet for Global Access choices.

## Accessibility / motion
Plan 3.4 uses standard finite Compose animation APIs. Compose MotionDurationScale
therefore scales their duration and completes them immediately when system motion
duration scale is zero.

## Project boundary
No REST, SSE, DTO, repository, policy, schedule, identity-engine, or firewall
contract changes.
