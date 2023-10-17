package me.antonio.noack.elementalcommunity

import android.media.MediaPlayer

class Sound(id: Int, all: AllManager) {

    private val player = MediaPlayer.create(all, id)

    fun play() {
        pauseTime = 0
        player.seekTo(0)
        player.start()
    }

    fun destroy() {
        pauseTime = 0
        player.stop()
    }

    private var pauseTime = 0
    fun pause() {
        if (isPlaying()) {
            pauseTime = player.currentPosition
            player.pause()
        }
    }

    fun unpause() {
        if (pauseTime > 0) {
            player.seekTo(pauseTime)
            player.start()
            pauseTime = 0
        }
    }

    fun setVolume(volume: Float) {
        player.setVolume(volume, volume)
    }

    fun isPlaying() = pauseTime != 0 || player.isPlaying

    init {
        sounds.add(this)
    }

    companion object {
        private val sounds = ArrayList<Sound>()
        fun destroyAll() {
            for (sound in sounds) sound.destroy()
        }
    }
}