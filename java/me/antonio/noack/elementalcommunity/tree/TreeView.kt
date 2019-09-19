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
import me.antonio.noack.elementalcommunity.AllManager
import me.antonio.noack.elementalcommunity.AllManager.Companion.addRecipe
import me.antonio.noack.elementalcommunity.BasicOperations
import me.antonio.noack.elementalcommunity.Element
import me.antonio.noack.elementalcommunity.GroupsEtc.drawElement
import me.antonio.noack.elementalcommunity.utils.Maths
import kotlin.math.*

class TreeView(ctx: Context, attributeSet: AttributeSet?): View(ctx, attributeSet){

    lateinit var all: AllManager
    var unlockeds = AllManager.unlockeds

    val tree = Tree()
    var hasTree = false

    private fun buildTree(){
        tree.invalidate()
        hasTree = true
    }

    override fun invalidate() {
        super.invalidate()
        hasTree = false
    }

    var widthPerNode = 100f

    fun validXY(event: MotionEvent): Triple<Boolean, Int, Int>{
        val x = event.x - measuredWidth * 0.5f
        val y = event.y - measuredHeight * 0.5f
        val fx = x / widthPerNode + scrollX
        val fy = y / widthPerNode + scrollY
        val xi = floor(fx).toInt()
        val yi = floor(fy).toInt()
        // we have no scroll section -> we are fine with always being valid
        return Triple(if(tree.multiplierX <= 1){
            fractOk(fx) && fractOk(fy)
        } else true, xi, yi)
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
        // todo clamp the moving into the appropriate area
        scrollX = clamp(scrollX, -tree.maxRadiusX, tree.maxRadiusX)
        scrollY = clamp(scrollY, -tree.top, tree.bottom)
    }

    fun setOnBorder(event: MotionEvent){
        val x = event.x
        isOnBorderX = when {
            x < measuredWidth * 0.12f -> -1
            x > measuredWidth * 0.88f -> +1
            else -> 0
        }
        val y = event.y
        isOnBorderY = when {
            y < measuredWidth * 0.12f -> -1
            y > measuredHeight - measuredWidth * 0.12f -> +1
            else -> 0
        }
    }

    init {

        val scrollListener = GestureDetector(object: GestureDetector.OnGestureListener {
            override fun onShowPress(e: MotionEvent?) {}
            override fun onDown(event: MotionEvent?): Boolean {
                return if(event != null){

                    val (valid, internalX, internalY) = validXY(event)

                    dragged = if(valid) getElementAt(internalX, internalY) else null

                    setOnBorder(event)

                    false

                } else false
            }
            override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean = false
            override fun onScroll(event: MotionEvent?, e2: MotionEvent?, dx: Float, dy: Float): Boolean {

                return if(event != null){

                    if(dragged == null){

                        val max = measuredWidth * 0.2f

                        if(dx != 0f && abs(dx) < max){ scrollX += dx / widthPerNode }
                        if(dy != 0f && abs(dy) < max){ scrollY += dy / widthPerNode }

                        checkScroll()

                    }

                    scrollDest = null

                    true

                } else false

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
                        val second = if(valid) getElementAt(internalX, internalY) else null
                        if(second != null){
                            BasicOperations.onRecipeRequest(first, second, all, measuredWidth, { unlockElement(first, second, it) }, { add(first, second, it) })

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

        if(!hasTree) buildTree()

        checkScroll()

        val thisTime = System.nanoTime()
        val deltaTime = clamp((thisTime - lastTime).toInt(), 0, 250000000) * 1e-9f
        lastTime = thisTime

        val width = measuredWidth.toFloat()
        val height = measuredHeight.toFloat()

        val centerX = width/2
        val centerY = height/2

        linePaint.color = 0x30000000.toInt()
        linePaint.strokeWidth = max(2f, min(width, height) * 0.005f)

        if(tree.multiplierX != 1 || tree.multiplierY != 1){
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

        for(element in tree.elements){
            // done draw the element
            if(element.treeX in minX .. maxX && element.treeY in minY .. maxY){
                val x0 = centerX + (element.treeX - scrollX) * widthPerNode
                val y0 = centerY + (element.treeY - scrollY) * widthPerNode
                if(activeElement == element && activeness > 0f){
                    val delta = activeness * widthPerNode * 0.5f
                    drawElement(canvas, x0, y0, delta, widthPerNode, true, element, bgPaint, textPaint)
                } else {
                    drawElement(canvas, x0, y0, 0f, widthPerNode, true, element, bgPaint, textPaint)
                }
            }
        }

        val dragged = dragged
        if(dragged != null){
            drawElement(canvas, mx - widthPerNode/2, my - widthPerNode/2, 0f, widthPerNode, true, dragged, bgPaint, textPaint) }

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
        addRecipe(sa, sb, element)
        val unlocked = unlockeds[element.group]
        return if(!unlocked.contains(element) && element.uuid > -1){
            unlocked.add(element)
            unlocked.sortBy { it.uuid }
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