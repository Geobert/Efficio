package fr.geobert.efficio

import android.graphics.*
import android.os.Handler
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.util.Log
import com.crashlytics.android.Crashlytics
import fr.geobert.efficio.adapter.*
import fr.geobert.efficio.data.*
import fr.geobert.efficio.db.*
import java.util.*
import kotlin.properties.Delegates

class TaskDragSwipeHelper(val fragment: TaskListFragment, var tasksList: MutableList<Task>,
                          val taskAdapter: TaskAdapter) :
        ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN,
                ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
    private enum class Direction { UP, DOWN }

    private val TAG = "TaskDragSwipeHelper"
    private var lastDragTask: TaskViewHolder? = null
    private var lastSwipeTask: TaskViewHolder? = null
    private var directionReached: Int? = null
    private var needAdapterSort: Boolean = false
    private var canvas: Canvas by Delegates.notNull()
    private var recyclerview: RecyclerView by Delegates.notNull()
    private val p = Paint()
    private val activity = fragment.activity
    private var direction: Direction by Delegates.notNull()
    private var lastTargetSameDep: Task? = null

    override fun onMove(recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder,
                        target: RecyclerView.ViewHolder): Boolean {
        Log.d(TAG, "onMove: viewHolder.adapterPosition: ${viewHolder.adapterPosition} / target.adapterPosition: ${target.adapterPosition}  / tasksList.count: ${tasksList.size}")
        Collections.swap(tasksList, viewHolder.adapterPosition, target.adapterPosition)
        if (viewHolder.adapterPosition > target.adapterPosition) {
            direction = Direction.UP
        } else {
            direction = Direction.DOWN
        }
        val dragged = (viewHolder as TaskViewHolder).task
        val targeted = (target as TaskViewHolder).task
        if (dragged.item.department.id == targeted.item.department.id)
            lastTargetSameDep = targeted

        taskAdapter.moveItem(viewHolder.adapterPosition, target.adapterPosition)
        return true
    }

    private var orig: Float = 0f

    private fun manageLastDragTask() {
        // end of drag n drop, adapter is correctly ordered but not our representation here
        val t = lastDragTask
        if (t != null) {
            t.cardView.cardElevation = orig
            //Log.d(TAG, "end of drag n drop, sort the list")
            updateTaskWeight(t)
            StoreCompositionTable.updateDepWeight(activity, t.task.item.department)
            ItemWeightTable.updateWeight(activity, t.task.item)
            //Log.d(TAG, "before sort : ${tasksList[0]} / ${tasksList[1]}")
            tasksList.sort()
            //Log.d(TAG, "after sort : ${tasksList[0]} / ${tasksList[1]}")
            fragment.updateWidgets()
            fragment.updateTasksList(needAdapterSort)
            lastDragTask = null
        }
    }

    private fun updateTaskWeight(dragged: TaskViewHolder) {
        val pos = dragged.adapterPosition
        val item = dragged.task.item
        needAdapterSort = false
        if (tasksList.size > 1) {
            if (pos == 0) { // the first
                val next = tasksList[pos + 1].item
                if (item.department.id == next.department.id) {
                    if (item.weight >= next.weight)
                        item.weight = next.weight - 1.0
                } else {
                    if (item.department.weight >= next.department.weight) {
                        item.department.weight = next.department.weight - 1.0
                        val lastItemSameDep = lastTargetSameDep?.item ?: null
                        if (lastItemSameDep != null)
                            item.weight = lastItemSameDep.weight - 1.0
                        needAdapterSort = true
                    }
                }
            } else if (pos == (tasksList.size - 1)) { // the last
                val prev = tasksList[pos - 1].item
                if (item.department.id == prev.department.id) {
                    if (item.weight <= prev.weight)
                        item.weight = prev.weight + 1.0
                } else {
                    if (item.department.weight <= prev.department.weight) {
                        item.department.weight = prev.department.weight + 1.0
                        val lastItemSameDep = lastTargetSameDep?.item ?: null
                        if (lastItemSameDep != null)
                            item.weight = lastItemSameDep.weight + 1.0
                        needAdapterSort = true
                    }
                }
            } else { // somewhere between
                val next = tasksList[pos + 1].item
                val prev = tasksList[pos - 1].item
                val itemDep = item.department
                val nextDep = next.department
                val prevDep = prev.department
                if (itemDep.id != prevDep.id && itemDep.id != nextDep.id) { // all different dep
                    if (prevDep.id == nextDep.id) {
                        itemDep.weight = nextDep.weight + if (direction == Direction.UP) -1.0 else 1.0
                    } else {
                        itemDep.weight = (prevDep.weight + nextDep.weight) / 2.0
                        if (itemDep.weight >= nextDep.weight || itemDep.weight <= prevDep.weight)
                            handleDoubleCollisionForDep(pos, itemDep, nextDep, prevDep)
                    }
                    val lastItemSameDep = lastTargetSameDep?.item ?: null
                    if (lastItemSameDep != null)
                        item.weight = lastItemSameDep.weight + if (direction == Direction.UP) -1.0 else 1.0
                    needAdapterSort = true
                } else {
                    if (item.weight <= prev.weight || item.weight >= next.weight) {
                        item.weight = (next.weight + prev.weight) / 2.0
                        if (item.weight >= next.weight || item.weight <= prev.weight)
                            handleDoubleCollision(pos, item, next, prev)
                    }
                }
            }
        }
    }

    private fun handleDoubleCollision(pos: Int, item: Item, next: Item, prev: Item) {
        Log.e(TAG, "handleDoubleCollision")
        if (!BuildConfig.DEBUG)
            Crashlytics.log("double collision occurred for Items!!! handleDoubleCollision")
    }

    private fun handleDoubleCollisionForDep(pos: Int, item: Department, next: Department, prev: Department) {
        Log.e(TAG, "handleDoubleCollisionForDep")
        if (!BuildConfig.DEBUG)
            Crashlytics.log("double collision occurred for Deps!!! handleDoubleCollisionForDep")
    }

    private fun manageLastSwipeTask() {
        Log.d(TAG, "manageLastSwipeTask")
        val t = lastSwipeTask?.task
        if (t != null) {
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
            TaskTable.updateTaskQty(activity, t.id, t.qty)
        }
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
        taskAdapter.notifyDataSetChanged()
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
            val verticalMargin = 12
            val horizontalMargin = 45
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
                icon = BitmapFactory.decodeResource(fragment.resources, R.drawable.minus_math)
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
                icon = BitmapFactory.decodeResource(fragment.resources, R.drawable.plus_math)
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

//    private fun updateTaskWeight(dragged: TaskViewHolder, target: TaskViewHolder): Boolean {
//        val dItem = dragged.task.item
//        val tItem = target.task.item
//        val dDep = dItem.department
//        val tDep = tItem.department
//        var res: Boolean = false
//        if (dragged.adapterPosition > target.adapterPosition) {
//            // item goes up
//            if (dDep.id == tDep.id) {
//                if (dItem.weight <= tItem.weight) {
//                    dItem.weight = tItem.weight + 1
//                }
//            } else {
//                if (dDep.weight <= tDep.weight) {
//                    dDep.weight = tDep.weight + 1
//                    res = true
//                }
//            }
//        } else if (dragged.adapterPosition < target.adapterPosition) {
//            // item goes down
//            if (dDep.id == tDep.id) {
//                if (dItem.weight >= tItem.weight) {
//                    dItem.weight = tItem.weight - 1
//                }
//            } else {
//                if (dDep.weight >= tDep.weight) {
//                    dDep.weight = tDep.weight - 1
//                    res = true
//                }
//            }
//        }
//        return res
//    }
}