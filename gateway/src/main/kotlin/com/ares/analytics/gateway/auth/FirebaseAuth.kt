package com.ares.analytics.gateway.auth

import com.google.firebase.auth.FirebaseAuth
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*

data class FirebasePrincipal(val uid: String, val email: String?, val name: String?) : Principal

class FirebaseAuthenticationProvider(config: Config) : AuthenticationProvider(config) {
    override suspend fun onAuthenticate(context: AuthenticationContext) {
        val authHeader = context.call.request.headers[HttpHeaders.Authorization]
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            context.challenge("Firebase", AuthenticationFailedCause.NoCredentials) { challenge, call ->
                call.respond(HttpStatusCode.Unauthorized, "Missing Authorization Header with Bearer token")
                challenge.complete()
            }
            return
        }

        val token = authHeader.substring(7)
        
        // Development / Offline bypass
        val isDev = System.getenv("DEV_MODE") == "true" || System.getenv("MOCK_AUTH") == "true"
        if (isDev && token.startsWith("mock-token")) {
            val parts = token.split(":")
            val uid = parts.getOrNull(1) ?: "mock-uid"
            val email = parts.getOrNull(2) ?: "mock-user@example.com"
            val name = parts.getOrNull(3) ?: "Mock User"
            val principal = FirebasePrincipal(uid = uid, email = email, name = name)
            context.principal(principal)
            return
        }

        try {
            // Verify ID Token via Firebase Admin SDK
            val decodedToken = FirebaseAuth.getInstance().verifyIdToken(token)
            val principal = FirebasePrincipal(
                uid = decodedToken.uid,
                email = decodedToken.email,
                name = decodedToken.name
            )
            context.principal(principal)
        } catch (e: Exception) {
            context.challenge("Firebase", AuthenticationFailedCause.InvalidCredentials) { challenge, call ->
                call.respond(HttpStatusCode.Unauthorized, "Invalid Firebase token: ${e.message}")
                challenge.complete()
            }
        }
    }

    class Config(name: String?) : AuthenticationProvider.Config(name)
}

fun AuthenticationConfig.firebase(
    name: String? = "firebase",
    configure: FirebaseAuthenticationProvider.Config.() -> Unit = {}
) {
    val provider = FirebaseAuthenticationProvider(FirebaseAuthenticationProvider.Config(name).apply(configure))
    register(provider)
}

