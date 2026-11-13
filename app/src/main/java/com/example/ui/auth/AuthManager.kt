package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

private const val TAG = "Auth"

object PhoneNumberFormatter {
    const val DEFAULT_COUNTRY_CODE = "+91"

    fun sanitizeCountryCode(rawCode: String): String {
        val digits = rawCode.filter { it.isDigit() }.take(4)
        return if (digits.isEmpty()) DEFAULT_COUNTRY_CODE else "+$digits"
    }

    fun sanitizeLocalNumber(rawNumber: String, countryCode: String = DEFAULT_COUNTRY_CODE): String {
        val digits = rawNumber.filter { it.isDigit() }
        val normalizedCode = sanitizeCountryCode(countryCode)
        return when {
            normalizedCode == "+91" && digits.length == 12 && digits.startsWith("91") -> {
                digits.drop(2)
            }
            normalizedCode == "+91" && digits.length == 11 && digits.startsWith("0") -> {
                digits.drop(1)
            }
            normalizedCode == "+91" -> digits.take(10)
            else -> digits.take(15)
        }
    }

    fun formatToE164(countryCode: String, localNumber: String): String {
        val cleanCode = sanitizeCountryCode(countryCode)
        val cleanLocal = sanitizeLocalNumber(localNumber, cleanCode)
        return "$cleanCode$cleanLocal"
    }

    fun isValidMobileNumber(countryCode: String, localNumber: String): Boolean {
        val cleanCode = sanitizeCountryCode(countryCode)
        val cleanLocal = sanitizeLocalNumber(localNumber, cleanCode)
        return if (cleanCode == "+91") {
            cleanLocal.length == 10
        } else {
            val totalDigits = cleanCode.drop(1).length + cleanLocal.length
            cleanLocal.length >= 6 && totalDigits in 8..15
        }
    }

    fun isValidOtpCode(otp: String): Boolean {
        val cleanOtp = otp.trim()
        return cleanOtp.length == 6 && cleanOtp.all { it.isDigit() }
    }

    fun maskE164(e164: String): String {
        val trimmed = e164.trim()
        if (trimmed.length <= 6) return trimmed
        val prefix = if (trimmed.startsWith("+91") && trimmed.length >= 13) {
            "+91 ${trimmed.substring(3, 5)}"
        } else {
            trimmed.take(4)
        }
        val suffix = trimmed.takeLast(4)
        return "$prefix••••$suffix"
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onUnauthenticated()
        return
    }
    val activity = context.findActivity() ?: run {
        onUnauthenticated()
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (e: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }
    val activity = context.findActivity() ?: run {
        onAuthError("Unable to locate foreground Activity for Google Sign-In")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type returned by Google Sign-In")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Sign in failed. Please try again.")
        }
    }
}

fun sendPhoneOtp(
    context: Context,
    phoneNumberE164: String,
    scope: CoroutineScope,
    forceResendingToken: PhoneAuthProvider.ForceResendingToken? = null,
    onAutoSignedIn: () -> Unit,
    onCodeSent: (verificationId: String, token: PhoneAuthProvider.ForceResendingToken) -> Unit,
    onAuthError: (String) -> Unit
) {
    val activity = context.findActivity() ?: run {
        onAuthError("Unable to locate foreground Activity for mobile verification.")
        return
    }

    val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            scope.launch {
                try {
                    Firebase.auth.signInWithCredential(credential).await()
                    onAutoSignedIn()
                } catch (e: Exception) {
                    Log.e(TAG, "Phone auto-verification sign-in failed", e)
                    onAuthError(mapPhoneAuthError(e))
                }
            }
        }

        override fun onVerificationFailed(e: FirebaseException) {
            Log.e(TAG, "Phone OTP verification failed", e)
            onAuthError(mapPhoneAuthError(e))
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            onCodeSent(verificationId, token)
        }
    }

    val optionsBuilder = PhoneAuthOptions.newBuilder(Firebase.auth)
        .setPhoneNumber(phoneNumberE164)
        .setTimeout(60L, TimeUnit.SECONDS)
        .setActivity(activity)
        .setCallbacks(callbacks)

    if (forceResendingToken != null) {
        optionsBuilder.setForceResendingToken(forceResendingToken)
    }

    PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
}

fun verifyPhoneOtp(
    verificationId: String,
    otpCode: String,
    scope: CoroutineScope,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit
) {
    scope.launch {
        try {
            val credential = PhoneAuthProvider.getCredential(verificationId, otpCode.trim())
            Firebase.auth.signInWithCredential(credential).await()
            onAuthSuccess()
        } catch (e: Exception) {
            Log.e(TAG, "OTP verification failed", e)
            onAuthError(mapPhoneAuthError(e))
        }
    }
}

private fun mapPhoneAuthError(exception: Exception): String {
    val authException = exception as? FirebaseAuthException
    return when (authException?.errorCode) {
        "ERROR_OPERATION_NOT_ALLOWED" ->
            "Mobile OTP login is not enabled on this Firebase project yet. Enable 'Phone' in Firebase Console -> Authentication -> Sign-in method, or sign in with Google below."
        "ERROR_INVALID_PHONE_NUMBER" ->
            "Please enter a valid 10-digit mobile number (e.g. +91 9876543210)."
        "ERROR_INVALID_VERIFICATION_CODE" ->
            "Incorrect 6-digit OTP. Please check the SMS code and try again."
        "ERROR_SESSION_EXPIRED" ->
            "This OTP has expired. Tap Resend OTP to receive a new code."
        "ERROR_TOO_MANY_REQUESTS", "ERROR_QUOTA_EXCEEDED" ->
            "Too many OTP attempts from this device. Please wait a while or sign in with Google below."
        "ERROR_APP_NOT_AUTHORIZED", "ERROR_CAPTCHA_CHECK_FAILED", "ERROR_MISSING_CLIENT_IDENTIFIER" ->
            "Phone OTP verification requires Play Integrity or SHA-1 verification in Firebase Console for this build. You can sign in with Google below."
        else -> exception.localizedMessage ?: "Mobile verification failed. Please try again."
    }
}

fun signOutUser(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clear credential state: ${e.message}")
            } finally {
                Firebase.auth.signOut()
                onSignOutComplete()
            }
        }
    }
}

@Composable
fun GoogleSignInButton(
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    modifier: Modifier = Modifier,
    isSecondary: Boolean = false
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }

    val onClickAction: () -> Unit = {
        isLoading = true
        onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isLoading = false
                onAuthSuccess()
            },
            onAuthError = { errorMsg ->
                isLoading = false
                onAuthError(errorMsg)
            },
            scope = coroutineScope,
            onAuthCancelled = {
                isLoading = false
            }
        )
    }

    if (isSecondary) {
        OutlinedButton(
            onClick = onClickAction,
            enabled = !isLoading,
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp)
                .testTag("google_sign_in_button")
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.5.dp
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.auth_sign_in_google),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    } else {
        Button(
            onClick = onClickAction,
            enabled = !isLoading,
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp)
                .testTag("google_sign_in_button")
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.5.dp
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.auth_sign_in_google),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
