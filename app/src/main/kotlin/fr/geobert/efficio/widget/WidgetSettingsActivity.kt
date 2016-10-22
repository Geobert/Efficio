package fr.geobert.efficio.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.SeekBar
import fr.geobert.efficio.BaseActivity
import fr.geobert.efficio.R
import fr.geobert.efficio.data.StoreLoaderListener
import fr.geobert.efficio.data.StoreManager
import fr.geobert.efficio.db.WidgetTable
import fr.geobert.efficio.misc.EditorToolbarTrait
import fr.geobert.efficio.misc.consume
import kotlinx.android.synthetic.main.widget_settings_activity.*


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

    private var opacity: Int = 80

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.widget_settings_activity)
        //setSupportActionBar(mToolbar)
        initToolbar(this)
        setTitle(R.string.title_activity_widget_settings)
        opacity_seekbar.max = 100
        opacity_seekbar.progress = opacity
        opacity_seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                opacity = p1
                setOpacityText(p1)
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {

            }

            override fun onStopTrackingTouch(p0: SeekBar?) {

            }
        })
    }

    override fun onResume() {
        super.onResume()
        storeManager.fetchAllStores()
    }

    override fun onStoreLoaded() {
        storeManager.storeAdapter.darkText = true
        store_spinner.adapter = storeManager.storeAdapter
        val info = WidgetTable.getWidgetInfo(this, widgetId)
        if (info != null && info.count > 0 && info.moveToFirst()) {
            val storeId = info.getLong(0)
            opacity = (info.getFloat(1) * 100).toInt()
            store_spinner.setSelection(storeManager.indexOf(storeId))
            opacity_seekbar.progress = opacity
            info.close()
        }
    }

    private fun setOpacityText(opacity: Int) {
        opacity_value_lbl.text = String.format("%d%%", opacity)
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
        val op = opacity / 100f
        if (isFirstConfig) {
            if (WidgetTable.create(this, widgetId, store_spinner.selectedItemId, op) <= 0) {
                // todo err management
                Log.e(TAG, "ERROR creating widget in DB")
            }
            prefs.edit().putBoolean("isConfigured_$widgetId", true).commit()
        } else {
            if (WidgetTable.update(this, widgetId, store_spinner.selectedItemId, op) <= 0) {
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
    }

}