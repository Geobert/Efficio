package fr.geobert.efficio.dialog

import android.app.*
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.View
import fr.geobert.efficio.*
import fr.geobert.efficio.data.Store
import fr.geobert.efficio.db.StoreTable
import fr.geobert.efficio.misc.*
import kotlinx.android.synthetic.main.input_dialog.view.*
import kotlin.properties.Delegates

class StoreNameDialog : DialogFragment() {
    companion object {
        fun newInstance(msg: String, title: String, default: String, storeId: Long,
                        action: Int): StoreNameDialog {
            val d = StoreNameDialog()
            val b = Bundle()
            b.putString("title", title)
            b.putString("msg", msg)
            b.putLong("storeId", storeId)
            b.putString("default", default)
            b.putInt("action", action)
            d.arguments = b
            return d
        }
    }

    private var customView: View by Delegates.notNull()
    private var storeId: Long by Delegates.notNull()
    private var action: Int by Delegates.notNull()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val b = AlertDialog.Builder(activity)
        customView = activity.layoutInflater.inflate(R.layout.input_dialog, null)
        storeId = arguments.getLong("storeId")
        customView.edit_text.text = SpannableStringBuilder(arguments.getString("default"))
        customView.edit_text.selectAll()
        customView.dialog_msg.text = SpannableStringBuilder(arguments.getString("msg"))
        action = arguments.getInt("action")
        b.setView(customView).setTitle(arguments.getString("title")).
                setPositiveButton(android.R.string.ok, { dialogInterface, i -> onOkClicked() }).
                setNegativeButton(android.R.string.cancel,
                        { dialogInterface, i -> dialogInterface.cancel() })
        return b.create()
    }

    private fun onOkClicked() {
        val t = customView.edit_text.text.toString()
        val a = activity
        when (action) {
            RENAME_STORE ->
                if (StoreTable.renameStore(activity, storeId, t) > 0) {
                    if (a is MainActivity) {
                        a.storeRenamed(t)
                    }
                    dialog.cancel()
                } else {
                    MessageDialog.newInstance(R.string.error_rename, R.string.error).
                            show(activity.fragmentManager, "ErrDialog")

                }
            CREATE_STORE -> {
                val s = Store(t)
                if (StoreTable.create(activity, s) > 0) {
                    if (a is MainActivity) {
                        a.storeCreated(s)
                    }
                } else {
                    MessageDialog.newInstance(R.string.error_create_store, R.string.error).
                            show(activity.fragmentManager, "ErrDialog")
                }
            }
        }
    }
}