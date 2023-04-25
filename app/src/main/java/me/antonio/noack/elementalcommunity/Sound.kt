package me.antonio.noack.elementalcommunity

import android.media.MediaPlayer

class Sound(id: Int, all: AllManager){

    private val player = MediaPlayer.create(all, id)

    fun play(){
        player.seekTo(0)
        player.start()
    }

    fun destroy(){
        player.stop()
    }

    companion object {
        private val sounds = ArrayList<Sound>()
        fun destroyAll(){
            for(sound in sounds) sound.destroy()
        }
    }
}