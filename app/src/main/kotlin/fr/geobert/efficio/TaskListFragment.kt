package fr.geobert.efficio


import android.app.Activity
import android.app.Fragment
import android.app.LoaderManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.CursorLoader
import android.content.Intent
import android.content.IntentFilter
import android.content.Loader
import android.database.Cursor
import android.os.Bundle
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.helper.ItemTouchHelper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import fr.geobert.efficio.adapter.TaskAdapter
import fr.geobert.efficio.adapter.TaskViewHolder
import fr.geobert.efficio.data.Department
import fr.geobert.efficio.data.DepartmentManager
import fr.geobert.efficio.data.Item
import fr.geobert.efficio.data.Store
import fr.geobert.efficio.data.Task
import fr.geobert.efficio.db.ItemDepTable
import fr.geobert.efficio.db.ItemTable
import fr.geobert.efficio.db.ItemWeightTable
import fr.geobert.efficio.db.TaskTable
import fr.geobert.efficio.dialog.DepartmentChoiceDialog
import fr.geobert.efficio.dialog.QuantityDialog
import fr.geobert.efficio.misc.CREATE_TASK
import fr.geobert.efficio.misc.GET_TASKS_OF_STORE
import fr.geobert.efficio.misc.RefreshInterface
import fr.geobert.efficio.misc.TopBottomSpaceItemDecoration
import fr.geobert.efficio.misc.map
import fr.geobert.efficio.widget.TaskListWidget
import kotlinx.android.synthetic.main.item_list_fragment.*
import java.util.*

class TaskListFragment : Fragment(), LoaderManager.LoaderCallbacks<Cursor>, TextWatcher,
        DepartmentManager.DepartmentChoiceListener, TaskViewHolder.TaskViewHolderListener,
        RefreshInterface {
    private val TAG = "TaskListFragment"

    var lastStoreId: Long = 1 // todo get it from prefs
    var currentStore: Store? = null
    var cursorLoader: CursorLoader? = null
    var taskAdapter: TaskAdapter? = null
    var tasksList: MutableList<Task> = LinkedList()
    val refreshReceiver = OnRefreshReceiver(this)

    private val header = Task()

    // this manage drag and swipe on tasks
    private val dragSwipeHlp by lazy { TaskDragSwipeHelper(this, tasksList, taskAdapter!!) }
    private val taskItemTouchHlp by lazy { ItemTouchHelper(dragSwipeHlp) }

    fun updateTasksList(needAdapterSort: Boolean) {
        val f = quick_add_text.text.trim().toString()
        if (needAdapterSort || !f.isEmpty()) {
            val l = if (!f.isEmpty()) filter(tasksList, f) else tasksList
            taskAdapter!!.animateTo(l)
            tasks_list.post {
                tasks_list.invalidateItemDecorations()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup,
                              savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.item_list_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        tasks_list.layoutManager = LinearLayoutManager(this.activity)
        tasks_list.itemAnimator = DefaultItemAnimator()
        tasks_list.setHasFixedSize(true)
        tasks_list.addItemDecoration(TopBottomSpaceItemDecoration(10))

        quick_add_btn.setOnClickListener {
            onAddTaskClicked()
        }

        quick_add_text.addTextChangedListener(this)
        quick_add_text.setOnEditorActionListener { textView, i, keyEvent -> onEditorAction(textView, i, keyEvent) }

        fetchStore(this, lastStoreId)

        activity.registerReceiver(refreshReceiver, IntentFilter(OnRefreshReceiver.REFRESH_ACTION))
    }

    override fun onResume() {
        super.onResume()
        quick_add_btn.isEnabled = quick_add_text.text.length > 0
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activity.unregisterReceiver(refreshReceiver)
    }

    private fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            onAddTaskClicked()
            return true
        }
        return false
    }

    private fun onAddTaskClicked() {
        val (t, pos) = taskAdapter!!.getTaskByName(quick_add_text.text.trim().toString())
        if (t != null) {
            t.isDone = false
            taskAdapter!!.notifyItemChanged(pos)
        } else {
            // case item does not exists yet
            createNewTask()
        }
    }

    private fun createNewTask() {
        val d = DepartmentChoiceDialog.newInstance(lastStoreId)
        d.setTargetFragment(this, CREATE_TASK)
        d.show(fragmentManager, "DepChoiceDialog")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            0 -> onItemEditFinished(resultCode == Activity.RESULT_OK)
            1 -> onDepEditFinished(resultCode == Activity.RESULT_OK)
        }

    }

    private fun onDepEditFinished(needUpdate: Boolean) {
        if (needUpdate) {
            fetchStore(this, lastStoreId)
            updateWidgets()
        }
    }

    fun onItemEditFinished(needUpdate: Boolean) {
        if (needUpdate) {
            quick_add_text.text.clear()
            fetchStore(this, lastStoreId)
            updateWidgets()
        }
    }

//
// DepartmentChoiceListener
//

    override fun onDepartmentChosen(d: Department) {
        Log.d(TAG, "onDepartmentChosen : ${d.name}")
        // we choose a department, so the task does not exist
        val maxWeightForDep = findMaxWeightForDepartment(d)
        val i = Item(quick_add_text.text.trim().toString(), d, maxWeightForDep + 1.0)
        i.id = ItemTable.create(activity, i)
        if (i.id > 0) {
            // add to adapter, but need to find the right position
            val t = Task(i)
            tasksList.add(t)
            tasksList.sort()
            taskAdapter!!.animateTo(tasksList)
            quick_add_text.text.clear()

            if (ItemWeightTable.create(activity, i, lastStoreId) > 0) {
                if (ItemDepTable.create(activity, i, lastStoreId) > 0) {
                    if (TaskTable.create(activity, t, lastStoreId) > 0) {
                        updateWidgets()
                    } else {
                        Log.e(TAG, "error on creating task")
                    }
                }
            } else {
                Log.e(TAG, "error on item weight creation")
            }
        } else {
            Log.e(TAG, "error on item creation")
        }

    }

    private fun findMaxWeightForDepartment(d: Department): Double {
        val filteredByDep = tasksList.filter { t -> t.item.department.id == d.id }
        return if (filteredByDep.size > 0) filteredByDep.last().item.weight else 0.0
    }

    fun updateWidgets() {
        Log.d(TAG, "updateWidgets")
        val appWidgetManager = AppWidgetManager.getInstance(activity)

        val intent = Intent(activity, TaskListWidget::class.java)
        intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE

        val thisWidget = ComponentName(activity, TaskListWidget::class.java)
        val ids = appWidgetManager.getAppWidgetIds(thisWidget)

        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        ids.forEach { appWidgetManager.notifyAppWidgetViewDataChanged(it, R.id.tasks_list_widget) }
    }

    override fun onDoneStateChanged(task: Task) {
        TaskTable.updateDoneState(activity, task.id, task.isDone)
        tasksList.sort()
        addHeaderIfNeeded(tasksList)
        val layMan = (tasks_list.layoutManager as LinearLayoutManager)
        var pos = layMan.findFirstVisibleItemPosition()
        taskAdapter!!.animateTo(tasksList)
        if (!task.isDone) pos = taskAdapter!!.getTaskPosition(task.id)
        tasks_list.scrollToPosition(pos)
        tasks_list.post { tasks_list.invalidateItemDecorations() }
        updateWidgets()
    }

    override fun onItemClicked(task: Task) {
        ItemEditorActivity.callMe(this, lastStoreId, task)
    }

    override fun onQtyClicked(task: Task) {
        QuantityDialog.newInstance(task).show(fragmentManager, "QtyEdit")
    }

    //
// TextWatcher
//
    override fun afterTextChanged(s: Editable) {
        quick_add_btn.isEnabled = s.trim().length > 0
        if (tasksList.count() > 0) {
            val filteredList = filter(tasksList, s.toString())
            addHeaderIfNeeded(filteredList)
            taskAdapter!!.animateTo(filteredList)
            tasks_list.post {
                tasks_list.invalidateItemDecorations()
            }
            tasks_list.scrollToPosition(0)
        }
    }

    private fun filter(list: MutableList<Task>, s: String): MutableList<Task> {
        val f = s.toLowerCase()
        val filtered = LinkedList<Task>()
        for (t in list) {
            if ((t.type == TaskAdapter.VIEW_TYPES.Normal && t.item.name.toLowerCase().contains(f)) ||
                    t.type == TaskAdapter.VIEW_TYPES.Header) {
                filtered.add(t)
            }
        }
        return filtered
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

    }

    override fun onRefresh(intent: Intent) {
        val extras = intent.extras
        val storeId = extras.getLong("storeId", -1)
        val newStoreId = extras.getLong("newStoreId", -1)
        val taskId = extras.getLong("taskId", -1)
        if (newStoreId > 0) lastStoreId = newStoreId
        if (taskId > -1) { // for the moment, this is only when a quantity has been edited via dialog
            taskAdapter!!.refreshTaskFromDB(activity, taskId)
        } else if (storeId == lastStoreId || storeId < 0L) {
            fetchStore(this, lastStoreId)
        }
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
        cursorLoader = when (i) {
            GET_TASKS_OF_STORE -> TaskTable.getAllTasksForStoreLoader(this.activity,
                    bundle?.getLong("storeId") ?: 0)
            else -> null
        }
        return cursorLoader
    }

    private fun addHeaderIfNeeded(list: MutableList<Task>) {
        var lastState: Boolean? = null
        var addPos: Int? = null
        list.remove(header)
        for (t in list) {
            if (t.isDone && lastState == null) {
                addPos = 0
            } else {
                if (lastState == false && t.isDone == true) {
                    addPos = list.indexOf(t)
                    break
                }
            }
            lastState = t.isDone
        }

        if (addPos != null) {
            list.add(addPos, header)
        }
    }

    override fun onLoadFinished(cursorLoader: Loader<Cursor>, cursor: Cursor) {
        when (cursorLoader.id) {
            GET_TASKS_OF_STORE -> {
                if (cursor.count > 0) {
                    tasksList = cursor.map(::Task)
                    tasks_list.visibility = View.VISIBLE
                    empty_text.visibility = View.GONE
                } else {
                    tasksList = LinkedList<Task>()
                    tasks_list.visibility = View.GONE
                    empty_text.visibility = View.VISIBLE
                }
                addHeaderIfNeeded(tasksList)
                if (taskAdapter == null)
                    taskAdapter = TaskAdapter(tasksList, this)
                else
                    taskAdapter!!.animateTo(tasksList)
                dragSwipeHlp.tasksList = tasksList
                tasks_list.adapter = taskAdapter
                taskItemTouchHlp.attachToRecyclerView(tasks_list)
            }
            else -> throw IllegalArgumentException("Unknown cursorLoader id")
        }
    }

    override fun onLoaderReset(cursorLoader: Loader<Cursor>?) {
        currentStore = null
        cursorLoader?.reset()
    }
}