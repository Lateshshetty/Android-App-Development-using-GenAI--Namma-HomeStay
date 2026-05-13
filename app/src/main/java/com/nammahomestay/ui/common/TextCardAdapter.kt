package com.nammahomestay.ui.common

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.nammahomestay.databinding.ItemTextCardBinding

data class TextCardRow(val text: String, val imageUrl: String = "")

/**
 * RecyclerView adapter for all listing screens (Browse, Inquiry, Menu, Spots).
 * Binds to item_text_card.xml — IDs [text] and [image] must stay stable.
 *
 * Logic unchanged; image_divider visibility wired to match image visibility.
 */
class TextCardAdapter(
    private var rows: List<TextCardRow> = emptyList(),
    private val click: (Int) -> Unit = {}
) : RecyclerView.Adapter<TextCardAdapter.VH>() {

    class VH(val binding: ItemTextCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemTextCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = rows.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val row = rows[position]
        holder.binding.text.text = row.text

        if (row.imageUrl.isBlank()) {
            holder.binding.image.visibility = View.GONE
            // hide warm divider too
            holder.binding.root.findViewById<View>(
                com.nammahomestay.R.id.image_divider
            )?.visibility = View.GONE
            Glide.with(holder.binding.image).clear(holder.binding.image)
        } else {
            holder.binding.image.visibility = View.VISIBLE
            // show warm divider between image and text
            holder.binding.root.findViewById<View>(
                com.nammahomestay.R.id.image_divider
            )?.visibility = View.VISIBLE
            Glide.with(holder.binding.image)
                .load(row.imageUrl)
                .centerCrop()
                .into(holder.binding.image)
        }

        holder.itemView.setOnClickListener { click(position) }
    }

    /** Submit a plain string list (inquiry box usage). */
    fun submit(next: List<String>) {
        rows = next.map { TextCardRow(it) }
        notifyDataSetChanged()
    }

    /** Submit rich rows with optional image URLs (browse / menu usage). */
    fun submitRows(next: List<TextCardRow>) {
        rows = next
        notifyDataSetChanged()
    }
}
