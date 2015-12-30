package fr.geobert.efficio.db

import android.content.ContentValues
import android.content.Context
import android.provider.BaseColumns
import fr.geobert.efficio.data.Item
import fr.geobert.efficio.misc.convertNonAscii

object ItemTable : BaseTable() {
    override val TABLE_NAME = "items"

    val COL_NAME = "item_name"
    val COL_NORM_NAME = "item_norm"

    override fun CREATE_COLUMNS() = "${COL_NORM_NAME} TEXT NOT NULL UNIQUE ON CONFLICT IGNORE, " +
            "$COL_NAME TEXT NOT NULL"

    override val COLS_TO_QUERY: Array<String> = arrayOf(BaseColumns._ID, COL_NAME)

    val CREATE_TRIGGER_ON_ITEM_DEL by lazy {
        "CREATE TRIGGER on_item_deleted " +
                "AFTER DELETE ON ${ItemTable.TABLE_NAME} BEGIN " +
                "DELETE FROM ${ItemWeightTable.TABLE_NAME} WHERE " +
                "${ItemWeightTable.COL_ITEM_ID} = old.${BaseColumns._ID};" +
                "DELETE FROM ${ItemDepTable.TABLE_NAME} WHERE " +
                "${ItemDepTable.COL_ITEM_ID} = old.${BaseColumns._ID};" +
                "DELETE FROM ${ItemWeightTable.TABLE_NAME} WHERE " +
                "${ItemWeightTable.COL_ITEM_ID} = old.${BaseColumns._ID};" +
                "END"
    }

    fun create(ctx: Context, item: Item): Long {
        val v = ContentValues()
        v.put(COL_NORM_NAME, item.name.convertNonAscii().toLowerCase())
        v.put(COL_NAME, item.name)
        return insert(ctx, v)
    }

    fun update() {

    }

    fun delete() {

    }
}