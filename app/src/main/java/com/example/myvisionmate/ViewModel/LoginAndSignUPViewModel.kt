package com.example.visionmate.ViewModel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myvisionmate.Repositary.Repositary
import com.example.visionmate.Models.AuthResponse
import com.example.visionmate.Models.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginAndSignUPViewModel(private val repo: Repositary): ViewModel() {
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
    private val _authResult = MutableLiveData<AuthResult>()
    val authResult: LiveData<AuthResult> = _authResult

     fun register_user(name:String,password:String,email:String,phone:String){
        if (name.isBlank() || email.isBlank() || password.isBlank() || phone.isBlank()) {
            _authResult.value = AuthResult.Error("All fields are required")
            return }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _authResult.value = AuthResult.Error("Invalid email format")
            return }
        if (password.length < 6) {
            _authResult.value = AuthResult.Error("Password must be at least 6 characters")
            return}

        if (phone.length != 10) {
            _authResult.value = AuthResult.Error("Phone number must be 10 digits")
            return }

        viewModelScope.launch {
            val response = repo.registerUser(name,email,password,phone)
            if(response.isSuccessful && response.body()!=null){
                val authResponse = response.body()
                if(authResponse!!.success &&authResponse.data!=null){
                    _authResult.value = AuthResult.Success(
                       user = authResponse.data.user,
                        token = authResponse.data.token,
                        message = authResponse.message
                    )
                }
                else {
                    _authResult.value = AuthResult.Error(authResponse.message)
                }
            }
            else{
                val errorMsg = when (response.code()) {
                    400 -> "User already exists or invalid data"
                    500 -> "Server error. Please try again later"
                    else -> "Registration failed: ${response.message()}"
                }
                _authResult.value = AuthResult.Error(errorMsg)
            }

        }
    }

     fun login_user(email:String,password: String){
        if (email.isBlank() || password.isBlank()) {
            _authResult.value = AuthResult.Error("Email and password are required")
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _authResult.value = AuthResult.Error("Invalid email format")
            return
        }

        viewModelScope.launch {
            val response = repo.loginUser(email,password)
            if(response.isSuccessful && response.body()!=null){
                val auth_response = response.body()
                if(auth_response!!.success && auth_response.data!=null){
                    _authResult.value = AuthResult.Success(
                        user = auth_response.data.user,
                        token = auth_response.data.token,
                        message = auth_response.message
                    )
                }
                else{
                    _authResult.value = AuthResult.Error(auth_response.message)
                }
            }
            else{
                val errorMsg = when (response.code()) {
                    401 -> "Invalid email or password"
                    404 -> "User not found. Please sign up"
                    500 -> "Server error. Please try again later"
                    else -> "Login failed: ${response.message()}"
                }
                _authResult.value = AuthResult.Error(errorMsg)
            }
        }

    }

    fun saveToken(token: String,context: Context){
        repo.saveToken(token,context)
    }

    fun isLoggedIn(context: Context): Boolean {
        return repo.isLoggedIn(context)
    }

    fun logout(context: Context) {
        repo.removeToken(context)
    }

}