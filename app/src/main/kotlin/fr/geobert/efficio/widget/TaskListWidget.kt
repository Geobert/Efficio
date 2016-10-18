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
import kotlin.properties.Delegates

/**
 * Implementation of App Widget functionality.
 */
class TaskListWidget : AppWidgetProvider() {
    val TAG = "TaskListWidget"

    var opacity: Float by Delegates.notNull()
    var storeName by Delegates.notNull<String>()
    var storeId by Delegates.notNull<Long>()

    companion object {
        val ACTION_CHECKBOX_CHANGED = "fr.geobert.efficio.action_checkbox_changed"
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
        val extras = intent.extras
        Log.d(TAG, "onReceive: ${intent.action}")

        val appWidgetManager = AppWidgetManager.getInstance(context)
        when (intent.action) {
            ACTION_CHECKBOX_CHANGED -> {
                val widgetId = extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                        AppWidgetManager.INVALID_APPWIDGET_ID) ?: AppWidgetManager.INVALID_APPWIDGET_ID
                Log.d(TAG, "widgetId: $widgetId")
                TaskTable.updateDoneState(context, extras.getLong("taskId"), true)
                appWidgetManager.notifyAppWidgetViewDataChanged(
                        widgetId, R.id.tasks_list_widget)
                val i = Intent(OnRefreshReceiver.REFRESH_ACTION)
                i.putExtra("storeId", storeId)
                context.sendBroadcast(i)
            }
            "android.appwidget.action.APPWIDGET_DELETED" -> {
                val widgetId = extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                        AppWidgetManager.INVALID_APPWIDGET_ID) ?: AppWidgetManager.INVALID_APPWIDGET_ID
                Log.d(TAG, "widgetId: $widgetId")
                WidgetTable.delete(context, widgetId)
            }
            "android.appwidget.action.APPWIDGET_UPDATE" -> {
                val widgetIds = extras?.getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                if (widgetIds != null) for (widgetId in widgetIds)
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

    private fun fetchWidgetInfo(ctx: Context, widgetId: Int): Boolean {
        val cursor = WidgetTable.getWidgetInfo(ctx, widgetId)
        if (cursor != null && cursor.count > 0 && cursor.moveToFirst()) {
            storeId = cursor.getLong(0)
            opacity = cursor.getFloat(1)
            storeName = cursor.getString(2)
            return true
        }
        return false
    }

    fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        Log.d(TAG, "updateAppWidget $appWidgetId")

        val intent = Intent(context, TaskListWidgetService::class.java)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        intent.data = Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME))

        fetchWidgetInfo(context, appWidgetId)

        // Construct the RemoteViews object
        val views = RemoteViews(context.packageName, R.layout.task_list_widget)
        views.setInt(R.id.widget_background, "setAlpha", (opacity * 255).toInt())
        views.setTextViewText(R.id.widget_store_chooser_btn, storeName)
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

