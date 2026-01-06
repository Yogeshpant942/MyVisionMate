package com.example.myvisionmate.Models

import com.example.visionmate.ViewModel.LoginAndSignUPViewModel

data class GuardianReponse(
    val success: Boolean,
    val message:String,
    val data:GuardianData?
)