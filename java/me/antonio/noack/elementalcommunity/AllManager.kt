package me.antonio.noack.elementalcommunity

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.SparseArray
import java.util.*
import kotlin.collections.HashMap
import kotlin.concurrent.thread
import android.view.View
import android.view.View.*
import androidx.appcompat.app.AlertDialog
import me.antonio.noack.elementalcommunity.GroupsEtc.GroupColors
import me.antonio.noack.elementalcommunity.api.WebServices
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.children
import me.antonio.noack.elementalcommunity.GroupsEtc.minimumCraftingCount
import me.antonio.noack.elementalcommunity.help.RecipeHelper
import me.antonio.noack.elementalcommunity.tree.TreeView
import kotlin.math.abs


// todo make a directory with all elements? :D

// Sounds:
// magic: https://freesound.org/people/suntemple/sounds/241809/
// ok: https://freesound.org/people/grunz/sounds/109663/
// click: https://freesound.org/people/vitriolix/sounds/706/

class AllManager: AppCompatActivity() {

    companion object {

        var customUUID = 0L
        var showCraftingCounts = true

        var unlockedIds = hashSetOf(1, 2, 3, 4)
        var elementById = SparseArray<Element>()

        var elementsByGroup = Array(GroupColors.size + 12){
            TreeSet<Element>()
        }

        var unlockeds = Array(GroupColors.size + 12){
            TreeSet<Element>()
        }

        const val MAX_FAVOURITES_RATIO = 0.15f
        var FAVOURITE_COUNT = 5
        var favourites = arrayOfNulls<Element>(FAVOURITE_COUNT)

        var elementByRecipe = HashMap<Pair<Element, Element>, Element>()

        val elementByName = HashMap<String, Element>()

        // for the tree
        fun addRecipe(a: Element, b: Element, r: Element, all: AllManager){
            if(a.uuid > b.uuid) return addRecipe(b, a, r, all)
            elementByRecipe[a to b] = r
            invalidate()
            all.updateDiamondCount()
        }

        fun toast(message: String, isLong: Boolean) = staticToast1(message, isLong)
        fun toast(message: Int, isLong: Boolean) = staticToast2(message, isLong)

        lateinit var staticToast1: (message: String, isLong: Boolean) -> Unit
        lateinit var staticToast2: (message: Int, isLong: Boolean) -> Unit
        lateinit var staticRunOnUIThread: (() -> Unit) -> Unit
        lateinit var save: () -> Unit
        lateinit var invalidate: () -> Unit

        lateinit var successSound: Sound
        lateinit var okSound: Sound
        lateinit var clickSound: Sound
        // lateinit var askingSound: Sound

        var askFrequency = AskFrequencyOption.ALWAYS

    }

    lateinit var pref: SharedPreferences
    lateinit var combiner: Combiner
    lateinit var unlocked: UnlockedRows
    lateinit var treeView: TreeView
    lateinit var startButton: View
    lateinit var flipper: ViewFlipper
    lateinit var treeViewButton: View
    lateinit var suggestButton: View
    lateinit var settingButton: View
    lateinit var back1: View
    lateinit var back2: View
    lateinit var back3: View
    lateinit var backArrow1: View
    lateinit var backArrow2: View
    lateinit var backArrow3: View
    lateinit var favTitle: TextView
    lateinit var favSlider: SeekBar
    lateinit var search1: EditText
    lateinit var search2: EditText
    lateinit var searchButton1: View
    lateinit var searchButton2: View
    lateinit var randomButton: View
    lateinit var spaceSlider: SeekBar
    lateinit var resetEverythingButton: View
    lateinit var newsView: NewsView
    lateinit var freqSlider: SeekBar
    lateinit var freqTitle: TextView
    lateinit var craftingCountsSwitch: SwitchCompat

    val diamondViews = ArrayList<TextView>()

    fun initViews(){
        combiner = findViewById(R.id.combiner)
        unlocked = findViewById(R.id.unlocked)
        treeView = findViewById(R.id.tree)
        startButton = findViewById(R.id.start)
        flipper = findViewById(R.id.flipper)
        treeViewButton = findViewById(R.id.treeButton)
        suggestButton = findViewById(R.id.suggest)
        settingButton = findViewById(R.id.settingsButton)
        back1 = findViewById(R.id.back1)
        back2 = findViewById(R.id.back2)
        back3 = findViewById(R.id.back3)
        favTitle = findViewById(R.id.favTitle)
        favSlider = findViewById(R.id.favSlider)
        backArrow1 = findViewById(R.id.backArrow1)
        backArrow2 = findViewById(R.id.backArrow2)
        backArrow3 = findViewById(R.id.backArrow3)
        search1 = findViewById(R.id.search1)
        search2 = findViewById(R.id.search2)
        searchButton1 = findViewById(R.id.searchButton1)
        searchButton2 = findViewById(R.id.searchButton2)
        randomButton = findViewById(R.id.randomButton)
        spaceSlider = findViewById(R.id.spaceSlider)
        resetEverythingButton = findViewById(R.id.resetEverythingButton)
        newsView = findViewById(R.id.newsView)
        freqSlider = findViewById(R.id.frequencySlider)
        freqTitle = findViewById(R.id.frequencyTitle)
        craftingCountsSwitch = findViewById(R.id.craftingCountsSwitch)
    }

    fun vibrate(millis: Long = 200L){
        val v = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        // Vibrate for 500 milliseconds
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            //deprecated in API 26
            v.vibrate(millis)
        }
    }

    fun goFullScreen(){
        val flags = SYSTEM_UI_FLAG_IMMERSIVE or SYSTEM_UI_FLAG_FULLSCREEN or SYSTEM_UI_FLAG_HIDE_NAVIGATION
        unlocked.systemUiVisibility = flags
        combiner.systemUiVisibility = flags
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.all)

        initViews()

        actionBar?.hide()

        if(Build.VERSION.SDK_INT >= 21){
            window.navigationBarColor = resources.getColor(R.color.colorPrimary)
        }

        goFullScreen()

        unlocked.all = this
        combiner.all = this
        treeView.all = this

        pref = getPreferences(Context.MODE_PRIVATE)

        askFrequency = AskFrequencyOption[pref]
        showCraftingCounts = pref.getBoolean("showCraftingCounts", true)

        successSound = Sound(R.raw.magic, this)
        okSound = Sound(R.raw.ok, this)
        clickSound = Sound(R.raw.click, this)
        // askingSound = So

        staticRunOnUIThread = {
            runOnUiThread(it)
        }

        staticToast1 = { msg, isLong -> runOnUiThread { Toast.makeText(this, msg, if(isLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show() } }
        staticToast2 = { msg, isLong -> runOnUiThread { Toast.makeText(this, msg, if(isLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show() } }
        invalidate = {

            combiner.invalidateSearch()
            unlocked.invalidateSearch()

            combiner.postInvalidate()
            unlocked.postInvalidate()
            treeView.hasTree = false
            treeView.postInvalidate()

        }
        
        startButton.setOnClickListener { flipper.displayedChild = 1 }
        treeViewButton.setOnClickListener {
            flipper.displayedChild = 2
        }
        suggestButton.setOnClickListener {
            combiner.invalidateSearch()
            flipper.displayedChild = 3
        }
        settingButton.setOnClickListener {
            favTitle.text = resources.getString(R.string.favourites).replace("#count", FAVOURITE_COUNT.toString())
            favSlider.progress = if(FAVOURITE_COUNT == 0) 0 else FAVOURITE_COUNT - 2
            freqSlider.progress = askFrequency.ordinal
            freqTitle.text = resources.getString(R.string.frequency_of_asking_title).replace("#frequency", askFrequency.displayName)
            flipper.displayedChild = 4
        }
        back1.setOnClickListener { flipper.displayedChild = 0 }
        back2.setOnClickListener { flipper.displayedChild = 0 }
        backArrow3.setOnClickListener { flipper.displayedChild = 0 }
        addSearchListeners(back3, backArrow1, searchButton1, search1, unlocked)
        addSearchListeners(back1, backArrow2, searchButton2, search2, combiner)
        randomButton.setOnClickListener { RandomSuggestion.make(this) }

        val spaceSliderOffset = 1
        spaceSlider.max = 5 + spaceSliderOffset
        spaceSlider.progress = treeView.tree.multiplierX - spaceSliderOffset
        spaceSlider.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val tree = treeView.tree
                val value = spaceSliderOffset + progress
                tree.multiplierX = value
                tree.multiplierY = value
                invalidate()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        favSlider.max = 10
        favSlider.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                var value = progress
                if(value != 0) value += 2
                FAVOURITE_COUNT = value
                favTitle.text = resources.getString(R.string.favourites).replace("#count", FAVOURITE_COUNT.toString())
                resizeFavourites(pref)
                save()
                invalidate()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        freqSlider.max = AskFrequencyOption.values().lastIndex
        freqSlider.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                askFrequency = AskFrequencyOption.values().getOrNull(progress) ?: AskFrequencyOption.ALWAYS
                freqTitle.text = resources.getString(R.string.frequency_of_asking_title).replace("#frequency", askFrequency.displayName)
                askFrequency.store(pref)
                freqSlider.invalidate()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        craftingCountsSwitch.isChecked = showCraftingCounts
        craftingCountsSwitch.setOnCheckedChangeListener { _, isChecked ->
            showCraftingCounts = isChecked
            pref.edit().putBoolean("showCraftingCounts", isChecked).apply()
            invalidate()
        }

        resetEverythingButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.are_you_sure_reset_everything)
                .setPositiveButton(android.R.string.yes){ _, _ ->
                    pref.edit().clear().putLong("customUUID", customUUID).apply()
                    unlockedIds = hashSetOf(1, 2, 3, 4)
                    for(list in unlockeds){ list.removeAll(list.filter { it.uuid > 4 }) }
                    for(i in 0 until favourites.size) favourites[i] = null
                    FAVOURITE_COUNT = 5
                    resizeFavourites(pref)
                    favSlider.progress = FAVOURITE_COUNT-2
                    combiner.invalidateSearch()
                    unlocked.invalidateSearch()
                    save()
                    invalidate()
                }
                .setNegativeButton(android.R.string.no, null)
                .setCancelable(true)
                .show()
        }

        resetEverythingButton.setOnLongClickListener {
            // generate 100 diamonds over 10 long clicks
            spendDiamonds(if(Math.random() < 0.1) -250 else 15)
            true
        }

        save = {
            val edit = pref.edit()
            edit.putString("unlocked", unlockedIds.joinToString(","))
            for((_, element) in elementByName){
                val id = element.uuid
                edit.putString("$id.name", element.name)
                edit.putInt("$id.group", element.group)
                if(element.craftingCount >= minimumCraftingCount){
                    edit.putInt("$id.crafted", element.craftingCount)
                }
            }
            for((src, dst) in elementByRecipe){
                edit.putInt("recipe.${src.first.uuid}.${src.second.uuid}", dst.uuid)
            }
            for((i, favourite) in favourites.withIndex()){
                if(favourite == null){
                    edit.remove("favourites[$i]")
                } else {
                    edit.putInt("favourites[$i]", favourite.uuid)
                }
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

        val names = arrayOf("???", "Earth", "Air", "Water", "Fire")
        val groups = intArrayOf(20, 5, 12, 20, 4)
        for(id in unlockedIds){
            val name = pref.getString("$id.name", names.getOrNull(id) ?: names[0])!!
            val group = pref.getInt("$id.group", groups.getOrNull(id) ?: groups[0])
            val craftingCount = pref.getInt("$id.crafted", -1)
            val element = Element.get(name, id, group, craftingCount)
            unlockeds[element.group].add(element)
        }

        FAVOURITE_COUNT = pref.getInt("favourites.length", FAVOURITE_COUNT)
        resizeFavourites(pref)

        invalidate()

        updateGroupSizesAndNames()

        askNews()

        for((key, value) in pref.all){
            if(key.endsWith(".name")){
                // an element
                val id = key.split('.')[0].toIntOrNull() ?: continue
                val name = value.toString()
                val group = pref.getInt("$id.group", -1)
                if(group < 0) continue
                val craftingCount = pref.getInt("$id.crafted", -1)
                Element.get(name, id, group, craftingCount)
            }
        }

        for((key, value) in pref.all){
            if(key.startsWith("recipe.") && (value is Int || value.toString().toIntOrNull() != null)){
                val parts = key.split('.')
                val a = elementById[parts[1].toIntOrNull() ?: continue] ?: continue
                val b = elementById[parts[2].toIntOrNull() ?: continue] ?: continue
                val c = elementById[if(value is Int) value else value.toString().toInt()] ?: continue
                addRecipe(a, b, c, this)
            }
        }

        diamondViews.clear()

        for(child in flipper.children){
            val diamondTextView = child.findViewById<TextView>(R.id.diamonds) ?: continue
            diamondViews.add(diamondTextView)
            diamondTextView.setOnClickListener {
                RecipeHelper.openHelper(this)
            }
            (diamondTextView.parent as? View)?.setOnClickListener {
                RecipeHelper.openHelper(this)
            }
        }

        updateDiamondCount()

    }

    fun getDiamondCount(): Int {
        return elementByRecipe.size + unlockedIds.size * 3 - pref.getInt("diamondCountSpent", 0)
    }

    fun spendDiamonds(count: Int): Boolean {
        val current = getDiamondCount()
        val hasEnough = count < 0 || current >= count
        if(hasEnough){
            successSound.play()
            pref.edit().putInt("diamondCountSpent", pref.getInt("diamondCountSpent", 0) + count).apply()
            updateDiamondCount()
        } else {
            RecipeHelper.offerSpendingMoneyOnDiamondsOrWatchAds(this)
        }
        return hasEnough
    }

    fun updateDiamondCount(){
        runOnUiThread {
            val count = getDiamondCount()
            for(view in diamondViews){
                view.text = count.toString()
            }
        }
    }

    fun resizeFavourites(pref: SharedPreferences){
        favourites = Array(FAVOURITE_COUNT){ favourites.getOrNull(it) }
        for(i in 0 until FAVOURITE_COUNT){
            favourites[i] = favourites.getOrNull(i) ?: elementById[pref.getInt("favourites[$i]", -1)] ?: null
        }
    }

    fun addSearchListeners(back3: View, backArrow: View, searchButton: View, search: TextView, unlocked: UnlockedRows){
        back3.setOnClickListener { flipper.displayedChild = 0 }
        backArrow.setOnClickListener { flipper.displayedChild = 0 }
        val diamondView = (back3.parent as? View)?.findViewById<View>(R.id.diamonds)
        searchButton.setOnClickListener {
            if(back3.visibility == VISIBLE){
                back3.visibility = GONE
                diamondView?.visibility = GONE
                search.visibility = VISIBLE
            } else {
                back3.visibility = VISIBLE
                diamondView?.visibility = VISIBLE
                search.visibility = GONE
            }
        }
        search.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                unlocked.search = s.toString()
                unlocked.invalidateSearch()
            }
            override fun afterTextChanged(s: Editable?) {
                unlocked.search = s.toString()
                unlocked.invalidateSearch()
            }
        })
    }

    /*override fun onGenericMotionEvent(event: MotionEvent): Boolean {
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
    }*/

    fun updateGroupSizesAndNames(){
        thread {
            WebServices.updateGroupSizesAndNames()
        }
    }

    fun askNews(){
        thread {
            WebServices.askNews(20, {
                newsView.news = it
                newsView.postInvalidate()
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

    override fun onResume() {
        super.onResume()
        goFullScreen()
    }
}
