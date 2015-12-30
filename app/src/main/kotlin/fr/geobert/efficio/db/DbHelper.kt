package fr.geobert.efficio.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import fr.geobert.efficio.R
import fr.geobert.efficio.misc.convertNonAscii
import kotlin.properties.Delegates

class DbHelper : SQLiteOpenHelper {
    private constructor(ctx: Context) : super(ctx, "items.db", null, DbHelper.DB_VERSION) {
    }

    private var ctx: Context by Delegates.notNull()

    companion object {
        val DB_VERSION = 1
        private var instance: DbHelper? = null

        fun getInstance(ctx: Context): DbHelper {
            synchronized(this) {
                if (instance == null) {
                    instance = DbHelper(ctx.applicationContext)
                }
                instance!!.ctx = ctx.applicationContext
                return instance!!
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(ItemTable.CREATE_TABLE)
        db.execSQL(DepartmentTable.CREATE_TABLE)
        db.execSQL(StoreTable.CREATE_TABLE)
        db.execSQL(StoreCompositionTable.CREATE_TABLE)
        db.execSQL(ItemWeightTable.CREATE_TABLE)
        db.execSQL(ItemDepTable.CREATE_TABLE)
        db.execSQL(TaskTable.CREATE_TABLE)

        // triggers
        db.execSQL(ItemTable.CREATE_TRIGGER_ON_ITEM_DEL)
        db.execSQL(StoreTable.CREATE_TRIGGER_ON_STORE_DEL)
        db.execSQL(DepartmentTable.CREATE_TRIGGER_ON_DEP_DELETE)

        val name = ctx.getString(R.string.store)
        db.execSQL("INSERT INTO ${StoreTable.TABLE_NAME} VALUES (1, '${name.convertNonAscii().toLowerCase()}', '$name')")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Here you can upgrade tables, as usual
    }
}
