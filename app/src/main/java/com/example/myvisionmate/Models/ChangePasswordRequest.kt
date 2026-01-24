package com.example.myvisionmate.Models

data class ChangePasswordRequest(
    val email:String?="",
    val newPassword:String? = null,
    val oldPassword:String? = null
)
