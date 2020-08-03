package me.antonio.noack.elementalcommunity

import android.app.Dialog
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import me.antonio.noack.elementalcommunity.api.WebServices
import java.lang.IllegalArgumentException
import kotlin.concurrent.thread
import kotlin.math.min

object BasicOperations {

    fun onRecipeRequest(first: Element, second: Element, all: AllManager, measuredWidth: Int, measuredHeight: Int, unlockElement: (Element) -> Unit, add: (Element) -> Unit){
        thread(true){
            WebServices.askRecipe(first, second, all, { result ->
                if(result != null){
                    unlockElement(result)
                } else if(AllManager.askFrequency.isTrue()){
                    // staticRunOnUIThread { AllManager.askingSound.play() }
                    askForRecipe(first, second, all, measuredWidth, measuredHeight, unlockElement){
                        WebServices.askRecipe(first, second, all, { result2 ->
                            if(result2 != null){// remove in the future, when the least amount of support is 2 or sth like that
                                synchronized(Unit){
                                    AllManager.unlockedIds.add(result2.uuid)
                                    AllManager.saveElement2(result2)
                                    add(result2)
                                }
                            }
                        }, {})
                    }
                } else {
                    AllManager.toast(R.string.no_result_found, false)
                }
            }, {
                AllManager.toast("${it.javaClass.simpleName}: ${it.message}", true)
            })
        }
    }

    fun askForRecipe(a: Element, b: Element, all: AllManager, measuredWidth: Int, measuredHeight: Int, unlockElement: (Element) -> Unit, onSuccess: () -> Unit){
        // ask for recipe to add :)
        // todo show that we are loading
        WebServices.getCandidates(a, b, { candidates ->
            AllManager.staticRunOnUIThread {
                val dialog: Dialog = AlertDialog.Builder(all)
                    .setView(R.layout.add_recipe)
                    .show()
                dialog.findViewById<TextView>(R.id.cancel)!!.setOnClickListener {
                    try {
                        dialog.dismiss()
                    } catch (e: Throwable) {
                    }
                }
                setSubmitAction(all, dialog.findViewById(R.id.submit)!!, dialog, true, { a }, { b }, {
                    unlockElement(it)
                    onSuccess()
                })
                if (candidates.isEmpty()) {
                    dialog.findViewById<View>(R.id.title2)!!.visibility = View.GONE
                } else {
                    val suggestionsView = dialog.findViewById<LinearLayout>(R.id.suggestions)!!
                    val theWidth = min(measuredWidth, measuredHeight) * 2f / 5
                    for (candidate in candidates) {
                        val view = CandidateView(all, null)
                        view.candidate = candidate
                        view.layoutParams = LinearLayout.LayoutParams(theWidth.toInt(), LinearLayout.LayoutParams.WRAP_CONTENT)
                        view.theWidth = theWidth
                        view.invalidate()
                        view.onLiked = {
                            try {
                                dialog.dismiss()
                            } catch (e: IllegalArgumentException) {
                            }
                            WebServices.likeRecipe(all, candidate.uuid, {
                                AllManager.toast(R.string.sent, false)
                            })
                        }
                        view.onDisliked = {
                            WebServices.dislikeRecipe(all, candidate.uuid, {
                                AllManager.toast(R.string.sent, false)
                            })
                        }
                        view.layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        suggestionsView.addView(view)
                    }
                }
            }
        }, {
            AllManager.toast("${it.javaClass.simpleName}: ${it.message}", true)
        })
    }

    fun setSubmitAction(all: AllManager, submit: TextView, dialog: Dialog, allowingDismiss: Boolean, getComponentA: () -> Element, getComponentB: () -> Element, unlockElement: (Element) -> Unit){
        submit.setOnClickListener {
            val name = dialog.findViewById<TextView>(R.id.name)!!.text.toString()
            val group = dialog.findViewById<GroupSelectorView>(R.id.colors)!!.selected
            if (group < 0) {
                AllManager.toast(R.string.please_choose_color, false)
                return@setOnClickListener
            }
            if (name.isEmpty()) {
                AllManager.toast(R.string.please_choose_name, false)
                return@setOnClickListener
            }
            for (char in name) {
                if (char !in 'A'..'Z' && char !in 'a'..'z' && char !in '0'..'9' && char !in " '.,-/*") {
                    AllManager.toast(R.string.only_az09, true)
                    return@setOnClickListener
                }
            }
            thread {
                WebServices.suggestRecipe(all, getComponentA(), getComponentB(), name, group, {
                    val lines = it.split('\n')
                    val str = lines[0]
                    val index1 = str.indexOf(':')
                    val index2 = str.indexOf(':', index1 + 1)
                    AllManager.toast(R.string.sent, false)
                    if(allowingDismiss){
                        AllManager.staticRunOnUIThread {
                            try {
                                dialog.dismiss()
                            } catch (e: IllegalArgumentException) {
                            }
                        }
                    }
                    if (index1 > 0 && index2 > 0) {
                        val rUUID = str.substring(0, index1).toIntOrNull() ?: return@suggestRecipe
                        val rGroup = str.substring(index1 + 1, index2).toIntOrNull() ?: return@suggestRecipe
                        val rName = str.substring(index2 + 1)
                        // val secondaryData = lines.getOrNull(1)?.split(':')
                        // removed, because it's rather expensive to compute and not that important
                        // maybe we should save that information on per-instance basis in the database...
                        val rCraftingCount = -1
                        val element = Element.get(rName, rUUID, rGroup, rCraftingCount)
                        unlockElement(element)
                    }
                })
            }
        }

    }

}