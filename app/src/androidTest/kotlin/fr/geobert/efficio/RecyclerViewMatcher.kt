package fr.geobert.efficio

import android.content.res.Resources
import android.support.v7.widget.RecyclerView
import android.view.View
import org.hamcrest.*

/**
 * Created by dannyroa on 5/10/15.
 * https://github.com/dannyroa/espresso-samples/blob/master/RecyclerView/app/src/androidTest/java/com/dannyroa/espresso_samples/recyclerview/RecyclerViewMatcher.java
 */
class RecyclerViewMatcher(private val recyclerViewId: Int) {

    fun atPosition(position: Int): Matcher<View> {
        return atPositionOnView(position, -1)
    }

    fun atPositionOnView(position: Int, targetViewId: Int): Matcher<View> {

        return object : TypeSafeMatcher<View>() {
            internal var resources: Resources? = null
            internal var childView: View? = null

            override fun describeTo(description: Description) {
                var idDescription = Integer.toString(recyclerViewId)
                if (this.resources != null) {
                    try {
                        idDescription = this.resources!!.getResourceName(recyclerViewId)
                    } catch (var4: Resources.NotFoundException) {
                        idDescription = String.format("%s (resource name not found)",
                                *arrayOf<Any>(Integer.valueOf(recyclerViewId)))
                    }

                }

                description.appendText("with id: " + idDescription)
            }

            override fun matchesSafely(view: View): Boolean {

                this.resources = view.resources

                if (childView == null) {
                    val recyclerView = view.rootView.findViewById(recyclerViewId) as RecyclerView
                    if (recyclerView.id == recyclerViewId) {
                        childView = recyclerView.findViewHolderForAdapterPosition(position).itemView
                    } else {
                        return false
                    }
                }

                if (targetViewId == -1) {
                    return view === childView
                } else {
                    val targetView = childView!!.findViewById(targetViewId)
                    return view === targetView
                }

            }
        }
    }
}