/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.signup

sealed interface SignUpEvents {
    data class SetUsername(val username: String) : SignUpEvents
    data class SetPassword(val password: String) : SignUpEvents
    data class SetConfirmPassword(val confirmPassword: String) : SignUpEvents
    data object Submit : SignUpEvents
    data object ClearError : SignUpEvents
}
