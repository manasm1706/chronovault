package com.example.chronovault.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chronovault.databinding.FragmentMyCapsuleCommentsBinding

class MyCapsuleCommentsFragment : Fragment() {

    private var _binding: FragmentMyCapsuleCommentsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MyCapsuleCommentsViewModel by viewModels()
    private val adapter = MyCapsuleCommentsAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyCapsuleCommentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvMyCapsuleComments.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMyCapsuleComments.adapter = adapter

        viewModel.comments.observe(viewLifecycleOwner) { comments ->
            adapter.submitList(comments)
            binding.tvEmptyComments.visibility = if (comments.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

