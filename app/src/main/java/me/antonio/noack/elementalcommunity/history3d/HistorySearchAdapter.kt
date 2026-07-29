package me.antonio.noack.elementalcommunity.history3d

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import me.antonio.noack.elementalcommunity.AllManager
import me.antonio.noack.elementalcommunity.Element
import me.antonio.noack.elementalcommunity.OneElement

class HistorySearchAdapter(
    private val manager: AllManager,
    private val dialog: Dialog
) : RecyclerView.Adapter<HistorySearchAdapter.ViewHolder>() {

    class ViewHolder(view: OneElement) : RecyclerView.ViewHolder(view)

    var currentItems: List<Element> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = OneElement(manager, null)
        view.simpleSize = true
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // present results
        val view = holder.itemView as OneElement
        val element = currentItems.getOrNull(position)
        if (element != null) {
            view.element = element
            view.alphaOverride = 255
            view.invalidate()
            view.setOnClickListener {
                manager.historyView?.skipToElement(element)
                dialog.dismiss()
            }
        } else {
            view.alphaOverride = 0
            view.element = null
            view.setOnClickListener { }
        }
    }

    override fun getItemCount(): Int {
        return currentItems.size
    }
}