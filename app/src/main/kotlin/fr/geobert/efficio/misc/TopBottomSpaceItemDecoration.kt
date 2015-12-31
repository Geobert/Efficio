package fr.geobert.efficio.misc

import android.graphics.Rect
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View

class TopBottomSpaceItemDecoration constructor(private val space: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State?) {
        super.getItemOffsets(outRect, view, parent, state)
        if (space <= 0) {
            return
        }

        if (parent.getChildLayoutPosition(view) < 1) {
            if (getOrientation(parent) == LinearLayoutManager.VERTICAL) {
                outRect.top = space
            } else {
                outRect.left = space
            }
        }

        if (parent.getChildAdapterPosition(view) == getTotalItemCount(parent) - 1) {
            if (getOrientation(parent) == LinearLayoutManager.VERTICAL) {
                outRect.bottom = space
            } else {
                outRect.right = space
            }
        }
    }

    private fun getTotalItemCount(parent: RecyclerView): Int {
        return parent.adapter.itemCount
    }

    private fun getOrientation(parent: RecyclerView): Int {
        if (parent.layoutManager is LinearLayoutManager) {
            return (parent.layoutManager as LinearLayoutManager).orientation
        } else {
            throw IllegalStateException("SpaceItemDecoration can only be used with a LinearLayoutManager.")
        }
    }
}