package fr.geobert.efficio.data

import android.database.Cursor
import fr.geobert.efficio.db.TaskTable

class Item(cursor: Cursor) { // from TaskTable query
    var id: Long = cursor.getLong(cursor.getColumnIndex(TaskTable.COL_ITEM_ID))
    var name = cursor.getString(cursor.getColumnIndex("item_name"))
    var weight: Int = cursor.getInt(cursor.getColumnIndex("item_weight"))
    var department = Department(cursor)
}