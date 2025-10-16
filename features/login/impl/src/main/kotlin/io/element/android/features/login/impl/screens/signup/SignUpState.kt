/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.signup

import android.os.Parcelable
import io.element.android.features.login.impl.accountprovider.AccountProvider
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.matrix.api.core.SessionId
import kotlinx.parcelize.Parcelize

data class SignUpState(
    val accountProvider: AccountProvider,
    val formState: SignUpFormState,
    val signUpAction: AsyncData<SessionId>,
    val appName: String,
    val eventSink: (SignUpEvents) -> Unit
) {
    val submitEnabled: Boolean
        get() = signUpAction !is AsyncData.Loading &&
            formState.username.isNotEmpty() &&
            formState.password.isNotEmpty() &&
            formState.confirmPassword.isNotEmpty() &&
            formState.password == formState.confirmPassword &&
            formState.password.length >= 8
}

@Parcelize
data class SignUpFormState(
    val username: String,
    val password: String,
    val confirmPassword: String,
) : Parcelable {
    companion object {
        val Default = SignUpFormState("", "", "")
    }
}
