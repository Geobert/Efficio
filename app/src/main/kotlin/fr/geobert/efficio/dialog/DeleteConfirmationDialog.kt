package fr.geobert.efficio.dialog

import android.R
import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.content.DialogInterface
import android.os.Bundle
import kotlin.properties.Delegates

class DeleteConfirmationDialog : DialogFragment() {
    var onClick: ((d: DialogInterface, i: Int) -> Unit) by Delegates.notNull()

    companion object {
        fun newInstance(msg: String, title: String, onClick: ((d: DialogInterface, i: Int) -> Unit)): DeleteConfirmationDialog {
            val d = DeleteConfirmationDialog()
            d.onClick = onClick
            val b = Bundle()
            b.putString("msg", msg)
            b.putString("title", title)
            d.arguments = b
            return d
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)
        builder.setMessage(arguments.getString("msg")).setTitle(arguments.getString("title"))
                .setCancelable(false)
                .setPositiveButton(R.string.yes, onClick)
                .setNegativeButton(R.string.cancel, { d, i -> d.cancel() })
        return builder.create()
    }
}