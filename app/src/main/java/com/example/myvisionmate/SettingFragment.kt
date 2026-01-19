package com.example.myvisionmate

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.myvisionmate.databinding.FragmentSettingBinding
import com.example.visionmate.LoginFragment
import com.example.visionmate.ViewModel.LoginAndSignUPViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import kotlin.math.log

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

        return inflater.inflate(R.layout.fragment_setting, container, false)
    }
    private fun setUplisteners() {
        logOut()
        binding.btnEditProfile.setOnClickListener(){
        editProfile()
        }
    }
    private fun editProfile() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_profile,null)
        val emailEt: TextView = dialogView.findViewById(R.id.etProfileEmail)
        val nameEt:TextView = dialogView.findViewById(R.id.etProfileName)
        val phoneNoEt:TextView = dialogView.findViewById(R.id.etProfilePhone)

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
                    vie
                }

            }
    }

    private fun changePassword(){

    }

    private fun logOut() {
        binding.btnLogout.setOnClickListener {
            pref.edit().clear().apply()
            viewModel.logout(requireContext())
            findNavController().navigate(R.id.action_settingFragment_to_loginFragment)

            Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()

        }
    }


}