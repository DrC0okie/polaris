package ch.heig.iict.polaris_health.ui.tour

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ch.heig.iict.polaris_health.R
import ch.heig.iict.polaris_health.databinding.ItemVisitBinding
import ch.heig.iict.polaris_health.domain.model.VisitDetails

class TourAdapter(
    private val onItemClicked: (VisitDetails, View) -> Unit
) : ListAdapter<VisitDetails, TourAdapter.VisitViewHolder>(VisitDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VisitViewHolder {
        val binding = ItemVisitBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VisitViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VisitViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VisitViewHolder(private val binding: ItemVisitBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val visit = getItem(position)
                    if (!visit.isLocked) {
                        onItemClicked(visit, binding.card)
                    }
                }
            }
        }

        fun bind(visit: VisitDetails) {
            binding.patientName.text = visit.patientFullName
            binding.visitStatus.text = "Beacon ID: ${visit.associatedBeaconId ?: "N/A"}" // Placeholder
            binding.card.transitionName = "visit_container_${visit.visitId}"

            if (visit.isLocked) {
                binding.lockIcon.setImageResource(R.drawable.round_lock_24)
                binding.root.alpha = 0.7f
            } else {
                binding.lockIcon.setImageResource(R.drawable.round_lock_open_24)
                binding.root.alpha = 1.0f
            }
        }
    }
}

object VisitDiffCallback : DiffUtil.ItemCallback<VisitDetails>() {
    override fun areItemsTheSame(oldItem: VisitDetails, newItem: VisitDetails): Boolean {
        return oldItem.visitId == newItem.visitId
    }

    override fun areContentsTheSame(oldItem: VisitDetails, newItem: VisitDetails): Boolean {
        return oldItem == newItem
    }
}