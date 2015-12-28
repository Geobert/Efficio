package fr.geobert.efficio.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import fr.geobert.efficio.R
import fr.geobert.efficio.data.Department
import java.util.*

class DepartmentAdapter(list: MutableList<Department>,
                        val listener: DepartmentViewHolder.OnClickListener) :
        RecyclerView.Adapter<DepartmentViewHolder>() {
    val depLists = LinkedList<Department>(list)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DepartmentViewHolder {
        val l = LayoutInflater.from(parent.context).inflate(R.layout.department_row, parent, false)
        return DepartmentViewHolder(l, listener)
    }

    override fun onBindViewHolder(holder: DepartmentViewHolder, position: Int) {
        holder.bind(depLists[position])
    }

    override fun getItemCount(): Int {
        return depLists.count()
    }

    fun removeItem(pos: Int): Department {
        val d = depLists.removeAt(pos)
        notifyItemRemoved(pos)
        return d
    }

    fun addItem(pos: Int, d: Department) {
        depLists.add(pos, d)
        notifyItemInserted(pos)
    }

    fun moveItem(from: Int, to: Int) {
        val d = depLists.removeAt(from)
        depLists.add(to, d)
        notifyItemMoved(from, to)
    }

    fun animateTo(deps: MutableList<Department>) {
        applyAndAnimateRemovals(deps)
        applyAndAnimateAdditions(deps)
        applyAndAnimateMoves(deps)
    }

    private fun applyAndAnimateMoves(deps: MutableList<Department>) {
        for (to in (deps.size - 1) downTo 0) {
            val d = deps[to]
            val from = depLists.indexOf(d)
            if (from >= 0 && from != to) {
                moveItem(from, to)
            }
        }
    }

    private fun applyAndAnimateAdditions(deps: MutableList<Department>) {
        for (i in 0..(deps.size - 1)) {
            val d = deps[i]
            if (!depLists.contains(d)) {
                addItem(i, d)
            }
        }
    }

    private fun applyAndAnimateRemovals(deps: MutableList<Department>) {
        for (i in (depLists.size - 1) downTo 0) {
            val d = depLists[i]
            if (!deps.contains(d)) {
                removeItem(i)
            }
        }
    }


}