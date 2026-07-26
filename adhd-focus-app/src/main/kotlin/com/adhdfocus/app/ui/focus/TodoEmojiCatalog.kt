package com.adhdfocus.app.ui.focus

data class TodoEmoji(
    val emoji: String,
    val label: String,
    val keywords: List<String>
)

object TodoEmojiCatalog {
    val all: List<TodoEmoji> = listOf(
        TodoEmoji("🪥", "Brush teeth", listOf("brush", "teeth", "tooth", "toothbrush", "dental")),
        TodoEmoji("🛁", "Bath", listOf("bath", "bathe", "tub", "wash")),
        TodoEmoji("🚿", "Shower", listOf("shower", "wash", "clean")),
        TodoEmoji("🧼", "Soap", listOf("soap", "wash", "hands", "clean")),
        TodoEmoji("🧴", "Lotion", listOf("lotion", "sunscreen", "cream")),
        TodoEmoji("👕", "Shirt", listOf("shirt", "clothes", "dressed", "get dressed")),
        TodoEmoji("👖", "Pants", listOf("pants", "clothes", "dressed")),
        TodoEmoji("🧦", "Socks", listOf("socks", "clothes", "dressed")),
        TodoEmoji("👟", "Shoes", listOf("shoes", "sneakers", "feet")),
        TodoEmoji("🧥", "Coat", listOf("coat", "jacket", "outside")),
        TodoEmoji("🎒", "Backpack", listOf("backpack", "bag", "school", "pack")),
        TodoEmoji("📚", "Books", listOf("book", "books", "read", "reading", "library")),
        TodoEmoji("✏️", "Pencil", listOf("pencil", "write", "homework", "school")),
        TodoEmoji("📝", "Homework", listOf("homework", "paper", "write", "worksheet")),
        TodoEmoji("🥣", "Breakfast", listOf("breakfast", "cereal", "eat", "meal")),
        TodoEmoji("🥪", "Lunch", listOf("lunch", "sandwich", "eat", "meal")),
        TodoEmoji("🍽️", "Dinner", listOf("dinner", "plate", "eat", "meal")),
        TodoEmoji("🥛", "Milk", listOf("milk", "drink")),
        TodoEmoji("💧", "Water", listOf("water", "drink", "hydrate")),
        TodoEmoji("🍎", "Snack", listOf("snack", "apple", "fruit", "eat")),
        TodoEmoji("💊", "Medicine", listOf("medicine", "meds", "pill", "vitamin")),
        TodoEmoji("🛏️", "Bed", listOf("bed", "bedtime", "sleep", "nap")),
        TodoEmoji("🌙", "Night", listOf("night", "bedtime", "sleep")),
        TodoEmoji("⏰", "Wake up", listOf("wake", "alarm", "morning", "time")),
        TodoEmoji("🧸", "Toys", listOf("toy", "toys", "play", "stuffed")),
        TodoEmoji("🧩", "Puzzle", listOf("puzzle", "game", "play")),
        TodoEmoji("🧹", "Sweep", listOf("sweep", "broom", "clean", "chores")),
        TodoEmoji("🧺", "Laundry", listOf("laundry", "clothes", "basket", "wash")),
        TodoEmoji("🗑️", "Trash", listOf("trash", "garbage", "bin", "chores")),
        TodoEmoji("🧽", "Wipe", listOf("wipe", "sponge", "clean", "table")),
        TodoEmoji("🛋️", "Living room", listOf("living room", "couch", "tidy")),
        TodoEmoji("🍴", "Dishes", listOf("dishes", "fork", "kitchen", "meal")),
        TodoEmoji("🐶", "Dog", listOf("dog", "pet", "puppy", "feed dog")),
        TodoEmoji("🐱", "Cat", listOf("cat", "pet", "kitten", "feed cat")),
        TodoEmoji("🐟", "Fish", listOf("fish", "pet", "feed fish")),
        TodoEmoji("🌱", "Plants", listOf("plant", "plants", "water plants")),
        TodoEmoji("⚽", "Soccer", listOf("soccer", "ball", "sports", "practice")),
        TodoEmoji("🏀", "Basketball", listOf("basketball", "ball", "sports", "practice")),
        TodoEmoji("🏈", "Football", listOf("football", "sports", "practice")),
        TodoEmoji("🎵", "Music", listOf("music", "practice", "instrument", "song")),
        TodoEmoji("🎨", "Art", listOf("art", "paint", "draw", "craft")),
        TodoEmoji("🚗", "Car", listOf("car", "leave", "ride", "appointment")),
        TodoEmoji("🚌", "Bus", listOf("bus", "school bus", "school")),
        TodoEmoji("🏠", "Home", listOf("home", "house")),
        TodoEmoji("✅", "Check", listOf("check", "done", "finish", "complete")),
        TodoEmoji("⭐", "Star", listOf("star", "special", "favorite")),
        TodoEmoji("❤️", "Heart", listOf("heart", "love", "kind")),
        TodoEmoji("😊", "Smile", listOf("smile", "happy", "kind"))
    )

    fun search(query: String, limit: Int = 36): List<TodoEmoji> {
        val terms = query.trim()
            .lowercase()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        if (terms.isEmpty()) {
            return all.take(limit)
        }

        return all
            .map { item -> item to score(item, terms) }
            .filter { (_, score) -> score > 0 }
            .sortedWith(compareByDescending<Pair<TodoEmoji, Int>> { it.second }.thenBy { it.first.label })
            .map { it.first }
            .take(limit)
    }

    private fun score(item: TodoEmoji, terms: List<String>): Int {
        val label = item.label.lowercase()
        val keywords = item.keywords.map { it.lowercase() }
        return terms.fold(0) { total, term ->
            total + when {
                label == term -> 10
                label.contains(term) -> 7
                keywords.any { it == term } -> 8
                keywords.any { it.contains(term) } -> 5
                else -> 0
            }
        }
    }
}
