package fr.geobert.efficio.db

import android.content.ContentValues
import android.content.Context
import android.content.CursorLoader
import android.provider.BaseColumns
import fr.geobert.efficio.data.Department
import fr.geobert.efficio.misc.normalize

object DepartmentTable : BaseTable() {
    override val TABLE_NAME = "departments"

    val COL_NAME = "dep_name"
    val COL_NORM_NAME = "dep_norm"

    override fun CREATE_COLUMNS() = "${COL_NORM_NAME} TEXT NOT NULL UNIQUE ON CONFLICT IGNORE, " +
            "$COL_NAME TEXT NOT NULL"

    override val COLS_TO_QUERY: Array<String> = arrayOf(BaseColumns._ID, COL_NAME)

    val CREATE_TRIGGER_ON_DEP_DELETE by lazy {
        "CREATE TRIGGER on_dep_deleted " +
                "AFTER DELETE ON ${DepartmentTable.TABLE_NAME} BEGIN " +
                "DELETE FROM ${StoreCompositionTable.TABLE_NAME} WHERE " +
                "${StoreCompositionTable.COL_DEP_ID} = old.${BaseColumns._ID};" +
                "DELETE FROM ${ItemDepTable.TABLE_NAME} WHERE " +
                "${ItemDepTable.COL_DEP_ID} = old.${BaseColumns._ID};" +
                "END"
    }

    fun getAllDepLoader(activity: Context): CursorLoader {
        return CursorLoader(activity, CONTENT_URI, COLS_TO_QUERY, null, null, "dep_name desc")
    }

    fun create(ctx: Context, department: Department): Long {
        val v = ContentValues()
        v.put(COL_NORM_NAME, department.name.normalize())
        v.put(COL_NAME, department.name)
        return insert(ctx, v)
    }


}