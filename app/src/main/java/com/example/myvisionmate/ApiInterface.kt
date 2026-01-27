package com.example.myvisionmate

import com.example.myvisionmate.Models.BaseReponse
import com.example.myvisionmate.Models.ChangePasswordRequest
import com.example.myvisionmate.Models.ChangePasswordResponse
import com.example.myvisionmate.Models.GuardianData
import com.example.myvisionmate.Models.GuardianReponse
import com.example.myvisionmate.Models.GuardianRequest
import com.example.myvisionmate.Models.GuardiansListData
import com.example.myvisionmate.Models.GuardiansListResponse
import com.example.myvisionmate.Models.UserResponse
import com.example.myvisionmate.Models.UserUpdateRequest
import com.example.myvisionmate.Repositary.Repositary
import com.example.visionmate.Models.AuthResponse
import com.example.visionmate.Models.userLoginRequest
import com.example.visionmate.Models.userRegister
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import kotlin.io.encoding.Base64

interface ApiInterface {

    @POST("auth/register")
    suspend fun register(@Body registerRequest: userRegister): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body loginRequest: userLoginRequest): Response<AuthResponse>

    @POST("guardian/register")
    suspend fun registerGuardian(@Header("Authorization")token:String, @Body registerRequest: GuardianRequest): Response<GuardianReponse>

    @GET("guardian/getGuardian")
    suspend fun getAllGuardian(@Header("Authorization")token:String): Response<GuardiansListResponse>

    @PATCH("guardian/guardian_update/:id")
    suspend fun updateGuardian(@Path("id") guardianId:String, @Header("Authorization")token:String, @Body request: GuardianRequest): Response<GuardianReponse>

    @DELETE("delete/{id}")
    suspend fun deleteGuardian(@Path("id") guardianId:String,@Header("Authorization")token:String): Response<BaseReponse>


    @PUT("auth/updateUser")
    suspend fun updateUser(@Header("Authorization")token: String,@Body updateRequest: UserUpdateRequest): Response<UserResponse>

    @PUT("auth/changePassword")
    suspend fun changeUserPassword(@Header("Authorization")token: String,@Body changeRequest: ChangePasswordRequest): Response<ChangePasswordResponse>


    

}