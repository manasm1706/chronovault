package com.example.chronovault.ui.map

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.example.chronovault.R
import com.example.chronovault.data.ServiceLocator
import com.example.chronovault.ui.capsules.CapsuleDetailsActivity
import com.example.chronovault.utils.LocationHelper
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * BottomSheet preview for capsule marker tap on the map
 */
class CapsulePreviewBottomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_ID = "capsule_id"
        private const val ARG_TITLE = "capsule_title"
        private const val ARG_STATUS = "capsule_status"

        fun newInstance(id: String, title: String, status: String): CapsulePreviewBottomSheet {
            return CapsulePreviewBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_ID, id)
                    putString(ARG_TITLE, title)
                    putString(ARG_STATUS, status)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_capsule_preview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val capsuleId = arguments?.getString(ARG_ID)?.takeIf { it.isNotBlank() } ?: run {
            dismissAllowingStateLoss()
            return
        }
        val title = arguments?.getString(ARG_TITLE) ?: ""
        val status = arguments?.getString(ARG_STATUS) ?: ""

        view.findViewById<TextView>(R.id.tv_preview_title).text = title
        view.findViewById<TextView>(R.id.tv_preview_status).text = status
        
        val btnViewDetails = view.findViewById<Button>(R.id.btn_view_details)
        
        // FIX: 5
        // Fetch capsule asynchronously to avoid blocking the main thread.
        val capsuleRepository = ServiceLocator.provideCapsuleRepository(requireContext().applicationContext)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val capsule = withContext(Dispatchers.IO) {
                    capsuleRepository.getCapsuleById(capsuleId)
                }

                if (!isAdded) return@launch

                if (capsule != null) {
                    if (capsule.isUnlocked) {
                        // Unlocked: show button to view details
                        btnViewDetails.isEnabled = true
                        btnViewDetails.text = "View Memory"
                        btnViewDetails.setOnClickListener {
                            if (!isAdded) return@setOnClickListener
                            runCatching {
                                val intent = Intent(requireContext(), CapsuleDetailsActivity::class.java).apply {
                                    putExtra("capsule_id", capsuleId)
                                }
                                startActivity(intent)
                                dismissAllowingStateLoss()
                            }.onFailure { throwable ->
                                android.util.Log.e("CapsulePreviewBottomSheet", "Failed to open capsule details", throwable)
                            }
                        }
                    } else {
                        // Locked: show lock message and disable button
                        btnViewDetails.isEnabled = false
                        val lockMessage = getLockedMessage(capsule)
                        btnViewDetails.text = "🔒 ${lockMessage.split("\n")[0]}"
                        view.findViewById<TextView>(R.id.tv_preview_status).text = lockMessage
                    }
                } else {
                    btnViewDetails.isEnabled = false
                    btnViewDetails.text = "Memory unavailable"
                }
            } catch (e: Exception) {
                // FIX: 14
                // Ignore normal lifecycle cancellation to avoid noisy StandaloneCoroutine logs.
                if (e is CancellationException) return@launch
                android.util.Log.e("CapsulePreviewBottomSheet", "Error loading capsule: ${e.message}")
                btnViewDetails.isEnabled = false
                btnViewDetails.text = "Error loading capsule"
            }
        }
    }

    private fun getLockedMessage(capsule: com.example.chronovault.data.local.entity.CapsuleEntity): String {
        return when {
            capsule.isTimeBased && capsule.unlockTime != null -> {
                val date = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(capsule.unlockTime))
                "⏰ Unlocks on $date"
            }
            capsule.isLocationBased -> "📍 Visit location to unlock"
                .plus("\n")
                .plus(
                    LocationHelper.getLocalityHint(requireContext(), capsule.latitude, capsule.longitude)
                        ?.let { getString(R.string.location_somewhere_in, it) }
                        ?: getString(R.string.location_unknown)
                )
            else -> "🔒 This memory is locked"
        }
    }
}

