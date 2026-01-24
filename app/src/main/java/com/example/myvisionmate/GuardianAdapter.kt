package com.example.myvisionmate

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.view.menu.MenuView
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
        holder.bind(getItem(position))
    }
    inner class GuardianViewHolder(private val binding: ItemEmergencyContactBinding): RecyclerView.ViewHolder(binding.root){
        fun bind(guardian: Guardian){
            binding.apply {
                tvGuardianName.text = guardian.name
                tvGuardianPhone.text = guardian.phone
            root.contentDescription =
                "Guardian ${guardian.name}, phone ${guardian.phone}. Hold button to remove contact."
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