package me.antonio.noack.elementalcommunity.io

import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import me.antonio.noack.elementalcommunity.AllManager
import me.antonio.noack.elementalcommunity.AllManager.Companion.toast
import me.antonio.noack.elementalcommunity.R
import me.antonio.noack.elementalcommunity.api.WebServices
import me.antonio.noack.elementalcommunity.cache.CombinationCache
import me.antonio.noack.webdroid.files.FileChooser
import me.antonio.noack.webdroid.files.FileSaver
import java.net.URLEncoder
import kotlin.concurrent.thread


object SaveLoadLogic {

    private val ignore = { key: String ->
        key.endsWith(".group") || key.endsWith(".name") || key.endsWith(".crafted")
    }

    fun init(all: AllManager) {

        val clearRecipeCacheView = all.findViewById<View>(R.id.clearRecipeCache)
        clearRecipeCacheView?.setOnLongClickListener {
            toast("The cache is updated automatically after one hour.", false)
            true
        }

        clearRecipeCacheView?.setOnClickListener {
            val edit = all.pref.edit()
            CombinationCache.invalidate(edit)
            edit.apply()
            toast("Cleared Recipe Cache!", false)
        }

        all.findViewById<View>(R.id.saveProgress)?.setOnClickListener {

            val dialog = AlertDialog.Builder(all)
                .setView(R.layout.progress_save)
                .show()

            dialog.findViewById<View>(R.id.save)?.setOnClickListener {
                save(all)
                dialog.dismiss()
            }

            dialog.findViewById<View>(R.id.upload)?.setOnClickListener {
                upload(all)
                dialog.dismiss()
            }

            dialog.findViewById<View>(R.id.back)?.setOnClickListener {
                dialog.dismiss()
            }

        }

        all.findViewById<View>(R.id.loadProgress)?.setOnClickListener {

            val dialog = AlertDialog.Builder(all)
                .setView(R.layout.progress_load)
                .show()

            dialog.findViewById<View>(R.id.load)?.setOnClickListener {
                load(all)
                dialog.dismiss()
            }

            dialog.findViewById<View>(R.id.download)?.setOnClickListener {
                download(all)
                dialog.dismiss()
            }

            dialog.findViewById<View>(R.id.back)?.setOnClickListener {
                dialog.dismiss()
            }

        }


    }

    fun load(all: AllManager) {
        FileChooser.requestFile(all, "text/plain") {
            applyDownload(all, it)
        }
    }

    fun save(all: AllManager) {
        thread {
            save(all, "text/plain", "elementalBackup.txt")
        }
    }

    const val WRITE_EXT_STORAGE_CODE = 1561
    var onWriteAllowed: (() -> Unit)? = null

    fun save(all: AllManager, mimeType: String, displayName: String) {
        try {
            FileSaver.save(all, displayName, mimeType, Saver.save(all.pref, ignore))
        } catch (e: Exception) {
            toast("$e", false)
            e.printStackTrace()
        }
    }

    private fun download(all: AllManager) {

        // download from server -> ask password :D
        val dialog = AlertDialog.Builder(all)
            .setView(R.layout.ask_password)
            .show()

        dialog.findViewById<View>(R.id.ok)?.setOnClickListener {
            download(all, dialog.findViewById<TextView>(R.id.password)!!.text.toString())
            dialog.dismiss()
        }

        dialog.findViewById<View>(R.id.back)?.setOnClickListener {
            dialog.dismiss()
        }

    }

    private fun download(all: AllManager, password: String) {

        toast("loading...", false)

        // download from server
        thread {
            WebServices.tryCaptcha(all, "load=$password", { data ->
                if (data.isNotBlank() && data != "#404") {

                    if (data.startsWith("#ip")) {

                        toast("You need to use the same network (ip)!", true)

                    } else {

                        applyDownload(all, data)

                    }

                } else toast("Save not found", true)
            })
        }

    }

    fun applyDownload(all: AllManager, data: String) {

        all.runOnUiThread {

            val dialog = AlertDialog.Builder(all)
                .setView(R.layout.ask_override)
                .show()

            dialog.findViewById<View>(R.id.copy)?.setOnClickListener {
                Loader.load(data, all.pref, true)
                toast(R.string.success, true)
                all.loadEverythingFromPreferences()
                dialog.dismiss()
            }

            dialog.findViewById<View>(R.id.merge)?.setOnClickListener {
                Loader.load(data, all.pref, false)
                toast(R.string.success, true)
                all.loadEverythingFromPreferences()
                dialog.dismiss()
            }

            dialog.findViewById<View>(R.id.back)?.setOnClickListener {
                dialog.dismiss()
            }

        }

    }

    private fun upload(all: AllManager) {

        thread {

            val data = Saver.save(all.pref, ignore)

            toast("loading...", false)

            WebServices.tryCaptchaLarge(
                all,
                "save=${URLEncoder.encode(data, "UTF-8")}",
                "u=${AllManager.customUUID}",
                { password ->
                    all.runOnUiThread {

                        val dialog = AlertDialog.Builder(all)
                            .setView(R.layout.show_password)
                            .show()

                        dialog.findViewById<TextView>(R.id.password)!!.text = password
                        dialog.findViewById<View>(R.id.ok)?.setOnClickListener {
                            dialog.dismiss()
                        }

                    }
                })

        }

    }

}