package fr.geobert.efficio.widget

import android.content.Intent
import android.util.Log
import android.widget.RemoteViewsService

class TaskListWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        Log.d("TaskListWidgetService", "TaskListWidgetService ok")
        return TaskListWidgetFactory(applicationContext, intent)
    }
}