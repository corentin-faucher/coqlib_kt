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
    var rate: Float = 0.5f  // Vitesse du débit de la parole. (entre 0 et 1)

    private var soundPool: SoundPool? = null
    private var audioManager: AudioManager? = null
    private val soundPoolIdOfRawRes = mutableMapOf<Int, Int>()
    private val volumeIdOfSoundPoolId = mutableMapOf<Int, Int>()
    private var ctx: WeakReference<Context> = WeakReference(null)
    /** Les volumes pour différentes sortes de sons,
     * e.g. "error sounds",  "monster sounds", "background sounds", ... */
    private var volumes: FloatArray = FloatArray(16) { 1f }

    fun getVolume(id: Int) : Float {
        if(id >= volumes.size) {
            printerror("Overflow volumeId $id.")
            return 0f
        }
        return volumes[id]
    }
    fun getAllVolumes() : FloatArray = volumes
    fun setVolume(newVolume: Float, id: Int) {
        if(id >= volumes.size)
            printerror("Overflow volumeId $id.")
        volumes[id] = max(0f, min(1f, newVolume))
        printdebug("volume id $id, set to ${volumes[id]}.")
    }
    fun setAllVolumes(newVolumes: FloatArray) {
        val count = min(volumes.size, newVolumes.size)
        for(i in 0 until count) {
            volumes[i] = max(0f, min(1f, newVolumes[i]))
        }
    }
    fun setAllVolumesToDefault() {
        volumes = FloatArray(16) { 1f }
    }
    /** Charge le son (wav dans res/raw, i.e. R.raw...).
     * volumeId est le volume à utiliser quand ce son est joué.
     * Retourne le soundId de android.media.SoundPool. */
    private fun initSound(@RawRes soundResId: Int, volumeId: Int) : Int
    {
        soundPoolIdOfRawRes[soundResId]?.let { soundPoolId ->
            // Si déjà init, juste mettre à jour le volume id.
            volumeIdOfSoundPoolId[soundPoolId] = volumeId
            return soundPoolId
        }
        val ctx = SoundManager.ctx.get() ?: run {
            printerror("No context for sound.")
            return 0
        }
        val newSoundPoolId = soundPool?.load(ctx, soundResId,1) ?: run {
            printerror("Ne peut charger le son $soundResId")
            return 0
        }
        soundPoolIdOfRawRes[soundResId] = newSoundPoolId
        volumeIdOfSoundPoolId[newSoundPoolId] = volumeId
        return newSoundPoolId
    }
    fun getSoundPoolId(@RawRes soundResId: Int) : Int {
        return soundPoolIdOfRawRes[soundResId] ?: run {
            printerror("Sound with res id $soundResId not loaded.")
            -1
        }
    }
    /** La fonction par défaut pour jouer les sons. */
    fun playWithSoundPoolId(soundPoolID: Int, pitch: Int = 0, volume: Float = 1f) {
        if (isMute) return
        if ( soundPoolID <0) {
            printerror("Son pas loadé."); return}
        val volumeIn = volumeIdOfSoundPoolId[soundPoolID]?.let { volId ->
            volumes.getOrElse(volId) { 1f } * volume } ?: volume
        val maxVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 1
        val currVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
        val usedVolume = min(1f, max(0f, volumeIn)) * currVolume.toFloat() / maxVolume.toFloat()
        // Ajustement pour Android... (trop fort pour des volume bas) -> Courbe parabolique.
        val volume2 = 0.8f *( (1f - 0.10f)*usedVolume*usedVolume + 0.10f*usedVolume )
//        printdebug("maxVol $maxVolume, currVolume $currVolume, usedVolume $usedVolume, adjusted $volume2")
        soundPool?.play(soundPoolID, volume2, volume2, 1, 0, 2f.pow(pitch.toFloat()/12f))
    }

    /** Fonction "helper" pour jouer un son avec son res id. */
    fun playWithResId(@RawRes resID: Int, pitch: Int = 0, volume: Float = 1f) {
        soundPoolIdOfRawRes[resID]?.let { soundPoolID ->
            playWithSoundPoolId(soundPoolID, pitch, volume)
        } ?: run{ printerror("Son $resID non chargé.") }
    }

    internal fun initWith(ctx: Context, extraSoundIds: Array<Pair<Int, Int>>?) {
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
        initSound(R.raw.duck_error, 0)
        initSound(R.raw.fireworks, 0)
        initSound(R.raw.note_piano, 0)
        initSound(R.raw.pouing_a, 0)
        initSound(R.raw.type_writter, 0)
        initSound(R.raw.woosh, 0)
        // 2.1 Chargements des sons spécifiques au projet courant.
        extraSoundIds?.forEach { (soundId, volumeID) ->
            initSound(soundId, volumeID)
        }
    }
}