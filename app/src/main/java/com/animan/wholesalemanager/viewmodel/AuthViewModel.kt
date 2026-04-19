package com.animan.wholesalemanager.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.animan.wholesalemanager.data.remote.FirebaseAuthHelper

class AuthViewModel : ViewModel() {

    private val authHelper = FirebaseAuthHelper()

    var isLoading = mutableStateOf(false)
    var errorMessage = mutableStateOf<String?>(null)

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        if (email.isBlank()) { errorMessage.value = "Email cannot be empty"; return }
        if (password.isBlank()) { errorMessage.value = "Password cannot be empty"; return }
        isLoading.value = true
        authHelper.login(email, password,
            onSuccess = { isLoading.value = false; onSuccess() },
            onError = { isLoading.value = false; errorMessage.value = it }
        )
    }

    fun register(email: String, password: String, onSuccess: () -> Unit) {
        isLoading.value = true
        authHelper.register(email, password,
            onSuccess = { isLoading.value = false; onSuccess() },
            onError = { isLoading.value = false; errorMessage.value = it }
        )
    }
}