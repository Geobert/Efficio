package fr.geobert.efficio.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Environment
import android.util.Log
import fr.geobert.efficio.R
import fr.geobert.efficio.extensions.TIME_ZONE
import fr.geobert.efficio.extensions.normalize
import hirondelle.date4j.DateTime
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.properties.Delegates

class DbHelper private constructor(ctx: Context) :
        SQLiteOpenHelper(ctx, DATABASE_NAME, null, DbHelper.DB_VERSION) {

    private var ctx: Context by Delegates.notNull()

    companion object {
        val TAG = "DbHelper"
        val DATABASE_NAME = "items.db"
        val DB_VERSION = 3
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

        fun delete() {
            instance = null
        }

        fun backupDb(): String? {
            try {
                val sd = Environment.getExternalStorageDirectory()
                val data = Environment.getDataDirectory()
                if (sd.canWrite()) {
                    val now = DateTime.now(TIME_ZONE)
                    val dbName = "efficio_${now.format("YYMMDD")}_${now.format("hhmmss")}.db"
                    val currentDBPath = "/data/fr.geobert.efficio/databases/$DATABASE_NAME"
                    val backupDBDir = "/Efficio/"
                    val backupDBPath = "$backupDBDir/$dbName"
                    val currentDB = File(data, currentDBPath)
                    val backupDir = File(sd, backupDBDir)
                    if (!backupDir.exists() && !backupDir.mkdirs()) {
                        Log.e(TAG, "failed to create dir")
                        return null
                    }
                    val backupDB = File(sd, backupDBPath)
                    if (currentDB.exists()) {
                        val srcFIS = FileInputStream(currentDB)
                        val dstFOS = FileOutputStream(backupDB)
                        val src = srcFIS.channel
                        val dst = dstFOS.channel
                        dst.transferFrom(src, 0, src.size())
                        src.close()
                        dst.close()
                        srcFIS.close()
                        dstFOS.close()
                        return dbName
                    } else {
                        Log.e(TAG, "DB PATH: ${currentDB.absolutePath} does not exists")
                    }
                } else {
                    Log.e(TAG, "External storage can't be write")
                }

            } catch (e: Exception) {
                Log.e(TAG, "exception while backupDb", e)
            }
            return null
        }

        fun restoreDatabase(ctx: Context, name: String): Boolean {
            try {
                val sd = Environment.getExternalStorageDirectory()

                val backupDBPath = "/Efficio/$name"
                val currentDB = ctx.getDatabasePath(DATABASE_NAME)
                val backupDB = File(sd, backupDBPath)

                if (backupDB.exists()) {
                    DbContentProvider.close()
                    val srcFIS = FileInputStream(backupDB)
                    val dstFOS = FileOutputStream(currentDB)
                    val dst = dstFOS.channel
                    val src = srcFIS.channel

                    dst.transferFrom(src, 0, src.size())
                    src.close()
                    dst.close()
                    srcFIS.close()
                    dstFOS.close()
                    DbContentProvider.reinit(ctx)
                    //DBPrefsManager.getInstance(ctx).put(RadisService.CONSOLIDATE_DB, true)
                    return true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return false
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
        db.execSQL(WidgetTable.CREATE_TABLE)

        // triggers
        db.execSQL(ItemTable.CREATE_TRIGGER_ON_ITEM_DEL)
        db.execSQL(StoreTable.CREATE_TRIGGER_ON_STORE_DEL)
        db.execSQL(DepartmentTable.CREATE_TRIGGER_ON_DEP_DELETE)
        db.execSQL(TaskTable.CREATE_TRIGGER_ON_TASK_DEL)

        val name = ctx.getString(R.string.store)
        db.execSQL("INSERT INTO ${StoreTable.TABLE_NAME} VALUES (1, '${name.normalize()}', '$name')")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion <= 1) upgradeFromV1(db)
        if (oldVersion <= 2) upgradeFromV2(db)
    }

    private fun upgradeFromV2(db: SQLiteDatabase) {
        TaskTable.upgradeFromV2(db)
    }

    private fun upgradeFromV1(db: SQLiteDatabase) {
        TaskTable.upgradeFromV1(db)
        DepartmentTable.upgradeFromV1(db)
    }
}
