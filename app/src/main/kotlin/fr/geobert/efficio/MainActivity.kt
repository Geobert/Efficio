package fr.geobert.efficio

import android.os.Bundle
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric

public class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!BuildConfig.DEBUG) Fabric.with(this, Crashlytics());
        setContentView(R.layout.main_activity)
        setTitle(R.string.app_name)
        val f = TaskListFragment()
        fragmentManager.beginTransaction().replace(R.id.content, f).commit()
    }
}
