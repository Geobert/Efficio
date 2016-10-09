package fr.geobert.efficio.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.RemoteViews
import fr.geobert.efficio.OnRefreshReceiver
import fr.geobert.efficio.R
import fr.geobert.efficio.db.TaskTable
import fr.geobert.efficio.db.WidgetTable

/**
 * Implementation of App Widget functionality.
 */
class TaskListWidget : AppWidgetProvider() {
    val TAG = "TaskListWidget"

    companion object {
        val ACTION_CHECKBOX_CHANGED = "fr.geobert.efficio.action_checkbox_changed"
        var instance: AppWidgetProvider? = null
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // There may be multiple widgets active, so update all of them
        Log.d(TAG, "onUpdate: ${appWidgetIds.size}")
        val prefs = context.getSharedPreferences("widgetPrefs", Context.MODE_PRIVATE)
        for (appWidgetId in appWidgetIds) {
//            if (prefs.getBoolean("isConfigured_$appWidgetId", false))
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        instance = this
        val extras = intent.extras
        val widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID)
        Log.d(TAG, "onReceive: ${intent.action} / widgetId: $widgetId")
        val appWidgetManager = AppWidgetManager.getInstance(context)
        when (intent.action) {
            ACTION_CHECKBOX_CHANGED -> {
                TaskTable.updateDoneState(context, extras.getLong("taskId"), true)
                appWidgetManager.notifyAppWidgetViewDataChanged(
                        widgetId, R.id.tasks_list_widget)
                val i = Intent(OnRefreshReceiver.REFRESH_ACTION)
                //i.putExtra("storeId", ) // TODO get storeId according to widgetId
                context.sendBroadcast(i)
            }
            "android.appwidget.action.APPWIDGET_DELETED" ->
                if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID)
                    WidgetTable.delete(context, widgetId)
            "android.appwidget.action.APPWIDGET_UPDATE" -> {
                onUpdate(context, appWidgetManager, intArrayOf(widgetId))
                appWidgetManager.notifyAppWidgetViewDataChanged(widgetId, R.id.tasks_list_widget)
            }
        }
        super.onReceive(context, intent)
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    private fun getStoreName(ctx: Context, widgetId: Int): String {
        val cursor = WidgetTable.getWidgetInfo(ctx, widgetId)
        var res: String = ""
        if (cursor != null) {
            if (cursor.count > 0 && cursor.moveToFirst()) {
                res = cursor.getString(2)
            }
            cursor.close()
        }
        return res
    }

    fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        Log.d(TAG, "updateAppWidget $appWidgetId")

        val intent = Intent(context, TaskListWidgetService::class.java)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        intent.data = Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME))

        val name = getStoreName(context, appWidgetId)

        // Construct the RemoteViews object
        val views = RemoteViews(context.packageName, R.layout.task_list_widget)
        views.setTextViewText(R.id.widget_store_chooser_btn, name)
        views.setRemoteAdapter(R.id.tasks_list_widget, intent)
        views.setEmptyView(R.id.tasks_list_widget, R.id.empty_text_widget)

        val onClickIntent = Intent(context, TaskListWidget::class.java)
        onClickIntent.action = ACTION_CHECKBOX_CHANGED
        onClickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)

        val pIntent = PendingIntent.getBroadcast(context, 0, onClickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT)
        views.setPendingIntentTemplate(R.id.tasks_list_widget, pIntent)

        val launchIntent = Intent("android.intent.action.MAIN")
        launchIntent.addCategory("android.intent.category.LAUNCHER")
        launchIntent.component = ComponentName("fr.geobert.efficio", "fr.geobert.efficio.MainActivity")
        val lpIntent = PendingIntent.getActivity(context, 0, launchIntent, 0)
        views.setOnClickPendingIntent(R.id.widget_store_chooser_btn, lpIntent)

        val launchConfigIntent = Intent(context, WidgetSettingsActivity::class.java)
        launchConfigIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        launchConfigIntent.putExtra(WidgetSettingsActivity.FIRST_CONFIG, false)
        val cpIntent = PendingIntent.getActivity(context, 0, launchConfigIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        views.setOnClickPendingIntent(R.id.widget_settings_btn, cpIntent)

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}

