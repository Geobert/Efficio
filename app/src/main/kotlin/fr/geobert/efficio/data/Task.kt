package fr.geobert.efficio.data

import android.database.Cursor
import fr.geobert.efficio.db.TaskTable
import kotlin.properties.Delegates


class Task {
    constructor(cursor: Cursor) {
        id = cursor.getLong(0)
        isDone = cursor.getInt(cursor.getColumnIndex(TaskTable.COL_IS_DONE)) == 1
        // todo        item = Item(cursor)
    }

    constructor(itemName: String, depName: String) {
        id = 0
        isDone = false
        item = Item(itemName, depName)
    }

    var id: Long by Delegates.notNull()
    var isDone: Boolean by Delegates.notNull()
    var item: Item by Delegates.notNull()
}