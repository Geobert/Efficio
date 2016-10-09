package fr.geobert.efficio.db

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.BaseColumns

abstract class BaseTable : BaseColumns {
    abstract val TABLE_NAME: String
    val PATH = TABLE_NAME
    val CONTENT_URI = DbContentProvider.BASE_CONTENT_URI.buildUpon().appendPath(PATH).build()
    val CONTENT_TYPE = "${ContentResolver.CURSOR_DIR_BASE_TYPE}/${DbContentProvider.CONTENT_AUTHORITY}/$PATH"
    val CONTENT_ITEM_TYPE = "${ContentResolver.CURSOR_ITEM_BASE_TYPE}/${DbContentProvider.CONTENT_AUTHORITY}/$PATH"

    val CREATE_TABLE: String = "CREATE TABLE $TABLE_NAME (" +
            "${BaseColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT, ${CREATE_COLUMNS()});"

    protected abstract fun CREATE_COLUMNS(): String
    abstract val COLS_TO_QUERY: Array<String>

    protected fun foreignId(key: String, refTable: String) =
            "FOREIGN KEY ($key) REFERENCES $refTable(${BaseColumns._ID})"

    protected fun leftOuterJoin(alias: String, key: String, foreignTable: String,
                                foreignKey: String = BaseColumns._ID) =
            "LEFT JOIN $foreignTable ON $alias.$key = $foreignTable.$foreignKey"

    fun buildWithId(id: Long): Uri {
        return ContentUris.withAppendedId(CONTENT_URI, id)
    }

    fun getIdFromUri(uri: Uri): Long {
        return ContentUris.parseId(uri)
    }

    protected fun insert(ctx: Context, values: ContentValues): Long {
        return ctx.contentResolver.insert(CONTENT_URI, values)?.lastPathSegment?.toLong() ?: 0
    }

    protected fun update(ctx: Context, id: Long, values: ContentValues): Int {
        return ctx.contentResolver.update(buildWithId(id), values, null, null)
    }

    protected fun delete(ctx: Context, id: Long): Int {
        return ctx.contentResolver.delete(buildWithId(id), null, null)
    }
}