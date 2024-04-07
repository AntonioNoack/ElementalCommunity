package me.antonio.noack.elementalcommunity

import android.annotation.SuppressLint
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
import me.antonio.noack.elementalcommunity.AllManager.Companion.checkIsUIThread
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

@SuppressLint("ClickableViewAccessibility")
open class UnlockedRows(ctx: Context, attributeSet: AttributeSet?) : View(ctx, attributeSet) {

    private var allowLeftFavourites = false

    lateinit var all: AllManager

    var search = ""
    private var lastSearch = ""
    private var lastParts = -1
    private val relativeRightBorder = 0.7f

    private var searchIsInvalid = true

    fun invalidateSearch() {
        searchIsInvalid = true
    }

    private fun validateSearch() {

        search = search.lowercase(Locale.getDefault()).trim()

        if (search.isEmpty()) {
            for ((group, unlocked) in AllManager.unlockedElements.withIndex()) {
                val list = shownSymbols[group]
                list.clear()
                list.addAll(unlocked)
            }

            lastSearch = search
            lastParts = 0
        } else {
            val parts = search.split(',').map { it.trim() }
            val shownGroups = if (search.startsWith(lastSearch) && lastParts == parts.size)
                shownSymbols else unlockeds
            for ((group, unlocked) in shownGroups.withIndex()) {
                val list = shownSymbols[group]
                val filtered = unlocked.filter {
                    val name = it.compacted
                    for (part in parts) {
                        if (name.contains(part)) {
                            return@filter true
                        }
                    }
                    false
                }
                if (list != filtered) {
                    list.clear()
                    list.addAll(filtered)
                }
            }

            lastSearch = search
            lastParts = parts.size
        }

        clampScroll()
        postInvalidate()
    }

    private val unlockeds = AllManager.unlockedElements
    private val shownSymbols = Array(unlockeds.size) { ConcurrentTreeSet<Element>() }

    private var entriesPerRow = 5

    var scroll = 0f
    var scrollDest: Element? = null

    private var dragged: Element? = null
    private var activeElement: Element? = null
    private var activeness = 0f

    private var mx = 0f
    private var my = 0f

    private var isOnBorderY = 0

    private var zoom = 1f

    private fun calculateEntriesPerRow(): Int {
        return (if (measuredWidth > measuredHeight) 10 * zoom else 5 * zoom).toInt()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        entriesPerRow = calculateEntriesPerRow()
    }

    private fun widthPerNode(width: Float = measuredWidth * 1f): Float {
        val avgMargin = getMargin(width / entriesPerRow)
        return (width - 2 * avgMargin) / (entriesPerRow + relativeRightBorder)
    }

    private fun widthPerNodeNMargin(width: Float = measuredWidth * 1f): Pair<Float, Float> {
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

        val scroll = scroll
        val widthPerNode = widthPerNode()

        intX = (event.x) / widthPerNode
        intY = (event.y + scroll) / widthPerNode

        var isSpecial = false
        if (FAVOURITE_COUNT > 0) {
            if (width > height && allowLeftFavourites) {
                // more width -> favourites at left
                val favSize = min(height / FAVOURITE_COUNT, width * AllManager.MAX_FAVOURITES_RATIO)
                if (event.x < favSize) {
                    val maybeX = event.y / favSize
                    if (!(searchIfNull && AllManager.favourites.getOrNull(maybeX.toInt()) == null)) {
                        isSpecial = true
                        intX = maybeX
                    }
                }
            } else {
                // more height -> favourites at bottom
                val favSize = min(width / FAVOURITE_COUNT, height * AllManager.MAX_FAVOURITES_RATIO)
                if (event.y > height - favSize) {
                    val maybeX = event.x / favSize
                    if (!(searchIfNull && AllManager.favourites.getOrNull(maybeX.toInt()) == null)) {
                        isSpecial = true
                        intX = maybeX
                    }
                }
            }
        }

        val fraX = fract(intX)
        val fraY = fract(intY)
        // < fraX in 0.18f .. 0.82f && fraY in 0.18f .. 0.82f
        val valid = sq(fraX - 0.5f) + sq(fraY - 0.5f) < 0.15f
        val internalX = intX.toInt()
        val internalY = intY.toInt()

        return Triple(
            when {
                isSpecial -> AreaType.FAVOURITES
                valid -> AreaType.ELEMENTS
                else -> AreaType.IGNORE
            }, internalX, internalY
        )
    }

    fun sq(f: Float): Float = f * f

    fun setOnBorder(event: MotionEvent) {
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

        val scrollListener = GestureDetector(ctx, object : GestureDetector.OnGestureListener {
            override fun onShowPress(e: MotionEvent) {}
            override fun onDown(event: MotionEvent): Boolean {
                val (valid, internalX, internalY) = validXY(event, true)

                dragged = when (valid) {
                    AreaType.ELEMENTS -> getElementAt(internalX, internalY)
                    AreaType.FAVOURITES -> AllManager.favourites[internalX]
                    AreaType.IGNORE -> null
                }

                setOnBorder(event)

                return false
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean = false

            override fun onScroll(
                event: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                dy: Float
            ): Boolean {

                if (dragged == null) {
                    val widthPerNode = widthPerNode()
                    if (dy != 0f && dy < widthPerNode * 0.7f) {
                        scroll += dy
                    }
                }

                scrollDest = null

                clampScroll()
                invalidate()

                return true

            }

            override fun onLongPress(e: MotionEvent) {}
            override fun onSingleTapUp(event: MotionEvent): Boolean = false
        })

        val zoomListener =
            ScaleGestureDetector(context, object : ScaleGestureDetector.OnScaleGestureListener {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    return if (detector.scaleFactor != 1f) {
                        val oldNodeHeight = widthPerNode()
                        zoom /= detector.scaleFactor
                        val newEntriesPerRow = max(3, calculateEntriesPerRow())
                        if (newEntriesPerRow != entriesPerRow) {
                            dragged = null
                            entriesPerRow = newEntriesPerRow
                            // get offset, so we don't lose our current positioning
                            // scroll > 0, posY = (scroll + height/2) / oldElementSize
                            // todo we need to count how many rows there are..., I think...
                            val newNodeHeight = widthPerNode()
                            val posY = (scroll + height / 2) / oldNodeHeight
                            scroll = posY * newNodeHeight - height / 2
                            clampScroll()
                        }
                        invalidate()
                        true
                    } else false
                }

                override fun onScaleBegin(detector: ScaleGestureDetector): Boolean = true
                override fun onScaleEnd(detector: ScaleGestureDetector) {}
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

            when (event.actionMasked) {
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {

                    if (dragged != null) {
                        val first = dragged!!
                        dragged = null
                        val (valid, internalX, internalY) = validXY(event, false)
                        val second = when (valid) {
                            AreaType.ELEMENTS -> getElementAt(internalX, internalY)
                            AreaType.FAVOURITES -> {
                                if (internalX in 0 until FAVOURITE_COUNT) {
                                    // set it here
                                    AllManager.favourites[internalX] = first
                                    AllManager.saveFavourites()
                                    AllManager.clickSound?.play()
                                    invalidate()
                                    null
                                } else getElementAt(internalX, internalY)
                            }

                            AreaType.IGNORE -> null
                        }
                        if (second != null) {
                            onRecipeRequest(first, second)
                        }
                    }

                    isOnBorderY = 0

                }
            }

            clampScroll()

            invalidate()

            true

        }

    }

    private fun unlockElement(sa: Element, sb: Element, element: Element) {
        //  add to achieved :D
        val newOne = add(sa, sb, element)
        activeElement = element
        activeness = 1f
        // scroll to destination on success
        scrollDest = element
        invalidate()
        (if (newOne) AllManager.successSound else AllManager.okSound)?.play()
    }

    open fun onRecipeRequest(first: Element, second: Element) {
        BasicOperations.onRecipeRequest(first, second, all, measuredWidth, measuredHeight, {
            unlockElement(first, second, it)
        }, {
            add(first, second, it)
        })
    }

    fun clampScroll() {
        val numRows = shownSymbols.sumOf { unlocked ->
            (unlocked.size + entriesPerRow - 1) / entriesPerRow
        }
        val (nodeHeight, avgMargin) = widthPerNodeNMargin()
        val favCount = FAVOURITE_COUNT
        val hasFavourites =
            (measuredHeight >= measuredWidth || !allowLeftFavourites) && favCount > 0
        val heightOfAllNodes = numRows * nodeHeight
        val totalSizeY =
            heightOfAllNodes + avgMargin + (if (hasFavourites) measuredWidth / favCount else 0)
        scroll = clamp(scroll, 0f, max(0f, totalSizeY - measuredHeight))
    }

    private fun getElementAt(x: Int, y: Int): Element? {
        if (x >= entriesPerRow) return null
        var sum = 0
        for (unlocked in shownSymbols) {
            val delta = (unlocked.size + entriesPerRow - 1) / entriesPerRow
            if (delta > 0 && y - sum in 0 until delta) {
                val indexHere = x + entriesPerRow * (y - sum)
                return unlocked.getOrNull(indexHere)
            }
            sum += delta
        }
        return null
    }

    fun <V : Comparable<V>> TreeSet<V>.getOrNull(index: Int): V? {
        return this.elementAtOrNull(index)
    }

    private fun getRow(element: Element): Int {
        var sum = 0
        search = ""
        invalidateSearch()
        for (index in shownSymbols.indices) {
            val unlocked = shownSymbols[index]
            val delta = (unlocked.size + entriesPerRow - 1) / entriesPerRow
            if (element.group == index) {
                return sum + unlocked.indexOf(element) / entriesPerRow
            }
            sum += delta
        }
        return 0
    }

    private fun countRows(): Int {
        var sum = 0
        for (index in shownSymbols.indices) {
            val unlocked = shownSymbols[index]
            val delta = (unlocked.size + entriesPerRow - 1) / entriesPerRow
            sum += delta
        }
        return sum
    }

    fun add(sa: Element, sb: Element, element: Element): Boolean {
        checkIsUIThread()
        addRecipe(sa, sb, element, all)
        val unlocked = unlockeds[element.group]
        return if (!unlocked.contains(element) && element.uuid > -1) {
            unlocked.add(element)
            // unlocked.sortBy { it.uuid }
            invalidateSearch()
            postInvalidate()
            true
        } else false
    }

    private fun neededHeight(): Float {
        val (widthPerNode, avgMargin) = widthPerNodeNMargin()
        return 2 * avgMargin + widthPerNode * countRows() + (if (measuredHeight > measuredWidth && FAVOURITE_COUNT > 0) measuredWidth / FAVOURITE_COUNT else 0)
    }

    private var lastTime = 0L

    private val bgPaint = Paint()
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        textPaint.textAlign = Paint.Align.CENTER
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (searchIsInvalid) {
            validateSearch()
        }

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
        val showUUIDs = AllManager.showElementUUID

        for (group in shownSymbols.indices) {
            val unlocked = shownSymbols[group]
            if (unlocked.isNotEmpty()) {

                val rows = (unlocked.size + entriesPerRow - 1) / entriesPerRow
                val endY0 = y0 + widthPerNode * rows

                if (endY0 > -widthPerNode && y0 < height) {

                    for ((index, element) in unlocked.withIndex()) {

                        if (index % entriesPerRow == 0) y0 += widthPerNode
                        if (y0 >= height) break
                        if (y0 < -widthPerNode) continue

                        textPaint.alpha = 255
                        bgPaint.alpha = 255

                        val x0 = avgMargin + (index % entriesPerRow) * widthPerNode
                        if (activeElement == element && activeness > 0f) {
                            val delta = activeness * widthPerNode * 0.5f
                            drawElement(
                                canvas,
                                showCraftingCounts,
                                showUUIDs,
                                x0,
                                y0,
                                delta,
                                widthPerNode,
                                true,
                                element,
                                bgPaint,
                                textPaint
                            )
                        } else {
                            drawElement(
                                canvas,
                                showCraftingCounts,
                                showUUIDs,
                                x0,
                                y0,
                                0f,
                                widthPerNode,
                                true,
                                element,
                                bgPaint,
                                textPaint
                            )
                        }
                    }

                    textPaint.color = 0xff000000.toInt()
                    textPaint.textSize = widthPerNode * 0.13f

                    val text = "${unlocked.size}/${GroupSizes[group]}"
                    canvas.drawText(
                        text,
                        width - textPaint.measureText(text),
                        y0 + widthPerNode * 2 / 3 - (textPaint.ascent() + textPaint.descent()),
                        textPaint
                    )

                }

                y0 = endY0

            }
        }

        drawFavourites(
            canvas,
            false,
            showUUIDs,
            width,
            height,
            bgPaint,
            textPaint,
            allowLeftFavourites
        )

        val dragged = dragged
        if (dragged != null) {
            drawElement(
                canvas,
                false,
                showUUIDs,
                mx - widthPerNode / 2,
                my - widthPerNode / 2,
                0f,
                widthPerNode,
                true,
                dragged,
                bgPaint,
                textPaint
            )
        }


        val scrollDestY = scrollDest?.run {
            val needed = neededHeight()
            clamp(
                (getRow(this) + 0.5f) / countRows() * needed - height * 0.5f,
                0f,
                max(0f, needed - height)
            )
        } ?: 0f

        if (activeness > 0f) {
            val distance = abs(scroll - scrollDestY) * 20 / min(width, height)
            this.activeness = max(
                this.activeness - deltaTime,
                min(1f, if (scrollDest == null) -1f else ln(distance) * 50 / widthPerNode)
            )
            invalidate()
        }

        if (scrollDest != null) {
            scroll = mix(scroll, scrollDestY, clamp(3f * deltaTime, 0f, 1f))
            when {
                scroll.isNaN() -> {
                    scrollDest = null
                    scroll = 0f
                }
                abs(scroll - scrollDestY) < widthPerNode * 0.05f -> {
                    scrollDest = null
                }
                else -> invalidate()
            }
        }

        if (isOnBorderY != 0 && dragged != null) {

            val oldScroll = scroll
            scroll += isOnBorderY * deltaTime * widthPerNode * 5f

            clampScroll()

            if (oldScroll == scroll) {
                isOnBorderY = 0
            } else invalidate()
        }
    }

    init {
        invalidateSearch()
    }
}