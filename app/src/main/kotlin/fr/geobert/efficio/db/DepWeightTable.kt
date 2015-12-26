package fr.geobert.efficio.db

import android.provider.BaseColumns


object DepWeightTable : BaseTable() {
    override val TABLE_NAME: String = "department_weight"

    val COL_STORE_ID = "store_id"
    val COL_DEP_ID = "dep_id"
    val COL_WEIGHT = "dep_weight"

    override fun CREATE_COLUMNS(): String = "$COL_STORE_ID INTEGER NOT NULL, " +
            "$COL_DEP_ID INTEGER NOT NULL, " +
            "$COL_WEIGHT INTEGER NOT NULL, " +
            "${foreignId(COL_STORE_ID, StoreTable.TABLE_NAME)}, " +
            "${foreignId(COL_DEP_ID, DepartmentTable.TABLE_NAME)}"


    val TABLE_TO_QUERY = "${TABLE_NAME} sc" +
            " LEFT OUTER JOIN ${StoreTable.TABLE_NAME} s ON sc.$COL_STORE_ID = s.${BaseColumns._ID}" +
            " LEFT OUTER JOIN ${DepartmentTable.TABLE_NAME} d ON sc.$COL_DEP_ID = d.${BaseColumns._ID}"

    override val COLS_TO_QUERY: Array<String> = arrayOf("s.${BaseColumns._ID}",
            "s.${StoreTable.COL_NAME}", "d.${BaseColumns._ID}", "d.${DepartmentTable.COL_NAME}",
            "sc.$COL_WEIGHT")

    fun create() {

    }

    fun update() {

    }

    fun delete() {

    }
}