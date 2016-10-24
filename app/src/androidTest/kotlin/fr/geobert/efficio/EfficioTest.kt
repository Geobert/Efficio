package fr.geobert.efficio

import android.support.test.espresso.Espresso
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.*
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.filters.LargeTest
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import org.junit.*
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

        // check dep order in dep editor
        clickInDrawer(R.id.edit_departments)
        checkOrderOfDeps(DEP_B, DEP_A)
        Espresso.pressBack()
        pauseTest(300)

        // creating an item, weight = max weight + 1, so B is last
        checkOrderOfTask(ITEM_A, ITEM_B)

        // drag B above A
        dragTask(1, Direction.UP)
        checkOrderOfTask(ITEM_B, ITEM_A)

        // set B as done
        clickTickOfTaskAt(0)

        // B going to bottom, A should be first
        checkOrderOfTask(ITEM_A, COMPLETED, ITEM_B)

        // uncheck B, position is 2 and not 1 because of header
        clickTickOfTaskAt(2)

        // B should go back to first pos
        checkOrderOfTask(ITEM_B, ITEM_A)

        // go to dep editor and back to refresh list and check order again
        clickInDrawer(R.id.edit_departments)
        // check order, as Item B is in Dep A, Dep A should be 1st after Item B was dragged up
        checkOrderOfDeps(DEP_A, DEP_B)
        Espresso.pressBack()
        pauseTest(300)
        checkOrderOfTask(ITEM_B, ITEM_A)

        // change dep order
        clickInDrawer(R.id.edit_departments)
        dragDep(1, Direction.UP)
        checkOrderOfDeps(DEP_B, DEP_A)
        Espresso.pressBack()
        checkOrderOfTask(ITEM_A, ITEM_B)

    }

    @Test fun testSort3Items() {
        addItem(ITEM_A, DEP_B)
        addItem(ITEM_B, DEP_B)
        addItem(ITEM_C, DEP_B)
        checkOrderOfTask(ITEM_A, ITEM_B, ITEM_C)

        // drag C above B
        dragTask(2, Direction.UP)
        checkOrderOfTask(ITEM_A, ITEM_C, ITEM_B)
        clickInDrawer(R.id.edit_departments)
        Espresso.pressBack()
        pauseTest(300)
        checkOrderOfTask(ITEM_A, ITEM_C, ITEM_B)

        addItem(ITEM_D, DEP_A)
        checkOrderOfTask(ITEM_A, ITEM_C, ITEM_B, ITEM_D)

        // drag D above B
        dragTask(3, Direction.UP)
        // D has diff dep as others, so its dep going above their dep, hence D is first
        checkOrderOfTask(ITEM_D, ITEM_A, ITEM_C, ITEM_B)
        clickInDrawer(R.id.edit_departments)
        Espresso.pressBack()
        pauseTest(300)
        checkOrderOfTask(ITEM_D, ITEM_A, ITEM_C, ITEM_B)

        // tick D, should fall at the bottom
        clickTickOfTaskAt(0)
        checkOrderOfTask(ITEM_A, ITEM_C, ITEM_B, COMPLETED, ITEM_D)

        // tick A
        clickTickOfTaskAt(0)
        checkOrderOfTask(ITEM_C, ITEM_B, COMPLETED, ITEM_A, ITEM_D)

        // untick D
        clickTickOfTaskAt(4)
        checkOrderOfTask(ITEM_D, ITEM_C, ITEM_B, COMPLETED, ITEM_A)

        // untick A
        clickTickOfTaskAt(4)
        checkOrderOfTask(ITEM_D, ITEM_A, ITEM_C, ITEM_B)

        // drag D under A
        dragTask(0, Direction.DOWN)
        checkOrderOfTask(ITEM_A, ITEM_C, ITEM_B, ITEM_D)

        // drag C above A
        dragTask(1, Direction.UP)
        checkOrderOfTask(ITEM_C, ITEM_A, ITEM_B, ITEM_D)

        // tick A
        clickTickOfTaskAt(1)
        checkOrderOfTask(ITEM_C, ITEM_B, ITEM_D, COMPLETED, ITEM_A)

        // untick C
        clickTickOfTaskAt(4)
        checkOrderOfTask(ITEM_C, ITEM_A, ITEM_B, ITEM_D)
    }


    @Test fun testChangeItemDep() {
        addItem(ITEM_A, DEP_B)
        addItem(ITEM_B, DEP_A)
        addItem(ITEM_C, DEP_A)
        checkOrderOfTask(ITEM_A, ITEM_B, ITEM_C)

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