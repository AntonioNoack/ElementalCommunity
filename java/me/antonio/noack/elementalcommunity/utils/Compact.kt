package me.antonio.noack.elementalcommunity.utils

object Compact {

    fun compacted(name: String): String {
        var text = name
        if(text.startsWith("the ", true)){
            text = text.substring(4)
        }
        return text.toLowerCase()
            .replace(";", "")
            .replace(" ", "")
    }

}