package fr.geobert.efficio.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.os.Bundle

class MessageDialog : DialogFragment() {
    companion object {
        fun newInstance(msg: Int, title: Int): MessageDialog {
            val d = MessageDialog()
            val b = Bundle()
            b.putInt("title", title)
            b.putInt("msg", msg)
            d.arguments = b
            return d
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog? {
        val b = AlertDialog.Builder(activity)
        b.setTitle(arguments.getInt("title")).setMessage(arguments.getInt("msg")).
                setPositiveButton(android.R.string.ok, { d, _ -> d.cancel() })
        return b.create()
    }
}