package fr.geobert.efficio.service

import android.app.*
import android.content.*
import android.util.Log

class InstallServiceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("InstallServiceReceiver", "Install the timer")
        val mgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val i = Intent(context, OnAlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT)
        mgr.cancel(pi)
        mgr.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis() + 5000,
                AlarmManager.INTERVAL_HALF_DAY, pi)
    }
}