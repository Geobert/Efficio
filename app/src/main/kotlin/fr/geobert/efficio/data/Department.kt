package fr.geobert.efficio.data

import android.database.Cursor
import android.os.Bundle
import kotlin.properties.Delegates

class Department : Comparable<Department> {
    var id: Long by Delegates.notNull()
    var name: String by Delegates.notNull()
    var weight: Double by Delegates.notNull()

    constructor(cursor: Cursor, b: Bundle) {
        id = cursor.getLong(b.getInt("id"))
        name = cursor.getString(b.getInt("name"))
        val idx = b.getInt("weight")
        weight = if (idx > 0) cursor.getDouble(idx) else 0.0
    }

    constructor(name: String, w: Double) {
        id = 0
        this.name = name
        weight = w
    }

    constructor() {
        id = 0
        this.name = ""
        weight = 0.0
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
        return weight == other.weight && name == other.name
    }
}
