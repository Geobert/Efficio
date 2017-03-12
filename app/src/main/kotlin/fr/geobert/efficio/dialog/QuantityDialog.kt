package fr.geobert.efficio.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.os.Bundle
import android.view.View
import fr.geobert.efficio.MainActivity
import fr.geobert.efficio.R
import fr.geobert.efficio.data.Task
import fr.geobert.efficio.db.TaskTable
import kotlinx.android.synthetic.main.quantity_dialog.view.*
import kotlin.properties.Delegates

class QuantityDialog : DialogFragment() {
    companion object {
        fun newInstance(task: Task): QuantityDialog {
            val d = QuantityDialog()
            val b = Bundle()
            b.putLong("taskId", task.id)
            b.putInt("taskQty", task.qty)
            d.arguments = b
            return d
        }
    }

    private var customView by Delegates.notNull<View>()
    private var taskId by Delegates.notNull<Long>()
    private var taskQty by Delegates.notNull<Int>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val b = AlertDialog.Builder(activity)
        customView = activity.layoutInflater.inflate(R.layout.quantity_dialog, null)
        taskId = arguments.getLong("taskId")
        taskQty = arguments.getInt("taskQty")
        customView.qty_picker.minValue = 1
        customView.qty_picker.maxValue = 999
        customView.qty_picker.wrapSelectorWheel = false
        customView.qty_picker.value = taskQty
        b.setView(customView).setTitle("Quantity").
                setPositiveButton(android.R.string.ok, { _, _ -> onOkClicked() }).
                setNegativeButton(android.R.string.cancel, { d, _ -> d.cancel() })
        return b.create()
    }

    private fun onOkClicked() {
        customView.qty_picker.clearFocus()
        TaskTable.updateTaskQty(activity, taskId, customView.qty_picker.value)
        val a = activity
        if (a is MainActivity) {
            a.refreshTask(taskId)
            a.updateWidgets()
        }
    }
}