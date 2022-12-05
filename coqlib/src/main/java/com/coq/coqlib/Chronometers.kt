@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.coq.coqlib

import android.util.Log
import com.coq.coqlib.maths.SmoothPos
import kotlin.math.PI

/** Utilisé pour l'affichage, pas le vrai Chrono
 * Incrémenté à chaque frame de 1/f seconde, i.e. ~16 ms.
 * (pour avoir des animations plus smooth que si on prend le vrai temps...) */
object RenderingChrono {
    var elapsedMS: Long = 0L
        private set
    val elapsedSec: Float
        get() = elapsedMS.toFloat() / 1000.0f
    val elapsedMS16: Short
        get() = elapsedMS.toShort()
    val elapsedMS32: Int
        get() = elapsedMS.toInt()
    /** Un temps écoulé qui reste toujours entre 0 et 24pi. (Pour les sin/cos) */
    var elapsedAngleMS: Long = 0L
        private set
    val elapsedAngle: Float
        get() = elapsedAngleMS.toFloat() / 1000f

    val shouldSleep: Boolean
        get() = elapsedMS - touchTime > sleepTime

    /** Setter isPause à "false" a pour effet d'empêcher de s'endormir (touch). */
    var isPaused: Boolean = false
        set(newValue) {
            if(!newValue) {
                touchTime = elapsedMS
                touchAngleMS = elapsedAngleMS
            }
            field = newValue
        }
    fun update() {
        if(isPaused) return
        // Manip pour un deltaT smooth...
        val currentTime = AppChrono.systemTime
        val newDeltaT = currentTime - lastTime
        lastTime = currentTime
        smoothDeltaT.pos = newDeltaT.toFloat()
        // Le deltaT "debounced"
        val deltaT = smoothDeltaT.pos.toLong()
        elapsedMS += deltaT
        elapsedAngleMS += deltaT
        // Fair "looper" le temps pour les angles.
        if(elapsedAngleMS > angleLoopTime)
            elapsedAngleMS -= 2* angleLoopTime
    }

    // Temps entre 2 frame (a priori 17 ms, i.e. 60 fps)
    private var smoothDeltaT = SmoothPos(16.7f, 5f)
    private var lastTime = AppChrono.systemTime
    private var touchTime: Long = 0L
    private var touchAngleMS: Long = 0L
    private val sleepTime: Long = 16000L
    private val angleLoopTime: Long = (60000.0 * PI).toLong()
}

/** Chronometre du temps écoulé depuis l'ouverture de l'app. (Vrais ms/sec écoulées) */
object AppChrono {
    var isPaused: Boolean = false
        set(newValue) {
            // (pass si pas de changement)
            if(newValue == isPaused) return
            // Mise en pause
            if(newValue)
                startSleepTimeMS = systemTime
            // Sortie de pause
            else
                lastSleepTimeMS = systemTime - startSleepTimeMS
            // Temps écoulé (ou temps de départ... voir plus bas)
            time = systemTime - time
            field = newValue
        }
    val elapsedMS: Long
        get() = if(isPaused) time else (systemTime - time)
    val lastSleepTimeSec: Float
        get() = lastSleepTimeMS.toFloat() / 1000f

    /** "time" : Si isPause : temps total écoulé sans pause, sinon c'est le systemTime au départ (et sans pause).
     * i.e. isPause == true : time == elapsedTime,  isPause == false : time == systemTime - elapsedTime. */
    private var time: Long = systemTime
    private var lastSleepTimeMS: Long = 0L
    private var startSleepTimeMS: Long = 0L
    internal val systemTime: Long
        get() = System.currentTimeMillis()
}

/** Un chronomètre basé sur RenderingChrono
 *  (pas le vrai temps +1/f à chaque frame).
 *  Pour les animations... */
class ChronoR {
    // Fields
    /** Le chronomètre est activé. */
    var isActive: Boolean = false
        private set
    private var time: Long = 0L

    // Computed properties
    /** Le temps écoulé depuis "start()" en millisec. */
    val elapsedMS: Long
        get() = if(isActive) (RenderingChrono.elapsedMS - time) else time
    /** Le temps écoulé depuis "start()" en secondes. */
    val elapsedSec: Float
        get() = elapsedMS.toFloat() / 1000f
    /** Le temps global où le chrono a commencé (en millisec). */
    val startTimeMS: Long
        get() = if(isActive) time else RenderingChrono.elapsedMS - time

    // Methods
    fun start() {
        time = RenderingChrono.elapsedMS
        isActive = true
    }
    fun stop() {
        isActive = false
        time = 0
    }
    fun pause() {
        time = elapsedMS
        isActive = false
    }
    fun unpause() {
        time = startTimeMS
        isActive = true
    }
    fun addMS(millisec: Long) {
        if (isActive) {
            time -= millisec
        } else {
            time += millisec
        }
    }
    fun addSec(sec: Float) {
        if (sec > 0f)
            addMS((sec*1000f).toLong())
    }
    // Il ne peut pas avoir un temps écoulé "négatif"...
    fun removeMS(millisec: Long) {
        time = if (isActive) { // time est le starting time.
            if (elapsedMS > millisec) time + millisec
            else RenderingChrono.elapsedMS  // (i.e. elapsed = 0)
        } else { // time est le temps écoulé.
            if (time > millisec) time - millisec else 0
        }
    }
    fun removeSec(sec: Float) {
        if (sec > 0f)
            removeMS((sec*1000f).toLong())
    }
}

/** Un chronomètre basé sur le AppChrono (temps écoulé sans les "pause" de l'app).
 * N'est pas actif à l'ouverture. */
class Chrono {
    // Fields
    /** Le chronomètre est activé. */
    var isActive: Boolean = false
        private set
    private var time: Long = 0L

    // Computed properties
    /** Le temps écoulé depuis "start()" en millisec. */
    val elapsedMS: Long
        get() = if(isActive) (AppChrono.elapsedMS - time) else time
    /** Le temps écoulé depuis "start()" en secondes. */
    val elapsedSec: Float
        get() = elapsedMS.toFloat() / 1000f
    /** Temps écoulé sous forme hh:mm:ss. */
    val elapsedHMS: String
        get() {
            val s = elapsedMS / 1000L
            val ss = s % 60
            val m = (s / 60) % 60
            val h = s / 3600
            return "$h:${String.format("%02d", m)}:${String.format("%02d", ss)}"
        }
    /** Le temps global où le chrono a commencé (en millisec). */
    val startTimeMS: Long
        get() = if(isActive) time else AppChrono.elapsedMS - time

    // Methods
fun start() {
        time = AppChrono.elapsedMS
        isActive = true
    }
    fun stop() {
        isActive = false
        time = 0
    }
    fun pause() {
        time = elapsedMS
        isActive = false
    }
    fun unpause() {
        time = startTimeMS
        isActive = true
    }
    fun addMS(millisec: Long) {
        if (isActive) {
            time -= millisec
        } else {
            time += millisec
        }
    }
    fun addSec(sec: Float) {
        if (sec > 0f)
            addMS((sec*1000f).toLong())
    }
    // Il ne peut pas avoir un temps écoulé "négatif"...
    fun removeMS(millisec: Long) {
        time = if (isActive) { // time est le starting time.
            if (elapsedMS > millisec) time + millisec
            else AppChrono.elapsedMS  // (i.e. elapsed = 0)
        } else { // time est le temps écoulé.
            if (time > millisec) time - millisec else 0
        }
    }
    fun removeSec(sec: Float) {
        if (sec > 0f)
            removeMS((sec*1000f).toLong())
    }
}

/// Chrono pour debugging... Pour voir le temps pris par divers instructions...
class ChronoChecker(name: String? = null) {
    // fields
    private var time: Long = AppChrono.elapsedMS
    private var count: Int = 0
    private var str = "🦤 ${name ?: "timer"}: "
    // computed properties
    val elapsedMS: Long
        get() = AppChrono.elapsedMS - time
    val elapsedSec: Float
        get() = elapsedMS.toFloat() / 1000f
    // methods
    fun tic(message: String? = null) {
        count += 1
        str += (message ?: count.toString()) + elapsedMS.toString() + ", "
    }
    fun print() {
        str += "ended $elapsedMS."
        Log.d("🐔coq", str)
    }

}

class CountDown(var ringTimeMS: Long) {
    // fields
    var isActive: Boolean = false
        private set
    private var time: Long = 0
    // computed properties
    val isRinging: Boolean
        get() {
            return if (isActive) {
                ((AppChrono.elapsedMS - time) > ringTimeMS)
            } else {
                (time > ringTimeMS)
            }
        }
    var ringTimeSec: Float
        get() = ringTimeMS.toFloat() / 1000.0f
        set(newRingTimeSec) {
            ringTimeMS = (newRingTimeSec * 1000.0f).toLong()
        }
    val elapsedMS64: Long
        get() = if(isActive) (AppChrono.elapsedMS - time) else time
    val remainingMS: Long
        get() {
            val elapsed = elapsedMS64
            return if (elapsed > ringTimeMS) 0 else (ringTimeMS - elapsed)
        }
    val remainingSec: Float
        get() = remainingMS.toFloat() / 1000.0f
    // convenience init
    constructor(ringSec: Float) : this(
        if(ringSec <0) 0L else (ringSec * 1000.0f).toLong())
    // methods
    fun start() {
        time = AppChrono.elapsedMS
        isActive = true
    }
    fun stop() {
        isActive = false
        time = 0
    }
}

/** Un compte à rebours basé sur le SystemTime
 *  (vrai temps de l'OS sans tenir compte des pause/resume de l'app).. */
class CountDownS(var ringTimeMS: Long) {
    // fields
    var isActive: Boolean = false
        private set
    private var time: Long = 0
    // computed properties
    val isRinging: Boolean
        get() {
            return if (isActive) {
                ((AppChrono.systemTime - time) > ringTimeMS)
            } else {
                (time > ringTimeMS)
            }
        }
    var ringTimeSec: Float
        get() = ringTimeMS.toFloat() / 1000.0f
        set(newRingTimeSec) {
            ringTimeMS = (newRingTimeSec * 1000.0f).toLong()
        }
    val elapsedMS64: Long
        get() = if(isActive) (AppChrono.systemTime - time) else time
    val remainingMS: Long
        get() {
            val elapsed = elapsedMS64
            return if (elapsed > ringTimeMS) 0 else (ringTimeMS - elapsed)
        }
    val remainingSec: Float
        get() = remainingMS.toFloat() / 1000.0f
    // convenience init
    constructor(ringSec: Float) : this(
        if(ringSec <0) 0L else (ringSec * 1000.0f).toLong())
    // methods
    fun start() {
        time = AppChrono.systemTime
        isActive = true
    }
    fun stop() {
        isActive = false
        time = 0
    }
}

/** Version simplifié de ChronoR. Time sur juste 16 bits, i.e. moins de 32 sec. */
class SmallChronoR {
    val elapsedMS16: Short
        get() = (RenderingChrono.elapsedMS16 - startTime).toShort()

    val elapsedSec: Float
        get() =(RenderingChrono.elapsedMS16 - startTime).toFloat()/1000.0f

    fun start() {
        startTime = RenderingChrono.elapsedMS16
    }
    fun setElapsedTo(newTimeMS: Int) {
        startTime = (RenderingChrono.elapsedMS - newTimeMS).toShort()
    }

    // Membre privé
    private var startTime: Short = 0
}
