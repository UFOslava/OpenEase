# GridType Workspace Developer Rules

These rules apply to all AI developer sessions in this repository.

## Workflow Guidelines

### 1. Mandatory Compilation & Testing
- Before completing any task or marking changes as done, you **must** compile the code to verify there are no compilation or syntax errors.
- Run `.\gradlew compileDebugSources` or the appropriate test commands to ensure correctness.

### 2. Device Deployment Check
- Check if a physical phone or an active emulator is connected via ADB by running `adb devices`.
- If a device is detected in the list, automatically attempt to install and launch the updated application using the `android run` command (specifying the target APK and device serial).
- If no device is connected, report this to the user.

### 3. Automated Git Commits
- Once changes have been verified and build successfully, stage all changes (`git add -A`) and make a Git commit with a descriptive, concise commit message.
