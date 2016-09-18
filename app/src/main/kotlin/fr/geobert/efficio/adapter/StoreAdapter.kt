package fr.geobert.efficio.adapter

import android.support.v4.app.FragmentActivity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import fr.geobert.efficio.R
import fr.geobert.efficio.data.Store
import kotlinx.android.synthetic.main.store_row.view.*
import java.util.*

class StoreAdapter(val activity: FragmentActivity, list: MutableList<Store>) : BaseAdapter() {
    val storeList = LinkedList(list)
    var darkText = false

    override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View? {
        val h: StoreViewHolder = if (p1 == null) {
            val v = activity.layoutInflater.inflate(R.layout.store_row, p2, false)
            if (darkText)
                v.store_name_lbl.setTextColor(R.drawable.toolbar_spinner_row_selector_dark)
            val t = StoreViewHolder(v)
            v.tag = t
            t
        } else {
            p1.tag as StoreViewHolder
        }

        if (storeList.isEmpty()) {
            return null
        }

        val s = storeList[p0]
        h.name.text = s.name
        return h.view
    }

    override fun getItem(p0: Int): Any? {
        return storeList[p0]
    }

    override fun getItemId(p0: Int): Long {
        return storeList[p0].id
    }

    override fun getCount(): Int {
        return storeList.count()
    }

    fun renameStore(name: String, storeId: Long) {
        val s = storeList.find { it.id == storeId }
        s!!.name = name
        notifyDataSetChanged()
    }

    fun deleteStore(storeId: Long) {
        val s = storeList.find { it.id == storeId }
        storeList.remove(s)
        notifyDataSetChanged()
    }
}