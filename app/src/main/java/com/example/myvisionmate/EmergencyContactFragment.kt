package com.example.myvisionmate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myvisionmate.Factory.GuardianViewModelFactory
import com.example.myvisionmate.Models.Guardian
import com.example.myvisionmate.Repositary.Repositary
import com.example.myvisionmate.ViewModel.GuardianViewModel
import com.example.myvisionmate.databinding.FragmentEmergencyContactBinding
import kotlinx.coroutines.launch

class EmergencyContactFragment : Fragment() {
    private lateinit var binding: FragmentEmergencyContactBinding
    private lateinit var viewModel: GuardianViewModel
    private lateinit var guardianAdapter: GuardianAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentEmergencyContactBinding.inflate(inflater, container, false)

        val api: ApiInterface = RetrofitService.api
        val repo = Repositary(api)
        val factory = GuardianViewModelFactory(repo)
        viewModel = ViewModelProvider(this, factory)
            .get(GuardianViewModel::class.java)

        setupRecyclerView()

        observeViewModel()

        loadGuardian()

        return binding.root
    }

    private fun setupRecyclerView() {
        guardianAdapter = GuardianAdapter { guardian ->
            val token = getAuthToken()
            if (token != null) {
                viewModel.deleteGuardian(token, guardian._id)
            } else {
                Toast.makeText(requireContext(), "Login required", Toast.LENGTH_SHORT).show()
            }
        }

        binding.recyclerViewContacts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = guardianAdapter
            setHasFixedSize(true)
        }
    }


    private fun observeViewModel() {

        lifecycleScope.launch {
            viewModel.gaurdian.collect { guardians ->
                updateUi(guardians)
            }
        }

        lifecycleScope.launch {
            viewModel.guardianResult.collect { result ->
                when (result) {
                    is GuardianViewModel.GuardianResult.Success -> {
                        // Toast.makeText(requireContext(), "Operation successful", Toast.LENGTH_SHORT).show()
                        viewModel.resetResult()
                    }

                    is GuardianViewModel.GuardianResult.Error -> {
                        Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
                        viewModel.resetResult()
                    }

                    null -> Unit
                }
            }
        }
    }

    private fun loadGuardian() {
        val token = getAuthToken()
        if (token != null) {
            viewModel.loadGuardians(token)
        } else {
            Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_SHORT).show()
        }
    }
    private fun getAuthToken(): String? {
        return requireContext()
            .getSharedPreferences("app_prefs", 0)
            .getString("auth_token", null)
    }

    private fun updateUi(guardians: List<Guardian>) {
        if (guardians.isEmpty()) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.recyclerViewContacts.visibility = View.GONE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.recyclerViewContacts.visibility = View.VISIBLE
        }

        guardianAdapter.submitList(guardians)
    }
}
