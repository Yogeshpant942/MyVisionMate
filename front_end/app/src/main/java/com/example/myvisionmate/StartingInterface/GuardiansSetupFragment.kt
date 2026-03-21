package com.example.visionmate.StartingInterface
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myvisionmate.ApiInterface
import com.example.myvisionmate.Factory.GuardianViewModelFactory
import com.example.myvisionmate.GuardianAdapter
import com.example.myvisionmate.Models.Guardian
import com.example.myvisionmate.R
import com.example.myvisionmate.Repositary.Repositary
import com.example.myvisionmate.RetrofitService
import com.example.myvisionmate.ViewModel.GuardianViewModel
import com.example.myvisionmate.databinding.FragmentGuardiansSetupBinding
import kotlinx.coroutines.launch
class GuardiansSetupFragment : Fragment() {
    lateinit var binding: FragmentGuardiansSetupBinding
    private lateinit var  GuardianAdapter : GuardianAdapter
    private val TAG = "EmergencyContactFragment"
    private val guardianNumber = mutableListOf<String>()

    lateinit var viewModel: GuardianViewModel
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentGuardiansSetupBinding.inflate(inflater,container,false)
        val api: ApiInterface = RetrofitService.api
        val repo = Repositary(api)
        val factory = GuardianViewModelFactory(repo)
        viewModel = ViewModelProvider(this,factory).get(GuardianViewModel::class)
        setUpUI()
        setUpRecyclerView()
        observeViewModel()
        loadGuardian()
        binding.btnNext.setOnClickListener {
            findNavController().navigate(R.id.action_guardiansSetupFragment_to_homeFragment)
        }


        return binding.root
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
                when(result){
                    is GuardianViewModel.GuardianResult.Success->{
                        Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                        if (result.message.contains("added")) {
                            binding.etGuardianName.text?.clear()
                            binding.etGuardianPhone.text?.clear()
                        }

                        viewModel.resetResult()
                    }
                    is GuardianViewModel.GuardianResult.Error -> {
                        Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                        viewModel.resetResult()
                    }
                    null->{}
                }
            }
        }
    }
    fun setUpRecyclerView(){
        GuardianAdapter = GuardianAdapter{guardian->
            val token = getAuthToken()
            if(token!=null){
                viewModel.deleteGuardian(guardian._id, token)
            }
            else {
                Toast.makeText(requireContext(), "Login required", Toast.LENGTH_SHORT).show()
            }

        }
        binding.rvGuardians.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = GuardianAdapter
            setHasFixedSize(true)
        }
    }

    private fun setUpUI() {
        binding.btnAddGuardian.setOnClickListener {
            val name = binding.etGuardianName.text.toString().trim()
            val phone = binding.etGuardianPhone.text.toString().trim()
            guardianNumber.add(phone)
            val no_prefs = requireContext().getSharedPreferences("visionmate", Context.MODE_PRIVATE)
            no_prefs.edit().putStringSet("guardian_no",guardianNumber.toSet()).apply()
            Log.d(TAG,"Guardian saved: $phone")

            val token = getAuthToken()
            if(token!=null) {
                viewModel.addGuardian(token, name, phone) }
            else{
                Toast.makeText(context, "Please login first", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun getAuthToken():String?{
        return requireContext().getSharedPreferences("app_prefs",0).getString("auth_token",null)
    }
    private fun loadGuardian(){
        val token = getAuthToken()
        Log.d(TAG, "loadGuardian() called, token = $token")

        if(token!=null){
            viewModel.loadGuardians(token)
        }
        else{
            Toast.makeText(context, "Please login first", Toast.LENGTH_SHORT).show()
        }
    }
    private fun updateUi(guardians: List<Guardian>){
        if(guardians.isEmpty()){
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.rvGuardians.visibility = View.GONE
        }
        else{
            binding.tvEmptyState.visibility = View.GONE
            binding.rvGuardians.visibility = View.VISIBLE
        }
        GuardianAdapter.submitList(guardians)
        Log.d(TAG, "RV visibility=${binding.rvGuardians.visibility}, itemCount=${GuardianAdapter.itemCount}")
    }
}