package fr.geobert.efficio

import android.os.SystemClock
import android.support.test.espresso.*
import android.support.test.espresso.action.*
import android.support.test.espresso.matcher.ViewMatchers
import android.view.*
import org.hamcrest.Matcher


enum class Direction {
    UP, DOWN
}

class DragViewInRecyclerAction(val direction: Direction, val nbItem: Int = 1) : ViewAction {
    private val TAG = "DragViewInRecyclerAction"

    private val STEP_COUNT = 10

    private val coordinatesProvider: CoordinatesProvider = GeneralLocation.CENTER

    override fun getDescription(): String {
        return "drag and drop a view"
    }

    override fun getConstraints(): Matcher<View> {
        return ViewMatchers.isAssignableFrom(View::class.java)
    }

    override fun perform(uiController: UiController, view: View) {
        val height = view.height

        val coords = coordinatesProvider.calculateCoordinates(view)

        val destY = coords[1] + nbItem * if (direction == Direction.UP) -height else height

        val ySteps = (destY - coords[1]) / STEP_COUNT

        val x = coords[0].toFloat() + 50
        var y = coords[1].toFloat()

        var downTime = SystemClock.uptimeMillis()
        var eventTime = SystemClock.uptimeMillis()
        var downEvent = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, x, y, 0)
        uiController.injectMotionEvent(downEvent)
        downEvent.recycle()

        try {
            Thread.sleep(1000)
            for (i in 0..STEP_COUNT) {
                y += ySteps
                downTime = SystemClock.uptimeMillis()
                eventTime = SystemClock.uptimeMillis()
                downEvent = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_MOVE, x, y, 0)
                uiController.injectMotionEvent(downEvent)
                downEvent.recycle()
            }

            downTime = SystemClock.uptimeMillis()
            eventTime = SystemClock.uptimeMillis()
            downEvent = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, x, y, 0)
            uiController.injectMotionEvent(downEvent)
            uiController.loopMainThreadUntilIdle()
            downEvent.recycle()
        } catch (e: Exception) {
            downEvent.recycle()
        }

    }

}