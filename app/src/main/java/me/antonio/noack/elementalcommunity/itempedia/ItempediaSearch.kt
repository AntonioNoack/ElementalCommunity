package me.antonio.noack.elementalcommunity.itempedia

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import me.antonio.noack.elementalcommunity.AllManager
import me.antonio.noack.elementalcommunity.R
import me.antonio.noack.elementalcommunity.utils.Compact.compacted

object ItempediaSearch {

    var search = ""

    fun setupSearchButton(all: AllManager) {
        val back = all.findViewById<View>(R.id.itempediaTitle)
        all.searchButton3?.setOnClickListener {
            if (back?.visibility == View.VISIBLE) {
                back.visibility = View.GONE
                all.search3?.visibility = View.VISIBLE
            } else {
                back?.visibility = View.VISIBLE
                all.search3?.visibility = View.GONE
            }
        }
        all.search3?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                onTextChange(all, s.toString())
            }

            override fun afterTextChanged(s: Editable?) {
                onTextChange(all, s.toString())
            }
        })
    }

    fun onTextChange(all: AllManager, search0: String) {
        val search = compacted(search0)
        if (search != ItempediaSearch.search) {
            ItempediaSearch.search = search
            ItempediaPageLoader.loadNumPages(all)
        }
    }

}