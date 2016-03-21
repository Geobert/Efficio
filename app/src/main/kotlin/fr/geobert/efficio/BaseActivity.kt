package fr.geobert.efficio

import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.View

abstract class BaseActivity : AppCompatActivity() {
    val mToolbar: Toolbar by lazy { findViewById(R.id.my_toolbar) as Toolbar }

    fun setIcon(id: Int) {
        mToolbar.setNavigationIcon(id)
    }

    fun setIconOnClick(listener: View.OnClickListener) {
        mToolbar.setNavigationOnClickListener(listener)
    }

    fun setMenu(id: Int) {
        mToolbar.inflateMenu(id)
    }

    override fun setTitle(title: CharSequence?) {
        mToolbar.title = title
    }

    override fun setTitle(titleId: Int) {
        mToolbar.setTitle(titleId)
    }

    override fun setTitleColor(textColor: Int) {
        mToolbar.setTitleTextColor(textColor)
    }
}
