package fr.geobert.efficio


import android.app.Fragment
import android.app.LoaderManager
import android.content.CursorLoader
import android.content.Loader
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
import fr.geobert.efficio.data.Department
import fr.geobert.efficio.data.Item
import fr.geobert.efficio.data.Store
import fr.geobert.efficio.data.Task
import fr.geobert.efficio.db.ItemDepTable
import fr.geobert.efficio.db.ItemTable
import fr.geobert.efficio.db.ItemWeightTable
import fr.geobert.efficio.db.TaskTable
import fr.geobert.efficio.misc.SpaceItemDecoration
import fr.geobert.efficio.misc.map
import kotlinx.android.synthetic.main.item_list_fragment.*
import java.util.*
import kotlin.properties.Delegates

class TaskListFragment : Fragment(), LoaderManager.LoaderCallbacks<Cursor>, TextWatcher,
        DepartmentChoiceDialog.DepartmentChoiceListener,
        TaskViewHolder.OnDoneStateChangeListener {
    private val GET_TASKS_OF_STORE = 100
    private val TAG = "TaskListFragment"

    var lastStoreId: Long = 1 // todo get it from prefs
    var currentStore: Store? = null
    var cursorLoader: CursorLoader? = null
    var taskAdapter: TaskAdapter by Delegates.notNull()
    var tasksList: MutableList<Task> = LinkedList()

    private val header = Task()

    private val taskItemTouchCbk =
            object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
                var lastDragTask: Task? = null
                override fun onMove(recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder,
                                    target: RecyclerView.ViewHolder): Boolean {
                    Collections.swap(tasksList, viewHolder.adapterPosition, target.adapterPosition)
                    taskAdapter.notifyItemMoved(viewHolder.adapterPosition, target.adapterPosition)
                    updateTaskWeight(viewHolder as TaskViewHolder, target as TaskViewHolder)
                    return true
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder?, direction: Int) {
                    // nothing
                }

                override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                    super.onSelectedChanged(viewHolder, actionState)
                    // end of drag n drop, adapter is correctly ordered but not our representation here
                    if (viewHolder == null) {
                        Log.d(TAG, "end of drag n drop, sort the list")
                        ItemWeightTable.updateWeight(activity, lastDragTask!!.item)
                        tasksList.sort()
                    }
                    lastDragTask = (viewHolder as TaskViewHolder?)?.task
                }
            }

    private val taskItemTouchHlp = ItemTouchHelper(taskItemTouchCbk)

    private fun updateTaskWeight(dragged: TaskViewHolder, target: TaskViewHolder) {
        val dTask = dragged.task
        val tTask = target.task
        val dItem = dTask.item
        val tItem = tTask.item
        val dDep = dItem.department
        val tDep = tItem.department
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
                // todo
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
                // todo
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
        tasks_list.addItemDecoration(SpaceItemDecoration(10, true))
        taskItemTouchHlp.attachToRecyclerView(tasks_list)

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
        val (t, pos) = taskAdapter.getTaskByName(quick_add_text.text.trim().toString())
        if (t != null) {
            t.isDone = false
            taskAdapter.notifyItemChanged(pos)
        } else {
            // case item does not exists yet
            createNewTask()
        }
    }

    private fun createNewTask() {
        val d = DepartmentChoiceDialog()
        d.listener = this
        val b = Bundle()
        b.putLong("storeId", lastStoreId)
        d.arguments = b
        d.show(fragmentManager, "DepChoiceDialog")
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
                        taskAdapter.animateTo(tasksList)
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

    override fun onChoiceCanceled() {
        // TODO
    }

    override fun onDoneStateChanged(task: Task) {
        TaskTable.updateDoneState(activity, task)
        tasksList.sort()
        addHeaderIfNeeded(tasksList)
        taskAdapter.animateTo(tasksList)
    }

    /// TextWatcher

    override fun afterTextChanged(s: Editable) {
        quick_add_btn.isEnabled = s.trim().length > 0
        if (tasksList.count() > 0) {
            val filteredList = filter(tasksList, s)
            addHeaderIfNeeded(filteredList)
            taskAdapter.animateTo(filteredList)
            tasks_list.scrollToPosition(0)
        }
    }

    private fun filter(list: MutableList<Task>, s: Editable): MutableList<Task> {
        val f = s.toString().toLowerCase()
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
            GET_TASKS_OF_STORE -> TaskTable.getAllTasksForStoreLoader(this.activity,
                    bundle?.getLong("storeId") ?: 0)
            else -> null
        }
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
                taskAdapter = TaskAdapter(tasksList, this)
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