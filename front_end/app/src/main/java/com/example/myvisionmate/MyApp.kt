package com.example.myvisionmate

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class MyApp: Application() {

    override fun onCreate() {
        super.onCreate()

        val pref = getSharedPreferences("theme_prefs",MODE_PRIVATE)
        val isDark = pref.getBoolean("dark mode",false)

        AppCompatDelegate.setDefaultNightMode(
            if(isDark){
                AppCompatDelegate.MODE_NIGHT_NO
            }
            else{
                AppCompatDelegate.MODE_NIGHT_YES
            }
        )
    }
}