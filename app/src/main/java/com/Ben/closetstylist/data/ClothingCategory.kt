package com.Ben.closetstylist.data

enum class ClothingCategory {
    TOP, BOTTOM, DRESS, OUTERWEAR, SHOES, ACCESSORY;

    fun displayName(): String = name.lowercase().replaceFirstChar { it.uppercase() }
}
