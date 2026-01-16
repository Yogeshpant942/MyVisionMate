package com.example.visionmate.startinginterface

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.myvisionmate.ApiInterface
import com.example.myvisionmate.Repositary.Repositary
import com.example.myvisionmate.RetrofitService
import com.example.myvisionmate.databinding.FragmentRegisterBinding
import com.example.visionmate.Factory.LogInAndSignUpFactory
import com.example.visionmate.Models.User
import com.example.visionmate.ViewModel.LoginAndSignUPViewModel
import retrofit2.Retrofit

class RegisterFragment : Fragment() {

    private lateinit var binding: FragmentRegisterBinding
    private lateinit var viewModel: LoginAndSignUPViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentRegisterBinding.inflate(inflater, container, false)
        // ---- ViewModel setup ----
        val api: ApiInterface = RetrofitService.api
        val repository = Repositary(api)
        val factory = LogInAndSignUpFactory(repository)

        viewModel = ViewModelProvider(this, factory)
            .get(LoginAndSignUPViewModel::class.java)

        binding.btnRegister.setOnClickListener {
            setUpUI()
        }

        observeViewModel()

        return binding.root
    }

    private fun setUpUI() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        viewModel.register_user(name, password, email, phone)
    }

    private fun observeViewModel() {
        viewModel.authResult.observe(viewLifecycleOwner) { result ->
            when (result) {

                is LoginAndSignUPViewModel.AuthResult.Success -> {
                    viewModel.saveToken(result.token,requireContext())
                    saveUserInfo(requireContext(), result.user)

                    Toast.makeText(
                        requireContext(),
                        result.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }

                is LoginAndSignUPViewModel.AuthResult.Error -> {
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
        val prefs = context
            .getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .edit()

        prefs.putString("user_id", user.id)
        prefs.putString("user_name", user.name)
        prefs.putString("user_email", user.email)
        prefs.putString("user_phone", user.phone)
        prefs.putBoolean("is_first_login", user.isFirstLogin)
        prefs.apply()
    }
}
