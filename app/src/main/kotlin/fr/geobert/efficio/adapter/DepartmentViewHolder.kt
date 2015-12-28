package fr.geobert.efficio.adapter

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import fr.geobert.efficio.R
import fr.geobert.efficio.data.Department

class DepartmentViewHolder(val l: View, val listener: OnClickListener) :
        RecyclerView.ViewHolder(l), View.OnClickListener {
    interface OnClickListener {
        fun onClick(d: Department)
    }

    val name = l.findViewById(R.id.dep_name) as TextView
    var dep: Department? = null

    init {
        l.setOnClickListener(this)
    }

    fun bind(d: Department) {
        name.text = d.name
        dep = d
    }

    override fun onClick(p0: View?) {
        val d = dep
        if (d != null) {
            listener.onClick(d)
        }
    }
}