package fr.geobert.efficio.misc

import android.database.Cursor
import java.util.*

public inline fun Cursor?.forEach(f: (it: Cursor) -> Unit): Unit {
    if (this != null) {
        if (moveToFirst()) {
            do {
                f(this)
            } while (moveToNext())
        }
    }
}

public inline fun <T> Cursor?.map(transform: (it: Cursor) -> T): MutableList<T> {
    return mapTo(LinkedList<T>(), transform)
}

public inline fun <T, C : MutableCollection<T>> Cursor?.mapTo(result: C, transform: (it: Cursor) -> T): C {
    return if (this == null) result else {
        if (moveToFirst())
            do {
                result.add(transform(this))
            } while (moveToNext())
        result
    }
}

public inline fun <T> Cursor?.mapAndClose(create: (it: Cursor) -> T): Collection<T> {
    try {
        return map(create)
    } finally {
        this?.close()
    }
}

public fun <T> Cursor?.getByFilter(filter: (it: Cursor) -> Boolean, create: (it: Cursor) -> T): T? {
    var result: T? = null
    this.forEach {
        if (filter(it)) {
            result = create(it)
            return@forEach
        }
    }
    return result
}
