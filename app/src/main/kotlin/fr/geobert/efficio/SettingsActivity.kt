package fr.geobert.efficio

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceFragment
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.view.View
import fr.geobert.efficio.db.DbHelper
import fr.geobert.efficio.dialog.FileChooserDialog
import fr.geobert.efficio.dialog.FileChooserDialogListener
import fr.geobert.efficio.dialog.MessageDialog
import fr.geobert.efficio.misc.MY_PERM_REQUEST_WRITE_EXT_STORAGE
import fr.geobert.efficio.misc.OnRefreshReceiver
import kotlinx.android.synthetic.main.item_editor.*
import kotlinx.android.synthetic.main.settings_activity.*

class SettingsActivity : BaseActivity(), FileChooserDialogListener {

    companion object {
        fun callMe(ctx: Context) {
            val i = Intent(ctx, SettingsActivity::class.java)
            ctx.startActivity(i)
        }
    }

    class SettingsFragment : PreferenceFragment(), FileChooserDialogListener {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            addPreferencesFromResource(R.xml.settings)
        }

        override fun onFileChosen(name: String) {
            (activity as FileChooserDialogListener).onFileChosen(name)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        setSpinnerVisibility(View.GONE)
        setTitle(R.string.settings)
        setIcon(R.drawable.ic_action_arrow_left)
        setIconOnClick(View.OnClickListener { onBackPressed() })

        if (savedInstanceState == null) {
            fragmentManager.beginTransaction().replace(R.id.flContent, SettingsFragment(),
                    "settings").commit()
        }

        backup_db_btn.setOnClickListener {
            askPermAndBackupDatabase()
        }

        restore_db_btn.setOnClickListener {
            chooseFileToRestore()
        }

    }

    private fun chooseFileToRestore() {
        val f = FileChooserDialog.newInstance("/Efficio")
        f.setTargetFragment(fragmentManager.findFragmentByTag("settings"), 0)
        f.show(fragmentManager, "FileChooser")
    }

    override fun onFileChosen(name: String) {
        val msg = if (DbHelper.restoreDatabase(this, name)) {
            val i = Intent(OnRefreshReceiver.REFRESH_ACTION)
            sendBroadcast(i)
            R.string.restore_database_done
        } else R.string.restore_database_fail
        Snackbar.make(my_toolbar, msg, Snackbar.LENGTH_LONG).show()
    }

    private fun askPermAndBackupDatabase() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    MY_PERM_REQUEST_WRITE_EXT_STORAGE)
        } else {
            doBackup()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            MY_PERM_REQUEST_WRITE_EXT_STORAGE -> {
                if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                    doBackup()
                } else {
                    MessageDialog.newInstance(R.string.permission_write_required, R.string.permission_required_title)
                            .show(fragmentManager, "PermAsk")
                }
            }
        }
    }

    private fun doBackup() {
        val name = DbHelper.backupDb()
        val msg = if (name != null)
            getString(R.string.backup_database_done).format(name)
        else getString(R.string.backup_database_fail)

        Snackbar.make(my_toolbar, msg,
                Snackbar.LENGTH_LONG).show()
    }
}

