package me.antonio.noack.elementalcommunity

import android.graphics.*
import androidx.core.math.MathUtils.clamp
import kotlin.math.*

object GroupsEtc {

    val GroupColors: IntArray
    val GroupSizes: IntArray

    const val minimumCraftingCount = 1
    var rainbowColor = 0x123456
    var rainbowIndex = -1

    fun tick(){
        time = System.nanoTime()
        val timeout = 5 * 1000000000L
        for((_, cache) in cacheBySize.entries){
            if(cache.map.isNotEmpty() && abs(cache.lastTime - time) > timeout){
                // println("cleared ${cache.size}")
                cache.map.clear()
            }// else println("${abs(cache.lastTime - time)}")
        }
    }

    fun brightness(color: Int): Float {
        val r = (color shr 16) and 255
        val g = (color shr 8) and 255
        val b = color and 255
        return (0.299f*r + 0.587f*g + 0.114f*b)/255f
    }

    val hues = intArrayOf(0, 31, 51, 110, 178, 208, 233, 277, 304, -1, -2).map { x -> x * 1f }
    val saturations = intArrayOf(22, 60, 85).map { x -> x / 100f }
    val rainbowColors: IntArray

    init {

        // Helligkeitskorrektur :D

        val hue0 = 288
        // val values = intArrayOf(81, 82, 69, 37).map { x -> x / 100f }
        val values = intArrayOf(100, 100, 85, 70).map { x -> x / 100f }
        val brightness0 = FloatArray(saturations.size)

        GroupColors = IntArray(hues.size * saturations.size)
        GroupSizes = IntArray(GroupColors.size)

        val hsv = FloatArray(3){ hue0 * 1f }
        for(i in 0 until saturations.size){
            hsv[0] = hue0 * 1f
            hsv[1] = saturations[i]
            hsv[2] = values[i]
            brightness0[i] = brightness(Color.HSVToColor(hsv))
        }

        for(i in 0 until hues.size){
            hsv[0] = hues[i]
            if(hsv[0] < -1.5f){
                val id = i * saturations.size
                GroupColors[id + 0] = 0xffffffff.toInt()
                GroupColors[id + 1] = 0xff000000.toInt() or rainbowColor
                GroupColors[id + 2] = 0xff000000.toInt()
                rainbowIndex = id + 1
            } else if(hsv[0] < -0.5f){
                val id = i * saturations.size
                GroupColors[id + 0] = 0xffdddddd.toInt()
                GroupColors[id + 1] = 0xffbbbbbb.toInt()
                GroupColors[id + 2] = 0xff888888.toInt()
            } else {
                for(j in 0 until saturations.size){
                    val id = i * saturations.size + j
                    hsv[1] = saturations[j]
                    hsv[2] = 1f - j * j * .33f * .33f + j * .33f * .25f
                    // tempArray[2] = MathUtils.clamp(values[j] * brightness0[j] / brightness(Color.HSVToColor(tempArray)), 0f, 1f)
                    GroupColors[id] = Color.HSVToColor(hsv)
                }
            }
        }

        val hueOffset = 360f - 50f
        val rainbowBrightness = 1f
        val rainbowSaturation = 1f
        val rainbowDegrees = 360f
        rainbowColors = intArrayOf(0xdd0000, 0xfe6230, 0xfef600, 0x00bb00, 0x009bfe, 0x0808a0, 0x522fa0)
        /*IntArray(256){
            hsv[0] = (hueOffset + it * rainbowDegrees / 255f) % 360f
            hsv[1] = rainbowSaturation
            hsv[2] = rainbowBrightness
            normalizeColor(Color.HSVToColor(hsv), rainbowBrightness)
        }*/


    }

    fun normalizeColor(color: Int, brightness: Float): Int {
        val multiplier = brightness / brightness(color)
        return rgb(
            (r(color) * multiplier + 0.5f).toInt(),
            (g(color) * multiplier + 0.5f).toInt(),
            (b(color) * multiplier + 0.5f).toInt())
    }

    fun r(c: Int) = c.shr(16).and(255)
    fun g(c: Int) = c.shr(8).and(255)
    fun b(c: Int) = c.and(255)
    fun rgb(r: Int, g: Int, b: Int) = 0xff000000.toInt() or clamp(r, 0, 255).shl(16) or clamp(g, 0, 255).shl(8) or clamp(b, 0, 255)

    private val fRect = RectF()

    fun getMargin(widthPerNode: Float): Float = widthPerNode * marginFactor

    fun drawElement(canvas: Canvas, drawCount: Boolean, drawUUID: Boolean, x0: Float, y0: Float, delta: Float, widthPerNode: Float, margin: Boolean, element: Element,
                    bgPaint: Paint, textPaint: Paint){
        drawElement(canvas,
            if(drawCount) element.craftingCount else -1,
            x0, y0, delta, widthPerNode, margin, element.name, element.group,
            if(drawUUID) element.uuid else -1,
            bgPaint, textPaint)
    }

    fun drawFavourites(canvas: Canvas, drawCount: Boolean, drawUUID: Boolean, width: Float, height: Float, bgPaint: Paint, textPaint: Paint, allowLeftFavourites: Boolean){

        textPaint.alpha = 127
        bgPaint.alpha = 127

        if(width > height && allowLeftFavourites){// on the left
            val fWidth = min(height / AllManager.FAVOURITE_COUNT, width * AllManager.MAX_FAVOURITES_RATIO)
            for((index, favourite) in AllManager.favourites.withIndex()){
                if(favourite != null) drawElement(canvas, drawCount, drawUUID, 0f,index*fWidth, 0f, fWidth, true, favourite, bgPaint, textPaint)
                else drawElement(canvas, -1, 0f, index*fWidth, 0f, fWidth, true, "", -1, -1, bgPaint, textPaint)
            }
        } else {// on the bottom
            val fWidth = min(width / AllManager.FAVOURITE_COUNT, height * AllManager.MAX_FAVOURITES_RATIO)
            val baseY = height - fWidth
            for((index, favourite) in AllManager.favourites.withIndex()){
                if(favourite != null) drawElement(canvas, drawCount, drawUUID, index*fWidth, baseY, 0f, fWidth, true, favourite, bgPaint, textPaint)
                else drawElement(canvas, -1, index*fWidth, baseY, 0f, fWidth, true, "", -1, -1, bgPaint, textPaint)
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

    /**
     * splits a string at the delimiter, or anywhere, it it's too long
     * */
    fun String.longSplitUntilCount(delimiter: Char, limit: Int, count: Int): List<String> {
        var parts = split(delimiter)
        while(parts.size < count){
            val maxPart = parts.withIndex().maxByOrNull { it.value.length }!!
            // todo split preferably at .,*- or something like that?
            val maxString = maxPart.value
            if(maxString.length > limit){
                // println("gonna split this string, $maxString")
                val centerIndex = maxString.length/(count+1-parts.size)
                parts = parts.subList(0, maxPart.index) + listOf(
                    maxString.substring(0, centerIndex), maxString.substring(centerIndex)
                ) + parts.subList(maxPart.index+1, parts.size)
            } else break
        }
        // println("split $this into $parts")
        return parts
    }

    fun splitText(text: String, sideRatio: Float, textPaint: Paint): List<String> {
        if(sideRatio < 4f) return listOf(text)
        val parts = text.longSplitUntilCount(' ', 32, 3)
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
        val index = (size * 3).toInt()
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

    fun getCacheEntry(rawText: String, key: String, craftingCount: Int, widthPerNode: Float, textPaint: Paint, bgPaint: Paint): CacheEntry {
        /*if(lastCacheWidth != widthPerNode){
            println("clear cache, $lastCacheWidth != $widthPerNode")
            cacheEntries.clear()
            lastCacheWidth = widthPerNode
            // println("cleared")
        }*/
        val cacheEntries = getCache(widthPerNode)
        var entry = cacheEntries[key]
        if(entry == null){

            val text = rawText.trim()

            // split long rawText into multiple sections...
            textPaint.textSize = 20f
            val width0 = textPaint.measureText(text)

            val color = if(brightness(bgPaint.color) > 0.3f) 0xff000000.toInt() else -1

            val sideRatio = width0 / textPaint.textSize
            val list = splitText(text, sideRatio, textPaint)

            var width1 = textPaint.measureText(list[0])
            for(i in 1 until list.size){
                width1 = max(width1, textPaint.measureText(list[i]))
            }

            val textSize = clamp(textPaint.textSize * widthPerNode * 0.8f / width1, widthPerNode*0.02f, widthPerNode * (0.8f - spacingFactorX2 * countSize) / max(1, list.size))
            textPaint.textSize = textSize
            val textDy = (widthPerNode - (textPaint.ascent() + textPaint.descent()))/2 - if(craftingCount > minimumCraftingCount) 0.5f * widthPerNode * countSize else 0f

            entry = CacheEntry(textSize, textDy, color, list)

            cacheEntries[key] = entry

        }

        return entry
    }

    private const val marginFactor = 0.02f
    private const val countSize = 0.2f
    private const val counterOffset = 0.85f
    private const val spacingFactorX0 = 0.2f
    private const val spacingFactor = 1f + spacingFactorX0
    private const val spacingFactorX2 = 1f + 2f * spacingFactorX0
    class CacheEntry(val textSize: Float, dy0: Float, val color: Int, val lines: List<String>){
        private val lineOffset = textSize * spacingFactor
        private val indexOffset = (lines.size - 1) * 0.5f
        val dys = FloatArray(lines.size){ dy0 + (it - indexOffset) * lineOffset }
    }

    fun drawElement(canvas: Canvas, craftingCount: Int, x0: Float, y0: Float, delta: Float, widthPerNode: Float, margin: Boolean,
                    rawName: String,
                    group: Int, uuid: Int,
                    bgPaint: Paint, textPaint: Paint){

        drawElementBackground(canvas, x0, y0, delta, widthPerNode, margin, group, bgPaint)

        val alpha = textPaint.alpha
        val cacheKey = if(craftingCount <= minimumCraftingCount) rawName else "$rawName ($craftingCount)"
        val cacheEntry = getCacheEntry(rawName, cacheKey, craftingCount, widthPerNode, textPaint, bgPaint)
        val color = cacheEntry.color
        textPaint.color = color
        textPaint.alpha = alpha

        var save = -1
        if(delta != 0f) {

            save = canvas.save()

            val zoom = (widthPerNode + delta + delta) / widthPerNode
            val half = widthPerNode * 0.5f
            canvas.scale(zoom, zoom, x0 + half, y0 + half)

        }

        val x = x0 + widthPerNode * 0.5f

        val textSize = cacheEntry.textSize
        textPaint.textSize = textSize
        val dys = cacheEntry.dys

        val lines = cacheEntry.lines
        for((index, text) in lines.withIndex()){
            canvas.drawText(text, x, y0 + dys[index], textPaint)
        }

        if(craftingCount > minimumCraftingCount || uuid > -1){
            textPaint.color = multiplyAlpha(color, 0.3f)
            textPaint.textSize = widthPerNode * countSize
            val text = if(craftingCount > minimumCraftingCount) if(uuid > -1) "($craftingCount, #$uuid)" else "($craftingCount)" else "(#$uuid)"
            textPaint.textSize *= 0.7f
            textPaint.textSize *= min(1f, widthPerNode * 0.7f / textPaint.measureText(text))
            canvas.drawText(text, x, y0 + max(dys.last() + spacingFactor * countSize * widthPerNode, widthPerNode * counterOffset), textPaint)
        }

        if(delta != 0f){

            canvas.restoreToCount(save)

        }

    }

    fun multiplyAlpha(color: Int, multiplier: Float): Int {
        return color.and(0xffffff) or (clamp(color.shr(24).and(255) * multiplier, 0f, 255f).toInt().shl(24))
    }

    fun drawElementBackground(canvas: Canvas, x0: Float, y0: Float, delta: Float, widthPerNode: Float, margin: Boolean, group: Int, bgPaint: Paint, opacity: Float = 1f){
        drawElementBackground(canvas, x0, y0, delta, widthPerNode, if(margin) getMargin(widthPerNode) else 0f, group, bgPaint, opacity)
    }

    fun bowLength(float: Float) = if(float > 1f) 0f else sqrt(1f - float * float)

    const val relativeRoundness = 0.1f
    fun drawElementBackground(canvas: Canvas, x0: Float, y0: Float, delta: Float, widthPerNode: Float, margin: Float, group: Int, bgPaint: Paint, opacity: Float = 1f){

        val alpha = bgPaint.alpha

        val color = GroupColors.getOrNull(group) ?: 0xff000000.toInt()
        bgPaint.color = color
        bgPaint.alpha = alpha

        if(opacity < 1f) bgPaint.color = bgPaint.color.and(0xffffff) or ((opacity * 255).toInt().shl(24))
        val roundness = widthPerNode * relativeRoundness
        val d0 = margin - delta
        val d1 = widthPerNode - margin - margin + delta

        if(group == rainbowIndex){

            val alpha = clamp(opacity, 0f, 1f).times(255f).toInt().shl(24)
            if(relativeRoundness * rainbowColors.size > 1f){

                // calculate the rainbow...
                val x1 = (x0 + d0).toInt()
                val x2 = (x0 + d1).toInt()
                val y1 = y0 + d0 + roundness
                val y2 = y0 + d1 - roundness
                val fDelta = (rainbowColors.size - 0.0001f) / (x2 - x1)
                // do we go top down or diagonal? -> top down
                val roundness2 = x2 - roundness
                for(x in x1 .. x2){
                    val index = x - x1
                    val rainbowIndex = index * fDelta
                    val bowIndent = if(index < roundness){
                        // calculate the size
                        acos(x / roundness * PI)
                        roundness * bowLength(1f - index / roundness)
                    } else if(x < roundness2){
                        // full width
                        roundness
                    } else {
                        // calculate the size
                        roundness * bowLength(1f - (x2 - x) / roundness)
                    }
                    val y3 = y1 - bowIndent
                    val y4 = y2 + bowIndent
                    val xf = x.toFloat()
                    bgPaint.color = rainbowColors[rainbowIndex.toInt()] or alpha
                    canvas.drawRect(xf, y3, xf+1f, y4, bgPaint)
                }

            } else {

                // calculate the rainbow:
                // first draw the first stripe, then the last, then the inner ones
                val width = d1 - d0
                val perStripe = width / rainbowColors.size

                val save = canvas.save()
                canvas.translate(x0 + d0, y0 + d0)

                bgPaint.color = rainbowColors[0] or alpha
                drawRoundRect(canvas, 0f, 0f, perStripe*2, width, roundness, roundness, bgPaint)

                bgPaint.color = rainbowColors.last() or alpha
                drawRoundRect(canvas, width-2*perStripe, 0f, width, width, roundness, roundness, bgPaint)

                var x1 = perStripe
                for(i in 1 until rainbowColors.size-1){
                    val x2 = (i+1) * perStripe
                    bgPaint.color = rainbowColors[i] or alpha
                    canvas.drawRect(x1, 0f, x2, width, bgPaint)
                    x1 = x2
                }

                canvas.restoreToCount(save)

            }

            bgPaint.color = -1 // force black letters, those are a little better readable

        } else {

            drawRoundRect(canvas,
                x0 + d0,
                y0 + d0,
                x0 + d1,
                y0 + d1,
                roundness, roundness, bgPaint)

        }

    }

    private fun drawRoundRect(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float, rx: Float, ry: Float, bgPaint: Paint){
        fRect.left = left
        fRect.top = top
        fRect.right = right
        fRect.bottom = bottom
        canvas.drawRoundRect(fRect, rx, ry, bgPaint)
    }

}