package fr.geobert.efficio

import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.action.ViewActions.replaceText
import android.support.test.espresso.assertion.ViewAssertions
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.contrib.DrawerActions
import android.support.test.espresso.contrib.DrawerMatchers
import android.support.test.espresso.contrib.NavigationViewActions
import android.support.test.espresso.contrib.RecyclerViewActions
import android.support.test.espresso.matcher.ViewMatchers
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.util.Log
import android.view.Gravity
import fr.geobert.efficio.adapter.TaskViewHolder

// consts
val ITEM_A = "Item A"
val ITEM_B = "Item B"
val ITEM_C = "Item C"
val ITEM_D = "Item D"
val ITEM_E = "Item E"
val DEP_A = "Dep 1"
val DEP_B = "Dep 2"
val DEP_C = "Dep 3"
val DEP_D = "Dep 4"
val COMPLETED: String by lazy { InstrumentationRegistry.getTargetContext().getString(R.string.completed) }

fun addItem(name: String, depName: String? = null) {
    onView(withId(R.id.quick_add_text)).perform(replaceText(name))
    onView(withId(R.id.quick_add_btn)).perform(click())
    if (depName != null) {
        // enter department
        onView(withId(R.id.add_dep_text)).perform(replaceText(depName))
        onView(withId(R.id.add_dep_btn)).perform(click())
    }
}

fun checkTaskListSize(expectedSize: Int) {
    onView(withId(R.id.tasks_list)).check(matches(withRecyclerViewSize(expectedSize)))
}

fun clickOnTask(pos: Int) {
    onView(withId(R.id.tasks_list)).perform(RecyclerViewActions.actionOnItemAtPosition<TaskViewHolder>(pos, click()))
}

private fun checkItemInRecyclerViewAt(pos: Int, text: String, recyclerView: Int) {
    onView(withRecyclerView(recyclerView).atPosition(pos)).check(
            ViewAssertions.matches(ViewMatchers.hasDescendant(ViewMatchers.withText(text))))
}

fun checkTaskListItemAt(pos: Int, text: String) {
    Log.d("EfficioTest", "checkTaskListItemAt: $pos, $text")
    checkItemInRecyclerViewAt(pos, text, R.id.tasks_list)
}

fun checkDepListItemAt(pos: Int, text: String) {
    checkItemInRecyclerViewAt(pos, text, R.id.dep_list)
}

fun checkOrderOfTask(vararg tasks: String) {
    var i = 0
    for (task in tasks) {
        if (task != COMPLETED)
            checkTaskListItemAt(i, task)
        i++
    }
}

fun checkOrderOfDeps(vararg deps: String) {
    var i = 0
    for (dep in deps) {
        checkDepListItemAt(i, dep)
        i++
    }
}

private fun dragItemInRecyclerView(pos: Int, dir: Direction, nbItem: Int, recyclerView: Int) {
    onView(withId(recyclerView)).perform(RecyclerViewActions.actionOnItemAtPosition<TaskViewHolder>(pos,
            dragViewInRecycler(dir, nbItem)))
}

fun dragTask(pos: Int, dir: Direction, nbItem: Int = 1) {
    dragItemInRecyclerView(pos, dir, nbItem, R.id.tasks_list)
}

fun dragDep(pos: Int, dir: Direction, nbItem: Int = 1) {
    dragItemInRecyclerView(pos, dir, nbItem, R.id.dep_list)
}

fun clickTickOfTaskAt(pos: Int) {
    onView(withRecyclerView(R.id.tasks_list).atPositionOnView(pos, R.id.task_checkbox)).perform(click())
    pauseTest(500) // animation
}

fun clickInDrawer(item: Int) {
    Espresso.onView(ViewMatchers.withId(R.id.drawer_layout))
            .check(ViewAssertions.matches(DrawerMatchers.isClosed(Gravity.START)))
            .perform(DrawerActions.open())
    Espresso.onView(ViewMatchers.withId(R.id.drawer_layout)).check(ViewAssertions.matches(DrawerMatchers.isOpen()))
    Espresso.onView(ViewMatchers.withId(R.id.nvView)).perform(NavigationViewActions.navigateTo(item))
}
