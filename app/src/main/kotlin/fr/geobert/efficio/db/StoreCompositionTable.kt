package fr.geobert.efficio.db


import android.content.ContentValues
import android.content.Context
import android.content.CursorLoader
import android.provider.BaseColumns
import fr.geobert.efficio.data.Department

object StoreCompositionTable : BaseTable() {
    override val TABLE_NAME: String = "department_weight"

    val COL_STORE_ID = "store_id"
    val COL_DEP_ID = "dep_id"
    val COL_WEIGHT = "dep_weight"

    override fun CREATE_COLUMNS(): String = "$COL_STORE_ID INTEGER NOT NULL, " +
            "$COL_DEP_ID INTEGER NOT NULL, " +
            "$COL_WEIGHT INTEGER NOT NULL, " +
            "${foreignId(COL_STORE_ID, StoreTable.TABLE_NAME)}, " +
            "${foreignId(COL_DEP_ID, DepartmentTable.TABLE_NAME)}"


    val TABLE_JOINED = "$TABLE_NAME " +
            " LEFT OUTER JOIN ${StoreTable.TABLE_NAME} ON $TABLE_NAME.$COL_STORE_ID = ${StoreTable.TABLE_NAME}.${BaseColumns._ID}" +
            " LEFT OUTER JOIN ${DepartmentTable.TABLE_NAME} ON $TABLE_NAME.$COL_DEP_ID = ${DepartmentTable.TABLE_NAME}.${BaseColumns._ID}"

    override val COLS_TO_QUERY: Array<String> = arrayOf("${StoreTable.TABLE_NAME}.${BaseColumns._ID}",
            "${StoreTable.TABLE_NAME}.${StoreTable.COL_NAME} as store_name",
            "${DepartmentTable.TABLE_NAME}.${BaseColumns._ID} as $COL_DEP_ID",
            "${DepartmentTable.TABLE_NAME}.${DepartmentTable.COL_NAME} as dep_name",
            "$TABLE_NAME.$COL_WEIGHT")

    val RESTRICT_TO_STORE = "($TABLE_NAME.$COL_STORE_ID = ?)"
    val ORDERING = "$COL_WEIGHT desc"

    fun getDepFromStoreLoader(activity: Context, storeId: Long): CursorLoader {
        return CursorLoader(activity, CONTENT_URI, COLS_TO_QUERY, RESTRICT_TO_STORE,
                arrayOf(storeId.toString()), ORDERING)
    }

    fun create(ctx: Context, storeId: Long, department: Department): Long {
        val v = ContentValues()
        v.put(COL_STORE_ID, storeId)
        v.put(COL_DEP_ID, department.id)
        v.put(COL_WEIGHT, 0)

        val res = ctx.contentResolver.insert(CONTENT_URI, v)
        return res.lastPathSegment.toLong()
    }
}