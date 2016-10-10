package fr.geobert.efficio.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import fr.geobert.efficio.BaseActivity
import fr.geobert.efficio.R
import fr.geobert.efficio.data.StoreLoaderListener
import fr.geobert.efficio.data.StoreManager
import fr.geobert.efficio.db.WidgetTable
import fr.geobert.efficio.misc.EditorToolbarTrait
import fr.geobert.efficio.misc.consume
import kotlinx.android.synthetic.main.toolbar.*

class WidgetSettingsActivity : BaseActivity(), StoreLoaderListener, EditorToolbarTrait {
    companion object {
        val FIRST_CONFIG = "FirstConfig"
    }

    private val TAG = "WidgetSettingsActivity"
    private val storeManager = StoreManager(this, this)

    private val widgetId by lazy {
        intent.extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID)
    }

    private val isFirstConfig by lazy { intent.extras.getBoolean(FIRST_CONFIG, true) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.widget_settings_activity)
        //setSupportActionBar(mToolbar)
        initToolbar(this)
    }

    override fun onResume() {
        super.onResume()
        storeManager.fetchAllStores()
    }

    override fun onStoreLoaded() {
        storeManager.storeAdapter.darkText = true
        store_spinner.adapter = storeManager.storeAdapter
    }

    override fun onMenuItemClick(item: MenuItem): Boolean = when (item.itemId) {
        R.id.confirm -> consume { onOkClicked() }
        else -> super.onOptionsItemSelected(item)
    }

    private fun onOkClicked() {
        saveValues()
        updateWidget()
        finish()
    }

    private fun saveValues() {
        val prefs = getSharedPreferences("widgetPrefs", Context.MODE_PRIVATE)
        if (isFirstConfig) {
            if (WidgetTable.create(this, widgetId, store_spinner.selectedItemId, 0.8f) <= 0) {
                // todo err management
                Log.e(TAG, "ERROR creating widget in DB")
            }
            prefs.edit().putBoolean("isConfigured_$widgetId", true).commit()
        } else {
            if (WidgetTable.update(this, widgetId, store_spinner.selectedItemId, 0.8f) <= 0) {
                // todo err management
                Log.e(TAG, "ERROR updating widget in DB")
            }
        }
    }

    private fun updateWidget() {
        val intent = Intent(this, TaskListWidget::class.java)

        Log.d("widgetSettings", "try to updateWidget: $widgetId")
        intent.action = "android.appwidget.action.APPWIDGET_UPDATE"
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(widgetId))
        setResult(RESULT_OK, intent)
        sendBroadcast(intent)

//        val result = Intent()
//        result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
//        Log.d("widgetSettings", "try to updateWidget: $widgetId")
//        setResult(RESULT_OK, result)
//
//        val remView = RemoteViews(applicationContext.packageName, R.id.widget_layout)
//        val mgr = AppWidgetManager.getInstance(applicationContext)
//        mgr.updateAppWidget(widgetId, remView)
        //mgr.notifyAppWidgetViewDataChanged(widgetId, R.id.tasks_list_widget)
        //TaskListWidget.instance!!.onUpdate(applicationContext, mgr, intArrayOf(widgetId))
    }

}