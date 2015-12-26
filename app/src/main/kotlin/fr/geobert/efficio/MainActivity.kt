package fr.geobert.efficio

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric

public class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!BuildConfig.DEBUG) Fabric.with(this, Crashlytics());
        setContentView(R.layout.main_activity)

        val f = TaskListFragment()
        fragmentManager.beginTransaction().replace(R.id.content, f).commit()
    }
}
