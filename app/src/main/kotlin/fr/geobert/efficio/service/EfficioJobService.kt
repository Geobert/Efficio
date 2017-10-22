package fr.geobert.efficio.service

import android.content.Context
import android.content.Intent
import android.support.v4.app.JobIntentService
import android.util.Log
import fr.geobert.efficio.data.PeriodUnit
import fr.geobert.efficio.data.Task
import fr.geobert.efficio.db.TaskTable
import fr.geobert.efficio.extensions.TIME_ZONE
import fr.geobert.efficio.extensions.plusMonth
import fr.geobert.efficio.extensions.plusYear
import fr.geobert.efficio.misc.refreshTaskList
import fr.geobert.efficio.misc.updateWidgets
import hirondelle.date4j.DateTime

class EfficioJobService : JobIntentService() {
    companion object {
        val JOB_ID = 1000
        fun enqueueWork(ctx: Context, storeId: Long) {
            val work = Intent(ctx, EfficioJobService::class.java)
            work.putExtra("storeId", storeId)
            enqueueWork(ctx, EfficioJobService::class.java, JOB_ID, work)
        }
    }

    override fun onHandleWork(intent: Intent) {
        Log.d("EfficioJobService", "processTasks")
        val storeId = intent.getLongExtra("storeId", -1)
        val cursor = TaskTable.getAllDoneAndSchedTasks(this, storeId)
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                val today = DateTime.today(TIME_ZONE)
                var needUpdate = false
                do {
                    val task = Task(cursor)
                    val insertDate = when (task.periodUnit) {
                        PeriodUnit.DAY -> task.lastChecked.plusDays(task.period)
                        PeriodUnit.WEEK -> task.lastChecked.plusDays(7 * task.period)
                        PeriodUnit.MONTH -> task.lastChecked.plusMonth(task.period)
                        PeriodUnit.YEAR -> task.lastChecked.plusYear(task.period)
                        PeriodUnit.NONE -> TODO() // should not happen, sql request removes PeriodUnit.NONE
                    }.minusDays(1) // do it the day before

                    if (today.gteq(insertDate)) {
                        TaskTable.updateDoneState(this, task.id, false)
                        needUpdate = true
                    }
                } while (cursor.moveToNext())

                if (needUpdate) {
                    updateWidgets(this)
                    refreshTaskList(this, -1)
                }
            }
            cursor.close()
        }
    }
}