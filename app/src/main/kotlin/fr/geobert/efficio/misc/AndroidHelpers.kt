package fr.geobert.efficio.misc

import android.appwidget.AppWidgetManager
import android.content.*
import fr.geobert.efficio.widget.TaskListWidget

inline fun consume(f: () -> Unit): Boolean {
    f()
    return true
}

fun updateWidgets(ctx: Context) {
    val appWidgetManager = AppWidgetManager.getInstance(ctx)

    val intent = Intent(ctx, TaskListWidget::class.java)
    intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE

    val thisWidget = ComponentName(ctx, TaskListWidget::class.java)
    val ids = appWidgetManager.getAppWidgetIds(thisWidget)

    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
    ctx.sendBroadcast(intent)
}

fun refreshTaskList(ctx: Context, storeId: Long) {
    val intent = Intent(OnRefreshReceiver.REFRESH_ACTION)
    intent.putExtra("newStoreId", storeId)
    ctx.sendBroadcast(intent)
}
