package me.antonio.noack.elementalcommunity

import kotlin.random.Random

object MusicScheduler {

    private var lastTime = 0L
    fun tick() {
        if (AllManager.backgroundMusic.any {
                it.isPlaying()
            } || AllManager.backgroundMusicVolume == 0f) return
        val time = System.nanoTime()
        if (time - lastTime > 30e9 * Random.nextDouble()) {
            val sound = AllManager.backgroundMusic.randomOrNull() ?: return
            sound.setVolume(AllManager.backgroundMusicVolume)
            sound.play()
        }
        lastTime = time
    }

    fun pause() {
        for (sound in AllManager.backgroundMusic) {
            sound.pause()
        }
    }

    fun unpause() {
        for (sound in AllManager.backgroundMusic) {
            sound.unpause()
        }
    }
}