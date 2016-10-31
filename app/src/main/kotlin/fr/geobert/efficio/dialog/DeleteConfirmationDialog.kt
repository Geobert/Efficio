package fr.geobert.efficio.dialog

import android.R
import android.app.*
import android.os.Bundle
import fr.geobert.efficio.misc.*

class DeleteConfirmationDialog : DialogFragment() {
    companion object {

        fun newInstance(msg: String, title: String, action: Int): DeleteConfirmationDialog {
            val d = DeleteConfirmationDialog()
            val b = Bundle()
            b.putString("msg", msg)
            b.putString("title", title)
            b.putInt("action", action)
            d.arguments = b
            return d
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)
        builder.setMessage(arguments.getString("msg")).setTitle(arguments.getString("title"))
                .setCancelable(false)
                .setPositiveButton(R.string.yes, { d, i ->
                    when (arguments.getInt("action")) {
                        DELETE_STORE, DELETE_TASK, DELETE_DEP -> {
                            val a = activity
                            if (a is DeleteDialogInterface) a.onDeletedConfirmed()
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, { d, i -> d.cancel() })
        return builder.create()
    }
}