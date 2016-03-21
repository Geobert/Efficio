package fr.geobert.efficio.misc

import android.os.Parcel

fun Parcel.writeBoolean(b: Boolean) {
    this.writeByte(if (b) 1 else 0)
}

fun Parcel.readBoolean(): Boolean = this.readByte() != 0.toByte()