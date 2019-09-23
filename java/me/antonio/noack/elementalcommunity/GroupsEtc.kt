package me.antonio.noack.elementalcommunity

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.*
import androidx.core.math.MathUtils.clamp
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

object GroupsEtc {

    val GroupColors: IntArray
    val GroupSizes: IntArray

    fun tick(){
        time = System.nanoTime()
        val timeout = 5 * 1000000000L
        for((_, cache) in cacheBySize.entries){
            if(cache.map.isNotEmpty() && abs(cache.lastTime - time) > timeout){
                println("cleared ${cache.size}")
                cache.map.clear()
            }// else println("${abs(cache.lastTime - time)}")
        }
    }

    private fun brightness(color: Int): Float {
        val r = (color shr 16) and 255
        val g = (color shr 8) and 255
        val b = color and 255
        return (0.299f*r + 0.587f*g + 0.114f*b)/255f
    }

    val hues = intArrayOf(0, 31, 51, 110, 178, 208, 233, 277, 304, -1).map { x -> x * 1f }
    val saturations = intArrayOf(22, 60, 85).map { x -> x / 100f }

    init {

        // Helligkeitskorrektur :D

        val hue0 = 288
        // val values = intArrayOf(81, 82, 69, 37).map { x -> x / 100f }
        val values = intArrayOf(100, 100, 85, 70).map { x -> x / 100f }
        val brightness0 = FloatArray(saturations.size)

        GroupColors = IntArray(hues.size * saturations.size)
        GroupSizes = IntArray(GroupColors.size)

        val tempArray = FloatArray(3){ hue0 * 1f }
        for(i in 0 until saturations.size){
            tempArray[0] = hue0 * 1f
            tempArray[1] = saturations[i]
            tempArray[2] = values[i]
            brightness0[i] = brightness(Color.HSVToColor(tempArray))
        }

        for(i in 0 until hues.size){
            tempArray[0] = hues[i]
            if(tempArray[0] < 0){
                val id = i * saturations.size
                GroupColors[id + 0] = 0xffdddddd.toInt()
                GroupColors[id + 1] = 0xffbbbbbb.toInt()
                GroupColors[id + 2] = 0xff888888.toInt()
            } else {
                for(j in 0 until saturations.size){
                    val id = i * saturations.size + j
                    tempArray[1] = saturations[j]
                    tempArray[2] = 1f - j * j * .33f * .33f + j * .33f * .25f
                    // tempArray[2] = MathUtils.clamp(values[j] * brightness0[j] / brightness(Color.HSVToColor(tempArray)), 0f, 1f)
                    GroupColors[id] = Color.HSVToColor(tempArray)
                }
            }
        }
    }

    private val fRect = RectF()

    fun getMargin(widthPerNode: Float): Float = widthPerNode * 0.02f

    fun drawElement(canvas: Canvas, x0: Float, y0: Float, delta: Float, widthPerNode: Float, margin: Boolean, element: Element, bgPaint: Paint, textPaint: Paint, opacity: Float = 1f){
        drawElement(canvas, x0, y0, delta, widthPerNode, margin, element.name, element.group, bgPaint, textPaint, opacity)
    }

    fun drawFavourites(canvas: Canvas, width: Float, height: Float, bgPaint: Paint, textPaint: Paint, allowLeftFavourites: Boolean){
        val opacity = 0.5f
        if(width > height && allowLeftFavourites){// on the left
            val fWidth = min(height / AllManager.FAVOURITE_COUNT, width * AllManager.MAX_FAVOURITES_RATIO)
            for((index, favourite) in AllManager.favourites.withIndex()){
                if(favourite != null) drawElement(canvas, 0f,index*fWidth, 0f, fWidth, true, favourite, bgPaint, textPaint, opacity)
                else drawElement(canvas, 0f, index*fWidth, 0f, fWidth, true, "", -1, bgPaint, textPaint, opacity)
            }
        } else {// on the bottom
            val fWidth = min(width / AllManager.FAVOURITE_COUNT, height * AllManager.MAX_FAVOURITES_RATIO)
            val baseY = height - fWidth
            for((index, favourite) in AllManager.favourites.withIndex()){
                if(favourite != null) drawElement(canvas, index*fWidth, baseY, 0f, fWidth, true, favourite, bgPaint, textPaint, opacity)
                else drawElement(canvas, index*fWidth, baseY, 0f, fWidth, true, "", -1, bgPaint, textPaint, opacity)
            }
        }
    }

    fun join(list: List<String>, from: Int, to: Int = list.size): String {
        if(from + 1 == to) return list[from]
        return list.subList(from, to).joinToString(" ")
    }

    fun split(parts: List<String>, index: Int): List<String> {
        return listOf(join(parts, 0, index), join(parts, index))
    }

    fun splitText(text: String, sideRatio: Float, textPaint: Paint): List<String> {
        if(sideRatio < 4f || !text.contains(' ')) return listOf(text)
        val parts = text.split(' ')
        if(parts.size < 3) return parts
        val partSizes = parts.map { part -> textPaint.measureText(part) }
        if(sideRatio < 10f){// 2

            var sum = 0f
            for(size in partSizes){
                sum += size
            }

            var lastDiff = sum
            var sum2 = 0f

            var index = partSizes.lastIndex
            for(i in 0 until partSizes.lastIndex){
                sum2 += partSizes[i]
                val len1 = sum2
                val len2 = sum - sum2
                val dif = abs(len1 - len2)
                if(dif > lastDiff){
                    index = i
                    break
                }
                lastDiff = dif
            }

            return split(parts, index)

        } else { // 3
            // wa + wb > wc, wb + wc > wa
            // wa ~ wb ~ wc

            var sum = 0f
            for(size in partSizes){
                sum += size
            }

            var ba = 0
            var bb = 0
            var bs = Float.POSITIVE_INFINITY

            var wa = partSizes[0]
            for(a in 1 until parts.lastIndex){
                var wb = partSizes[a]
                for(b in a + 1 until parts.size){
                    val wc = sum - (wa + wb)
                    val score = abs(wa - wb) + abs(wb - wc) + abs(wc - wa)
                    if(score < bs){
                        bs = score
                        ba = a
                        bb = b
                    }
                    wb += partSizes[b]
                }
                wa += partSizes[a]
            }

            var sa = 0f
            for(i in 0 until ba){ sa += partSizes[i] }
            var sb = 0f
            for(i in ba until bb){ sb += partSizes[i] }
            val sc = sum - (sa + sb)
            return when {
                sa + sb < sc -> split(parts, bb)
                sb + sc < sa -> split(parts, ba)
                else ->
                    listOf(join(parts, 0, ba), join(parts, ba, bb), join(parts, bb))
            }
        }

    }

    val cacheBySize = HashMap<Int, Cache>()
    var time = System.nanoTime()

    fun getCache(size: Float): HashMap<String, CacheEntry> {
        val index = (ln(size) * 5).toInt()
        var cache = cacheBySize[index] ?: null
        if(cache == null){
            cache = Cache(size)
            cacheBySize.put(index, cache)
        }
        cache.lastTime = time
        return cache.map
    }

    class Cache(val size: Float){
        val map = HashMap<String, CacheEntry>()
        var lastTime = System.nanoTime()
    }

    fun getCacheEntry(rawText: String, widthPerNode: Float, textPaint: Paint, bgPaint: Paint): CacheEntry {
        /*if(lastCacheWidth != widthPerNode){
            println("clear cache, $lastCacheWidth != $widthPerNode")
            cacheEntries.clear()
            lastCacheWidth = widthPerNode
            // println("cleared")
        }*/
        val cacheEntries = getCache(widthPerNode)
        var entry = cacheEntries[rawText]
        if(entry == null){

            val text = rawText.trim()

            // todo split long rawText into multiple sections...
            textPaint.textSize = 20f
            val width0 = textPaint.measureText(text)

            val color = if(brightness(bgPaint.color) > 0.3f) 0xff000000.toInt() else -1

            val sideRatio = width0 / textPaint.textSize
            val list: List<String> = splitText(text, sideRatio, textPaint)

            var width1 = textPaint.measureText(list[0])
            for(i in 1 until list.size){
                width1 = max(width1, textPaint.measureText(list[i]))
            }

            val textSize = clamp(textPaint.textSize * widthPerNode * 0.8f / width1, widthPerNode*0.02f, widthPerNode*0.35f)
            textPaint.textSize = textSize
            val textDy = (widthPerNode - (textPaint.ascent() + textPaint.descent()))/2

            entry = CacheEntry(textSize, textDy, color, list)

            cacheEntries[rawText] = entry

        }

        return entry
    }

    private const val spacingFactor = 0.6f
    class CacheEntry(val textSize: Float, dy0: Float, val color: Int, val lines: List<String>){
        val lineOffset = textSize * spacingFactor * 2f
        val indexOffset = (lines.size - 1) * 0.5f
        val dy = dy0
        val dys = FloatArray(lines.size){ dy + (it - indexOffset) * lineOffset }
    }

    fun drawElement(canvas: Canvas, x0: Float, y0: Float, delta: Float, widthPerNode: Float, margin: Boolean, rawName: String, group: Int, bgPaint: Paint, textPaint: Paint, opacity: Float = 1f){

        drawElementRaw(canvas, x0, y0, delta, widthPerNode, margin, group, bgPaint, opacity)

        val cacheEntry = getCacheEntry(rawName, widthPerNode, textPaint, bgPaint)
        textPaint.color = cacheEntry.color
        if(opacity < 1f) textPaint.color = textPaint.color.and(0xffffff) or ((opacity * 255).toInt().shl(24))

        if(delta == 0f){

            val textSize = cacheEntry.textSize
            textPaint.textSize = textSize

            val x = x0 + widthPerNode * 0.5f
            val dys = cacheEntry.dys

            for((index, text) in cacheEntry.lines.withIndex()){
                canvas.drawText(text, x, y0 + dys[index], textPaint)
            }

        } else {

            val save = canvas.save()

            val zoom = (widthPerNode + delta + delta) / widthPerNode
            val half = widthPerNode * 0.5f
            canvas.scale(zoom, zoom, x0 + half, y0 + half)

            val textSize = cacheEntry.textSize
            textPaint.textSize = textSize

            val x = x0 + widthPerNode * 0.5f
            val dys = cacheEntry.dys

            for((index, text) in cacheEntry.lines.withIndex()){
                canvas.drawText(text, x, y0 + dys[index], textPaint)
            }

            canvas.restoreToCount(save)

        }

    }

    fun drawElementRaw(canvas: Canvas, x0: Float, y0: Float, delta: Float, widthPerNode: Float, margin: Boolean, group: Int, bgPaint: Paint, opacity: Float = 1f){
        drawElementRaw(canvas, x0, y0, delta, widthPerNode, if(margin) getMargin(widthPerNode) else 0f, group, bgPaint, opacity)
    }

    fun drawElementRaw(canvas: Canvas, x0: Float, y0: Float, delta: Float, widthPerNode: Float, margin: Float, group: Int, bgPaint: Paint, opacity: Float = 1f){

        bgPaint.color = if(group < 0) 0xff000000.toInt() else GroupColors[group]
        if(opacity < 1f) bgPaint.color = bgPaint.color.and(0xffffff) or ((opacity * 255).toInt().shl(24))
        val roundness = widthPerNode * 0.1f
        val d0 = margin - delta
        val d1 = widthPerNode - margin - margin + delta
        drawRoundRect(canvas,
            x0 + d0,
            y0 + d0,
            x0 + d1,
            y0 + d1,
            roundness, roundness, bgPaint)

    }

    private fun drawRoundRect(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float, rx: Float, ry: Float, bgPaint: Paint){
        fRect.left = left
        fRect.top = top
        fRect.right = right
        fRect.bottom = bottom
        canvas.drawRoundRect(fRect, rx, ry, bgPaint)
    }

}