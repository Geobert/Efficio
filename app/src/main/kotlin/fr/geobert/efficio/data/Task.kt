package fr.geobert.efficio.data

import android.database.Cursor


class Task(cursor: Cursor) {
    var id: Long = cursor.getLong(0)
    var isDone: Boolean = cursor.getInt(9) == 1
    var item = Item(cursor)
}