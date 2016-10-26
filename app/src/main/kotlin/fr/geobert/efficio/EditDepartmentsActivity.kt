package fr.geobert.efficio

import android.app.*
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.helper.ItemTouchHelper
import android.text.SpannableStringBuilder
import android.view.*
import android.widget.LinearLayout
import fr.geobert.efficio.data.*
import fr.geobert.efficio.db.DepartmentTable
import fr.geobert.efficio.dialog.MessageDialog
import kotlinx.android.synthetic.main.department_chooser_dialog.*
import kotlinx.android.synthetic.main.edit_dep_text.view.*
import kotlin.properties.Delegates


class EditDepartmentsActivity : BaseActivity(), DepartmentManager.DepartmentChoiceListener {
    private var depManager: DepartmentManager by Delegates.notNull()
    private var storeId: Long by Delegates.notNull()

    private val onDepRenamedListener = object : EditDepartmentNameDialog.OnDepRenameListener {
        override fun onRenamingDone() {
            depManager.request()
        }
    }

    private val depTouchHelper by lazy { ItemTouchHelper(DepartmentDragHelper(this, depManager)) }

    companion object {
        fun callMe(frg: Fragment, storeId: Long) {
            val i = Intent(frg.activity, EditDepartmentsActivity::class.java)
            i.putExtra("storeId", storeId)
            frg.startActivityForResult(i, 1)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.edit_departments_activity)
        title = getString(R.string.edit_departments)
        storeId = intent.extras.getLong("storeId")
        depManager = DepartmentManager(this, findViewById(R.id.department_layout)!!,
                storeId, this, true)
        depManager.setEditMode(true)
        depManager.request()
        depTouchHelper.attachToRecyclerView(dep_list)
        setIcon(R.mipmap.ic_action_arrow_left)
        setIconOnClick(View.OnClickListener { onBackPressed() })
        setSpinnerVisibility(View.GONE)
        titleColor = ContextCompat.getColor(this, android.R.color.primary_text_dark)
    }

    override fun onBackPressed() {
        setResult(if (depManager.hasChanged()) Activity.RESULT_OK else Activity.RESULT_CANCELED)
        super.onBackPressed()
    }

    override fun onDepartmentChosen(d: Department) {
        EditDepartmentNameDialog.newInstance(d.id, d.name, onDepRenamedListener).show(fragmentManager,
                "RenameDepDialog")
    }

    class EditDepartmentNameDialog : DialogFragment() {

        interface OnDepRenameListener {
            fun onRenamingDone()
        }

        private var listener: OnDepRenameListener by Delegates.notNull()

        companion object {
            fun newInstance(depId: Long, depName: String, listener: OnDepRenameListener): EditDepartmentNameDialog {
                val d = EditDepartmentNameDialog()
                d.listener = listener
                val b = Bundle()
                b.putLong("depId", depId)
                b.putString("depName", depName)
                d.arguments = b
                return d
            }
        }

        private var customView: LinearLayout by Delegates.notNull()

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog? {
            val b = AlertDialog.Builder(activity)
            customView = LayoutInflater.from(activity).inflate(R.layout.edit_dep_text, null) as LinearLayout
            customView.edt.text = SpannableStringBuilder(arguments.getString("depName"))
            customView.edt.selectAll()
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