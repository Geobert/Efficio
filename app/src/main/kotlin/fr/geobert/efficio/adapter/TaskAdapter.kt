package fr.geobert.efficio.adapter

import android.database.Cursor
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import fr.geobert.efficio.R
import fr.geobert.efficio.data.Task
import fr.geobert.efficio.misc.map

class TaskAdapter(cursor: Cursor) : RecyclerView.Adapter<TaskRowHolder>() {
    val taskList = cursor.map { Task(it) }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskRowHolder {
        val l = LayoutInflater.from(parent.context).inflate(R.layout.item_row, parent, false)
        return TaskRowHolder(l)
    }

    override fun onBindViewHolder(holder: TaskRowHolder, position: Int) {
        val t = taskList[position]
        holder.checkbox.isChecked = t.isDone
        holder.name.text = t.item.name
    }

    override fun getItemCount(): Int {
        return taskList.count()
    }
}