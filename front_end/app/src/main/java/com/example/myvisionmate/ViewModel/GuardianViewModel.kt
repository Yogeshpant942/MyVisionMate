package com.example.myvisionmate.ViewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myvisionmate.Models.Guardian
import com.example.myvisionmate.Repositary.Repositary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.log

class GuardianViewModel(private val repo: Repositary) : ViewModel() {
    private val TAG = "GuardianViewModel"

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
                Log.d(TAG, "Calling getAllGuardian API")
                val response = repo.getAllGuardian(token)

                Log.d(TAG, "API response code = ${response.code()}")

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    Log.d(TAG, "API success = ${body.success}")

                    if (body.success && body.data != null) {
                        Log.d(TAG, "Guardians received = ${body.data.guardians.size}")
                        _guardian.value = body.data.guardians
                    } else {
                        Log.e(TAG, "API error message = ${body.message}")
                    }
                } else {
                    Log.e(TAG, "API failed, errorBody = ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in loadGuardians()", e)
            }
        }
    }

    fun deleteGuardian(guardianId: String, token: String?) {
        viewModelScope.launch {
            try {
                val response = repo.deleteGuardian(guardianId, token!!)

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
