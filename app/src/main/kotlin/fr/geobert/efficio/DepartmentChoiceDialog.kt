package fr.geobert.efficio

import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.app.LoaderManager
import android.content.Loader
import android.database.Cursor
import android.os.Bundle
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import fr.geobert.efficio.adapter.DepartmentAdapter
import fr.geobert.efficio.adapter.DepartmentViewHolder
import fr.geobert.efficio.data.Department
import fr.geobert.efficio.db.DepartmentTable
import fr.geobert.efficio.db.StoreCompositionTable
import fr.geobert.efficio.misc.SpaceItemDecoration
import fr.geobert.efficio.misc.map
import kotlinx.android.synthetic.main.department_chooser_dialog.view.*
import java.util.*
import kotlin.properties.Delegates

class DepartmentChoiceDialog : DialogFragment(), LoaderManager.LoaderCallbacks<Cursor>,
        TextWatcher, DepartmentViewHolder.OnClickListener {
    interface DepartmentChoiceListener {
        fun onDepartmentChosen(d: Department)
        fun onChoiceCanceled()
    }

    private val TAG = "DepartmentChoiceDialog"
    private val GET_DEP_FROM_STORE = 200
    //private val GET_ALL_DEP = 210

    private var customView: View by Delegates.notNull()
    private var depAdapter: DepartmentAdapter by Delegates.notNull()
    private var depsList: MutableList<Department> by Delegates.notNull()
    var listener: DepartmentChoiceListener by Delegates.notNull()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)
        val builder = AlertDialog.Builder(activity)
        customView = activity.layoutInflater.inflate(R.layout.department_chooser_dialog, null)
        builder.setTitle(R.string.choose_or_create_dep)
                .setView(customView)
                .setNegativeButton(android.R.string.cancel, { d, i ->
                    d.cancel()
                })

        return builder.create()
    }

    fun init() {
        customView.dep_list.layoutManager = LinearLayoutManager(activity)
        customView.dep_list.itemAnimator = DefaultItemAnimator()
        customView.dep_list.addItemDecoration(SpaceItemDecoration(10, true))
        customView.dep_list.setHasFixedSize(true)
        customView.add_dep_btn.setOnClickListener {
            onAddDepClicked()
        }
        customView.add_dep_text.addTextChangedListener(this)
    }

    private fun onDepartmentChosen(d: Department) {
        listener.onDepartmentChosen(d)
        dialog.cancel()
    }

    private fun onAddDepClicked() {
        val t = customView.add_dep_text.text.toString()
        if (t.trim().length > 0) {
            var existingDep: Department? = null
            for (d in depsList) {
                if (d.name == t) {
                    existingDep = d
                    break
                }
            }

            if (existingDep != null) {
//                if (existingDep.weight == -1) {
//                    // department comes from another store, so add it to StoreCompositionTable
//                    StoreCompositionTable.create(activity, arguments.getLong("storeId"), existingDep)
//                }
                onDepartmentChosen(existingDep)
            } else {
                val d = Department(t)
                d.id = DepartmentTable.create(activity, d)
                if (d.id > 0) {
                    if (StoreCompositionTable.create(activity, arguments.getLong("storeId"), d) > 0) {
                        onDepartmentChosen(d)
                    } else {
                        Log.e(TAG, "create department weight failed")
                    }
                } else {
                    Log.e(TAG, "create department failed")
                }
            }
        }
    }

    fun request() {
        activity.loaderManager.initLoader(GET_DEP_FROM_STORE, arguments, this)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        init()
        request()
    }

    //
    // TextWatcher
    //

    override fun afterTextChanged(s: Editable) {
        if (depsList.count() > 0) {
            val filteredList = filter(depsList, s)
            depAdapter.animateTo(filteredList)
            customView.dep_list.scrollToPosition(0)
        }
    }

    private fun filter(list: MutableList<Department>, s: Editable): MutableList<Department> {
        val f = s.toString().toLowerCase()
        val filtered = LinkedList<Department>()
        for (d in list) {
            if (d.name.toLowerCase().contains(f)) {
                filtered.add(d)
            }
        }
        return filtered
    }

    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        // nothing
    }

    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        // nothing
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        loader.reset()
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
        fun setViewsVisibility(nb: Int) {
            if (nb > 0) {
                customView.empty_text.visibility = View.GONE
                customView.dep_list.visibility = View.VISIBLE
            } else {
                customView.empty_text.visibility = View.VISIBLE
                customView.dep_list.visibility = View.GONE
            }
        }

        when (loader.id) {
            GET_DEP_FROM_STORE -> {
                if (data.count > 0) {
                    val b = Bundle()
                    b.putInt("id", data.getColumnIndex(StoreCompositionTable.COL_DEP_ID))
                    b.putInt("name", data.getColumnIndex("dep_name"))
                    b.putInt("weight", data.getColumnIndex("dep_weight"))
                    depsList = data.map { Department(it, b) }
                    depAdapter = DepartmentAdapter(depsList, this)
                    customView.dep_list.adapter = depAdapter
                } else {
                    depsList = LinkedList<Department>()
                    //activity.loaderManager.initLoader(GET_ALL_DEP, Bundle(), this)
                }
            }
        //            GET_ALL_DEP -> {
        //                if (data.count > 0) {
        //                    val b = Bundle()
        //                    b.putInt("id", data.getColumnIndex(BaseColumns._ID))
        //                    b.putInt("name", data.getColumnIndex(DepartmentTable.COL_NAME))
        //                    depsList = data.map {
        //                        val d = Department(it, b)
        //                        d.weight = -1 // flag it as coming from another store
        //                        d
        //                    }
        //                    depAdapter = DepartmentAdapter(depsList, this)
        //                    customView.dep_list.adapter = depAdapter
        //                } else {
        //                    depsList = LinkedList<Department>()
        //                }
        //            }
            else -> throw IllegalArgumentException("Unknown cursorLoader id")
        }
        setViewsVisibility(data.count)
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor>? {
        return when (id) {
            GET_DEP_FROM_STORE -> StoreCompositionTable.getDepFromStoreLoader(activity,
                    args?.getLong("storeId") ?: 0)
        //GET_ALL_DEP -> DepartmentTable.fetchAllDep(activity)
            else -> null
        }
    }

    override fun onClick(d: Department) {
        onDepartmentChosen(d)
    }
}