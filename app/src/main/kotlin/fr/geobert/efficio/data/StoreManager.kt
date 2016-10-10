package fr.geobert.efficio.data

import android.app.LoaderManager
import android.content.Loader
import android.database.Cursor
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v4.content.CursorLoader
import fr.geobert.efficio.adapter.StoreAdapter
import fr.geobert.efficio.db.StoreTable
import fr.geobert.efficio.misc.GET_ALL_STORES
import fr.geobert.efficio.misc.map
import java.util.*
import kotlin.properties.Delegates

class StoreManager(val activity: FragmentActivity, val callback: StoreLoaderListener) : LoaderManager.LoaderCallbacks<Cursor> {
    private var storeLoader: CursorLoader? = null
    var storesList: MutableList<Store> = LinkedList()
        private set
    var storeAdapter: StoreAdapter by Delegates.notNull()
        private set

    fun fetchAllStores() {
        if (storeLoader == null) {
            activity.loaderManager.initLoader(GET_ALL_STORES, Bundle(), this)
        } else {
            activity.loaderManager.restartLoader(GET_ALL_STORES, Bundle(), this)
        }
    }

    // cursor loading
    override fun onLoadFinished(loader: Loader<Cursor>, cursor: Cursor) {
        when (loader.id) {
            GET_ALL_STORES -> {
                if (cursor.count > 0) {
                    storesList = cursor.map(::Store)
                } else {
                    storesList.clear()
                }
                storeAdapter = StoreAdapter(activity, storesList)
                callback.onStoreLoaded()
            }
        }
    }

    override fun onLoaderReset(p0: Loader<Cursor>?) {

    }

    override fun onCreateLoader(p0: Int, p1: Bundle?): Loader<Cursor>? {
        return StoreTable.getAllStoresLoader(activity)
    }
}