package com.gridtype.keyboard

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Main : NavKey
@Serializable data object Setup : NavKey
@Serializable data object Preferences : NavKey
