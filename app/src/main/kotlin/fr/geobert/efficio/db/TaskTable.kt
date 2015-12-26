package fr.geobert.efficio.db

import android.content.ContentValues
import android.content.Context
import android.content.CursorLoader
import android.provider.BaseColumns
import fr.geobert.efficio.data.Task

object TaskTable : BaseTable() {
    override val TABLE_NAME: String = "tasks"

    val COL_STORE_ID = "store_id"
    val COL_ITEM_ID = "item_id"
    val COL_IS_DONE = "is_done"

    override fun CREATE_COLUMNS(): String = "$COL_STORE_ID INTEGER NOT NULL, " +
            "$COL_ITEM_ID INTEGER NOT NULL, " +
            "$COL_IS_DONE INTEGER NOT NULL, " +
            "${foreignId(COL_STORE_ID, StoreTable.TABLE_NAME)}, " +
            "${foreignId(COL_ITEM_ID, ItemTable.TABLE_NAME)}"


    val TABLE_JOINED = "$TABLE_NAME " +
            "${leftOuterJoin(TABLE_NAME, COL_STORE_ID, StoreTable.TABLE_NAME)} " +
            "${leftOuterJoin(TABLE_NAME, COL_ITEM_ID, ItemTable.TABLE_NAME)} " +
            "${leftOuterJoin(TABLE_NAME, COL_STORE_ID, ItemDepTable.TABLE_NAME)} " +
            "${leftOuterJoin(TABLE_NAME, COL_STORE_ID, DepWeightTable.TABLE_NAME, DepWeightTable.COL_STORE_ID)} " +
            "${leftOuterJoin(TABLE_NAME, COL_STORE_ID, ItemWeightTable.TABLE_NAME, ItemWeightTable.COL_STORE_ID)} " +
            "${leftOuterJoin(DepWeightTable.TABLE_NAME, DepWeightTable.COL_DEP_ID, DepartmentTable.TABLE_NAME)} "

    override val COLS_TO_QUERY: Array<String> = arrayOf("$TABLE_NAME.${BaseColumns._ID}", // 0
            "$TABLE_NAME.$COL_ITEM_ID", "${ItemTable.TABLE_NAME}.${ItemTable.COL_NAME} as item_name", // 2
            "$TABLE_NAME.$COL_STORE_ID", "${StoreTable.TABLE_NAME}.${StoreTable.COL_NAME} as store_name", // 4
            "${DepartmentTable.TABLE_NAME}.${BaseColumns._ID} as dep_id", // 5
            "${DepartmentTable.TABLE_NAME}.${DepartmentTable.COL_NAME} as dep_name", // 6
            "${DepWeightTable.TABLE_NAME}.${DepWeightTable.COL_WEIGHT} as dep_weight", // 7
            "${ItemWeightTable.TABLE_NAME}.${ItemWeightTable.COL_WEIGHT} as item_weight", // 8
            COL_IS_DONE // 9
    )

    val RESTRICT_TO_STORE = "(${TaskTable.TABLE_NAME}.$COL_STORE_ID = ?)"
    val ORDERING = "dep_weight desc, item_weight desc"

    fun fetchAllTasksForStore(ctx: Context, storeId: Long): CursorLoader {
        return CursorLoader(ctx, TaskTable.CONTENT_URI, TaskTable.COLS_TO_QUERY, RESTRICT_TO_STORE,
                arrayOf(storeId.toString()), ORDERING)
    }

    fun addTask(ctx: Context, task: Task, storeId: Long) {
        val values = ContentValues()
    }
}
