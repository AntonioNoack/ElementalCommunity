package me.antonio.noack.elementalcommunity

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.core.graphics.drawable.toDrawable

object Style {

    // todo background with texture instead of just colors?
    class Styling(val colors: IntArray) {
        fun replaceWith(color: Int, newStyle: Styling): Int {
            val index = colors.indexOf(color and 0xffffff)
            if (index !in newStyle.colors.indices) return color
            val newColor = newStyle.colors[index] and 0xffffff
            val oldAlpha = color.and(0xff shl 24)
            return newColor or oldAlpha
        }
    }

    const val PRIMARY = 0
    const val PRIMARY_DARK = 1
    const val PRIMARY_TEXT = 2
    const val DIAMOND = 3
    const val DIAMOND2 = 4
    const val GREEN = 5
    const val GREEN2 = 6
    const val ACCENT = 7
    const val WHITE = 8
    const val BLACK = 9
    const val PLUS_ARROW = 10

    val defaultStyle = Styling(
        intArrayOf(
            /* var colorPrimary = */0x2CA4EC,
            /* var colorPrimaryDark = */0x006096,
            /* var colorPrimaryText = */0x004268,
            /* var colorDiamond = */0x674280,
            /* var colorDiamond2 = */0xD1ADEC,
            /* var colorGreen = */0x285518,
            /* var colorGreen2 = */0xB5ECAD,
            /* var colorAccent = */0xF1C190,
            0xffffff,
            0x000000,
            0x777777
        )
    )

    val darkStyle = Styling(
        intArrayOf(
            0x183D64, 0x00141D, 0xeeeeee, 0x674280,
            0xD1ADEC, 0xB4C8B2, 0x384E36, 0xF1C190,
            0x000000, 0xffffff, 0xbbbbbb
        )
    )

    val lightStyle = Styling(
        intArrayOf(
            0xCEECFF, 0x4EC5FB, 0x019DF6, 0x9364B2,
            0xFFF3F3, 0x4D9D2E, 0xF7FFF2, 0xFBA656,
            0xffffff, 0x555555, 0x777777
        )
    )

    val neonStyle = Styling(
        intArrayOf(
            0x009EFF, 0x0A84BB, 0x004A72, 0xAF39FD,
            0xB671E4, 0x3FA419, 0xA7ED9B, 0xFF932C,
            0x000000, 0xffffff, 0xbbbbbb
        )
    )

    fun applyStyle(all: AllManager, oldStyle: Styling, newStyle: Styling) {
        applyStyle(all.flipper!!, oldStyle, newStyle)
    }

    fun applyStyle(dialog: AlertDialog, oldStyle: Styling, newStyle: Styling) {
        val view = dialog.findViewById<View>(R.id.dialog)
            ?: runCatching { dialog.listView }.getOrNull() ?: return
        applyStyle(view, oldStyle, newStyle)
    }

    fun applyStyle(view: View, oldStyle: Styling, newStyle: Styling) {
        if (oldStyle === newStyle) return
        if (view is TextView) {
            view.setTextColor(mapColorStateList(view.textColors, oldStyle, newStyle))
            if (view is EditText) {
                view.setHintTextColor(mapColorStateList(view.hintTextColors, oldStyle, newStyle))
            }
        }
        if (view is ImageView) {
            view.imageTintList = mapColorStateList(view.imageTintList, oldStyle, newStyle)
        }
        applyBackgroundColor(view, oldStyle, newStyle)
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                applyStyle(view.getChildAt(i), oldStyle, newStyle)
            }
        }
        if (view is RadioButton) {
            view.highlightColor = oldStyle.replaceWith(view.highlightColor, newStyle)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                view.outlineAmbientShadowColor =
                    oldStyle.replaceWith(view.outlineAmbientShadowColor, newStyle)
                view.outlineSpotShadowColor =
                    oldStyle.replaceWith(view.outlineSpotShadowColor, newStyle)
            }
            view.buttonTintList = mapColorStateList(view.buttonTintList, oldStyle, newStyle)
        }
        if (view is SeekBar) {
            view.progressTintList = mapColorStateList(view.progressTintList, oldStyle, newStyle)
            view.thumbTintList = mapColorStateList(view.thumbTintList, oldStyle, newStyle)
        }
        if (false && view is SwitchCompat) {
            view.thumbTintList = mapColorStateList(view.thumbTintList, oldStyle, newStyle)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            view.foregroundTintList = mapColorStateList(view.foregroundTintList, oldStyle, newStyle)
        }
        view.backgroundTintList = mapColorStateList(view.backgroundTintList, oldStyle, newStyle)

    }

    private fun applyBackgroundColor(view: View, oldStyle: Styling, newStyle: Styling) {
        val bg = view.background as? ColorDrawable ?: return
        val oldColor = bg.color
        val newColor = oldStyle.replaceWith(oldColor, newStyle)
        if (newColor != oldColor) {
            view.background = newColor.toDrawable()
        }
    }

    @SuppressLint("DiscouragedPrivateApi")
    fun mapColorStateList(
        original: ColorStateList?,
        oldStyle: Styling,
        newStyle: Styling
    ): ColorStateList? {
        original ?: return null
        return try {
            val stateSpecsField = ColorStateList::class.java.getDeclaredField("mStateSpecs")
            val colorsField = ColorStateList::class.java.getDeclaredField("mColors")

            stateSpecsField.isAccessible = true
            colorsField.isAccessible = true

            @Suppress("UNCHECKED_CAST")
            val stateSpecs = stateSpecsField.get(original) as Array<IntArray>
            val colors = colorsField.get(original) as IntArray

            // Modify the colors
            val newColors = IntArray(colors.size) { idx ->
                // For example, make all colors semi-transparent
                oldStyle.replaceWith(colors[idx], newStyle)
            }

            ColorStateList(stateSpecs, newColors)
        } catch (e: Exception) {
            e.printStackTrace()
            original
        }
    }

}