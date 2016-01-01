package fr.geobert.efficio.data

import android.database.Cursor
import android.os.Bundle

class Department : Comparable<Department>, ImplParcelable {
    override val parcels = hashMapOf<String, Any?>()

    var id: Long by parcels
    var name: String by parcels
    var weight: Int by parcels

    constructor(cursor: Cursor, b: Bundle) {
        id = cursor.getLong(b.getInt("id"))
        name = cursor.getString(b.getInt("name"))
        val idx = b.getInt("weight")
        weight = if (idx > 0) cursor.getInt(idx) else 0
    }

    constructor(name: String) {
        id = 0
        this.name = name
        weight = 0
    }

    constructor() {
        id = 0
        this.name = ""
        weight = 0
    }

    constructor(department: Department) {
        id = department.id
        name = department.name
        weight = department.weight
    }

    override fun compareTo(other: Department): Int {
        return name.compareTo(other.name)
    }

    fun isEquals(other: Department): Boolean {
        return weight == other.weight && name.equals(other.name)
    }
}
