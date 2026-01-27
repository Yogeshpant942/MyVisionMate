package com.example.myvisionmate

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.myvisionmate.Repositary.Repositary

class MainActivity : AppCompatActivity() {
    private lateinit var navController: NavController
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

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