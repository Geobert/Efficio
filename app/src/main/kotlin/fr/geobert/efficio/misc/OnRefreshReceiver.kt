package fr.geobert.efficio.misc

import android.content.*

class OnRefreshReceiver(val toRefresh: RefreshInterface) : BroadcastReceiver() {
    companion object {
        val REFRESH_ACTION = "fr.geobert.efficio.ACTION_REFRESH"
    }

    override fun onReceive(ctx: Context, intent: Intent?) {
        if (intent != null && intent.action == REFRESH_ACTION) {
            toRefresh.onRefresh(intent)
        }
    }
}