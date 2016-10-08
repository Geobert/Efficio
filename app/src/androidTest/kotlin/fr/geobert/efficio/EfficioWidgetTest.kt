package fr.geobert.efficio

import android.content.Intent
import android.graphics.Point
import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso
import android.support.test.espresso.action.ViewActions
import android.support.test.espresso.matcher.ViewMatchers
import android.support.test.filters.LargeTest
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import android.support.test.uiautomator.By
import android.support.test.uiautomator.UiDevice
import android.support.test.uiautomator.Until
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.properties.Delegates


/**
 * Test the widget behavior,
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class EfficioWidgetTest {
    private var mDevice: UiDevice by Delegates.notNull()

    @Rule @JvmField var activityRule: ActivityTestRule<MainActivity> =
            ActivityTestRule(MainActivity::class.java)

    private val EFFICIO_PACKAGE = "fr.geobert.efficio"
    private val LAUNCH_TIMEOUT: Long = 5000

    val screenSize: Point by lazy { Point(mDevice.displayWidth, mDevice.displayHeight) }
    val screenCenter: Point by lazy { Point(screenSize.x / 2, screenSize.y / 2) }


    fun backToHome() {
        // back to home
        mDevice.pressHome()
        val launcherPackage = mDevice.launcherPackageName!!
        mDevice.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)), LAUNCH_TIMEOUT)
        pauseTest(800)
    }

    fun deleteWidgetOnHome() {
        backToHome()
        val widget = Point(screenCenter.x - 100, screenCenter.y)
        mDevice.swipe(arrayOf(widget, widget, Point(screenCenter.x, 200)), 100)
    }

    fun scrollWidgetListToTop() {
        var w = mDevice.findObject(By.text("Nova Launcher"))

        while (w == null) {
            mDevice.swipe(screenCenter.x, 80, screenCenter.x, screenCenter.y, 20)
            w = mDevice.findObject(By.text("Nova Launcher"))
        }
    }

    val STEPS = 80
    fun setWidgetOnHome() {
        backToHome()

        // long press on home to bring up widgets menu entry
        mDevice.swipe(arrayOf(screenCenter, screenCenter), 150)
        pauseTest(2000)

        // look for widgets button, with different case
        val tab = mDevice.findObject(By.text("Widgets"))
        if (tab != null) tab.click() else mDevice.findObject(By.text("WIDGETS")).click()
        mDevice.waitForIdle()

        // look for efficio's widget in a vertical list
        var widget = mDevice.findObject(By.text("Efficio"))
        var additionalSwipe = 0
        var retry = 10

        scrollWidgetListToTop()

        while ((widget == null || additionalSwipe > 0) && retry > 0) {
            mDevice.swipe(screenCenter.x, screenSize.y - 100, screenCenter.x, 0, STEPS)
            mDevice.waitForIdle()
            if (widget == null) {
                widget = mDevice.findObject(By.text("Efficio"))
                retry--
            } else {
                additionalSwipe--
            }
        }

        if (widget == null) { // nothing found, swipe the other way
            retry = 10
            while ((widget == null || additionalSwipe > 0) && retry > 0) {
                mDevice.swipe(screenCenter.x, 80, screenCenter.x, screenCenter.y, STEPS)
                mDevice.waitForIdle()
                if (widget == null) {
                    widget = mDevice.findObject(By.text("Efficio"))
                    retry--
                } else {
                    additionalSwipe--
                }
            }
        }

        var b = widget.visibleBounds
        if (b.centerY() > (screenSize.y - 150))
            mDevice.swipe(screenCenter.x, screenCenter.y, screenCenter.x, 0, STEPS)

        if (b.centerY() < 50)
            mDevice.swipe(screenCenter.x, 80, screenCenter.x, 200, STEPS)

        b = widget.visibleBounds
        // long press on the widget's pic
        val c = Point(b.left + 100, b.bottom + 50) // click on the widget image
        mDevice.swipe(arrayOf(c, c, screenCenter), 150)

        // keep default list and click ok
        mDevice.findObject(By.desc("OK")).click()

        // only test the presence of the widget, not the list
        Assert.assertNotNull(mDevice.findObject(By.text("Store")))
    }

    fun launchApp() {
        val ctx = InstrumentationRegistry.getInstrumentation().context
        val intent = ctx.packageManager.getLaunchIntentForPackage(EFFICIO_PACKAGE)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        ctx.startActivity(intent)
        mDevice.wait(Until.hasObject(By.pkg(EFFICIO_PACKAGE).depth(0)), 5000)
    }

    @Before fun setup() {
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        MainActivity.TEST_MODE = true
        launchApp() // clear DB
        MainActivity.TEST_MODE = false
        deleteWidgetOnHome()
        setWidgetOnHome()
    }

    @Test fun testWidget() {
        // test add item
        launchApp()
        addItem(ITEM_A, DEP_A)
        backToHome()
        Assert.assertNotNull(mDevice.findObject(By.text(ITEM_A)))

        // test add another item
        launchApp()
        addItem(ITEM_B, DEP_B)
        backToHome()
        Assert.assertNotNull(mDevice.findObject(By.text(ITEM_B)))

        // test check item
        launchApp()
        clickTickOfTaskAt(0)
        backToHome()
        Assert.assertNull(mDevice.findObject(By.text(ITEM_A)))

        // test uncheck item
        launchApp()
        clickTickOfTaskAt(2)
        backToHome()
        Assert.assertNotNull(mDevice.findObject(By.text(ITEM_A)))

        // test delete item
        launchApp()
        clickOnTask(1)
        Espresso.onView(ViewMatchers.withId(R.id.delete_task_btn)).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withText("OK")).perform(ViewActions.click())
        backToHome()
        Assert.assertNotNull(mDevice.findObject(By.text(ITEM_A)))
        Assert.assertNull(mDevice.findObject(By.text(ITEM_B)))

    }
}