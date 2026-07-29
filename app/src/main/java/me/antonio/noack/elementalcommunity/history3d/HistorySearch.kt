package me.antonio.noack.elementalcommunity.history3d

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.antonio.noack.elementalcommunity.AllManager
import me.antonio.noack.elementalcommunity.AllManager.Companion.applyStyle
import me.antonio.noack.elementalcommunity.R
import me.antonio.noack.elementalcommunity.api.WebServices
import me.antonio.noack.elementalcommunity.utils.Compact.compacted
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object HistorySearch {

    fun setupSearchButton(all: AllManager) {
        val button: View? = all.findViewById(R.id.searchButton4)
        button?.setOnClickListener {
            openSearchDialogue(all)
        }
    }

    private fun openSearchDialogue(all: AllManager) {

        val dialog = AlertDialog.Builder(all)
            .setView(R.layout.element_search_results)
            .setCancelable(true)
            .show()
        applyStyle(dialog)

        val rec = dialog.findViewById<RecyclerView>(R.id.previews)!!
        rec.setHasFixedSize(true)
        val numColumns = 10 // good number?
        rec.layoutManager = GridLayoutManager(all, numColumns)
        val adapter = HistorySearchAdapter(all, dialog)
        rec.adapter = adapter

        var oldSearch = ""
        fun onTextChanged(newSearch0: String) {
            val newSearch = compacted(newSearch0)
            if (newSearch != oldSearch) {
                oldSearch = newSearch
                WebServices.askPage(0, newSearch, { elements, _ ->
                    all.runOnUiThread {
                        // show results
                        adapter.currentItems = elements
                            .sortedBy { it.compacted.distance(newSearch) }
                        adapter.notifyDataSetChanged()
                        rec.smoothScrollToPosition(0)
                    }
                })
            }
        }

        dialog.findViewById<EditText>(R.id.search)
            ?.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?, start: Int, count: Int, after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    onTextChanged(s.toString())
                }

                override fun afterTextChanged(s: Editable?) {
                    onTextChanged(s.toString())
                }
            })

        dialog.findViewById<View>(R.id.cancel)
            ?.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }


    /**
     * Levenshtein distance / edit distance,
     * O(|this| * |other|), so quite expensive for long strings
     * returns the number of changes, which would need to happen to change one string to the other
     * operations: change character, add character, remove character
     * distance >= abs(|this|-|other|)
     *
     * if you heavily rely on this method, write me, and I'll cache its dynamic allocations
     * */
    @JvmStatic
    fun CharSequence.distance(other: CharSequence, ignoreCase: Boolean = false): Int {
        if (this == other) return 0
        val sx = this.length + 1
        val sy = other.length + 1
        if (sx <= 1 || sy <= 1) return abs(sx - sy)
        if (sx <= 2 && sy <= 2) return 1
        // switching both sides may be valuable
        if (sx > sy + 5) return other.distance(this, ignoreCase)
        // create cache
        val dist = IntArray(sx * max(sy, 3))
        for (x in 1 until sx) dist[x] = x
        for (y in 1 until sy) {
            var i2 = (y % 3) * sx
            dist[i2++] = y
            var i1 = ((y + 2) % 3) * sx
            var i0 = ((y + 1) % 3) * sx - 1
            val prev1 = other[y - 1]
            for (i in 1 until sx) {
                val prev0 = this[i - 1]
                dist[i2] = when {
                    prev0.equals(prev1, ignoreCase) -> dist[i1]
                    i > 1 && y > 1 &&
                            prev0.equals(other[y - 2], ignoreCase) &&
                            prev1.equals(this[i - 2], ignoreCase) ->
                        min(dist[i0], min(dist[i2 - 1], dist[i1 + 1])) + 1
                    else -> min(dist[i1], min(dist[i2 - 1], dist[i1 + 1])) + 1
                }
                i0++
                i1++
                i2++
            }
        }
        val yi = (((sy + 2) % 3) + 1)
        return dist[sx * yi - 1]
    }

}