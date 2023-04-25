package me.antonio.noack.elementalcommunity.paint

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.math.MathUtils.clamp
import top.defaults.colorpicker.ColorPickerPopup
import top.defaults.colorpicker.ColorPickerPopup.ColorPickerObserver
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class PaintView(ctx: Context, attributeSet: AttributeSet) : View(ctx, attributeSet) {

    fun saveToBytes() {
        // todo
    }

    fun readFromBytes() {
        // todo
    }

    fun bytesToImage() {
        // todo
    }

    private val paint = Paint()

    init {
        paint.isAntiAlias = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        // colorBarTop = height >= width
        val fieldSize = max(width / fieldsOnX, height / fieldsOnY)
        setMeasuredDimension((fieldSize * fieldsOnX).toInt(), (fieldSize * fieldsOnY).toInt())
    }

    private var colorBarTop = false

    private val fieldsOnX = if (colorBarTop) size.toFloat() else (size + 1f + spacing)
    private val fieldsOnY = if (colorBarTop) (size + 1f + spacing) else size.toFloat()

    private var touchingField = false

    private var touchingColorIndex = 0
    private var touchX = 0
    private var touchY = 0

    fun tryToColor() {
        if (touchingField) {
            if (touchX in 0 until size && touchY in 0 until size) {
                image[touchX + touchY * size] = selectedColor.toByte()
                invalidate()
            }
        }
    }

    var downTime = 0L
    var movement = 0f
    var lastX = 0f
    var lastY = 0f

    private fun setLocked(locked: Boolean) {
        var par = parent
        while (par != null) {
            if (par is LockableScrollView) {
                par.scrollable = !locked
                return
            }
            par = par.parent
        }
    }

    init {
        setOnTouchListener { _, event ->
            val ex = event.x
            val ey = event.y
            touchingColorIndex = if (colorBarTop) {
                ex * colorCount / width
            } else {
                ey * colorCount / height
            }.toInt()
            var fieldX = ex * fieldsOnX / width
            var fieldY = ey * fieldsOnY / height
            if (colorBarTop) {
                fieldY -= 1f + spacing
            } else {
                fieldX -= 1f + spacing
            }
            touchX = fieldX.toInt()
            touchY = fieldY.toInt()
            println(event.action)
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    setLocked(true)
                    downTime = System.nanoTime()
                    lastX = ex
                    lastY = ey
                    movement = 0f
                    touchingField = if (colorBarTop) {
                        fieldY > 1f + spacing
                    } else {
                        fieldX > 1f + spacing
                    }
                    tryToColor()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    setLocked(false)
                    val deltaTime = System.nanoTime() - downTime
                    if (movement < width / 20f) {
                        if (deltaTime < 500e6) {
                            performClick()
                        } else {
                            performLongClick()
                        }
                    }
                    if (!touchingField) {
                        // select color
                        selectedColor = clamp(
                            touchingColorIndex, 0, colorCount - 1
                        )
                        invalidate()
                    }
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = ex - lastX
                    val dy = ey - lastY
                    movement += sqrt(dx * dx + dy * dy)
                    lastX = ex
                    lastY = ey
                    // draw if started inside field
                    tryToColor()
                    true
                }
                else -> false
            }
        }
        setOnLongClickListener {
            if (touchingField) {
                // clear color
                if (touchX in 0 until size && touchY in 0 until size) {
                    image[touchX + touchY * size] = 0
                    invalidate()
                }
            } else {
                val tci = touchingColorIndex
                if (tci in 1 until colorCount) {
                    // open color chooser
                    // then choose that color
                    ColorPickerPopup.Builder(context)
                        .initialColor(palette[tci] or black)
                        .enableBrightness(true)
                        .enableAlpha(false)
                        .okTitle("Choose")
                        .cancelTitle("Cancel")
                        .showIndicator(true)
                        .showValue(true)
                        .build()
                        .show(this, object : ColorPickerObserver() {
                            override fun onColorPicked(color: Int) {
                                palette[tci] = color or black
                                this@PaintView.invalidate()
                            }
                        })
                }
            }
            true
        }
    }

    override fun draw(canvas: Canvas?) {
        super.draw(canvas)
        canvas ?: return

        // todo draw the color bar
        // todo draw the field itself

        // todo draw the selected color

        paint.color = primaryText
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        val fieldSize = min(width / fieldsOnX, height / fieldsOnY)

        // draw color bar
        if (colorBarTop) {
            paint.color = primary
            canvas.drawRect(0f, 0f, width + 0f, fieldSize, paint)
            for (i in 0 until colorCount) {
                drawColor(
                    canvas,
                    fieldSize * i, 0f,
                    fieldSize, fieldSize,
                    palette[i], i == selectedColor
                )
            }
        } else {
            paint.color = primary
            canvas.drawRect(0f, 0f, fieldSize, height + 0f, paint)
            for (i in 0 until colorCount) {
                drawColor(
                    canvas,
                    0f, fieldSize * i,
                    fieldSize, fieldSize,
                    palette[i], i == selectedColor
                )
            }
        }

        // draw image itself
        val x0 = if (colorBarTop) 0f else fieldSize * (1f + spacing)
        val y0 = if (colorBarTop) fieldSize * (1f + spacing) else 0f
        for (yi in 0 until size) {
            for (xi in 0 until size) {
                val color = palette[image[xi + yi * size].toInt()]
                drawColor(
                    canvas, x0 + xi * fieldSize, y0 + yi * fieldSize,
                    fieldSize, fieldSize, color, false
                )
            }
        }

    }

    var padding = 4f

    fun drawColor(
        canvas: Canvas,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        color: Int,
        isSelected: Boolean
    ) {
        val p = padding
        if (isSelected) {
            paint.color = accent2 or black
            canvas.drawCircle(x + w / 2, y + h / 2, w / 2, paint)
        }
        // caro, when transparent
        if (color.ushr(255) > 0) {
            paint.color = color
            val r = if (isSelected) w / 2.5f else w / 2.2f
            canvas.drawCircle(x + w / 2, y + h / 2, r, paint)
        }
    }

    // palette[0] is always transparent
    val palette = IntArray(colorCount) { defaultPalette.getOrElse(it) { black } }
    var selectedColor = 1
    val image = ByteArray(size * size)

    companion object {
        private val spacing = 0.2f
        val black = 255 shl 24
        val defaultPalette = intArrayOf(
            0, -1, black,
            0xffff shl 16, // red
            0xff00ff shl 8, // green
            (255 shl 24) or 255 // blue
        )
        val size = 8
        val colorCount = size
        val accent2 = 0xF37A02 or black
        val primary = 0x2CA4EC or black
        val primaryDark = 0x006096 or black
        val primaryText = 0x004268 or black
    }

}