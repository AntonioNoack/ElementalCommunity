package me.antonio.noack.webdroid.files

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import me.antonio.noack.elementalcommunity.AllManager
import me.antonio.noack.elementalcommunity.R
import me.antonio.noack.elementalcommunity.io.SaveLoadLogic
import java.io.File
import java.util.*

object FileChooser {

    lateinit var folder: File

    fun requestFile(all: AllManager, mimeType: String, onSuccess: (String) -> Unit){
        select(all){
            onSuccess(it.readText())
        }
    }

    fun select(all: AllManager, onSelect: (file: File) -> Unit){

        if(ContextCompat.checkSelfPermission(all, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
            println("granted")
        } else {
            SaveLoadLogic.onWriteAllowed = {
                select(all, onSelect)
            }
            ActivityCompat.requestPermissions(all, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), SaveLoadLogic.WRITE_EXT_STORAGE_CODE)
            return
        }

        val res = all.resources
        val dialog = AlertDialog.Builder(all)
            .setView(R.layout.select_folder)
            .show()

        dialog.findViewById<View>(R.id.back)?.setOnClickListener { dialog.dismiss() }
        dialog.findViewById<TextView>(R.id.title)?.setText(R.string.please_select_the_file)

        folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        //all.getExternalFilesDir(Environment.DIRECTORY_DCIM)!!

        val button = dialog.findViewById<TextView>(R.id.select)!!
        val list = dialog.findViewById<ViewGroup>(R.id.fileList)!!
        val path = dialog.findViewById<TextView>(R.id.path)!!

        button.setOnClickListener {
            if(button.isEnabled){
                onSelect(folder)
                dialog.dismiss()
            }
        }

        navigateSelect(
            all,
            all.layoutInflater,
            path,
            list,
            button,
            res.getString(R.string.load_progress_from_name)
        )

    }

    @SuppressLint("SetTextI18n")
    fun navigateSelect(all: AllManager, inflater: LayoutInflater, pathView: TextView, list: ViewGroup, button: TextView, buttonTemplate: String){

        pathView.text = folder.absolutePath
        button.setText(R.string.please_select_a_file)
        button.isEnabled = false

        list.removeAllViews()

        if(folder.parentFile?.listFiles() != null){

            val entry = inflater.inflate(R.layout.select_file, list, false)
            val folder = folder

            entry.setOnClickListener {

                FileChooser.folder = folder.parentFile!!
                navigateSelect(
                    all,
                    inflater,
                    pathView,
                    list,
                    button,
                    buttonTemplate
                )

            }

            entry.findViewById<TextView>(R.id.title)?.text = "Parent Directory"
            list.addView(entry)

        }

        for(file in folder.listFiles()?.sortedBy { it.name.lowercase(Locale.getDefault()) } ?: emptyList()){

            if(!file.name.startsWith(".")){

                val entry = inflater.inflate(R.layout.select_file, list, false)

                entry.setOnClickListener {

                    folder = file

                    if(file.isDirectory){

                        navigateSelect(
                            all,
                            inflater,
                            pathView,
                            list,
                            button,
                            buttonTemplate
                        )

                    } else {

                        button.isEnabled = true
                        button.text = buttonTemplate.replace("#name", file.name)

                    }

                }

                val titleView = entry.findViewById<TextView>(R.id.title)
                titleView?.apply {
                    text = file.name
                    if(!file.isDirectory){
                        setTypeface(null, Typeface.ITALIC)
                        text = "$text " // because italic is broken, and doesn't consider the extra length
                    }
                }

                list.addView(entry)

            }

        }

    }

}