package fr.geobert.efficio

import android.content.*
import android.os.*
import android.support.v7.app.ActionBarDrawerToggle
import android.view.View
import android.widget.AdapterView
import com.crashlytics.android.Crashlytics
import fr.geobert.efficio.data.*
import fr.geobert.efficio.db.*
import fr.geobert.efficio.dialog.*
import fr.geobert.efficio.misc.*
import io.fabric.sdk.android.Fabric
import kotlinx.android.synthetic.main.main_activity.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlin.properties.Delegates

class MainActivity : BaseActivity(), DeleteDialogInterface, StoreLoaderListener {
    private var lastStoreId: Long by Delegates.notNull()
    private var taskListFrag: TaskListFragment by Delegates.notNull()
    private var currentStore: Store by Delegates.notNull()
    private var storeManager: StoreManager = StoreManager(this, this)

    private val prefs: SharedPreferences by lazy { getPreferences(Context.MODE_PRIVATE) }

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
            val client = contentResolver.acquireContentProviderClient("fr.geobert.efficio")
            val provider = client.localContentProvider as DbContentProvider
            provider.deleteDatabase(this)
            if (Build.VERSION.SDK_INT < 24) client.release() else client.close()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cleanDatabaseIfTestingMode()
        if (!BuildConfig.DEBUG) Fabric.with(this, Crashlytics())
        setContentView(R.layout.main_activity)
        title = ""
        if (savedInstanceState == null) {
            taskListFrag = TaskListFragment()
            fragmentManager.beginTransaction().replace(R.id.flContent, taskListFrag).commit()
        }
        setSupportActionBar(mToolbar)
        setUpDrawerToggle()
        setupDrawerContent()
    }

    override fun onResume() {
        super.onResume()
        store_spinner.onItemSelectedListener = null
        lastStoreId = prefs.getLong("lastStoreId", 1)
        storeManager.fetchAllStores()
        store_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // nothing
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                lastStoreId = id
                prefs.edit().putLong("lastStoreId", id).commit()
                currentStore = storeManager.storesList[position]
                refreshTaskList(id)
            }

        }
    }

    override fun onDeletedConfirmed() {
        StoreTable.deleteStore(this, currentStore)
        // now that currentStore is deleted, select another one, in none, create a default one
        storeManager.storesList.remove(currentStore)
        storeManager.storeAdapter.deleteStore(lastStoreId)
        if (storeManager.storesList.count() > 0) {
            currentStore = storeManager.storesList[0]
            lastStoreId = currentStore.id
            prefs.edit().putLong("lastStoreId", lastStoreId).commit()
            refreshTaskList(lastStoreId)
        } else {
            StoreTable.create(this, getString(R.string.store))
            storeManager.fetchAllStores()
        }
    }

    fun refreshTaskList(id: Long) {
        val intent = Intent(OnRefreshReceiver.REFRESH_ACTION)
        intent.putExtra("newStoreId", id)
        sendBroadcast(intent)
    }

    private fun setUpDrawerToggle() {
        // Defer code dependent on restoration of previous instance state.
        // NB: required for the drawer indicator to show up!
        drawer_layout.setDrawerListener(mDrawerToggle)
        drawer_layout.post({ mDrawerToggle.syncState() })
    }

    private fun setupDrawerContent() {
        nvView.setNavigationItemSelectedListener({ item ->
            when (item.itemId) {
                R.id.edit_departments -> callEditDepartment()
                R.id.create_new_store -> callCreateNewStore()
                R.id.rename_store -> callRenameStore()
                R.id.delete_current_store -> callDeleteStore()
            }
            drawer_layout.closeDrawer(nvView)
            true
        })
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
        storeManager.storeAdapter.renameStore(name, lastStoreId)
    }

    private fun callCreateNewStore() {
        val d = StoreNameDialog.newInstance(getString(R.string.choose_new_store_name),
                getString(R.string.create_new_store), getString(R.string.store), 0, CREATE_STORE)
        d.show(fragmentManager, "createStore")
    }

    fun storeCreated(store: Store) {
        storeManager.fetchAllStores()
    }

    private fun callEditDepartment() {
        EditDepartmentsActivity.callMe(taskListFrag, lastStoreId)
    }

    override fun onStoreLoaded() {
        store_spinner.adapter = storeManager.storeAdapter
        if (storeManager.storesList.count() == 1) {
            currentStore = storeManager.storesList[0]
            lastStoreId = currentStore.id
        } else {
            currentStore = storeManager.storesList.find { it.id == lastStoreId } as Store
        }
    }

    companion object {
        var TEST_MODE: Boolean = false
    }
}