package fr.geobert.efficio.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import fr.geobert.efficio.R
import fr.geobert.efficio.data.Task
import java.util.*

class TaskAdapter(list: MutableList<Task>, val listener: TaskViewHolder.OnDoneStateChangeListener) :
        RecyclerView.Adapter<TaskViewHolder>() {
    val taskList = LinkedList<Task>(list)

    enum class VIEW_TYPES {
        Header, Normal
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val l = LayoutInflater.from(parent.context).inflate(when (viewType) {
            VIEW_TYPES.Normal.ordinal -> R.layout.item_row
            VIEW_TYPES.Header.ordinal -> R.layout.task_list_header
            else -> 0
        }, parent, false)
        return TaskViewHolder(l, when (viewType) {
            VIEW_TYPES.Normal.ordinal -> false
            else -> true
        }, listener)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(taskList[position])
    }

    override fun getItemCount(): Int {
        return taskList.count()
    }

    override fun getItemViewType(position: Int): Int {
        return taskList[position].type.ordinal
    }

    fun removeItem(pos: Int): Task {
        val d = taskList.removeAt(pos)
        notifyItemRemoved(pos)
        return d
    }

    fun addItem(pos: Int, d: Task) {
        taskList.add(pos, d)
        notifyItemInserted(pos)
    }

    fun moveItem(from: Int, to: Int) {
        val d = taskList.removeAt(from)
        taskList.add(to, d)
        notifyItemMoved(from, to)
    }

    fun animateTo(newList: MutableList<Task>) {
        applyAndAnimateRemovals(newList)
        applyAndAnimateAdditions(newList)
        applyAndAnimateMoves(newList)
    }

    private fun applyAndAnimateMoves(newList: MutableList<Task>) {
        for (to in (newList.size - 1) downTo 0) {
            val d = newList[to]
            val from = taskList.indexOf(d)
            if (from >= 0 && from != to) {
                moveItem(from, to)
            }
        }
    }

    private fun applyAndAnimateAdditions(newList: MutableList<Task>) {
        for (i in 0..(newList.size - 1)) {
            val d = newList[i]
            if (!taskList.contains(d)) {
                addItem(i, d)
            }
        }
    }

    private fun applyAndAnimateRemovals(newList: MutableList<Task>) {
        for (i in (taskList.size - 1) downTo 0) {
            val d = taskList[i]
            if (!newList.contains(d)) {
                removeItem(i)
            }
        }
    }
}