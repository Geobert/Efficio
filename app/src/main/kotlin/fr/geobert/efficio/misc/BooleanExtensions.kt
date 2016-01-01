package fr.geobert.efficio.misc

import android.os.Parcel

public fun Parcel.writeBoolean(b: Boolean) {
    this.writeByte(if (b) 1 else 0)
}

public fun Parcel.readBoolean(): Boolean = this.readByte() != 0.toByte()