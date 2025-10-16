/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.signup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import dev.zacsweers.metro.Inject
import io.element.android.features.login.impl.accountprovider.AccountProviderDataSource
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.core.extensions.flatMap
import io.element.android.libraries.core.meta.BuildMeta
import io.element.android.libraries.matrix.api.auth.MatrixAuthenticationService
import io.element.android.libraries.matrix.api.core.SessionId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Inject
class SignUpPresenter(
    private val authenticationService: MatrixAuthenticationService,
    private val accountProviderDataSource: AccountProviderDataSource,
    private val registrationService: RegistrationService,
    private val buildMeta: BuildMeta,
) : Presenter<SignUpState> {
    @Composable
    override fun present(): SignUpState {
        val localCoroutineScope = rememberCoroutineScope()
        val signUpAction: MutableState<AsyncData<SessionId>> = remember {
            mutableStateOf(AsyncData.Uninitialized)
        }

        val formState = rememberSaveable {
            mutableStateOf(SignUpFormState.Default)
        }
        val accountProvider by accountProviderDataSource.flow.collectAsState()

        // Configure the authentication service with our predetermined server
        LaunchedEffect(accountProvider.url) {
            if (accountProvider.url.isNotEmpty()) {
                authenticationService.setHomeserver(accountProvider.url)
            }
        }

        fun handleEvents(event: SignUpEvents) {
            when (event) {
                is SignUpEvents.SetUsername -> updateFormState(formState) {
                    copy(username = event.username)
                }
                is SignUpEvents.SetPassword -> updateFormState(formState) {
                    copy(password = event.password)
                }
                is SignUpEvents.SetConfirmPassword -> updateFormState(formState) {
                    copy(confirmPassword = event.confirmPassword)
                }
                SignUpEvents.Submit -> {
                    localCoroutineScope.submit(formState.value, signUpAction, accountProvider.url)
                }
                SignUpEvents.ClearError -> signUpAction.value = AsyncData.Uninitialized
            }
        }

        return SignUpState(
            accountProvider = accountProvider,
            formState = formState.value,
            signUpAction = signUpAction.value,
            appName = buildMeta.productionApplicationName,
            eventSink = ::handleEvents
        )
    }

    private fun CoroutineScope.submit(
        formState: SignUpFormState,
        signUpActionState: MutableState<AsyncData<SessionId>>,
        serverUrl: String
    ) = launch {
        signUpActionState.value = AsyncData.Loading()

        // Register the user using Matrix API
        registrationService.register(
            serverUrl = serverUrl,
            username = formState.username.trim(),
            password = formState.password
        ).flatMap { registrationResult ->
            // After successful registration, login with the credentials
            authenticationService.login(formState.username.trim(), formState.password)
        }.onSuccess { sessionId ->
            signUpActionState.value = AsyncData.Success(sessionId)
        }.onFailure { failure ->
            signUpActionState.value = AsyncData.Failure(failure)
        }
    }

    private fun updateFormState(formState: MutableState<SignUpFormState>, updateLambda: SignUpFormState.() -> SignUpFormState) {
        formState.value = updateLambda(formState.value)
    }
}
