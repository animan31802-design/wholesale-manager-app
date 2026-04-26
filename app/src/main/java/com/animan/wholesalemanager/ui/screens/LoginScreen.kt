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
fun LoginScreen(
    onLoginSuccess: () -> Unit = {},
    onNavigateToRegister: () -> Unit = {}
) {
    val viewModel: AuthViewModel = viewModel()
    val context = LocalContext.current

    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    AuthBackground {

        Text("Login", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value         = email,
            onValueChange = { email = it; viewModel.errorMessage.value = null },
            label         = { Text("Email") },
            modifier      = Modifier.fillMaxWidth(),
            singleLine    = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value               = password,
            onValueChange       = { password = it; viewModel.errorMessage.value = null },
            label               = { Text("Password") },
            modifier            = Modifier.fillMaxWidth(),
            singleLine          = true,
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

        Spacer(modifier = Modifier.height(20.dp))

        viewModel.errorMessage.value?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick  = {
                viewModel.login(email.trim(), password.trim(), context) {
                    onLoginSuccess()
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
                Text("Login")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(
            onClick  = onNavigateToRegister,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Don't have an account? Register")
        }
    }
}