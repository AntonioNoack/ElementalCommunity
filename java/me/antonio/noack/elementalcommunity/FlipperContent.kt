package me.antonio.noack.elementalcommunity

enum class FlipperContent(val id: Int){

    MENU(0),
    GAME(1),
    MANDALA(2),
    TREE(3),
    COMBINER(4),
    SETTINGS(5)

    ;

    fun bind(all: AllManager){
        all.flipper.displayedChild = id
    }


}