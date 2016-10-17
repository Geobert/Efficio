package fr.geobert.efficio

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.util.Log
import fr.geobert.efficio.adapter.TaskAdapter
import fr.geobert.efficio.adapter.TaskViewHolder
import fr.geobert.efficio.data.Task
import fr.geobert.efficio.db.ItemWeightTable
import fr.geobert.efficio.db.StoreCompositionTable
import fr.geobert.efficio.db.TaskTable
import java.util.*
import kotlin.properties.Delegates

class TaskDragSwipeHelper(val fragment: TaskListFragment, val tasksList: MutableList<Task>,
                          val taskAdapter: TaskAdapter) :
        ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN,
                ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
    private val TAG = "TaskDragSwipeHelper"
    private var lastDragTask: TaskViewHolder? = null
    private var lastSwipeTask: TaskViewHolder? = null
    private var directionReached: Int? = null
    private var needAdapterSort: Boolean = false
    private var canvas: Canvas by Delegates.notNull()
    private var recyclerview: RecyclerView by Delegates.notNull()
    private val p = Paint()
    private val activity = fragment.activity

    override fun onMove(recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder,
                        target: RecyclerView.ViewHolder): Boolean {
        Log.d(TAG, "onMove")
        Collections.swap(tasksList, viewHolder.adapterPosition, target.adapterPosition)
        val r = fragment.updateTaskWeight(viewHolder as TaskViewHolder, target as TaskViewHolder)
        taskAdapter.moveItem(viewHolder.adapterPosition, target.adapterPosition)
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
        fragment.updateWidgets()
        fragment.updateTasksList(needAdapterSort)
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
}