package fr.geobert.efficio

import android.support.test.espresso.Espresso
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.action.ViewActions.replaceText
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.isDisplayed
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.espresso.matcher.ViewMatchers.withText
import android.support.test.filters.LargeTest
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [Testing Fundamentals](http://d.android.com/tools/testing/testing_android.html)
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class EfficioTest {
    @Rule @JvmField var activityRule: ActivityTestRule<MainActivity> =
            ActivityTestRule(MainActivity::class.java)

    init {
        MainActivity.TEST_MODE = true
    }

    @Test fun testEnterSameItem() {
        addItem(ITEM_A, DEP_A)
        checkTaskListSize(1)

        addItem(ITEM_A)
        checkTaskListSize(1)
    }

    @Test fun testEnterTwoItems() {
        addItem(ITEM_A, DEP_A)
        checkTaskListSize(1)

        addItem(ITEM_B, DEP_B)
        checkTaskListSize(2)
    }

    @Test fun testEditItemName() {
        addItem(ITEM_A, DEP_A)
        clickOnTask(0)

        onView(withId(R.id.item_name_edt)).perform(replaceText(ITEM_B))
        onView(withId(R.id.confirm)).perform(click())
        onView(withText(ITEM_B)).check(matches(isDisplayed()))
    }

    // check the sort after drag an item
    @Test fun testSort2Items() {
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

    @Test fun testSort3Items() {
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