# Plan 3.1 alert candidate audit

Status: candidate audit, no behavior change

## Rules

- Only short non-editing confirmations are candidates for CupertinoAlertDialog.
- Multi-field forms and identity review workflows remain sheets/forms unless separately approved.
- Destructive safeguards and server reconciliation semantics must not change.

## Material AlertDialog references

- None detected.

## Platform Dialog/Popup references

- `app/src/main/java/com/lias/remote/ui/components/HigAlertDialog.kt`
- `app/src/main/java/com/lias/remote/ui/screens/schedules/SchedulePickerSheets.kt`

## Candidate files by text scan

### `app/src/main/java/com/lias/remote/MainActivity.kt`
- L18: `//     - removes obsolete SettingsViewModel(LiasApiClient) constructor`

### `app/src/main/java/com/lias/remote/core/device/DevicePresentation.kt`
- L63: `tags.remove(`

### `app/src/main/java/com/lias/remote/core/diagnostics/Diagnostics.kt`
- L80: `"LIAS rejected the configured authentication token."`
- L123: `"LIAS Rejected the Change",`

### `app/src/main/java/com/lias/remote/core/models/DeviceIdentity.kt`
- L175: `"Identity still being confirmed"`

### `app/src/main/java/com/lias/remote/core/models/Models.kt`
- L430: `@SerialName("confirmed_by")`
- L431: `val confirmedBy: List<String>? = emptyList(),`
- L451: `val safeConfirmedBy: List<String>`
- L453: `confirmedBy ?: emptyList()`

### `app/src/main/java/com/lias/remote/core/network/ApiResult.kt`
- L28: `* Authentication/authorization was rejected by LIAS.`

### `app/src/main/java/com/lias/remote/core/network/ConnectionValidation.kt`
- L11: `//   - Rejects malformed URLs before a network request.`

### `app/src/main/java/com/lias/remote/core/network/Endpoints.kt`
- L71: `fun deviceIdentitySplit(pdid: String): String =`
- L72: `"${deviceIdentity(pdid)}/split"`

### `app/src/main/java/com/lias/remote/core/network/EngineApiTypes.kt`
- L190: `@SerialName("revoked_at")`
- L191: `val revokedAt: String? = null`
- L255: `data class IdentitySplitRequest(`
- L265: `const val IDENTITY_CANDIDATE_REOPEN = "identity_candidate_reopen"`
- L267: `const val IDENTITY_SPLIT = "identity_split"`

### `app/src/main/java/com/lias/remote/core/network/EventConstants.kt`
- L20: `const val DEVICE_REMOVED =`
- L21: `"device.removed"`
- L73: `DEVICE_REMOVED,`

### `app/src/main/java/com/lias/remote/core/network/LiasApiClient.kt`
- L194: `"LIAS rejected the authentication token."`
- L497: `> delete(`
- L503: `method = "DELETE"`
- L568: `suspend fun cancelDeviceExtension(`
- L571: `delete(`
- L595: `suspend fun cancelTagExtension(`
- L598: `delete(`

### `app/src/main/java/com/lias/remote/core/network/LiasSseClient.kt`
- L18: `//     Cancel the active request so the reconnect uses the new token.`
- L31: `import kotlinx.coroutines.CancellationException`
- L104: `* reconnect loop to reopen against the new endpoint.`
- L107: `?.cancel()`
- L146: `?.cancel()`
- L225: `error: CancellationException`
- L272: `?.cancel()`
- L278: `?.cancel()`

### `app/src/main/java/com/lias/remote/core/network/LiasTemporaryAccessApi.kt`
- L64: `return delete(`
- L112: `suspend fun LiasApiClient.cancelDeviceExtend(`
- L126: `return delete(`
- L174: `suspend fun LiasApiClient.cancelTagExtend(`
- L188: `return delete(`

### `app/src/main/java/com/lias/remote/core/policy/PolicySemantics.kt`
- L15: `//   - deleted targets`

### `app/src/main/java/com/lias/remote/core/store/SecureTokenStore.kt`
- L143: `.remove(`
- L146: `.remove(`
- L225: `.remove(`
- L228: `.remove(`

### `app/src/main/java/com/lias/remote/core/store/SettingsRepository.kt`
- L11: `//     storage and then removed.`
- L253: `* Remove plaintext storage regardless of whether a token`
- L260: `.remove(`

### `app/src/main/java/com/lias/remote/core/util/ConfigurationSafety.kt`
- L6: `//   Cross-resource dependency analysis for destructive LIAS actions.`
- L10: `//   mappings decoupled. Some backend delete operations therefore do not`
- L13: `// Android must not treat those deletes as harmless.`
- L16: `//   - Referenced schedules cannot be casually deleted.`
- L17: `//   - Tags still assigned to devices cannot be deleted.`
- L18: `//   - Tags still targeted by policies cannot be deleted.`
- L20: `//   - Built-in tags cannot be deleted.`
- L40: `val canDeleteSafely: Boolean`
- L88: `val canDeleteSafely: Boolean`
- L108: `"Built-in system tags cannot be deleted."`

### `app/src/main/java/com/lias/remote/core/util/EffectiveAccessPresentation.kt`
- L41: `val canCancelExtension: Boolean,`
- L69: `canCancelExtension =`
- L125: `canCancelExtension =`
- L152: `canCancelExtension =`
- L208: `canCancelExtension =`

### `app/src/main/java/com/lias/remote/core/util/ScheduleProjection.kt`
- L25: `//   - Identical start/end: no projected segment; validation rejects it.`

### `app/src/main/java/com/lias/remote/core/util/ScheduleValidation.kt`
- L151: `* Internal contradictions are rejected by the backend using`
- L152: `* MergeSchedules([]Schedule{s}).`

### `app/src/main/java/com/lias/remote/repositories/EventRepository.kt`
- L49: `* Mutation ownership is split into focused files, all coordinated by`
- L831: `val confirmedBy =`
- L843: `.safeConfirmedBy`
- L856: `confirmedBy.isEmpty()`
- L863: `"Device is online · verified by ${confirmedBy.size} source${if (confirmedBy.size == 1) "" else "s"}"`
- L910: `EventConstants.DEVICE_REMOVED -> {`

### `app/src/main/java/com/lias/remote/repositories/EventRepositoryActions.kt`
- L14: `//   - deletePolicy`
- L16: `//   - deleteSchedule`
- L21: `//   - deleteTag`
- L85: `remove(`

### `app/src/main/java/com/lias/remote/repositories/IdentityRepositoryActions.kt`
- L13: `import com.lias.remote.core.network.IdentitySplitRequest`
- L48: `it in setOf("pending", "confirmed", "rejected")`
- L259: `suspend fun EventRepository.confirmIdentityCandidate(`
- L278: `"confirm"`
- L296: `suspend fun EventRepository.rejectIdentityCandidate(`
- L315: `"reject"`
- L332: `suspend fun EventRepository.reopenIdentityCandidate(`
- L339: `EngineFeatures.IDENTITY_CANDIDATE_REOPEN`
- L352: `"reopen"`
- L360: `refreshIdentityCandidates("rejected")`

### `app/src/main/java/com/lias/remote/repositories/MutationCoordinator.kt`
- L83: `resourceMutexes.remove(`

### `app/src/main/java/com/lias/remote/repositories/PolicyScheduleMutations.kt`
- L6: `//   Canonical policy/schedule persistence with dependency-safe delete.`
- L9: `//   - Referenced schedules cannot be deleted from Android.`
- L249: `suspend fun EventRepository.deletePolicy(`
- L265: `"Global Access cannot be deleted."`
- L291: `api.delete<Unit>(`
- L379: `suspend fun EventRepository.deleteSchedule(`
- L452: `"Remove it from those rules before deleting the schedule. Deleting it directly would make those LIAS schedule bundles fail closed to Block."`
- L459: `api.delete<Unit>(`

### `app/src/main/java/com/lias/remote/repositories/TagMutations.kt`
- L9: `//   Backend DeleteTag removes only the tag itself.`
- L15: `// Therefore Android refuses destructive deletion while dependencies`
- L130: `suspend fun EventRepository.deleteTag(`
- L170: `"Infrastructure is immutable and cannot be deleted."`
- L181: `"Built-in system tags cannot be deleted."`
- L244: `"Delete or retarget "`
- L282: `api.delete<Unit>(`

### `app/src/main/java/com/lias/remote/repositories/TemporaryAccessRepository.kt`
- L22: `import com.lias.remote.core.network.cancelDeviceExtend`
- L23: `import com.lias.remote.core.network.cancelTagExtend`
- L79: `* LIAS may create/remove temporary internal policy and`
- L164: `suspend fun EventRepository.cancelDeviceExtensionAuthoritatively(`
- L173: `api.cancelDeviceExtend(`
- L237: `suspend fun EventRepository.cancelTagExtensionAuthoritatively(`
- L246: `api.cancelTagExtend(`

### `app/src/main/java/com/lias/remote/ui/LiasViewModel.kt`
- L21: `import com.lias.remote.repositories.cancelDeviceExtensionAuthoritatively`
- L22: `import com.lias.remote.repositories.cancelTagExtensionAuthoritatively`
- L25: `import com.lias.remote.repositories.deletePolicy`
- L26: `import com.lias.remote.repositories.deleteSchedule`
- L27: `import com.lias.remote.repositories.deleteTag`
- L35: `import com.lias.remote.repositories.confirmIdentityCandidate`
- L39: `import com.lias.remote.repositories.rejectIdentityCandidate`
- L41: `import com.lias.remote.repositories.reopenIdentityCandidate`
- L42: `import com.lias.remote.repositories.revokeIdentityBinding`
- L46: `import com.lias.remote.repositories.splitIdentity`

### `app/src/main/java/com/lias/remote/ui/components/DependencyDeleteSheets.kt`
- L2: `// File: app/src/main/java/com/lias/remote/ui/components/DependencyDeleteSheets.kt`
- L6: `//   Dependency-aware destructive-action presentation.`
- L9: `//   - Do not present an enabled destructive button for an operation`
- L10: `//     the repository will reject.`
- L34: `fun ScheduleDeleteSheet(`
- L38: `onDelete: () -> Unit`
- L61: `title = "Delete Schedule",`
- L62: `onCancel = onDismiss`
- L156: `"Edit the rules above and remove this schedule first. Delete becomes available after no policy references remain.",`
- L162: `text = "Cannot Delete While In Use",`

### `app/src/main/java/com/lias/remote/ui/components/DestructiveBiometricAuth.kt`
- L9: `object DestructiveBiometricAuth {`
- L68: `errorCode == BiometricPrompt.ERROR_USER_CANCELED ||`
- L69: `errorCode == BiometricPrompt.ERROR_CANCELED`
- L86: `.setSubtitle("Authenticate to permanently delete $objectLabel.")`
- L88: `.setNegativeButtonText("Cancel")`
- L89: `.setConfirmationRequired(true)`
- L110: `fun requiresProtectedDelete(`

### `app/src/main/java/com/lias/remote/ui/components/DetailedWeekGrid.kt`
- L23: `// Top-level import removed to fix 'Unresolved reference drawRect'.`
- L115: `val startParts = c.overlapStart.split(":")`
- L116: `val endParts = c.overlapEnd.split(":")`

### `app/src/main/java/com/lias/remote/ui/components/GroupedList.kt`
- L95: `isDestructive: Boolean = false,`
- L98: `val headlineColor = if (isDestructive) LiasThemeColors.red else LiasThemeColors.label`

### `app/src/main/java/com/lias/remote/ui/components/HigAlertDialog.kt`
- L11: `//   - Adds confirmEnabled.`
- L13: `//   - Keeps destructive-action semantics explicit.`
- L53: `confirmText: String,`
- L54: `onConfirm: () -> Unit,`
- L55: `isDestructive: Boolean = false,`
- L56: `cancelText: String = "Cancel",`
- L57: `confirmEnabled: Boolean = true,`
- L58: `onCancel: () -> Unit = {},`
- L188: `onCancel()`
- L205: `text = cancelText,`

### `app/src/main/java/com/lias/remote/ui/components/HigButton.kt`
- L13: `//   - Destructive state is communicated through semantics as well as`
- L117: `mergeDescendants =`
- L129: `"Destructive action"`
- L156: `isDestructive: Boolean = false,`
- L162: `isDestructive`
- L189: `mergeDescendants =`
- L197: `isDestructive`
- L200: `"Destructive action"`

### `app/src/main/java/com/lias/remote/ui/components/HigField.kt`
- L111: `mergeDescendants =`
- L139: `mergeDescendants =`

### `app/src/main/java/com/lias/remote/ui/components/HigLargeTitleScaffold.kt`
- L14: `//   - Removes assumptions that large text always fits one line.`

### `app/src/main/java/com/lias/remote/ui/components/HigSheets.kt`
- L204: `onCancel: () -> Unit,`
- L228: `"Cancel",`
- L230: `onCancel`
- L254: `* Keeps title visually centered against the Cancel button.`

### `app/src/main/java/com/lias/remote/ui/components/MinutePickerSheet.kt`
- L25: `onConfirm: (minutes: Int) -> Unit,`
- L39: `HigSheetHeader(title = "Extend Access", onCancel = onDismiss)`
- L80: `onClick = { onConfirm(selectedMinutes.toInt()) },`

### `app/src/main/java/com/lias/remote/ui/components/SegmentedControl.kt`
- L30: `isDestructive: Boolean = false`
- L48: `val destructive =`
- L49: `isDestructive &&`
- L61: `destructive ->`

### `app/src/main/java/com/lias/remote/ui/components/StatusPills.kt`
- L13: `//   - Removes forced uppercase from visible status text to improve`
- L148: `mergeDescendants =`

### `app/src/main/java/com/lias/remote/ui/components/UndoToast.kt`
- L13: `//     - recreate a deleted server Policy from stale cached data`
- L14: `//     - recreate deleted schedules/tags`

### `app/src/main/java/com/lias/remote/ui/navigation/LiasNavHost.kt`
- L855: `mergeDescendants =`

### `app/src/main/java/com/lias/remote/ui/screens/ActionSheets.kt`
- L267: `onCancel = onDismiss`
- L298: `isDestructive = true,`

### `app/src/main/java/com/lias/remote/ui/screens/ExtendAccessSheet.kt`
- L56: `onConfirm: (minutes: Int) -> Unit,`
- L57: `onCancelExtension: (() -> Unit)? = null`
- L120: `onCancel =`
- L306: `onConfirm(`
- L324: `onCancelExtension !=`
- L330: `"Cancel Extended Access",`
- L332: `onCancelExtension,`
- L333: `isDestructive =`

### `app/src/main/java/com/lias/remote/ui/screens/PauseSheet.kt`
- L6: `//   Server-aligned Pause confirmation.`
- L12: `// Batch 24 intentionally removes the fake 15/30/60/120-minute Pause`
- L16: `//   onConfirm still returns Int so pre-Batch-24 call sites compile.`
- L44: `onConfirm: (minutes: Int) -> Unit`
- L75: `onCancel =`
- L130: `onConfirm(`

### `app/src/main/java/com/lias/remote/ui/screens/devices/DeviceDetailScreen.kt`
- L14: `//   - Extend supports active extension management/cancellation.`
- L16: `//   - Removes emoji device/action icons.`
- L959: `onConfirm = {`
- L971: `onCancelExtension =`
- L978: `.cancelDeviceExtension(`
- L1004: `onConfirm = {`
- L1031: `onConfirm = {`
- L1057: `onConfirm = {`
- L1162: `"This device may have been removed, reidentified, or is no longer returned by LIAS."`

### `app/src/main/java/com/lias/remote/ui/screens/devices/DeviceRenameDialog.kt`
- L30: `onConfirm: (String) -> Unit`
- L53: `confirmText =`
- L55: `confirmEnabled =`
- L57: `onConfirm = {`
- L58: `onConfirm(`

### `app/src/main/java/com/lias/remote/ui/screens/devices/DevicesScreen.kt`
- L460: `onConfirm = {`
- L472: `onCancelExtension =`
- L480: `.cancelDeviceExtension(`
- L527: `onConfirm = {`
- L587: `onConfirm = {`
- L603: `onCancelExtension =`
- L610: `.cancelTagExtension(`

### `app/src/main/java/com/lias/remote/ui/screens/devices/MoveTagSheet.kt`
- L11: `//   - infrastructure cannot be granted or removed here.`
- L60: `onConfirm: (tagIds: List<String>) -> Unit`
- L139: `"Cancel",`
- L198: `finalTags.remove(`
- L213: `onConfirm(`
- L405: `selected.remove(`
- L414: `selected.remove(`

### `app/src/main/java/com/lias/remote/ui/screens/devices/TagEditorSheet.kt`
- L49: `import com.lias.remote.ui.components.TagDeleteSheet`
- L60: `onDelete: (() -> Unit)? = null`
- L95: `var showDeleteSheet by`
- L165: `onCancel =`
- L348: `onDelete != null`
- L358: `dependencyImpact.canDeleteSafely`
- L369: `dependencyImpact.canDeleteSafely`
- L371: `"Delete Tag"`
- L373: `"Review Delete Dependencies"`
- L376: `showDeleteSheet =`

### `app/src/main/java/com/lias/remote/ui/screens/devices/UserAssignmentSheet.kt`
- L87: `onCancel =`

### `app/src/main/java/com/lias/remote/ui/screens/home/HomeScreen.kt`
- L499: `onConfirm = { minutes ->`
- L503: `onCancelExtension =`
- L506: `viewModel.cancelDeviceExtension(device.pdid)`

### `app/src/main/java/com/lias/remote/ui/screens/identity/IdentityReviewScreen.kt`
- L52: `MERGE,`
- L53: `REJECT,`
- L54: `REOPEN,`
- L56: `SPLIT,`
- L57: `REVOKE`
- L71: `var confirmation by remember { mutableStateOf("") }`
- L74: `var splitMac by remember { mutableStateOf("") }`
- L75: `var revokeTarget by remember { mutableStateOf<Pair<String, Long>?>(null) }`
- L132: `options = listOf("Pending", "Confirmed", "Rejected"),`
- L221: `onMerge = {`

### `app/src/main/java/com/lias/remote/ui/screens/rules/PolicyScheduleSelector.kt`
- L6: `//   Multi-schedule selection + merged timeline preview.`
- L155: `"MERGED TIMELINE",`

### `app/src/main/java/com/lias/remote/ui/screens/rules/PolicyWizardSheet.kt`
- L236: `* ALLOW according to LIAS. No server merge is necessary.`
- L427: `onCancel =`
- L663: `updated.remove(`
- L1018: `isDestructive =`
- L1230: `"LIAS Rejected This Bundle",`

### `app/src/main/java/com/lias/remote/ui/screens/rules/RulesScreen.kt`
- L40: `import com.lias.remote.ui.components.DestructiveBiometricAuth`
- L42: `import com.lias.remote.ui.components.requiresProtectedDelete`
- L98: `var policyToDelete by`
- L105: `var policyDeleteAuthError by`
- L391: `onDelete = {`
- L393: `policyDeleteAuthError =`
- L396: `policyToDelete =`
- L463: `onDelete = {`
- L465: `policyDeleteAuthError =`
- L468: `policyToDelete =`

### `app/src/main/java/com/lias/remote/ui/screens/schedules/ScheduleEditorSheet.kt`
- L188: `onCancel =`
- L509: `canDelete =`
- L520: `onDelete = {`
- L533: `updated.removeAt(`

### `app/src/main/java/com/lias/remote/ui/screens/schedules/SchedulePickerSheets.kt`
- L87: `onConfirm: (String) -> Unit`
- L150: `onConfirm(`
- L170: `onConfirm: (String) -> Unit`
- L239: `onConfirm(`
- L339: `onCancel = onDismiss`

### `app/src/main/java/com/lias/remote/ui/screens/schedules/ScheduleRuleEditor.kt`
- L51: `onDelete: (() -> Unit)? = null,`
- L103: `onDelete != null`
- L107: `"Delete",`
- L109: `onDelete`
- L351: `isDestructive =`

### `app/src/main/java/com/lias/remote/ui/screens/schedules/ScheduleRuleEditorCard.kt`
- L49: `canDelete: Boolean,`
- L51: `onDelete: () -> Unit`
- L95: `if (canDelete) {`
- L96: `HigTextButton(text = "Remove", onClick = onDelete, isDestructive = true)`
- L133: `if (!updated.add(day)) updated.remove(day)`
- L241: `onConfirm = { value ->`
- L266: `onConfirm = { value ->`

### `app/src/main/java/com/lias/remote/ui/screens/schedules/SchedulesScreen.kt`
- L11: `//   - Delete is disabled until all references are removed.`
- L12: `//   - Removes false "defaults to open" wording.`
- L45: `import com.lias.remote.ui.components.DestructiveBiometricAuth`
- L47: `import com.lias.remote.ui.components.requiresProtectedDelete`
- L55: `import com.lias.remote.ui.components.ScheduleDeleteSheet`
- L94: `var scheduleToDelete by`
- L99: `var scheduleDeleteAuthError by`
- L273: `impact.canDeleteSafely`
- L280: `scheduleDeleteAuthError =`
- L283: `scheduleToDelete =`

### `app/src/main/java/com/lias/remote/ui/screens/settings/SettingsScreen.kt`
- L19: `// Removed:`
- L643: `isDestructive =`
- L751: `// Policy restore confirmation`
- L771: `confirmText =`
- L773: `onConfirm = {`
- L791: `isDestructive =`
- L797: `// Firewall confirmation`
- L813: `"This temporarily removes LIAS access-control enforcement. Use it only for troubleshooting. LIAS will rebuild its rules on a subsequent synchronization.",`
- L814: `confirmText =`
- L816: `onConfirm = {`

