package fr.geobert.efficio.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.util.Log

object WidgetTable : BaseTable() {
    override val TABLE_NAME: String = "widgets"
    val TAG = "WidgetTable"
    val COL_WIDGET_ID = "widget_id"
    val COL_STORE_ID = "store_id"
    val COL_OPACITY = "opacity"

    val TABLE_JOINED = "$TABLE_NAME " +
            "${leftOuterJoin(TABLE_NAME, COL_STORE_ID, StoreTable.TABLE_NAME)} "

    val RESTRICT_TO_WIDGET = "(${WidgetTable.TABLE_NAME}.${WidgetTable.COL_WIDGET_ID} = ?)"

    override fun CREATE_COLUMNS(): String = "$COL_WIDGET_ID INTEGER NOT NULL, " +
            "$COL_STORE_ID INTEGER NOT NULL, " +
            "$COL_OPACITY FLOAT NOT NULL"

    override val COLS_TO_QUERY: Array<String> = arrayOf(COL_STORE_ID, COL_OPACITY, "${StoreTable.TABLE_NAME}.${StoreTable.COL_NAME}")

    fun create(ctx: Context, widgetId: Int, storeId: Long, opacity: Float): Long {
        Log.d(TAG, "create: $widgetId")
        val v = ContentValues()
        v.put(COL_WIDGET_ID, widgetId)
        v.put(COL_STORE_ID, storeId)
        v.put(COL_OPACITY, opacity)
        return insert(ctx, v)
    }

    fun delete(ctx: Context, widgetId: Int): Int {
        Log.d(TAG, "delete: $widgetId", Throwable())
        return super.delete(ctx, widgetId.toLong())
    }

    fun getWidgetInfo(ctx: Context, widgetId: Int): Cursor? {
        return ctx.contentResolver.query(WidgetTable.CONTENT_URI, WidgetTable.COLS_TO_QUERY,
                RESTRICT_TO_WIDGET, arrayOf(widgetId.toString()), null)
    }


}