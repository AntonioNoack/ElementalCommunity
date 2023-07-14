package me.antonio.noack.elementalcommunity

import android.view.View
import androidx.core.view.children

enum class FlipperContent(val id: Int) {

    MENU(R.id.menuLayout),
    GAME(R.id.gameLayout),
    MANDALA(R.id.mandalaLayout),
    TREE(R.id.treeLayout),
    GRAPH(R.id.graphLayout),
    COMBINER(R.id.combinerLayout),
    SETTINGS(R.id.settingsLayout),

    ;

    fun bind(all: AllManager) {
        val flipper = all.flipper ?: return
        val v = flipper.findViewById<View>(id)
        val index = if (v == null) 0 else flipper.children.indexOf(v)
        if (v == null) System.err.println("Missing $name!#1")
        else if (index < 0) System.err.println("Missing $name!#2")
        if (index > 0) {
            flipper.setInAnimation(all, R.anim.slide_in_right)
            flipper.setOutAnimation(all, R.anim.slide_out_right)
        } else {
            flipper.setInAnimation(all, R.anim.slide_in_left)
            flipper.setOutAnimation(all, R.anim.slide_out_left)
        }
        flipper.displayedChild = index
    }

}