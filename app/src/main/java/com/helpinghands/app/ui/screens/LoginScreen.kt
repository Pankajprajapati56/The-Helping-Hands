package com.helpinghands.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.helpinghands.app.R
import com.helpinghands.app.data.model.AppUser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (AppUser) -> Unit
) {
    var userId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val handleLogin = {
        val trimmedUser = userId.trim()
        val trimmedPass = password.trim()

        if (trimmedUser.isEmpty() || trimmedPass.isEmpty()) {
            errorMessage = "कृपया User ID और Password दोनों दर्ज करें"
        } else if (trimmedUser.equals("admin", ignoreCase = true) && trimmedPass == "admin123") {
            errorMessage = null
            onLoginSuccess(
                AppUser(
                    username = "admin",
                    role = "ADMIN",
                    displayName = "System Administrator"
                )
            )
        } else if (trimmedUser.startsWith("THH", ignoreCase = true) || trimmedUser.toIntOrNull() != null) {
            val formattedId = if (trimmedUser.startsWith("THH", ignoreCase = true)) {
                trimmedUser.uppercase()
            } else {
                "THH%03d".format(trimmedUser.toInt())
            }
            errorMessage = null
            onLoginSuccess(
                AppUser(
                    username = formattedId,
                    role = "MEMBER",
                    memberId = formattedId,
                    displayName = "Member ($formattedId)"
                )
            )
        } else {
            // General login fallback
            errorMessage = null
            onLoginSuccess(
                AppUser(
                    username = trimmedUser,
                    role = if (trimmedUser.contains("admin", ignoreCase = true)) "ADMIN" else "MEMBER",
                    displayName = trimmedUser
                )
            )
        }
    }

    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // App Logo
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_helping_hands_logo),
                        contentDescription = "Helping Hands Logo",
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "THE HELPING HANDS",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "समिति प्रबंधन प्रणाली 2026 • कांगहट्टी (मंदसौर)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text(
                            text = "लॉगिन करें (Sign In)",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Admin अथवा Member ID से प्रवेश करें",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedTextField(
                            value = userId,
                            onValueChange = {
                                userId = it
                                errorMessage = null
                            },
                            label = { Text("User ID / Member ID") },
                            placeholder = { Text("e.g. admin or THH001") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null)
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("username_input")
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                errorMessage = null
                            },
                            label = { Text("Password / PIN") },
                            placeholder = { Text("e.g. admin123") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null)
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle password visibility"
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("password_input")
                        )

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        Button(
                            onClick = handleLogin,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("login_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("LOGIN (प्रवेश करें)", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Quick Demo Login Chips
                Text(
                    text = "त्वरित डेमो लॉगिन (Quick Demo Access):",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    FilledTonalButton(
                        onClick = {
                            userId = "admin"
                            password = "admin123"
                            handleLogin()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text("Admin (admin123)")
                    }

                    FilledTonalButton(
                        onClick = {
                            userId = "THH003"
                            password = "1234"
                            handleLogin()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text("Member THH003")
                    }
                }
            }
        }
    }
}
