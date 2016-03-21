package fr.geobert.efficio.misc

import android.support.v4.content.ContextCompat
import android.support.v7.widget.Toolbar
import android.view.View
import fr.geobert.efficio.BaseActivity
import fr.geobert.efficio.R

interface EditorToolbarTrait : Toolbar.OnMenuItemClickListener {
    fun initToolbar(activity: BaseActivity) {
        activity.setIcon(android.R.drawable.ic_menu_close_clear_cancel)
        activity.setIconOnClick(View.OnClickListener { activity.onBackPressed() })
        activity.setMenu(R.menu.confirm_cancel_menu)
        activity.titleColor = ContextCompat.getColor(activity, android.R.color.primary_text_dark)
        activity.mToolbar.setOnMenuItemClickListener(this)
    }
}

