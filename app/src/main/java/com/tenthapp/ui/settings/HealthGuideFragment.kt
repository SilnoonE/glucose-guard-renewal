package com.example.glucoseguard.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.glucoseguard.R
import com.example.glucoseguard.databinding.FragmentHealthGuideBinding
import com.example.glucoseguard.databinding.ItemGuideCardBinding

class HealthGuideFragment : Fragment() {
    private var _binding: FragmentHealthGuideBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHealthGuideBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCards()
    }

    private fun setupCards() {
        // 1. Management
        setupGuideCard(
            binding.cardManage,
            R.string.guide_manage,
            R.string.guide_m_desc,
            R.string.guide_m_keywords_full,
            R.drawable.ic_assignment,
            R.color.primary,
            5
        ) {
            findNavController().navigate(R.id.action_healthGuide_to_manage)
        }

        // 2. Exercise (Target guide is replaced by Exercise recommendation as requested in the 5 main list)
        setupGuideCard(
            binding.cardExercise,
            R.string.guide_e_title,
            R.string.guide_e_desc,
            R.string.guide_e_keywords_full,
            R.drawable.ic_fitness_center,
            R.color.secondary,
            7
        ) {
            findNavController().navigate(R.id.action_healthGuide_to_exercise)
        }

        // 3. High
        setupGuideCard(
            binding.cardHigh,
            R.string.guide_h_title,
            R.string.guide_h_desc,
            R.string.guide_h_keywords_full,
            R.drawable.ic_warning,
            R.color.status_high,
            6
        ) {
            findNavController().navigate(R.id.action_healthGuide_to_high)
        }

        // 4. Low
        setupGuideCard(
            binding.cardLow,
            R.string.guide_l_title,
            R.string.guide_l_desc,
            R.string.guide_l_keywords_full,
            R.drawable.ic_emergency,
            R.color.status_warning,
            8
        ) {
            findNavController().navigate(R.id.action_healthGuide_to_low)
        }

        // 5. Food
        setupGuideCard(
            binding.cardFood,
            R.string.guide_f_title,
            R.string.guide_f_desc,
            R.string.guide_f_keywords_full,
            R.drawable.ic_restaurant,
            R.color.primary,
            10
        ) {
            findNavController().navigate(R.id.action_healthGuide_to_food)
        }
    }

    private fun setupGuideCard(
        itemBinding: ItemGuideCardBinding,
        titleRes: Int,
        descRes: Int,
        keywordRes: Int,
        iconRes: Int,
        colorRes: Int,
        readTime: Int,
        onClick: () -> Unit
    ) {
        itemBinding.tvGuideTitle.setText(titleRes)
        itemBinding.tvGuideDesc.setText(descRes)
        itemBinding.tvGuideKeywords.text = "${getString(R.string.guide_keywords_label)} ${getString(keywordRes)}"
        itemBinding.ivGuideIcon.setImageResource(iconRes)
        itemBinding.iconBg.setCardBackgroundColor(ContextCompat.getColor(requireContext(), colorRes))
        itemBinding.tvGuideReadTime.text = getString(R.string.guide_read_time_fmt, readTime)
        itemBinding.root.setOnClickListener { onClick() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
