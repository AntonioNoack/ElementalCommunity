package me.antonio.noack.elementalcommunity.help

import android.app.Dialog
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import me.antonio.noack.elementalcommunity.AllManager
import me.antonio.noack.elementalcommunity.AskFrequencyOption
import me.antonio.noack.elementalcommunity.R
import me.antonio.noack.elementalcommunity.api.WebServices
import me.antonio.noack.elementalcommunity.utils.Maths
import java.math.BigInteger

object SettingsInit {

    fun init(all: AllManager){
        all.apply {

            settingButton.setOnLongClickListener {
                AllManager.toast("Click to open the settings.", true)
                true
            }
            settingButton.setOnClickListener {
                favTitle.text = resources.getString(R.string.favourites).replace("#count", AllManager.FAVOURITE_COUNT.toString())
                favSlider.progress = if(AllManager.FAVOURITE_COUNT == 0) 0 else AllManager.FAVOURITE_COUNT - 2
                freqSlider.progress = AllManager.askFrequency.ordinal
                freqTitle.text = resources.getString(R.string.frequency_of_asking_title).replace("#frequency", AllManager.askFrequency.displayName)
                flipper.displayedChild = 4
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
                spaceSlider.progress = tree.multiplierX - spaceSliderOffset
                spaceSlider.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        val tree = tree
                        val value = spaceSliderOffset + progress
                        tree.multiplierX = value
                        tree.multiplierY = value
                        AllManager.invalidate()
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                })
            }

            favSlider.setOnLongClickListener {
                AllManager.toast("How many favourites for crafting shall be displayed?", true)
                true
            }
            favSlider.max = 10
            favSlider.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    var value = progress
                    if(value != 0) value += 2
                    AllManager.FAVOURITE_COUNT = value
                    favTitle.text = resources.getString(R.string.favourites).replace("#count", AllManager.FAVOURITE_COUNT.toString())
                    resizeFavourites(pref)
                    AllManager.save()
                    AllManager.invalidate()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

            freqSlider.setOnLongClickListener {
                AllManager.toast("When an element doesn't exist, you will be shown a window to enter your suggestion, or just get a simple message that there is none.", true)
                true
            }
            freqSlider.max = AskFrequencyOption.values().lastIndex
            freqSlider.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    AllManager.askFrequency = AskFrequencyOption.values().getOrNull(progress) ?: AskFrequencyOption.ALWAYS
                    freqTitle.text = resources.getString(R.string.frequency_of_asking_title).replace("#frequency", AllManager.askFrequency.displayName)
                    AllManager.askFrequency.store(pref)
                    freqSlider.invalidate()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

            craftingCountsSwitch.setOnLongClickListener {
                AllManager.toast("Enable/disable whether how often an element was created, will be displayed below the element name.", true)
                true
            }
            craftingCountsSwitch.isChecked = AllManager.showCraftingCounts
            craftingCountsSwitch.setOnCheckedChangeListener { _, isChecked ->
                AllManager.showCraftingCounts = isChecked
                pref.edit().putBoolean("showCraftingCounts", isChecked).apply()
                AllManager.invalidate()
            }

            displayUUIDSwitch.setOnLongClickListener {
                AllManager.toast("Enable/disable whether the uuid of an element will be displayed below the element name.", true)
                true
            }
            displayUUIDSwitch.isChecked = AllManager.showElementUUID
            displayUUIDSwitch.setOnCheckedChangeListener { _, isChecked ->
                AllManager.showElementUUID = isChecked
                pref.edit().putBoolean("showElementUUID", isChecked).apply()
                AllManager.invalidate()
            }

            switchServerButton.setOnLongClickListener {
                AllManager.toast("Switch to a different server: different recipes, however your elements will stay yours.", true)
                true
            }
            switchServerButton.setOnClickListener { switchServer() }

            resetEverythingButton.setOnClickListener {
                AlertDialog.Builder(this)
                    .setTitle(R.string.are_you_sure_reset_everything)
                    .setPositiveButton(android.R.string.yes){ _, _ ->
                        pref.edit().clear().putLong("customUUID", AllManager.customUUID).apply()
                        AllManager.unlockedIds = hashSetOf(1, 2, 3, 4)
                        for(list in AllManager.unlockeds){ list.removeAll(list.filter { it.uuid > 4 }) }
                        for(i in 0 until AllManager.favourites.size) AllManager.favourites[i] = null
                        AllManager.FAVOURITE_COUNT = 5
                        resizeFavourites(pref)
                        favSlider.progress = AllManager.FAVOURITE_COUNT -2
                        combiner.invalidateSearch()
                        unlocked.invalidateSearch()
                        AllManager.save()
                        AllManager.invalidate()
                    }
                    .setNegativeButton(android.R.string.no, null)
                    .setCancelable(true)
                    .show()
            }

            resetEverythingButton.setOnLongClickListener {
                // generate 100 diamonds over 10 long clicks
                spendDiamonds(if(Maths.random() < 0.1) -250 else 15)
                true
            }


        }
    }

    fun AllManager.switchServer(){

        val dialog: Dialog = AlertDialog.Builder(this)
            .setView(R.layout.switch_server)
            .setCancelable(true)
            .show()

        dialog.findViewById<View>(R.id.switchToDefault)!!.setOnClickListener {

            if(WebServices.serverInstance != 0){
                askNews()
            }

            WebServices.serverName = "Default"
            WebServices.serverInstance = 0
            pref.edit()
                .putInt("serverInstance", 0)
                .putString("serverName", "Default")
                .apply()
            // shows the server
            newsView.postInvalidate()

            AllManager.toast(R.string.success, true)

            dialog.dismiss()
        }

        dialog.findViewById<View>(R.id.submit)!!.setOnClickListener {
            val name = dialog.findViewById<TextView>(R.id.name)!!.text.trim().toString()
            if(name.isNotEmpty()){

                val pass = dialog.findViewById<TextView>(R.id.password)!!.text.trim().toString()
                val passInt = if(pass.isEmpty()) 0L else hashPassword(pass)

                if(name.startsWith("http://") || name.startsWith("https://")){
                    AllManager.toast("External servers are not controlled by Antonio Noack, and might crash your game! (Success)", true)
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

                    if(realName == null || realName.isEmpty()){
                        when(id){
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

                        if(WebServices.serverInstance != id){
                            askNews()
                        }

                        WebServices.serverName = realName
                        WebServices.serverInstance = id
                        pref.edit()
                            .putInt("serverInstance", id)
                            .putString("serverName", realName)
                            .apply()
                        // shows the server
                        newsView.postInvalidate()
                        AllManager.toast("Success! You probably want to backup your save, and start from 0 to enjoy that server to its fullest.", false)
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
        for(char in name){
            if(when(char){
                    in 'A' .. 'Z', in 'a' .. 'z', in '0' .. '9' -> false
                    else -> true
                }){
                AllManager.toast("'$char' is not allowed in the server name!", false)
                return null
            }
        }
        return Unit
    }

    fun AllManager.createOwnServer(){
        val dialog: Dialog = AlertDialog.Builder(this)
            .setView(R.layout.switch_server_create)
            .setCancelable(true)
            .show()

        dialog.findViewById<View>(R.id.createServer)!!.setOnClickListener {
            val name = dialog.findViewById<TextView>(R.id.name)!!.text.trim().toString()
            if(name.isNotEmpty()) {
                checkServerName(name) ?: return@setOnClickListener
                val pass = dialog.findViewById<TextView>(R.id.password)!!.text.trim().toString()
                val passInt = if(pass.isEmpty()) 0L else hashPassword(pass)
                WebServices.createServerInstance(this, name, passInt, { realName, id ->
                    if(realName == null || realName.isEmpty()){
                        when(id){
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
                        newsView.postInvalidate()
                        AllManager.toast("Success! You can invite others with this name and password, if you want to, too.", true)
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

    fun String.toLong2(): Long {
        var value = 0L
        for(char in this){
            value = value * 10 + char.toInt() - '0'.toInt()
        }
        return value
    }


}