package fr.geobert.efficio

import android.app.LoaderManager
import android.content.Context
import android.content.Intent
import android.content.Loader
import android.content.SharedPreferences
import android.database.Cursor
import android.os.Bundle
import android.support.v4.content.CursorLoader
import android.support.v7.app.ActionBarDrawerToggle
import android.view.View
import android.widget.AdapterView
import com.crashlytics.android.Crashlytics
import fr.geobert.efficio.adapter.StoreAdapter
import fr.geobert.efficio.data.Store
import fr.geobert.efficio.db.StoreTable
import fr.geobert.efficio.dialog.DeleteConfirmationDialog
import fr.geobert.efficio.dialog.StoreNameDialog
import fr.geobert.efficio.misc.*
import io.fabric.sdk.android.Fabric
import kotlinx.android.synthetic.main.main_activity.*
import kotlinx.android.synthetic.main.toolbar.*
import java.util.*
import kotlin.properties.Delegates

class MainActivity : BaseActivity(), LoaderManager.LoaderCallbacks<Cursor>,
        DeleteDialogInterface {
    private var lastStoreId: Long by Delegates.notNull()
    private var taskListFrag: TaskListFragment by Delegates.notNull()
    private var storeLoader: CursorLoader? = null
    private var storesList: MutableList<Store> = LinkedList()
    private var currentStore: Store by Delegates.notNull()
    private var storeAdapter: StoreAdapter by Delegates.notNull()
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!BuildConfig.DEBUG) Fabric.with(this, Crashlytics());
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
        fetchAllStores()
        store_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // nothing
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                lastStoreId = id
                prefs.edit().putLong("lastStoreId", id).commit()
                currentStore = storesList[position]
                refreshTaskList(id)
            }

        }
    }

    override fun onDeletedConfirmed() {
        StoreTable.deleteStore(this, currentStore)
        // now that currentStore is deleted, select another one, in none, create a default one
        storesList.remove(currentStore)
        storeAdapter.deleteStore(lastStoreId)
        if (storesList.count() > 0) {
            currentStore = storesList[0]
            lastStoreId = currentStore.id
            prefs.edit().putLong("lastStoreId", lastStoreId).commit()
            refreshTaskList(lastStoreId)
        } else {
            StoreTable.create(this, getString(R.string.store))
            fetchAllStores()
        }
    }

    fun refreshTaskList(id: Long) {
        val intent = Intent(OnRefreshReceiver.REFRESH_ACTION)
        intent.putExtra("newStoreId", id)
        sendBroadcast(intent)
    }

    private fun fetchAllStores() {
        if (storeLoader == null) {
            loaderManager.initLoader(GET_ALL_STORES, Bundle(), this)
        } else {
            loaderManager.restartLoader(GET_ALL_STORES, Bundle(), this)
        }
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
        storeAdapter.renameStore(name, lastStoreId)
    }

    private fun callCreateNewStore() {
        val d = StoreNameDialog.newInstance(getString(R.string.choose_new_store_name),
                getString(R.string.create_new_store), getString(R.string.store), 0, CREATE_STORE)
        d.show(fragmentManager, "createStore")
    }

    fun storeCreated(store: Store) {
        fetchAllStores()
    }

    private fun callEditDepartment() {
        EditDepartmentsActivity.callMe(taskListFrag, lastStoreId)
    }

    // cursor loading
    override fun onLoadFinished(loader: Loader<Cursor>, cursor: Cursor) {
        when (loader.id) {
            GET_ALL_STORES -> {
                if (cursor.count > 0) {
                    storesList = cursor.map { Store(it) }
                } else {
                    storesList.clear();
                }
                storeAdapter = StoreAdapter(this, storesList)
                store_spinner.adapter = storeAdapter
                if (storesList.count() == 1) {
                    currentStore = storesList[0]
                    lastStoreId = currentStore.id
                } else {
                    currentStore = storesList.find { it.id == lastStoreId } as Store
                }
            }
        }
    }

    override fun onLoaderReset(p0: Loader<Cursor>?) {

    }

    override fun onCreateLoader(p0: Int, p1: Bundle?): Loader<Cursor>? {
        return StoreTable.getAllStoresLoader(this)
    }
}