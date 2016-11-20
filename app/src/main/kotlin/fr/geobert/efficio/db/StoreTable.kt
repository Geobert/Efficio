package fr.geobert.efficio.db

import android.content.ContentValues
import android.content.Context
import android.content.CursorLoader
import android.provider.BaseColumns
import fr.geobert.efficio.data.Store
import fr.geobert.efficio.extensions.normalize

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
                "DELETE FROM ${WidgetTable.TABLE_NAME} WHERE " +
                "${WidgetTable.COL_STORE_ID} = old.${BaseColumns._ID};" +
                "END"
    }


    fun create(ctx: Context, name: String): Long {
        val v = ContentValues()
        v.put(COL_NAME, name)
        v.put(COL_NORM_NAME, name.normalize())
        return insert(ctx, v)
    }

    fun create(ctx: Context, store: Store): Long {
        return create(ctx, store.name)
    }

    fun renameStore(ctx: Context, storeId: Long, name: String): Int {
        val v = ContentValues()
        v.put(COL_NAME, name)
        v.put(COL_NORM_NAME, name.normalize())
        return update(ctx, storeId, v)
    }

    fun getAllStoresLoader(ctx: Context): CursorLoader {
        return CursorLoader(ctx, StoreTable.CONTENT_URI, StoreTable.COLS_TO_QUERY, null, null, null)
    }

    fun deleteStore(ctx: Context, currentStore: Store): Int {
        return delete(ctx, currentStore.id)
    }
}
