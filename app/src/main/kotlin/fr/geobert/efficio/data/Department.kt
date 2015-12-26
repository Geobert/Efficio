package fr.geobert.efficio.data

import android.database.Cursor

class Department(cursor: Cursor) {
    var id: Long = cursor.getLong(cursor.getColumnIndex("dep_id"))
    var name: String = cursor.getString(cursor.getColumnIndex("dep_name"))
    var weight: Int = cursor.getInt(cursor.getColumnIndex("dep_weight"))
}