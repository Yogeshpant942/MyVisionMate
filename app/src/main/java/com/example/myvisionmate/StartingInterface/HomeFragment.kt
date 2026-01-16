package com.example.myvisionmate.StartingInterface

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.myvisionmate.R
import com.example.myvisionmate.databinding.FragmentHomeBinding
class HomeFragment : Fragment() {
    lateinit var binding: FragmentHomeBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater,container,false)

        binding.cardDescribe.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_describeSurroundingsFragment)
        }
        binding.cardReadText.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_readTextFragment)
        }
        binding.cardStartScanning.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_cameraFragment)
        }
        return binding.root
    }

}