package com.adhdfocus.app.domain.puzzle

data class PuzzleDefinition(
    val puzzleKey: String,
    val title: String,
    val subtitle: String,
    val imageUrl: String,
    val ageBand: PuzzleAgeBand
)

private data class PuzzleVariant(
    val suffix: String,
    val title: String,
    val subtitle: String,
    val assetName: String
)

object PuzzleCatalog {
    private val variantsByAgeBand: Map<PuzzleAgeBand, List<PuzzleVariant>> = mapOf(
        PuzzleAgeBand.AGE_3_4 to listOf(
            PuzzleVariant("a", "Tiny Animal Trail", "Woodland friends discover the day together.", "puzzle_age_3_4"),
            PuzzleVariant("b", "Sunny Meadow Picnic", "A bright picnic path full of butterflies and baby animals.", "puzzle_age_3_4_b")
        ),
        PuzzleAgeBand.AGE_5_6 to listOf(
            PuzzleVariant("a", "Rainbow Rescue", "A cheerful festival world with helpers everywhere.", "puzzle_age_5_6"),
            PuzzleVariant("b", "Carnival Garden", "A playful rainbow park with rides, streamers, and celebration.", "puzzle_age_5_6_b")
        ),
        PuzzleAgeBand.AGE_7_8 to listOf(
            PuzzleVariant("a", "Rocket Garden", "Curious explorers meet robots among the stars.", "puzzle_age_7_8"),
            PuzzleVariant("b", "Star Camp", "An observatory adventure lights up the night sky.", "puzzle_age_7_8_b")
        ),
        PuzzleAgeBand.AGE_9_10 to listOf(
            PuzzleVariant("a", "Forest Quest", "Hidden maps and treehouse trails lead to discovery.", "puzzle_age_9_10"),
            PuzzleVariant("b", "Canyon Clues", "Safe explorer paths reveal waterfalls and cave secrets.", "puzzle_age_9_10_b")
        ),
        PuzzleAgeBand.AGE_11_12 to listOf(
            PuzzleVariant("a", "Ocean Atlas", "A marine mission of wonder, maps, and sea life.", "puzzle_age_11_12"),
            PuzzleVariant("b", "Tidepool Lab", "Science and adventure meet on a bright island coast.", "puzzle_age_11_12_b")
        ),
        PuzzleAgeBand.AGE_13_14 to listOf(
            PuzzleVariant("a", "Mountain Run", "Sunrise effort and alpine focus shape the journey.", "puzzle_age_13_14"),
            PuzzleVariant("b", "Base Camp Rise", "A training camp scene built around courage and momentum.", "puzzle_age_13_14_b")
        ),
        PuzzleAgeBand.AGE_15_16 to listOf(
            PuzzleVariant("a", "City Lights", "Creative urban energy turns effort into progress.", "puzzle_age_15_16"),
            PuzzleVariant("b", "Arts District", "A rooftop-to-street scene full of movement and collaboration.", "puzzle_age_15_16_b")
        ),
        PuzzleAgeBand.AGE_17_18 to listOf(
            PuzzleVariant("a", "Adventure Path", "A horizon scene made for next-step confidence.", "puzzle_age_17_18"),
            PuzzleVariant("b", "Open Road Outlook", "A sunrise stop filled with reflection and possibility.", "puzzle_age_17_18_b")
        ),
        PuzzleAgeBand.AGE_19_24 to listOf(
            PuzzleVariant("a", "Traveler's View", "Golden-hour exploration through a living city.", "puzzle_age_19_24"),
            PuzzleVariant("b", "Railway Stories", "A rich station district invites movement and discovery.", "puzzle_age_19_24_b")
        ),
        PuzzleAgeBand.AGE_25_34 to listOf(
            PuzzleVariant("a", "Garden Escape", "A restorative retreat where effort feels grounded.", "puzzle_age_25_34"),
            PuzzleVariant("b", "Wellness Courtyard", "A modern greenhouse world built for recharge and focus.", "puzzle_age_25_34_b")
        ),
        PuzzleAgeBand.AGE_35_49 to listOf(
            PuzzleVariant("a", "Landscape Drift", "A layered lakeside weekend scene of calm momentum.", "puzzle_age_35_49"),
            PuzzleVariant("b", "Lakeside Lodge", "A balanced outdoor escape with steady, restorative energy.", "puzzle_age_35_49_b")
        ),
        PuzzleAgeBand.AGE_50_PLUS to listOf(
            PuzzleVariant("a", "Starlight Harbor", "A peaceful coastal evening with lantern glow and calm water.", "puzzle_age_50_plus"),
            PuzzleVariant("b", "Moonrise Conservatory", "A harbor garden scene shaped by serenity and reflection.", "puzzle_age_50_plus_b")
        )
    )

    fun definitionFor(ageBand: PuzzleAgeBand, cycleIndex: Int): PuzzleDefinition {
        val variants = variantsByAgeBand[ageBand].orEmpty().ifEmpty {
            listOf(
                PuzzleVariant(
                    suffix = "default",
                    title = ageBand.displayName,
                    subtitle = "Keep completing days to reveal the full puzzle.",
                    assetName = "puzzle_age_5_6"
                )
            )
        }
        val variantIndex = cycleIndex.mod(variants.size)
        val variant = variants[variantIndex]
        val roundNumber = (cycleIndex / variants.size) + 1

        return PuzzleDefinition(
            puzzleKey = "${ageBand.key}-${variant.suffix}-$cycleIndex",
            title = variant.title,
            subtitle = "${variant.subtitle} Round $roundNumber.",
            imageUrl = variant.assetName,
            ageBand = ageBand
        )
    }
}
