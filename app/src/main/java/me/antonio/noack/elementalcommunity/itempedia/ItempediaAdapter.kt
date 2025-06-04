package me.antonio.noack.elementalcommunity.itempedia

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import me.antonio.noack.elementalcommunity.AllManager
import me.antonio.noack.elementalcommunity.AllManager.Companion.applyStyle
import me.antonio.noack.elementalcommunity.Element
import me.antonio.noack.elementalcommunity.OneElement
import me.antonio.noack.elementalcommunity.R
import me.antonio.noack.elementalcommunity.api.WebServices
import java.text.DateFormat
import java.util.Date

class ItempediaAdapter(private val manager: AllManager) :
    RecyclerView.Adapter<ItempediaAdapter.ViewHolder>() {

    class ViewHolder(view: OneElement) : RecyclerView.ViewHolder(view)

    var currentItems: List<Element> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = OneElement(manager, null)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // present results
        val view = holder.itemView as OneElement
        val element = currentItems.getOrNull(position)
        if (element != null) {
            val uuid = element.uuid
            view.element = element
            view.alphaOverride = if (uuid in AllManager.unlockedIds.keys) 255 else HIDDEN_ALPHA
            view.invalidate()
            view.setOnClickListener {
                // open menu, where we show more data about the element
                val dialog = AlertDialog.Builder(manager)
                    .setView(R.layout.itempedia_item)
                    .show()
                applyStyle(dialog)

                WebServices.askStats(uuid, { stats ->
                    dialog.findViewById<TextView>(R.id.numRecipes)?.text =
                        stats.numRecipes.toString()
                    dialog.findViewById<TextView>(R.id.numIngredients)?.text =
                        stats.numIngredients.toString()
                    dialog.findViewById<TextView>(R.id.numSuggestedRecipes)?.text =
                        stats.numSuggestedRecipes.toString()
                    dialog.findViewById<TextView>(R.id.numSuggestedIngredients)?.text =
                        stats.numSuggestedIngredients.toString()
                    dialog.findViewById<TextView>(R.id.craftingCountAsIngredient)?.text =
                        stats.numCraftedAsIngredient.toString()
                })
                dialog.findViewById<OneElement>(R.id.elementView)?.element = element
                dialog.findViewById<TextView>(R.id.uuid)?.text = "#$uuid"
                dialog.findViewById<TextView>(R.id.craftingCount)?.text =
                    element.craftingCount.toString()
                dialog.findViewById<TextView>(R.id.creationDate)?.text =
                    DateFormat.getDateInstance()
                        .format(Date(element.createdDate * 86_400_000L)) // days to millis
                dialog.findViewById<View>(R.id.back)?.setOnClickListener { dialog.dismiss() }
            }
        } else {
            view.alphaOverride = HIDDEN_ALPHA
            view.element = null
            view.setOnClickListener { }
        }
    }

    override fun getItemCount(): Int {
        return currentItems.size
    }

    companion object {
        const val HIDDEN_ALPHA = 120
        const val ITEMS_PER_PAGE = 250 // -> 200 pages
    }
}