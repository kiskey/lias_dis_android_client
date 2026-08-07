// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/policies/PoliciesScreen.kt
// Version: 2.1.0
// Audit Fixes:
//   1. Legacy route wrapper mapped directly to RulesScreen.
// ====================================================================

package com.lias.remote.ui.screens.policies

import androidx.compose.runtime.Composable
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.screens.rules.RulesScreen

@Composable
fun PoliciesScreen(viewModel: LiasViewModel) {
    RulesScreen(viewModel = viewModel)
}
