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
import fr.geobert.efficio.misc.map
import java.util.*
import kotlin.properties.Delegates

class TaskListWidgetFactory(val ctx: Context, intent: Intent) : RemoteViewsService.RemoteViewsFactory {
    val TAG = "TaskListWidgetFactory"
    val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID)
    var tasksList: MutableList<Task> by Delegates.notNull()

    val storeId = 1L // todo use widgetId to get storeID

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
        //fetchStoreTask(storeId)
    }

    private fun fetchStoreTask(storeId: Long) {
        Log.d(TAG, "fetchStoreTask")
        val token = Binder.clearCallingIdentity();
        // todo : use widgetId here
        try {
            val cursor = TaskTable.getAllNotDoneTasksForStore(ctx, storeId)
            Log.d(TAG, "cursor count : ${cursor?.count}")
            if (cursor != null && cursor.count > 0) {
                tasksList = cursor.map { Task(it) }
            } else {
                tasksList = LinkedList()
            }
            cursor?.close()
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    override fun getItemId(position: Int): Long {
        Log.d(TAG, "getItemId : $position = ${tasksList[position].id}")
        return tasksList[position].id
    }

    override fun onDataSetChanged() {
        Log.d(TAG, "onDataSetChanged")
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