package fr.geobert.efficio

import android.app.Activity
import android.app.Instrumentation
import android.support.test.espresso.Espresso
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.action.ViewActions.replaceText
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.core.deps.guava.base.Throwables
import android.support.test.espresso.core.deps.guava.collect.Sets
import android.support.test.espresso.matcher.ViewMatchers.isDisplayed
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.espresso.matcher.ViewMatchers.withText
import android.support.test.filters.LargeTest
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import android.support.test.runner.lifecycle.Stage
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.atomic.AtomicReference

/**
 * [Testing Fundamentals](http://d.android.com/tools/testing/testing_android.html)
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class EfficioTest {
    init {
        MainActivity.TEST_MODE = true
    }

    @Rule @JvmField var activityRule: ActivityTestRule<MainActivity> =
            ActivityTestRule(MainActivity::class.java)

    private fun getActivity() = activityRule.activity

    // consts
    val ITEM_A = "Item A"
    val ITEM_B = "Item B"
    val ITEM_C = "Item C"
    val DEP_A = "Dep 1"
    val DEP_B = "Dep 2"

    // tearDown helpers
    private fun closeAllActivities(instrumentation: Instrumentation) {
        val NUMBER_OF_RETRIES = 100
        var i = 0
        while (closeActivity(instrumentation)) {
            if (i++ > NUMBER_OF_RETRIES) {
                throw AssertionError("Limit of retries excesses")
            }
            Thread.sleep(200)
        }
    }

    private fun <X> callOnMainSync(instrumentation: Instrumentation, callable: Callable<X>): X {
        val retAtomic = AtomicReference<X>()
        val exceptionAtomic = AtomicReference<Throwable>()
        instrumentation.runOnMainSync({
            try {
                retAtomic.set(callable.call())
            } catch (e: Throwable) {
                exceptionAtomic.set(e)
            }
        })
        val exception = exceptionAtomic.get()
        if (exception != null) {
            Throwables.propagate(exception)
        }
        return retAtomic.get()
    }

    private fun getActivitiesInStages(vararg stages: Stage): HashSet<Activity> {
        val activities = Sets.newHashSet<Activity>()
        val instance = ActivityLifecycleMonitorRegistry.getInstance()
        for (stage in stages) {
            val activitiesInStage = instance.getActivitiesInStage(stage)
            if (activitiesInStage != null) {
                activities.addAll(activitiesInStage)
            }
        }
        return activities
    }

    private fun closeActivity(instrumentation: Instrumentation): Boolean {
        val activityClosed = callOnMainSync(instrumentation, Callable<Boolean> {
            val activities = getActivitiesInStages(Stage.RESUMED, Stage.STARTED, Stage.PAUSED,
                    Stage.STOPPED, Stage.CREATED)
            activities.removeAll(getActivitiesInStages(Stage.DESTROYED))
            if (activities.size > 0) {
                val activity = activities.iterator().next()
                activity.finish()
                true
            } else {
                false
            }
        })
        if (activityClosed) {
            instrumentation.waitForIdleSync()
        }
        return activityClosed
    }
    // end tearDown helpers

    @After
    fun tearDown() {
//        closeAllActivities(InstrumentationRegistry.getInstrumentation())
    }

    fun testEnterSameItem() {
        addItem(ITEM_A, DEP_A)
        checkTaskListSize(1)

        addItem(ITEM_A)
        checkTaskListSize(1)
    }

    fun testEnterTwoItems() {
        addItem(ITEM_A, DEP_A)
        checkTaskListSize(1)

        addItem(ITEM_B, DEP_B)
        checkTaskListSize(2)
    }

    fun testEditItemName() {
        addItem(ITEM_A, DEP_A)
        clickOnTask(0)

        onView(withId(R.id.item_name_edt)).perform(replaceText(ITEM_B))
        onView(withId(R.id.confirm)).perform(click())
        onView(withText(ITEM_B)).check(matches(isDisplayed()))
    }

    // check the sort after drag an item
    fun testSort2Items() {
        addItem(ITEM_A, DEP_B)
        checkTaskListSize(1)

        addItem(ITEM_B, DEP_A)
        checkTaskListSize(2)

        // Item B is in Department 1, no weight yet, should be first
        checkTaskListItemAt(0, ITEM_B)

        // drag A above B
        dragTask(1, Direction.UP)
        checkTaskListItemAt(0, ITEM_A)

        // set A as done
        clickTickOfTaskAt(0)

        // A going to bottom, B should be first
        checkTaskListItemAt(0, ITEM_B)

        // uncheck A, position is 2 and not 1 because of header
        clickTickOfTaskAt(2)

        // A should go back to first pos because of dep weight
        checkTaskListItemAt(0, ITEM_A)

    }

    fun testSort3Items() {
        addItem(ITEM_A, DEP_B)
        addItem(ITEM_B, DEP_A)
        dragTask(1, Direction.UP)
        // same state as end of testSort2Items

        // add C item same dep as A
        addItem(ITEM_C, DEP_B)
        checkTaskListItemAt(0, ITEM_A)
        checkTaskListItemAt(1, ITEM_C)
        checkTaskListItemAt(2, ITEM_B)

        // drag C above A
        dragTask(1, Direction.UP)
        checkTaskListItemAt(0, ITEM_C)
        checkTaskListItemAt(1, ITEM_A)
        checkTaskListItemAt(2, ITEM_B)

        // tick C, should fall at the bottom
        clickTickOfTaskAt(0)
        checkTaskListItemAt(0, ITEM_A)
        checkTaskListItemAt(1, ITEM_B)

        // tick A
        clickTickOfTaskAt(0)
        checkTaskListItemAt(0, ITEM_B)
        // pos 1 = header
        checkTaskListItemAt(2, ITEM_A)
        checkTaskListItemAt(3, ITEM_C)

        // untick C
        clickTickOfTaskAt(3)
        checkTaskListItemAt(0, ITEM_C)
        checkTaskListItemAt(1, ITEM_B)
        // pos 2 = header
        checkTaskListItemAt(3, ITEM_A)

        // untick A
        clickTickOfTaskAt(3)
        checkTaskListItemAt(0, ITEM_C)
        checkTaskListItemAt(1, ITEM_A)
        checkTaskListItemAt(2, ITEM_B)

        // drag C under A
        dragTask(0, Direction.DOWN)
        checkTaskListItemAt(0, ITEM_A)
        checkTaskListItemAt(1, ITEM_C)
        checkTaskListItemAt(2, ITEM_B)

        // tick C
        clickTickOfTaskAt(1)
        checkTaskListItemAt(0, ITEM_A)
        checkTaskListItemAt(1, ITEM_B)
        // pos 2 = header
        checkTaskListItemAt(3, ITEM_C)

        // untick C
        clickTickOfTaskAt(3)
        checkTaskListItemAt(0, ITEM_A)
        checkTaskListItemAt(1, ITEM_C)
        checkTaskListItemAt(2, ITEM_B)
    }


    @Test fun testChangeItemDep() {
        addItem(ITEM_A, DEP_B)
        addItem(ITEM_B, DEP_A)
        addItem(ITEM_C, DEP_A)
        checkTaskListItemAt(0, ITEM_B)
        checkTaskListItemAt(1, ITEM_C)
        checkTaskListItemAt(2, ITEM_A)

        // put C above A, so it get some weight
        dragTask(1, Direction.UP)

        // change C dep to the same as B, should reset item's weight, so under B
        clickOnTask(0)
        Espresso.closeSoftKeyboard()
        onView(withText(DEP_B)).perform(click())
        onView(withId(R.id.confirm)).perform(click())
        checkTaskListItemAt(0, ITEM_B)
        checkTaskListItemAt(1, ITEM_A)
        checkTaskListItemAt(2, ITEM_C)
    }
}