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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.os.Handler
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
import fr.geobert.efficio.data.DepartmentManager
import fr.geobert.efficio.data.Item
import fr.geobert.efficio.data.Store
import fr.geobert.efficio.data.Task
import fr.geobert.efficio.db.ItemDepTable
import fr.geobert.efficio.db.ItemTable
import fr.geobert.efficio.db.ItemWeightTable
import fr.geobert.efficio.db.StoreCompositionTable
import fr.geobert.efficio.db.TaskTable
import fr.geobert.efficio.dialog.DepartmentChoiceDialog
import fr.geobert.efficio.misc.CREATE_TASK
import fr.geobert.efficio.misc.GET_TASKS_OF_STORE
import fr.geobert.efficio.misc.RefreshInterface
import fr.geobert.efficio.misc.TopBottomSpaceItemDecoration
import fr.geobert.efficio.misc.map
import fr.geobert.efficio.widget.TaskListWidget
import kotlinx.android.synthetic.main.item_list_fragment.*
import java.util.*
import kotlin.properties.Delegates

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
    private val taskItemTouchCbk =
            object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN,
                    ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
                var lastDragTask: TaskViewHolder? = null
                var lastSwipeTask: TaskViewHolder? = null
                var directionReached: Int? = null
                var needAdapterSort: Boolean = false
                var canvas: Canvas by Delegates.notNull()
                var recyclerview: RecyclerView by Delegates.notNull()
                val p = Paint()

                override fun onMove(recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder,
                                    target: RecyclerView.ViewHolder): Boolean {
                    Log.d(TAG, "onMove")
                    Collections.swap(tasksList, viewHolder.adapterPosition, target.adapterPosition)
                    val r = updateTaskWeight(viewHolder as TaskViewHolder, target as TaskViewHolder)
                    taskAdapter!!.moveItem(viewHolder.adapterPosition, target.adapterPosition)
                    if (!needAdapterSort) needAdapterSort = r
                    return true
                }

                private var orig: Float = 0f

                private fun manageLastDragTask() {
                    // end of drag n drop, adapter is correctly ordered but not our representation here
                    lastDragTask!!.cardView.cardElevation = orig
                    Log.d(TAG, "end of drag n drop, sort the list")
                    StoreCompositionTable.updateDepWeight(activity, lastDragTask!!.task.item.department)
                    ItemWeightTable.updateWeight(activity, lastDragTask!!.task.item)
                    //Log.d(TAG, "before sort : ${tasksList[0]} / ${tasksList[1]}")
                    tasksList.sort()
                    //Log.d(TAG, "after sort : ${tasksList[0]} / ${tasksList[1]}")
                    updateWidgets()
                    val f = quick_add_text.text.trim().toString()
                    if (needAdapterSort || !f.isEmpty()) {
                        val l = if (!f.isEmpty()) filter(tasksList, f) else tasksList
                        taskAdapter!!.animateTo(l)
                        tasks_list.post {
                            tasks_list.invalidateItemDecorations()
                        }
                    }
                    lastDragTask = null
                }

                private fun manageLastSwipeTask() {
                    Log.d(TAG, "manageLastSwipeTask")
                    val t = lastSwipeTask?.task
                    if (t != null)
                        when (directionReached) {
                            ItemTouchHelper.RIGHT -> {
                                // minus
                                if (t.qty > 1)
                                    t.qty--
                            }
                            ItemTouchHelper.LEFT -> {
                                // plus
                                t.qty++
                            }
                        }
                    TaskTable.updateTaskQty(activity, lastSwipeTask!!.task)
                    lastSwipeTask = null
                    directionReached = null
                }

                override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                    super.onSelectedChanged(viewHolder, actionState)
                    val vh = viewHolder as TaskViewHolder?
                    if (vh == null) {
                        if (lastDragTask != null) { // we were in drag mode
                            manageLastDragTask()
                        } else if (lastSwipeTask != null) { // we were in swipe mode
                            manageLastSwipeTask()
                        }

                    } else {
                        when (actionState) {
                            ItemTouchHelper.ACTION_STATE_DRAG -> {
                                orig = vh.cardView.cardElevation
                                vh.cardView.cardElevation = 20.0f
                            }
                            ItemTouchHelper.ACTION_STATE_SWIPE -> {
                                if (handler == null) {
                                    handler = Handler()
                                } else {
                                    handler?.removeCallbacks(postAction)
                                }
                            }
                        }
                    }
                    when (actionState) {
                        ItemTouchHelper.ACTION_STATE_DRAG -> {
                            if (lastDragTask == null) needAdapterSort = false
                            lastDragTask = vh
                        }
                    }
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder?, direction: Int) {
                    // no action here, done in onSelectedChanged
                }

                var handler: Handler? = null
                var postAction: Runnable? = Runnable {
                    taskAdapter?.notifyDataSetChanged()
                    handler = null
                }

                override fun onChildDraw(c: Canvas, recyclerView: RecyclerView,
                                         viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float,
                                         actionState: Int, isCurrentlyActive: Boolean) {
                    val dXToUse = if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                        //Log.d(TAG, "onChildDraw: dX: $dX / isCurrentlyActive: $isCurrentlyActive")
                        canvas = c
                        recyclerview = recyclerView
                        lastSwipeTask = viewHolder as TaskViewHolder
                        val icon: Bitmap?
                        val itemView = viewHolder.itemView
                        val height = itemView.bottom - itemView.top
                        val width = height / 3
                        val verticalMargin = 8
                        val horizontalMargin = 25
                        val extHorizontalMargin = 15
                        val maxDx = 150f
                        val colorChange = maxDx - 5
                        if (dX > 0) {
                            val tmpDx = if (dX > maxDx) maxDx else dX
                            p.color = Color.parseColor(if (tmpDx > colorChange) "#3967cc" else "#6183ce")
                            val background = RectF(itemView.left.toFloat() + extHorizontalMargin,
                                    itemView.top.toFloat() + verticalMargin,
                                    tmpDx + horizontalMargin,
                                    itemView.bottom.toFloat() - verticalMargin)
                            c.drawRect(background, p)
                            icon = BitmapFactory.decodeResource(resources, R.drawable.minus_math)
                            val icon_dest = RectF(itemView.left.toFloat() + width,
                                    itemView.top.toFloat() + width,
                                    itemView.left.toFloat() + 2 * width,
                                    itemView.bottom.toFloat() - width)
                            c.drawBitmap(icon, null, icon_dest, p)
                            if (dX > maxDx) {
                                directionReached = ItemTouchHelper.RIGHT
                                if (isCurrentlyActive)
                                    maxDx
                                else
                                    Math.max(maxDx - (dX - maxDx), 0f)
                            } else dX
                        } else {
                            val tmpDx = if (dX < -maxDx) -maxDx else dX
                            p.color = Color.parseColor(if (tmpDx < -colorChange) "#2dce45" else "#4cbf85")
                            val background = RectF(itemView.right.toFloat() + tmpDx - horizontalMargin,
                                    itemView.top.toFloat() + verticalMargin,
                                    itemView.right.toFloat() - extHorizontalMargin,
                                    itemView.bottom.toFloat() - verticalMargin)
                            c.drawRect(background, p)
                            icon = BitmapFactory.decodeResource(resources, R.drawable.plus_math)
                            val icon_dest = RectF(itemView.right.toFloat() - 2 * width,
                                    itemView.top.toFloat() + width,
                                    itemView.right.toFloat() - width,
                                    itemView.bottom.toFloat() - width)
                            c.drawBitmap(icon, null, icon_dest, p)
                            if (dX < -maxDx) {
                                directionReached = ItemTouchHelper.LEFT
                                if (isCurrentlyActive)
                                    -maxDx
                                else
                                    -Math.max((maxDx - (-dX - maxDx)), 0f)
                            } else dX
                        }
                    } else {
                        dX
                    }
                    val h = handler
                    if (!isCurrentlyActive && h != null) {
                        h.removeCallbacks(postAction)
                        h.postDelayed(postAction, 100)
                    }
                    super.onChildDraw(c, recyclerView, viewHolder, dXToUse, dY, actionState, isCurrentlyActive)
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
        Log.d(TAG, "dragged task: $dTask")
        if (dragged.adapterPosition > target.adapterPosition) {
            // item goes up
            Log.d(TAG, "item goes up")
            if (dDep.id == tDep.id) {
                if (dItem.weight <= tItem.weight) {
                    dItem.weight = tItem.weight + 1
                }
            } else {
                if (dDep.weight <= tDep.weight) {
                    dDep.weight = tDep.weight + 1
                    res = true
                }
            }
        } else if (dragged.adapterPosition < target.adapterPosition) {
            // item goes down
            Log.d(TAG, "item goes down")
            if (dDep.id == tDep.id) {
                if (dItem.weight >= tItem.weight) {
                    dItem.weight = tItem.weight - 1
                }
            } else {
                if (dDep.weight >= tDep.weight) {
                    dDep.weight = tDep.weight - 1
                    res = true
                }
            }
        }
        Log.d(TAG, "dragged task after: $dTask")
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
                        updateWidgets()
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
        val appWidgetManager = AppWidgetManager.getInstance(activity)

        val intent = Intent(activity, TaskListWidget::class.java)
        intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE

        val thisWidget = ComponentName(activity, TaskListWidget::class.java)
        val ids = appWidgetManager.getAppWidgetIds(thisWidget)

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