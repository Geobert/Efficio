package fr.geobert.efficio.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class OnAlarmReceiver : BroadcastReceiver() {

    private val startServiceLock = Object()

    override fun onReceive(context: Context, intent: Intent) {
        synchronized(startServiceLock) {
            Log.d("OnAlarmReceiver", "onReceive")
            EfficioJobService.enqueueWork(context, -1)
        }
    }

}
