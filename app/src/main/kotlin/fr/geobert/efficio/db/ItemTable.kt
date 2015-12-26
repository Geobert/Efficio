package fr.geobert.efficio.db

import android.provider.BaseColumns

object ItemTable : BaseTable() {
    override val TABLE_NAME = "items"

    val COL_NAME = "item_name"

    override fun CREATE_COLUMNS() = "$COL_NAME TEXT NOT NULL"

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

    fun create() {

    }

    fun update() {

    }

    fun delete() {

    }
}