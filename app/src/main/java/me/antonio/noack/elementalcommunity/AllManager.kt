package me.antonio.noack.elementalcommunity

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.View.GONE
import android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
import android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
import android.view.View.SYSTEM_UI_FLAG_IMMERSIVE
import android.view.View.VISIBLE
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import android.widget.ViewFlipper
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import me.antonio.noack.elementalcommunity.itempedia.ItempediaAdapter
import me.antonio.noack.elementalcommunity.itempedia.ItempediaPageLoader.createItempediaPages
import me.antonio.noack.elementalcommunity.itempedia.ItempediaPageLoader.loadNumPages
import me.antonio.noack.elementalcommunity.itempedia.ItempediaSearch
import me.antonio.noack.elementalcommunity.itempedia.ItempediaSwipeDetector
import me.antonio.noack.elementalcommunity.mandala.MandalaView
import me.antonio.noack.elementalcommunity.tree.TreeView
import me.antonio.noack.elementalcommunity.utils.IntArrayList
import me.antonio.noack.webdroid.Captcha
import me.antonio.noack.webdroid.files.FileChooser
import me.antonio.noack.webdroid.files.FileSaver
import java.util.Random
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread
import kotlin.math.abs

// Sounds:
// magic: https://freesound.org/people/suntemple/sounds/241809/
// ok: https://freesound.org/people/grunz/sounds/109663/
// click: https://freesound.org/people/vitriolix/sounds/706/

class AllManager : AppCompatActivity() {

    companion object {

        var chosenStyle = Style.defaultStyle

        fun applyStyle(dialog: AlertDialog) {
            Style.applyStyle(dialog, Style.defaultStyle, chosenStyle)
        }

        var customUUID = 0L
        var showCraftingCounts = true
        var showElementUUID = true
        var offlineMode = false

        val unlockedIds = ConcurrentHashSet<Int>(512)

        init {
            for (i in 1..4) {
                unlockedIds.put(i)
            }
        }

        val elementById = ConcurrentHashMap<Int, Element>(512)

        val elementsByGroup = Array(GroupColors.size + 12) {
            ConcurrentTreeSet<Element>()
        }

        val unlockedElements = Array(GroupColors.size + 12) {
            ConcurrentTreeSet<Element>()
        }

        const val MAX_FAVOURITES_RATIO = 0.15f
        var FAVOURITE_COUNT = 5
        var favourites = arrayOfNulls<Element>(FAVOURITE_COUNT)

        val elementByRecipe = ConcurrentHashMap<Pair<Element, Element>, Element>(2048)
        val recipesByElement = ConcurrentHashMap<Element, MutableList<Pair<Element, Element>>>(2048)

        val elementByName = ConcurrentHashMap<String, Element>(2048)

        fun registerBaseElements(pref: SharedPreferences?) {
            // how TF is this taking 100ms????
            val names = arrayOf("Earth", "Air", "Water", "Fire")
            val groups = intArrayOf(5, 12, 20, 4)
            for (index in 0 until 4) {// after a restart, this should be only 4
                val id = index + 1
                val defName = names[index]
                val defGroup = groups[index]
                val name = pref?.getString("$id.name", defName) ?: defName
                val group = pref?.getInt("$id.group", defGroup) ?: defGroup
                val craftingCount = pref?.getInt("$id.crafted", -1) ?: -1
                val element = elementById.getOrPut(id) { Element(name, id, group, craftingCount) }
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
            if (save) {
                invalidate()
                all?.updateDiamondCount()
                saveElement2(r)
            }
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

        var successSound: Sound? = null
        var okSound: Sound? = null
        var clickSound: Sound? = null
        var backgroundMusic: List<Sound> = emptyList()

        var askFrequency = AskFrequencyOption.ALWAYS
        var backgroundMusicVolume = 1f

        const val diamondBuyKey = "diamondCountBought"
        const val diamondWatchKey = "diamondCountWatched"
        const val diamondSpentKey = "diamondCountSpent"

        fun checkIsUIThread() {
            if (Looper.getMainLooper().thread != Thread.currentThread()) {
                throw IllegalStateException("Must execute on main thread!")
            }
        }

    }

    lateinit var pref: SharedPreferences
    val combiner: Combiner? get() = findViewById(R.id.combiner)
    val unlocked: UnlockedRows? get() = findViewById(R.id.unlocked)
    val flipper: ViewFlipper? get() = findViewById(R.id.flipper)

    private val startButton: View? get() = findViewById(R.id.start)
    private val treeViewButton: View? get() = findViewById(R.id.treeButton)
    private val graphViewButton: View? get() = findViewById(R.id.graphButton)
    private val mandalaViewButton: View? get() = findViewById(R.id.mandalaButton)
    private val itempediaViewButton: View? get() = findViewById(R.id.itempediaButton)
    private val suggestButton: View? get() = findViewById(R.id.suggest)
    val settingButton: View? get() = findViewById(R.id.settingsButton)

    private val back1: View? get() = findViewById(R.id.back1)
    private val back2: View? get() = findViewById(R.id.back2)
    private val back3: View? get() = findViewById(R.id.back3)

    private val backArrow1: View? get() = findViewById(R.id.backArrow1)
    private val backArrow2: View? get() = findViewById(R.id.backArrow2)
    private val backArrow3: View? get() = findViewById(R.id.backArrow3)
    private val backArrow4: View? get() = findViewById(R.id.backArrow4)
    private val backArrow5: View? get() = findViewById(R.id.backArrow5)
    private val backArrow6: View? get() = findViewById(R.id.backArrow6)
    val favTitle: TextView? get() = findViewById(R.id.favTitle)
    val favSlider: SeekBar? get() = findViewById(R.id.favSlider)

    private val search1: EditText? get() = findViewById(R.id.search1)
    private val search2: EditText? get() = findViewById(R.id.search2)
    val search3: EditText? get() = findViewById(R.id.search3)

    private val searchButton1: View? get() = findViewById(R.id.searchButton1)
    private val searchButton2: View? get() = findViewById(R.id.searchButton2)
    val searchButton3: View? get() = findViewById(R.id.searchButton3)

    private val randomButton: View? get() = findViewById(R.id.randomButton)
    val resetEverythingButton: View? get() = findViewById(R.id.resetEverythingButton)
    val newsView: NewsView? get() = findViewById(R.id.newsView)
    val freqSlider: SeekBar? get() = findViewById(R.id.frequencySlider)
    val freqTitle: TextView? get() = findViewById(R.id.frequencyTitle)
    val switchServerButton: View? get() = findViewById(R.id.switchServer)
    val craftingCountsSwitch: SwitchCompat? get() = findViewById(R.id.craftingCountsSwitch)
    val displayUUIDSwitch: SwitchCompat? get() = findViewById(R.id.displayUUIDSwitch)
    val offlineModeSwitch: SwitchCompat? get() = findViewById(R.id.offlineModeSwitch)
    val volumeSlider: SeekBar? get() = findViewById(R.id.backgroundVolumeSlider)
    val volumeTitle: TextView? get() = findViewById(R.id.backgroundVolumeTitle)

    val styleDefault: RadioButton? get() = findViewById(R.id.radio_default)
    val styleDark: RadioButton? get() = findViewById(R.id.radio_dark)
    val styleLight: RadioButton? get() = findViewById(R.id.radio_light)
    val styleNeon: RadioButton? get() = findViewById(R.id.radio_neon)

    val treeView: TreeView? get() = findViewById(R.id.tree)
    private val graphView: GraphView? get() = findViewById(R.id.graph)
    val mandalaView: MandalaView? get() = findViewById(R.id.tree2)
    val spaceSlider: SeekBar? get() = findViewById(R.id.spaceSlider)

    private val diamondViews = ArrayList<TextView>()

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            SaveLoadLogic.WRITE_EXT_STORAGE_CODE -> {
                FileSaver.continueSave(this, resultCode, data)
            }
            FileChooser.READ_REQUEST_CODE -> {
                if (resultCode == RESULT_OK) {
                    val uri = data?.data
                    if (uri != null) {
                        contentResolver.openInputStream(uri)?.use {
                            SaveLoadLogic.applyDownload(this, String(it.readBytes()))
                        }
                    }
                }
            }
            FileSaver.WRITE_REQUEST_CODE -> {
                FileSaver.continueSave(this, resultCode, data)
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    @Suppress("DEPRECATION")
    private fun goFullScreen() {
        val flags = SYSTEM_UI_FLAG_IMMERSIVE or
                SYSTEM_UI_FLAG_FULLSCREEN or
                SYSTEM_UI_FLAG_HIDE_NAVIGATION
        unlocked?.systemUiVisibility = flags
        combiner?.systemUiVisibility = flags
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        val clock = Clock()

        super.onCreate(savedInstanceState)
        clock.stop("super.onCreate")

        setContentView(R.layout.all_pages)
        clock.stop("Set Layout")

        findViewById<Button>(R.id.captchaButton)
            .setOnClickListener {
                Captcha.get(this, {
                    Toast.makeText(this, "Success: $it", Toast.LENGTH_SHORT)
                        .show()
                }, {
                    Toast.makeText(this, "Failure :/", Toast.LENGTH_SHORT)
                        .show()
                })
            }

        clock.stop("Init Views")

        actionBar?.hide()

        window.navigationBarColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            resources.getColor(R.color.colorPrimary, theme)
        } else {
            @Suppress("DEPRECATION")
            resources.getColor(R.color.colorPrimary)
        }

        goFullScreen()

        clock.stop("Fullscreen")

        unlocked?.all = this
        combiner?.all = this
        treeView?.all = this
        graphView?.all = this
        mandalaView?.all = this

        thread(name = "Loading Sounds") {
            val clock1 = Clock()
            successSound = Sound(R.raw.magic, this)
            okSound = Sound(R.raw.ok, this)
            clickSound = Sound(R.raw.click, this)
            backgroundMusic = listOf(
                Sound(R.raw.music_aquatic_omniverse, this),
                Sound(R.raw.music_clouds_make2, this),
                Sound(R.raw.music_infinite_elements, this)
            )
            clock1.stop("Sounds Async")
        }

        staticRunOnUIThread = { callback ->
            MusicScheduler.tick()
            runOnUiThread {
                try {
                    callback()
                } catch (e: Exception) {
                    staticToast1(e.message.toString(), true)
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
                    msg, if (isLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
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
        edit = { pref.edit() }

        chosenStyle = when (pref.getString("style", "")) {
            "dark" -> Style.darkStyle
            "light" -> Style.lightStyle
            "neon" -> Style.neonStyle
            else -> Style.defaultStyle
        }
        Style.applyStyle(this, Style.defaultStyle, chosenStyle)
        clock.stop("Styling")

        addClickListeners()

        clock.stop("Added click listeners")

        loadEverythingFromPreferences()

        MusicScheduler.tick()

        clock.total("Total")

    }

    private val lazyItempediaInit = lazy {
        initializeItempedia()
    }

    private fun initializeItempedia() {
        val rec = findViewById<RecyclerView>(R.id.itempediaElements)!!
        rec.setHasFixedSize(true)
        val numColumns = 10 // good number?
        rec.layoutManager = GridLayoutManager(this, numColumns)
        itempediaAdapter = ItempediaAdapter(this)
        rec.adapter = itempediaAdapter
        rec.addOnItemTouchListener(ItempediaSwipeDetector(this))
        loadNumPages(this)
        createItempediaPages(this, 10)
    }

    lateinit var itempediaAdapter: ItempediaAdapter
    var deltaItempediaPage: (Int) -> Unit = {}

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
        itempediaViewButton?.setOnClickListener {
            lazyItempediaInit.value
            FlipperContent.ITEMPEDIA.bind(this)
        }

        for (backArrow in listOf(
            back1, back2,
            backArrow1, backArrow2, backArrow3,
            backArrow4, backArrow5, backArrow6
        )) {
            backArrow?.setOnClickListener { FlipperContent.MENU.bind(this) }
        }

        addSearchListeners(back3, searchButton1, search1, unlocked)
        addSearchListeners(back1, searchButton2, search2, combiner)
        ItempediaSearch.setupSearchButton(this)

        randomButton?.setOnClickListener { RandomSuggestion.make(this) }
    }

    fun loadEverythingFromPreferences() {

        val clock = Clock()

        pref.getInt("0", 0)

        clock.stop("Loading preferences")

        askFrequency = AskFrequencyOption[pref]
        backgroundMusicVolume = pref.getFloat("backgroundMusicVolume", 1f)
        showCraftingCounts = pref.getBoolean("showCraftingCounts", true)
        showElementUUID = pref.getBoolean("showElementUUID", true)
        offlineMode = pref.getBoolean("offlineMode", false)

        WebServices.serverInstance = pref.getInt("serverInstance", 0)
        WebServices.serverName = pref.getString("serverName", "Default")!!

        clock.stop("Getting a few prefs")

        SettingsInit.initMainButton(this)

        clock.stop("Settings.init")

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
        }

        saveElement2 = { element ->
            MusicScheduler.tick()
            val edit = pref.edit()
            saveElement(edit, element)
            edit.apply()
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

        clock.stop("Callbacks")

        readUnlockedElementsLegacy()

        clock.stop("Legacy-Loading")

        registerBaseElements(pref)

        clock.stop("Register base elements")

        readUnlockedElements()

        loadOfflineElements(this, pref)

        clock.stop("Loading offline")

        createUniqueIdentifier()

        FAVOURITE_COUNT = pref.getInt("favourites.length", FAVOURITE_COUNT)
        resizeFavourites(pref)

        invalidate()

        updateGroupSizesAndNames()

        clock.stop("Stuff")

        askNews()

        clock.stop("News")

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
        if (flipper != null) {
            for (ci in 0 until flipper.childCount) {
                val child = flipper.getChildAt(ci)
                val diamondTextView = child.findViewById<TextView>(R.id.diamonds) ?: continue
                diamondViews.add(diamondTextView)
                diamondTextView.setOnClickListener {
                    RecipeHelper.openHelper(this)
                }
                (diamondTextView.parent as? View)?.setOnClickListener {
                    RecipeHelper.openHelper(this)
                }
            }
        }

        clock.stop("Diamond View Stuff")

        updateDiamondCount()

        clock.stop("Diamond Count")

        CombinationCache.init(pref)

        clock.stop("CombinationCache.init")

        SaveLoadLogic.init(this)

        clock.stop("SaveLoadLogic.init")
    }

    private fun readUnlockedElementsLegacy() {
        val unlockedIdsString = pref.getString("unlocked", null)
        if (unlockedIdsString != null) {
            unlockedIds.addAll(
                unlockedIdsString
                    .split(',').mapNotNull { x -> x.toIntOrNull() })
        } else unlockedIds.addAll(listOf(1, 2, 3, 4))
    }

    private fun readUnlockedElements() {
        thread(name = "Reading Unlocked") {
            readUnlockedElements0()
            runOnUiThread(::updateDiamondCount)
        }
    }

    private fun readUnlockedElements0() {
        val clock = Clock()
        val reader = SplitReader2()
        val recipeMemory = HashMap<Element, IntArrayList>(512)
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
                    val element = elementById.getOrPut(id) { Element(name, id, group, craftCount) }
                    if (wasCrafted) {
                        unlockedIds.put(id)
                        unlockedElements[group].add(element)
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
        clock.stop("Read Unlocked: Load All, #${unlockedIds.size}")
        for ((element, abs) in recipeMemory) {
            abs.forEachPair { a, b ->
                val ea = elementById[a]
                val eb = elementById[b]
                if (ea != null && eb != null) {
                    addRecipe(ea, eb, element, this, false)
                }
            }
        }
        clock.stop("Read Unlocked: Adding Recipes, #${recipeMemory.size}")
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
            successSound?.play()
            pref.edit().putInt(diamondSpentKey, pref.getInt(diamondSpentKey, 0) + count).apply()
            updateDiamondCount()
        } else {
            RecipeHelper.offerSpendingMoneyOnDiamondsOrWatchAds(this)
        }
        return hasEnough
    }

    fun updateDiamondCount() {
        val count = getDiamondCount()
        for (view in diamondViews) {
            view.text = count.toString()
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
        searchButton: View?,
        search: TextView?,
        unlocked: UnlockedRows?
    ) {
        back?.setOnClickListener { flipper?.displayedChild = 0 }
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
        WebServices.updateGroupSizesAndNames()
    }

    fun askNews() {
        if (newsView != null) {
            WebServices.askNews(
                20, {
                    newsView?.news = it
                    newsView?.postInvalidate()
                }, if (offlineMode) {
                    { /* can be ignored */ }
                } else ServerService.defaultOnError)
        }
    }

    @Suppress("DEPRECATION")
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
