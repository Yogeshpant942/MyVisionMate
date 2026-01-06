package com.example.visionmate.Factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myvisionmate.Repositary.Repositary
import com.example.visionmate.ViewModel.LoginAndSignUPViewModel

class LogInAndSignUpFactory(private val repository: Repositary
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(LoginAndSignUPViewModel::class.java) -> {
                LoginAndSignUPViewModel(repository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}