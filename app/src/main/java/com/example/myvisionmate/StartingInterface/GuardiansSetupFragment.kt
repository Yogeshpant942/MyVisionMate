package com.example.visionmate.StartingInterface

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.FragmentNavigatorExtras
import com.example.myvisionmate.R
import com.example.myvisionmate.databinding.FragmentGuardiansSetupBinding

class GuardiansSetupFragment : Fragment() {
    lateinit var binding: FragmentGuardiansSetupBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentGuardiansSetupBinding.inflate(inflater,container,false)
        binding.btnAddGuardian.setOnClickListener {
            val name = binding.etGuardianName.text.toString()
            val phone = binding.etGuardianPhone.text.toString()

        }
        return binding.root
    }


}