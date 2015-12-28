package fr.geobert.efficio.data


class Store(var name: String) {
    var id: Long = 0
    val tasks: Array<Task> = emptyArray()
}