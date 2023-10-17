package me.antonio.noack.webdroid.files

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat
import me.antonio.noack.elementalcommunity.AllManager
import me.antonio.noack.elementalcommunity.R
import me.antonio.noack.elementalcommunity.io.SaveLoadLogic
import java.io.IOException
import java.io.OutputStream


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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

            FileSaver.content = content
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = mimeType
            intent.putExtra(Intent.EXTRA_TITLE, fileName)
            startActivityForResult(all, intent, WRITE_REQUEST_CODE, Bundle.EMPTY)

        } else {

            if (ContextCompat.checkSelfPermission(
                    all,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                println("requesting write permissions")
                SaveLoadLogic.onWriteAllowed = { SaveLoadLogic.save(all, mimeType, fileName) }
                ActivityCompat.requestPermissions(
                    all, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    SaveLoadLogic.WRITE_EXT_STORAGE_CODE
                )
                return
            }

            val contentValues = ContentValues()
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, mimeType)

            val resolver = all.contentResolver
            var stream: OutputStream? = null
            var uri: Uri? = null
            try {

                val contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                uri = resolver.insert(contentUri, contentValues)
                if (uri == null) {
                    throw IOException("Failed to create new MediaStore record.")
                }
                stream = resolver.openOutputStream(uri)
                if (stream == null) {
                    throw IOException("Failed to get output stream.")
                }
                stream.write(content.toByteArray())
                stream.close()
                stream = null
                AllManager.toast(R.string.success, false)
                println(contentUri)
            } catch (e: IOException) {
                if (uri != null) {
                    resolver.delete(uri, null, null)
                }
                throw e
            } finally {
                stream?.close()
            }
        }


    }

}