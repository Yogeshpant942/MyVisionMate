package com.example.myvisionmate

import com.example.visionmate.Models.AuthResponse
import com.example.visionmate.Models.userLoginRequest
import com.example.visionmate.Models.userRegister
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiInterface {

    @POST("auth/register")
    suspend fun register(
        @Body registerRequest: userRegister
    ): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body loginRequest: userLoginRequest): Response<AuthResponse>

    @POST("guardian/register")
    suspend fun registerGuadian(@Body registerRequest:GuardianRegisterRequest):

}