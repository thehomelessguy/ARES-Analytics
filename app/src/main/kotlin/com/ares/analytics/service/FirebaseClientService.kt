package com.ares.analytics.service

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
data class FirebaseSignInResponse(
    val idToken: String,
    val refreshToken: String,
    val expiresIn: String,
    val localId: String,
    val displayName: String? = null,
    val email: String? = null
)

sealed class FirebaseAuthState {
    object Unauthenticated : FirebaseAuthState()
    object Authenticating : FirebaseAuthState()
    data class Authenticated(
        val firebaseToken: String,
        val uid: String,
        val email: String,
        val displayName: String,
        val githubToken: String? = null
    ) : FirebaseAuthState()
    data class Error(val message: String) : FirebaseAuthState()
}

class FirebaseClientService {

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private val _authState = MutableStateFlow<FirebaseAuthState>(FirebaseAuthState.Unauthenticated)
    val authState: StateFlow<FirebaseAuthState> = _authState.asStateFlow()

    // Configuration values (loaded from workspace config or env)
    var apiKey: String = System.getenv("FIREBASE_API_KEY") ?: "AIzaSyCkTBJqV7CAFRsxm4047oGXwbm_QP-BT7I"
    var projectId: String = System.getenv("FIREBASE_PROJECT_ID") ?: "aresfirst-portal"

    fun isDevMode(): Boolean {
        return apiKey.isEmpty() || apiKey == "mock" || System.getenv("DEV_MODE") == "true"
    }

    suspend fun signInWithGoogleToken(googleIdToken: String, email: String, name: String) {
        _authState.value = FirebaseAuthState.Authenticating
        if (isDevMode()) {
            // Local dev fallback
            val mockToken = "mock-token:$email:$email:$name"
            _authState.value = FirebaseAuthState.Authenticated(
                firebaseToken = mockToken,
                uid = "mock-$email",
                email = email,
                displayName = name
            )
            return
        }

        try {
            val url = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithIdp?key=$apiKey"
            val postBody = "id_token=$googleIdToken&providerId=google.com"
            
            val response = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                header("Referer", "https://${projectId}.firebaseapp.com")
                header("Origin", "https://${projectId}.firebaseapp.com")
                setBody(buildJsonObject {
                    put("requestUri", "http://localhost")
                    put("postBody", postBody)
                    put("returnSecureToken", true)
                    put("returnIdToken", true)
                })
            }

            if (response.status == HttpStatusCode.OK) {
                val data = response.body<FirebaseSignInResponse>()
                _authState.value = FirebaseAuthState.Authenticated(
                    firebaseToken = data.idToken,
                    uid = data.localId,
                    email = data.email ?: email,
                    displayName = data.displayName ?: name
                )
            } else {
                val body = response.bodyAsText()
                _authState.value = FirebaseAuthState.Error("Firebase Sign-In failed (${response.status}): $body")
            }
        } catch (e: Exception) {
            _authState.value = FirebaseAuthState.Error("Network error during Firebase Sign-In: ${e.message}")
        }
    }

    fun linkGitHubToken(githubToken: String) {
        val current = _authState.value
        if (current is FirebaseAuthState.Authenticated) {
            _authState.value = current.copy(githubToken = githubToken)
        }
    }

    fun logout() {
        _authState.value = FirebaseAuthState.Unauthenticated
    }

    fun getFirebaseToken(): String? {
        val current = _authState.value
        return if (current is FirebaseAuthState.Authenticated) {
            current.firebaseToken
        } else null
    }

    fun close() {
        try {
            httpClient.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
