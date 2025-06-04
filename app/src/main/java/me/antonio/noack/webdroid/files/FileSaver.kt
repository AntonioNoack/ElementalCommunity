package me.antonio.noack.webdroid.files

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import androidx.core.app.ActivityCompat.startActivityForResult
import me.antonio.noack.elementalcommunity.AllManager


object FileSaver {

    val WRITE_REQUEST_CODE = 135
    var content = ""

    fun continueSave(all: AllManager, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK && data != null && content.isNotEmpty()) {
            val fos = all.contentResolver.openOutputStream(data.data ?: return) ?: return
            try {
                val bytes = content.toByteArray()
                fos.write(bytes)
                fos.flush()
                fos.close()
                println("Saved file :) with ${bytes.size} bytes")
                content = ""
            } catch (e: Exception) {
                fos.close()
                e.printStackTrace()
            }
        }
    }

    fun save(all: AllManager, fileName: String, mimeType: String, content: String) {
        FileSaver.content = content
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = mimeType
        intent.putExtra(Intent.EXTRA_TITLE, fileName)
        startActivityForResult(all, intent, WRITE_REQUEST_CODE, Bundle.EMPTY)
    }

}