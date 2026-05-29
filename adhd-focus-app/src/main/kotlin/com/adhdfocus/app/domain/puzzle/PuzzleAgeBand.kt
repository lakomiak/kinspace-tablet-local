package com.adhdfocus.app.domain.puzzle

enum class PuzzleAgeBand(
    val key: String,
    val displayName: String,
    val ageRangeLabel: String,
    val themeKeyword: String
) {
    AGE_3_4("3-4", "Ages 3-4", "3-4 years", "animals"),
    AGE_5_6("5-6", "Ages 5-6", "5-6 years", "rainbow"),
    AGE_7_8("7-8", "Ages 7-8", "7-8 years", "space"),
    AGE_9_10("9-10", "Ages 9-10", "9-10 years", "nature"),
    AGE_11_12("11-12", "Ages 11-12", "11-12 years", "ocean"),
    AGE_13_14("13-14", "Ages 13-14", "13-14 years", "mountains"),
    AGE_15_16("15-16", "Ages 15-16", "15-16 years", "city"),
    AGE_17_18("17-18", "Ages 17-18", "17-18 years", "adventure"),
    AGE_19_24("19-24", "Ages 19-24", "19-24 years", "travel"),
    AGE_25_34("25-34", "Ages 25-34", "25-34 years", "garden"),
    AGE_35_49("35-49", "Ages 35-49", "35-49 years", "landscape"),
    AGE_50_PLUS("50+", "Ages 50+", "50+ years", "starlight");

    companion object {
        val DEFAULT: PuzzleAgeBand = AGE_5_6

        fun fromAge(age: Int?): PuzzleAgeBand {
            if (age == null || age < 0) return DEFAULT
            return when (age) {
                in 0..4 -> AGE_3_4
                in 5..6 -> AGE_5_6
                in 7..8 -> AGE_7_8
                in 9..10 -> AGE_9_10
                in 11..12 -> AGE_11_12
                in 13..14 -> AGE_13_14
                in 15..16 -> AGE_15_16
                in 17..18 -> AGE_17_18
                in 19..24 -> AGE_19_24
                in 25..34 -> AGE_25_34
                in 35..49 -> AGE_35_49
                else -> AGE_50_PLUS
            }
        }

        fun fromKey(key: String?): PuzzleAgeBand {
            if (key.isNullOrBlank()) return DEFAULT
            return values().firstOrNull { it.key == key } ?: DEFAULT
        }

        fun isValidKey(key: String?): Boolean {
            return values().any { it.key == key }
        }
    }
}
