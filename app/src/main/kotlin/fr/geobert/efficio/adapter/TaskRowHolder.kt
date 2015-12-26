package fr.geobert.efficio.adapter

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import fr.geobert.efficio.R

class TaskRowHolder(val view: View) : RecyclerView.ViewHolder(view) {
    var name = view.findViewById(R.id.name) as TextView
    var checkbox = view.findViewById(R.id.checkbox) as CheckBox
}