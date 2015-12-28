package fr.geobert.efficio.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import fr.geobert.efficio.R
import fr.geobert.efficio.data.Task
import java.util.*

class TaskAdapter(list: MutableList<Task>) : RecyclerView.Adapter<TaskViewHolder>() {
    val taskList = LinkedList<Task>(list)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val l = LayoutInflater.from(parent.context).inflate(R.layout.item_row, parent, false)
        return TaskViewHolder(l)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val t = taskList[position]
        holder.checkbox.isChecked = t.isDone
        holder.name.text = t.item.name
    }

    override fun getItemCount(): Int {
        return taskList.count()
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
}