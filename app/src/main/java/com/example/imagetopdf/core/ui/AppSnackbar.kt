package com.example.imagetopdf.core.ui

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

val LocalAppSnackbarHostState = compositionLocalOf<SnackbarHostState> {
    error("AppSnackbarHostState not provided")
}

@Composable
fun rememberAppSnackbarHostState(): SnackbarHostState = remember { SnackbarHostState() }

@Composable
fun AppSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState = hostState, modifier = modifier)
}
