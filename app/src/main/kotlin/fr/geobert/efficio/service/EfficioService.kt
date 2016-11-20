package fr.geobert.efficio.service

import android.app.IntentService
import android.content.*
import android.os.PowerManager
import android.util.Log
import fr.geobert.efficio.data.*
import fr.geobert.efficio.db.TaskTable
import fr.geobert.efficio.extensions.*
import fr.geobert.efficio.misc.*
import hirondelle.date4j.DateTime

class EfficioService : IntentService(TAG) {
    companion object {
        val TAG = "EfficioService"
        private var lockStatic: android.os.PowerManager.WakeLock? = null
        val LOCK_NAME_STATIC: String = "fr.geobert.efficio.StaticLock"

        fun callMe(context: android.content.Context, storeId: Long) {
            EfficioService.acquireStaticLock(context)
            val i = Intent(context, EfficioService::class.java)
            i.putExtra("storeId", storeId)
            context.startService(i)
        }

        fun acquireStaticLock(context: android.content.Context): PowerManager.WakeLock {
            val l = getLock(context)
            l.acquire()
            return l
        }

        private fun getLock(context: Context): PowerManager.WakeLock {
            synchronized(this, {
                val lock = if (lockStatic == null) {
                    val mgr = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                    val l = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LOCK_NAME_STATIC)
                    l.setReferenceCounted(true)
                    lockStatic = l
                    l
                } else {
                    lockStatic
                }
                return lock as PowerManager.WakeLock
            })
        }
    }

    override fun onHandleIntent(intent: Intent) {
        try {
            processTasks(intent.getLongExtra("storeId", -1))
        } finally {
            if (getLock(this).isHeld) {
                getLock(this).release()
            }
        }
        stopSelf()
    }

    private fun processTasks(storeId: Long) {
        synchronized(this, {
            Log.d(TAG, "processTasks")
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
        })
    }
}