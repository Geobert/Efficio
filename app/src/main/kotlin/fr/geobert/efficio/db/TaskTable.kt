package fr.geobert.efficio.db

import android.app.Activity
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
            "${leftOuterJoin(TABLE_NAME, COL_ITEM_ID, ItemDepTable.TABLE_NAME, ItemDepTable.COL_ITEM_ID)} " +
            "${leftOuterJoin(TABLE_NAME, COL_STORE_ID, StoreCompositionTable.TABLE_NAME, StoreCompositionTable.COL_STORE_ID)} AND items_dep.dep_id = department_weight._id " +
            "${leftOuterJoin(TABLE_NAME, COL_ITEM_ID, ItemWeightTable.TABLE_NAME, ItemWeightTable.COL_ITEM_ID)} " +
            "${leftOuterJoin(StoreCompositionTable.TABLE_NAME, StoreCompositionTable.COL_DEP_ID, DepartmentTable.TABLE_NAME)} "

    override val COLS_TO_QUERY: Array<String> = arrayOf("$TABLE_NAME.${BaseColumns._ID}",
            "$TABLE_NAME.$COL_ITEM_ID", "${ItemTable.TABLE_NAME}.${ItemTable.COL_NAME} as item_name",
            "$TABLE_NAME.$COL_STORE_ID", "${StoreTable.TABLE_NAME}.${StoreTable.COL_NAME} as store_name",
            "${DepartmentTable.TABLE_NAME}.${BaseColumns._ID} as dep_id",
            "${DepartmentTable.TABLE_NAME}.${DepartmentTable.COL_NAME} as dep_name",
            "${StoreCompositionTable.TABLE_NAME}.${StoreCompositionTable.COL_WEIGHT} as dep_weight",
            "${ItemWeightTable.TABLE_NAME}.${ItemWeightTable.COL_WEIGHT} as item_weight",
            COL_IS_DONE
    )

    val RESTRICT_TO_STORE = "(${TaskTable.TABLE_NAME}.$COL_STORE_ID = ?)"
    val ORDERING = "is_done asc, dep_weight desc, item_weight desc, item_name asc"

    val CREATE_TRIGGER_ON_TASK_DEL by lazy {
        "CREATE TRIGGER on_task_deleted " +
                "AFTER DELETE ON ${TaskTable.TABLE_NAME} BEGIN " +
                "DELETE FROM ${ItemTable.TABLE_NAME} WHERE " +
                "${ItemTable.TABLE_NAME}.${BaseColumns._ID} = old.${BaseColumns._ID};" +
                "END"
    }

    fun getAllTasksForStoreLoader(ctx: Context, storeId: Long): CursorLoader {
        return CursorLoader(ctx, TaskTable.CONTENT_URI, TaskTable.COLS_TO_QUERY, RESTRICT_TO_STORE,
                arrayOf(storeId.toString()), ORDERING)
    }

    fun getTaskByIdLoader(ctx: Context, taskId: Long): CursorLoader {
        return CursorLoader(ctx, buildWithId(taskId), COLS_TO_QUERY, null, null, null)
    }

    fun create(activity: Activity, t: Task, storeId: Long): Long {
        val v = ContentValues()
        v.put(COL_STORE_ID, storeId)
        v.put(COL_ITEM_ID, t.item.id)
        v.put(COL_IS_DONE, false)
        return insert(activity, v)
    }

    fun updateDoneState(activity: Activity, task: Task): Int {
        val v = ContentValues()
        v.put(COL_IS_DONE, task.isDone)
        return update(activity, task.id, v)
    }

    fun updateTask(activity: Activity, task: Task): Int {
        if (ItemDepTable.updateItemDep(activity, task.item.id, task.item.department.id) > 0) {
            return ItemTable.updateItem(activity, task.item)
        }
        return 0
    }

    fun deleteTask(activity: Activity, taskId: Long) {
        activity.contentResolver.delete(buildWithId(taskId), null, null)
    }
}
