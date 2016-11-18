package fr.geobert.efficio

import android.content.*
import android.os.Bundle
import android.preference.PreferenceFragment
import android.view.View

class SettingsActivity : BaseActivity() {

    companion object {
        fun callMe(ctx: Context) {
            val i = Intent(ctx, SettingsActivity::class.java)
            ctx.startActivity(i)
        }
    }

    class SettingsFragment : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            addPreferencesFromResource(R.xml.settings)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        setSpinnerVisibility(View.GONE)
        setTitle(R.string.settings)
        setIcon(R.mipmap.ic_action_arrow_left)
        setIconOnClick(View.OnClickListener { onBackPressed() })

        if (savedInstanceState == null) {
            fragmentManager.beginTransaction().replace(R.id.flContent, SettingsFragment(),
                    "settings").commit()
        }
    }
}

