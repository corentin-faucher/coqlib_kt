package com.coq.coqlib

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import androidx.annotation.RawRes
import java.lang.ref.WeakReference
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

object SoundManager {
    var isMute = false

    private var soundPool: SoundPool? = null
    private var audioManager: AudioManager? = null
    private val soundPoolIdOfRawRes = mutableMapOf<Int, Int>()
    private var ctx: WeakReference<Context> = WeakReference(null)

    fun initSound(@RawRes soundResId: Int) {
        if(soundPoolIdOfRawRes.containsKey(soundResId)) {
            printwarning("$soundResId deja init"); return }
        val ctx = SoundManager.ctx.get() ?: run { printerror("No context for sound."); return }
        soundPoolIdOfRawRes[soundResId] = soundPool?.load(ctx, soundResId,1) ?:
                run{
                    printerror("Ne peut charger le son $soundResId"); -1}
    }
    fun getSoundPoolId(@RawRes soundResId: Int) : Int {
        return soundPoolIdOfRawRes[soundResId] ?: throw Exception("Son non chargé.")
    }

    fun play(soundPoolID: Int, pitch: Short = 0, volume: Float = 1f) {
        if (isMute) return
        if ( soundPoolID <0) {
            printerror("Son pas loadé."); return}

        val maxVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 1
        val currVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
        val usedVolume = max(1f, min(0f, volume)) * currVolume.toFloat() / maxVolume.toFloat()

        soundPool?.play(soundPoolID, usedVolume, usedVolume, 1, 0, 2f.pow(pitch.toFloat()/12f))
    }

    fun playWithResId(@RawRes resID: Int, pitch: Short = 0, volume: Float = 1f) {
        soundPoolIdOfRawRes[resID]?.let { soundPoolID ->
            play(soundPoolID, pitch, volume)
        } ?: run{ printerror("Son $resID non chargé.") }
    }

    internal fun initWith(ctx: Context, extraSoundIds: IntArray?) {
        audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        SoundManager.ctx = WeakReference(ctx)
        // 1. Init du "SoundPool"
        soundPool = SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                        .build()
                )
                .build()

        soundPool?.setOnLoadCompleteListener { _, sampleID, status ->
            if(status != 0) {
                printerror("Ne peut charger le son $sampleID.")
            }
        }

        soundPoolIdOfRawRes.clear()

        // 2. Chargement des sons de base
        initSound(R.raw.arpeggio)
        initSound(R.raw.clap_clap)
        initSound(R.raw.duck_error)
        initSound(R.raw.fireworks)
        initSound(R.raw.go_start)
        initSound(R.raw.note_piano)
        initSound(R.raw.pouing_a)
        initSound(R.raw.pouing_b)
        initSound(R.raw.ready_set)
        initSound(R.raw.sheep)
        initSound(R.raw.ship_horn)
        initSound(R.raw.tac_tac)
        initSound(R.raw.type_writter)
        initSound(R.raw.woosh)
        extraSoundIds?.forEach { soundId ->
            initSound(soundId)
        }
    }
}