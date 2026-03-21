package com.example.myvisionmate.MainInterface

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myvisionmate.ApiInterface
import com.example.myvisionmate.Factory.GuardianViewModelFactory
import com.example.myvisionmate.GuardianAdapter
import com.example.myvisionmate.Models.Guardian
import com.example.myvisionmate.R
import com.example.myvisionmate.Repositary.Repositary
import com.example.myvisionmate.RetrofitService
import com.example.myvisionmate.ViewModel.GuardianViewModel
import com.example.myvisionmate.databinding.FragmentEmergencyContactBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class EmergencyContactFragment : Fragment() {

    private lateinit var binding: FragmentEmergencyContactBinding
    private lateinit var viewModel: GuardianViewModel
    private lateinit var guardianAdapter: GuardianAdapter
    lateinit var pref: SharedPreferences

    private val TAG = "EmergencyContactFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        Log.d(TAG, "onCreateView() called")

        binding = FragmentEmergencyContactBinding.inflate(inflater, container, false)

        pref = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        Log.d(TAG, "SharedPreferences initialized")

        val api: ApiInterface = RetrofitService.api
        Log.d(TAG, "Retrofit API instance created")

        val repo = Repositary(api)
        val factory = GuardianViewModelFactory(repo)

        viewModel = ViewModelProvider(this, factory)
            .get(GuardianViewModel::class.java)

        Log.d(TAG, "GuardianViewModel initialized")

        setupRecyclerView()
        observeViewModel()
        loadGuardian()

        return binding.root
    }

    private fun setupRecyclerView() {
        Log.d(TAG, "setupRecyclerView() called")

        guardianAdapter = GuardianAdapter { guardian ->
            Log.d("GuardianClick", "Adapter callback reached")
            Log.d(
                "GuardianClick",
                "name=${guardian.name}, id=${guardian._id}"
            )
            showGuardianItemDialog(guardian)
        }

        binding.recyclerViewContacts.apply {
            Log.d(TAG, "RecyclerView configuration started")
            layoutManager = LinearLayoutManager(requireContext())
            adapter = guardianAdapter
            setHasFixedSize(true)
        }

        Log.d(TAG, "RecyclerView configuration finished")
    }

    fun showGuardianItemDialog(guardian: Guardian) {
        Log.d(TAG, "showGuardianItemDialog() called")
        Log.d(TAG, "Guardian received -> name=${guardian.name}, id=${guardian._id}")

        val dialogView = layoutInflater.inflate(
            R.layout.dialog_emergency_action,
            null
        )

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .show()

        Log.d(TAG, "Dialog shown")

        val delButton: Button = dialogView.findViewById(R.id.btnDelete)
        val callButton: Button = dialogView.findViewById(R.id.btnCall)
        val editButton: Button = dialogView.findViewById(R.id.btnEdit)

        val token: String? = pref.getString("auth_token", "")
        Log.d(TAG, "Auth token fetched: ${token != null}")

        delButton.setOnClickListener {
            Log.d("GuardianDelete", "Delete button clicked")
            Log.d(
                "GuardianDelete",
                "Deleting guardian -> name=${guardian.name}, id=${guardian._id}"
            )

            viewModel.deleteGuardian(guardian._id, token)

            val newList = guardianAdapter.currentList.toMutableList()
            newList.remove(guardian)

            Log.d("GuardianDelete", "New list size after delete = ${newList.size}")

            guardianAdapter.submitList(newList)
            dialog.dismiss()
        }

        callButton.setOnClickListener {
            Log.d(TAG, "Call button clicked for ${guardian.phone}")

            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${guardian.phone}")
            }
            startActivity(intent)
            dialog.dismiss()
        }

        editButton.setOnClickListener {
            Log.d(TAG, "Edit button clicked (TODO)")
            dialog.dismiss()
        }
    }

    private fun observeViewModel() {
        Log.d(TAG, "observeViewModel() started")

        lifecycleScope.launch {
            Log.d(TAG, "Collecting guardian list")
            viewModel.gaurdian.collect { guardians ->
                Log.d(TAG, "Collected guardians list, size = ${guardians.size}")
                updateUi(guardians)
            }
        }

        lifecycleScope.launch {
            Log.d(TAG, "Collecting guardianResult")
            viewModel.guardianResult.collect { result ->
                when (result) {
                    is GuardianViewModel.GuardianResult.Success -> {
                        Log.d(TAG, "Guardian operation success")
                        viewModel.resetResult()
                    }

                    is GuardianViewModel.GuardianResult.Error -> {
                        Log.e(TAG, "Guardian operation error: ${result.message}")
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
            Log.d(TAG, "Calling loadGuardians()")
            viewModel.loadGuardians(token)
        } else {
            Log.w(TAG, "Token is null, user not logged in")
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