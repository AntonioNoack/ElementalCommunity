package me.antonio.noack.elementalcommunity

import android.app.Dialog
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import kotlinx.android.synthetic.main.all.*
import java.lang.Math.abs
import java.util.*
import kotlin.collections.HashMap
import kotlin.concurrent.thread
import android.view.MotionEvent
import android.view.InputDevice
import androidx.appcompat.app.AlertDialog
import kotlinx.android.synthetic.main.combiner.*
import kotlinx.android.synthetic.main.game.*
import kotlinx.android.synthetic.main.menu.*
import kotlinx.android.synthetic.main.settings.*
import me.antonio.noack.elementalcommunity.GroupsEtc.GroupColors
import me.antonio.noack.elementalcommunity.api.WebServices
import kotlin.collections.ArrayList


// Sounds:
// magic: https://freesound.org/people/suntemple/sounds/241809/
// ok: https://freesound.org/people/grunz/sounds/109663/

class AllManager: AppCompatActivity() {

    companion object {

        var customUUID = 0L

        var unlockedIds = hashSetOf(1, 2, 3, 4)
        var elementById = HashMap<Int, Element>()

        var elementsByGroup = Array(GroupColors.size){
            ArrayList<Element>()
        }

        var unlockeds = Array(GroupColors.size){
            ArrayList<Element>()
        }

        val elementByName = HashMap<String, Element>()

        lateinit var staticToast1: (message: String, isLong: Boolean) -> Unit
        lateinit var staticToast2: (message: Int, isLong: Boolean) -> Unit
        lateinit var staticRunOnUIThread: (() -> Unit) -> Unit
        lateinit var save: () -> Unit
        lateinit var invalidate: () -> Unit

        lateinit var successSound: Sound
        lateinit var okSound: Sound
        // lateinit var askingSound: Sound

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.all)

        actionBar?.hide()

        unlocked.all = this
        combiner.all = this

        val pref = getPreferences(Context.MODE_PRIVATE)

        successSound = Sound(R.raw.magic, this)
        okSound = Sound(R.raw.ok, this)
        // askingSound = So

        staticRunOnUIThread = {
            runOnUiThread(it)
        }

        staticToast1 = { msg, isLong -> runOnUiThread { Toast.makeText(this, msg, if(isLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show() } }
        staticToast2 = { msg, isLong -> runOnUiThread { Toast.makeText(this, msg, if(isLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show() } }
        invalidate = {
            combiner.postInvalidate()
            unlocked.postInvalidate()
        }

        start.setOnClickListener { flipper.displayedChild = 1 }
        suggest.setOnClickListener { flipper.displayedChild = 2 }
        settings.setOnClickListener { flipper.displayedChild = 3 }
        back1.setOnClickListener { flipper.displayedChild = 0 }
        back2.setOnClickListener { flipper.displayedChild = 0 }
        back3.setOnClickListener { flipper.displayedChild = 0 }

        resetEverything.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.are_you_sure_reset_everything)
                .setPositiveButton(android.R.string.yes){ _, _ ->
                    pref.edit().clear().putLong("customUUID", customUUID).apply()
                    unlockedIds = hashSetOf(1, 2, 3, 4)
                    for(list in unlockeds){ list.removeAll(list.filter { it.uuid > 4 }) }
                    invalidate()
                }
                .setNegativeButton(android.R.string.no, null)
                .setCancelable(true)
                .show()
        }

        save = {
            val edit = pref.edit()
            edit.putString("unlocked", unlockedIds.joinToString(","))
            for((_, element) in elementByName){
                val id = element.uuid
                edit.putString("$id.name", element.name)
                edit.putInt("$id.group", element.group)
            }
            edit.apply()
        }

        unlockedIds.addAll(pref.getString("unlocked", "1,2,3,4")!!.split(',').map { x -> x.toInt() })

        // for identification <3 :D
        var ci = pref.getLong("customUUID", -1)
        if(ci < 0){
            ci = abs(Random().nextLong())
            pref.edit().putLong("customUUID", ci).apply()
        }

        customUUID = ci

        val unlocked = unlocked
        val names = arrayOf("???", "Earth", "Air", "Water", "Fire")
        val groups = intArrayOf(20, 5, 12, 20, 4)
        for(id in unlockedIds){
            val name = pref.getString("$id.name", names.getOrNull(id) ?: names[0])!!
            val group = pref.getInt("$id.group", groups.getOrNull(id) ?: groups[0])
            val element = Element.get(name, id, group)
            unlocked.add(element)
        }

        // todo update names...

        updateGroupSizes()

        askNews()

    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (0 != event.source and InputDevice.SOURCE_CLASS_POINTER) {
            when (event.action) {
                MotionEvent.ACTION_SCROLL -> {
                    unlocked.scroll += event.getAxisValue(MotionEvent.AXIS_VSCROLL)
                    unlocked.checkScroll()
                    unlocked.invalidate()
                    return true
                }
            }
        }
        return super.onGenericMotionEvent(event)
    }

    // todo make a directory with all elements? :D
    fun updateGroupSizes(){
        thread {
            WebServices.updateGroupSizes()
        }
    }

    fun askNews(){
        thread {
            WebServices.askNews(20, {
                news.news = it.toArray(arrayOfNulls(it.size))
                news.postInvalidate()
            })
        }
    }

    override fun onBackPressed() {
        if(flipper.displayedChild > 0){
            flipper.displayedChild = 0
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        Sound.destroyAll()
        super.onDestroy()
    }
}
