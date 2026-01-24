package com.example.myvisionmate

import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.myvisionmate.databinding.FragmentSettingBinding
import com.example.visionmate.ViewModel.LoginAndSignUPViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class SettingFragment : Fragment() {
    lateinit var binding: FragmentSettingBinding
    lateinit var pref: SharedPreferences
    lateinit var viewModel: LoginAndSignUPViewModel
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSettingBinding.inflate(inflater,container,false)
        setUplisteners()
        UserobserverViewModel()
        PasswordObserveViewModel()
        return binding.root
    }
    private fun setUplisteners() {
        logOut()
        binding.btnEditProfile.setOnClickListener(){
        editProfile()
        }
        binding.btnChangePassword.setOnClickListener {
            changePassword()
        }
    }
    private fun editProfile() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_profile,null)
        val emailEt: TextView = dialogView.findViewById(R.id.etProfileEmail)
        val nameEt:TextView = dialogView.findViewById(R.id.etProfileName)
        val phoneNoEt:TextView = dialogView.findViewById(R.id.etProfilePhone)
        val token = pref?.getString("auth_token","")

        emailEt.setText(pref.getString("user_email",""))
        nameEt.setText(pref.getString("user_name",""))
        phoneNoEt.setText(pref.getString("user_phone",""))

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit Profile")
            .setView(dialogView)
            .setPositiveButton("save"){_,_ ->
                val name = nameEt.text.toString().trim()
                val email = emailEt.text.toString().trim()
                val phone = phoneNoEt.text.toString().trim()

                if (name.isEmpty() || email.isEmpty()) {
                    Toast.makeText(context, "Name and email required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                lifecycleScope.launch {
                        viewModel.updateUser(token,email,name,phone)
                }
            }
    }

    private fun changePassword(){
          val dialogView = layoutInflater.inflate(R.layout.dialog_change_password,null)
         val emailEt: TextView = dialogView.findViewById(R.id.etEmail)
         val newPasswordEt: TextView = dialogView.findViewById(R.id.etNewPassword)
         val oldPasswordEt: TextView = dialogView.findViewById(R.id.etCurrentPassword)
        val token = pref?.getString("auth_token","")

        emailEt.setText(pref.getString("user_email",""))

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Change Password")
            .setView(dialogView)
            .setPositiveButton("Change"){_,_->
                val newPass = newPasswordEt.text.toString().trim()
                val email = emailEt.text.toString().trim()
                val oldPass = oldPasswordEt.text.toString().trim()

                if (email.isEmpty()) {
                    Toast.makeText(context, "email required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                lifecycleScope.launch {
                    viewModel.updatePassword(token,email,newPass,oldPass)
                }
            }
    }

    private fun logOut() {
        binding.btnLogout.setOnClickListener {
            pref.edit().clear().apply()
            viewModel.logout(requireContext())
            findNavController().navigate(R.id.action_settingFragment_to_loginFragment)

            Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
        }
    }

    fun UserobserverViewModel(){
        viewModel.updateResult.observe(viewLifecycleOwner, Observer{result->
            when(result){
                is LoginAndSignUPViewModel.UpdateResult.Success->{
                    pref.edit()
                        .putString("user_name",result.user.name)
                        .putString("user_email",result.user.email)
                        .putString("user_phone",result.user.phone)
                    Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
                }
                is LoginAndSignUPViewModel.UpdateResult.Error -> {
                    Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    fun PasswordObserveViewModel(){
        viewModel.changeResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is LoginAndSignUPViewModel.PasswordResult.Success -> {
                    Toast.makeText(context, result.message ?: "Password updated", Toast.LENGTH_SHORT).show()
                }
                is LoginAndSignUPViewModel.PasswordResult.Error -> {
                    Toast.makeText(context, result.message ?: "Password update failed", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }


}