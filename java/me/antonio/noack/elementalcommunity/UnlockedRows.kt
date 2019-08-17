package me.antonio.noack.elementalcommunity

import android.app.Dialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.math.MathUtils.clamp
import androidx.core.view.marginRight
import me.antonio.noack.elementalcommunity.AllManager.Companion.save
import me.antonio.noack.elementalcommunity.AllManager.Companion.staticRunOnUIThread
import me.antonio.noack.elementalcommunity.AllManager.Companion.staticToast1
import me.antonio.noack.elementalcommunity.AllManager.Companion.staticToast2
import me.antonio.noack.elementalcommunity.AllManager.Companion.unlockedIds
import me.antonio.noack.elementalcommunity.GroupsEtc.GroupSizes
import me.antonio.noack.elementalcommunity.GroupsEtc.drawElement
import me.antonio.noack.elementalcommunity.GroupsEtc.getMargin
import me.antonio.noack.elementalcommunity.api.WebServices
import me.antonio.noack.elementalcommunity.utils.Maths.fract
import me.antonio.noack.elementalcommunity.utils.Maths.mix
import java.lang.IllegalArgumentException
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.max

// show things that might soon be available (if much more players)

open class UnlockedRows(ctx: Context, attributeSet: AttributeSet?): View(ctx, attributeSet) {

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
                for((group, unlocked) in (if(search.startsWith(lastSearch) && lastParts == parts.size) shownSymbols else unlockeds).withIndex()){
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
    val shownSymbols = Array(unlockeds.size){ ArrayList<Element>() }

    private var entriesPerRow = 5

    var scroll = 0f
    var scrollDest = 0f

    private var dragged: Element? = null
    private var activeElement: Element? = null
    private var activeness = 0f

    private var mx = 0f
    private var my = 0f

    private var isOnBorder = 0

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

    fun validXY(event: MotionEvent): Triple<Boolean, Int, Int> {

        val measuredWidth = measuredWidth
        val entriesPerRow = entriesPerRow
        val scroll = scroll

        val width = measuredWidth * 1f
        val avgMargin = getMargin(width / entriesPerRow)
        val widthPerNode = widthPerNode()
        val intX = (event.x) / widthPerNode
        val intY = (event.y + scroll) / widthPerNode
        val fraX = fract(intX)
        val fraY = fract(intY)
        val valid = sq(fraX - 0.5f) + sq(fraY - 0.5f) < 0.15f // < fraX in 0.18f .. 0.82f && fraY in 0.18f .. 0.82f
        val internalX = intX.toInt()
        val internalY = intY.toInt()

        return Triple(valid, internalX, internalY)

    }

    fun sq(f: Float): Float = f*f

    init {

        val scrollListener = GestureDetector(object: GestureDetector.OnGestureListener {
            override fun onShowPress(e: MotionEvent?) {}
            override fun onDown(event: MotionEvent?): Boolean {
                return if(event != null){

                    val (valid, internalX, internalY) = validXY(event)

                    dragged = if(valid) getElementAt(internalX, internalY) else null

                    val y = event.y
                    isOnBorder = when {
                        y < measuredWidth * 0.12f -> -1
                        y > measuredHeight * 0.88f -> +1
                        else -> 0
                    }

                    false

                } else false
            }
            override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean = false
            override fun onScroll(event: MotionEvent?, e2: MotionEvent?, distanceX: Float, dy: Float): Boolean {

                return if(event != null){

                    if(dragged == null){

                        val widthPerNode = widthPerNode()
                        if(dy != 0f && dy < widthPerNode * 0.7f){
                            scroll += dy
                        }

                    }

                    scrollDest = Float.NaN

                    true

                } else false

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

            isOnBorder = when {
                my < measuredWidth * 0.12f -> -1
                my > measuredHeight - measuredWidth * 0.12f -> +1
                else -> 0
            }

            when(event.actionMasked){
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {

                    if(dragged != null){
                        val first = dragged!!
                        dragged = null
                        val (valid, internalX, internalY) = validXY(event)
                        val second = if(valid) getElementAt(internalX, internalY) else null
                        if(second != null){
                            onRecipeRequest(first, second)
                        }
                    }

                    isOnBorder = 0

                }
            }

            checkScroll()

            invalidate()

            true

        }

    }

    fun unlockElement(element: Element){
        //  add to achieved :D
        val newOne = add(element)
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
        thread(true){
            WebServices.askRecipe(first, second, { result ->
                if(result != null){
                    unlockElement(result)
                } else {
                    // staticRunOnUIThread { AllManager.askingSound.play() }
                    askForRecipe(first, second)
                    WebServices.askRecipe(first, second, { result2 ->
                        if(result2 != null){// remove in the future, when the least amount of support is 2 or sth like that
                            synchronized(Unit){
                                add(result2)
                                unlockedIds.add(result2.uuid)
                                save()
                            }
                        }
                    }, {})
                }
            }, {
                staticToast1("${it.javaClass.simpleName}: ${it.message}", true)
            })
        }
    }

    fun askForRecipe(a: Element, b: Element){
        // ask for recipe to add :)
        // todo show that we are loading
        WebServices.getCandidates(a, b, { candidates ->
            staticRunOnUIThread {
                val dialog: Dialog = AlertDialog.Builder(context)
                    .setView(R.layout.add_recipe)
                    .show()
                dialog.findViewById<TextView>(R.id.cancel).setOnClickListener {
                    try { dialog.dismiss() } catch (e: Throwable){}
                }
                dialog.findViewById<TextView>(R.id.submit).setOnClickListener {
                    val name = dialog.findViewById<TextView>(R.id.name).text.toString()
                    val group = dialog.findViewById<Colors>(R.id.colors).selected
                    if(group < 0){
                        staticToast2(R.string.please_choose_color, false)
                        return@setOnClickListener
                    }
                    if(name.isEmpty()){
                        staticToast2(R.string.please_choose_name, false)
                        return@setOnClickListener
                    }
                    for(char in name){
                        if(char !in 'A' .. 'Z' && char !in 'a' .. 'z' && char !in '0' .. '9' && char !in " ,.'"){
                            staticToast2(R.string.only_az09, true)
                            return@setOnClickListener
                        }
                    }
                    thread {
                        WebServices.suggestRecipe(all, a, b, name, group, {
                            val str = String(it).split('\n')[0]
                            println("by recipe: $str")
                            val index1 = str.indexOf(':')
                            val index2 = str.indexOf(':', index1+1)
                            staticToast2(R.string.sent, false)
                            staticRunOnUIThread { try { dialog.dismiss() } catch (e: IllegalArgumentException){} }
                            if(index1 > 0 && index2 > 0){
                                val rUUID = str.substring(0, index1).toIntOrNull() ?: return@suggestRecipe
                                val rGroup = str.substring(index1+1, index2).toIntOrNull() ?: return@suggestRecipe
                                val rName = str.substring(index2+1)
                                val element = Element.get(rName, rUUID, rGroup)
                                unlockElement(element)
                            }
                        })
                    }
                }
                if(candidates.isEmpty()){
                    dialog.findViewById<View>(R.id.title2).visibility = GONE
                } else {
                    val suggestionsView = dialog.findViewById<LinearLayout>(R.id.suggestions)
                    val theWidth = measuredWidth * 2f / entriesPerRow
                    for(candidate in candidates){
                        val view = OneElement(context, null)
                        view.candidate = candidate
                        view.theWidth = theWidth
                        view.invalidate()
                        view.onLiked = {
                            try { dialog.dismiss() } catch (e: IllegalArgumentException){}
                            WebServices.likeRecipe(all, candidate.uuid, {
                                staticToast2(R.string.sent, false)
                            })
                        }
                        view.onDisliked = {
                            WebServices.dislikeRecipe(all, candidate.uuid, {
                                staticToast2(R.string.sent, false)
                            })
                        }
                        view.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                        suggestionsView.addView(view)
                    }
                }
            }
        }, {
            staticToast1("${it.javaClass.simpleName}: ${it.message}", true)
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
        val maxSize = sum * widthPerNode + avgMargin
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

    fun add(element: Element): Boolean {
        val unlocked = unlockeds[element.group]
        return if(!unlocked.contains(element) && element.uuid > -1){
            unlocked.add(element)
            unlocked.sortBy { it.uuid }
            invalidateSearch()
            postInvalidate()
            true
        } else false
    }

    private fun neededHeight(): Float {
        val (widthPerNode, avgMargin) = widthPerNodeNMargin()
        return 2 * avgMargin + widthPerNode * countRows()
    }

    private var lastTime = 0L

    private val bgPaint = Paint()
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    init {textPaint.textAlign = Paint.Align.CENTER }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if(canvas == null) return

        val thisTime = System.nanoTime()
        val deltaTime = clamp((thisTime - lastTime).toInt(), 0, 250000000) * 1e-9f
        lastTime = thisTime

        val (widthPerNode, avgMargin) = widthPerNodeNMargin()
        val height = measuredHeight

        var y0 = avgMargin - scroll - widthPerNode
        val entriesPerRow = entriesPerRow
        val activeElement = activeElement
        val activeness = activeness
        val textPaint = textPaint
        val bgPaint = bgPaint

        textPaint.textSize = widthPerNode * 0.13f
        textPaint.textAlign = Paint.Align.CENTER

        shownSymbols.forEachIndexed { group, unlocked ->

            if(unlocked.isNotEmpty()){

                val rows = (unlocked.size + entriesPerRow - 1) / entriesPerRow
                val endY0 = y0 + widthPerNode * rows

                if(endY0 > -widthPerNode && y0 < height){

                    for((index, element) in unlocked.withIndex()){

                        if(index % entriesPerRow == 0) y0 += widthPerNode
                        if(y0 >= height) break
                        if(y0 < -widthPerNode) continue

                        val x0 = avgMargin + (index % entriesPerRow) * widthPerNode
                        if(activeElement == element && activeness > 0f){

                            val delta = activeness * widthPerNode * 0.5f
                            drawElement(canvas, x0, y0, delta, widthPerNode, true, element, bgPaint, textPaint)

                        } else {

                            drawElement(canvas, x0, y0, 0f, widthPerNode, true, element, bgPaint, textPaint)

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

        val dragged = dragged
        if(dragged != null){
            drawElement(canvas, mx - widthPerNode/2, my - widthPerNode/2, 0f, widthPerNode, true, dragged, bgPaint, textPaint) }

        if(activeness > 0f){

            this.activeness -= deltaTime
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

        if(isOnBorder != 0 && dragged != null){

            val oldScroll = scroll
            scroll += isOnBorder * deltaTime * widthPerNode * 5f

            checkScroll()

            if(oldScroll == scroll){
                isOnBorder = 0
            } else {
                invalidate()
            }
        }

    }

    init {
        invalidateSearch()
    }

}