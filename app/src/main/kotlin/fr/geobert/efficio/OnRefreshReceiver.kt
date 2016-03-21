package fr.geobert.efficio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import fr.geobert.efficio.misc.RefreshInterface

class OnRefreshReceiver(val toRefresh: RefreshInterface) : BroadcastReceiver() {
    companion object {
        val REFRESH_ACTION = "fr.geobert.efficio.ACTION_REFRESH"
    }

    override fun onReceive(ctx: Context, intent: Intent?) {
        if (intent != null && intent.action.equals(REFRESH_ACTION)) {
            toRefresh.onRefresh(intent)
        }
    }
}