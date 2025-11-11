/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.usersearch.impl

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.MatrixPatterns
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.user.MatrixUser
import io.element.android.libraries.usersearch.api.UserListDataSource
import io.element.android.libraries.usersearch.api.UserRepository
import io.element.android.libraries.usersearch.api.UserSearchResult
import io.element.android.libraries.usersearch.api.UserSearchResultState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@ContributesBinding(SessionScope::class)
@Inject
class MatrixUserRepository(
    private val client: MatrixClient,
    private val dataSource: UserListDataSource
) : UserRepository {
    override fun search(query: String): Flow<UserSearchResultState> = flow {
        // Auto-append server domain if user only enters a username
        // This allows users to search for "john" instead of requiring "@john:qchat.eapp.click"
        val normalizedQuery = if (!query.startsWith("@") && !query.contains(":") && query.isNotEmpty()) {
            // User typed just a username, convert it to a full Matrix ID
            val homeserver = client.sessionId.value.substringAfter(":")
            "@$query:$homeserver"
        } else {
            query
        }

        val shouldQueryProfile = MatrixPatterns.isUserId(normalizedQuery) && !client.isMe(UserId(normalizedQuery))
        val shouldFetchSearchResults = query.length >= MINIMUM_SEARCH_LENGTH
        // If the search term is a MXID that's not ours, we'll show a 'fake' result for that user, then update it when we get search results.
        val fakeSearchResult = if (shouldQueryProfile) {
            // Create fake user with display name set to just the username (without server domain)
            // This ensures the loading state shows only "john" instead of "@john:qchat.eapp.click"
            val userId = UserId(normalizedQuery)
            UserSearchResult(MatrixUser(
                userId = userId,
                displayName = userId.extractedDisplayName  // Extract just "john" from "@john:qchat.eapp.click"
            ))
        } else {
            null
        }
        if (shouldQueryProfile || shouldFetchSearchResults) {
            emit(UserSearchResultState(isSearching = shouldFetchSearchResults, results = listOfNotNull(fakeSearchResult)))
        }
        if (shouldFetchSearchResults) {
            // Search using the original query for partial username matching
            // Then check if we need to explicitly query the normalized full Matrix ID profile
            val results = fetchSearchResults(query, normalizedQuery, shouldQueryProfile)
            emit(results)
        }
    }

    private suspend fun fetchSearchResults(
        searchQuery: String,
        normalizedUserId: String,
        shouldQueryProfile: Boolean
    ): UserSearchResultState {
        // Debounce
        delay(DEBOUNCE_TIME_MILLIS)
        // Search using the original query (e.g., "john") for partial matching
        val results = dataSource
            .search(searchQuery, MAXIMUM_SEARCH_RESULTS)
            .filter { !client.isMe(it.userId) }
            .map { UserSearchResult(it) }
            .toMutableList()

        // If the query is another user's MXID and the result doesn't contain that user ID, query the profile information explicitly
        if (shouldQueryProfile && results.none { it.matrixUser.userId.value == normalizedUserId }) {
            results.add(
                0,
                dataSource.getProfile(UserId(normalizedUserId))
                    ?.let { UserSearchResult(it) }
                    ?: UserSearchResult(
                        MatrixUser(
                            userId = UserId(normalizedUserId),
                            displayName = UserId(normalizedUserId).extractedDisplayName
                        ),
                        isUnresolved = true
                    )
            )
        }

        return UserSearchResultState(results = results, isSearching = false)
    }

    companion object {
        private const val DEBOUNCE_TIME_MILLIS = 250L
        private const val MINIMUM_SEARCH_LENGTH = 3
        private const val MAXIMUM_SEARCH_RESULTS = 10L
    }
}
