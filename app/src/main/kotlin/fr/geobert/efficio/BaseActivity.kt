package fr.geobert.efficio

import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.View

public abstract class BaseActivity : AppCompatActivity() {
    public val mToolbar: Toolbar by lazy { findViewById(R.id.my_toolbar) as Toolbar }

    public fun setIcon(id: Int) {
        mToolbar.setNavigationIcon(id)
    }

    public fun setIconOnClick(listener: View.OnClickListener) {
        mToolbar.setNavigationOnClickListener(listener)
    }

    public fun setMenu(id: Int) {
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
