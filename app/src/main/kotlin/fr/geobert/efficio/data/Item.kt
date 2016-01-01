package fr.geobert.efficio.data

import android.database.Cursor
import android.os.Bundle
import fr.geobert.efficio.db.TaskTable
import fr.geobert.efficio.misc.normalize

class Item : ImplParcelable {
    override val parcels = hashMapOf<String, Any?>()
    var id: Long by parcels
    var name: String by parcels
    fun normName(): String = name.normalize()
    var weight: Int by parcels
    var department: Department by parcels

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

    override fun getClassLoaderOf(name: String): ClassLoader? {
        return when (name) {
            "department" -> department.javaClass.classLoader
            else -> null
        }
    }

    fun isEquals(item: Item): Boolean {
        return weight == item.weight && name.equals(item.name) &&
                department.isEquals(item.department)
    }
}