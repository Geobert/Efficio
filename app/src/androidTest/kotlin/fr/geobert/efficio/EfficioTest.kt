package fr.geobert.efficio

import android.app.*
import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.*
import android.support.test.espresso.assertion.ViewAssertions
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import android.support.test.espresso.core.deps.guava.base.Throwables
import android.support.test.espresso.core.deps.guava.collect.Sets
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.filters.LargeTest
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import android.support.test.runner.lifecycle.*
import fr.geobert.efficio.adapter.TaskViewHolder
import org.hamcrest.Matchers.allOf
import org.junit.*
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
        closeAllActivities(InstrumentationRegistry.getInstrumentation())
    }

    fun testInitState() {
        onView(allOf(withId(R.id.store_name_lbl), withText(R.string.store))).
                check(matches(isDisplayed()))
        checkTaskListSize(0)
    }

    fun testEnterSameItem() {
        addItem("Item A", "Department 1")
        checkTaskListSize(1)

        addItem("Item A")
        checkTaskListSize(1)
    }

    fun testEnterTwoItems() {
        addItem("Item A", "Department 1")
        checkTaskListSize(1)

        addItem("Item B", "Department 2")
        checkTaskListSize(2)
    }

    fun testEditItemName() {
        addItem("Item A", "Department 1")
        clickOnTask(0)

        onView(withId(R.id.item_name_edt)).perform(replaceText("Item B"))
        onView(withId(R.id.confirm)).perform(click())
        onView(withText("Item B")).check(matches(isDisplayed()))

        onView(withText("Item B")).perform(longClick())
    }

    @Test fun testDragItem() {
        addItem("Item A", "Department 1")
        checkTaskListSize(1)

        addItem("Item B", "Department 2")
        checkTaskListSize(2)

        onView(withRecyclerView(R.id.tasks_list).atPosition(0)).check(
                ViewAssertions.matches(hasDescendant(withText("Item A"))))

        onView(withId(R.id.tasks_list)).perform(actionOnItemAtPosition<TaskViewHolder>(1,
                dragViewInRecycler(Direction.UP)))

        onView(withRecyclerView(R.id.tasks_list).atPosition(0)).check(
                ViewAssertions.matches(hasDescendant(withText("Item B"))))

        onView(withRecyclerView(R.id.tasks_list).atPositionOnView(0, R.id.task_checkbox)).perform(click())
        pauseTest(500)

        onView(withRecyclerView(R.id.tasks_list).atPosition(0)).check(
                ViewAssertions.matches(hasDescendant(withText("Item A"))))

        // position is 2 and not 1 because of header
        onView(withRecyclerView(R.id.tasks_list).atPositionOnView(2, R.id.task_checkbox)).perform(click())
        pauseTest(500)

        onView(withRecyclerView(R.id.tasks_list).atPosition(0)).check(
                ViewAssertions.matches(hasDescendant(withText("Item B"))))
    }



}