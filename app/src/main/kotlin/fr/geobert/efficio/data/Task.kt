package fr.geobert.efficio.data

import android.database.Cursor
import fr.geobert.efficio.adapter.TaskAdapter
import fr.geobert.efficio.db.TaskTable
import kotlin.properties.Delegates

class Task : Comparable<Task> {
    var id: Long by Delegates.notNull()
    var isDone: Boolean by Delegates.notNull()
    var item: Item by Delegates.notNull()
    var type: TaskAdapter.VIEW_TYPES by Delegates.notNull()

    constructor() {
        type = TaskAdapter.VIEW_TYPES.Header
        isDone = true
        id = 0
        item = Item()
    }

    constructor(cursor: Cursor) {
        id = cursor.getLong(0)
        isDone = cursor.getInt(cursor.getColumnIndex(TaskTable.COL_IS_DONE)) == 1
        item = Item(cursor)
        type = TaskAdapter.VIEW_TYPES.Normal
    }

    constructor(item: Item) {
        id = 0
        isDone = false
        this.item = item
        type = TaskAdapter.VIEW_TYPES.Normal
    }

    constructor(task: Task) {
        id = task.id
        isDone = task.isDone
        this.item = Item(task.item)
        type = task.type
    }

    // used for sort()
    override fun compareTo(other: Task): Int {
        return if (isDone != other.isDone) {
            if (isDone) 1 else -1
        } else {
            if (isDone) item.name.compareTo(other.item.name) else
                if (item.department.weight > other.item.department.weight) 1 else
                    if (item.department.weight < other.item.department.weight) -1 else {
                        val r = item.department.name.compareTo(other.item.department.name)
                        if (r != 0) r else
                            if (item.weight > other.item.weight) 1 else
                                if (item.weight < other.item.weight) -1 else
                                    item.name.compareTo(other.item.name)
                    }
        }
    }

    fun isEquals(other: Task): Boolean {
        return isDone == other.isDone &&
                type == other.type &&
                item.isEquals(other.item)
    }
}