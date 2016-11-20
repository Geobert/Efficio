package fr.geobert.efficio.extensions

import kotlin.comparisons.compareValues

fun compareLists(list1: List<Comparable<*>>, list2: List<Comparable<*>>): Int {
    for (i in 0..Math.min(list1.size, list2.size) - 1) {
        val elem1 = list1[i]
        val elem2 = list2[i]

        if (elem1.javaClass != elem2.javaClass) {
            throw IllegalArgumentException("Lists of different type provided")
        }

        compareValues(elem1, elem2).let {
            if (it != 0) return it
        }
    }
    return compareValues(list1.size, list2.size)
}