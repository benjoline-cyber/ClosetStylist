package com.Ben.closetstylist.data

enum class Season {
    SPRING, SUMMER, FALL, WINTER;

    fun displayName(): String = name.lowercase().replaceFirstChar { it.uppercase() }
}
