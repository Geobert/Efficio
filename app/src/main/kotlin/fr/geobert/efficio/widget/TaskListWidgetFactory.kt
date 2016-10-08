package fr.geobert.efficio.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import fr.geobert.efficio.R
import fr.geobert.efficio.data.Task
import fr.geobert.efficio.db.TaskTable
import fr.geobert.efficio.db.WidgetTable
import fr.geobert.efficio.misc.map
import java.util.*
import kotlin.properties.Delegates

class TaskListWidgetFactory(val ctx: Context, intent: Intent) : RemoteViewsService.RemoteViewsFactory {
    val TAG = "TaskListWidgetFactory"
    val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID)
    var tasksList: MutableList<Task> = LinkedList()

    var storeId: Long by Delegates.notNull()
    var opacity: Float by Delegates.notNull()
    var storeName: String by Delegates.notNull()

    override fun getLoadingView(): RemoteViews? {
        return null // todo a true loading view?
    }

    override fun getViewAt(position: Int): RemoteViews? {
        Log.d(TAG, "getViewAt $position")
        val t = tasksList[position]

        val rv = RemoteViews(ctx.packageName, R.layout.widget_task_row)
        rv.setTextViewText(R.id.name, t.item.name)
        rv.setTextViewText(R.id.dep_name, t.item.department.name)

        val intent = Intent()
        intent.putExtra("taskId", t.id)
        //val pIntent = PendingIntent.getActivity(ctx, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        rv.setOnClickFillInIntent(R.id.task_btn, intent)
        //rv.setOnClickPendingIntent(R.id.checkbox, pIntent)
        return rv
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun onCreate() {
        Log.d(TAG, "onCreate")
    }

    private fun fetchWidgetInfo(widgetId: Int): Boolean {
        val cursor = WidgetTable.getWidgetInfo(ctx, widgetId)
        if (cursor != null && cursor.count > 0 && cursor.moveToFirst()) {
            storeId = cursor.getLong(0)
            opacity = cursor.getFloat(1)
            storeName = cursor.getString(2)

            Log.d(TAG, "fetchWidgetInfo: storeId:$storeId, opacity:$opacity")
            return true
        }
        Log.e(TAG, "fetchWidgetInfo: failed")
        return false
    }

    private fun fetchStoreTask(storeId: Long) {
        Log.d(TAG, "fetchStoreTask, store:$storeId")
        val token = Binder.clearCallingIdentity()
        try {
            val cursor = TaskTable.getAllNotDoneTasksForStore(ctx, storeId)
            Log.d(TAG, "cursor count : ${cursor?.count}")
            if (cursor != null && cursor.count > 0) {
                tasksList = cursor.map(::Task)
            } else {
                tasksList = LinkedList()
            }
            cursor?.close()
        } finally {
            Binder.restoreCallingIdentity(token)
        }
    }

    override fun getItemId(position: Int): Long {
        Log.d(TAG, "getItemId : $position = ${tasksList[position].id}")
        return tasksList[position].id
    }

    override fun onDataSetChanged() {
        Log.d(TAG, "onDataSetChanged for widgetId: $widgetId")
        if (fetchWidgetInfo(widgetId))
            fetchStoreTask(storeId)
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    override fun getCount(): Int {
        Log.d(TAG, "getCount: ${tasksList.count()}")
        return tasksList.count()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
    }

}