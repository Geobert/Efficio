package fr.geobert.efficio.db


object ItemWeightTable : BaseTable() {
    override val TABLE_NAME: String = "items_weight"

    val COL_STORE_ID = "store_id"
    val COL_ITEM_ID = "item_id"
    val COL_WEIGHT = "item_weight"

    override fun CREATE_COLUMNS(): String = "$COL_STORE_ID INTEGER NOT NULL, " +
            "$COL_ITEM_ID INTEGER NOT NULL, " +
            "$COL_WEIGHT INTEGER NOT NULL, " +
            "${foreignId(COL_STORE_ID, StoreTable.TABLE_NAME)}," +
            "${foreignId(COL_ITEM_ID, ItemTable.TABLE_NAME)}"

    override val COLS_TO_QUERY: Array<String>
        get() = throw UnsupportedOperationException()
}