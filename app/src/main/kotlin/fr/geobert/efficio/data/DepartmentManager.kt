package fr.geobert.efficio.data

import android.app.*
import android.content.Loader
import android.database.Cursor
import android.os.Bundle
import android.support.v7.widget.*
import android.text.*
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import fr.geobert.efficio.R
import fr.geobert.efficio.adapter.*
import fr.geobert.efficio.db.*
import fr.geobert.efficio.misc.*
import kotlinx.android.synthetic.main.department_chooser_dialog.view.*
import java.util.*
import kotlin.properties.Delegates


class DepartmentManager(val activity: Activity,
                        val layout: View,
                        val storeId: Long,
                        val listener: DepartmentChoiceListener,
                        val trackDifference: Boolean = false) :
        LoaderManager.LoaderCallbacks<Cursor>, TextWatcher, DepartmentViewHolder.OnClickListener {

    val TAG = "DepartmentManager"
    private var list: RecyclerView by Delegates.notNull()
    private var addDepBtn: ImageButton by Delegates.notNull()
    private var addDepEdt: EditText by Delegates.notNull()
    private var emptyTxt: TextView by Delegates.notNull()
    private var cursorLoader: Loader<Cursor>? = null

    interface DepartmentChoiceListener {
        fun onDepartmentChosen(d: Department)
    }

    var depAdapter: DepartmentAdapter by Delegates.notNull()
    var departmentsList: MutableList<Department> by Delegates.notNull()
    private var origDepList: MutableList<Department>? = null

    init {
        list = layout.dep_list
        //        list = layout.findViewById(R.id.dep_list) as RecyclerView
        addDepBtn = layout.findViewById(R.id.add_dep_btn) as ImageButton
        addDepEdt = layout.findViewById(R.id.add_dep_text) as EditText
        emptyTxt = layout.findViewById(R.id.empty_text) as TextView

        list.layoutManager = LinearLayoutManager(activity)
        list.itemAnimator = DefaultItemAnimator()
        list.addItemDecoration(TopBottomSpaceItemDecoration(10))
        list.setHasFixedSize(true)
        addDepBtn.setOnClickListener { onAddDepClicked() }
        addDepEdt.addTextChangedListener(this)
        addDepEdt.setOnEditorActionListener { textView, i, keyEvent ->
            onEditorAction(i)
        }
    }

    private fun onEditorAction(actionId: Int): Boolean {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            onAddDepClicked()
            return true
        }
        return false
    }

    fun nbDepartment() = departmentsList.size
    fun getDepartment(pos: Int) = departmentsList[pos]

    fun setEditMode(isEdit: Boolean) {
        if (isEdit) {
            addDepBtn.visibility = View.GONE
            addDepEdt.visibility = View.GONE
            layout.empty_text.text = SpannableStringBuilder(activity.getString(R.string.no_dep_edit))
        } else {
            addDepEdt.visibility = View.VISIBLE
            addDepBtn.visibility = View.VISIBLE
            layout.empty_text.text = SpannableStringBuilder(activity.getString(R.string.no_dep))
        }
    }

    private fun maxDepWeight(): Double {
        return if (departmentsList.size > 0) departmentsList.last().weight else 0.0
    }

    private fun onAddDepClicked() {
        val t = addDepEdt.text.trim().toString()
        if (t.length > 0) {
            val n = t.normalize()
            var existingDep: Department? = null
            for (d in departmentsList) {
                if (d.name.normalize() == n) {
                    existingDep = d
                    break
                }
            }

            if (existingDep != null) {
                onDepartmentChosen(existingDep)
            } else {
                val d = Department(t, maxDepWeight() + 1.0)
                d.id = DepartmentTable.create(activity, d)
                if (d.id > 0) {
                    if (StoreCompositionTable.create(activity, storeId, d) > 0) {
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

    private fun onDepartmentChosen(department: Department) {
        listener.onDepartmentChosen(department)
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        loader.reset()
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
        fun setViewsVisibility(nb: Int) {
            if (nb > 0) {
                emptyTxt.visibility = View.GONE
                list.visibility = View.VISIBLE
            } else {
                emptyTxt.visibility = View.VISIBLE
                list.visibility = View.GONE
            }
        }

        when (loader.id) {
            GET_DEP_FROM_STORE -> {
                cursorLoader = loader
                if (data.count > 0) {
                    val b = Bundle()
                    b.putInt("id", data.getColumnIndex(StoreCompositionTable.COL_DEP_ID))
                    b.putInt("name", data.getColumnIndex("dep_name"))
                    b.putInt("weight", data.getColumnIndex("dep_weight"))
                    departmentsList = data.map { Department(it, b) }
                    if (trackDifference && origDepList == null)
                        origDepList = data.map { Department(it, b) }
                    depAdapter = DepartmentAdapter(departmentsList, this)
                    list.adapter = depAdapter
                } else {
                    departmentsList = LinkedList<Department>()
                }
            }
            else -> throw IllegalArgumentException("Unknown cursorLoader id")
        }
        setViewsVisibility(data.count)
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor>? {
        return when (id) {
            GET_DEP_FROM_STORE -> StoreCompositionTable.getDepFromStoreLoader(activity, storeId)
            else -> null
        }
    }

    override fun onClick(d: Department) {
        onDepartmentChosen(d)
    }

    fun request(reset: Boolean = false) {
        if (reset) origDepList = null
        val b = Bundle()
        b.putLong("storeId", storeId)
        activity.loaderManager.destroyLoader(GET_DEP_FROM_STORE)
        activity.loaderManager.initLoader(GET_DEP_FROM_STORE, b, this)
    }

    fun hasChanged(): Boolean {
        val o = origDepList
        return if (o != null) compareLists(o, departmentsList) != 0 else false
    }

    //
    // TextWatcher
    //

    override fun afterTextChanged(s: Editable) {
        if (departmentsList.count() > 0) {
            val filteredList = filter(departmentsList, s)
            depAdapter.animateTo(filteredList)
            list.scrollToPosition(0)
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

    fun remove(department: Department) {
        val pos = departmentsList.indexOf(department)
        depAdapter.removeItem(pos)
        departmentsList.remove(department)
        depAdapter.notifyItemRemoved(pos)
    }
}