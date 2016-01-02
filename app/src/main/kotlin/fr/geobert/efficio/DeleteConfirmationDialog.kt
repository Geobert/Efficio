package fr.geobert.efficio

import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.content.DialogInterface
import android.os.Bundle
import kotlin.properties.Delegates

class DeleteConfirmationDialog : DialogFragment() {
    var onClick: ((d: DialogInterface, i: Int) -> Unit) by Delegates.notNull()

    companion object {
        fun newInstance(msg: Int, titleId: Int, onClick: ((d: DialogInterface, i: Int) -> Unit)): DeleteConfirmationDialog {
            val d = DeleteConfirmationDialog()
            d.onClick = onClick
            val b = Bundle()
            b.putInt("msg", msg)
            b.putInt("title", titleId)
            d.arguments = b
            return d
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)
        builder.setMessage(arguments.getInt("msg")).setTitle(arguments.getInt("title"))
                .setCancelable(false)
                .setPositiveButton(android.R.string.yes, onClick)
                .setNegativeButton(android.R.string.cancel, { d, i -> d.cancel() })
        return builder.create()
    }
}