package com.example.myvisionmate.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myvisionmate.Models.Guardian
import com.example.myvisionmate.Repositary.Repositary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GuardianViewModel(private val repo: Repositary) : ViewModel() {

    sealed class GuardianResult {
        data class Success(val message: String) : GuardianResult()
        data class Error(val message: String) : GuardianResult()
    }

    private val _guardian = MutableStateFlow<List<Guardian>>(emptyList())
    val gaurdian: StateFlow<List<Guardian>> = _guardian

    private val _guardianResult = MutableStateFlow<GuardianResult?>(null)
    val guardianResult: StateFlow<GuardianResult?> = _guardianResult

    fun addGuardian(token: String, name: String, phone: String) {
        if (name.isBlank() || phone.isBlank()) {
            _guardianResult.value = GuardianResult.Error("Name and Phone are rqeuired")
        }
        if (phone.length != 10) {
            _guardianResult.value = GuardianResult.Error("Phone must be 10 digits")
            return
        }

        viewModelScope.launch {
            try {
                val response = repo.registerGuardian(token, name, phone)

                if (response.isSuccessful && response.body() != null) {
                    val guardianResponse = response.body()!!

                    if (guardianResponse.success) {
                        _guardianResult.value = GuardianResult.Success(
                            "Guardian added successfully"
                        )
                        loadGuardians(token)
                    } else {
                        _guardianResult.value = GuardianResult.Error(
                            guardianResponse.message
                        )
                    }
                } else {
                    _guardianResult.value = GuardianResult.Error(
                        "Failed to add guardian"
                    )
                }
            } catch (e: Exception) {
                _guardianResult.value = GuardianResult.Error(
                    e.message ?: "Network error"
                )
            }
        }
    }

    fun loadGuardians(token: String) {
        viewModelScope.launch {
            try {
                val response = repo.getAllGuardian(token)

                if (response.isSuccessful && response.body() != null) {
                    val listResponse = response.body()!!

                    if (listResponse.success && listResponse.data != null) {
                        _guardian.value = listResponse.data.guardians
                    } else {
                        _guardianResult.value =
                            GuardianResult.Error(listResponse.message)
                    }
                } else {
                    _guardianResult.value =
                        GuardianResult.Error("failed to load guardian data")
                }
            } catch (e: Exception) {
                _guardianResult.value = GuardianResult.Error(
                    e.message ?: "Network error"
                )
            }
        }
    }

    fun deleteGuardian(guardianId: String, token: String) {
        viewModelScope.launch {
            try {
                val response = repo.deleteGuardian(guardianId, token)

                if (response.isSuccessful && response.body() != null) {
                    val guardianResponse = response.body()!!

                    if (guardianResponse.success) {
                        _guardianResult.value = GuardianResult.Success(
                            "guardian Deleted Successfully"
                        )
                        loadGuardians(token)
                    } else {
                        _guardianResult.value = GuardianResult.Error(
                            guardianResponse.message
                        )
                    }
                } else {
                    _guardianResult.value = GuardianResult.Error(
                        "Failed to delete guardian"
                    )
                }
            } catch (e: Exception) {
                _guardianResult.value = GuardianResult.Error(
                    e.message ?: "Network error"
                )
            }
        }
    }
    fun resetResult() {
        _guardianResult.value = null
    }

}
