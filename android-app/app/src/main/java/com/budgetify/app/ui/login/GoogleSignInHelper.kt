package com.budgetify.app.ui.login

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.budgetify.app.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

sealed class GoogleSignInOutcome {
    data class Token(val idToken: String) : GoogleSignInOutcome()
    object Cancelled : GoogleSignInOutcome()
    object NotConfigured : GoogleSignInOutcome()
    object Failed : GoogleSignInOutcome()
}

/**
 * Single sign-on: shows the system "Sign in with Google" sheet (Credential Manager) and
 * returns a Google ID token. The token is then exchanged for a Budgetify JWT by the API
 * (POST /api/auth/google) and used to sign in to Firebase Authentication.
 */
suspend fun requestGoogleIdToken(context: Context): GoogleSignInOutcome {
    val webClientId = context.getString(R.string.google_web_client_id)
    if (webClientId.isBlank() || webClientId.startsWith("REPLACE")) {
        return GoogleSignInOutcome.NotConfigured
    }
    return try {
        val option = GetSignInWithGoogleOption.Builder(webClientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val result = CredentialManager.create(context).getCredential(context = context, request = request)
        val credential = result.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            GoogleSignInOutcome.Token(GoogleIdTokenCredential.createFrom(credential.data).idToken)
        } else {
            Log.w("GoogleSignIn", "Unexpected credential type")
            GoogleSignInOutcome.Failed
        }
    } catch (e: GetCredentialCancellationException) {
        GoogleSignInOutcome.Cancelled
    } catch (e: GetCredentialException) {
        Log.w("GoogleSignIn", "Credential Manager error: ${e.message}")
        GoogleSignInOutcome.Failed
    } catch (e: GoogleIdTokenParsingException) {
        Log.w("GoogleSignIn", "Could not parse Google ID token: ${e.message}")
        GoogleSignInOutcome.Failed
    }
}
