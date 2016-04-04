package fr.geobert.efficio.dialog

import android.R
import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.os.Bundle
import fr.geobert.efficio.misc.DELETE_STORE
import fr.geobert.efficio.misc.DELETE_TASK
import fr.geobert.efficio.misc.DeleteDialogInterface

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
                        DELETE_STORE, DELETE_TASK -> {
                            val a = activity
                            if (a is DeleteDialogInterface) a.onDeletedConfirmed()
                        }
                    //                        DELETE_TASK ->
                    //                            targetFragment.onActivityResult(targetRequestCode, Activity.RESULT_OK,
                    //                                    activity.intent)
                    }


                })
                .setNegativeButton(R.string.cancel, { d, i -> d.cancel() })
        return builder.create()
    }
}