package me.antonio.noack.elementalcommunity

import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import me.antonio.noack.elementalcommunity.BasicOperations.setSubmitAction
import me.antonio.noack.elementalcommunity.api.WebServices
import me.antonio.noack.elementalcommunity.io.ElementType
import me.antonio.noack.elementalcommunity.io.SplitReader

object RandomSuggestion {

    private fun getRandomComponent(): Element {
        return AllManager.elementByName.values.random()
    }

    private fun getRandomRecipe(callback: (Element, Element) -> Unit) {
        WebServices.getRandomRecipe({
            val data = SplitReader(
                listOf(ElementType.INT, ElementType.INT, ElementType.STRING, ElementType.INT),
                '\n', ':', it
            )
            val components = ArrayList<Element>(2)
            while (data.read() >= 4 && components.size < 2) {
                val uuid = data.getInt(0)
                val group = data.getInt(1)
                val name = data.getString(2)
                val counter = data.getInt(3)
                components += Element.get(name, uuid, group, counter, true)
            }
            if (components.size >= 2) {
                callback(components[0], components[1])
            } else {
                getRandomRecipeOffline(callback)
            }
        }, {
            it.printStackTrace()
            getRandomRecipeOffline(callback)
        })
    }

    private fun getRandomRecipeOffline(callback: (Element, Element) -> Unit) {
        callback(getRandomComponent(), getRandomComponent())
    }

    fun make(all: AllManager) {
        val dialog = AlertDialog.Builder(all)
            .setView(R.layout.random)
            .show()

        val first = dialog.findViewById<OneElement>(R.id.first)!!
        val second = dialog.findViewById<OneElement>(R.id.second)!!
        getRandomRecipe { a, b ->
            var compA = a
            var compB = b

            fun next() {
                dialog.findViewById<TextView>(R.id.name)?.text = ""

                getRandomRecipe { a, b ->
                    compA = a
                    compB = b

                    first.element = compA
                    second.element = compB

                    first.invalidate()
                    second.invalidate()
                }
            }

            dialog.findViewById<TextView>(R.id.next)?.setOnClickListener { next() }

            dialog.findViewById<TextView>(R.id.cancel)?.setOnClickListener {
                dialog.dismiss()
            }

            setSubmitAction(
                all, dialog.findViewById(R.id.submit)!!,
                dialog, false, { compA }, { compB }, { result ->
                    if (AllManager.unlockedIds.contains(compA.uuid) && AllManager.unlockedIds.contains(
                            compB.uuid
                        )
                    ) {
                        // unlock this element...
                        AllManager.unlockedIds.put(result.uuid)
                        AllManager.addRecipe(compA, compB, result, all)
                    }
                    next()
                })

            next()
        }
    }

}