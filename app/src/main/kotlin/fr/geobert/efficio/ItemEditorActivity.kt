package fr.geobert.efficio

import android.app.*
import android.content.*
import android.database.Cursor
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.util.Log
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.*
import fr.geobert.efficio.data.*
import fr.geobert.efficio.db.TaskTable
import fr.geobert.efficio.dialog.DeleteConfirmationDialog
import fr.geobert.efficio.misc.*
import kotlinx.android.synthetic.main.item_editor.*
import kotlin.properties.Delegates

class ItemEditorActivity : BaseActivity(), DepartmentManager.DepartmentChoiceListener,
        LoaderManager.LoaderCallbacks<Cursor>, EditorToolbarTrait, DeleteDialogInterface {
    private var task: Task by Delegates.notNull()
    private var origTask: Task by Delegates.notNull()
    private var depManager: DepartmentManager by Delegates.notNull()
    private var cursorLoader: CursorLoader? = null

    companion object {
        val NEED_WEIGHT_UPDATE = "needUpdateWeight"
        fun callMe(ctx: Fragment, storeId: Long, task: Task) {
            val i = Intent(ctx.activity, ItemEditorActivity::class.java)
            i.putExtra("storeId", storeId)
            i.putExtra("taskId", task.id)
            ctx.startActivityForResult(i, 0)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.item_editor)
        initToolbar(this)
        setTitle(R.string.change_item_name_and_dep)
        val extras = intent.extras
        depManager = DepartmentManager(this, findViewById(R.id.department_layout)!!,
                extras.getLong("storeId"), this)
        depManager.request()
        delete_task_btn.setOnClickListener { onDeleteClicked() }
        initPeriodWidgets()
        fetchTask()
    }

    private fun initPeriodWidgets() {
        val adapter = ArrayAdapter.createFromResource(this, R.array.periodicity_choices,
                android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        period_spinner.adapter = adapter
        val adapterUnit = ArrayAdapter.createFromResource(this, R.array.periodicity_units,
                android.R.layout.simple_spinner_item)
        adapterUnit.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        period_unit_spinner.adapter = adapterUnit

        cancel_custom_period.setOnClickListener {
            setCustomPeriodContVisibility(false)
            period_spinner.setSelection(if (task.periodicity != Period.CUSTOM) // avoid being stuck in custom period mode
                task.periodicity.ordinal else Period.NONE.ordinal)
        }

        period_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, pos: Int, id: Long) {
                setCustomPeriodContVisibility(pos == (period_spinner.adapter.count - 1))
            }

            override fun onNothingSelected(arg0: AdapterView<*>) {
                // nothing
            }
        }
    }

    private fun setCustomPeriodContVisibility(visible: Boolean) {
        val fadeIn = AnimationUtils.makeInAnimation(this, false)
        val fadeOut = AnimationUtils.makeOutAnimation(this, true)

        if (visible && custom_periodicity_cont.visibility == View.GONE) {
            period_spinner.startAnimation(fadeOut)
            period_spinner.visibility = View.GONE
            custom_periodicity_cont.visibility = View.VISIBLE
            custom_periodicity_cont.startAnimation(fadeIn)
        } else if (period_spinner.visibility == View.GONE) {
            custom_periodicity_cont.startAnimation(fadeOut)
            custom_periodicity_cont.visibility = View.GONE
            period_spinner.visibility = View.VISIBLE
            period_spinner.startAnimation(fadeIn)
        }
    }

    override fun onDeletedConfirmed() {
        TaskTable.deleteTask(this, task.id)
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun onDeleteClicked() {
        val d = DeleteConfirmationDialog.newInstance(getString(R.string.confirm_delete_task),
                getString(R.string.delete_task), DELETE_TASK)
        d.show(fragmentManager, "DeleteConfirmDialog")
    }

    private fun fetchTask() {
        if (cursorLoader == null)
            loaderManager.initLoader(GET_TASK, intent.extras, this)
        else
            loaderManager.restartLoader(GET_TASK, intent.extras, this)
    }

    private fun onOkClicked() {
        if (!fillTask()) {
            Snackbar.make(my_toolbar, R.string.need_valid_period, Snackbar.LENGTH_SHORT).show()
            return
        }
        if (!task.isEquals(origTask)) {
            // change department, the item's weight is not relevant anymore
            val data = Intent()
            if (task.item.department.id != origTask.item.department.id) {
                data.putExtra(NEED_WEIGHT_UPDATE, true)
                data.putExtra("taskId", task.id)
            } else {
                data.putExtra(NEED_WEIGHT_UPDATE, false)
            }
            TaskTable.updateTask(this, task)
            setResult(Activity.RESULT_OK, data)
        } else setResult(Activity.RESULT_CANCELED)
        finish()
    }

    private fun fillTask(): Boolean {
        task.item.name = item_name_edt.text.trim().toString()
        when (Period.fromInt(period_spinner.selectedItemPosition)) {
            Period.NONE -> {
                task.periodUnit = PeriodUnit.NONE
                task.period = 0
            }
            Period.CUSTOM -> {
                task.periodUnit = PeriodUnit.fromInt(period_unit_spinner.selectedItemPosition + 1)
                try {
                    task.period = period_edt.text.trim().toString().toInt()
                } catch (e: NumberFormatException) {
                    Log.e("ItemEditorActivity", "error converting toInt: ${period_edt.text}")
                    return false
                }
            }
            else -> {
                task.periodUnit = PeriodUnit.fromInt(period_unit_spinner.selectedItemPosition + 1)
                task.period = 1
            }
        }
        return true
    }

    override fun onDepartmentChosen(d: Department) {
        task.item.department = d
        setDepName()
    }

    private fun setDepName() {
        dep_name.text =
                getString(R.string.current_department).format(task.item.department.name)
    }

    override fun onMenuItemClick(item: MenuItem): Boolean = when (item.itemId) {
        R.id.confirm -> consume { onOkClicked() }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onCreateLoader(p0: Int, b: Bundle): Loader<Cursor>? {
        cursorLoader = TaskTable.getTaskByIdLoader(this, b.getLong("taskId"))
        return cursorLoader
    }

    override fun onLoadFinished(p0: Loader<Cursor>?, data: Cursor) {
        if (data.count > 0) {
            data.moveToFirst()
            task = Task(data)
            origTask = Task(data)
            updateUI()
        } else {
            // todo should not happen
        }
    }

    private fun updateUI() {
        item_name_edt.setText(task.item.name)
        setDepName()
        period_spinner.setSelection(task.periodicity.ordinal, true)
        if (task.periodicity == Period.CUSTOM) {
            period_edt.setText(task.period.toString())
            period_unit_spinner.setSelection(task.periodUnit.ordinal)
        } else {
            period_edt.setText("1")
        }
    }

    override fun onLoaderReset(p0: Loader<Cursor>?) {
        // todo
    }
}