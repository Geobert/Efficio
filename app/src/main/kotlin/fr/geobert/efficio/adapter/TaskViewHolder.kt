package fr.geobert.efficio.adapter

import android.support.v4.content.ContextCompat
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.TextView
import fr.geobert.efficio.R
import fr.geobert.efficio.data.Task
import kotlin.properties.Delegates

class TaskViewHolder(val view: View, isHeader: Boolean, val listener: OnDoneStateChangeListener) :
        RecyclerView.ViewHolder(view), CompoundButton.OnCheckedChangeListener {
    interface OnDoneStateChangeListener {
        fun onDoneStateChanged(task: Task)
    }

    var name: TextView by Delegates.notNull()
    var checkbox: CheckBox by Delegates.notNull()
    var layout: CardView by Delegates.notNull()
    var content: View by Delegates.notNull()
    var task: Task by Delegates.notNull()
    private var isListenerActive: Boolean = true

    init {
        if (!isHeader) {
            name = view.findViewById(R.id.name) as TextView
            checkbox = view.findViewById(R.id.checkbox) as CheckBox
            layout = view.findViewById(R.id.item_row_layout) as CardView
            content = view.findViewById(R.id.card_content)
            checkbox.setOnCheckedChangeListener(this)
        }
    }

    override fun onCheckedChanged(p0: CompoundButton?, isChecked: Boolean) {
        if (isListenerActive) {
            task.isDone = isChecked
            listener.onDoneStateChanged(task)
            layout.isSelected = isChecked
            setBgColor(isChecked)
        }
    }

    fun setBgColor(isChecked: Boolean) {
        layout.setCardBackgroundColor(ContextCompat.getColor(view.context,
                if (isChecked) R.color.colorDivider else android.R.color.white))
    }

    fun bind(task: Task) {
        synchronized(this) {
            this.task = task
            if (task.type == TaskAdapter.VIEW_TYPES.Normal) {
                isListenerActive = false
                checkbox.isChecked = task.isDone
                name.text = task.item.name
                layout.isSelected = task.isDone
                setBgColor(task.isDone)
                isListenerActive = true
            }
        }
    }
}