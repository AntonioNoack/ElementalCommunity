package me.antonio.noack.elementalcommunity

fun escape(str: String) = str
    .replace("\n", " ")
    .replace('\t', ' ')
    .replace("   ", " ")
    .replace("  ", " ")
    .replace("\"", "\\\"")
    .replace("xmlns:android=\"http://schemas.android.com/apk/res/android\"", "")
    .replace("   ", " ")
    .replace("  ", " ")