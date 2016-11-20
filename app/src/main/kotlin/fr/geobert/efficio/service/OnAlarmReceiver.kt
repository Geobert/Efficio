package fr.geobert.efficio.service

import android.content.*
import android.util.Log

class OnAlarmReceiver : BroadcastReceiver() {

    private val startServiceLock = Object()

    override fun onReceive(context: Context, intent: Intent) {
        synchronized(startServiceLock) {
            Log.d("OnAlarmReceiver", "onReceive")
            EfficioService.acquireStaticLock(context)
            context.startService(Intent(context, EfficioService::class.java))
        }
    }

}
