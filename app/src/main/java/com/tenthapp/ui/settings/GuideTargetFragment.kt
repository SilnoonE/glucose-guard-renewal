package com.example.glucoseguard.ui.settings
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.glucoseguard.R
import com.example.glucoseguard.databinding.FragmentGuideTargetBinding
import com.example.glucoseguard.util.*
class GuideTargetFragment : Fragment() {
    private var _binding: FragmentGuideTargetBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGuideTargetBinding.inflate(inflater,container,false);return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val target = TargetPreferences.read(requireContext())
        binding.etMin.setText(target.min.toString());binding.etBeforeMax.setText(target.beforeMax.toString());binding.etAfterMax.setText(target.afterMax.toString())
        binding.btnSaveTarget.setOnClickListener {
            val min=binding.etMin.text.toString().toIntOrNull();val before=binding.etBeforeMax.text.toString().toIntOrNull();val after=binding.etAfterMax.text.toString().toIntOrNull()
            if(min == null || before == null || after == null || min !in 70..200 || before !in (min+1)..600 || after !in (min+1)..600) {
                binding.etMin.error=getString(R.string.target_invalid);return@setOnClickListener
            }
            TargetPreferences.save(requireContext(),GlucosePolicy.Target(min,before,after))
            Toast.makeText(requireContext(),R.string.target_saved,Toast.LENGTH_SHORT).show()
        }
    }
    override fun onDestroyView() { _binding = null;super.onDestroyView() }
}
