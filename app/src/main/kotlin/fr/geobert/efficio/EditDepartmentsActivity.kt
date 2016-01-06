package fr.geobert.efficio

import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.app.Fragment
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import fr.geobert.efficio.adapter.DepartmentViewHolder
import fr.geobert.efficio.data.Department
import fr.geobert.efficio.db.DepartmentTable
import fr.geobert.efficio.db.StoreCompositionTable
import kotlinx.android.synthetic.main.department_chooser_dialog.*
import kotlinx.android.synthetic.main.edit_dep_text.view.*
import java.util.*
import kotlin.properties.Delegates

class EditDepartmentsActivity : BaseActivity(), DepartmentManager.DepartmentChoiceListener {
    private var depManager: DepartmentManager by Delegates.notNull()
    private var storeId: Long by Delegates.notNull()

    private val onDepRenamedListener = object : EditDepartmentNameDialog.OnDepRenameListener {
        override fun onRenamingDone() {
            depManager.request()
        }
    }

    private val dragNDropMgr =
            object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
                override fun onMove(recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder,
                                    target: RecyclerView.ViewHolder): Boolean {
                    Collections.swap(depManager.departmentsList, viewHolder.adapterPosition,
                            target.adapterPosition)
                    depManager.depAdapter.notifyItemMoved(viewHolder.adapterPosition,
                            target.adapterPosition)
                    updateDepWeight(viewHolder as DepartmentViewHolder, target as DepartmentViewHolder)
                    return true
                }

                private var orig: Float = 0f
                private var lastDragTask: DepartmentViewHolder? = null

                override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                    super.onSelectedChanged(viewHolder, actionState)
                    // end of drag n drop, adapter is correctly ordered but not our representation here
                    val vh = viewHolder as DepartmentViewHolder?
                    if (vh == null) {
                        val activity = this@EditDepartmentsActivity
                        lastDragTask!!.cardView.cardElevation = orig
                        StoreCompositionTable.updateDepWeight(activity, lastDragTask!!.dep!!)
                    } else {
                        orig = vh.cardView.cardElevation
                        vh.cardView.cardElevation = 20.0f
                    }
                    lastDragTask = vh
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder?, direction: Int) {
                    // nothing
                }
            }

    private val depTouchHelper = ItemTouchHelper(dragNDropMgr)

    companion object {
        fun callMe(frg: Fragment, storeId: Long) {
            val i = Intent(frg.activity, EditDepartmentsActivity::class.java)
            i.putExtra("storeId", storeId)
            frg.startActivityForResult(i, 1)
        }
    }

    private fun updateDepWeight(dragged: DepartmentViewHolder, target: DepartmentViewHolder) {
        if (dragged.adapterPosition < target.adapterPosition) {
            // going up
            if (dragged.dep!!.weight <= target.dep!!.weight)
                dragged.dep!!.weight = target.dep!!.weight + 1

        } else {
            // going down
            if (dragged.dep!!.weight >= target.dep!!.weight)
                dragged.dep!!.weight = target.dep!!.weight - 1
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.edit_departments_activity)
        title = getString(R.string.edit_departments)
        storeId = intent.extras.getLong("storeId")
        depManager = DepartmentManager(this, findViewById(R.id.department_layout), storeId, this)
        depManager.setEditMode(true)
        depManager.request()
        depTouchHelper.attachToRecyclerView(dep_list)
        setIcon(R.mipmap.ic_action_arrow_left)
        setIconOnClick(View.OnClickListener { onBackPressed() })
        titleColor = ContextCompat.getColor(this, android.R.color.primary_text_dark)
    }

    override fun onDepartmentChosen(d: Department) {
        EditDepartmentNameDialog.newInstance(d.id, onDepRenamedListener).show(fragmentManager,
                "RenameDepDialog")
    }

    class EditDepartmentNameDialog : DialogFragment() {

        interface OnDepRenameListener {
            fun onRenamingDone()
        }

        private var listener: OnDepRenameListener by Delegates.notNull()

        companion object {
            fun newInstance(depId: Long, listener: OnDepRenameListener): EditDepartmentNameDialog {
                val d = EditDepartmentNameDialog()
                d.listener = listener
                val b = Bundle()
                b.putLong("depId", depId)
                d.arguments = b
                return d
            }
        }

        private var customView: LinearLayout by Delegates.notNull()

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog? {
            val b = AlertDialog.Builder(activity)
            customView = LayoutInflater.from(activity).inflate(R.layout.edit_dep_text, null) as LinearLayout

            b.setTitle(R.string.edit_dep_name).setView(customView).setCancelable(true).
                    setPositiveButton(android.R.string.ok, { d, i -> onOkClicked() }).
                    setNegativeButton(android.R.string.cancel, { d, i -> d.cancel() })
            return b.create()
        }

        private fun onOkClicked() {
            val s = customView.edt.text.trim().toString()
            if (s.length > 0) {
                if (DepartmentTable.updateDepartment(activity, arguments.getLong("depId"), s) > 0) {
                    listener.onRenamingDone()
                    dialog.cancel()
                } else {
                    MessageDialog.newInstance(R.string.error_on_update_dep_name,
                            R.string.error_title).show(activity.fragmentManager,
                            "ErrDialog")
                }
            } else {
                MessageDialog.newInstance(R.string.error_empty_dep_name,
                        R.string.error_title).show(activity.fragmentManager,
                        "ErrDialog")
            }
        }
    }
}