package com.family.talkly.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.family.talkly.ui.theme.WhatsappGreen
import com.family.talkly.ui.theme.WhatsappTeal

data class CountryCode(val country: String, val code: String, val flag: String)

val COUNTRY_CODES = listOf(
    CountryCode("Bangladesh", "+880", "🇧🇩"),
    CountryCode("United States", "+1", "🇺🇸"),
    CountryCode("United Kingdom", "+44", "🇬🇧"),
    CountryCode("India", "+91", "🇮🇳"),
    CountryCode("Pakistan", "+92", "🇵🇰"),
    CountryCode("Saudi Arabia", "+966", "🇸🇦"),
    CountryCode("UAE", "+971", "🇦🇪"),
    CountryCode("Canada", "+1", "🇨🇦"),
    CountryCode("Australia", "+61", "🇦🇺")
)

@Composable
fun PhonePasswordAuthScreen(
    isLoading: Boolean,
    errorMessage: String?,
    onSignIn: (phoneNumber: String, password: String) -> Unit,
    onSignUp: (phoneNumber: String, password: String, name: String) -> Unit,
    onForgotPassword: (phoneNumber: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) -> Unit = { _, _, _ -> },
    onClearError: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Sign In, 1 = Register
    var selectedCountry by remember { mutableStateOf(COUNTRY_CODES[0]) } // Default BD +880
    var phoneNumberInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }

    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var localValidationMessage by remember { mutableStateOf<String?>(null) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Brand Header Icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(WhatsappTeal, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FamilyRestroom,
                        contentDescription = "Talkly",
                        tint = Color.White,
                        modifier = Modifier.size(46.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Welcome to Talkly",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = WhatsappTeal
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Talkly Family Messenger - Secure Mobile Auth",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Mode Tabs (Sign In / Register)
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFFF0F4F6),
                    contentColor = WhatsappTeal,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = WhatsappGreen
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF0F4F6), RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                            localValidationMessage = null
                            onClearError()
                        },
                        text = {
                            Text(
                                text = "Sign In",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 15.sp,
                                color = if (selectedTab == 0) WhatsappTeal else Color.Gray
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            localValidationMessage = null
                            onClearError()
                        },
                        text = {
                            Text(
                                text = "Register",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 15.sp,
                                color = if (selectedTab == 1) WhatsappTeal else Color.Gray
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Auth Input Form Card
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {

                        // Full Name Field (Register Mode Only)
                        if (selectedTab == 1) {
                            Text(
                                text = "Full Name",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = WhatsappTeal
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = nameInput,
                                onValueChange = {
                                    nameInput = it
                                    localValidationMessage = null
                                },
                                textStyle = TextStyle(color = Color(0xFF111B21), fontSize = 16.sp),
                                placeholder = { Text("e.g. Abdur Rahman", fontSize = 14.sp, color = Color.Gray) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = WhatsappTeal,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = WhatsappGreen,
                                    unfocusedBorderColor = Color.LightGray,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Mobile Phone Number Label & Input
                        Text(
                            text = "Mobile Phone Number",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = WhatsappTeal
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Country Code Dropdown Trigger
                            Box {
                                Surface(
                                    color = Color(0xFFF0F4F6),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.clickable { dropdownExpanded = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${selectedCountry.flag} ${selectedCountry.code}",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF111B21)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Select country",
                                            tint = Color.Gray
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = dropdownExpanded,
                                    onDismissRequest = { dropdownExpanded = false }
                                ) {
                                    COUNTRY_CODES.forEach { item ->
                                        DropdownMenuItem(
                                            text = {
                                                Text("${item.flag}  ${item.country} (${item.code})")
                                            },
                                            onClick = {
                                                selectedCountry = item
                                                dropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Phone Number Field
                            OutlinedTextField(
                                value = phoneNumberInput,
                                onValueChange = { input ->
                                    if (input.length <= 12 && input.all { it.isDigit() }) {
                                        phoneNumberInput = input
                                        localValidationMessage = null
                                    }
                                },
                                textStyle = TextStyle(
                                    color = Color(0xFF111B21),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                placeholder = { Text("1712345678", fontSize = 15.sp, color = Color.Gray) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = null,
                                        tint = WhatsappTeal,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFF111B21),
                                    unfocusedTextColor = Color(0xFF111B21),
                                    focusedBorderColor = WhatsappGreen,
                                    unfocusedBorderColor = Color.LightGray,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Password Field
                        Text(
                            text = "Password",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = WhatsappTeal
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = {
                                passwordInput = it
                                localValidationMessage = null
                            },
                            textStyle = TextStyle(color = Color(0xFF111B21), fontSize = 16.sp),
                            placeholder = { Text("At least 6 characters", fontSize = 14.sp, color = Color.Gray) },
                            singleLine = true,
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = if (selectedTab == 0) ImeAction.Done else ImeAction.Next
                            ),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = WhatsappTeal,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle Password Visibility",
                                        tint = Color.Gray
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = WhatsappGreen,
                                unfocusedBorderColor = Color.LightGray,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Forgot Password Link (Sign In Mode Only)
                        if (selectedTab == 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = {
                                        localValidationMessage = null
                                        showForgotPasswordDialog = true
                                    },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        text = "Forgot Password?",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = WhatsappTeal
                                    )
                                }
                            }
                        }

                        // Confirm Password Field (Register Mode Only)
                        if (selectedTab == 1) {
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Confirm Password",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = WhatsappTeal
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = confirmPasswordInput,
                                onValueChange = {
                                    confirmPasswordInput = it
                                    localValidationMessage = null
                                },
                                textStyle = TextStyle(color = Color(0xFF111B21), fontSize = 16.sp),
                                placeholder = { Text("Re-enter password", fontSize = 14.sp, color = Color.Gray) },
                                singleLine = true,
                                visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                shape = RoundedCornerShape(12.dp),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = WhatsappTeal,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                                        Icon(
                                            imageVector = if (isConfirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "Toggle Confirm Password Visibility",
                                            tint = Color.Gray
                                        )
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = WhatsappGreen,
                                    unfocusedBorderColor = Color.LightGray,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Display Error Messages
                        val displayError = localValidationMessage ?: errorMessage
                        if (!displayError.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = displayError,
                                color = Color.Red,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Bottom Actions (Primary Button & Mode Switch)
            Column(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus()

                        val fullPhone = "${selectedCountry.code}${phoneNumberInput.trim()}"

                        if (phoneNumberInput.length < 6) {
                            localValidationMessage = "Please enter a valid mobile phone number."
                            return@Button
                        }

                        if (passwordInput.length < 6) {
                            localValidationMessage = "Password must be at least 6 characters long."
                            return@Button
                        }

                        if (selectedTab == 1) { // Register Mode
                            if (nameInput.isBlank()) {
                                localValidationMessage = "Please enter your full name."
                                return@Button
                            }
                            if (passwordInput != confirmPasswordInput) {
                                localValidationMessage = "Passwords do not match. Please re-check."
                                return@Button
                            }
                            onSignUp(fullPhone, passwordInput, nameInput.trim())
                        } else { // Sign In Mode
                            onSignIn(fullPhone, passwordInput)
                        }
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsappGreen),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (selectedTab == 1) Icons.Default.PersonAdd else Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (selectedTab == 1) "Create Account" else "Sign In",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Switcher Link
                TextButton(
                    onClick = {
                        selectedTab = if (selectedTab == 0) 1 else 0
                        localValidationMessage = null
                        onClearError()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (selectedTab == 0) "Don't have an account? Register now" else "Already have an account? Sign In",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = WhatsappTeal
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "By continuing, you agree to Talkly's Terms of Service and Privacy Policy.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (showForgotPasswordDialog) {
            var forgotPhoneInput by remember { mutableStateOf(phoneNumberInput) }
            var forgotCountry by remember { mutableStateOf(selectedCountry) }
            var forgotDropdownExpanded by remember { mutableStateOf(false) }
            var isSendingReset by remember { mutableStateOf(false) }
            var resetSuccessMessage by remember { mutableStateOf<String?>(null) }
            var resetErrorMessage by remember { mutableStateOf<String?>(null) }

            AlertDialog(
                onDismissRequest = {
                    if (!isSendingReset) {
                        showForgotPasswordDialog = false
                    }
                },
                title = {
                    Text(
                        text = "Reset Password",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = WhatsappTeal
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Enter your mobile phone number to receive password reset instructions.",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box {
                                Surface(
                                    color = Color(0xFFF0F4F6),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.clickable { forgotDropdownExpanded = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${forgotCountry.flag} ${forgotCountry.code}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF111B21)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = Color.Gray
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = forgotDropdownExpanded,
                                    onDismissRequest = { forgotDropdownExpanded = false }
                                ) {
                                    COUNTRY_CODES.forEach { item ->
                                        DropdownMenuItem(
                                            text = { Text("${item.flag} ${item.country} (${item.code})") },
                                            onClick = {
                                                forgotCountry = item
                                                forgotDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            OutlinedTextField(
                                value = forgotPhoneInput,
                                onValueChange = { input ->
                                    if (input.length <= 12 && input.all { it.isDigit() }) {
                                        forgotPhoneInput = input
                                        resetErrorMessage = null
                                        resetSuccessMessage = null
                                    }
                                },
                                placeholder = { Text("1712345678", fontSize = 14.sp, color = Color.Gray) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (!resetSuccessMessage.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = resetSuccessMessage!!,
                                color = WhatsappGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (!resetErrorMessage.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = resetErrorMessage!!,
                                color = Color.Red,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (forgotPhoneInput.isBlank() || forgotPhoneInput.length < 6) {
                                resetErrorMessage = "Please enter a valid mobile phone number."
                                return@Button
                            }
                            val fullPhone = "${forgotCountry.code}${forgotPhoneInput.trim()}"
                            isSendingReset = true
                            resetErrorMessage = null
                            resetSuccessMessage = null
                            onForgotPassword(
                                fullPhone,
                                { successMsg ->
                                    isSendingReset = false
                                    resetSuccessMessage = successMsg
                                },
                                { errorMsg ->
                                    isSendingReset = false
                                    resetErrorMessage = errorMsg
                                }
                            )
                        },
                        enabled = !isSendingReset,
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsappGreen)
                    ) {
                        if (isSendingReset) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                        } else {
                            Text("Send Reset Link", color = Color.White)
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showForgotPasswordDialog = false },
                        enabled = !isSendingReset
                    ) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            )
        }
    }
}
