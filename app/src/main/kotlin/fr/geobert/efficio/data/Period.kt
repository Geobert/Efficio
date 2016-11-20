package fr.geobert.efficio.data

enum class Period {
    NONE, DAILY, WEEKLY, MONTHLY, YEARLY, CUSTOM;

    companion object {
        private val map = Period.values().associateBy(Period::ordinal)
        fun fromInt(ord: Int) = map[ord] as Period
    }
}

enum class PeriodUnit {
    NONE, DAY, WEEK, MONTH, YEAR;

    companion object {
        private val map = PeriodUnit.values().associateBy(PeriodUnit::ordinal)
        fun fromInt(ord: Int) = map[ord] as PeriodUnit
    }
}