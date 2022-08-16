package com.example.filtergallery.recycler

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class CustomItemDecorator : RecyclerView.ItemDecoration() {

    companion object {
        private const val MARGIN = 10
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)
        outRect.right = MARGIN
        outRect.left = MARGIN
    }
}