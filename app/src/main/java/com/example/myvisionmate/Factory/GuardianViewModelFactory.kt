package com.example.myvisionmate.Factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myvisionmate.Repositary.Repositary
import com.example.myvisionmate.ViewModel.GuardianViewModel

data class GuardianViewModelFactory(
    private val repository: Repositary
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GuardianViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GuardianViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}