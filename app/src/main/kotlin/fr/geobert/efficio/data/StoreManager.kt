package fr.geobert.efficio.data

import android.app.LoaderManager
import android.content.Loader
import android.database.Cursor
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import fr.geobert.efficio.adapter.StoreAdapter
import fr.geobert.efficio.db.StoreTable
import fr.geobert.efficio.extensions.map
import fr.geobert.efficio.misc.*
import java.util.*
import kotlin.properties.Delegates

class StoreManager(val activity: FragmentActivity, val callback: StoreLoaderListener) :
        LoaderManager.LoaderCallbacks<Cursor> {
    private var storeLoader: Loader<Cursor>? = null
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
        storeLoader = StoreTable.getAllStoresLoader(activity)
        return storeLoader
    }

    fun indexOf(storeId: Long): Int {
        var i = -1
        for (s in storesList) {
            i++
            if (s.id == storeId) break
        }
        return if (i == storesList.size) -1 else i
    }

    fun renameStore(name: String, storeId: Long) {
        storeAdapter.renameStore(name, storeId)
        val store = storesList.find { it.id == storeId }
        store?.name = name
    }
}