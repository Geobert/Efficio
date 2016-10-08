package fr.geobert.efficio

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.action.ViewActions.replaceText
import android.support.test.espresso.assertion.ViewAssertions
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.contrib.RecyclerViewActions
import android.support.test.espresso.matcher.ViewMatchers
import android.support.test.espresso.matcher.ViewMatchers.withId
import fr.geobert.efficio.adapter.TaskViewHolder

// consts
val ITEM_A = "Item A"
val ITEM_B = "Item B"
val ITEM_C = "Item C"
val DEP_A = "Dep 1"
val DEP_B = "Dep 2"

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

fun checkTaskListItemAt(pos: Int, text: String) {
    onView(withRecyclerView(R.id.tasks_list).atPosition(pos)).check(
            ViewAssertions.matches(ViewMatchers.hasDescendant(ViewMatchers.withText(text))))
}

fun dragTask(pos: Int, dir: Direction, nbItem: Int = 1) {
    onView(withId(R.id.tasks_list)).perform(RecyclerViewActions.actionOnItemAtPosition<TaskViewHolder>(pos,
            dragViewInRecycler(dir, nbItem)))
}

fun clickTickOfTaskAt(pos: Int) {
    onView(withRecyclerView(R.id.tasks_list).atPositionOnView(pos, R.id.task_checkbox)).perform(click())
    pauseTest(500) // animation
}

