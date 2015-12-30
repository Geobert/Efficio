package fr.geobert.efficio.data

import android.database.Cursor
import fr.geobert.efficio.adapter.TaskAdapter
import fr.geobert.efficio.db.TaskTable
import kotlin.properties.Delegates

class Task : Comparable<Task> {
    var id: Long by Delegates.notNull()
    var isDone: Boolean by Delegates.notNull()
    var item: Item by Delegates.notNull()
    var type: TaskAdapter.VIEW_TYPES = TaskAdapter.VIEW_TYPES.Normal

    constructor() {
        type = TaskAdapter.VIEW_TYPES.Header
        isDone = true
    }

    constructor(cursor: Cursor) {
        id = cursor.getLong(0)
        isDone = cursor.getInt(cursor.getColumnIndex(TaskTable.COL_IS_DONE)) == 1
        item = Item(cursor)
    }

    constructor(item: Item) {
        id = 0
        isDone = false
        this.item = item
    }


    override fun compareTo(other: Task): Int {
        return if (isDone != other.isDone) {
            if (isDone) 1 else -1
        } else {
            if (isDone) item.name.compareTo(other.item.name) else
                if (item.department.weight > other.item.department.weight) 1 else
                    if (item.department.weight < other.item.department.weight) -1 else
                        if (item.weight > other.item.weight) 1 else
                            if (item.weight < other.item.weight) -1 else
                                item.name.compareTo(other.item.name)
        }
    }
}