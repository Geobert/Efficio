package fr.geobert.efficio.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.os.Bundle
import android.os.Environment
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckedTextView
import android.widget.TextView
import fr.geobert.efficio.R
import kotlinx.android.synthetic.main.file_chooser_dialog.view.*
import java.io.File
import kotlin.properties.Delegates

interface FileChooserDialogListener {
    fun onFileChosen(name: String)
}

class FileViewHolder(val view: View, val adapter: FileAdapter) : RecyclerView.ViewHolder(view) {
    var file: File by Delegates.notNull()
    val text: TextView = view.findViewById(android.R.id.text1)

    init {
        view.isClickable = true
        view.setOnClickListener {
            val old = adapter.selectedIdx
            adapter.selectedIdx = adapterPosition
            adapter.notifyItemChanged(adapterPosition)
            if (old != null)
                adapter.notifyItemChanged(old)
        }
    }

    fun bind(f: File) {
        synchronized(this) {
            file = f
            text.text = f.name
        }
    }
}

class FileAdapter(val fileList: List<File>) : RecyclerView.Adapter<FileViewHolder>() {
    var selectedIdx: Int? = null

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        holder.bind(fileList[position])
        holder.view.isSelected = position == selectedIdx
        holder.view.isActivated = position == selectedIdx
        if (holder.view is CheckedTextView) holder.view.isChecked = position == selectedIdx
    }

    override fun getItemCount(): Int {
        return fileList.count()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val l = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_single_choice, parent, false)
        return FileViewHolder(l, this)
    }
}

class FileChooserDialog : DialogFragment() {
    private var customView: View by Delegates.notNull()
    private val fileListView: RecyclerView by lazy { customView.findViewById<RecyclerView>(R.id.file_list) }

    companion object {
        fun newInstance(path: String): FileChooserDialog {
            val d = FileChooserDialog()
            val b = Bundle()
            b.putString("path", path)
            d.arguments = b
            return d
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val b = createDialogBuilder(R.layout.file_chooser_dialog)
        b.setTitle(R.string.choose_file_title)
        return b.create()
    }

    private fun createDialogBuilder(layoutId: Int): AlertDialog.Builder {
        val builder = AlertDialog.Builder(activity)
        customView = activity.layoutInflater.inflate(layoutId, null)
        builder.setView(customView)
                .setNegativeButton(android.R.string.cancel, { d, _ ->
                    d.cancel()
                })
                .setPositiveButton(android.R.string.ok, { _, _ ->
                    onFileChosen()
                })
        return builder
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val path = Environment.getExternalStorageDirectory().toString() + "/Efficio/"
        val dir = File(path)
        if (dir.exists()) {
            val files = dir.listFiles()
            if (files.isEmpty()) showNoBackupMsg()
            else {
                customView.file_list.layoutManager = LinearLayoutManager(this.activity)
                customView.file_list.itemAnimator = DefaultItemAnimator()
                customView.file_list.setHasFixedSize(true)
                val fileAdapter = FileAdapter(files.toList())
                customView.file_list.adapter = fileAdapter
            }
        } else {
            showNoBackupMsg()
        }
    }

    private fun showNoBackupMsg() {
        MessageDialog.newInstance(R.string.no_backup, R.string.error).show(fragmentManager, "Err")
    }

    fun onFileChosen() {
        val adap = customView.file_list.adapter as FileAdapter
        val file = adap.fileList[adap.selectedIdx!!]
        val frag = targetFragment
        if (frag is FileChooserDialogListener) frag.onFileChosen(file.name)
    }


}