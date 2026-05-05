package com.Ben.closetstylist.data

enum class Occasion {
    WORK,
    CASUAL,
    CASUAL_DRESSED_UP,
    FORMAL,
    ACTIVEWEAR,
    LOUNGEWEAR;

    fun displayName(): String = when (this) {
        WORK -> "Work"
        CASUAL -> "Casual"
        CASUAL_DRESSED_UP -> "Casual dressed up"
        FORMAL -> "Formal"
        ACTIVEWEAR -> "Activewear"
        LOUNGEWEAR -> "Loungewear"
    }
}
