package fr.geobert.efficio.data

import android.database.Cursor
import android.os.Bundle
import kotlin.properties.Delegates

class Department : Comparable<Department> {
    var id: Long by Delegates.notNull()
    var name: String by Delegates.notNull()
    var weight: Int by Delegates.notNull()

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

    override fun compareTo(other: Department): Int {
        return name.compareTo(other.name)
    }
}
