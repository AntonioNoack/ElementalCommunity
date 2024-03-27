package me.antonio.noack.elementalcommunity.itempedia

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import me.antonio.noack.elementalcommunity.AllManager
import me.antonio.noack.elementalcommunity.Element
import me.antonio.noack.elementalcommunity.OneElement
import me.antonio.noack.elementalcommunity.R
import me.antonio.noack.elementalcommunity.api.WebServices
import java.text.DateFormat
import java.util.Date
import kotlin.concurrent.thread

class ItempediaAdapter(private val manager: AllManager) :
    RecyclerView.Adapter<ItempediaAdapter.ViewHolder>() {

    class ViewHolder(val view: OneElement) : RecyclerView.ViewHolder(view)

    var startIndex = 0
    var currentItems: List<Element> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = OneElement(manager, null)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // present results
        val uuid = position + startIndex
        val index = currentItems.binarySearch { it.uuid.compareTo(uuid) }
        if (index >= 0) {
            val element = currentItems[index]
            holder.view.element = element
            holder.view.alpha = if (uuid in AllManager.unlockedIds.keys) 255 else HIDDEN_ALPHA
            holder.view.invalidate()
            holder.view.setOnClickListener {
                // open menu, where we show more data about the element
                val dialog = AlertDialog.Builder(manager)
                    .setView(R.layout.itempedia_item)
                    .show()
                thread {
                    WebServices.askStats(uuid, { stats ->
                        manager.runOnUiThread {
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
                        }
                    })
                }
                dialog.findViewById<OneElement>(R.id.elementView)?.element = element
                dialog.findViewById<TextView>(R.id.uuid)?.text = "#$uuid"
                dialog.findViewById<TextView>(R.id.craftingCount)?.text =
                    element.craftingCount.toString()
                dialog.findViewById<TextView>(R.id.creationDate)?.text =
                    DateFormat.getDateInstance().format(Date(element.createdDate * 86_400_000L)) // days to millis
                dialog.findViewById<View>(R.id.back)?.setOnClickListener { dialog.dismiss() }
            }
        } else {
            holder.view.alpha = HIDDEN_ALPHA
            holder.view.element = null
            holder.view.setOnClickListener { }
        }
    }

    override fun getItemCount(): Int {
        return ITEMS_PER_PAGE
    }

    companion object {
        const val HIDDEN_ALPHA = 120
        const val ITEMS_PER_PAGE = 250 // -> 200 pages
    }
}