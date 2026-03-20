package com.example.myvisionmate

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.myvisionmate.Repositary.Repositary
import com.example.myvisionmate.databinding.FragmentSettingBinding
import com.example.visionmate.Factory.LogInAndSignUpFactory
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
        pref = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        val api: ApiInterface = RetrofitService.api
        val repo = Repositary(api)
        val factory = LogInAndSignUpFactory(repo)
        viewModel = ViewModelProvider(this,factory).get(LoginAndSignUPViewModel::class.java)
        val name = pref.getString("user_name",null)
        val email = pref.getString("user_email",null)
        binding.tvUserName.text = name
        Log.d("user_name",name.toString())
        Log.d("user_email",email.toString())
        binding.tvUserEmail.text = pref.getString("user_email",null)
        setUplisteners()
        UserobserverViewModel()
        PasswordObserveViewModel()
        setContactCount()
        binding.btnManageGuardians.setOnClickListener {
            findNavController().navigate(R.id.action_settingFragment_to_emergencyContactFragment)
        }
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
        val emailEt: TextView = dialogView.findViewById(R.id.tvProfileEmail)
        val nameEt:TextView = dialogView.findViewById(R.id.etProfileName)
        val phoneNoEt:TextView = dialogView.findViewById(R.id.tvProfilePhone)
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
            .show()
    }
    private fun changePassword(){
          val dialogView = layoutInflater.inflate(R.layout.dialog_change_password,null)
         val emailEt: TextView = dialogView.findViewById(R.id.tvEmail)
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
            .show()
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

    fun setContactCount(){
        val pref_contact = requireContext().getSharedPreferences("visionmate", Context.MODE_PRIVATE)
        val no_count = pref_contact.getStringSet("guardian_no", emptySet())

        binding.tvGuardianCount.text = "${no_count?.size}"    }


}