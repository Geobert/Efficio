package fr.geobert.efficio.misc

inline fun consume(f: () -> Unit): Boolean {
    f()
    return true
}