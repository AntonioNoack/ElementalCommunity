package me.antonio.noack.webdroid.files

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import me.antonio.noack.elementalcommunity.AllManager
import me.antonio.noack.elementalcommunity.R
import me.antonio.noack.elementalcommunity.io.SaveLoadLogic
import java.io.IOException
import java.io.OutputStream

object FileSaver {

    fun getFolder() = if(Build.VERSION.SDK_INT >= 19) Environment.DIRECTORY_DOCUMENTS else Environment.DIRECTORY_DOWNLOADS

    fun save(all: AllManager, fileName: String, mimeType: String, content: String){

        if(ContextCompat.checkSelfPermission(all, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
            println("granted")
        } else {
            SaveLoadLogic.onWriteAllowed = { SaveLoadLogic.save(all, mimeType, fileName) }
            ActivityCompat.requestPermissions(all, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                SaveLoadLogic.WRITE_EXT_STORAGE_CODE
            )
            return
        }

        val relativeLocation = getFolder()
        val contentValues = ContentValues()
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, mimeType)

        if(Build.VERSION.SDK_INT >= 29){
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, relativeLocation)
        }

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