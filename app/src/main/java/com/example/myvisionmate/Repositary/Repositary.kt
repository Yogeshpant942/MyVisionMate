package com.example.myvisionmate.Repositary

import android.content.Context
import com.example.myvisionmate.ApiInterface
import com.example.visionmate.Models.AuthResponse
import com.example.visionmate.Models.userLoginRequest
import com.example.visionmate.Models.userRegister
import retrofit2.Response

class Repositary(private val api: ApiInterface) {

    suspend fun registerUser(
        name: String,
        email: String,
        password: String,
        phone: String
    ): Response<AuthResponse> {
        val registerRequest = userRegister(
            name = name,
            email = email,
            password = password,
            phoneNo = phone
        )
        return api.register(registerRequest)
    }

    suspend fun loginUser(
        email: String,
        password: String
    ): Response<AuthResponse> {
        val loginRequest = userLoginRequest(
            email = email,
            password = password
        )
        return api.login(loginRequest)
    }

    fun saveToken(token: String, context: Context) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("auth_token", token).apply()
    }

    fun getToken(context: Context): String? {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return prefs.getString("auth_token", null)
    }

    fun removeToken(context: Context) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove("auth_token").apply()
    }

    fun isLoggedIn(context: Context): Boolean {
        return getToken(context) != null
    }



    fun registerGuardian()
}