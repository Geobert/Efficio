package fr.geobert.efficio

import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import com.crashlytics.android.Crashlytics
import fr.geobert.efficio.adapter.DepartmentViewHolder
import fr.geobert.efficio.data.*
import fr.geobert.efficio.db.StoreCompositionTable
import java.util.*

class DepartmentDragHelper(val activity: EditDepartmentsActivity,
                           val depManager: DepartmentManager) :
        ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
    override fun onMove(recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder,
                        target: RecyclerView.ViewHolder): Boolean {
        Collections.swap(depManager.departmentsList, viewHolder.adapterPosition,
                target.adapterPosition)
        depManager.depAdapter.notifyItemMoved(viewHolder.adapterPosition,
                target.adapterPosition)
        return true
    }

    private var orig: Float = 0f
    private var lastDragTask: DepartmentViewHolder? = null

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)
        // end of drag n drop, adapter is correctly ordered but not our representation here
        val vh = viewHolder as DepartmentViewHolder?
        if (vh == null) {
            val last = lastDragTask
            if (last != null) {
                last.cardView.cardElevation = orig
                updateDepWeight(last)
                StoreCompositionTable.updateDepWeight(activity, last.dep!!)
            }
        } else {
            orig = vh.cardView.cardElevation
            vh.cardView.cardElevation = 20.0f
        }
        lastDragTask = vh
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder?, direction: Int) {
        // nothing
    }

    private fun updateDepWeight(dragged: DepartmentViewHolder) {
        val pos = dragged.adapterPosition
        val dep = dragged.dep!!
        if (depManager.nbDepartment() > 1)
            if (pos == 0) { // first
                val next = depManager.getDepartment(pos + 1)
                if (dep.weight >= next.weight)
                    dep.weight = next.weight - 1.0
            } else if (pos == (depManager.nbDepartment() - 1)) { // last
                val prev = depManager.getDepartment(pos - 1)
                if (dep.weight <= prev.weight)
                    dep.weight = prev.weight + 1.0
            } else { // between
                val next = depManager.getDepartment(pos + 1)
                val prev = depManager.getDepartment(pos - 1)
                if (dep.weight <= prev.weight || dep.weight >= next.weight) {
                    dep.weight = (prev.weight + next.weight) / 2.0
                    if (dep.weight <= prev.weight || dep.weight >= next.weight)
                        handleDoubleCollision(pos, dep, next, prev)
                }

            }
    }

    private fun handleDoubleCollision(pos: Int, dep: Department, next: Department, prev: Department) {
        if (!BuildConfig.DEBUG)
            Crashlytics.log("double collision occurred for Department!!! handleDoubleCollision")
    }
}