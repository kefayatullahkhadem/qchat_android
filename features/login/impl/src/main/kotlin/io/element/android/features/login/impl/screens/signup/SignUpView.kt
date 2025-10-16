/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.signup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.login.impl.R
import io.element.android.features.login.impl.error.loginError
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.designsystem.atomic.molecules.ButtonColumnMolecule
import io.element.android.libraries.designsystem.atomic.molecules.IconTitleSubtitleMolecule
import io.element.android.libraries.designsystem.components.BigIcon
import io.element.android.libraries.designsystem.components.button.BackButton
import io.element.android.libraries.designsystem.components.dialogs.ErrorDialog
import io.element.android.libraries.designsystem.components.form.textFieldState
import io.element.android.libraries.designsystem.modifiers.onTabOrEnterKeyFocusNext
import io.element.android.libraries.designsystem.theme.components.Button
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextField
import io.element.android.libraries.designsystem.theme.components.TopAppBar
import io.element.android.libraries.testtags.TestTags
import io.element.android.libraries.testtags.testTag
import io.element.android.libraries.ui.strings.CommonStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpView(
    state: SignUpState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLoading by remember(state.signUpAction) {
        derivedStateOf {
            state.signUpAction is AsyncData.Loading
        }
    }
    val focusManager = LocalFocusManager.current

    fun submit() {
        focusManager.clearFocus(force = true)
        state.eventSink(SignUpEvents.Submit)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    BackButton(onClick = onBackClick)
                },
            )
        }
    ) { padding ->
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(padding)
                .consumeWindowInsets(padding)
                .verticalScroll(state = scrollState)
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
        ) {
            // Title
            IconTitleSubtitleMolecule(
                modifier = Modifier.padding(top = 20.dp, start = 16.dp, end = 16.dp),
                iconStyle = BigIcon.Style.Default(CompoundIcons.UserProfileSolid()),
                title = stringResource(R.string.screen_create_account_title),
                subTitle = stringResource(id = R.string.screen_login_subtitle)
            )
            Spacer(Modifier.height(40.dp))
            SignUpForm(
                state = state,
                isLoading = isLoading,
                onSubmit = ::submit
            )
            // Min spacing
            Spacer(Modifier.height(24.dp))
            // Flexible spacing to keep the submit button at the bottom
            Spacer(modifier = Modifier.weight(1f))
            // Submit
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
            ) {
                ButtonColumnMolecule {
                    Button(
                        text = stringResource(R.string.screen_create_account_title),
                        showProgress = isLoading,
                        onClick = ::submit,
                        enabled = state.submitEnabled || isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(TestTags.loginContinue)
                    )
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }

            if (state.signUpAction is AsyncData.Failure) {
                SignUpErrorDialog(error = state.signUpAction.error, onDismiss = {
                    state.eventSink(SignUpEvents.ClearError)
                })
            }
        }
    }
}

@Composable
private fun SignUpForm(
    state: SignUpState,
    isLoading: Boolean,
    onSubmit: () -> Unit,
) {
    var usernameFieldState by textFieldState(stateValue = state.formState.username)
    var passwordFieldState by textFieldState(stateValue = state.formState.password)
    var confirmPasswordFieldState by textFieldState(stateValue = state.formState.confirmPassword)

    val focusManager = LocalFocusManager.current
    val eventSink = state.eventSink

    Column {
        TextField(
            label = stringResource(CommonStrings.common_username),
            value = usernameFieldState,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .onTabOrEnterKeyFocusNext(focusManager)
                .testTag(TestTags.loginEmailUsername)
                .semantics {
                    contentType = ContentType.Username
                },
            placeholder = stringResource(CommonStrings.common_username),
            onValueChange = {
                val sanitized = it.sanitize()
                usernameFieldState = sanitized
                eventSink(SignUpEvents.SetUsername(sanitized))
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onNext = {
                focusManager.moveFocus(FocusDirection.Down)
            }),
            singleLine = true,
            trailingIcon = if (usernameFieldState.isNotEmpty()) {
                {
                    Box(Modifier.clickable {
                        usernameFieldState = ""
                        eventSink(SignUpEvents.SetUsername(""))
                    }) {
                        Icon(
                            imageVector = CompoundIcons.Close(),
                            contentDescription = stringResource(CommonStrings.action_clear),
                            tint = ElementTheme.colors.iconSecondary
                        )
                    }
                }
            } else {
                null
            },
        )

        var passwordVisible by remember { mutableStateOf(false) }
        if (state.signUpAction is AsyncData.Loading) {
            passwordVisible = false
        }

        Spacer(Modifier.height(20.dp))
        TextField(
            value = passwordFieldState,
            enabled = !isLoading,
            label = stringResource(CommonStrings.common_password),
            modifier = Modifier
                .fillMaxWidth()
                .onTabOrEnterKeyFocusNext(focusManager)
                .testTag(TestTags.loginPassword)
                .semantics {
                    contentType = ContentType.Password
                },
            onValueChange = {
                val sanitized = it.sanitize()
                passwordFieldState = sanitized
                eventSink(SignUpEvents.SetPassword(sanitized))
            },
            placeholder = stringResource(CommonStrings.common_password),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val image =
                    if (passwordVisible) CompoundIcons.VisibilityOn() else CompoundIcons.VisibilityOff()
                val description =
                    if (passwordVisible) stringResource(CommonStrings.a11y_hide_password) else stringResource(CommonStrings.a11y_show_password)
                Box(Modifier.clickable { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = image,
                        contentDescription = description,
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            singleLine = true,
        )

        Spacer(Modifier.height(20.dp))
        TextField(
            value = confirmPasswordFieldState,
            enabled = !isLoading,
            label = stringResource(CommonStrings.action_confirm_password),
            modifier = Modifier
                .fillMaxWidth()
                .onTabOrEnterKeyFocusNext(focusManager)
                .semantics {
                    contentType = ContentType.Password
                },
            onValueChange = {
                val sanitized = it.sanitize()
                confirmPasswordFieldState = sanitized
                eventSink(SignUpEvents.SetConfirmPassword(sanitized))
            },
            placeholder = stringResource(CommonStrings.action_confirm_password),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val image =
                    if (passwordVisible) CompoundIcons.VisibilityOn() else CompoundIcons.VisibilityOff()
                val description =
                    if (passwordVisible) stringResource(CommonStrings.a11y_hide_password) else stringResource(CommonStrings.a11y_show_password)
                Box(Modifier.clickable { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = image,
                        contentDescription = description,
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = { onSubmit() }
            ),
            singleLine = true,
        )

        // Password validation hint
        if (state.formState.password.isNotEmpty() && state.formState.password.length < 8) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Password must be at least 8 characters",
                style = ElementTheme.typography.fontBodySmRegular,
                color = ElementTheme.colors.textCriticalPrimary,
                modifier = Modifier.padding(start = 16.dp)
            )
        }

        // Password match validation
        if (state.formState.confirmPassword.isNotEmpty() && state.formState.password != state.formState.confirmPassword) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Passwords do not match",
                style = ElementTheme.typography.fontBodySmRegular,
                color = ElementTheme.colors.textCriticalPrimary,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}

private fun String.sanitize(): String {
    return replace("\n", "")
}

@Composable
private fun SignUpErrorDialog(error: Throwable, onDismiss: () -> Unit) {
    ErrorDialog(
        title = stringResource(id = CommonStrings.dialog_title_error),
        content = error.message ?: stringResource(loginError(error)),
        onSubmit = onDismiss
    )
}
