package fr.geobert.efficio.data

import kotlin.properties.Delegates

class Item { // from TaskTable query
    var id: Long by Delegates.notNull()
    var name: String by Delegates.notNull()
    var weight: Int by Delegates.notNull()
    var department: Department by Delegates.notNull()

//    constructor(cursor: Cursor) {
    //        id = cursor.getLong(cursor.getColumnIndex(TaskTable.COL_ITEM_ID))
    //        weight = cursor.getInt(cursor.getColumnIndex("item_weight"))
    //        department = Department(cursor)
    //        name = cursor.getString(cursor.getColumnIndex("item_name"))
    //    }

    constructor(name: String, depName: String) {
        id = 0
        this.name = name
        weight = 0
        department = Department(depName)
    }
}