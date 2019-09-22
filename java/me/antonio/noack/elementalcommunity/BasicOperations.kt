package me.antonio.noack.elementalcommunity

import android.app.Dialog
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import me.antonio.noack.elementalcommunity.api.WebServices
import java.lang.IllegalArgumentException
import kotlin.concurrent.thread

object BasicOperations {

    fun onRecipeRequest(first: Element, second: Element, all: AllManager, measuredWidth: Int, unlockElement: (Element) -> Unit, add: (Element) -> Unit){
        thread(true){
            WebServices.askRecipe(first, second, { result ->
                if(result != null){
                    unlockElement(result)
                } else {
                    // staticRunOnUIThread { AllManager.askingSound.play() }
                    askForRecipe(first, second, all, measuredWidth, unlockElement)
                    WebServices.askRecipe(first, second, { result2 ->
                        if(result2 != null){// remove in the future, when the least amount of support is 2 or sth like that
                            synchronized(Unit){
                                add(result2)
                                AllManager.unlockedIds.add(result2.uuid)
                                AllManager.save()
                            }
                        }
                    }, {})
                }
            }, {
                AllManager.toast("${it.javaClass.simpleName}: ${it.message}", true)
            })
        }
    }

    fun askForRecipe(a: Element, b: Element, all: AllManager, measuredWidth: Int, unlockElement: (Element) -> Unit){
        // ask for recipe to add :)
        // todo show that we are loading
        WebServices.getCandidates(a, b, { candidates ->
            AllManager.staticRunOnUIThread {
                val dialog: Dialog = AlertDialog.Builder(all)
                    .setView(R.layout.add_recipe)
                    .show()
                dialog.findViewById<TextView>(R.id.cancel).setOnClickListener {
                    try {
                        dialog.dismiss()
                    } catch (e: Throwable) {
                    }
                }
                setSubmitAction(all, dialog.findViewById(R.id.submit)!!, dialog, true, { a }, { b }, {})
                if (candidates.isEmpty()) {
                    dialog.findViewById<View>(R.id.title2).visibility = View.GONE
                } else {
                    val suggestionsView = dialog.findViewById<LinearLayout>(R.id.suggestions)
                    val theWidth = measuredWidth * 2f / 5
                    for (candidate in candidates) {
                        val view = Candidate(all, null)
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
            val name = dialog.findViewById<TextView>(R.id.name).text.toString()
            val group = dialog.findViewById<Colors>(R.id.colors).selected
            if (group < 0) {
                AllManager.toast(R.string.please_choose_color, false)
                return@setOnClickListener
            }
            if (name.isEmpty()) {
                AllManager.toast(R.string.please_choose_name, false)
                return@setOnClickListener
            }
            for (char in name) {
                if (char !in 'A'..'Z' && char !in 'a'..'z' && char !in '0'..'9' && char !in " ,.'") {
                    AllManager.toast(R.string.only_az09, true)
                    return@setOnClickListener
                }
            }
            thread {
                WebServices.suggestRecipe(all, getComponentA(), getComponentB(), name, group, {
                    val str = it.split('\n')[0]
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
                        val element = Element.get(rName, rUUID, rGroup)
                        unlockElement(element)
                    }
                })
            }
        }

    }

}