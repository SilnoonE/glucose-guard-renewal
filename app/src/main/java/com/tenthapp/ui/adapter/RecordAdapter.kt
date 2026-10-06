package com.example.glucoseguard.ui.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.glucoseguard.R
import com.example.glucoseguard.data.model.GlucoseRecord
import com.example.glucoseguard.data.model.InsulinRecord
import com.example.glucoseguard.data.model.MealRecord
import com.example.glucoseguard.databinding.ItemMemoBinding
import com.example.glucoseguard.databinding.ItemRecordBinding
import com.example.glucoseguard.util.CategoryMapper
import java.text.SimpleDateFormat
import java.util.*

class RecordAdapter(
    private val onEditClick: (Any) -> Unit,
    private val onDeleteClick: (Any) -> Unit,
    private val onItemClick: (Any) -> Unit = {}
) : ListAdapter<Any, RecyclerView.ViewHolder>(DiffCallback()) {

    companion object {
        private const val TYPE_STANDARD = 0
        private const val TYPE_MEMO = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is MealRecord -> TYPE_MEMO
            else -> TYPE_STANDARD
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_MEMO) {
            val binding = ItemMemoBinding.inflate(inflater, parent, false)
            MemoViewHolder(binding)
        } else {
            val binding = ItemRecordBinding.inflate(inflater, parent, false)
            StandardViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        if (holder is StandardViewHolder) {
            holder.bind(item)
        } else if (holder is MemoViewHolder && item is MealRecord) {
            holder.bind(item)
        }
    }

    inner class StandardViewHolder(private val binding: ItemRecordBinding) : RecyclerView.ViewHolder(binding.root) {
        private val dateTimeFormat = SimpleDateFormat("yyyy.MM.dd a hh:mm", Locale.getDefault())

        fun bind(item: Any) {
            val context = binding.root.context
            binding.btnEdit.setOnClickListener { onEditClick(item) }
            binding.btnDelete.setOnClickListener { onDeleteClick(item) }
            binding.root.setOnClickListener { onItemClick(item) }

            when (item) {
                is GlucoseRecord -> {
                    binding.tvCategory.text = CategoryMapper.getTranslatedCategory(context, item.category)
                    binding.tvTime.text = dateTimeFormat.format(Date(item.timestamp))
                    binding.tvValue.text = "${item.value} mg/dL"
                    
                    val status = com.example.glucoseguard.util.GlucosePolicy.classify(item.value, item.category, com.example.glucoseguard.util.TargetPreferences.read(context))
                    val (label, color) = when (status) {
                        com.example.glucoseguard.util.GlucosePolicy.Status.LOW -> R.string.status_low to R.color.status_low
                        com.example.glucoseguard.util.GlucosePolicy.Status.BELOW_TARGET -> R.string.below_target to R.color.status_warning
                        com.example.glucoseguard.util.GlucosePolicy.Status.ABOVE_TARGET -> R.string.above_target to R.color.status_high
                        else -> R.string.within_target to R.color.primary
                    }
                    binding.indicatorView.setBackgroundColor(ContextCompat.getColor(context, color))
                    binding.tvValue.setTextColor(ContextCompat.getColor(context, color))
                    binding.tvStatusTag.setText(label)
                    binding.tvStatusTag.setTextColor(ContextCompat.getColor(context, color))
                }
                is InsulinRecord -> {
                    binding.indicatorView.setBackgroundColor(ContextCompat.getColor(context, R.color.secondary))
                    binding.tvCategory.text = item.type
                    binding.tvTime.text = dateTimeFormat.format(Date(item.timestamp))
                    binding.tvValue.text = "${item.dosage} Unit"
                    binding.tvValue.setTextColor(ContextCompat.getColor(context, R.color.secondary))
                    binding.tvStatusTag.text = item.injectionSite
                    binding.tvStatusTag.setTextColor(ContextCompat.getColor(context, R.color.text_sub))
                }
            }
        }
    }

    inner class MemoViewHolder(private val binding: ItemMemoBinding) : RecyclerView.ViewHolder(binding.root) {
        private val timeFormat = SimpleDateFormat("yyyy.MM.dd a hh:mm", Locale.getDefault())

        fun bind(item: MealRecord) {
            binding.tvTime.text = timeFormat.format(Date(item.timestamp))
            binding.tvMemoContent.text = item.memo
            binding.tvMemoContent.setTextColor(android.graphics.Color.parseColor("#18312E")) // 진한 브라운 톤
            
            binding.root.setOnClickListener { onItemClick(item) }
            binding.btnEditMemo.setOnClickListener { onEditClick(item) }
            binding.btnDeleteMemo.setOnClickListener { onDeleteClick(item) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Any>() {
        override fun areItemsTheSame(oldItem: Any, newItem: Any) = when {
            oldItem is GlucoseRecord && newItem is GlucoseRecord -> oldItem.id == newItem.id
            oldItem is InsulinRecord && newItem is InsulinRecord -> oldItem.id == newItem.id
            oldItem is MealRecord && newItem is MealRecord -> oldItem.id == newItem.id
            else -> false
        }
        override fun areContentsTheSame(oldItem: Any, newItem: Any) = when {
            oldItem is GlucoseRecord && newItem is GlucoseRecord -> oldItem.id == newItem.id && oldItem.value == newItem.value && oldItem.timestamp == newItem.timestamp && oldItem.category == newItem.category && oldItem.memo == newItem.memo
            oldItem is InsulinRecord && newItem is InsulinRecord -> oldItem.id == newItem.id && oldItem.type == newItem.type && oldItem.dosage == newItem.dosage && oldItem.timestamp == newItem.timestamp && oldItem.injectionSite == newItem.injectionSite && oldItem.memo == newItem.memo
            oldItem is MealRecord && newItem is MealRecord -> oldItem.id == newItem.id && oldItem.timestamp == newItem.timestamp && oldItem.memo == newItem.memo
            else -> false
        }
    }
}
