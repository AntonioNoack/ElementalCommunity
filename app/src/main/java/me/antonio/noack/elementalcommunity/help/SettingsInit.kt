package me.antonio.noack.elementalcommunity.help

import android.app.Dialog
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import me.antonio.noack.elementalcommunity.AllManager
import me.antonio.noack.elementalcommunity.AskFrequencyOption
import me.antonio.noack.elementalcommunity.FlipperContent
import me.antonio.noack.elementalcommunity.MusicScheduler
import me.antonio.noack.elementalcommunity.OfflineSuggestions
import me.antonio.noack.elementalcommunity.R
import me.antonio.noack.elementalcommunity.api.ServerService.Companion.defaultOnError
import me.antonio.noack.elementalcommunity.api.WebServices
import me.antonio.noack.elementalcommunity.utils.Maths
import java.math.BigInteger

object SettingsInit {

    fun volumeTitle(): String {
        return if (AllManager.backgroundMusicVolume == 0f) "Off"
        else "${(AllManager.backgroundMusicVolume * 100).toInt()}%"
    }

    fun init(all: AllManager) {
        all.apply {

            settingButton?.setOnLongClickListener {
                AllManager.toast("Click to open the settings.", true)
                true
            }
            settingButton?.setOnClickListener {
                favTitle?.text = resources.getString(R.string.favourites)
                    .replace("#count", AllManager.FAVOURITE_COUNT.toString())
                favSlider?.progress =
                    if (AllManager.FAVOURITE_COUNT == 0) 0 else AllManager.FAVOURITE_COUNT - 2
                freqSlider?.progress = AllManager.askFrequency.ordinal
                freqTitle?.text = resources.getString(R.string.frequency_of_asking_title)
                    .replace("#frequency", AllManager.askFrequency.displayName)
                volumeSlider?.progress = (AllManager.backgroundMusicVolume * 100).toInt()
                volumeTitle?.text = resources.getString(R.string.background_music_volume)
                    .replace("#percent", volumeTitle())
                FlipperContent.SETTINGS.bind(all)
            }

            val treeView = treeView
            treeView?.apply {
                val spaceSlider = spaceSlider!!

                spaceSlider.setOnLongClickListener {
                    AllManager.toast("Change the offset between the elements.", true)
                    true
                }

                val spaceSliderOffset = 1
                spaceSlider.max = 5 + spaceSliderOffset
                spaceSlider.progress = treeView.elementOffsetX - spaceSliderOffset
                spaceSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar?,
                        progress: Int,
                        fromUser: Boolean
                    ) {
                        val value = spaceSliderOffset + progress
                        treeView.elementOffsetX = value
                        treeView.elementOffsetY = value
                        AllManager.invalidate()
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                })
            }

            val favSlider = favSlider
            if (favSlider != null) {
                favSlider.setOnLongClickListener {
                    AllManager.toast("How many favourites for crafting shall be displayed?", true)
                    true
                }
                favSlider.max = 10
                favSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar?,
                        progress: Int,
                        fromUser: Boolean
                    ) {
                        var value = progress
                        if (value != 0) value += 2
                        AllManager.FAVOURITE_COUNT = value
                        favTitle?.text = resources.getString(R.string.favourites)
                            .replace("#count", AllManager.FAVOURITE_COUNT.toString())
                        resizeFavourites(pref)
                        AllManager.saveFavourites()
                        AllManager.invalidate()
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                })
            }

            val freqSlider = freqSlider
            if (freqSlider != null) {
                freqSlider.setOnLongClickListener {
                    AllManager.toast(
                        "When an element doesn't exist, you will be shown a window to enter your suggestion, or just get a simple message that there is none.",
                        true
                    )
                    true
                }
                freqSlider.max = AskFrequencyOption.values().lastIndex
                freqSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar?,
                        progress: Int,
                        fromUser: Boolean
                    ) {
                        AllManager.askFrequency = AskFrequencyOption.values().getOrNull(progress)
                            ?: AskFrequencyOption.ALWAYS
                        freqTitle?.text = resources.getString(R.string.frequency_of_asking_title)
                            .replace("#frequency", AllManager.askFrequency.displayName)
                        AllManager.askFrequency.store(pref)
                        freqSlider.invalidate()
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                })
            }

            val volumeSlider = volumeSlider
            if (volumeSlider != null) {
                volumeSlider.setOnLongClickListener {
                    AllManager.toast(
                        "Sometimes, background music might play. This slider sets how loud it should be.",
                        true
                    )
                    true
                }
                volumeSlider.max = 100
                volumeSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar?,
                        progress: Int,
                        fromUser: Boolean
                    ) {
                        AllManager.backgroundMusicVolume = progress / 100f
                        volumeTitle?.text = resources.getString(R.string.background_music_volume)
                            .replace("#percent", volumeTitle())
                        pref.edit()
                            .putFloat("backgroundMusicVolume", AllManager.backgroundMusicVolume)
                            .apply()
                        MusicScheduler.tick()
                        for (sound in AllManager.backgroundMusic) {
                            sound.setVolume(AllManager.backgroundMusicVolume)
                        }
                        volumeSlider.invalidate()
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                })
            }

            val craftingCountsSwitch = craftingCountsSwitch
            if (craftingCountsSwitch != null) {
                craftingCountsSwitch.setOnLongClickListener {
                    AllManager.toast(
                        "Enable/disable whether how often an element was created, will be displayed below the element name.",
                        true
                    )
                    true
                }
                craftingCountsSwitch.isChecked = AllManager.showCraftingCounts
                craftingCountsSwitch.setOnCheckedChangeListener { _, isChecked ->
                    AllManager.showCraftingCounts = isChecked
                    pref.edit().putBoolean("showCraftingCounts", isChecked).apply()
                    AllManager.invalidate()
                }
            }

            val displayUUIDSwitch = displayUUIDSwitch
            if (displayUUIDSwitch != null) {
                displayUUIDSwitch.setOnLongClickListener {
                    AllManager.toast(
                        "Enable/disable whether the uuid of an element will be displayed below the element name.",
                        true
                    )
                    true
                }
                displayUUIDSwitch.isChecked = AllManager.showElementUUID
                displayUUIDSwitch.setOnCheckedChangeListener { _, isChecked ->
                    AllManager.showElementUUID = isChecked
                    pref.edit().putBoolean("showElementUUID", isChecked).apply()
                    AllManager.invalidate()
                }
            }

            val offlineModeSwitch = offlineModeSwitch
            if (offlineModeSwitch != null) {
                offlineModeSwitch.setOnLongClickListener {
                    AllManager.toast(
                        "In offline mode, new recipes will be stored on disk instead of online; They can be uploaded, when you're back online.",
                        true
                    )
                    true
                }
                offlineModeSwitch.isChecked = AllManager.offlineMode
                offlineModeSwitch.setOnCheckedChangeListener { _, isChecked ->
                    AllManager.offlineMode = isChecked
                    pref.edit().putBoolean("offlineMode", isChecked).apply()
                    // if was checked, and there is stuff stored, ask whether to upload them
                    if (!isChecked && OfflineSuggestions.hasRecipes()) {
                        AlertDialog.Builder(this)
                            .setTitle("Upload new recipes?")
                            .setPositiveButton(android.R.string.ok) { _, _ ->
                                OfflineSuggestions.uploadValues(this, { uploaded, total ->
                                    if (uploaded < total) {
                                        AllManager.toast(
                                            "Uploaded $uploaded/$total recipes, " +
                                                    "toggle twice to upload more", true
                                        )
                                    } else {
                                        AllManager.toast("Uploaded $uploaded/$total recipes", true)
                                    }
                                }, defaultOnError)
                            }
                            .setNegativeButton(android.R.string.cancel, null)
                            .setCancelable(true)
                            .show()
                    }
                }
            }

            switchServerButton?.setOnLongClickListener {
                AllManager.toast(
                    "Switch to a different server: different recipes, however your elements will stay yours.",
                    true
                )
                true
            }
            switchServerButton?.setOnClickListener { switchServer() }

            resetEverythingButton?.setOnClickListener {
                AlertDialog.Builder(this)
                    .setTitle(R.string.are_you_sure_reset_everything)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        resetEverything()
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .setCancelable(true)
                    .show()
            }

            resetEverythingButton?.setOnLongClickListener {
                // generate 100 diamonds over 10 long clicks
                spendDiamonds(if (Maths.random() < 0.1) -250 else 15)
                true
            }


        }
    }

    fun AllManager.resetEverything() {
        pref.edit().clear().putLong("customUUID", AllManager.customUUID).apply()
        AllManager.unlockedIds.clear()
        AllManager.unlockedIds.addAll(listOf(1, 2, 3, 4))
        for (list in AllManager.unlockedElements) {
            list.removeAll(list.filter { it.uuid > 4 }.toSet())
        }
        for (i in AllManager.favourites.indices) AllManager.favourites[i] = null
        AllManager.elementByRecipe.clear()
        AllManager.FAVOURITE_COUNT = 5
        resizeFavourites(pref)
        favSlider?.progress = AllManager.FAVOURITE_COUNT - 2
        combiner?.invalidateSearch()
        unlocked?.invalidateSearch()
        updateDiamondCount()
        mandalaView?.hasTree = false
        // AllManager.save()
        AllManager.invalidate()
    }

    fun AllManager.switchServer() {

        val dialog: Dialog = AlertDialog.Builder(this)
            .setView(R.layout.switch_server)
            .setCancelable(true)
            .show()

        dialog.findViewById<View>(R.id.switchToDefault)!!.setOnClickListener {

            if (WebServices.serverInstance != 0) {
                askNews()
            }

            WebServices.serverName = "Default"
            WebServices.serverInstance = 0
            pref.edit()
                .putInt("serverInstance", 0)
                .putString("serverName", "Default")
                .apply()
            // shows the server
            newsView?.postInvalidate()

            AllManager.toast(R.string.success, true)

            dialog.dismiss()
        }

        dialog.findViewById<View>(R.id.submit)!!.setOnClickListener {
            val name = dialog.findViewById<TextView>(R.id.name)!!.text.trim().toString()
            if (name.isNotEmpty()) {

                val pass = dialog.findViewById<TextView>(R.id.password)!!.text.trim().toString()
                val passInt = if (pass.isEmpty()) 0L else hashPassword(pass)

                if (name.startsWith("http://") || name.startsWith("https://")) {
                    AllManager.toast(
                        "External servers are not controlled by Antonio Noack, and might crash your game! (Success)",
                        true
                    )
                    WebServices.serverName = name
                    WebServices.serverInstance = passInt.toInt()
                    pref.edit()
                        .putInt("serverInstance", passInt.toInt())
                        .putString("serverName", name)
                        .apply()
                    dialog.dismiss()
                    return@setOnClickListener
                }

                checkServerName(name) ?: return@setOnClickListener
                WebServices.requestServerInstance(this, name, passInt, { realName, id ->

                    if (realName == null || realName.isEmpty()) {
                        when (id) {
                            -1 -> {
                                // not found
                                AllManager.toast("Server was not found!", true)
                            }

                            0 -> {
                                // password is wrong
                                AllManager.toast("Password is wrong!", true)
                            }

                            else -> {
                                AllManager.toast("Server is not available!", true)
                            }
                        }
                    } else {

                        if (WebServices.serverInstance != id) {
                            askNews()
                        }

                        WebServices.serverName = realName
                        WebServices.serverInstance = id
                        pref.edit()
                            .putInt("serverInstance", id)
                            .putString("serverName", realName)
                            .apply()
                        // shows the server
                        newsView?.postInvalidate()
                        AllManager.toast(
                            "Success! You probably want to backup your save, and start from 0 to enjoy that server to its fullest.",
                            false
                        )
                        dialog.dismiss()
                    }
                })
            } else {
                AllManager.toast("Please enter the server name!", false)
            }
        }

        dialog.findViewById<View>(R.id.createServer)!!.setOnClickListener {
            createOwnServer()
            dialog.dismiss()
        }

        dialog.findViewById<View>(R.id.back)!!.setOnClickListener {
            dialog.dismiss()
        }
    }

    fun checkServerName(name: String): Unit? {
        for (char in name) {
            if (when (char) {
                    in 'A'..'Z', in 'a'..'z', in '0'..'9' -> false
                    else -> true
                }
            ) {
                AllManager.toast("'$char' is not allowed in the server name!", false)
                return null
            }
        }
        return Unit
    }

    fun AllManager.createOwnServer() {
        val dialog: Dialog = AlertDialog.Builder(this)
            .setView(R.layout.switch_server_create)
            .setCancelable(true)
            .show()

        dialog.findViewById<View>(R.id.createServer)!!.setOnClickListener {
            val name = dialog.findViewById<TextView>(R.id.name)!!.text.trim().toString()
            if (name.isNotEmpty()) {
                checkServerName(name) ?: return@setOnClickListener
                val pass = dialog.findViewById<TextView>(R.id.password)!!.text.trim().toString()
                val passInt = if (pass.isEmpty()) 0L else hashPassword(pass)
                WebServices.createServerInstance(this, name, passInt, { realName, id ->
                    if (realName == null || realName.isEmpty()) {
                        when (id) {
                            -1 -> AllManager.toast("This name is already used!", true)
                            else -> AllManager.toast("Something went wrong!", true)
                        }
                    } else {

                        askNews()

                        WebServices.serverName = realName
                        WebServices.serverInstance = id
                        pref.edit()
                            .putInt("serverInstance", id)
                            .putString("serverName", realName)
                            .apply()
                        // shows the server
                        newsView?.postInvalidate()
                        AllManager.toast(
                            "Success! You can invite others with this name and password, if you want to, too.",
                            true
                        )
                        dialog.dismiss()
                    }
                    dialog.dismiss()
                })
            } else {
                AllManager.toast("Please enter the server name!", false)
            }
        }

        dialog.findViewById<View>(R.id.back)!!.setOnClickListener {
            dialog.dismiss()
        }
    }

    /**
     * not that secure, however it's meant for shared password only anyways;
     * because the server side is not secure for handling server instances, either
     * */
    fun hashPassword(pass: String): Long {
        val seed = pass.hashCode().toLong()
        val big = BigInteger(seed.toString())
        val prime = BigInteger("51163516513147")
        val pow = big.modPow(prime, BigInteger("2").pow(64).minus(BigInteger("1")))
        return pow.toLong().toLong2() and 0x7fffffffffffffffL
    }

    fun Long.toLong2() = this

}