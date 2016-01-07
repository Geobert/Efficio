package fr.geobert.efficio

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.view.View
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric
import kotlinx.android.synthetic.main.main_activity.*
import kotlin.properties.Delegates

public class MainActivity : BaseActivity() {
    private var lastStoreId: Long = 1 // Todo, get it from prefs
    private var taskListFrag: TaskListFragment by Delegates.notNull()

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
        setTitle(R.string.app_name)
        titleColor = ContextCompat.getColor(this, android.R.color.primary_text_dark)
        if(savedInstanceState==null) {
            taskListFrag = TaskListFragment()
            fragmentManager.beginTransaction().replace(R.id.flContent, taskListFrag).commit()
        }
        setSupportActionBar(mToolbar)
        setUpDrawerToggle()
        setupDrawerContent()
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

    }

    private fun callRenameStore() {

    }

    private fun callCreateNewStore() {

    }

    private fun callEditDepartment() {
        EditDepartmentsActivity.callMe(taskListFrag, lastStoreId)
    }
}