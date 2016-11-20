package fr.geobert.efficio.extensions

import hirondelle.date4j.DateTime
import java.util.*

val TIME_ZONE: TimeZone = TimeZone.getDefault()

fun DateTime.plusMonth(nb: Int): DateTime = this.plus(0, nb, 0, 0, 0, 0, 0, DateTime.DayOverflow.LastDay)
fun DateTime.minusMonth(nb: Int): DateTime = this.minus(0, nb, 0, 0, 0, 0, 0, DateTime.DayOverflow.LastDay)
fun DateTime.plusYear(nb: Int): DateTime = this.plus(nb, 0, 0, 0, 0, 0, 0, DateTime.DayOverflow.LastDay)
fun DateTime.minusYear(nb: Int): DateTime = this.minus(nb, 0, 0, 0, 0, 0, 0, DateTime.DayOverflow.LastDay)
