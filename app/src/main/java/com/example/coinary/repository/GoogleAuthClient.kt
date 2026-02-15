package com.example.coinary.repository

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

/**
 * FirebaseUser: Internal domain model representing an authenticated user's core profile.
 */
data class FirebaseUser(
    val userId: String,
    val username: String?,
    val profilePictureUrl: String?,
    val email: String?
)

/**
 * GoogleAuthClient: Orchestrates the authentication lifecycle between Google Identity Services
 * and Firebase Authentication.
 * * @param context Application context required for initializing Google SDK components.
 */
class GoogleAuthClient(private val context: Context) {

    private val auth: FirebaseAuth = Firebase.auth

    /**
     * Lazy initialization of the Google Sign-In Client.
     * Configures the OIDC ID Token request needed for Firebase credential exchange.
     */
    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("920276614118-f3i355t2uhj8im2q6heor58q7a6hjhao.apps.googleusercontent.com")
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    /**
     * Generates the intent required to launch the Google Sign-In selector.
     * @return Intent to be used with ActivityResultLauncher.
     */
    fun getSignInIntent(): Intent = googleSignInClient.signInIntent

    /**
     * Validates the Google Sign-In intent result and exchanges the ID Token for a
     * Firebase Auth Credential.
     * * @param intent The data returned from the ActivityResult.
     * @return Result containing the [FirebaseUser] on success, or an Exception on failure.
     */
    suspend fun signInWithIntent(intent: Intent): Result<FirebaseUser> {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
            val account = task.getResult(ApiException::class.java)
                ?: throw Exception("GOOGLE_SIGNIN_ERROR: No account data retrieved.")

            val idToken = account.idToken
                ?: throw Exception("TOKEN_ERROR: Missing ID Token from Google.")

            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()

            val user = authResult.user
                ?: throw Exception("FIREBASE_AUTH_ERROR: User object is null after authentication.")

            Result.success(
                FirebaseUser(
                    userId = user.uid,
                    username = user.displayName,
                    profilePictureUrl = user.photoUrl?.toString(),
                    email = user.email
                )
            )
        } catch (e: Exception) {
            // Propagate exception to the caller for UI-level handling
            Result.failure(e)
        }
    }

    /**
     * Retrieves the current user session if it exists.
     * @return [FirebaseUser] or null if no session is active.
     */
    fun getSignedInUser(): FirebaseUser? = auth.currentUser?.let { user ->
        FirebaseUser(
            userId = user.uid,
            username = user.displayName,
            profilePictureUrl = user.photoUrl?.toString(),
            email = user.email
        )
    }

    /**
     * Invalidates the user session in both Google Identity Services and Firebase.
     * This ensures a full clean-up for the next login attempt.
     */
    suspend fun signOut() {
        try {
            // Sign out from Google SDK to allow account switching on next login
            googleSignInClient.signOut().await()
            // Sign out from Firebase instance
            auth.signOut()
        } catch (e: Exception) {
            // System-level sign-out failures are logged but do not disrupt user experience
            e.printStackTrace()
        }
    }
}