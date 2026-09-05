package com.icit.expense.ui

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.icit.expense.BuildConfig
import java.util.concurrent.TimeUnit

private const val TAG = "LoginScreen"

sealed class LoginState {
    data object Initial : LoginState()
    data object PhoneEntry : LoginState()
    data class OtpEntry(val verificationId: String) : LoginState()
    data object Loading : LoginState()
}

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    var loginState by remember { mutableStateOf<LoginState>(LoginState.Initial) }
    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d(TAG, "Google Sign-In result code: ${result.resultCode}")
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                Log.d(TAG, "Google account retrieved: ${account?.email}")
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                auth.signInWithCredential(credential)
                    .addOnCompleteListener { authTask ->
                        if (authTask.isSuccessful) {
                            Log.d(TAG, "Firebase Google Sign-In Successful")
                            onLoginSuccess()
                        } else {
                            Log.e(TAG, "Firebase Google Sign-In Failed", authTask.exception)
                            Toast.makeText(context, "Google Sign-In Failed: ${authTask.exception?.message}", Toast.LENGTH_SHORT).show()
                            loginState = LoginState.Initial
                        }
                    }
            } catch (e: ApiException) {
                Log.e(TAG, "Google Sign-In ApiException: code=${e.statusCode}", e)
                Toast.makeText(context, "Google Sign-In Error: ${e.message}", Toast.LENGTH_SHORT).show()
                loginState = LoginState.Initial
            }
        } else {
            Log.w(TAG, "Google Sign-In cancelled or failed")
            loginState = LoginState.Initial
        }
    }

    fun startGoogleLogin() {
        loginState = LoginState.Loading
        val webClientId = BuildConfig.WEB_CLIENT_ID

        // Empty means secrets.properties is missing or has no WEB_CLIENT_ID; the "1:" prefix means
        // the Android app ID was pasted in place of the web client ID. Both fail at the same point,
        // so catch them here with a message that says what to fix.
        if (webClientId.isBlank() || webClientId.startsWith("1:")) {
            Log.e(
                TAG,
                "Invalid Web Client ID. Set WEB_CLIENT_ID in secrets.properties to the OAuth " +
                    "web client ID, which looks like '123456789-xyz.apps.googleusercontent.com'"
            )
            Toast.makeText(context, "Configuration Error: Invalid Web Client ID", Toast.LENGTH_LONG).show()
            loginState = LoginState.Initial
            return
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(context, gso)
        googleSignInLauncher.launch(googleSignInClient.signInIntent)
    }

    fun sendOtp() {
        if (phoneNumber.length < 10) {
            Toast.makeText(context, "Please enter a valid phone number", Toast.LENGTH_SHORT).show()
            return
        }
        loginState = LoginState.Loading
        val fullPhoneNumber = if (phoneNumber.startsWith("+")) phoneNumber else "+91$phoneNumber"
        Log.d(TAG, "Sending OTP to: $fullPhoneNumber")
        
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(fullPhoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(context as Activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    Log.d(TAG, "Phone verification completed automatically")
                    auth.signInWithCredential(credential)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Log.d(TAG, "Firebase Phone Sign-In Successful (Auto)")
                                onLoginSuccess()
                            } else {
                                Log.e(TAG, "Firebase Phone Sign-In Failed (Auto)", task.exception)
                            }
                        }
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Log.e(TAG, "Phone verification failed: ${e.message}", e)
                    Toast.makeText(context, "Verification Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    loginState = LoginState.PhoneEntry
                }

                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    Log.d(TAG, "OTP Code sent, verificationId: $verificationId")
                    loginState = LoginState.OtpEntry(verificationId)
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyOtp(verificationId: String) {
        if (otpCode.length < 6) {
            Toast.makeText(context, "Please enter a valid 6-digit OTP", Toast.LENGTH_SHORT).show()
            return
        }
        loginState = LoginState.Loading
        Log.d(TAG, "Verifying OTP: $otpCode for verificationId: $verificationId")
        
        val credential = PhoneAuthProvider.getCredential(verificationId, otpCode)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Firebase OTP Verification Successful")
                    onLoginSuccess()
                } else {
                    Log.e(TAG, "Firebase OTP Verification Failed", task.exception)
                    Toast.makeText(context, "Invalid OTP: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    loginState = LoginState.OtpEntry(verificationId)
                }
            }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (loginState != LoginState.Initial && loginState != LoginState.Loading) {
                    IconButton(onClick = { loginState = LoginState.Initial }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                } else {
                    Spacer(Modifier.size(48.dp))
                }
                
                if (loginState == LoginState.Initial) {
                    TextButton(onClick = onSkip) {
                        Text("SKIP", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (val state = loginState) {
                LoginState.Initial -> {
                    AppBranding()
                    Spacer(modifier = Modifier.height(48.dp))
                    GoogleLoginButton(onClick = { startGoogleLogin() })
                    // Commented out Phone Login as per request
                    // Spacer(modifier = Modifier.height(16.dp))
                    // PhoneLoginButton(onClick = { loginState = LoginState.PhoneEntry })
                }
                LoginState.PhoneEntry -> {
                    Text("Enter Phone Number", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("We'll send you an OTP for verification", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(32.dp))
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { input -> 
                            if (input.all { char -> char.isDigit() }) phoneNumber = input 
                        },
                        label = { Text("Phone Number") },
                        prefix = { Text("+91 ") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { sendOtp() },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Send OTP", fontWeight = FontWeight.Bold)
                    }
                }
                is LoginState.OtpEntry -> {
                    Text("Verify OTP", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Enter the code sent to +91 $phoneNumber", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(32.dp))
                    OutlinedTextField(
                        value = otpCode,
                        onValueChange = { input -> 
                            if (input.length <= 6 && input.all { it.isDigit() }) otpCode = input 
                        },
                        label = { Text("6-Digit OTP") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = TextStyle(textAlign = TextAlign.Center),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { verifyOtp(state.verificationId) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Verify & Continue", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { sendOtp() }) {
                        Text("Resend OTP")
                    }
                }
                LoginState.Loading -> {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun AppBranding() {
    Surface(
        modifier = Modifier.size(100.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text("E", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Black)
        }
    }
    Spacer(modifier = Modifier.height(24.dp))
    Text("Expense Tracker", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    Text("Simple, Clean, Powerful", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun GoogleLoginButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
    ) {
        Icon(Icons.Rounded.Email, contentDescription = null)
        Spacer(modifier = Modifier.width(12.dp))
        Text("Continue with Google", fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun PhoneLoginButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Icon(Icons.Rounded.Phone, contentDescription = null)
        Spacer(modifier = Modifier.width(12.dp))
        Text("Continue with Phone", fontWeight = FontWeight.SemiBold)
    }
}
