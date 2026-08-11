# Plan 3.1 Home card interaction patch report

Home file: `app/src/main/java/com/lias/remote/ui/screens/home/HomeScreen.kt`
Removed Extend access button blocks: 0
Removed Details button blocks: 1
Detected extend callback: True
Detected details callback: True

## Required review

- Restricted device card surface must call the existing extend-access callback.
- Trailing disclosure must call the existing details callback.
- No Material icon imports are allowed.

ERROR: expected visible button blocks were not found or the source shape is unsupported.
No source file was modified.
Set PLAN31_HOME_FILE to the exact Home screen file and rerun, or share this report for an exact patch.
