package me.antonio.noack.elementalcommunity.help

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import me.antonio.noack.elementalcommunity.AllManager
import me.antonio.noack.elementalcommunity.Element
import me.antonio.noack.elementalcommunity.R
import me.antonio.noack.elementalcommunity.RecipeView
import me.antonio.noack.elementalcommunity.api.WebServices
import me.antonio.noack.elementalcommunity.io.ElementType
import me.antonio.noack.elementalcommunity.io.SplitReader
import me.antonio.noack.elementalcommunity.utils.Compact.compacted
import me.antonio.noack.webdroid.StringID
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

object RecipeHelper {

    fun openHelper(all: AllManager) {

        val dialog = AlertDialog.Builder(all)
            .setView(R.layout.recipe_helper)
            .setCancelable(true)
            .show()

        val window = dialog.window
        window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        cache.clear()

        dialog.findViewById<TextView>(R.id.diamonds)!!.text = all.getDiamondCount().toString()

        val res = all.resources
        val costString = all.resources.getString(R.string.cost_template)
        val offerList = dialog.findViewById<ViewGroup>(R.id.offers)!!

        for (offer in offers) {
            val headerView = all.layoutInflater.inflate(R.layout.helper_offer, offerList, false)
            headerView.findViewById<TextView>(R.id.title)!!.text = res.getString(offer.title)
            headerView.findViewById<TextView>(R.id.cost)!!.text = costString
                .replace("#cost", offer.cost.toString())
                .replace("#name", res.getString(offer.shortName))
            offerList.addView(headerView)
            offer.buildLayout(all, offerList, dialog)
        }


        // todo set on click listeners etc...


    }

    val lookUpCost = 10

    fun readRecipes(raw: String, search: String): ArrayList<Recipe> {
        val data = SplitReader(
            listOf(
                ElementType.INT, ElementType.INT, ElementType.INT
            ), ';', ':', raw
        )
        val returnValue = ArrayList<Recipe>()
        while (data.hasRemaining) {
            if (data.read() >= 3) {
                val compA = data.getInt(0)
                val compB = data.getInt(1)
                val result = data.getInt(2)
                val a = AllManager.elementById[compA] ?: continue
                val b = AllManager.elementById[compB] ?: continue
                val r = AllManager.elementById[result] ?: continue
                val isKnown = AllManager.elementByRecipe[a to b] != null
                val aIsKnown = AllManager.unlockedIds.contains(a.uuid)
                val bIsKnown = AllManager.unlockedIds.contains(b.uuid)
                val rIsKnown = AllManager.unlockedIds.contains(r.uuid)
                // todo show this in the list...
                // todo make the names unreadable
                val score = (if (isKnown) 12
                else if (aIsKnown && bIsKnown) 0
                else if (aIsKnown || bIsKnown) 4
                else if (rIsKnown) 12 else 8) + if (compacted(search) == compacted(r.name)) 0 else 1
                returnValue.add(Recipe(a, b, r, score))
            }
        }
        returnValue.sortBy { it.score }
        return returnValue
    }

    // todo remember unlocked recipes...
    // todo show all known recipes, too?...

    fun makeLessReadable(string: String, apply: Boolean): String {
        if (!apply) return string
        val random = Random(string.hashCode())
        val randomIndex = random.nextInt(string.length)
        val readableChance = 0.3f * (1 - 1 / (1 + 0.1f * string.length))
        return string.mapIndexed { index, c ->
            if (index == randomIndex || random.nextFloat() < readableChance) {
                c
            } else '*'
        }.joinToString("")
    }

    val cache = ConcurrentHashMap<String, ArrayList<Recipe>>()
    val offers = listOf(
        Offer(
            R.string.recipe_lookup,
            R.string.recipe_lookup_short,
            lookUpCost
        ) { all, list, dialog ->

            val mainList =
                all.layoutInflater.inflate(R.layout.helper_search_recipe, list, false) as ViewGroup
            val textField = mainList.findViewById<EditText>(R.id.search)!!

            var oldSearch: String
            val recipes = mainList.findViewById<ViewGroup>(R.id.previews)!!

            // todo show names of elements blurred, which are not already discovered
            fun showResult(list: List<Recipe>, search: String) {
                all.runOnUiThread {
                    synchronized(recipes) {
                        recipes.removeAllViews()
                        for (recipe in list) {
                            val a = recipe.a
                            val b = recipe.b
                            val r = recipe.r
                            // todo show this in the list...
                            // todo make the names unreadable
                            val recipeView = RecipeView(all, null)
                            val isKnown = AllManager.elementByRecipe[a to b] != null
                            recipeView.aName = makeLessReadable(a.name, !isKnown)
                            recipeView.aGroup = a.group
                            recipeView.bName = makeLessReadable(b.name, !isKnown)
                            recipeView.bGroup = b.group
                            recipeView.rName = makeLessReadable(
                                r.name,
                                !isKnown && compacted(search) != compacted(r.name)
                            )
                            recipeView.rGroup = r.group
                            recipeView.overlay =
                                if (isKnown) 0 else (0xaa000000.toInt() or all.resources.getColor(R.color.colorPrimaryDark)
                                    .and(0xffffff))
                            if (!isKnown) {
                                var wasUnlocked = false
                                recipeView.setOnClickListener {
                                    if (wasUnlocked) return@setOnClickListener
                                    if (all.spendDiamonds(lookUpCost + 1)) {// +1, because unlocking this elements gives you 1 point
                                        wasUnlocked = true
                                        recipeView.aName = a.name
                                        recipeView.aGroup = a.group
                                        recipeView.bName = b.name
                                        recipeView.bGroup = b.group
                                        recipeView.rName = r.name
                                        recipeView.rGroup = r.group
                                        recipeView.overlay = 0
                                        AllManager.addRecipe(a, b, r, all)
                                        recipeView.invalidate()
                                        dialog.findViewById<TextView>(R.id.diamonds)?.text =
                                            all.getDiamondCount().toString()
                                    }
                                }
                            }
                            recipeView.invalidate()
                            recipes.addView(recipeView)
                        }
                        recipes.visibility =
                            if (recipes.childCount == 0) View.GONE else View.VISIBLE
                    }
                }
            }

            textField.addTextChangedListener(object : TextWatcher {
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val search = compacted(s.toString())
                    oldSearch = search
                    if (search.isBlank()) {
                        showResult(emptyList(), "")
                    } else {
                        val old = cache[search]
                        if (old == null) {
                            WebServices.askRecipes(search, { answer ->
                                val recipeList = readRecipes(answer, search)
                                cache[search] = recipeList
                                if (oldSearch == search) {
                                    showResult(recipeList, search)
                                }
                            })
                        } else {
                            showResult(old, search)
                        }
                    }
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun afterTextChanged(s: Editable?) {}
            })

            list.addView(mainList)
        }
    )

    class Recipe(val a: Element, val b: Element, val r: Element, val score: Int)

    fun offerSpendingMoneyOnDiamondsOrWatchAds(all: AllManager) {
        // todo xD
        AllManager.toast("You don't have enough crystals to continue this action!", false)
    }

    // todo multiple products per offer?
    class Offer(
        val title: StringID,
        val shortName: StringID,
        val cost: Int,
        val buildLayout: (all: AllManager, list: ViewGroup, dialog: AlertDialog) -> Unit
    )

}