package me.antonio.noack.elementalcommunity.utils

import java.util.*

object Compact {

    fun compacted(name: String): String {
        var text = name
        if(text.startsWith("the ", true)){
            text = text.substring(4)
        }
        return text.lowercase(Locale.getDefault())
            .replace(";", "")
            .replace(" ", "")
    }

}