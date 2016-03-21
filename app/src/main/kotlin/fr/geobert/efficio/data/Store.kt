package fr.geobert.efficio.data

import android.database.Cursor


class Store(var name: String) {
    var id: Long = 0
    val tasks: Array<Task> = emptyArray()

    // used only in MainActivity for the moment (to fill the Spinner),
    // so no need to query the tasks
    constructor(cursor: Cursor) : this(cursor.getString(1)) {
        id = cursor.getLong(0)
    }
}