package fr.geobert.efficio.data

import android.database.Cursor
import android.os.Bundle
import fr.geobert.efficio.db.TaskTable
import fr.geobert.efficio.extensions.normalize
import kotlin.properties.Delegates

class Item {
    var id: Long by Delegates.notNull()
    var name: String by Delegates.notNull()
    fun normName(): String = name.normalize()
    var weight: Double by Delegates.notNull()
    var department: Department by Delegates.notNull()

    constructor(cursor: Cursor) {
        // from TaskTable query
        id = cursor.getLong(cursor.getColumnIndex(TaskTable.COL_ITEM_ID))
        weight = cursor.getDouble(cursor.getColumnIndex("item_weight"))
        val depIdColIdx = cursor.getColumnIndex("dep_id")
        val depId = cursor.getLong(depIdColIdx)
        val dep = departmentsList[depId]
        if (dep == null) {
            val b = Bundle()
            b.putInt("id", depIdColIdx)
            b.putInt("name", cursor.getColumnIndex("dep_name"))
            b.putInt("weight", cursor.getColumnIndex("dep_weight"))
            department = Department(cursor, b)
            departmentsList[depId] = department
        } else {
            department = dep
        }

        name = cursor.getString(cursor.getColumnIndex("item_name"))
    }

    constructor(name: String, dep: Department, w: Double) {
        id = 0
        this.name = name
        weight = w
        department = dep
    }

    constructor() {
        id = 0
        this.name = ""
        weight = 0.0
        department = Department()
    }

    constructor(item: Item) {
        id = item.id
        name = item.name
        weight = item.weight
        department = Department(item.department)
    }

    fun isEquals(item: Item): Boolean {
        return weight == item.weight && name == item.name &&
                department.isEquals(item.department)
    }

    companion object {
        val departmentsList: MutableMap<Long, Department> = mutableMapOf()
    }
}