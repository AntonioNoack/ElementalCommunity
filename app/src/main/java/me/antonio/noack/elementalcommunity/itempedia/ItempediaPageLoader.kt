package me.antonio.noack.elementalcommunity.itempedia

import android.annotation.SuppressLint
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.math.MathUtils
import androidx.recyclerview.widget.RecyclerView
import me.antonio.noack.elementalcommunity.AllManager
import me.antonio.noack.elementalcommunity.BasicOperations
import me.antonio.noack.elementalcommunity.R
import me.antonio.noack.elementalcommunity.api.ServerService
import me.antonio.noack.elementalcommunity.api.WebServices

object ItempediaPageLoader {

    @SuppressLint("SetTextI18n")
    fun createItempediaPages(all: AllManager, numElements: Int) {

        val pageList = all.findViewById<LinearLayout>(R.id.pageFlipper)!!
        pageList.removeAllViews()
        val numPages =
            (numElements + ItempediaAdapter.ITEMS_PER_PAGE - 1) / ItempediaAdapter.ITEMS_PER_PAGE

        val views = ArrayList<TextView>()
        var previouslyClicked = 0

        fun openPage(i: Int) {
            val view = views[i]
            views[previouslyClicked].alpha = 0.7f
            view.alpha = 1f
            previouslyClicked = i
            loadItempediaPage(all, i, ItempediaSearch.search)
        }

        val layoutInflater = all.layoutInflater
        for (i in 0 until numPages) {
            layoutInflater.inflate(R.layout.itempedia_page, pageList)
            val view = pageList.getChildAt(pageList.childCount - 1) as TextView
            view.text = "${i + 1}"
            view.alpha = if (i == 0) 1f else 0.7f
            view.setOnClickListener {
                openPage(i)
            }
            views.add(view)
        }
        all.deltaItempediaPage = { delta ->
            openPage(MathUtils.clamp(previouslyClicked + delta, 0, numPages - 1))
        }
    }

    fun loadNumPages(all: AllManager) {
        val search = ItempediaSearch.search
        WebServices.askPage(-1, search, { _, maxUUID ->
            createItempediaPages(all, maxUUID)
        })
        loadItempediaPage(all, 0, search)
    }

    private var lastPage = -1
    private var lastSearch = ""

    @SuppressLint("NotifyDataSetChanged")
    fun loadItempediaPage(all: AllManager, pageIndex: Int, search: String) {
        if (pageIndex == lastPage && search == lastSearch) return
        lastPage = pageIndex
        lastSearch = search
        BasicOperations.todo++
        val lazyDone = lazy {
            BasicOperations.done++
        }
        WebServices.askPage(pageIndex, search, { list, _ ->
            lazyDone.value
            if (lastPage == pageIndex && lastSearch == search) {
                list.sortBy { it.uuid }
                all.itempediaAdapter.currentItems = list
                all.itempediaAdapter.notifyDataSetChanged()
                val rec = all.findViewById<RecyclerView>(R.id.itempediaElements)!!
                rec.smoothScrollToPosition(0)
            }
        }, {
            lazyDone.value
            ServerService.defaultOnError(it)
        })
    }
}