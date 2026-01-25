package com.example.visionmate.Models

data class AuthResponse (
    val success: Boolean,
    val message:String,
    val data:AuthData?
)