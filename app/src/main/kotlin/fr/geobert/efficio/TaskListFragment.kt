package fr.geobert.efficio


import android.app.Fragment
import android.app.LoaderManager
import android.content.CursorLoader
import android.content.Loader
import android.database.Cursor
import android.os.Bundle
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import fr.geobert.efficio.adapter.TaskAdapter
import fr.geobert.efficio.data.Department
import fr.geobert.efficio.data.Store
import fr.geobert.efficio.data.Task
import fr.geobert.efficio.db.TaskTable
import fr.geobert.efficio.misc.map
import kotlinx.android.synthetic.main.item_list_fragment.*
import kotlin.properties.Delegates

class TaskListFragment : Fragment(), LoaderManager.LoaderCallbacks<Cursor>, TextWatcher,
        DepartmentChoiceDialog.DepartmentChoiceListener {
    private val GET_TASKS_OF_STORE = 100
    private val TAG = "TaskListFragment"

    var lastStoreId: Long = 1 // todo get it from prefs
    var currentStore: Store? = null
    var cursorLoader: CursorLoader? = null
    var taskAdapter: TaskAdapter by Delegates.notNull()
    var tasksList: MutableList<Task> by Delegates.notNull()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.item_list_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        tasks_list.layoutManager = LinearLayoutManager(this.activity)
        tasks_list.itemAnimator = DefaultItemAnimator()
        tasks_list.setHasFixedSize(true)

        quick_add_btn.setOnClickListener {
            onAddTaskClicked()
        }

        quick_add_text.addTextChangedListener(this)

        fetchStore(this, lastStoreId)
    }

    override fun onResume() {
        super.onResume()
        quick_add_btn.isEnabled = quick_add_text.text.length > 0
    }

    private fun onAddTaskClicked() {
        // todo case where item already exists

        // case item does not exists yet
        createNewTask()
    }

    private fun createNewTask() {
        val d = DepartmentChoiceDialog()
        d.listener = this
        val b = Bundle()
        b.putLong("storeId", lastStoreId)
        d.arguments = b
        d.show(fragmentManager, "DepChoiceDiag")
    }

    //
    // DepartmentChoiceListener
    //

    override fun onDepartmentChosen(d: Department) {
        Log.d(TAG, "onDepartmentChosen : ${d.name}")
    }

    override fun onChoiceCanceled() {

    }


    /// TextWatcher

    override fun afterTextChanged(s: Editable) {
        quick_add_btn.isEnabled = s.length > 0
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

    }

    //
    // Database operations
    //

    fun fetchStore(ctx: Fragment, storeId: Long) {
        val b = Bundle()
        b.putLong("storeId", storeId)
        if (cursorLoader == null) {
            ctx.loaderManager.initLoader(GET_TASKS_OF_STORE, b, this)
        } else {
            ctx.loaderManager.restartLoader(GET_TASKS_OF_STORE, b, this)
        }
    }

    override fun onCreateLoader(i: Int, bundle: Bundle?): Loader<Cursor>? {
        return when (i) {
            GET_TASKS_OF_STORE -> TaskTable.fetchAllTasksForStore(this.activity,
                    bundle?.getLong("storeId") ?: 0)
            else -> null
        }
    }

    override fun onLoadFinished(cursorLoader: Loader<Cursor>, cursor: Cursor) {
        when (cursorLoader.id) {
            GET_TASKS_OF_STORE -> {
                if (cursor.count > 0) {
                    tasksList = cursor.map { Task(it) }
                    taskAdapter = TaskAdapter(tasksList)
                    tasks_list.adapter = taskAdapter
                    tasks_list.visibility = View.VISIBLE
                    empty_text.visibility = View.GONE
                } else {
                    tasks_list.visibility = View.GONE
                    empty_text.visibility = View.VISIBLE
                }
            }
            else -> throw IllegalArgumentException("Unknown cursorLoader id")
        }
    }

    override fun onLoaderReset(cursorLoader: Loader<Cursor>?) {
        currentStore = null
        cursorLoader?.reset()
    }
}