package com.example.myvisionmate

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myvisionmate.Models.Guardian
import com.example.myvisionmate.databinding.ItemEmergencyContactBinding
class GuardianAdapter(
    private val onDeleteClick:(Guardian)->Unit
): ListAdapter<Guardian, GuardianAdapter.GuardianViewHolder>(GuardianDiffCallBack()){
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): GuardianAdapter.GuardianViewHolder {
        val binding = ItemEmergencyContactBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return GuardianViewHolder(binding)
    }
    override fun onBindViewHolder(
        holder: GuardianAdapter.GuardianViewHolder,
        position: Int
    ) {
        Log.d("GuardianAdapter", "Binding item at position $position")

        holder.bind(getItem(position))
    }
    inner class GuardianViewHolder(private val binding: ItemEmergencyContactBinding): RecyclerView.ViewHolder(binding.root){
        fun bind(guardian: Guardian){
            Log.d("GuardianAdapter", "Binding guardian: ${guardian.name}")
            binding.apply {
                tvGuardianName.text = guardian.name
                tvGuardianPhone.text = guardian.phone
            root.contentDescription =
                "Guardian ${guardian.name}, phone ${guardian.phone}. Hold button to remove contact."
                binding.root.setOnLongClickListener{
                    val pos = bindingAdapterPosition
                    if(pos!= RecyclerView.NO_POSITION){
                    onDeleteClick(getItem(pos))
                    }
                    true
                }
            }
        }
    }
    }
class GuardianDiffCallBack : DiffUtil.ItemCallback<Guardian>() {
    override fun areItemsTheSame(oldItem: Guardian, newItem: Guardian): Boolean {
        return oldItem._id == newItem._id
    }

    override fun areContentsTheSame(oldItem: Guardian, newItem: Guardian): Boolean {
        return oldItem == newItem
    }

}