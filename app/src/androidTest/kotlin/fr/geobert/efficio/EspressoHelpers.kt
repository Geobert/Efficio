package fr.geobert.efficio

import android.support.test.espresso.*
import android.support.test.espresso.action.ViewActions.actionWithAssertions
import android.support.test.espresso.matcher.ViewMatchers
import android.support.test.espresso.util.*
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ListView
import junit.framework.AssertionFailedError
import org.hamcrest.*

// sugar wrapper for the backquote
fun <T> iz(matcher: Matcher<T>): Matcher<T> = Matchers.`is`(matcher)

fun <T> iz(value: T): Matcher<T> = iz(Matchers.equalTo(value))

fun has(expectedCount: Int, selector: Matcher<View>): ViewAssertion {
    return ViewAssertion { view, exception ->
        if (view != null) {
            val descendants = TreeIterables.breadthFirstViewTraversal(view)
            val selected = descendants.filter { selector.matches(it) }
            if (selected.count() != expectedCount) {
                throw AssertionFailedError(HumanReadables.getViewHierarchyErrorMessage(view, selected,
                        "Found ${selected.count()} views instead of $expectedCount matching: $selector",
                        "****MATCHES****"))
            }
        }
    }
}

fun has(expectedCount: Int, clazz: Class<out View>) = has(expectedCount, ViewMatchers.isAssignableFrom(clazz))

fun pauseTest(t: Long) {
    try {
        Thread.sleep(t)
    } catch (e: Exception) {
    }
}

fun withListSize(expectedCount: Int): Matcher<View> {
    return object : TypeSafeMatcher<View>() {
        override fun describeTo(description: Description) {
            description.appendText("ListView should have $expectedCount items")
        }

        override fun matchesSafely(item: View?): Boolean {
            return if (item is ListView) item.count == expectedCount else false
        }

    }
}

fun withRecyclerViewSize(expectedCount: Int): Matcher<View> {
    return object : TypeSafeMatcher<View>() {
        override fun describeTo(description: Description) {
            description.appendText("RecyclerView should have $expectedCount items")
        }

        override fun matchesSafely(item: View?): Boolean {
            return if (item is RecyclerView) item.adapter.itemCount == expectedCount else false
        }

    }
}

fun dragViewInRecycler(dir: Direction, step: Int = 1): ViewAction {
    return actionWithAssertions(DragViewInRecyclerAction(dir, step))
}

fun withRecyclerView(recyclerViewId: Int): RecyclerViewMatcher {
    return RecyclerViewMatcher(recyclerViewId)
}