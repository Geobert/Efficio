package fr.geobert.efficio.adapter

import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import fr.geobert.efficio.R
import fr.geobert.efficio.data.Department

class DepartmentViewHolder(l: View, val listener: OnClickListener) :
        RecyclerView.ViewHolder(l), View.OnClickListener {
    interface OnClickListener {
        fun onClick(d: Department)
    }

    val name = l.findViewById<TextView>(R.id.dep_name)
    var dep: Department? = null
    val cardView: CardView = l.findViewById(R.id.card_view)

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