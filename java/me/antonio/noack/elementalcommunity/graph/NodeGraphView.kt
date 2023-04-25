package me.antonio.noack.elementalcommunity.graph

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
import me.antonio.noack.elementalcommunity.*
import me.antonio.noack.elementalcommunity.AllManager.Companion.addRecipe
import me.antonio.noack.elementalcommunity.GroupsEtc.drawElement
import me.antonio.noack.elementalcommunity.GroupsEtc.drawFavourites
import me.antonio.noack.elementalcommunity.utils.Maths
import kotlin.concurrent.thread
import kotlin.math.*

@SuppressLint("ClickableViewAccessibility")
abstract class NodeGraphView(
    ctx: Context, attributeSet: AttributeSet?,
    val alwaysValid: Boolean,
    val iterativeTree: Boolean
) : View(ctx, attributeSet) {

    var allowLeftFavourites = true

    lateinit var all: AllManager
    val unlockeds get() = AllManager.unlockedElements

    var hasTree = false
    var hasTreeReally = false

    // measured border coordinates for prevention of scrolling further away

    var minXf = 0f
    var maxXf = 0f
    var minYf = 0f
    var maxYf = 0f

    var ctr = 0

    var elementSize = 25f

    abstract fun buildTreeAsync()

    abstract fun buildTreeIteratively()

    private fun buildTree() {
        hasTree = true
        val ctr = ++ctr
        threadOrNot("TreeBuilder") {
            buildTreeAsync()
            if (ctr == this.ctr) {
                this.postInvalidate()
                updateSize()
                hasTreeReally = true
            }
        }
    }

    private fun updateSize() {

        minXf = Float.POSITIVE_INFINITY
        minYf = Float.POSITIVE_INFINITY
        maxXf = Float.NEGATIVE_INFINITY
        maxYf = Float.NEGATIVE_INFINITY

        forAllElements { element ->
            minXf = min(minXf, element.px)
            maxXf = max(maxXf, element.px)
            minYf = min(minYf, element.py)
            maxYf = max(maxYf, element.py)
        }

    }

    fun validXY(event: MotionEvent): Triple<AreaType, Int, Int> {

        val x = event.x - measuredWidth * 0.5f
        val y = event.y - measuredHeight * 0.5f
        var intX = x / elementSize + scrollX

        var isSpecial = false
        if (AllManager.FAVOURITE_COUNT > 0) {
            if (width > height && allowLeftFavourites) {
                // more width -> favourites at left
                val favSize = min(
                    height.toFloat() / AllManager.FAVOURITE_COUNT,
                    width * AllManager.MAX_FAVOURITES_RATIO
                )
                if (event.x < favSize) {
                    val maybeX = event.y / favSize
                    isSpecial = true
                    intX = maybeX
                }
            } else {
                // more height -> favourites at bottom
                val favSize = min(
                    width.toFloat() / AllManager.FAVOURITE_COUNT,
                    height * AllManager.MAX_FAVOURITES_RATIO
                )
                if (event.y > height - favSize) {
                    val maybeX = event.x / favSize
                    isSpecial = true
                    intX = maybeX
                }
            }
        }

        val intY = y / elementSize + scrollY
        val xi = floor(intX).toInt()
        val yi = floor(intY).toInt()
        val valid = alwaysValid || (fractOk(intX) && fractOk(intY))
        return Triple(
            when {
                isSpecial -> AreaType.FAVOURITES
                valid -> AreaType.ELEMENTS
                else -> AreaType.IGNORE
            }, xi, yi
        )
    }

    private fun fractOk(value: Float): Boolean {
        val fract = value - floor(value)
        return fract > 0.15f && fract < 0.85f
    }

    fun getElementAt(x: Float, y: Float): Element? {
        // x = centerX + (element.treeX - scrollX) * widthPerNode
        val centerX = width / 2
        val centerY = height / 2
        val localX = (x - centerX) / elementSize + scrollX
        val localY = (y - centerY) / elementSize + scrollY
        val padding = 20f / elementSize
        var bestDistance = 1.41f + padding
        var bestElement: Element? = null
        forAllElements { element ->
            val dx = element.px - localX
            val dy = element.py - localY
            val distance = dx * dx + dy * dy
            if (distance < bestDistance) {
                bestDistance = distance
                bestElement = element
            }
        }
        return bestElement
    }

    var dragged: Element? = null
    var isOnBorderX = 0
    var isOnBorderY = 0

    var scrollX = 0f
    var scrollY = 0f

    // var scroll = 0f
    var scrollDest: Element? = null

    var mx = 0f
    var my = 0f

    fun checkScroll() {
        scrollX = clamp(scrollX, minXf, maxXf)
        scrollY = clamp(scrollY, minYf, maxYf)
    }

    fun setOnBorder(event: MotionEvent) {
        val x = event.x
        val offset = 0.08f * min(measuredWidth, measuredHeight)
        isOnBorderX = when {
            x < offset -> -1
            x > measuredWidth - offset -> +1
            else -> 0
        }
        val y = event.y
        isOnBorderY = when {
            y < offset -> -1
            y > measuredHeight - offset -> +1
            else -> 0
        }
    }

    init {

        val scrollListener = GestureDetector(context, object : GestureDetector.OnGestureListener {
            override fun onShowPress(e: MotionEvent?) {}
            override fun onDown(event: MotionEvent?): Boolean {
                return if (event != null) {

                    val (valid, internalX, _) = validXY(event)

                    dragged = when (valid) {
                        AreaType.ELEMENTS -> getElementAt(event.x, event.y)
                        AreaType.FAVOURITES -> AllManager.favourites[internalX]
                        AreaType.IGNORE -> null
                    }

                    setOnBorder(event)

                    false

                } else false
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent?,
                velocityX: Float,
                velocityY: Float
            ): Boolean = false

            override fun onScroll(
                event: MotionEvent?,
                e2: MotionEvent?,
                dx: Float,
                dy: Float
            ): Boolean {

                if (dragged == null) {

                    val max = measuredWidth * 0.2f

                    if (dx != 0f && abs(dx) < max) {
                        scrollX += dx / elementSize
                    }
                    if (dy != 0f && abs(dy) < max) {
                        scrollY += dy / elementSize
                    }

                    checkScroll()

                    invalidate()

                }

                scrollDest = null

                return true

            }

            override fun onLongPress(e: MotionEvent?) {}
            override fun onSingleTapUp(event: MotionEvent?): Boolean = false
        })

        val zoomListener =
            ScaleGestureDetector(context, object : ScaleGestureDetector.OnScaleGestureListener {
                override fun onScale(detector: ScaleGestureDetector?): Boolean {
                    return if (detector != null && detector.scaleFactor != 1f) {
                        elementSize = clamp(elementSize * detector.scaleFactor, 10f, 1000f)
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

            setOnBorder(event)

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    if (iterativeTree && dragged == null) {
                        // push all elements
                        forAllElements { element ->
                            val cx = (mx - width / 2) / elementSize + scrollX
                            val cy = (my - height / 2) / elementSize + scrollY
                            val dx = element.px - cx
                            val dy = element.py - cy
                            val sca1 = 10f / (dx * dx + dy * dy + 1f)
                            if (sca1.isFinite()) {
                                element.vx += dx * sca1
                                element.vy += dy * sca1
                            }
                        }
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {

                    val dragged = dragged
                    if (dragged != null) {
                        this.dragged = null
                        val (valid, internalX, _) = validXY(event)
                        val second = when (valid) {
                            AreaType.ELEMENTS -> getElementAt(event.x, event.y)
                            AreaType.FAVOURITES -> {
                                // set it here
                                AllManager.favourites[internalX] = dragged
                                AllManager.saveFavourites()
                                AllManager.clickSound.play()
                                invalidate()
                                null
                            }
                            AreaType.IGNORE -> null
                        }
                        if (second != null) {
                            BasicOperations.onRecipeRequest(
                                dragged, second, all,
                                measuredWidth, measuredHeight,
                                { unlockElement(dragged, second, it) },
                                { add(dragged, second, it) })

                        }
                    }

                    isOnBorderX = 0
                    isOnBorderY = 0

                }
            }

            checkScroll()

            invalidate()

            true

        }

    }

    val linePaint = Paint()
    val bgPaint = Paint()
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        textPaint.textAlign = Paint.Align.CENTER
    }

    // canvas: Canvas, x0: Float, y0: Float, delta: Float, widthPerNode: Float, margin: Boolean, element: Element, bgPaint: Paint, textPaint: Paint

    var lastTime = System.nanoTime()

    inline fun forAllElements(run: (Element) -> Unit) {
        if (unlockeds.all { it.isEmpty() }) {
            AllManager.registerBaseElements(null)
        }
        for (list in unlockeds) {
            synchronized(list) {
                for (element in list) {
                    run(element)
                }
            }
        }
    }

    override fun draw(canvas: Canvas?) {
        super.draw(canvas)
        canvas ?: return

        if (iterativeTree) {
            buildTreeIteratively()
            updateSize()
            invalidate()
        } else {
            buildTree()
        }

        GroupsEtc.tick()

        checkScroll()

        val thisTime = System.nanoTime()
        val deltaTime = clamp((thisTime - lastTime).toInt(), 0, 250000000) * 1e-9f
        lastTime = thisTime

        if (!iterativeTree && !hasTreeReally) return

        val width = measuredWidth.toFloat()
        val height = measuredHeight.toFloat()

        val centerX = width / 2
        val centerY = height / 2

        linePaint.color = 0x30000000
        linePaint.strokeWidth = max(2f, min(width, height) * 0.005f)

        // val line0 = System.nanoTime()
        /*forAllElements { element ->
            if (element.hasTreeOutput) {
                drawElementOutput(canvas, centerX, centerY, element)
            }
        }*/

        // new way
        for ((element, recipes) in AllManager.recipesByElement) {
            for ((srcA, srcB) in recipes) {
                drawArrow(canvas, centerX, centerY, srcA, element)
                if (srcA !== srcB) drawArrow(canvas, centerX, centerY, srcB, element)
            }
        }

        val minX = -centerX / elementSize + scrollX - 1.5f
        val maxX = centerX / elementSize + scrollX + 0.5f
        val minY = -centerY / elementSize + scrollY - 1.5f
        val maxY = centerY / elementSize + scrollY + 0.5f

        val showCraftingCounts = AllManager.showCraftingCounts
        val showUUIDs = AllManager.showElementUUID

        forAllElements { element ->
            // done draw the element
            if (element.px in minX..maxX && element.py in minY..maxY) {
                val x0 = centerX + (element.px - scrollX) * elementSize
                val y0 = centerY + (element.py - scrollY) * elementSize
                bgPaint.alpha = 255
                textPaint.alpha = 255
                linePaint.alpha = 255
                if (activeElement == element && activeness > 0f) {
                    val delta = activeness * elementSize * 0.5f
                    drawElement(
                        canvas, showCraftingCounts, showUUIDs,
                        x0, y0, delta, elementSize,
                        true, element, bgPaint, textPaint
                    )
                } else {
                    drawElement(
                        canvas, showCraftingCounts, showUUIDs,
                        x0, y0, 0f, elementSize,
                        true, element, bgPaint, textPaint
                    )
                }
            }
        }

        bgPaint.alpha = 255
        textPaint.alpha = 255
        linePaint.alpha = 255

        drawFavourites(
            canvas, false, showUUIDs,
            width, height, bgPaint, textPaint,
            allowLeftFavourites
        )

        val dragged = dragged
        if (dragged != null) {
            drawElement(
                canvas, false, showUUIDs,
                mx - elementSize / 2,
                my - elementSize / 2,
                0f, elementSize,
                true, dragged,
                bgPaint, textPaint
            )
        }

        if (activeness > 0f) {
            this.activeness -= deltaTime
            invalidate()
        }

        val scrollDest = scrollDest
        if (scrollDest != null) {

            val dt = clamp(3f * deltaTime, 0f, 1f)

            val scrollDestX = scrollDest.px + 0.5f
            val scrollDestY = scrollDest.py + 0.5f

            scrollX = Maths.mix(scrollX, scrollDestX, dt)
            scrollY = Maths.mix(scrollY, scrollDestY, dt)
            when {
                scrollX.isNaN() -> {
                    this.scrollDest = null
                    scrollX = 0f
                    scrollY = 0f
                }
                sqrt(sq(scrollX - scrollDestX, scrollY - scrollDestY)) < 0.05f -> {
                    scrollX = scrollDestX
                    scrollY = scrollDestY
                    this.scrollDest = null
                }
                else -> invalidate()
            }

        }

        if ((isOnBorderX != 0 || isOnBorderY != 0) && dragged != null) {

            val oldScrollX = scrollX
            val oldScrollY = scrollY

            scrollX += isOnBorderX * deltaTime * 10f
            scrollY += isOnBorderY * deltaTime * 10f

            checkScroll()

            if (oldScrollX == scrollX && oldScrollY == scrollY) {
                isOnBorderX = 0
                isOnBorderY = 0
            } else invalidate()

        }

    }

    fun sq(x: Float, y: Float) = x * x + y * y

    /*fun drawElementOutput(canvas: Canvas, centerX: Float, centerY: Float, src: Element) {
        // y of dst is larger
        val x0 = centerX + (src.px - scrollX + 0.5f) * elementSize
        var y0 = centerY + (src.py - scrollY + 0.5f) * elementSize
        y0 += elementSize * .4f // there is a box anyways ;)
        val y2 = y0 + elementSize * 0.3f
        // val xc = (x0 + x1) / 2
        // val yc = (y1 - widthPerNode * 0.5f)
        // dy
        canvas.drawLine(x0, y0, x0, y2, linePaint)
    }*/

    private fun drawPath(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        src: Element,
        dst: Element
    ) {
        val x0 = centerX + (src.px - scrollX + 0.5f) * elementSize
        val y0 = centerY + (src.py - scrollY + 0.5f) * elementSize
        val x1 = centerX + (dst.px - scrollX + 0.5f) * elementSize
        val y1 = centerY + (dst.py - scrollY + 0.5f) * elementSize
        canvas.drawLine(x0, y0, x1, y1, linePaint)
    }

    private fun drawArrow(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        src: Element,
        dst: Element
    ) {
        val x0 = centerX + (src.px - scrollX + 0.5f) * elementSize
        val y0 = centerY + (src.py - scrollY + 0.5f) * elementSize
        val x1 = centerX + (dst.px - scrollX + 0.5f) * elementSize
        val y1 = centerY + (dst.py - scrollY + 0.5f) * elementSize
        val dx = x1 - x0
        val dy = y1 - y0
        val length = length(dx, dy)
        if (length > 5f) {
            val arrowHeadLength = clamp(length * 0.3f, 0f, elementSize) / length
            if (arrowHeadLength > 5f) {
                val vx = -dy * arrowHeadLength * 0.3f
                val vy = +dx * arrowHeadLength * 0.3f
                val wx = -dx * arrowHeadLength
                val wy = -dy * arrowHeadLength
                // arrow head
                canvas.drawLine(x1, y1, x1 - vx + wx, y1 - vy + wy, linePaint)
                canvas.drawLine(x1, y1, x1 + vx + wx, y1 + vy + wy, linePaint)
            }
            // arrow line
            canvas.drawLine(x0, y0, x1, y1, linePaint)
        }
    }

    fun length(dx: Float, dy: Float) = sqrt(dx * dx + dy * dy)

    private var activeElement: Element? = null
    private var activeness = 0f

    fun add(srcA: Element, srcB: Element, dst: Element): Boolean {
        addRecipe(srcA, srcB, dst, all)
        val unlocked = unlockeds[dst.group]
        synchronized(unlocked) {
            return if (!unlocked.contains(dst) && dst.uuid > -1) {
                if (srcA === srcB) {
                    // find location in close proximity
                    dst.px = srcA.px + (Math.random().toFloat() - 0.5f)
                    dst.py = srcA.py + (Math.random().toFloat() - 0.5f)
                } else {
                    dst.px = (srcA.px + srcB.px) * 0.5f
                    dst.py = (srcA.px + srcB.px) * 0.5f
                }
                unlocked.add(dst)
                // unlocked.sortBy { it.uuid }
                // invalidateSearch()
                postInvalidate()
                true
            } else false
        }
    }

    private fun unlockElement(sa: Element, sb: Element, element: Element) {

        //  add to achieved :D
        val newOne = add(sa, sb, element)
        synchronized(Unit) {
            activeElement = element
            activeness = 1f
        }

        scrollDest = element

        AllManager.staticRunOnUIThread {
            invalidate()
            (if (newOne) AllManager.successSound else AllManager.okSound).play()
        }

    }

    companion object {
        fun threadOrNot(name: String, runnable: () -> Unit) {
            if (true) {
                thread(name = name) { runnable() }
            } else {
                runnable()
            }
        }
    }

}