package fr.geobert.efficio

import android.app.Fragment
import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.content.ContextCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import com.crashlytics.android.Crashlytics
import fr.geobert.efficio.data.Store
import fr.geobert.efficio.data.StoreLoaderListener
import fr.geobert.efficio.data.StoreManager
import fr.geobert.efficio.db.DbContentProvider
import fr.geobert.efficio.db.StoreTable
import fr.geobert.efficio.dialog.DeleteConfirmationDialog
import fr.geobert.efficio.dialog.StoreNameDialog
import fr.geobert.efficio.misc.*
import fr.geobert.efficio.service.EfficioService
import fr.geobert.efficio.service.InstallServiceReceiver
import fr.geobert.efficio.service.OnAlarmReceiver
import io.fabric.sdk.android.Fabric
import kotlinx.android.synthetic.main.main_activity.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlin.properties.Delegates

class MainActivity : BaseActivity(), DeleteDialogInterface, StoreLoaderListener,
        SharedPreferences.OnSharedPreferenceChangeListener {
    private var lastStoreId: Long by Delegates.notNull()
    private var taskListFrag: TaskListFragment? = null
    private var currentStore: Store by Delegates.notNull()
    private var storeManager: StoreManager = StoreManager(this, this)
    private val TAG = "MainActivity"

    val prefs: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    private val mDrawerToggle: ActionBarDrawerToggle by lazy {
        object : ActionBarDrawerToggle(this, /* host Activity */
                drawer_layout, /* DrawerLayout object */
                mToolbar,
                R.string.drawer_open, /* "open drawer" description for accessibility */
                R.string.drawer_close  /* "close drawer" description for accessibility */) {

            override fun onDrawerClosed(drawerView: View?) {
                invalidateOptionsMenu() // calls onPrepareOptionsMenu()
            }

            override fun onDrawerOpened(drawerView: View?) {
                invalidateOptionsMenu() // calls onPrepareOptionsMenu()
            }

        }
    }

    private fun cleanDatabaseIfTestingMode() {
        // if run by espresso, delete database, can't do it from test, doesn't work
        // see https://stackoverflow.com/questions/33059307/google-espresso-delete-user-data-on-each-test
        if (TEST_MODE) {
            //DBPrefsManager.getInstance(this).resetAll()
            Log.d(TAG, "ERASING DB")
            val client = contentResolver.acquireContentProviderClient("fr.geobert.efficio")
            val provider = client.localContentProvider as DbContentProvider
            provider.deleteDatabase(this)
            @Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT < 24) client.release() else client.close()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cleanDatabaseIfTestingMode()
        if (!BuildConfig.DEBUG)
            Fabric.with(this, Crashlytics())
        setContentView(R.layout.main_activity)
        PreferenceManager.setDefaultValues(this, R.xml.settings, false)
        title = ""
        if (savedInstanceState == null) {
            taskListFrag = TaskListFragment()
            fragmentManager.beginTransaction().replace(R.id.flContent, taskListFrag, "tasksList").commit()
        }
        setSupportActionBar(mToolbar)
        setUpDrawerToggle()
        setupDrawerContent()
        lastStoreId = prefs.getLong("lastStoreId", 1)
        prefs.registerOnSharedPreferenceChangeListener(this)
        installServiceTimer()
    }

    private fun installServiceTimer() {
        val pi = PendingIntent.getBroadcast(this, 0, Intent(this, OnAlarmReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE)
        if (pi == null) { // no alarm set
            val intent = Intent(this, InstallServiceReceiver::class.java)
            intent.action = "fr.geobert.efficio.INSTALL_TIMER"
            sendBroadcast(intent)
        }
        EfficioService.callMe(this, lastStoreId)
    }

    override fun onResume() {
        super.onResume()
        if (taskListFrag == null) {
            taskListFrag = fragmentManager.findFragmentByTag("tasksList") as TaskListFragment
        }
        store_spinner.onItemSelectedListener = null
        lastStoreId = prefs.getLong("lastStoreId", 1)
        storeManager.fetchAllStores()

        store_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // nothing
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                lastStoreId = id
                prefs.edit().putLong("lastStoreId", id).apply()
                currentStore = storeManager.storesList[position]
                refreshTaskList(this@MainActivity, id)
            }

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        prefs.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onDeletedConfirmed() {
        StoreTable.deleteStore(this, currentStore)
        // now that currentStore is deleted, select another one, in none, create a default one
        storeManager.storesList.remove(currentStore)
        storeManager.storeAdapter.deleteStore(lastStoreId)
        if (storeManager.storesList.count() > 0) {
            currentStore = storeManager.storesList[0]
            lastStoreId = currentStore.id
            prefs.edit().putLong("lastStoreId", lastStoreId).apply()
            refreshTaskList(this, lastStoreId)
        } else {
            StoreTable.create(this, getString(R.string.store))
            storeManager.fetchAllStores()
        }
    }

    fun refreshTask(taskId: Long) {
        val intent = Intent(OnRefreshReceiver.REFRESH_ACTION)
        intent.putExtra("storeId", lastStoreId)
        intent.putExtra("taskId", taskId)
        sendBroadcast(intent)
    }

    private fun setUpDrawerToggle() {
        // Defer code dependent on restoration of previous instance state.
        // NB: required for the drawer indicator to show up!
        drawer_layout.addDrawerListener(mDrawerToggle)
        drawer_layout.post({ mDrawerToggle.syncState() })
    }

    private fun setupDrawerContent() {
        nvView.setNavigationItemSelectedListener({ item ->
            when (item.itemId) {
                R.id.edit_departments -> callEditDepartment()
                R.id.create_new_store -> callCreateNewStore()
                R.id.rename_store -> callRenameStore()
                R.id.delete_current_store -> callDeleteStore()
                R.id.settings -> callSettings()
            }
            drawer_layout.closeDrawer(nvView)
            true
        })
    }

    private fun callSettings() {
        SettingsActivity.callMe(this)
    }

    private fun callDeleteStore() {
        val d = DeleteConfirmationDialog.newInstance(getString(R.string.confirm_delete_store).
                format(currentStore.name), getString(R.string.delete_current_store), DELETE_STORE)
        d.show(fragmentManager, "deleteStore")
    }

    private fun callRenameStore() {
        val d = StoreNameDialog.newInstance(getString(R.string.choose_new_store_name),
                getString(R.string.rename_current_store), currentStore.name, lastStoreId,
                RENAME_STORE)
        d.show(fragmentManager, "renameStore")
    }

    fun storeRenamed(name: String) {
        currentStore.name = name
        storeManager.renameStore(name, lastStoreId)
        updateWidgets()
    }

    private fun callCreateNewStore() {
        val d = StoreNameDialog.newInstance(getString(R.string.choose_new_store_name),
                getString(R.string.create_new_store), getString(R.string.store), 0, CREATE_STORE)
        d.show(fragmentManager, "createStore")
    }

    fun storeCreated() {
        storeManager.fetchAllStores()
    }

    private fun callEditDepartment() {
        EditDepartmentsActivity.callMe(taskListFrag as Fragment, lastStoreId)
    }

    override fun onStoreLoaded() {
        store_spinner.adapter = storeManager.storeAdapter
        if (storeManager.storesList.count() == 1) {
            currentStore = storeManager.storesList[0]
            lastStoreId = currentStore.id
            title = currentStore.name
            titleColor = ContextCompat.getColor(this, R.color.colorPrimaryDark)
            setSpinnerVisibility(View.GONE)
        } else {
            currentStore = storeManager.storesList.find { it.id == lastStoreId } as Store
            store_spinner.setSelection(storeManager.indexOf(lastStoreId))
            store_spinner.background.setColorFilter(ContextCompat.getColor(this,
                    R.color.colorPrimaryDark), PorterDuff.Mode.SRC_ATOP)
            setSpinnerVisibility(View.VISIBLE)
            title = ""
        }
    }

    fun updateWidgets() {
        Log.d(TAG, "updateWidgets")
        updateWidgets(this)
    }

    override fun onSharedPreferenceChanged(preferences: SharedPreferences, key: String) {
        when (key) {
            "invert_checkbox_pref", "invert_list_pref" -> refreshTaskList(this, lastStoreId)
        }
    }

    companion object {
        var TEST_MODE: Boolean = false
    }
}