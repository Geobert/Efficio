package fr.geobert.efficio

import android.app.Dialog
import android.os.Bundle
import android.text.SpannableStringBuilder
import fr.geobert.efficio.data.Department
import fr.geobert.efficio.data.Task
import fr.geobert.efficio.db.TaskTable
import kotlinx.android.synthetic.main.item_editor_dialog.view.*
import kotlin.properties.Delegates

class ItemEditorDialog : DepartmentChoiceDialog() {
    private var task: Task by Delegates.notNull()
    private var origTask: Task by Delegates.notNull()
    var editListener: ItemEditorListener? = null

    interface ItemEditorListener {
        fun onItemEditFinished(needUpdate: Boolean)
    }

    companion object {
        fun newInstance(storeId: Long, task: Task,
                        listener: ItemEditorListener): DepartmentChoiceDialog {
            val d = ItemEditorDialog()
            d.editListener = listener
            val b = Bundle()
            b.putLong("storeId", storeId)
            b.putParcelable("task", task)
            d.arguments = b
            return d
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val b = createDialogBuilder(R.layout.item_editor_dialog, savedInstanceState)
        b.setTitle(R.string.change_item_name_and_dep)
        b.setPositiveButton(android.R.string.ok, { dialogInterface, i ->
            onOkClicked()
        })
        return b.create()
    }

    private fun onOkClicked() {
        task.item.name = customView.item_name_edt.text.trim().toString()
        val needUpdate = !task.isEquals(origTask)
        if (needUpdate) {
            TaskTable.updateTask(activity, task)
        }
        dialog.cancel()
        editListener?.onItemEditFinished(needUpdate)
    }

    override fun init() {
        super.init()
        task = arguments.getParcelable<Task>("task")
        origTask = Task(task)
        customView.item_name_edt.text = SpannableStringBuilder(task.item.name)
        setDepName()
    }

    override fun onDepartmentChosen(d: Department) {
        task.item.department = d
        setDepName()
    }

    private fun setDepName() {
        customView.dep_name.text =
                getString(R.string.current_department).format(task.item.department.name)
    }

}