package com.example.filtergallery.recycler

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.filtergallery.CustomFilter
import com.example.filtergallery.FilterGalleryActivity.Companion.CUSTOM_FILTER_IMAGES_PATH
import com.example.filtergallery.FilterGalleryActivity.Companion.DEFAULT_SAVE_FILTER_VIEW_EXP
import com.example.filtergallery.R


class FilterGalleryAdapter(
    private val items: List<CustomFilter>,
    private val onFilterClickListenerCallback: (Int) -> Unit
) : RecyclerView.Adapter<FilterViewHolder>() {

    fun getItemCustomFilter(position: Int) = items.getOrNull(position)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_custom_filter_view, parent, false)
        return FilterViewHolder(
            itemView = itemView,
            onFilterClickCallback = onFilterClickListenerCallback
        )
    }

    override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
        holder.bind(position, items[position])
    }

    override fun getItemCount(): Int = items.size
}

class FilterViewHolder(
    itemView: View,
    private val onFilterClickCallback: (Int) -> Unit
) : RecyclerView.ViewHolder(itemView) {

    private val customGlSurfaceView: ImageView = itemView.findViewById(R.id.item_custom_filter_view)

    fun bind(position: Int, customFilter: CustomFilter) {
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val path = "${CUSTOM_FILTER_IMAGES_PATH.absolutePath}/${customFilter.nameOfFilter}$DEFAULT_SAVE_FILTER_VIEW_EXP"
        val bitmap = BitmapFactory.decodeFile(path, options)
        customGlSurfaceView.setImageURI(null)
        customGlSurfaceView.setImageBitmap(bitmap)
        customGlSurfaceView.setOnClickListener {
            onFilterClickCallback.invoke(position)
        }
    }
}