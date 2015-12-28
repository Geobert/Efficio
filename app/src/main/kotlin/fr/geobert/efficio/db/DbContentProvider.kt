package fr.geobert.efficio.db

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import android.provider.BaseColumns
import android.util.Log
import java.util.*


class DbContentProvider : ContentProvider() {
    companion object {
        val CONTENT_AUTHORITY = "fr.geobert.efficio"
        val BASE_CONTENT_URI = Uri.parse("content://$CONTENT_AUTHORITY")

        val ITEM = 100
        val ITEM_WITH_ID = 101
        val DEP = 200
        val DEP_WITH_ID = 201
        val STORE = 300
        val STORE_WITH_ID = 301
        val ITEM_WEIGHT = 400
        val ITEM_WEIGHT_WITH_ID = 401
        val DEP_WEIGHT = 500
        val DEP_WEIGHT_WITH_ID = 501
        val ITEM_DEP = 600
        val ITEM_DEP_WITH_ID = 601
        val TASK = 700
        val TASK_WITH_ID = 701

        fun createUriMatcher(): UriMatcher {
            var matcher: UriMatcher = UriMatcher(UriMatcher.NO_MATCH)
            val authority = CONTENT_AUTHORITY

            fun addUri(path: String, noId: Int, withId: Int) {
                matcher.addURI(authority, path, noId)
                matcher.addURI(authority, "$path/#", withId)
            }

            addUri(ItemTable.PATH, ITEM, ITEM_WITH_ID)
            addUri(DepartmentTable.PATH, DEP, DEP_WITH_ID)
            addUri(StoreTable.PATH, STORE, STORE_WITH_ID)
            addUri(ItemWeightTable.PATH, ITEM_WEIGHT, ITEM_WEIGHT_WITH_ID)
            addUri(StoreCompositionTable.PATH, DEP_WEIGHT, DEP_WEIGHT_WITH_ID)
            addUri(ItemDepTable.PATH, ITEM_DEP, ITEM_DEP_WITH_ID)
            addUri(TaskTable.PATH, TASK, TASK_WITH_ID)

            return matcher
        }

        private val sURIMatcher = createUriMatcher()
        private var mDbHelper: DbHelper? = null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        val uriType = sURIMatcher.match(uri)
        val id = mDbHelper!!.writableDatabase!!.insert(switchToTableWrite(uriType, uri), null, values)
        var insertionUri: Uri? = null
        if (id > 0) {
            insertionUri = when (uriType) {
                ITEM -> ItemTable.buildWithId(id)
                DEP -> DepartmentTable.buildWithId(id)
                STORE -> StoreTable.buildWithId(id)
                ITEM_WEIGHT -> ItemWeightTable.buildWithId(id)
                DEP_WEIGHT -> StoreCompositionTable.buildWithId(id)
                ITEM_DEP -> ItemDepTable.buildWithId(id)
                else -> throw IllegalArgumentException("Unknown URI: " + uri)
            }
            context.contentResolver.notifyChange(uri, null)
        }
        return insertionUri
    }

    override fun query(uri: Uri, projection: Array<out String>, selection: String?,
                       selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
        val queryBuilder = SQLiteQueryBuilder()
        val uriType = sURIMatcher.match(uri)
        when (uriType) {
            ITEM_WITH_ID -> queryBuilder.appendWhere("items.${BaseColumns._ID}=${uri.lastPathSegment}")
            DEP_WITH_ID -> queryBuilder.appendWhere("${BaseColumns._ID}=${uri.lastPathSegment}")
            else -> {
            }
        }
        queryBuilder.tables = switchToTableRead(uriType, uri)
        val c = queryBuilder.query(mDbHelper!!.readableDatabase, projection, selection,
                selectionArgs, null, null, sortOrder)
        c.setNotificationUri(context.contentResolver, uri)
        return c
    }

    private fun switchToTableRead(uriType: Int, uri: Uri): String {
        return when (uriType) {
            ITEM, ITEM_WITH_ID -> ItemTable.TABLE_NAME
            DEP, DEP_WITH_ID -> DepartmentTable.TABLE_NAME
            STORE, STORE_WITH_ID -> StoreTable.TABLE_NAME
            ITEM_WEIGHT, ITEM_WEIGHT_WITH_ID -> ItemWeightTable.TABLE_NAME
            DEP_WEIGHT, DEP_WEIGHT_WITH_ID -> StoreCompositionTable.TABLE_JOINED
            ITEM_DEP, ITEM_DEP_WITH_ID -> ItemDepTable.TABLE_NAME
            TASK, TASK_WITH_ID -> TaskTable.TABLE_JOINED
            else -> throw IllegalArgumentException("Unknown URI: " + uri)
        }
    }

    private fun switchToTableWrite(uriType: Int, uri: Uri): String {
        return when (uriType) {
            ITEM, ITEM_WITH_ID -> ItemTable.TABLE_NAME
            DEP, DEP_WITH_ID -> DepartmentTable.TABLE_NAME
            STORE, STORE_WITH_ID -> StoreTable.TABLE_NAME
            ITEM_WEIGHT, ITEM_WEIGHT_WITH_ID -> ItemWeightTable.TABLE_NAME
            DEP_WEIGHT, DEP_WEIGHT_WITH_ID -> StoreCompositionTable.TABLE_NAME
            ITEM_DEP, ITEM_DEP_WITH_ID -> ItemDepTable.TABLE_NAME
            TASK, TASK_WITH_ID -> TaskTable.TABLE_NAME
            else -> throw IllegalArgumentException("Unknown URI: " + uri)
        }
    }

    override fun onCreate(): Boolean {
        Log.d("DbContentProvider", "onCreate")
        mDbHelper = DbHelper.getInstance(context)
        return true
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        val db = mDbHelper!!.writableDatabase
        var id: String? = uri.lastPathSegment
        val table = switchToTableWrite(sURIMatcher.match(uri), uri)
        // return nb updated items
        return if (id != null) {
            if (selection == null || selection.trim().length == 0) {
                db.update(table, values, "${BaseColumns._ID}=?", arrayOf(id))
            } else {
                val selArgs = if (selectionArgs != null) {
                    val args = ArrayList<String>(selectionArgs.size + 1)
                    args.add(id)
                    Collections.addAll(args, *selectionArgs)
                    args.toArray<String>(arrayOfNulls<String>(args.size))
                } else {
                    arrayOf(id)
                }
                db.update(table, values, "${BaseColumns._ID}=? and " + selection, selArgs)
            }
        } else {
            db.update(table, values, selection, selectionArgs)
        }
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        val uriType = sURIMatcher.match(uri)
        val db = mDbHelper!!.writableDatabase

        var customSelection = selection ?: "1"

        var deleted = db!!.delete(switchToTableWrite(uriType, uri), customSelection, selectionArgs)

        if (deleted > 0) {
            context.contentResolver.notifyChange(uri, null)
        }

        return deleted
    }

    override fun getType(p0: Uri?): String? {
        return null
    }
}