package me.antonio.noack.elementalcommunity

import android.content.Context
import android.util.AttributeSet

class Combiner(ctx: Context, attributeSet: AttributeSet?) : UnlockedRows(ctx, attributeSet) {

    override fun onRecipeRequest(compA: Element, compB: Element) {
        BasicOperations.askForCandidates(compA, compB, all,
            measuredWidth, measuredHeight,
            { result -> addRecipeAndInvalidate(compA, compB, result) }) {}
    }

}