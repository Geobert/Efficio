package fr.geobert.efficio.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.RemoteViews
import fr.geobert.efficio.BaseActivity
import fr.geobert.efficio.R
import fr.geobert.efficio.data.StoreLoaderListener
import fr.geobert.efficio.data.StoreManager
import fr.geobert.efficio.db.WidgetTable
import fr.geobert.efficio.misc.EditorToolbarTrait
import fr.geobert.efficio.misc.consume
import kotlinx.android.synthetic.main.toolbar.*

class WidgetSettingsActivity : BaseActivity(), StoreLoaderListener, EditorToolbarTrait {
    private val TAG = "WidgetSettingsActivity"
    private val storeManager = StoreManager(this, this)

    private val widgetId by lazy {
        intent.extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID)
    }

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
        if (WidgetTable.create(this, widgetId, store_spinner.selectedItemId, 0.8f) <= 0) {
            // todo err management
            Log.e(TAG, "ERROR creating widget in DB")
        }
        val prefs = getSharedPreferences("widgetPrefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("isConfigured_$widgetId", true).commit()
    }

    private fun updateWidget() {
        val result = Intent()
        result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        Log.d("widgetSettings", "try to updateWidget: $widgetId")
        setResult(RESULT_OK, result)

        val remView = RemoteViews(applicationContext.packageName, R.id.tasks_list_widget)
        val mgr = AppWidgetManager.getInstance(applicationContext)
        mgr.updateAppWidget(widgetId, remView)
        TaskListWidget.instance!!.onUpdate(applicationContext, mgr, intArrayOf(widgetId))
    }

}