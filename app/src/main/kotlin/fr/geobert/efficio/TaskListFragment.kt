package fr.geobert.efficio


import android.app.Activity
import android.app.Fragment
import android.app.LoaderManager
import android.appwidget.AppWidgetManager
import android.content.*
import android.database.Cursor
import android.os.Bundle
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import fr.geobert.efficio.adapter.TaskAdapter
import fr.geobert.efficio.adapter.TaskViewHolder
import fr.geobert.efficio.data.*
import fr.geobert.efficio.db.*
import fr.geobert.efficio.dialog.DepartmentChoiceDialog
import fr.geobert.efficio.misc.*
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

    private val taskItemTouchCbk =
            object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
                var lastDragTask: TaskViewHolder? = null
                var needAdapterSort: Boolean = false

                override fun onMove(recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder,
                                    target: RecyclerView.ViewHolder): Boolean {
                    Collections.swap(tasksList, viewHolder.adapterPosition, target.adapterPosition)
                    taskAdapter!!.notifyItemMoved(viewHolder.adapterPosition, target.adapterPosition)
                    val r = updateTaskWeight(viewHolder as TaskViewHolder, target as TaskViewHolder)
                    if (!needAdapterSort) needAdapterSort = r
                    return true
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder?, direction: Int) {
                    // nothing
                }

                private var orig: Float = 0f

                override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                    super.onSelectedChanged(viewHolder, actionState)
                    // end of drag n drop, adapter is correctly ordered but not our representation here
                    val vh = viewHolder as TaskViewHolder?
                    if (vh == null) {
                        lastDragTask!!.cardView.cardElevation = orig
                        Log.d(TAG, "end of drag n drop, sort the list")
                        StoreCompositionTable.updateDepWeight(activity, lastDragTask!!.task.item.department)
                        ItemWeightTable.updateWeight(activity, lastDragTask!!.task.item)
                        tasksList.sort()
                        if (needAdapterSort) {
                            val f = quick_add_text.text.trim().toString()
                            val l = if (f.length > 0) filter(tasksList, f) else tasksList
                            taskAdapter!!.animateTo(l)
                            tasks_list.post {
                                tasks_list.invalidateItemDecorations()
                            }
                        }
                    } else {
                        orig = vh.cardView.cardElevation
                        vh.cardView.cardElevation = 20.0f
                    }
                    lastDragTask = vh
                }
            }

    private val taskItemTouchHlp = ItemTouchHelper(taskItemTouchCbk)

    private fun updateTaskWeight(dragged: TaskViewHolder, target: TaskViewHolder): Boolean {
        val dTask = dragged.task
        val tTask = target.task
        val dItem = dTask.item
        val tItem = tTask.item
        val dDep = dItem.department
        val tDep = tItem.department
        var res: Boolean = false
        if (dragged.adapterPosition < target.adapterPosition) {
            // item goes up
            if (dDep.id == tDep.id) {
                if (dItem.weight == tItem.weight) {
                    dItem.weight++
                } else {
                    if (dItem.weight < tItem.weight) {
                        dItem.weight = tItem.weight + 1
                    }
                }
            } else {
                if (dDep.weight == tDep.weight) {
                    dDep.weight++
                    res = true
                } else if (dDep.weight < tDep.weight) {
                    dDep.weight = tDep.weight + 1
                    res = true
                }
            }
        } else if (dragged.adapterPosition > target.adapterPosition) {
            // item goes down
            if (dDep.id == tDep.id) {
                if (dItem.weight == tItem.weight) {
                    dItem.weight--
                } else {
                    if (dItem.weight > tItem.weight) {
                        dItem.weight = tItem.weight - 1
                    }
                }
            } else {
                if (dDep.weight == tDep.weight) {
                    dDep.weight--
                    res = true
                } else if (dDep.weight > tDep.weight) {
                    dDep.weight = tDep.weight - 1
                    res = true
                }
            }
        }
        return res
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
        taskItemTouchHlp.attachToRecyclerView(tasks_list)

        quick_add_btn.setOnClickListener {
            onAddTaskClicked()
        }

        quick_add_text.addTextChangedListener(this)

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
            1 -> onDepEditFinished()
        }

    }

    private fun onDepEditFinished() {
        fetchStore(this, lastStoreId)
    }

    fun onItemEditFinished(needUpdate: Boolean) {
        if (needUpdate) {
            quick_add_text.text.clear()
            fetchStore(this, lastStoreId)
        }
    }

    //
    // DepartmentChoiceListener
    //

    override fun onDepartmentChosen(d: Department) {
        Log.d(TAG, "onDepartmentChosen : ${d.name}")
        // we choose a department, so the task does not exist

        val i = Item(quick_add_text.text.trim().toString(), d)
        i.id = ItemTable.create(activity, i)
        if (i.id > 0) {
            if (ItemWeightTable.create(activity, i, lastStoreId) > 0) {
                if (ItemDepTable.create(activity, i, lastStoreId) > 0) {
                    val t = Task(i)
                    if (TaskTable.create(activity, t, lastStoreId) > 0) {
                        // add to adapter, but need to find the right position
                        tasksList.add(t)
                        tasksList.sort()
                        taskAdapter!!.animateTo(tasksList)
                        quick_add_text.text.clear()
                    }
                }
            } else {
                Log.e(TAG, "error on item weight creation")
            }
        } else {
            Log.e(TAG, "error on item creation")
        }

    }

    override fun onDoneStateChanged(task: Task) {
        TaskTable.updateDoneState(activity, task.id, task.isDone)
        tasksList.sort()
        addHeaderIfNeeded(tasksList)
        taskAdapter!!.animateTo(tasksList)
        updateWidgets()
    }

    private fun updateWidgets() {
        Log.d(TAG, "updateWidgets")
        val appWidgetManager = AppWidgetManager.getInstance(activity);

        val intent = Intent(activity, TaskListWidget::class.java)
        intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE

        val thisWidget = ComponentName(activity, TaskListWidget::class.java);
        val ids = appWidgetManager.getAppWidgetIds(thisWidget);

        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        ids.forEach { appWidgetManager.notifyAppWidgetViewDataChanged(it, R.id.tasks_list_widget) }
    }

    override fun onItemClicked(task: Task) {
        ItemEditorActivity.callMe(this, lastStoreId, task)
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
        if (newStoreId > 0) lastStoreId = newStoreId
        if (storeId == lastStoreId || storeId < 0L) {
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
                    tasksList = cursor.map { Task(it) }
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
                tasks_list.adapter = taskAdapter
            }
            else -> throw IllegalArgumentException("Unknown cursorLoader id")
        }
    }

    override fun onLoaderReset(cursorLoader: Loader<Cursor>?) {
        currentStore = null
        cursorLoader?.reset()
    }
}