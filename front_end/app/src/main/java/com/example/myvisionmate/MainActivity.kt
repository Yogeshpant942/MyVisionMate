package com.example.myvisionmate

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.provider.Settings
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.myvisionmate.Repositary.Repositary
import com.example.myvisionmate.Services.EmergencyService
import com.example.myvisionmate.Services.ShakeLaunchService

class MainActivity : AppCompatActivity() {
    private lateinit var navController: NavController
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        val serviceIntent = Intent(this, EmergencyService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)

           val intent = Intent(this, ShakeLaunchService::class.java)
   ContextCompat.startForegroundService(this, intent)


        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        if (!Settings.canDrawOverlays(this)) {
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName"))
            )
        }
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        val api: ApiInterface = RetrofitService.api
        val repo = Repositary(api)

        val isLoggedIn = repo.isLoggedIn(this)
        android.util.Log.d("MainActivity", "User logged in: $isLoggedIn")
        if(isLoggedIn){
            navController.navigate(R.id.homeFragment)
        }
    }
}