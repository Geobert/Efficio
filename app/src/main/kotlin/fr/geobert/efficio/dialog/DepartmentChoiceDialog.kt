package fr.geobert.efficio.dialog

import android.app.*
import android.os.Bundle
import android.view.View
import fr.geobert.efficio.R
import fr.geobert.efficio.data.*
import kotlin.properties.Delegates


open class DepartmentChoiceDialog : DialogFragment(), DepartmentManager.DepartmentChoiceListener {
    private var manager: DepartmentManager by Delegates.notNull()
    private val TAG = "DepartmentChoiceDialog"
    protected var customView: View by Delegates.notNull()

    companion object {
        fun newInstance(storeId: Long): DepartmentChoiceDialog {
            val d = DepartmentChoiceDialog()
            val b = Bundle()
            b.putLong("storeId", storeId)
            d.arguments = b
            return d
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val b = createDialogBuilder(R.layout.department_chooser_dialog, savedInstanceState)
        b.setTitle(R.string.choose_or_create_dep)
        return b.create()
    }

    protected fun createDialogBuilder(layoutId: Int, savedInstanceState: Bundle?): AlertDialog.Builder {
        val builder = AlertDialog.Builder(activity)
        customView = activity.layoutInflater.inflate(layoutId, null)
        builder.setView(customView)
                .setNegativeButton(android.R.string.cancel, { d, i ->
                    d.cancel()
                })
        return builder
    }

    override fun onDepartmentChosen(d: Department) {
        val f = targetFragment
        if (f is DepartmentManager.DepartmentChoiceListener) f.onDepartmentChosen(d)
        dialog.cancel()
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        manager = DepartmentManager(activity, customView, arguments.getLong("storeId"), this)
        manager.request()
    }


}