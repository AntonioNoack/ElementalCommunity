package me.antonio.noack.elementalcommunity

import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import kotlinx.android.synthetic.main.random.*
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

        dialog.first.element = compA
        dialog.second.element = compB

        fun next(){

            dialog.findViewById<TextView>(R.id.name)?.text = ""

            compA = getRandomComponent()
            compB = getRandomComponent()

            dialog.first.element = compA
            dialog.second.element = compB

            dialog.first.invalidate()
            dialog.second.invalidate()

        }

        dialog.findViewById<TextView>(R.id.next)?.setOnClickListener { next() }

        dialog.findViewById<TextView>(R.id.cancel)?.setOnClickListener {
            dialog.dismiss()
        }

        setSubmitAction(all, dialog.findViewById(R.id.submit)!!, dialog, false, { compA }, { compB }, { result ->
            if(AllManager.unlockedIds.contains(compA.uuid) && AllManager.unlockedIds.contains(compB.uuid)){
                // unlock this element...
                AllManager.unlockedIds.add(result.uuid)
                AllManager.addRecipe(compA, compB, result)
            }
            next()
        })

    }

}