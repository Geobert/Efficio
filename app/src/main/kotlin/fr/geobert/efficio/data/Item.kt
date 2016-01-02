package fr.geobert.efficio.data

import android.database.Cursor
import android.os.Bundle
import fr.geobert.efficio.db.TaskTable
import fr.geobert.efficio.misc.normalize
import kotlin.properties.Delegates

class Item {
    var id: Long by Delegates.notNull()
    var name: String by Delegates.notNull()
    fun normName(): String = name.normalize()
    var weight: Int by Delegates.notNull()
    var department: Department by Delegates.notNull()

    constructor(cursor: Cursor) {
        // from TaskTable query
        id = cursor.getLong(cursor.getColumnIndex(TaskTable.COL_ITEM_ID))
        weight = cursor.getInt(cursor.getColumnIndex("item_weight"))
        val b = Bundle()
        b.putInt("id", cursor.getColumnIndex("dep_id"))
        b.putInt("name", cursor.getColumnIndex("dep_name"))
        b.putInt("weight", cursor.getColumnIndex("dep_weight"))
        department = Department(cursor, b)
        name = cursor.getString(cursor.getColumnIndex("item_name"))
    }

    constructor(name: String, dep: Department) {
        id = 0
        this.name = name
        weight = 0
        department = dep
    }

    constructor() {
        id = 0
        this.name = ""
        weight = 0
        department = Department()
    }

    constructor(item: Item) {
        id = item.id
        name = item.name
        weight = item.weight
        department = Department(item.department)
    }

    fun isEquals(item: Item): Boolean {
        return weight == item.weight && name.equals(item.name) &&
                department.isEquals(item.department)
    }
}