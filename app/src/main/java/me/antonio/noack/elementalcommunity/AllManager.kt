package me.antonio.noack.elementalcommunity

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.View.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.children
import me.antonio.noack.elementalcommunity.GroupsEtc.GroupColors
import me.antonio.noack.elementalcommunity.OfflineSuggestions.loadOfflineElements
import me.antonio.noack.elementalcommunity.api.ServerService
import me.antonio.noack.elementalcommunity.api.WebServices
import me.antonio.noack.elementalcommunity.cache.CombinationCache
import me.antonio.noack.elementalcommunity.graph.GraphView
import me.antonio.noack.elementalcommunity.help.RecipeHelper
import me.antonio.noack.elementalcommunity.help.SettingsInit
import me.antonio.noack.elementalcommunity.io.SaveLoadLogic
import me.antonio.noack.elementalcommunity.io.SplitReader2
import me.antonio.noack.elementalcommunity.mandala.MandalaView
import me.antonio.noack.elementalcommunity.tree.TreeView
import me.antonio.noack.elementalcommunity.utils.IntArrayList
import me.antonio.noack.webdroid.files.FileSaver
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread
import kotlin.math.abs

// Sounds:
// magic: https://freesound.org/people/suntemple/sounds/241809/
// ok: https://freesound.org/people/grunz/sounds/109663/
// click: https://freesound.org/people/vitriolix/sounds/706/

// todo does offline mode work with synchronizing/loading-saving your progress via a file / the server?

class AllManager : AppCompatActivity() {

    companion object {

        var customUUID = 0L
        var showCraftingCounts = true
        var showElementUUID = true
        var offlineMode = false

        val unlockedIds = ConcurrentHashSet<Int>(512)

        init {
            for (i in 1..4) unlockedIds.put(i)
        }

        val elementById = ConcurrentHashMap<Int, Element>(512)

        val elementsByGroup = Array(GroupColors.size + 12) {
            TreeSet<Element>()
        }

        val unlockedElements = Array(GroupColors.size + 12) {
            TreeSet<Element>()
        }

        const val MAX_FAVOURITES_RATIO = 0.15f
        var FAVOURITE_COUNT = 5
        var favourites = arrayOfNulls<Element>(FAVOURITE_COUNT)

        val elementByRecipe = ConcurrentHashMap<Pair<Element, Element>, Element>(2048)
        val recipesByElement = ConcurrentHashMap<Element, MutableList<Pair<Element, Element>>>(2048)

        val elementByName = ConcurrentHashMap<String, Element>(2048)

        fun registerBaseElements(pref: SharedPreferences?) {
            val names = arrayOf("???", "Earth", "Air", "Water", "Fire")
            val groups = intArrayOf(20, 5, 12, 20, 4)
            for (id in unlockedIds.keys) {// after a restart, this should be only 4
                val defName = names.getOrNull(id) ?: ""
                val defGroup = groups.getOrNull(id) ?: 0
                val name = pref?.getString("$id.name", defName) ?: defName
                val group = pref?.getInt("$id.group", defGroup) ?: defGroup
                val craftingCount = pref?.getInt("$id.crafted", -1) ?: -1
                val element = Element.get(name, id, group, craftingCount, false)
                unlockedElements[element.group].add(element)
            }
        }

        // for the tree
        fun addRecipe(a: Element, b: Element, r: Element, all: AllManager?, save: Boolean = true) {
            if (a.uuid > b.uuid) return addRecipe(b, a, r, all, save)
            val pair = a to b
            unlockedIds.put(r.uuid)
            elementByRecipe[pair] = r
            val list = recipesByElement[r]
            if (list == null) {
                recipesByElement[r] = arrayListOf(a to b)
            } else synchronized(list) {
                if (pair !in list) {
                    list.add(pair)
                }
            }
            invalidate()
            all?.updateDiamondCount()
            if (save) saveElement2(r)
        }

        fun getRecipe(a: Element, b: Element): Element? {
            if (a.uuid > b.uuid) return getRecipe(b, a)
            return elementByRecipe[a to b]
        }

        fun toast(message: String, isLong: Boolean) = staticToast1(message, isLong)
        fun toast(message: Int, isLong: Boolean) = staticToast2(message, isLong)

        lateinit var staticToast1: (message: String, isLong: Boolean) -> Unit
        lateinit var staticToast2: (message: Int, isLong: Boolean) -> Unit
        lateinit var staticRunOnUIThread: (() -> Unit) -> Unit
        lateinit var edit: () -> SharedPreferences.Editor
        lateinit var saveElement2: (Element) -> Unit
        lateinit var saveElement: (SharedPreferences.Editor, Element) -> Unit
        lateinit var saveFavourites: () -> Unit

        lateinit var invalidate: () -> Unit

        lateinit var successSound: Sound
        lateinit var okSound: Sound
        lateinit var clickSound: Sound
        lateinit var backgroundMusic: List<Sound>

        var askFrequency = AskFrequencyOption.ALWAYS
        var backgroundMusicVolume = 1f

        const val diamondBuyKey = "diamondCountBought"
        const val diamondWatchKey = "diamondCountWatched"
        const val diamondSpentKey = "diamondCountSpent"

    }

    lateinit var pref: SharedPreferences
    var combiner: Combiner? = null
    var unlocked: UnlockedRows? = null
    var startButton: View? = null
    var flipper: ViewFlipper? = null
    var treeViewButton: View? = null
    var graphViewButton: View? = null
    var mandalaViewButton: View? = null
    var suggestButton: View? = null
    var settingButton: View? = null
    var back1: View? = null
    var back2: View? = null
    var back3: View? = null
    var backArrow1: View? = null
    var backArrow2: View? = null
    var backArrow3: View? = null
    var backArrow4: View? = null
    var backArrow5: View? = null
    var favTitle: TextView? = null
    var favSlider: SeekBar? = null
    var search1: EditText? = null
    var search2: EditText? = null
    var searchButton1: View? = null
    var searchButton2: View? = null
    var randomButton: View? = null
    var resetEverythingButton: View? = null
    var newsView: NewsView? = null
    var freqSlider: SeekBar? = null
    var freqTitle: TextView? = null
    var switchServerButton: View? = null
    var craftingCountsSwitch: SwitchCompat? = null
    var displayUUIDSwitch: SwitchCompat? = null
    var offlineModeSwitch: SwitchCompat? = null
    var volumeSlider: SeekBar? = null
    var volumeTitle: TextView? = null

    var treeView: TreeView? = null
    var graphView: GraphView? = null
    var spaceSlider: SeekBar? = null
    var mandalaView: MandalaView? = null

    private val diamondViews = ArrayList<TextView>()

    private fun initViews() {
        combiner = findViewById(R.id.combiner) ?: null
        unlocked = findViewById(R.id.unlocked) ?: null
        treeView = findViewById(R.id.tree) ?: null
        graphView = findViewById(R.id.graph) ?: null
        mandalaView = findViewById(R.id.tree2) ?: null
        startButton = findViewById(R.id.start) ?: null
        flipper = findViewById(R.id.flipper) ?: null
        treeViewButton = findViewById(R.id.treeButton) ?: null
        graphViewButton = findViewById(R.id.graphButton) ?: null
        mandalaViewButton = findViewById(R.id.mandalaButton) ?: null
        suggestButton = findViewById(R.id.suggest) ?: null
        settingButton = findViewById(R.id.settingsButton) ?: null
        back1 = findViewById(R.id.back1) ?: null
        back2 = findViewById(R.id.back2) ?: null
        back3 = findViewById(R.id.back3) ?: null
        favTitle = findViewById(R.id.favTitle) ?: null
        favSlider = findViewById(R.id.favSlider) ?: null
        backArrow1 = findViewById(R.id.backArrow1) ?: null
        backArrow2 = findViewById(R.id.backArrow2) ?: null
        backArrow3 = findViewById(R.id.backArrow3) ?: null
        backArrow4 = findViewById(R.id.backArrow4) ?: null
        backArrow5 = findViewById(R.id.backArrow5) ?: null
        search1 = findViewById(R.id.search1) ?: null
        search2 = findViewById(R.id.search2) ?: null
        searchButton1 = findViewById(R.id.searchButton1) ?: null
        searchButton2 = findViewById(R.id.searchButton2) ?: null
        randomButton = findViewById(R.id.randomButton) ?: null
        spaceSlider = findViewById(R.id.spaceSlider) ?: null
        resetEverythingButton = findViewById(R.id.resetEverythingButton) ?: null
        newsView = findViewById(R.id.newsView) ?: null
        freqSlider = findViewById(R.id.frequencySlider) ?: null
        freqTitle = findViewById(R.id.frequencyTitle) ?: null
        volumeSlider = findViewById(R.id.backgroundVolumeSlider) ?: null
        volumeTitle = findViewById(R.id.backgroundVolumeTitle) ?: null
        craftingCountsSwitch = findViewById(R.id.craftingCountsSwitch) ?: null
        displayUUIDSwitch = findViewById(R.id.displayUUIDSwitch) ?: null
        switchServerButton = findViewById(R.id.switchServer) ?: null
        offlineModeSwitch = findViewById(R.id.offlineModeSwitch) ?: null
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            SaveLoadLogic.WRITE_EXT_STORAGE_CODE -> {
                FileSaver.continueSave(this, resultCode, data)
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun goFullScreen() {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            SYSTEM_UI_FLAG_IMMERSIVE or SYSTEM_UI_FLAG_FULLSCREEN or SYSTEM_UI_FLAG_HIDE_NAVIGATION
        } else {
            SYSTEM_UI_FLAG_FULLSCREEN or SYSTEM_UI_FLAG_HIDE_NAVIGATION
        }
        unlocked?.systemUiVisibility = flags
        combiner?.systemUiVisibility = flags
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.all)

        initViews()

        actionBar?.hide()

        if (Build.VERSION.SDK_INT >= 21) {
            window.navigationBarColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                resources.getColor(R.color.colorPrimary, theme)
            } else {
                resources.getColor(R.color.colorPrimary)
            }
        }

        goFullScreen()

        unlocked?.all = this
        combiner?.all = this
        treeView?.all = this
        graphView?.all = this
        mandalaView?.all = this

        successSound = Sound(R.raw.magic, this)
        okSound = Sound(R.raw.ok, this)
        clickSound = Sound(R.raw.click, this)
        backgroundMusic = listOf(
            Sound(R.raw.music_aquatic_omniverse, this),
            Sound(R.raw.music_clouds_make2, this),
            Sound(R.raw.music_infinite_elements, this)
        )
        // askingSound = So

        staticRunOnUIThread = { callback ->
            MusicScheduler.tick()
            runOnUiThread {
                try {
                    callback()
                } catch (e: Exception) {
                    staticToast1(e.message + e.localizedMessage, true)
                }
            }
        }

        staticToast1 = { msg, isLong ->
            MusicScheduler.tick()
            runOnUiThread {
                Toast.makeText(
                    this,
                    msg,
                    if (isLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
                ).show()
            }
        }
        staticToast2 = { msg, isLong ->
            MusicScheduler.tick()
            runOnUiThread {
                Toast.makeText(
                    this,
                    msg,
                    if (isLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
                ).show()
            }
        }
        invalidate = {
            MusicScheduler.tick()

            combiner?.invalidateSearch()
            unlocked?.invalidateSearch()

            combiner?.postInvalidate()
            unlocked?.postInvalidate()
            treeView?.hasTree = false
            treeView?.postInvalidate()
            mandalaView?.isInvalidated = true
            mandalaView?.postInvalidate()

        }

        pref = BetterPreferences(getPreferences(Context.MODE_PRIVATE))
        edit = {
            pref.edit()
        }

        addClickListeners()
        loadEverythingFromPreferences()

        MusicScheduler.tick()

    }

    private fun addClickListeners() {
        startButton?.setOnClickListener { FlipperContent.GAME.bind(this) }
        treeViewButton?.setOnClickListener { FlipperContent.TREE.bind(this) }
        graphViewButton?.setOnClickListener { FlipperContent.GRAPH.bind(this) }
        suggestButton?.setOnClickListener {
            combiner?.invalidateSearch()
            FlipperContent.COMBINER.bind(this)
        }
        mandalaViewButton?.setOnClickListener {
            FlipperContent.MANDALA.bind(this)
        }
        back1?.setOnClickListener { FlipperContent.MENU.bind(this) }
        back2?.setOnClickListener { FlipperContent.MENU.bind(this) }
        backArrow3?.setOnClickListener { FlipperContent.MENU.bind(this) }
        backArrow4?.setOnClickListener { FlipperContent.MENU.bind(this) }
        backArrow5?.setOnClickListener { FlipperContent.MENU.bind(this) }
        addSearchListeners(back3, backArrow1, searchButton1, search1, unlocked)
        addSearchListeners(back1, backArrow2, searchButton2, search2, combiner)
        randomButton?.setOnClickListener { RandomSuggestion.make(this) }
    }

    fun loadEverythingFromPreferences() {

        askFrequency = AskFrequencyOption[pref]
        backgroundMusicVolume = pref.getFloat("backgroundMusicVolume", 1f)
        showCraftingCounts = pref.getBoolean("showCraftingCounts", true)
        showElementUUID = pref.getBoolean("showElementUUID", true)
        offlineMode = pref.getBoolean("offlineMode", false)

        println("offline mode? $offlineMode")

        WebServices.serverInstance = pref.getInt("serverInstance", 0)
        WebServices.serverName = pref.getString("serverName", "Default")!!

        SettingsInit.init(this)

        saveElement = { edit, element ->
            MusicScheduler.tick()
            val id = element.uuid
            val value = StringBuilder()
            value.append(element.name)
            value.append(';')
            value.append(element.group.toString())
            value.append(';')
            value.append(element.craftingCount.toString())
            val unlockedRecipes = recipesByElement[element]
            val unlocked = id in unlockedIds.keys || unlockedRecipes?.isNotEmpty() == true
            if (unlocked) {
                value.append(';')
                value.append('1')
                if (unlockedRecipes?.isEmpty() == false) {
                    value.append(';')
                    for (index in unlockedRecipes.indices) {
                        val (a, b) = unlockedRecipes[index]
                        value.append(a.uuid.toString())
                        value.append('-')
                        value.append(b.uuid.toString())
                        if (index + 1 < unlockedRecipes.size) value.append(',')
                    }
                }
            }
            edit.putString(id.toString(), value.toString())
            println("saved $id/$value")
        }

        saveElement2 = { element ->
            MusicScheduler.tick()
            synchronized(Unit) {
                val edit = pref.edit()
                saveElement(edit, element)
                edit.apply()
            }
        }

        saveFavourites = {
            MusicScheduler.tick()
            val edit = pref.edit()
            edit.putInt("favourites.length", FAVOURITE_COUNT)
            for ((i, favourite) in favourites.withIndex()) {
                if (favourite == null) {
                    edit.remove("favourites[$i]")
                } else {
                    edit.putInt("favourites[$i]", favourite.uuid)
                }
            }
            edit.apply()
        }

        readUnlockedElementsLegacy()

        registerBaseElements(pref)

        readUnlockedElements()

        loadOfflineElements(this, pref)

        createUniqueIdentifier()

        FAVOURITE_COUNT = pref.getInt("favourites.length", FAVOURITE_COUNT)
        resizeFavourites(pref)

        invalidate()

        updateGroupSizesAndNames()

        askNews()

        val edit2 = pref.edit()
        for ((key, value) in pref.all) {
            if (key.startsWith("recipe.") && (value is Int || value.toString()
                    .toIntOrNull() != null)
            ) {
                val parts = key.split('.')
                val a = elementById[parts[1].toIntOrNull() ?: continue] ?: continue
                val b = elementById[parts[2].toIntOrNull() ?: continue] ?: continue
                val c =
                    elementById[if (value is Int) value else value.toString().toInt()] ?: continue
                addRecipe(a, b, c, this)
                edit2.remove(key)
            }
        }
        edit2.apply()

        diamondViews.clear()

        val flipper = flipper
        if (flipper != null) for (child in flipper.children) {
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

        CombinationCache.init(pref)

        SaveLoadLogic.init(this)

        // todo everything is saved, isn't it?
        // todo then clear the cache :D

    }

    private fun readUnlockedElementsLegacy() {
        val unlockedIdsString = pref.getString("unlocked", null)
        if (unlockedIdsString != null) {
            unlockedIds.addAll(unlockedIdsString
                .split(',')
                .mapNotNull { x -> x.toIntOrNull() })
        } else unlockedIds.addAll(listOf(1, 2, 3, 4))
    }

    private fun readUnlockedElements() {
        val reader = SplitReader2()
        val recipeMemory = HashMap<Element, IntArrayList>()
        for ((key, value) in pref.all) {
            val id = key.toIntOrNull()
            if (id != null && value != null) {
                val valueStr = value.toString()
                if (valueStr.indexOf(';') != valueStr.lastIndexOf(';')) {
                    reader.input = valueStr
                    val name = reader.readString(';', ';', "")
                    val group = reader.readInt(';', ';', -1)
                    // println("parsing $valueStr for id $id -> '$name', $group")
                    if (group < 0) continue
                    val craftCount = reader.readInt(';', ';', -1)
                    val wasCrafted = reader.readInt(';', ';', 0) > 0
                    val element = Element.get(name, id, group, craftCount, false)
                    if (wasCrafted) {
                        // println("added element $name/$group/$craftCount/$wasCrafted")
                        unlockedIds.put(id)
                        val list = unlockedElements[group]
                        synchronized(list) {
                            list.add(element)
                        }
                    }
                    if (reader.hasRemaining) {
                        val list = IntArrayList()
                        recipeMemory[element] = list
                        // still not ideal, but better than previously
                        while (reader.hasRemaining) {
                            val a = reader.readInt(',', '-', -1)
                            val b = reader.readInt(',', '-', -1)
                            if (a != -1 && b != -1) list.addPair(a, b)
                            reader.next()
                        }
                    }
                }
            }
            /** legacy removal */
            if (key.endsWith(".name")) {
                // an element
                // reading could be optimized, but it's legacy, so doesn't matter
                val edit = pref.edit()
                val id1 = key.split('.')[0].toIntOrNull() ?: continue
                val name = value.toString()
                val group = pref.getInt("$id1.group", -1)
                if (group < 0) continue
                val craftingCount = pref.getInt("$id1.crafted", -1)
                val element = Element.get(name, id1, group, craftingCount, false)
                saveElement(edit, element)
                edit.remove("$id1.name")
                edit.remove("$id1.group")
                edit.remove("$id1.crafted")
                edit.apply()
            }
        }
        for ((element, abs) in recipeMemory) {
            abs.forEachPair { a, b ->
                val ea = elementById[a]
                val eb = elementById[b]
                if (ea != null && eb != null) {
                    addRecipe(ea, eb, element, this, false)
                }
            }
        }
    }

    private fun createUniqueIdentifier() {
        // to potentially block a user, or at least for filtering of his actions
        var ci = pref.getLong("customUUID", -1)
        if (ci < 0) {
            ci = abs(Random().nextLong())
            pref.edit().putLong("customUUID", ci).apply()
        }
        customUUID = ci
    }

    fun getDiamondCount(): Int {
        // split for statistics + no-loss, when everything is reset
        var count = elementByRecipe.size
        count += 3 * unlockedIds.size
        count += pref.getInt(diamondWatchKey, 0)
        count += pref.getInt(diamondBuyKey, 0)
        count -= pref.getInt(diamondSpentKey, 0)
        return count
    }

    fun spendDiamonds(count: Int): Boolean {
        val current = getDiamondCount()
        val hasEnough = count < 0 || current >= count
        if (hasEnough) {
            successSound.play()
            pref.edit().putInt(diamondSpentKey, pref.getInt(diamondSpentKey, 0) + count).apply()
            updateDiamondCount()
        } else {
            RecipeHelper.offerSpendingMoneyOnDiamondsOrWatchAds(this)
        }
        return hasEnough
    }

    fun updateDiamondCount() {
        runOnUiThread {
            val count = getDiamondCount()
            for (view in diamondViews) {
                view.text = count.toString()
            }
        }
    }

    fun resizeFavourites(pref: SharedPreferences) {
        favourites = Array(FAVOURITE_COUNT) { favourites.getOrNull(it) }
        for (i in 0 until FAVOURITE_COUNT) {
            favourites[i] =
                favourites.getOrNull(i) ?: elementById[pref.getInt("favourites[$i]", -1)]
        }
    }

    private fun addSearchListeners(
        back: View?,
        backArrow: View?,
        searchButton: View?,
        search: TextView?,
        unlocked: UnlockedRows?
    ) {
        back?.setOnClickListener { flipper?.displayedChild = 0 }
        backArrow?.setOnClickListener { flipper?.displayedChild = 0 }
        val diamondView = (back?.parent as? View)?.findViewById<View>(R.id.diamonds)
        searchButton?.setOnClickListener {
            if (back?.visibility == VISIBLE) {
                back.visibility = GONE
                diamondView?.visibility = GONE
                search?.visibility = VISIBLE
            } else {
                back?.visibility = VISIBLE
                diamondView?.visibility = VISIBLE
                search?.visibility = GONE
            }
        }
        search?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                unlocked?.search = s.toString()
                unlocked?.invalidateSearch()
            }

            override fun afterTextChanged(s: Editable?) {
                unlocked?.search = s.toString()
                unlocked?.invalidateSearch()
            }
        })
    }

    private fun updateGroupSizesAndNames() {
        thread {
            WebServices.updateGroupSizesAndNames()
        }
    }

    fun askNews() {
        if (newsView != null) thread(name = "NewsThread") {
            WebServices.askNews(20, {
                newsView?.news = it
                newsView?.postInvalidate()
            }, if (offlineMode) {
                { /* can be ignored */ }
            } else ServerService.defaultOnError)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val flipper = flipper
        if (flipper != null && flipper.displayedChild > 0) {
            flipper.displayedChild = 0
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        Sound.destroyAll()
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        MusicScheduler.pause()
    }

    override fun onResume() {
        super.onResume()
        MusicScheduler.unpause()
        goFullScreen()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            SaveLoadLogic.WRITE_EXT_STORAGE_CODE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    SaveLoadLogic.onWriteAllowed?.invoke()
                }
            }
        }
    }

}
