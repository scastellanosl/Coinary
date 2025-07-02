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

data class FirebaseUser(
    val userId: String,
    val username: String?,
    val profilePictureUrl: String?,
    val email: String? // <-- agrega este campo
)

class GoogleAuthClient(private val context: Context) {
    private val auth: FirebaseAuth = Firebase.auth

    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("920276614118-f3i355t2uhj8im2q6heor58q7a6hjhao.apps.googleusercontent.com")
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }


    fun getSignInIntent(): Intent = googleSignInClient.signInIntent

    suspend fun signInWithIntent(intent: Intent): Result<FirebaseUser> {
        return try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(intent).getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken!!, null)
            val authResult = auth.signInWithCredential(credential).await()
            Result.success(
                FirebaseUser(
                    userId = authResult.user?.uid!!,
                    username = authResult.user?.displayName,
                    profilePictureUrl = authResult.user?.photoUrl?.toString(),
                    email = authResult.user?.email // <-- agrega esto
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getSignedInUser(): FirebaseUser? = auth.currentUser?.let { user ->
        FirebaseUser(
            userId = user.uid,
            username = user.displayName,
            profilePictureUrl = user.photoUrl?.toString(),
            email = user.email // <-- agrega esto
        )
    }

    suspend fun signOut() {
        try {
            googleSignInClient.signOut().await()
            auth.signOut()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
