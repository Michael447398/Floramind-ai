package com.example.data.repository

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.example.core.ImageUtils
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

sealed interface GoogleSignInResult {
    data class Success(val user: FirebaseUser) : GoogleSignInResult
    data object Cancelled : GoogleSignInResult
    data class Error(val message: String) : GoogleSignInResult
}

class AuthRepository(
    private val auth: FirebaseAuth = Firebase.auth
) {
    fun currentUser(): FirebaseUser? = auth.currentUser

    fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun attemptSilentSignIn(context: Context): FirebaseUser? {
        auth.currentUser?.let { return it }
        val clientId = runCatching { context.getString(R.string.default_web_client_id) }.getOrNull()
            ?: return null

        return try {
            val credentialManager = CredentialManager.create(context)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(true)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val firebaseCred = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(firebaseCred).await().user
            } else {
                null
            }
        } catch (e: Exception) {
            Log.d(TAG, "Silent auto sign-in skipped: ${e.message}")
            null
        }
    }

    suspend fun signInWithGoogleInteractive(activityContext: Context): GoogleSignInResult {
        val clientId = try {
            activityContext.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            return GoogleSignInResult.Error("Google Sign-In configuration missing: default_web_client_id not found.")
        }

        val activity = activityContext as? Activity
            ?: return GoogleSignInResult.Error("Sign-In requires an active screen context.")

        return try {
            val credentialManager = CredentialManager.create(activity)
            val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInOption)
                .build()

            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(authCredential).await()
                val user = authResult.user
                    ?: return GoogleSignInResult.Error("Sign-in succeeded but user profile is unavailable.")
                GoogleSignInResult.Success(user)
            } else {
                GoogleSignInResult.Error("Unexpected credential type returned.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            GoogleSignInResult.Cancelled
        } catch (e: NoCredentialException) {
            Log.w(TAG, "No Google account available on device: ${e.message}", e)
            GoogleSignInResult.Error(
                "No Google account is signed in on this device. Add a Google account in Android Settings > Accounts, then tap Continue with Google."
            )
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Credential Manager error during Google Sign-In", e)
            GoogleSignInResult.Error(
                e.errorMessage?.toString()?.takeIf { it.isNotBlank() }
                    ?: "Unable to complete Google account selection. Ensure Google Play Services is available and try again."
            )
        } catch (e: FirebaseAuthUserCollisionException) {
            Log.w(TAG, "Existing account collision during Google Sign-In", e)
            GoogleSignInResult.Error(
                "An account already exists for this email address with a different sign-in provider."
            )
        } catch (e: FirebaseNetworkException) {
            Log.w(TAG, "Network failure during Google Sign-In", e)
            GoogleSignInResult.Error(
                "Network error while signing in. Please check your internet connection and try again."
            )
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed", e)
            GoogleSignInResult.Error(e.localizedMessage ?: "Google Sign-In failed.")
        }
    }

    suspend fun signOut(context: Context) {
        withContext(NonCancellable) {
            try {
                CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clear credential state: ${e.message}")
            } finally {
                auth.signOut()
            }
        }
    }

    suspend fun deleteAccountAndData(
        context: Context,
        floraRepository: FloraRepository
    ): Result<Unit> = runCatching {
        val user = auth.currentUser
            ?: throw IllegalStateException("You must be signed in to delete your account.")
        val uid = user.uid
        floraRepository.deleteAllUserData(uid).getOrThrow()
        ImageUtils.clearAllLocalScanImages(context)
        try {
            user.delete().await()
        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            throw IllegalStateException("For your security, please sign out and sign back in with Google before permanently deleting your Firebase account.", e)
        }
        signOut(context)
    }

    companion object {
        private const val TAG = "AuthRepository"
    }
}
