/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.signup

import dev.zacsweers.metro.Inject
import io.element.android.libraries.core.extensions.flatMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Service for registering new users via the Matrix Client-Server API.
 *
 * This implements the registration flow as defined in the Matrix specification:
 * https://spec.matrix.org/v1.9/client-server-api/#post_matrixclientv3register
 *
 * Server Requirements:
 * - Must have `enable_registration: true` in homeserver.yaml
 * - For "m.login.dummy" auth: Must allow registration without verification
 * - Or configure other verification methods (captcha, email, etc.)
 */
@Inject
class RegistrationService(
    private val okHttpClient: OkHttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Registers a new user account on the Matrix homeserver.
     *
     * @param serverUrl The homeserver URL (e.g., "https://qchat.eapp.click")
     * @param username The desired username (localpart only, without @domain)
     * @param password The user's password
     * @return Result containing registration details or error
     */
    suspend fun register(
        serverUrl: String,
        username: String,
        password: String
    ): Result<RegistrationResult> = withContext(Dispatchers.IO) {
        runCatching {
            // Matrix registration endpoint (v3 API as per spec)
            // Endpoint: POST /_matrix/client/v3/register
            val registerUrl = "$serverUrl/_matrix/client/v3/register"

            val registerRequest = RegistrationRequest(
                username = username,  // localpart only (e.g., "john" not "@john:server.com")
                password = password,
                auth = AuthData(type = "m.login.dummy"),  // No verification required
                inhibitLogin = false  // Get access token immediately
            )

            val requestBody = json.encodeToString(
                RegistrationRequest.serializer(),
                registerRequest
            ).toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(registerUrl)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: throw Exception("Empty response")
                val result = json.decodeFromString<RegistrationResponse>(responseBody)
                RegistrationResult(
                    userId = result.userId,
                    accessToken = result.accessToken,
                    deviceId = result.deviceId
                )
            } else {
                val errorBody = response.body?.string() ?: "Unknown error"
                throw Exception("Registration failed: ${response.code} - $errorBody")
            }
        }
    }
}

@Serializable
private data class RegistrationRequest(
    @SerialName("username") val username: String,
    @SerialName("password") val password: String,
    @SerialName("auth") val auth: AuthData,
    @SerialName("inhibit_login") val inhibitLogin: Boolean = false
)

@Serializable
private data class AuthData(
    @SerialName("type") val type: String
)

@Serializable
private data class RegistrationResponse(
    @SerialName("user_id") val userId: String,
    @SerialName("access_token") val accessToken: String,
    @SerialName("device_id") val deviceId: String
)

data class RegistrationResult(
    val userId: String,
    val accessToken: String,
    val deviceId: String
)
