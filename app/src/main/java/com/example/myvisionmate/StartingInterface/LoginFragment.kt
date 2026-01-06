package com.example.visionmate

import android.os.Bundle
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
import com.example.myvisionmate.databinding.FragmentLoginBinding
import com.example.visionmate.Factory.LogInAndSignUpFactory
import com.example.visionmate.ViewModel.LoginAndSignUPViewModel

class LoginFragment : Fragment() {

    private lateinit var binding: FragmentLoginBinding
    private lateinit var viewModel: LoginAndSignUPViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentLoginBinding.inflate(inflater, container, false)

        // -------- ViewModel setup --------
        val api: ApiInterface = RetrofitService.api
        val repository = Repositary(api)
        val factory = LogInAndSignUpFactory(repository)

        viewModel = ViewModelProvider(this, factory)
            .get(LoginAndSignUPViewModel::class.java)

        // -------- Login button --------
        binding.btnLogin.setOnClickListener {
            loginUser()
        }

        // -------- Navigate to Register --------
        binding.tvSignUp.setOnClickListener {
            findNavController()
                .navigate(R.id.action_loginFragment_to_registerFragment)
        }

        observeViewModel()

        return binding.root
    }

    private fun loginUser() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        viewModel.login_user(email, password)
    }

    private fun observeViewModel() {
        viewModel.authResult.observe(viewLifecycleOwner) { result ->
            when (result) {

                is LoginAndSignUPViewModel.AuthResult.Success -> {
                    viewModel.saveToken(result.token, requireContext())

                    Toast.makeText(
                        requireContext(),
                        result.message,
                        Toast.LENGTH_SHORT
                    ).show()

                    findNavController()
                        .navigate(R.id.action_loginFragment_to_welcomeFragment)
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
}
