package com.example.visionmate.ViewModel

import android.content.Context
import android.util.Log
import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myvisionmate.Repositary.Repositary
import com.example.visionmate.Models.User
import kotlinx.coroutines.launch

class LoginAndSignUPViewModel(private val repo: Repositary) : ViewModel() {

    /* -------------------- SEALED CLASSES -------------------- */

    sealed class UpdateResult {
        data class Success(
            val user: User,
            val message: String?
        ) : UpdateResult()

        data class Error(
            val message: String
        ) : UpdateResult()
    }

    sealed class PasswordResult {
        data class Success(
            val message: String?
        ) : PasswordResult()

        data class Error(
            val message: String?
        ) : PasswordResult()
    }

    sealed class AuthResult {
        data class Success(
            val user: User,
            val token: String,
            val message: String
        ) : AuthResult()

        data class Error(
            val message: String
        ) : AuthResult()
    }

    /* -------------------- LIVE DATA -------------------- */

    private val _authResult = MutableLiveData<AuthResult>()
    val authResult: LiveData<AuthResult> = _authResult

    private val _updateResult = MutableLiveData<UpdateResult>()
    val updateResult: LiveData<UpdateResult> = _updateResult

    private val _changeResult = MutableLiveData<PasswordResult>()
    val changeResult: LiveData<PasswordResult> = _changeResult

    /* -------------------- REGISTER -------------------- */

    fun register_user(name: String, password: String, email: String, phone: String) {

        Log.d("AuthVM", "register_user called")
        Log.d("AuthVM", "Input -> name=$name, email=$email, phone=$phone, passwordLength=${password.length}")

        if (name.isBlank() || email.isBlank() || password.isBlank() || phone.isBlank()) {
            Log.e("AuthVM", "Validation failed: empty fields")
            _authResult.value = AuthResult.Error("All fields are required")
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Log.e("AuthVM", "Validation failed: invalid email")
            _authResult.value = AuthResult.Error("Invalid email format")
            return
        }

        if (password.length < 6) {
            Log.e("AuthVM", "Validation failed: password too short")
            _authResult.value = AuthResult.Error("Password must be at least 6 characters")
            return
        }

        if (phone.length != 10) {
            Log.e("AuthVM", "Validation failed: phone length != 10")
            _authResult.value = AuthResult.Error("Phone number must be 10 digits")
            return
        }

        viewModelScope.launch {
            try {
                Log.d("AuthVM", "Calling registerUser API")

                val response = repo.registerUser(name, email, password, phone)

                Log.d("AuthVM", "API response code = ${response.code()}")

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    Log.d("AuthVM", "API success = ${body.success}")

                    if (body.success && body.data != null) {
                        Log.d("AuthVM", "Registration SUCCESS, userId=${body.data.user.id}")

                        _authResult.value = AuthResult.Success(
                            user = body.data.user,
                            token = body.data.token,
                            message = body.message
                        )
                    } else {
                        Log.e("AuthVM", "API error message: ${body.message}")
                        _authResult.value = AuthResult.Error(body.message)
                    }
                } else {
                    val errorMsg = when (response.code()) {
                        400 -> "User already exists or invalid data"
                        500 -> "Server error. Please try again later"
                        else -> "Registration failed"
                    }

                    Log.e("AuthVM", "HTTP error: ${response.code()} -> $errorMsg")
                    _authResult.value = AuthResult.Error(errorMsg)
                }
            } catch (e: Exception) {
                Log.e("AuthVM", "Exception during register_user", e)
                _authResult.value = AuthResult.Error(
                    e.message ?: "Network error"
                )
            }
        }
    }


    /* -------------------- LOGIN -------------------- */

    fun login_user(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authResult.value = AuthResult.Error("Email and password are required")
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _authResult.value = AuthResult.Error("Invalid email format")
            return
        }

        viewModelScope.launch {
            val response = repo.loginUser(email, password)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success && body.data != null) {
                    _authResult.value = AuthResult.Success(
                        user = body.data.user,
                        token = body.data.token,
                        message = body.message
                    )
                } else {
                    _authResult.value = AuthResult.Error(body.message)
                }
            } else {
                val errorMsg = when (response.code()) {
                    401 -> "Invalid email or password"
                    404 -> "User not found"
                    500 -> "Server error"
                    else -> "Login failed"
                }
                _authResult.value = AuthResult.Error(errorMsg)
            }
        }
    }

    /* -------------------- TOKEN -------------------- */

    fun saveToken(token: String, context: Context) {
        repo.saveToken(token, context)
    }

    fun isLoggedIn(context: Context): Boolean {
        return repo.isLoggedIn(context)
    }

    fun logout(context: Context) {
        repo.removeToken(context)
    }

    /* -------------------- UPDATE USER -------------------- */

    suspend fun updateUser(token: String?, email: String, name: String, phone: String) {
        if (name.isBlank() || email.isBlank() || phone.isBlank()) {
            _updateResult.value = UpdateResult.Error("All fields are required")
            return
        }

        if (phone.length != 10) {
            _updateResult.value = UpdateResult.Error("Phone number must be 10 digits")
            return
        }

        val response = repo.updateUser(token, email, name, phone)

        if (response.isSuccessful && response.body() != null) {
            val body = response.body()!!
            if (body.success && body.data != null) {
                _updateResult.value = UpdateResult.Success(
                    user = body.data,
                    message = body.message
                )
            } else {
                _updateResult.value = UpdateResult.Error(body.message ?: "Update failed")
            }
        } else {
            _updateResult.value = UpdateResult.Error("Server error: ${response.code()}")
        }
    }

    /* -------------------- UPDATE PASSWORD -------------------- */

    suspend fun updatePassword(
        token: String?,
        email: String,
        newPassword: String,
        oldPassword: String
    ) {
        if (email.isBlank() || newPassword.isBlank() || oldPassword.isBlank()) {
            _changeResult.value = PasswordResult.Error("All fields are required")
            return
        }

        if (newPassword.length < 6) {
            _changeResult.value = PasswordResult.Error("Password must be at least 6 characters")
            return
        }

        val response = repo.UpdatePassword(token, email, newPassword, oldPassword)

        if (response.isSuccessful && response.body() != null) {
            val body = response.body()!!
            if (body.success == true) {
                _changeResult.value = PasswordResult.Success(body.message)
            } else {
                _changeResult.value = PasswordResult.Error(body.message)
            }
        } else {
            _changeResult.value = PasswordResult.Error("Server error: ${response.code()}")
        }
    }
}
