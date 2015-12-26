package fr.geobert.efficio.db

object ItemDepTable : BaseTable() {
    override val TABLE_NAME: String = "items_dep"

    val COL_ITEM_ID = "item_id"
    val COL_DEP_ID = "dep_id"
    val COL_STORE_ID = "store_id"

    override fun CREATE_COLUMNS(): String = "$COL_STORE_ID INTEGER NOT NULL, " +
            "$COL_DEP_ID INTEGER NOT NULL, " +
            "$COL_ITEM_ID INTEGER NOT NULL, " +
            "${foreignId(COL_STORE_ID, StoreTable.TABLE_NAME)}," +
            "${foreignId(COL_ITEM_ID, ItemTable.TABLE_NAME)}," +
            "${foreignId(COL_DEP_ID, DepartmentTable.TABLE_NAME)}"

    override val COLS_TO_QUERY: Array<String>
        get() = throw UnsupportedOperationException()

}