package fr.geobert.efficio.db

import android.content.ContentValues
import android.content.Context
import android.provider.BaseColumns
import fr.geobert.efficio.data.Store
import fr.geobert.efficio.misc.convertNonAscii

object StoreTable : BaseTable() {
    override val TABLE_NAME = "stores"

    val COL_NAME = "store_name"
    val COL_NORM_NAME = "store_norm"

    override fun CREATE_COLUMNS() = "${COL_NORM_NAME} TEXT NOT NULL UNIQUE ON CONFLICT IGNORE, " +
            "$COL_NAME TEXT NOT NULL"

    override val COLS_TO_QUERY: Array<String> = arrayOf(BaseColumns._ID, COL_NAME)

    val CREATE_TRIGGER_ON_STORE_DEL by lazy {
        "CREATE TRIGGER on_store_deleted " +
                "AFTER DELETE ON ${StoreTable.TABLE_NAME} BEGIN " +
                "DELETE FROM ${StoreCompositionTable.TABLE_NAME} WHERE " +
                "${StoreCompositionTable.COL_STORE_ID} = old.${BaseColumns._ID};" +
                "DELETE FROM ${ItemWeightTable.TABLE_NAME} WHERE " +
                "${ItemWeightTable.COL_STORE_ID} = old.${BaseColumns._ID};" +
                "DELETE FROM ${ItemDepTable.TABLE_NAME} WHERE " +
                "${ItemDepTable.COL_STORE_ID} = old.${BaseColumns._ID};" +
                "END"
    }

    fun create(ctx: Context, store: Store): Long {
        val v = ContentValues()
        v.put(COL_NAME, store.name)
        v.put(COL_NORM_NAME, store.name.convertNonAscii().toLowerCase())
        return insert(ctx, v)
    }

    fun update() {

    }

    fun delete() {

    }
}
