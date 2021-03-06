package fr.geobert.efficio.adapter

import android.content.Context
import android.content.SharedPreferences
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import fr.geobert.efficio.R
import fr.geobert.efficio.data.Task
import fr.geobert.efficio.db.TaskTable
import fr.geobert.efficio.extensions.normalize
import java.util.*


class TaskAdapter(list: MutableList<Task>, val listener: TaskViewHolder.TaskViewHolderListener,
                  private val prefs: SharedPreferences) :
        RecyclerView.Adapter<TaskViewHolder>() {
    private val taskList = LinkedList<Task>(list)
    val TAG = "TaskAdapter"

    enum class VIEW_TYPES {
        Header, Normal
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val l = LayoutInflater.from(parent.context).inflate(when (viewType) {
            VIEW_TYPES.Normal.ordinal ->
                if (prefs.getBoolean("invert_checkbox_pref", false)) R.layout.item_row_invert
                else R.layout.item_row
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

    private fun getTaskPosition(taskId: Long): Int {
        return taskList.indexOfFirst { taskId == it.id }
    }

    fun getTaskById(taskId: Long): Task? {
        return taskList.find { taskId == it.id }
    }

    fun refreshTaskFromDB(ctx: Context, taskId: Long) {
        val c = TaskTable.getTaskById(ctx, taskId)
        if (c != null && c.moveToFirst()) {
            val pos = getTaskPosition(taskId)
            taskList[pos] = Task(c)
            notifyItemChanged(pos)
        }
    }

    fun getTaskByName(name: String): Pair<Task?, Int> {
        return taskList
                .firstOrNull { it.type == VIEW_TYPES.Normal && it.item.normName() == name.normalize() }
                ?.let { Pair(it, taskList.indexOf(it)) }
                ?: Pair(null, 0)
    }

    private fun removeItem(pos: Int): Task {
        val d = taskList.removeAt(pos)
        notifyItemRemoved(pos)
        return d
    }

    private fun addItem(pos: Int, d: Task) {
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