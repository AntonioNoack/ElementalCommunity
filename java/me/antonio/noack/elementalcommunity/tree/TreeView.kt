package me.antonio.noack.elementalcommunity.tree

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

class TreeView(ctx: Context, attributeSet: AttributeSet?): View(ctx, attributeSet){

    var allowLeftFavourites = true

    lateinit var all: AllManager
    var unlockeds = AllManager.unlockeds

    val tree = Tree()
    var hasTree = false
    var hasTreeReally = false
    var oldMultiplier = tree.multiplierX

    // measured border coordinates for prevention of scrolling further away

    var minX = 0
    var maxX = 0
    var minY = 0
    var maxY = 0

    var minXf = 0f
    var maxXf = 0f
    var minYf = 0f
    var maxYf = 0f

    var ctr = 0

    private fun buildTree(){

        hasTree = true
        val ctr = ++ctr
        thread {

            // println("starting the tree")

            tree.invalidate()

            // println("tree done")

            if(ctr == this.ctr){

                this.postInvalidate()

                minX = 0
                minY = 0
                maxX = 0
                maxY = 0

                for(element in tree.elements){
                    minX = min(minX, element.treeX)
                    maxX = max(maxX, element.treeX)
                    minY = min(minY, element.treeY)
                    maxY = max(maxY, element.treeY)
                }

                minXf = minX.toFloat()
                maxXf = maxX.toFloat()
                minYf = minY.toFloat()
                maxYf = maxY.toFloat()

                hasTreeReally = true

            }

        }


    }

    var widthPerNode = 100f

    fun validXY(event: MotionEvent, searchIfNull: Boolean = false): Triple<AreaType, Int, Int>{

        val x = event.x - measuredWidth * 0.5f
        val y = event.y - measuredHeight * 0.5f
        var intX = x / widthPerNode + scrollX

        var isSpecial = false
        if(AllManager.FAVOURITE_COUNT > 0){
            if(width > height && allowLeftFavourites){
                // more width -> favourites at left
                val favSize = min(height.toFloat() / AllManager.FAVOURITE_COUNT, width * AllManager.MAX_FAVOURITES_RATIO)
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
                val favSize = min(width.toFloat() / AllManager.FAVOURITE_COUNT, height * AllManager.MAX_FAVOURITES_RATIO)
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

        val intY = y / widthPerNode + scrollY
        val xi = floor(intX).toInt()
        val yi = floor(intY).toInt()
        // we have no scroll section -> we are fine with always being valid
        val valid = if(tree.multiplierX <= 1){
            fractOk(intX) && fractOk(intY)
        } else true
        return Triple(if(isSpecial) if(intX-floor(intX) > 0.5f) AreaType.FAVOURITES_TOP else AreaType.FAVOURITES_BOTTOM else if(valid){
            AreaType.ELEMENTS
        } else AreaType.IGNORE, xi, yi)
    }

    fun fractOk(value: Float): Boolean {
        val fract = value - floor(value)
        return fract > 0.15f && fract < 0.85f
    }

    fun getElementAt(x: Int, y: Int): Element? = tree.treeMap[x.shl(16) or y.and(0xffff)]

    var dragged: Element? = null
    var isOnBorderX = 0
    var isOnBorderY = 0

    var scrollX = 0f
    var scrollY = 0f

    // var scroll = 0f
    var scrollDest: Element? = null

    var mx = 0f
    var my = 0f

    fun checkScroll(){
        scrollX = clamp(scrollX, minXf, maxXf)
        scrollY = clamp(scrollY, minYf, maxYf)
    }

    fun setOnBorder(event: MotionEvent){
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

        val scrollListener = GestureDetector(object: GestureDetector.OnGestureListener {
            override fun onShowPress(e: MotionEvent?) {}
            override fun onDown(event: MotionEvent?): Boolean {
                return if(event != null){

                    val (valid, internalX, internalY) = validXY(event)

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
            override fun onScroll(event: MotionEvent?, e2: MotionEvent?, dx: Float, dy: Float): Boolean {

                if(dragged == null){

                    val max = measuredWidth * 0.2f

                    if(dx != 0f && abs(dx) < max){ scrollX += dx / widthPerNode }
                    if(dy != 0f && abs(dy) < max){ scrollY += dy / widthPerNode }

                    checkScroll()

                    invalidate()

                }

                scrollDest = null

                return true

            }
            override fun onLongPress(e: MotionEvent?) {}
            override fun onSingleTapUp(event: MotionEvent?): Boolean = false
        })

        val zoomListener = ScaleGestureDetector(context, object: ScaleGestureDetector.OnScaleGestureListener {
            override fun onScale(detector: ScaleGestureDetector?): Boolean {
                return if(detector != null && detector.scaleFactor != 1f){
                    widthPerNode = clamp(widthPerNode * detector.scaleFactor, 10f, 1000f)
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

            when(event.actionMasked){
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {

                    if(dragged != null){
                        val first = dragged!!
                        dragged = null
                        val (valid, internalX, internalY) = validXY(event)
                        val second = when(valid){
                            AreaType.ELEMENTS -> getElementAt(internalX, internalY)
                            AreaType.FAVOURITES_TOP,
                            AreaType.FAVOURITES_BOTTOM -> {
                                // set it here
                                AllManager.favourites[internalX] = first
                                AllManager.save()
                                AllManager.clickSound.play()
                                invalidate()
                                null
                            }
                            AreaType.IGNORE -> null
                        }
                        if(second != null){
                            BasicOperations.onRecipeRequest(first, second, all, measuredWidth, measuredHeight, { unlockElement(first, second, it) }, { add(first, second, it) })

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

    override fun draw(canvas: Canvas?) {
        super.draw(canvas)
        canvas ?: return

        if(!hasTree || oldMultiplier != tree.multiplierX) buildTree()

        GroupsEtc.tick()

        checkScroll()

        val thisTime = System.nanoTime()
        val deltaTime = clamp((thisTime - lastTime).toInt(), 0, 250000000) * 1e-9f
        lastTime = thisTime

        if(!hasTreeReally) return

        val width = measuredWidth.toFloat()
        val height = measuredHeight.toFloat()

        val centerX = width/2
        val centerY = height/2

        linePaint.color = 0x30000000.toInt()
        linePaint.strokeWidth = max(2f, min(width, height) * 0.005f)

        // val line0 = System.nanoTime()
        if(tree.multiplierX != 1 || tree.multiplierY != 1){
            synchronized(tree.elements){
                for(element in tree.elements){
                    if(element.hasTreeOutput){
                        drawElementOutput(canvas, centerX, centerY, element)
                    }
                }

                for(element in tree.elements){
                    val srcA = element.srcA ?: continue
                    val srcB = element.srcB ?: continue
                    drawPath(canvas, centerX, centerY, srcA, element, true)
                    if(srcA != srcB) drawPath(canvas, centerX, centerY, srcB, element, false)
                }
            }
        }

        /*for((start, end) in tree.getPaths()){
            // done draw the path
            // done
            // |______
            //       |

        }*/

        // x = centerX + (element.treeX - scrollX) * widthPerNode
        val minX = (- centerX / widthPerNode + scrollX - 1.5f).toInt()
        val maxX = (centerX / widthPerNode + scrollX + 0.5f).toInt()
        val minY = (- centerY / widthPerNode + scrollY - 1.5f).toInt()
        val maxY = (centerY / widthPerNode + scrollY + 0.5f).toInt()

        val showCraftingCounts = AllManager.showCraftingCounts

        synchronized(tree.elements){
            for(element in tree.elements){
                // done draw the element
                if(element.treeX in minX .. maxX && element.treeY in minY .. maxY){
                    val x0 = centerX + (element.treeX - scrollX) * widthPerNode
                    val y0 = centerY + (element.treeY - scrollY) * widthPerNode
                    if(activeElement == element && activeness > 0f){
                        val delta = activeness * widthPerNode * 0.5f
                        drawElement(canvas, showCraftingCounts, x0, y0, delta, widthPerNode, true, element, bgPaint, textPaint)
                    } else {
                        drawElement(canvas, showCraftingCounts, x0, y0, 0f, widthPerNode, true, element, bgPaint, textPaint)
                    }
                }
            }
        }

        drawFavourites(canvas, false, width, height, bgPaint, textPaint, allowLeftFavourites)

        val dragged = dragged
        if(dragged != null){
            drawElement(canvas, false, mx - widthPerNode/2, my - widthPerNode/2, 0f, widthPerNode, true, dragged, bgPaint, textPaint) }

        if(activeness > 0f){
            this.activeness -= deltaTime
            invalidate()
        }

        val scrollDest = scrollDest
        if(scrollDest != null){

            val dt = clamp(3f * deltaTime, 0f, 1f)

            val scrollDestX = scrollDest.treeX + 0.5f
            val scrollDestY = scrollDest.treeY + 0.5f

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
                else -> {
                    invalidate()
                }
            }

        }

        if((isOnBorderX != 0 || isOnBorderY != 0) && dragged != null){

            val oldScrollX = scrollX
            val oldScrollY = scrollY

            scrollX += isOnBorderX * deltaTime * 10f
            scrollY += isOnBorderY * deltaTime * 10f

            checkScroll()

            if(oldScrollX == scrollX && oldScrollY == scrollY){
                isOnBorderX = 0
                isOnBorderY = 0
            } else {
                invalidate()
            }

        }

    }

    fun sq(x: Float, y: Float) = x*x+y*y

    fun drawElementOutput(canvas: Canvas, centerX: Float, centerY: Float, src: Element){
        // y of dst is larger
        val x0 = centerX + (src.treeX - scrollX + 0.5f) * widthPerNode
        var y0 = centerY + (src.treeY - scrollY + 0.5f) * widthPerNode
        y0 += widthPerNode * .4f // there is a box anyways ;)
        val y2 = y0 + widthPerNode * 0.3f
        // val xc = (x0 + x1) / 2
        // val yc = (y1 - widthPerNode * 0.5f)
        // dy
        canvas.drawLine(x0, y0, x0, y2, linePaint)
    }

    fun drawPath(canvas: Canvas, centerX: Float, centerY: Float, src: Element, dst: Element, down: Boolean){
        // y of dst is larger
        val x0 = centerX + (src.treeX - scrollX + 0.5f) * widthPerNode
        var y0 = centerY + (src.treeY - scrollY + 0.5f) * widthPerNode
        val x1 = centerX + (dst.treeX - scrollX + 0.5f) * widthPerNode
        var y1 = centerY + (dst.treeY - scrollY + 0.5f) * widthPerNode
        y0 += widthPerNode * .4f // there is a box anyways ;)
        y1 -= widthPerNode * .4f // there is a box anyways ;)
        val y2 = y0 + widthPerNode * 0.3f
        val y3 = y1 - widthPerNode * 0.3f
        // val xc = (x0 + x1) / 2
        // val yc = (y1 - widthPerNode * 0.5f)
        // dy
        // if(out) canvas.drawLine(x0, y0, x0, y2, linePaint)
        // dx
        // y2 > y3
        if(y3 > 0 && y2 < centerY*2 && min(x0, x1) < centerX*2 && max(x0, x1) > 0f) {
            canvas.drawLine(x0, y2, x1, y3, linePaint)
        }
        // dy
        if(down && (y1 > 0 && y3 < centerY*2 && x1 > 0 && x1 < centerX*2)) canvas.drawLine(x1, y3, x1, y1, linePaint)
    }

    var activeElement: Element? = null
    var activeness = 0f

    fun add(sa: Element, sb: Element, element: Element): Boolean {
        addRecipe(sa, sb, element, all)
        val unlocked = unlockeds[element.group]
        return if(!unlocked.contains(element) && element.uuid > -1){
            unlocked.add(element)
            // unlocked.sortBy { it.uuid }
            // invalidateSearch()
            postInvalidate()
            true
        } else false
    }

    fun unlockElement(sa: Element, sb: Element, element: Element){

        //  add to achieved :D
        val newOne = add(sa, sb, element)
        synchronized(Unit){
            AllManager.unlockedIds.add(element.uuid)
            activeElement = element
            activeness = 1f
            AllManager.save()
        }

        scrollDest = element

        AllManager.staticRunOnUIThread {
            invalidate()
            (if (newOne) AllManager.successSound else AllManager.okSound).play()
        }

    }



}