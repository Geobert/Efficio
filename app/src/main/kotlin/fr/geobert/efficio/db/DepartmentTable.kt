package fr.geobert.efficio.db

import android.provider.BaseColumns

object DepartmentTable : BaseTable() {
    override val TABLE_NAME = "departments"

    val COL_NAME = "dep_name"

    override fun CREATE_COLUMNS() = "$COL_NAME TEXT NOT NULL"

    override val COLS_TO_QUERY: Array<String> = arrayOf(BaseColumns._ID, COL_NAME)

    val CREATE_TRIGGER_ON_DEP_DELETE by lazy {
        "CREATE TRIGGER on_dep_deleted " +
                "AFTER DELETE ON ${DepartmentTable.TABLE_NAME} BEGIN " +
                "DELETE FROM ${DepWeightTable.TABLE_NAME} WHERE " +
                "${DepWeightTable.COL_DEP_ID} = old.${BaseColumns._ID};" +
                "DELETE FROM ${ItemDepTable.TABLE_NAME} WHERE " +
                "${ItemDepTable.COL_DEP_ID} = old.${BaseColumns._ID};" +
                "END"
    }

    fun create() {

    }

    fun update() {

    }

    fun delete() {

    }
}