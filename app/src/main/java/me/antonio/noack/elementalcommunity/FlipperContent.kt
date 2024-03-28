package me.antonio.noack.elementalcommunity

enum class FlipperContent(val id: Int) {

    MENU(0),
    GAME(1),
    MANDALA(2),
    TREE(3),
    GRAPH(4),
    COMBINER(5),
    ITEMPEDIA(6),
    SETTINGS(7);

    fun bind(all: AllManager) {
        val flipper = all.flipper ?: return
        if (id > 0) {
            flipper.setInAnimation(all, R.anim.slide_in_right)
            flipper.setOutAnimation(all, R.anim.slide_out_right)
        } else {
            flipper.setInAnimation(all, R.anim.slide_in_left)
            flipper.setOutAnimation(all, R.anim.slide_out_left)
        }
        flipper.displayedChild = id
    }

}