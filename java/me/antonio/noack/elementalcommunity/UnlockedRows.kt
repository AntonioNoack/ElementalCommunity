package me.antonio.noack.elementalcommunity

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.math.MathUtils.clamp
import me.antonio.noack.elementalcommunity.AllManager.Companion.FAVOURITE_COUNT
import me.antonio.noack.elementalcommunity.AllManager.Companion.addRecipe
import me.antonio.noack.elementalcommunity.AllManager.Companion.save
import me.antonio.noack.elementalcommunity.AllManager.Companion.staticRunOnUIThread
import me.antonio.noack.elementalcommunity.AllManager.Companion.unlockedIds
import me.antonio.noack.elementalcommunity.GroupsEtc.GroupSizes
import me.antonio.noack.elementalcommunity.GroupsEtc.drawElement
import me.antonio.noack.elementalcommunity.GroupsEtc.drawFavourites
import me.antonio.noack.elementalcommunity.GroupsEtc.getMargin
import me.antonio.noack.elementalcommunity.utils.Maths.fract
import me.antonio.noack.elementalcommunity.utils.Maths.mix
import java.util.*
import kotlin.math.*

// show things that might soon be available (if much more players)

// todo create a list of top-most-used/favourite elements

open class UnlockedRows(ctx: Context, attributeSet: AttributeSet?): View(ctx, attributeSet) {

    var allowLeftFavourites = false

    lateinit var all: AllManager

    var search = ""
    var lastSearch = ""
    var lastParts = -1
    val relativeRightBorder = 0.7f

    fun invalidateSearch(){
        search = search.toLowerCase().trim()
        if(search.isEmpty()){
            synchronized(Unit){
                for((group, unlocked) in unlockeds.withIndex()){
                    val list = shownSymbols[group]
                    list.clear()
                    list.addAll(unlocked)
                }
            }

            lastSearch = search
            lastParts = 0

        } else {

            val parts = search.split(',').map { it.trim() }
            synchronized(Unit){
                for((group, unlocked) in
                (if(search.startsWith(lastSearch) && lastParts == parts.size) shownSymbols else unlockeds).withIndex()){
                    val list = shownSymbols[group]
                    val filtered = unlocked.filter {
                        val name = it.lcName
                        for(part in parts){
                            if(name.contains(part)){
                                return@filter true
                            }
                        }
                        false
                    }
                    if(list != filtered){
                        list.clear()
                        list.addAll(filtered)
                    }
                }
            }

            lastSearch = search
            lastParts = parts.size

        }

        checkScroll()
        postInvalidate()
    }

    var unlockeds = AllManager.unlockeds
    val shownSymbols = Array(unlockeds.size){ TreeSet<Element>() }

    private var entriesPerRow = 5

    var scroll = 0f
    var scrollDest = 0f

    private var dragged: Element? = null
    private var activeElement: Element? = null
    private var activeness = 0f

    private var mx = 0f
    private var my = 0f

    private var isOnBorderY = 0

    private var zoom = 1f

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        entriesPerRow = (if(measuredWidth > measuredHeight) 10 * zoom else 5 * zoom).toInt()
    }

    fun widthPerNode(measuredWidth: Int): Float {
        val width = measuredWidth * 1f
        val avgMargin = getMargin(width / entriesPerRow)
        return (width - 2 * avgMargin) / (entriesPerRow + relativeRightBorder)
    }

    fun widthPerNode(width: Float = this.measuredWidth * 1f): Float {
        val avgMargin = getMargin(width / entriesPerRow)
        return (width - 2 * avgMargin) / (entriesPerRow + relativeRightBorder)
    }

    fun widthPerNodeNMargin(width: Float = this.measuredWidth * 1f): Pair<Float, Float> {
        val avgMargin = getMargin(width / entriesPerRow)
        return (width - 2 * avgMargin) / (entriesPerRow + relativeRightBorder) to avgMargin
    }

    fun validXY(event: MotionEvent, searchIfNull: Boolean): Triple<AreaType, Int, Int> {

        val measuredWidth = measuredWidth
        val measuredHeight = measuredHeight
        val width = measuredWidth.toFloat()
        val height = measuredHeight.toFloat()

        var intX: Float
        val intY: Float


        val entriesPerRow = entriesPerRow
        val scroll = scroll
        val widthPerNode = widthPerNode()

        intX = (event.x) / widthPerNode
        intY = (event.y + scroll) / widthPerNode

        var isSpecial = false
        if(FAVOURITE_COUNT > 0){
            if(width > height && allowLeftFavourites){
                // more width -> favourites at left
                val favSize = min(height / FAVOURITE_COUNT, width * AllManager.MAX_FAVOURITES_RATIO)
                if(event.x < favSize){
                    val maybeX = event.y / favSize
                    if(searchIfNull && AllManager.favourites.getOrNull(maybeX.toInt()) == null){
                    } else {
                        isSpecial = true
                        intX = maybeX
                    }
                }
            } else {
                // more height -> favourites at bottom
                val favSize = min(width / FAVOURITE_COUNT, height * AllManager.MAX_FAVOURITES_RATIO)
                if(event.y > height - favSize){
                    val maybeX = event.x / favSize
                    if(searchIfNull && AllManager.favourites.getOrNull(maybeX.toInt()) == null){
                    } else {
                        isSpecial = true
                        intX = maybeX
                    }
                }
            }
        }

        val fraX = fract(intX)
        val fraY = fract(intY)
        val valid = sq(fraX - 0.5f) + sq(fraY - 0.5f) < 0.15f // < fraX in 0.18f .. 0.82f && fraY in 0.18f .. 0.82f
        val internalX = intX.toInt()
        val internalY = intY.toInt()

        return Triple(if(isSpecial) if(fraX > 0.5f) AreaType.FAVOURITES_TOP else AreaType.FAVOURITES_BOTTOM else if(valid){
             AreaType.ELEMENTS
        } else AreaType.IGNORE, internalX, internalY)

    }

    fun sq(f: Float): Float = f*f

    fun setOnBorder(event: MotionEvent){
        // val x = event.x
        val offset = 0.08f * min(measuredWidth, measuredHeight)
        val y = event.y
        isOnBorderY = when {
            y < offset -> -1
            y > measuredHeight - offset -> +1
            else -> 0
        }
    }

    init {

        val scrollListener = GestureDetector(object: GestureDetector.OnGestureListener {
            override fun onShowPress(e: MotionEvent?) {}
            override fun onDown(event: MotionEvent?): Boolean {
                return if(event != null){

                    val (valid, internalX, internalY) = validXY(event, true)

                    dragged = when(valid){
                        AreaType.ELEMENTS -> getElementAt(internalX, internalY)
                        AreaType.FAVOURITES_TOP,
                        AreaType.FAVOURITES_BOTTOM -> AllManager.favourites[internalX]
                        AreaType.IGNORE -> null
                    }

                    setOnBorder(event)

                    false

                } else false
            }
            override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean = false
            override fun onScroll(event: MotionEvent?, e2: MotionEvent?, distanceX: Float, dy: Float): Boolean {

                if(dragged == null){

                    val widthPerNode = widthPerNode()
                    if(dy != 0f && dy < widthPerNode * 0.7f){
                        scroll += dy
                    }

                }

                scrollDest = Float.NaN

                checkScroll()
                invalidate()

                return true

            }
            override fun onLongPress(e: MotionEvent?) {}
            override fun onSingleTapUp(event: MotionEvent?): Boolean = false
        })

        val zoomListener = ScaleGestureDetector(context, object: ScaleGestureDetector.OnScaleGestureListener {
            override fun onScale(detector: ScaleGestureDetector?): Boolean {
                return if(detector != null && detector.scaleFactor != 1f){
                    zoom /= detector.scaleFactor
                    val newEntriesPerRow = max(3, (if(measuredWidth > measuredHeight) 10 * zoom else 5 * zoom).toInt())
                    if(newEntriesPerRow != entriesPerRow){
                        dragged = null
                        entriesPerRow = newEntriesPerRow
                    }
                    invalidate()
                    true
                } else false
            }
            override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean = true
            override fun onScaleEnd(detector: ScaleGestureDetector?) {}
        })

        setOnTouchListener { _, event ->

            mx = event.x
            my = event.y

            scrollListener.onTouchEvent(event)
            zoomListener.onTouchEvent(event)

            isOnBorderY = when {
                my < measuredWidth * 0.12f -> -1
                my > measuredHeight - measuredWidth * 0.12f -> +1
                else -> 0
            }

            when(event.actionMasked){
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {

                    if(dragged != null){
                        val first = dragged!!
                        dragged = null
                        val (valid, internalX, internalY) = validXY(event, false)
                        val second = when(valid){
                            AreaType.ELEMENTS -> getElementAt(internalX, internalY)
                            AreaType.FAVOURITES_TOP,
                            AreaType.FAVOURITES_BOTTOM -> {
                                if(internalX in 0 until FAVOURITE_COUNT){
                                    // set it here
                                    AllManager.favourites[internalX] = first
                                    AllManager.save()
                                    AllManager.clickSound.play()
                                    invalidate()
                                    null
                                } else getElementAt(internalX, internalY)
                            }
                            AreaType.IGNORE -> null
                        }
                        if(second != null){
                            onRecipeRequest(first, second)
                        }
                    }

                    isOnBorderY = 0

                }
            }

            checkScroll()

            invalidate()

            true

        }

    }

    fun unlockElement(sa: Element, sb: Element, element: Element){
        //  add to achieved :D
        val newOne = add(sa, sb, element)
        synchronized(Unit){
            unlockedIds.add(element.uuid)
            activeElement = element
            activeness = 1f
            save()
        }
        // scroll to destination on success
        val height = measuredHeight
        val needed = neededHeight()
        scrollDest = clamp((getRow(element, true) + 0.5f)/countRows() * needed - height * 0.5f, 0f, max(0f, needed - height))
        staticRunOnUIThread {
            invalidate()
            (if(newOne) AllManager.successSound else AllManager.okSound).play()
        }
    }

    open fun onRecipeRequest(first: Element, second: Element){
        BasicOperations.onRecipeRequest(first, second, all, measuredWidth, measuredHeight, {
            unlockElement(first, second, it)
        }, {
            add(first, second, it)
        })
    }

    fun checkScroll(){
        var sum = 0
        synchronized(Unit){
            shownSymbols.forEach { unlocked ->
                sum += (unlocked.size + entriesPerRow - 1) / entriesPerRow
            }
        }
        val (widthPerNode, avgMargin) = widthPerNodeNMargin()
        val favCount = FAVOURITE_COUNT
        val maxSize = sum * widthPerNode + avgMargin + (if((measuredHeight >= measuredWidth || !allowLeftFavourites) && favCount > 0) measuredWidth / favCount else 0)
        scroll = clamp(scroll,0f, 1f * max(0f, maxSize - measuredHeight))
    }

    private fun getElementAt(x: Int, y: Int): Element? {
        if(x >= entriesPerRow) return null
        var sum = 0
        synchronized(Unit){
            shownSymbols.forEach { unlocked ->
                val delta = (unlocked.size + entriesPerRow - 1) / entriesPerRow
                if(delta > 0 && y-sum in 0 until delta){
                    val indexHere = x + entriesPerRow * (y - sum)
                    return unlocked.getOrNull(indexHere)
                }
                sum += delta
            }
        }
        return null
    }

    fun <V: Comparable<V>> TreeSet<V>.getOrNull(index: Int): V? {
        return this.elementAtOrNull(index)
    }

    private fun getRow(element: Element, forceInAll: Boolean): Int {
        var sum = 0
        if(forceInAll){
            search = ""
            invalidateSearch()
        }
        synchronized(Unit){
            shownSymbols.forEachIndexed { index, unlocked ->
                val delta = (unlocked.size + entriesPerRow - 1) / entriesPerRow
                if(element.group == index){
                    return sum + unlocked.indexOf(element) / entriesPerRow
                }
                sum += delta
            }
        }
        return 0
    }

    private fun countRows(): Int {
        var sum = 0
        synchronized(Unit){
            shownSymbols.forEach { unlocked ->
                val delta = (unlocked.size + entriesPerRow - 1) / entriesPerRow
                sum += delta
            }
        }
        return sum
    }

    fun add(sa: Element, sb: Element, element: Element): Boolean {
        addRecipe(sa, sb, element)
        val unlocked = unlockeds[element.group]
        return if(!unlocked.contains(element) && element.uuid > -1){
            unlocked.add(element)
            // unlocked.sortBy { it.uuid }
            invalidateSearch()
            postInvalidate()
            true
        } else false
    }

    private fun neededHeight(): Float {
        val (widthPerNode, avgMargin) = widthPerNodeNMargin()
        return 2 * avgMargin + widthPerNode * countRows() + (if(measuredHeight > measuredWidth) measuredWidth / FAVOURITE_COUNT else 0)
    }

    private var lastTime = 0L

    private val bgPaint = Paint()
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    init {textPaint.textAlign = Paint.Align.CENTER }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if(canvas == null) return

        GroupsEtc.tick()

        val thisTime = System.nanoTime()
        val deltaTime = clamp((thisTime - lastTime).toInt(), 0, 250000000) * 1e-9f
        lastTime = thisTime

        val (widthPerNode, avgMargin) = widthPerNodeNMargin()
        val width = measuredWidth.toFloat()
        val height = measuredHeight.toFloat()

        var y0 = avgMargin - scroll - widthPerNode
        val entriesPerRow = entriesPerRow
        val activeElement = activeElement
        val activeness = activeness
        val textPaint = textPaint
        val bgPaint = bgPaint

        textPaint.textSize = widthPerNode * 0.13f
        textPaint.textAlign = Paint.Align.CENTER

        val showCraftingCounts = AllManager.showCraftingCounts

        shownSymbols.forEachIndexed { group, unlocked ->

            if(unlocked.isNotEmpty()){

                val rows = (unlocked.size + entriesPerRow - 1) / entriesPerRow
                val endY0 = y0 + widthPerNode * rows

                if(endY0 > -widthPerNode && y0 < height){

                    synchronized(Unit){
                        for((index, element) in unlocked.withIndex()){

                            if(index % entriesPerRow == 0) y0 += widthPerNode
                            if(y0 >= height) break
                            if(y0 < -widthPerNode) continue

                            val x0 = avgMargin + (index % entriesPerRow) * widthPerNode
                            if(activeElement == element && activeness > 0f){

                                val delta = activeness * widthPerNode * 0.5f
                                drawElement(canvas, showCraftingCounts, x0, y0, delta, widthPerNode, true, element, bgPaint, textPaint)

                            } else {

                                drawElement(canvas, showCraftingCounts, x0, y0, 0f, widthPerNode, true, element, bgPaint, textPaint)

                            }
                        }
                    }

                    textPaint.color = 0xff000000.toInt()
                    textPaint.textSize = widthPerNode * 0.13f

                    val text = "${unlocked.size}/${GroupSizes[group]}"
                    canvas.drawText(text, width - textPaint.measureText(text), y0 + widthPerNode*2/3 - (textPaint.ascent() + textPaint.descent()), textPaint)

                }

                y0 = endY0

            }
        }

        drawFavourites(canvas, false, width, height, bgPaint, textPaint, allowLeftFavourites)

        val dragged = dragged
        if(dragged != null){
            drawElement(canvas, false, mx - widthPerNode/2, my - widthPerNode/2, 0f, widthPerNode, true, dragged, bgPaint, textPaint) }

        if(activeness > 0f){

            val x = abs(scroll - scrollDest) * 20 / min(width, height)
            this.activeness = max(this.activeness - deltaTime, min(1f, if(scrollDest.isNaN()) -1f else ln(x) * 50 / widthPerNode))
            invalidate()

        }

        if(!scrollDest.isNaN()){

            scroll = mix(scroll, scrollDest, clamp(3f * deltaTime, 0f, 1f))
            when {
                scroll.isNaN() -> {
                    scrollDest = Float.NaN
                    scroll = 0f
                }
                abs(scroll - scrollDest) < widthPerNode * 0.05f -> {
                    scrollDest = Float.NaN
                }
                else -> {
                    invalidate()
                }
            }

        }

        if(isOnBorderY != 0 && dragged != null){

            val oldScroll = scroll
            scroll += isOnBorderY * deltaTime * widthPerNode * 5f

            checkScroll()

            if(oldScroll == scroll){
                isOnBorderY = 0
            } else {
                invalidate()
            }
        }

    }

    init {
        invalidateSearch()
    }

}