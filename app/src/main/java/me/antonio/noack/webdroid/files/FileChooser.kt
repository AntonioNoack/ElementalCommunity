package me.antonio.noack.webdroid.files

import android.content.Intent
import android.os.Bundle
import androidx.core.app.ActivityCompat
import me.antonio.noack.elementalcommunity.AllManager

object FileChooser {

    val READ_REQUEST_CODE = 771

    fun requestFile(all: AllManager, mimeType: String) {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = mimeType
        ActivityCompat.startActivityForResult(
            all, intent, READ_REQUEST_CODE,
            Bundle.EMPTY
        )
    }
}