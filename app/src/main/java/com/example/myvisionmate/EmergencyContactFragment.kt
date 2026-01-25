package com.example.myvisionmate

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myvisionmate.Factory.GuardianViewModelFactory
import com.example.myvisionmate.Models.Guardian
import com.example.myvisionmate.Repositary.Repositary
import com.example.myvisionmate.ViewModel.GuardianViewModel
import com.example.myvisionmate.databinding.FragmentEmergencyContactBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class EmergencyContactFragment : Fragment() {
    private lateinit var binding: FragmentEmergencyContactBinding
    private lateinit var viewModel: GuardianViewModel
    private lateinit var guardianAdapter: GuardianAdapter
    private val TAG = "EmergencyContactFragment"


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
                viewModel.deleteGuardian(guardian._id, token)
            } else {
                Toast.makeText(requireContext(), "Login required", Toast.LENGTH_SHORT).show()
            }
        }

        guardianAdapter = GuardianAdapter{guardian ->
            showGuardianItemDialog(guardian)
        }
        binding.recyclerViewContacts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = guardianAdapter
            setHasFixedSize(true)
        }
    }

    fun showGuardianItemDialog(guardian: Guardian) {

        val dialogView = layoutInflater.inflate(
            R.layout.dialog_emergency_action,
            null
        )

        // 🔥 IMPORTANT: create & show dialog
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .show()

        val delButton: Button = dialogView.findViewById(R.id.btnDelete)
        val callButton: Button = dialogView.findViewById(R.id.btnCall)
        val editButton: Button = dialogView.findViewById(R.id.btnEdit)

        // DELETE
        delButton.setOnClickListener {
            dialog.dismiss()
        }

        // CALL
        callButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${9528650567}")
            }
            startActivity(intent)
            dialog.dismiss()
        }

        // EDIT
        editButton.setOnClickListener {
            // TODO: edit logic
            dialog.dismiss()
        }
    }









    private fun observeViewModel() {

        lifecycleScope.launch {
            viewModel.gaurdian.collect { guardians ->
                Log.d(TAG, "Collected guardians list, size = ${guardians.size}")
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
        Log.d(TAG, "loadGuardian() called, token = $token")
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
        Log.d(TAG, "updateUi() called with ${guardians.size} guardians")

        if (guardians.isEmpty()) {
            Log.d(TAG, "Guardian list empty, showing empty state")
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.recyclerViewContacts.visibility = View.GONE
        } else {
            Log.d(TAG, "Guardian list NOT empty, showing RecyclerView")
            binding.emptyStateLayout.visibility = View.GONE
            binding.recyclerViewContacts.visibility = View.VISIBLE
        }

        Log.d(TAG, "Submitting list to adapter")
        guardianAdapter.submitList(guardians)
    }
}
