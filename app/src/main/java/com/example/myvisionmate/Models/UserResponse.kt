package com.example.myvisionmate.Models

import com.example.visionmate.Models.User

data class UserResponse(
    val success:Boolean =false,
    val message:String? = "",
    val data: User
)