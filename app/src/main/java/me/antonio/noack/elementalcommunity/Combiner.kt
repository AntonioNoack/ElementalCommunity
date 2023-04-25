package me.antonio.noack.elementalcommunity

import android.content.Context
import android.util.AttributeSet
import kotlin.concurrent.thread

class Combiner(ctx: Context, attributeSet: AttributeSet?): UnlockedRows(ctx, attributeSet) {

    override fun onRecipeRequest(first: Element, second: Element) {
        thread {
            askForRecipe(first, second)
        }
    }

    fun askForRecipe(a: Element, b: Element){
        BasicOperations.askForRecipe(a, b, all, measuredWidth, measuredHeight, { add(a, b, it) }){}
    }

}