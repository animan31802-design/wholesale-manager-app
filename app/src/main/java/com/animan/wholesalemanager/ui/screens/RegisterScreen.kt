package com.animan.wholesalemanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.animan.wholesalemanager.ui.components.AuthBackground
import com.animan.wholesalemanager.viewmodel.AuthViewModel

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {}
) {
    val viewModel: AuthViewModel = viewModel()
    val context = LocalContext.current

    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var localError      by remember { mutableStateOf<String?>(null) }

    AuthBackground {

        Text("Create account", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value         = email,
            onValueChange = { email = it; localError = null },
            label         = { Text("Email") },
            modifier      = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value               = password,
            onValueChange       = { password = it; localError = null },
            label               = { Text("Password") },
            modifier            = Modifier.fillMaxWidth(),
            visualTransformation = if (passwordVisible) VisualTransformation.None
            else PasswordVisualTransformation(),
            trailingIcon        = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Filled.VisibilityOff
                        else Icons.Filled.Visibility,
                        contentDescription = null
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value               = confirmPassword,
            onValueChange       = { confirmPassword = it; localError = null },
            label               = { Text("Confirm password") },
            modifier            = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(20.dp))

        val error = localError ?: viewModel.errorMessage.value
        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                localError = when {
                    email.isBlank()              -> "Email cannot be empty"
                    password.length < 6          -> "Password must be at least 6 characters"
                    password != confirmPassword  -> "Passwords do not match"
                    else                         -> null
                }
                if (localError != null) return@Button

                viewModel.register(email.trim(), password.trim(), context) {
                    onRegisterSuccess()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled  = !viewModel.isLoading.value
        ) {
            if (viewModel.isLoading.value) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier    = Modifier.size(20.dp)
                )
            } else {
                Text("Register")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(
            onClick  = onNavigateToLogin,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Already have an account? Login")
        }
    }
}