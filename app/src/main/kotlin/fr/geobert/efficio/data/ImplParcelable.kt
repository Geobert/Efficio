package fr.geobert.efficio.data

import android.os.Parcel
import android.os.Parcelable
import fr.geobert.efficio.adapter.TaskAdapter
import fr.geobert.efficio.misc.readBoolean
import fr.geobert.efficio.misc.writeBoolean
import java.util.*

public interface ImplParcelable : Parcelable {
    val parcels: HashMap<String, Any?>

    open fun getClassLoaderOf(name: String): ClassLoader? {
        return null
    }

    override fun writeToParcel(p: Parcel, flags: Int) {
        parcels.forEach {
            when (it.value) {
                is Boolean -> p.writeBoolean(it.value as Boolean)
                is Long -> p.writeLong(it.value as Long)
                is Int -> p.writeInt(it.value as Int)
                is String -> p.writeString(it.value as String)
                is Parcelable -> p.writeParcelable(it.value as Parcelable, 0)
                is TaskAdapter.VIEW_TYPES -> p.writeInt((it.value as TaskAdapter.VIEW_TYPES).ordinal)
                else -> throw RuntimeException()
            }
        }
    }

    open fun readFromParcel(p: Parcel) {
        parcels.forEach {
            when (it.value) {
                is Boolean -> parcels[it.key] = p.readBoolean()
                is Long -> parcels[it.key] = p.readLong()
                is Int -> parcels[it.key] = p.readInt()
                is String -> parcels[it.key] = p.readString()
                is Parcelable -> parcels[it.key] = p.readParcelable(getClassLoaderOf(it.key))
                is TaskAdapter.VIEW_TYPES -> TaskAdapter.VIEW_TYPES.values()[p.readInt()]
            }
        }
    }

    override fun describeContents(): Int = 0

}
