package fr.geobert.efficio.misc

import android.support.v4.content.ContextCompat
import android.support.v7.widget.Toolbar
import android.view.View
import fr.geobert.efficio.BaseActivity
import fr.geobert.efficio.R

interface EditorToolbarTrait : Toolbar.OnMenuItemClickListener {
    fun initToolbar(activity: BaseActivity) {
        activity.setIcon(R.drawable.cancel_dark)
        activity.setIconOnClick(View.OnClickListener { activity.onBackPressed() })
        activity.setMenu(R.menu.confirm_cancel_menu)
        activity.titleColor = ContextCompat.getColor(activity, R.color.colorPrimaryDark)
        activity.mToolbar.setOnMenuItemClickListener(this)
    }
}

