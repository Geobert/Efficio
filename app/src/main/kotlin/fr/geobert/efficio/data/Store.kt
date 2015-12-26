package fr.geobert.efficio.data

import android.content.ContentValues
import fr.geobert.efficio.db.StoreTable


class Store(var name: String) {
    var id: Long = 0
    val tasks: Array<Task> = emptyArray()

    fun getContentValues(): ContentValues {
        val v = ContentValues()
        v.put(StoreTable.COL_NAME, name)
        return v
    }
}