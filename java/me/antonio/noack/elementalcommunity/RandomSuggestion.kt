package me.antonio.noack.elementalcommunity

import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import me.antonio.noack.elementalcommunity.BasicOperations.setSubmitAction

object RandomSuggestion {

    fun getRandomComponent(): Element {
        return AllManager.elementByName.values.random()
    }

    fun make(all: AllManager){

        val dialog = AlertDialog.Builder(all)
            .setView(R.layout.random)
            .show()

        var compA = getRandomComponent()
        var compB = getRandomComponent()

        val first = dialog.findViewById<OneElement>(R.id.first)!!
        val second = dialog.findViewById<OneElement>(R.id.second)!!

        fun next(){
            all.runOnUiThread {

                dialog.findViewById<TextView>(R.id.name)?.text = ""

                compA = getRandomComponent()
                compB = getRandomComponent()

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

        setSubmitAction(all, dialog.findViewById(R.id.submit)!!, dialog, false, { compA }, { compB }, { result ->
            all.runOnUiThread {
                if(AllManager.unlockedIds.contains(compA.uuid) && AllManager.unlockedIds.contains(compB.uuid)){
                    // unlock this element...
                    AllManager.unlockedIds.add(result.uuid)
                    AllManager.addRecipe(compA, compB, result, all)
                }
                next()
            }
        })

        next()

    }

}