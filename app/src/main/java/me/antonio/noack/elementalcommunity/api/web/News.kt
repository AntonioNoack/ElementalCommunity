package me.antonio.noack.elementalcommunity.api.web

import me.antonio.noack.elementalcommunity.Element

class News(val dt: Int, val a: Element, val b: Element, val result: String, val resultGroup: Int, val w: Int){
    override fun toString(): String {
        return "$a + $b -> $result($resultGroup) at $dt, $w"
    }
}