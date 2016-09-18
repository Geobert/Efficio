package fr.geobert.efficio

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.action.ViewActions.replaceText
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.contrib.RecyclerViewActions
import android.support.test.espresso.matcher.ViewMatchers.withId
import fr.geobert.efficio.adapter.TaskViewHolder

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