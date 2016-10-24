package fr.geobert.efficio.db

import android.app.Activity
import android.content.ContentValues
import fr.geobert.efficio.data.Item

object ItemWeightTable : BaseTable() {
    override val TABLE_NAME: String = "items_weight"

    val COL_STORE_ID = "store_id"
    val COL_ITEM_ID = "item_id"
    val COL_WEIGHT = "item_weight"

    override fun CREATE_COLUMNS(): String = "$COL_STORE_ID INTEGER NOT NULL, " +
            "$COL_ITEM_ID INTEGER NOT NULL, " +
            "$COL_WEIGHT REAL NOT NULL, " +
            "${foreignId(COL_STORE_ID, StoreTable.TABLE_NAME)}," +
            foreignId(COL_ITEM_ID, ItemTable.TABLE_NAME)

    override val COLS_TO_QUERY: Array<String>
        get() = throw UnsupportedOperationException()

    fun create(activity: Activity, i: Item, storeId: Long): Long {
        val v = ContentValues()
        v.put(COL_STORE_ID, storeId)
        v.put(COL_ITEM_ID, i.id)
        v.put(COL_WEIGHT, i.weight)
        return insert(activity, v)
    }

    fun updateWeight(activity: Activity, item: Item): Int {
        val v = ContentValues()
        v.put(COL_WEIGHT, item.weight)
        return update(activity, item.id, v)
    }


}