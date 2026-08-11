# Compose Cupertino Plan 3.0 remote Actions workflow

Status: local wrapperless workflow support

## Purpose

This project checkout may not include `./gradlew`. Plan 3.0 validation is
therefore split into:

1. local static migration validation; and
2. full remote Android validation in GitHub Actions using installed Gradle 8.11.1.

## Local command

Run from the repository root:

```bash
scripts/run_cupertino_migration_gate.sh
```

This validates:

- Kotlin `2.2.0`;
- AGP `8.10.1`;
- Compose BOM `2025.06.00`;
- serialization JSON `1.7.3`;
- Cupertino fork `io.github.schott12521:*:2.3.1`;
- `compileSdk 36`, `minSdk 26`, `targetSdk 35`;
- Java/JVM 17;
- absence of the old Cupertino namespace/group from app/build inputs;
- presence of the new `com.slapps.cupertino` namespace in app source.

## Remote command

Commit and push the migration branch, then run:

```bash
git status
git add .
git commit -m "Migrate Compose Cupertino Plan 3.0"
git push origin luna
```

GitHub Actions workflow:

```text
LIAS Android Cupertino Plan 3.0
```

Remote validation runs:

- `:app:compileDebugKotlin`;
- `:app:compileReleaseKotlin`;
- `:app:testDebugUnitTest`;
- `:app:lintDebug`;
- `:app:assembleRelease`;
- debug/release runtime dependency reports;
- old/new Cupertino dependency graph assertions.

## Acceptance rule

Do not run the post-acceptance contract recorder until:

- local static validation passes;
- the GitHub Actions workflow passes;
- the runtime acceptance matrix is reviewed and accepted.

