package me.antonio.noack.elementalcommunity.mandala

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
import kotlin.math.*

@SuppressLint("ClickableViewAccessibility")
class MandalaView(ctx: Context, attributeSet: AttributeSet?) : View(ctx, attributeSet) {

    var allowLeftFavourites = true

    lateinit var all: AllManager
    var unlockeds = AllManager.unlockedElements

    lateinit var tree: Mandala
    var hasTree = false
    var isInvalidated = true

    // measured border coordinates for prevention of scrolling further away

    private fun buildTree(e: Element) {
        tree = Mandala(e, if (hasTree) tree else null)
        hasTree = true
        isInvalidated = false
    }

    var widthPerNode = 100f

    fun validXY(event: MotionEvent, searchIfNull: Boolean = false): Triple<AreaType, Float, Float> {

        val (sx, sy) = getScale()
        val x = event.x - measuredWidth * 0.5f
        val y = event.y - measuredHeight * 0.5f
        var intX = x / sx

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
                    if (searchIfNull && AllManager.favourites.getOrNull(maybeX.toInt()) == null) {
                    } else {
                        isSpecial = true
                        intX = maybeX
                    }
                }
            } else {
                // more height -> favourites at bottom
                val favSize = min(
                    width.toFloat() / AllManager.FAVOURITE_COUNT,
                    height * AllManager.MAX_FAVOURITES_RATIO
                )
                if (event.y > height - favSize) {
                    val maybeX = event.x / favSize
                    if (searchIfNull && AllManager.favourites.getOrNull(maybeX.toInt()) == null) {
                    } else {
                        isSpecial = true
                        intX = maybeX
                    }
                }
            }
        }

        val intY = y / sy
        // we have no scroll section -> we are fine with always being valid
        return Triple(if (isSpecial) AreaType.FAVOURITES else AreaType.ELEMENTS, intX, intY)
    }

    fun getDistanceSq(loc: MovingLocation, x: Float, y: Float): Float {
        return sq(loc.targetX - x, loc.targetY - y)
    }

    fun getElementAt(x: Float, y: Float): Element? {
        var minElement = tree.element
        var minDistance = sq(x, y)
        tree.toThisElement.forEach { r ->
            val da = getDistanceSq(r.al, x, y)
            if (da < minDistance) {
                minDistance = da
                minElement = r.a
            }
            val db = getDistanceSq(r.bl, x, y)
            if (db < minDistance) {
                minDistance = db
                minElement = r.b
            }
        }
        tree.fromThisElement.forEach { r ->
            val da = getDistanceSq(r.al, x, y)
            if (da < minDistance) {
                minDistance = da
                minElement = r.a
            }
            val db = getDistanceSq(r.bl, x, y)
            if (db < minDistance) {
                minDistance = db
                minElement = r.b
            }
            val dr = getDistanceSq(r.rl, x, y)
            if (dr < minDistance) {
                minDistance = dr
                minElement = r.r
            }
        }
        return minElement
    }

    fun getScale(): Pair<Float, Float> {
        val min = min(measuredWidth, measuredHeight)
        val sc = if (min > widthPerNode) {
            // w/n or w/n*0.5 is seldom important
            (min - widthPerNode) * 0.495f
        } else {
            min * 0.45f
        }
        return sc to sc
    }

    var dragged: Element? = null

    var mx = 0f
    var my = 0f
    var md = 0f

    init {

        val scrollListener = GestureDetector(object : GestureDetector.OnGestureListener {
            override fun onShowPress(e: MotionEvent?) {}
            override fun onDown(event: MotionEvent?): Boolean {
                return if (event != null) {

                    md = 0f

                    val (valid, internalX, internalY) = validXY(event)

                    dragged = when (valid) {
                        AreaType.ELEMENTS -> getElementAt(internalX, internalY)
                        AreaType.FAVOURITES -> AllManager.favourites[internalX.toInt()]
                        AreaType.IGNORE -> null
                    }

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
                md += sqrt(dx * dx + dy * dy)
                return true
            }

            override fun onLongPress(e: MotionEvent?) {}
            override fun onSingleTapUp(event: MotionEvent?): Boolean = false
        })

        val zoomListener =
            ScaleGestureDetector(context, object : ScaleGestureDetector.OnScaleGestureListener {
                override fun onScale(detector: ScaleGestureDetector?): Boolean {
                    return if (detector != null && detector.scaleFactor != 1f) {
                        widthPerNode = clamp(
                            widthPerNode * detector.scaleFactor,
                            10f,
                            min(measuredWidth, measuredHeight) * 0.5f
                        )
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

            if (dragged != null && event.pointerCount > 1) {
                dragged = null
                invalidate()
            }

            when (event.actionMasked) {
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {

                    if (dragged != null) {
                        val first = dragged!!
                        dragged = null
                        val (valid, internalX, internalY) = validXY(event)
                        val second = when (valid) {
                            AreaType.ELEMENTS -> getElementAt(internalX, internalY)
                            AreaType.FAVOURITES -> {
                                // set it here
                                AllManager.favourites[internalX.toInt()] = first
                                AllManager.saveFavourites()
                                AllManager.clickSound.play()
                                invalidate()
                                null
                            }
                            AreaType.IGNORE -> null
                        }
                        if (second != null) {
                            if (second == first &&
                                md < min(measuredWidth, measuredHeight) * 0.03f &&
                                first != tree.element
                            ) {
                                // AllManager.okSound.play()
                                switchTo(first)
                            } else {
                                BasicOperations.onRecipeRequest(first, second,
                                    all, measuredWidth, measuredHeight,
                                    { unlockElement(first, second, it) }, {
                                        add(first, second, it)
                                    })
                            }
                        }
                    }

                }
            }

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
    var time = 0f

    fun switchTo(element: Element) {
        buildTree(element)
        time = 0f
        postInvalidate()
    }

    override fun draw(canvas: Canvas?) {
        super.draw(canvas)
        canvas ?: return

        if (!hasTree) {
            if (AllManager.elementById.isEmpty()) {
                AllManager.registerBaseElements(null)
            }
            buildTree(AllManager.elementById[1]!!)
        }

        GroupsEtc.tick()

        val thisTime = System.nanoTime()
        val deltaTime = clamp((thisTime - lastTime).toInt(), 0, 250000000) * 1e-9f
        lastTime = thisTime

        time += deltaTime * 2f

        val width = measuredWidth.toFloat()
        val height = measuredHeight.toFloat()

        val centerX = width / 2
        val centerY = height / 2

        linePaint.color = 0x30000000
        linePaint.strokeWidth = max(2f, min(width, height) * 0.005f)

        val showCraftingCounts = AllManager.showCraftingCounts
        val showUUIDs = AllManager.showElementUUID

        val save = canvas.save()
        canvas.translate(centerX, centerY)

        val (scaleX, scaleY) = getScale()

        // val line0 = System.nanoTime()
        tree.draw(time, { element, x, y, alpha, sizeFactor ->
            val size = sizeFactor * widthPerNode
            if (alpha > 0.01f) {
                val alphaInt = (min(1f, alpha) * 255).toInt()
                bgPaint.alpha = alphaInt
                textPaint.alpha = alphaInt
                drawElement(
                    canvas,
                    showCraftingCounts,
                    showUUIDs,
                    x * scaleX - size * 0.5f,
                    y * scaleY - size * 0.5f,
                    0f,
                    size,
                    true,
                    element,
                    bgPaint,
                    textPaint
                )
            }
        }) { x0, y0, x1, y1, alpha ->
            if (alpha > 0.003f) {
                linePaint.alpha = (min(alpha * 0.5f, 1f) * 255).toInt()
                canvas.drawLine(x0 * scaleX, y0 * scaleY, x1 * scaleX, y1 * scaleY, linePaint)
            }
        }

        bgPaint.alpha = 255
        textPaint.alpha = 255
        linePaint.alpha = 255

        canvas.restoreToCount(save)

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

        if (time < 1f) invalidate()

    }

    private fun sq(x: Float, y: Float) = x * x + y * y

    var activeElement: Element? = null
    var activeness = 0f

    fun add(sa: Element, sb: Element, element: Element): Boolean {
        addRecipe(sa, sb, element, all)
        val unlocked = unlockeds[element.group]
        return if (!unlocked.contains(element) && element.uuid > -1) {
            unlocked.add(element)
            // unlocked.sortBy { it.uuid }
            // invalidateSearch()
            postInvalidate()
            true
        } else false
    }

    fun unlockElement(componentA: Element, componentB: Element, result: Element) {

        //  add to achieved :D
        val newOne = add(componentA, componentB, result)
        synchronized(Unit) {
            activeElement = result
            activeness = 1f
        }

        switchTo(result)

        AllManager.staticRunOnUIThread {
            invalidate()
            (if (newOne) AllManager.successSound else AllManager.okSound).play()
        }

    }


}