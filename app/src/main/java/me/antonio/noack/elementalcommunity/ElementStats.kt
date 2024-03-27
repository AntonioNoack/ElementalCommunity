package me.antonio.noack.elementalcommunity

data class ElementStats(
    val element: Element,
    val numRecipes: Int,
    val numIngredients: Int,
    val numCraftedAsIngredient: Int,
    val numSuggestedRecipes: Int,
    val numSuggestedIngredients: Int,
)