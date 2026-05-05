package com.Ben.closetstylist.domain

enum class StylistPersona(val description: String) {
    MINIMALIST("Phoebe Philo / The Row — quiet luxury, neutral palette, structured silhouettes, minimal accessories"),
    CLEAN_GIRL("Hailey Bieber / Sofia Richie — polished basics, monochrome, gold jewelry, slicked back hair energy"),
    INDIE("Alexa Chung / Jane Birkin — vintage-leaning, mixed prints, ballet flats, slightly undone"),
    PREP("Ralph Lauren / menswear-leaning — blazers, oxford shirts, loafers, structured"),
    STREETWEAR("relaxed, sneakers-forward, oversized fits, sportswear influences"),
    ROMANTIC("feminine, florals, soft textures, dresses-forward"),
    EDITORIAL("fashion-forward, statement pieces, willing to take risks");

    fun displayName(): String = when (this) {
        MINIMALIST -> "Minimalist"
        CLEAN_GIRL -> "Clean Girl"
        INDIE -> "Indie"
        PREP -> "Prep"
        STREETWEAR -> "Streetwear"
        ROMANTIC -> "Romantic"
        EDITORIAL -> "Editorial"
    }
}
