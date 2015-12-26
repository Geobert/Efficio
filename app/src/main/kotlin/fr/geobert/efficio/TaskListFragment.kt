package fr.geobert.efficio

import android.app.Fragment
import android.app.LoaderManager
import android.content.CursorLoader
import android.content.Loader
import android.database.Cursor
import android.os.Bundle
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import fr.geobert.efficio.adapter.TaskAdapter
import fr.geobert.efficio.data.Store
import fr.geobert.efficio.db.TaskTable
import kotlinx.android.synthetic.main.item_list_fragment.*
import kotlin.properties.Delegates

class TaskListFragment : Fragment(), LoaderManager.LoaderCallbacks<Cursor> {
    private val GET_TASKS_OF_STORE = 100

    var lastStoreId: Long = 1 // todo get it from prefs
    var currentStore: Store? = null
    var cursorLoader: CursorLoader? = null
    var taskAdapter: TaskAdapter by Delegates.notNull()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.item_list_fragment, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        tasks_list.layoutManager = LinearLayoutManager(this.activity)
        tasks_list.itemAnimator = DefaultItemAnimator()
        tasks_list.setHasFixedSize(true)

        fetchStore(this, lastStoreId)
    }

    fun fetchStore(ctx: Fragment, storeId: Long) {
        val b = Bundle()
        b.putLong("storeId", storeId)
        if (cursorLoader == null) {
            ctx.loaderManager.initLoader(GET_TASKS_OF_STORE, b, this)
        } else {
            ctx.loaderManager.restartLoader(GET_TASKS_OF_STORE, b, this)
        }
    }

    override fun onCreateLoader(i: Int, bundle: Bundle?): Loader<Cursor>? {
        return when (i) {
            GET_TASKS_OF_STORE -> TaskTable.fetchAllTasksForStore(this.activity,
                    bundle?.getLong("storeId") ?: 0)
            else -> null
        }
    }

    override fun onLoadFinished(cursorLoader: Loader<Cursor>, cursor: Cursor) {
        when (cursorLoader.id) {
            GET_TASKS_OF_STORE -> {
                taskAdapter = TaskAdapter(cursor)
                tasks_list.adapter = taskAdapter
                if (taskAdapter.itemCount == 0) {
                    tasks_list.visibility = View.GONE
                    empty_text.visibility = View.VISIBLE
                } else {
                    tasks_list.visibility = View.VISIBLE
                    empty_text.visibility = View.GONE
                }
            }
            else -> throw IllegalArgumentException("Unknown cursorLoader id")
        }
    }

    override fun onLoaderReset(cursorLoader: Loader<Cursor>?) {
        currentStore = null
        cursorLoader?.reset()
    }
}