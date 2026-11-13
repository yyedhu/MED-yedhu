package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.ui.auth.GoogleSignInButton
import com.example.ui.auth.PhoneNumberFormatter
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.auth.sendPhoneOtp
import com.example.ui.auth.verifyPhoneOtp
import com.example.ui.components.StatusPill
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.delay

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var authError by remember { mutableStateOf<String?>(null) }
    var countryCode by remember { mutableStateOf(PhoneNumberFormatter.DEFAULT_COUNTRY_CODE) }
    var phoneInput by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var resendToken by remember { mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null) }
    var sentE164Phone by remember { mutableStateOf("") }
    var isSendingOtp by remember { mutableStateOf(false) }
    var isVerifyingOtp by remember { mutableStateOf(false) }
    var resendCooldownSeconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope
        )
    }

    LaunchedEffect(resendCooldownSeconds) {
        if (resendCooldownSeconds > 0) {
            delay(1000L)
            resendCooldownSeconds -= 1
        }
    }

    val isPhoneValid = remember(countryCode, phoneInput) {
        PhoneNumberFormatter.isValidMobileNumber(countryCode, phoneInput)
    }
    val isOtpValid = remember(otpInput) {
        PhoneNumberFormatter.isValidOtpCode(otpInput)
    }

    fun triggerSendOtp(useResendToken: Boolean) {
        if (!isPhoneValid || isSendingOtp) return
        val formattedE164 = PhoneNumberFormatter.formatToE164(countryCode, phoneInput)
        authError = null
        isSendingOtp = true
        sendPhoneOtp(
            context = context,
            phoneNumberE164 = formattedE164,
            scope = scope,
            forceResendingToken = if (useResendToken) resendToken else null,
            onAutoSignedIn = {
                isSendingOtp = false
                onAuthSuccess()
            },
            onCodeSent = { newVerificationId, newToken ->
                isSendingOtp = false
                verificationId = newVerificationId
                resendToken = newToken
                sentE164Phone = formattedE164
                otpInput = ""
                resendCooldownSeconds = 30
            },
            onAuthError = { errorMsg ->
                isSendingOtp = false
                authError = errorMsg
            }
        )
    }

    fun triggerVerifyOtp() {
        val activeVerificationId = verificationId ?: return
        if (!isOtpValid || isVerifyingOtp) return
        authError = null
        isVerifyingOtp = true
        verifyPhoneOtp(
            verificationId = activeVerificationId,
            otpCode = otpInput,
            scope = scope,
            onAuthSuccess = {
                isVerifyingOtp = false
                onAuthSuccess()
            },
            onAuthError = { errorMsg ->
                isVerifyingOtp = false
                authError = errorMsg
            }
        )
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.LocalPharmacy,
                            contentDescription = "MediRelay emblem",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.weight(1f))
                StatusPill(
                    text = "KOCHI ONLY • RX",
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }

            Surface(
                shape = MaterialTheme.shapes.large,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_pharmacy),
                    contentDescription = "Neighborhood medical store courier illustration",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(MaterialTheme.shapes.large)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.auth_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.auth_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Primary Mobile Number OTP Login Card
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.large,
                tonalElevation = 2.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.PhoneAndroid,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Text(
                                text = stringResource(R.string.auth_phone_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        StatusPill(
                            text = "OTP LOGIN",
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    }

                    AnimatedContent(
                        targetState = verificationId != null,
                        label = "phone_auth_step"
                    ) { isOtpStep ->
                        if (!isOtpStep) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = countryCode,
                                        onValueChange = { input ->
                                            val digits = input.filter { it.isDigit() }.take(4)
                                            countryCode = if (digits.isEmpty()) "+" else "+$digits"
                                        },
                                        label = { Text("Code") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Phone,
                                            imeAction = ImeAction.Next
                                        ),
                                        modifier = Modifier
                                            .width(84.dp)
                                            .testTag("country_code_input")
                                    )
                                    OutlinedTextField(
                                        value = phoneInput,
                                        onValueChange = { raw ->
                                            phoneInput = PhoneNumberFormatter.sanitizeLocalNumber(
                                                rawNumber = raw,
                                                countryCode = countryCode
                                            )
                                            if (authError != null) authError = null
                                        },
                                        label = { Text("Mobile Number") },
                                        placeholder = { Text("98765 43210") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Phone,
                                            imeAction = ImeAction.Done
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onDone = { triggerSendOtp(useResendToken = false) }
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("phone_number_input")
                                    )
                                }

                                Text(
                                    text = stringResource(R.string.auth_phone_helper),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Button(
                                    onClick = { triggerSendOtp(useResendToken = false) },
                                    enabled = isPhoneValid && !isSendingOtp,
                                    shape = MaterialTheme.shapes.medium,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 54.dp)
                                        .testTag("send_otp_button")
                                ) {
                                    if (isSendingOtp) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(22.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.5.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Filled.Sms,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(R.string.auth_send_otp),
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "OTP sent to ${PhoneNumberFormatter.maskE164(sentE164Phone)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    TextButton(
                                        onClick = {
                                            verificationId = null
                                            otpInput = ""
                                            authError = null
                                        },
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("change_phone_button")
                                    ) {
                                        Text(stringResource(R.string.auth_change_number))
                                    }
                                }

                                OutlinedTextField(
                                    value = otpInput,
                                    onValueChange = { raw ->
                                        val digits = raw.filter { it.isDigit() }.take(6)
                                        otpInput = digits
                                        if (authError != null) authError = null
                                    },
                                    label = { Text("Enter 6-digit OTP") },
                                    placeholder = { Text("••••••") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.Lock,
                                            contentDescription = null
                                        )
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = { triggerVerifyOtp() }
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("otp_code_input")
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (resendCooldownSeconds > 0) {
                                            "Resend available in ${resendCooldownSeconds}s"
                                        } else {
                                            "Didn't receive SMS?"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    TextButton(
                                        onClick = { triggerSendOtp(useResendToken = true) },
                                        enabled = resendCooldownSeconds == 0 && !isSendingOtp && !isVerifyingOtp,
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("resend_otp_button")
                                    ) {
                                        Text(stringResource(R.string.auth_resend_otp))
                                    }
                                }

                                Button(
                                    onClick = { triggerVerifyOtp() },
                                    enabled = isOtpValid && !isVerifyingOtp,
                                    shape = MaterialTheme.shapes.medium,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 54.dp)
                                        .testTag("verify_otp_button")
                                ) {
                                    if (isVerifyingOtp) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(22.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.5.dp
                                        )
                                    } else {
                                        Text(
                                            text = stringResource(R.string.auth_verify_otp),
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (authError != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = authError ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(R.string.auth_or_continue_with),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f)
                )
            }

            GoogleSignInButton(
                isSecondary = true,
                onAuthSuccess = {
                    authError = null
                    onAuthSuccess()
                },
                onAuthError = { message ->
                    authError = message
                }
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FeatureHighlightRow(
                    icon = Icons.Filled.LocalPharmacy,
                    title = "Every Kochi medical store in one view",
                    subtitle = "Compare live prices, stock, and 24x7 availability across Kochi pharmacies."
                )
                FeatureHighlightRow(
                    icon = Icons.Filled.Medication,
                    title = "Zero-permission prescription attachment",
                    subtitle = "Safely share Rx photos with licensed pharmacists via Android Photo Picker."
                )
                FeatureHighlightRow(
                    icon = Icons.Filled.VerifiedUser,
                    title = "Synced orders & live GPS rider tracking",
                    subtitle = "Track packed pharmacy confirmation and doorstep rider arrival in real time."
                )
            }
        }
    }
}

@Composable
private fun FeatureHighlightRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
