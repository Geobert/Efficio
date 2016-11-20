package fr.geobert.efficio.db

import android.app.Activity
import android.content.*
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.provider.BaseColumns
import fr.geobert.efficio.data.Task
import fr.geobert.efficio.extensions.TIME_ZONE
import hirondelle.date4j.DateTime

object TaskTable : BaseTable() {
    override val TABLE_NAME: String = "tasks"

    val COL_STORE_ID = "store_id"
    val COL_ITEM_ID = "item_id"
    val COL_IS_DONE = "is_done"
    val COL_QTY = "quantity"
    val COL_LAST_CHECKED = "last_checked"
    val COL_PERIOD = "periodicity"
    val COL_PERIOD_UNIT = "period_unit"

    override fun CREATE_COLUMNS(): String = "$COL_STORE_ID INTEGER NOT NULL, " +
            "$COL_ITEM_ID INTEGER NOT NULL, " +
            "$COL_IS_DONE INTEGER NOT NULL, " +
            "$COL_QTY INTEGER NOT NULL DEFAULT 1, " +
            "${foreignId(COL_STORE_ID, StoreTable.TABLE_NAME)}, " +
            "${foreignId(COL_ITEM_ID, ItemTable.TABLE_NAME)}, " +
            "$COL_LAST_CHECKED INTEGER NOT NULL DEFAULT 0, " +
            "$COL_PERIOD INTEGER NOT NULL DEFAULT 0, " +
            "$COL_PERIOD_UNIT INTEGER NOT NULL DEFAULT 0"


    val ADD_LAST_CHECKED_COL = "ALTER TABLE $TABLE_NAME ADD COLUMN $COL_LAST_CHECKED INTEGER NOT NULL DEFAULT 0"
    val ADD_PERIOD_COL = "ALTER TABLE $TABLE_NAME ADD COLUMN $COL_PERIOD INTEGER NOT NULL DEFAULT 0"
    val ADD_PERIOD_UNIT_COL = "ALTER TABLE $TABLE_NAME ADD COLUMN $COL_PERIOD_UNIT INTEGER NOT NULL DEFAULT 0"

    val TABLE_JOINED = "$TABLE_NAME " +
            "${leftOuterJoin(TABLE_NAME, COL_STORE_ID, StoreTable.TABLE_NAME)} " +
            "${leftOuterJoin(TABLE_NAME, COL_ITEM_ID, ItemTable.TABLE_NAME)} " +
            "${leftOuterJoin(TABLE_NAME, COL_ITEM_ID, ItemDepTable.TABLE_NAME, ItemDepTable.COL_ITEM_ID)} " +
            "${leftOuterJoin(TABLE_NAME, COL_STORE_ID, StoreCompositionTable.TABLE_NAME, StoreCompositionTable.COL_STORE_ID)} AND items_dep.dep_id = department_weight.dep_id " +
            "${leftOuterJoin(TABLE_NAME, COL_ITEM_ID, ItemWeightTable.TABLE_NAME, ItemWeightTable.COL_ITEM_ID)} " +
            "${leftOuterJoin(StoreCompositionTable.TABLE_NAME, StoreCompositionTable.COL_DEP_ID, DepartmentTable.TABLE_NAME)} "

    override val COLS_TO_QUERY: Array<String> = arrayOf("$TABLE_NAME.${BaseColumns._ID}",
            "$TABLE_NAME.$COL_ITEM_ID", "${ItemTable.TABLE_NAME}.${ItemTable.COL_NAME} as item_name",
            "$TABLE_NAME.$COL_STORE_ID", "${StoreTable.TABLE_NAME}.${StoreTable.COL_NAME} as store_name",
            "${DepartmentTable.TABLE_NAME}.${BaseColumns._ID} as dep_id",
            "${DepartmentTable.TABLE_NAME}.${DepartmentTable.COL_NAME} as dep_name",
            "${StoreCompositionTable.TABLE_NAME}.${StoreCompositionTable.COL_WEIGHT} as dep_weight",
            "${ItemWeightTable.TABLE_NAME}.${ItemWeightTable.COL_WEIGHT} as item_weight",
            COL_IS_DONE,
            COL_QTY,
            COL_PERIOD,
            COL_PERIOD_UNIT,
            COL_LAST_CHECKED
    )

    val RESTRICT_TO_STORE = "(${TaskTable.TABLE_NAME}.$COL_STORE_ID = ?)"
    val ORDERING = "is_done asc, dep_weight asc, dep_name asc, item_weight asc, item_name asc"

    val CREATE_TRIGGER_ON_TASK_DEL by lazy {
        "CREATE TRIGGER on_task_deleted " +
                "AFTER DELETE ON ${TaskTable.TABLE_NAME} BEGIN " +
                "DELETE FROM ${ItemTable.TABLE_NAME} WHERE " +
                "${ItemTable.TABLE_NAME}.${BaseColumns._ID} = old.$COL_ITEM_ID;" +
                "END"
    }

    // async
    fun getAllTasksForStoreLoader(ctx: Context, storeId: Long): CursorLoader {
        return CursorLoader(ctx, TaskTable.CONTENT_URI, TaskTable.COLS_TO_QUERY, RESTRICT_TO_STORE,
                arrayOf(storeId.toString()), ORDERING)
    }

    fun getTaskByIdLoader(ctx: Context, taskId: Long): CursorLoader {
        return CursorLoader(ctx, buildWithId(taskId), COLS_TO_QUERY, null, null, null)
    }

    // sync version
    fun getAllTasksForStore(ctx: Context, storeId: Long): Cursor? {
        return ctx.contentResolver.query(TaskTable.CONTENT_URI,
                TaskTable.COLS_TO_QUERY, RESTRICT_TO_STORE, arrayOf(storeId.toString()), ORDERING)
    }

    fun getTaskById(ctx: Context, taskId: Long): Cursor? {
        return ctx.contentResolver.query(buildWithId(taskId), COLS_TO_QUERY, null, null, null)
    }

    fun getAllNotDoneTasksForStore(ctx: Context, storeId: Long): Cursor? {
        return ctx.contentResolver.query(TaskTable.CONTENT_URI,
                TaskTable.COLS_TO_QUERY, "$RESTRICT_TO_STORE and $TABLE_NAME.$COL_IS_DONE = 0",
                arrayOf(storeId.toString()), ORDERING)
    }

    fun getAllDoneAndSchedTasks(ctx: Context, storeId: Long): Cursor? {
        return if (storeId > -1)
            ctx.contentResolver.query(TaskTable.CONTENT_URI,
                    TaskTable.COLS_TO_QUERY, "$RESTRICT_TO_STORE AND $TABLE_NAME.$COL_IS_DONE = ? AND $TABLE_NAME.$COL_PERIOD_UNIT != ?",
                    arrayOf(storeId.toString(), "1", "0"), ORDERING)
        else
            ctx.contentResolver.query(TaskTable.CONTENT_URI,
                    TaskTable.COLS_TO_QUERY, "$TABLE_NAME.$COL_IS_DONE = ? AND $TABLE_NAME.$COL_PERIOD_UNIT != ?",
                    arrayOf("1", "0"), ORDERING)
    }

    fun create(activity: Activity, t: Task, storeId: Long): Long {
        val v = ContentValues()
        v.put(COL_STORE_ID, storeId)
        v.put(COL_ITEM_ID, t.item.id)
        v.put(COL_IS_DONE, false)
        return insert(activity, v)
    }

    fun updateDoneState(activity: Context, taskId: Long, isDone: Boolean): Int {
        val v = ContentValues()
        v.put(COL_IS_DONE, isDone)
        if (isDone)
            v.put(COL_LAST_CHECKED, DateTime.today(TIME_ZONE).getMilliseconds(TIME_ZONE))
        return update(activity, taskId, v)
    }

    fun updateTaskQty(activity: Activity, taskId: Long, qty: Int): Int {
        val v = ContentValues()
        v.put(COL_QTY, qty)
        return update(activity, taskId, v)
    }

    fun updateTask(activity: Activity, task: Task): Int {
        val v = ContentValues()
        v.put(COL_QTY, task.qty)
        v.put(COL_PERIOD, task.period)
        v.put(COL_PERIOD_UNIT, task.periodUnit.ordinal)
        if (update(activity, task.id, v) > 0)
            if (ItemDepTable.updateItemDep(activity, task.item.id, task.item.department.id) > 0) {
                return ItemTable.updateItem(activity, task.item)
            }
        return 0
    }

    fun deleteTask(activity: Activity, taskId: Long): Int {
        return delete(activity, taskId)
    }

    fun upgradeFromV1(db: SQLiteDatabase) {
        db.execSQL("DROP TRIGGER on_task_deleted")
        db.execSQL(CREATE_TRIGGER_ON_TASK_DEL)
    }

    fun upgradeFromV2(db: SQLiteDatabase) {
        db.execSQL(ADD_LAST_CHECKED_COL)
        db.execSQL(ADD_PERIOD_COL)
        db.execSQL(ADD_PERIOD_UNIT_COL)
    }


}
