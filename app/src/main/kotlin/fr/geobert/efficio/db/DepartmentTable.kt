package fr.geobert.efficio.db

import android.app.Activity
import android.content.*
import android.database.sqlite.SQLiteDatabase
import android.provider.BaseColumns
import fr.geobert.efficio.data.Department
import fr.geobert.efficio.extensions.normalize

object DepartmentTable : BaseTable() {
    override val TABLE_NAME = "departments"

    val COL_NAME = "dep_name"
    val COL_NORM_NAME = "dep_norm"

    override fun CREATE_COLUMNS() = "$COL_NORM_NAME TEXT NOT NULL UNIQUE ON CONFLICT IGNORE, " +
            "$COL_NAME TEXT NOT NULL"

    override val COLS_TO_QUERY: Array<String> = arrayOf(BaseColumns._ID, COL_NAME)

    val CREATE_TRIGGER_ON_DEP_DELETE by lazy {
        "CREATE TRIGGER on_dep_deleted " +
                "AFTER DELETE ON ${DepartmentTable.TABLE_NAME} BEGIN " +
                "DELETE FROM ${TaskTable.TABLE_NAME} WHERE " +
                "${TaskTable.COL_ITEM_ID} IN " +
                "(SELECT ${ItemDepTable.COL_ITEM_ID} FROM ${ItemDepTable.TABLE_NAME} WHERE " +
                "${ItemDepTable.TABLE_NAME}.${ItemDepTable.COL_DEP_ID} = old.${BaseColumns._ID});" +
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

    fun updateDepartment(activity: Activity, depId: Long, s: String): Int {
        val v = ContentValues()
        v.put(COL_NAME, s)
        v.put(COL_NORM_NAME, s.normalize())
        return update(activity, depId, v)
    }

    fun deleteDep(activity: Activity, depId: Long): Int {
        return delete(activity, depId)
    }

    fun upgradeFromV1(db: SQLiteDatabase) {
        db.execSQL("DROP TRIGGER on_dep_deleted")
        db.execSQL(CREATE_TRIGGER_ON_DEP_DELETE)
    }
}