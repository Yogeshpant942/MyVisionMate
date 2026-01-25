package com.example.visionmate.startinginterface

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.myvisionmate.ApiInterface
import com.example.myvisionmate.R
import com.example.myvisionmate.Repositary.Repositary
import com.example.myvisionmate.RetrofitService
import com.example.myvisionmate.databinding.FragmentRegisterBinding
import com.example.visionmate.Factory.LogInAndSignUpFactory
import com.example.visionmate.Models.User
import com.example.visionmate.ViewModel.LoginAndSignUPViewModel

class RegisterFragment : Fragment() {

    private lateinit var binding: FragmentRegisterBinding
    private lateinit var viewModel: LoginAndSignUPViewModel

    private val TAG = "RegisterFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        Log.d(TAG, "onCreateView called")

        binding = FragmentRegisterBinding.inflate(inflater, container, false)

        // ---- ViewModel setup ----
        val api: ApiInterface = RetrofitService.api
        val repository = Repositary(api)
        val factory = LogInAndSignUpFactory(repository)

        viewModel = ViewModelProvider(this, factory)
            .get(LoginAndSignUPViewModel::class.java)

        binding.btnRegister.setOnClickListener {
            Log.d(TAG, "Register button clicked")
            setUpUI()
        }

        observeViewModel()

        return binding.root
    }

    // ---------------- UI ACTION ----------------

    private fun setUpUI() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        Log.d(
            TAG,
            "Register input -> name=$name, email=$email, phone=$phone, passwordLength=${password.length}"
        )

        viewModel.register_user(name, password, email, phone)
    }

    // ---------------- OBSERVER ----------------

    private fun observeViewModel() {
        viewModel.authResult.observe(viewLifecycleOwner) { result ->
            when (result) {

                is LoginAndSignUPViewModel.AuthResult.Success -> {
                    Log.d(TAG, "Registration SUCCESS")
                    Log.d(TAG, "Message: ${result.message}")
                    Log.d(TAG, "Token: ${result.token}")

                    viewModel.saveToken(result.token, requireContext())
                    saveUserInfo(requireContext(), result.user)

                    Toast.makeText(
                        requireContext(),
                        result.message,
                        Toast.LENGTH_SHORT
                    ).show()
                    findNavController().navigate(
                        R.id.action_registerFragment_to_welcomeFragment
                    )

                }

                is LoginAndSignUPViewModel.AuthResult.Error -> {
                    Log.e(TAG, "Registration ERROR: ${result.message}")

                    Toast.makeText(
                        requireContext(),
                        result.message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun saveUserInfo(context: Context, user: User) {
        Log.d(
            TAG,
            "Saving user info -> id=${user.id}, name=${user.name}, email=${user.email}"
        )
        val prefs = context
            .getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .edit()

        prefs.putString("user_id", user.id)
        prefs.putString("user_name", user.name)
        prefs.putString("user_email", user.email)
        prefs.putString("user_phone", user.phone)
        prefs.putBoolean("is_first_login", user.isFirstLogin)
        prefs.apply()

        Log.d(TAG, "User info saved successfully")
    }
}
