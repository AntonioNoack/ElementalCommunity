package me.antonio.noack.elementalcommunity

import android.app.Dialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.math.MathUtils.clamp
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
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.max

// todo show things that might soon be available (if more players)

open class UnlockedRows(ctx: Context, attributeSet: AttributeSet?): View(ctx, attributeSet) {

    lateinit var all: AllManager

    var unlockeds = AllManager.unlockeds
    private var entriesPerRow = 5

    var scroll = 0f
    var scrollDest = 0f

    private var dragged: Element? = null
    private var activeElement: Element? = null
    private var activeness = 0f

    private val maxTouches = 64
    private var oldX = FloatArray(maxTouches)
    private val oldY = FloatArray(maxTouches)

    private var mx = 0f
    private var my = 0f

    private var isOnBorder = 0

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        entriesPerRow = if(measuredWidth > measuredHeight) 10 else 5
    }

    init {

        setOnTouchListener { _, event ->

            val width = measuredWidth * 1f
            val avgMargin = getMargin(width / entriesPerRow)
            val widthPerNode = (width - 2 * avgMargin) / (entriesPerRow + 0.5f)
            val intX = (event.x - avgMargin) / widthPerNode
            val intY = (event.y - avgMargin + scroll) / widthPerNode
            val fraX = fract(intX)
            val fraY = fract(intY)
            val valid = fraX in 0.1f .. 0.9f && fraY in 0.1f .. 0.9f
            val internalX = intX.toInt()
            val internalY = intY.toInt()

            when(event.actionMasked){
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {

                    mx = event.x
                    my = event.y

                    val index = event.actionIndex
                    val id = event.getPointerId(index)
                    oldX[id] = event.getX(index)
                    oldY[id] = event.getY(index)

                    dragged = if(valid) getElementAt(internalX, internalY) else null

                    invalidate()

                }

                MotionEvent.ACTION_MOVE -> {

                    mx = event.x
                    my = event.y

                    if(dragged == null){

                        for(index in 0 until event.pointerCount){
                            val id = event.getPointerId(index)
                            val dy = oldY[id] - event.getY(index)
                            oldY[id] = event.getY(index)
                            if(dy < widthPerNode * 0.3f){
                                scroll += dy
                            }
                        }

                    } else {

                        val y = event.y
                        isOnBorder = when {
                            y < measuredWidth * 0.08f -> -1
                            y > measuredHeight * 0.92f -> +1
                            else -> 0
                        }

                    }

                    checkScroll()
                    invalidate()

                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {

                    if(dragged != null){
                        val first = dragged!!
                        dragged = null
                        val second = if(valid) getElementAt(internalX, internalY) else null
                        if(second != null){
                            onRecipeRequest(first, second)
                        }
                    }

                    invalidate()

                }

            }

            true

        }

    }

    open fun onRecipeRequest(first: Element, second: Element){
        thread(true){
            WebServices.askRecipe(first, second, { result ->
                if(result != null){
                    //  add to achieved :D
                    val newOne = add(result)
                    unlockedIds.add(result.uuid)
                    activeElement = result
                    activeness = 1f
                    save()
                    // scroll to destination on success
                    scrollDest = getRow(result)*1f/countRows() * max(0f, neededHeight() - measuredHeight)
                    staticRunOnUIThread {
                        invalidate()
                        (if(newOne) AllManager.successSound else AllManager.okSound).play()
                    }
                } else {
                    // staticRunOnUIThread { AllManager.askingSound.play() }
                    askForRecipe(first, second)
                    WebServices.askRecipe(first, second, { result2 ->
                        if(result2 != null){// todo remove in the future, when the least amount of support is 2 or sth like that
                            add(result2)
                            unlockedIds.add(result2.uuid)
                            save()
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
                    dialog.dismiss()
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
                            staticToast2(R.string.sent, false)
                            staticRunOnUIThread { dialog.dismiss() }
                        })
                    }
                }
                if(candidates.isEmpty()){
                    dialog.findViewById<View>(R.id.title2).visibility = View.GONE
                } else {
                    val suggestionsView = dialog.findViewById<LinearLayout>(R.id.suggestions)
                    val theWidth = measuredWidth * 2f / entriesPerRow
                    for(candidate in candidates){
                        val view = OneElement(context, null)
                        view.candidate = candidate
                        view.theWidth = theWidth
                        view.invalidate()
                        view.onLiked = {
                            dialog.dismiss()
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
        unlockeds.forEach { unlocked ->
            sum += (unlocked.size + entriesPerRow - 1) / entriesPerRow
        }
        val width = measuredWidth
        val widthPerEntry = width / entriesPerRow
        val maxSize = sum * widthPerEntry
        scroll = clamp(scroll,0f, 1f * max(0, maxSize - measuredHeight))
    }

    private fun getElementAt(x: Int, y: Int): Element? {
        var sum = 0
        unlockeds.forEach { unlocked ->
            val delta = (unlocked.size + entriesPerRow - 1) / entriesPerRow
            if(delta > 0 && y-sum in 0 until delta){
                val indexHere = x + entriesPerRow * (y - sum)
                return unlocked.getOrNull(indexHere)
            }
            sum += delta
        }
        return null
    }

    private fun getRow(element: Element): Int {
        var sum = 0
        unlockeds.forEachIndexed { index, unlocked ->
            val delta = (unlocked.size + entriesPerRow - 1) / entriesPerRow
            if(element.group == index){
                return sum + unlocked.indexOf(element) / entriesPerRow
            }
            sum += delta
        }
        return 0
    }

    private fun countRows(): Int {
        var sum = 0
        unlockeds.forEach { unlocked ->
            val delta = (unlocked.size + entriesPerRow - 1) / entriesPerRow
            sum += delta
        }
        return sum
    }

    fun add(element: Element): Boolean {
        val unlocked = unlockeds[element.group]
        return if(!unlocked.contains(element) && element.uuid > -1){
            unlocked.add(element)
            unlocked.sortBy { it.uuid }
            postInvalidate()
            true
        } else false
    }

    private fun neededHeight(): Float {
        val width = measuredWidth * 1f
        val avgMargin = getMargin(width / entriesPerRow)
        val widthPerNode = (width - 2 * avgMargin) / (entriesPerRow + 0.5f)
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

        val width = measuredWidth * 1f
        val avgMargin = getMargin(width / entriesPerRow)
        val widthPerNode = (width - 2 * avgMargin) / (entriesPerRow + 0.5f)

        var y0 = avgMargin - scroll - widthPerNode

        textPaint.textSize = widthPerNode * 0.13f
        textPaint.textAlign = Paint.Align.CENTER

        unlockeds.forEachIndexed { group, unlocked ->

            if(unlocked.isNotEmpty()){

                unlocked.forEachIndexed { index, element ->

                    if(index % entriesPerRow == 0) y0 += widthPerNode

                    val x0 = avgMargin + (index % entriesPerRow) * widthPerNode
                    if(activeElement == element && activeness > 0f){

                        val delta = activeness * widthPerNode * 0.5f
                        drawElement(canvas, x0 - delta, y0 - delta, widthPerNode + 2 * delta, true, element, bgPaint, textPaint)

                    } else {

                        drawElement(canvas, x0, y0, widthPerNode, true, element, bgPaint, textPaint)

                    }

                }

                textPaint.color = 0xff000000.toInt()
                textPaint.textSize = widthPerNode * 0.13f

                val text = "${unlocked.size}/${GroupSizes[group]}"
                canvas.drawText(text, width - textPaint.measureText(text), y0 + widthPerNode*2/3 - (textPaint.ascent() + textPaint.descent()), textPaint)

            }
        }

        val dragged = dragged
        if(dragged != null){
            drawElement(canvas, mx - widthPerNode/2, my - widthPerNode/2, widthPerNode, true, dragged, bgPaint, textPaint) }

        if(activeness > 0f){

            activeness -= deltaTime
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

        if(isOnBorder != 0){

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

}