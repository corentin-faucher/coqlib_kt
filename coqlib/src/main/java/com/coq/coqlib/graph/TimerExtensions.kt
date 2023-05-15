/** Extensions utiles pour les timers lorsque l'on a une thread OpenGL.
 * Permet d'exécuter le "Runnable"/Task d'un timer dans la thread OpenGL.
 * Corentin Faucher
 * 29 novembre 2022
 * */

package com.coq.coqlib.graph

import com.coq.coqlib.CoqActivity
import com.coq.coqlib.printerror
import java.util.*
import kotlin.concurrent.schedule
import kotlin.concurrent.scheduleAtFixedRate

fun Timer.scheduleGL(delayMS: Long, r: Runnable) {
    val view = CoqActivity.currentView.get() ?: run {
        printerror("No GLSurfaceView initialzed...")
        return
    }
    schedule(delayMS) {
        view.queueEvent(r)
    }
}

fun Timer.scheduleGL(delaySec: Float, r: Runnable) {
    val view = CoqActivity.currentView.get() ?: run {
        printerror("No GLSurfaceView initialzed...")
        return
    }
    schedule((delaySec * 1000f).toLong()) {
        view.queueEvent(r)
    }
}

// TODO : Vérifier les utilisation de scheduleAtFixedRate...
/** Attention : peut causer des memory leak. S'assurer de cancel() le timer. */
fun Timer.scheduleAtFixedRateGL(delayMS: Long, periodMS: Long, r: Runnable) {
    val view = CoqActivity.currentView.get() ?: run {
        printerror("No GLSurfaceView initialzed...")
        return
    }
    scheduleAtFixedRate(delayMS, periodMS) {
        view.queueEvent(r)
    }
}

/** Attention : peut causer des memory leak. S'assurer de cancel() le timer. */
fun Timer.scheduleAtFixedRateGL(delaySec: Float, periodSec: Float, r: Runnable) {
    val view = CoqActivity.currentView.get() ?: run {
        printerror("No GLSurfaceView initialzed...")
        return
    }
    scheduleAtFixedRate((delaySec * 1000f).toLong(), (periodSec * 1000f).toLong()) {
        view.queueEvent(r)
    }
}