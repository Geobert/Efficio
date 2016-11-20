package fr.geobert.efficio.data

import android.database.Cursor
import fr.geobert.efficio.adapter.TaskAdapter
import fr.geobert.efficio.db.TaskTable
import fr.geobert.efficio.extensions.TIME_ZONE
import hirondelle.date4j.DateTime
import kotlin.properties.Delegates

class Task : Comparable<Task> {
    var id: Long by Delegates.notNull()
    var isDone: Boolean by Delegates.notNull()
    var item: Item by Delegates.notNull()
    var type: TaskAdapter.VIEW_TYPES by Delegates.notNull()
    var qty: Int = 1
    var lastChecked: DateTime = DateTime.now(TIME_ZONE)
    var period: Int = 0
    var periodUnit: PeriodUnit = PeriodUnit.NONE

    val periodicity: Period
        get() =
        when (periodUnit) {
            PeriodUnit.NONE -> Period.NONE
            else -> if (period == 1) {
                when (periodUnit) {
                    PeriodUnit.DAY -> Period.DAILY
                    PeriodUnit.WEEK -> Period.WEEKLY
                    PeriodUnit.MONTH -> Period.MONTHLY
                    PeriodUnit.YEAR -> Period.YEARLY
                    else -> Period.NONE
                }
            } else Period.CUSTOM
        }


    constructor() {
        type = TaskAdapter.VIEW_TYPES.Header
        isDone = true
        id = 0
        item = Item()
    }

    constructor(cursor: Cursor) {
        id = cursor.getLong(0)
        isDone = cursor.getInt(cursor.getColumnIndex(TaskTable.COL_IS_DONE)) == 1
        item = Item(cursor)
        type = TaskAdapter.VIEW_TYPES.Normal
        qty = cursor.getInt(cursor.getColumnIndex(TaskTable.COL_QTY))

        val instant = cursor.getLong(cursor.getColumnIndex(TaskTable.COL_LAST_CHECKED))
        lastChecked = if (instant > 0) DateTime.forInstant(instant, TIME_ZONE) else lastChecked
        periodUnit = PeriodUnit.fromInt(cursor.getInt(cursor.getColumnIndex(TaskTable.COL_PERIOD_UNIT)))
        period = cursor.getInt(cursor.getColumnIndex(TaskTable.COL_PERIOD))
    }

    constructor(item: Item) {
        id = 0
        isDone = false
        this.item = item
        type = TaskAdapter.VIEW_TYPES.Normal
    }

    constructor(task: Task) {
        id = task.id
        isDone = task.isDone
        this.item = Item(task.item)
        type = task.type
    }

    // used for sort()
    override fun compareTo(other: Task): Int {
        return if (isDone != other.isDone) {
            if (isDone) 1 else -1 // done task always appears later on list
        } else { // same task done state
            if (isDone) item.name.compareTo(other.item.name) else // if task is done, we don't care about weights
                if (item.department.weight > other.item.department.weight) 1 else // task is not done, compare department
                    if (item.department.weight < other.item.department.weight) -1 else {
                        val r = item.department.name.compareTo(other.item.department.name) // same dep weight compare their names
                        if (r != 0) r else
                            if (item.weight > other.item.weight) 1 else // same dep, compare item
                                if (item.weight < other.item.weight) -1 else
                                    item.name.compareTo(other.item.name)
                    }
        }
    }

    fun isEquals(other: Task): Boolean {
        return isDone == other.isDone &&
                type == other.type &&
                item.isEquals(other.item) &&
                period == other.period &&
                periodUnit == other.periodUnit &&
                periodicity == other.periodicity

    }

    override fun toString(): String {
        return "[name: ${item.name} / depWeight: ${item.department.weight} / itemWeight: ${item.weight}]"
    }
}