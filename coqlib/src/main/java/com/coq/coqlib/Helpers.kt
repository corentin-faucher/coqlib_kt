/** Helpers.kt
 * Fonctions diverses utiles...
 * Corentin Faucher
 * 28 novembre 2022
 * */
@file:Suppress("unused")

package com.coq.coqlib

import android.util.Log
import java.io.IOException
import java.lang.ref.WeakReference
import kotlin.math.min

/** -- Notes sur kotlin: ----------------
 *
 * Attention aux Timer ! Il ne run pas dans la thread opengl. Si on crée une surface (par exemple)
 * if faut se ramener à la thread opengl...
 Timer(true).schedule(300L) {
    root.queueToGLView {
        PopMessage.over(parentNode, "Bonjour!")
    }
 }

// Equivalent de DispatchQueue.main.async...
// -> Handler(Looper.getMainLooper()).post

// Tour de passe-passe pour avoir un constructeur optional ou préparer les données avant le super().
// Mettre le constructor private et créer un "operator fun invoke" dans le companion object.
// Ex.:
// private constructor() {
// }
// companion object {
//    operator fun invoke() : PopSurface {
//          doStuff()
//          return MyClass()
//    }
*/

/** Affichage d'un message d'erreur.
 * Utilisez tag:🐔coq dans le log pour ne voir que c'est messages de debuging. */
fun printerror(message: String, e: IOException? = null, depth: Int = 3) {
    val stes = Exception().stackTrace
    val mes = message + (e?.localizedMessage?.let { ", $it" } ?: "")
    if (stes.size < 3) {
        Log.e("🐔coq", "❌ Error: $mes")
        return
    }
    var str = "❌ Error: $mes  → ${stes[2]}"
    val depth2 = min(stes.size, depth+1)
    for (index in 3..depth2) {
        val ste = stes[index]
        str += "\n   → $ste"
    }
    Log.e("🐔coq", str)
}

fun printwarning(message: String, depth: Int = 1) {
    val stes = Exception().stackTrace
    if (stes.size < 3) {
        Log.w("🐔coq", "⚠️ Warn.: $message")
        return
    }
    var str = "⚠️ Warn.: $message  → ${stes[2]}"
    val depth2 = min(stes.size, depth+1)
    for (index in 3..depth2) {
        val ste = stes[index]
        str += "\n   → $ste"
    }
    Log.w("🐔coq", str)
}
fun printdebug(message: String, depth: Int = 1) {
    if (!BuildConfig.DEBUG) {
        return
    }
    val stes = Exception().stackTrace
    if (stes.size < 3) {
        Log.d("🐔coq", "🐞 Debug: $message")
        return
    }
    var str = "🐞 Debug: $message  → ${stes[2]}"
    val depth2 = min(stes.size, depth+1)
    for (index in 3..depth2) {
        val ste = stes[index]
        str += "\n   → $ste"
    }
    Log.d("🐔coq", str)
}

fun printhere(depth: Int = 1) {
    val stes = Exception().stackTrace
    if (stes.size < 3) {
        Log.i("🐔coq", "🐔 Now at ?? (no stack ?)")
        return
    }
    var str = "🐔 Now in ${stes[2]}"
    val depth2 = min(stes.size, depth+1)
    for (index in 3..depth2) {
        val ste = stes[index]
        str += "\n   → $ste"
    }
    Log.i("🐔coq", str)
}

/*-- Extensions pratiques... --*/
/** Nettoie une map de WeakReference des pointeurs null. */
fun <K, T> MutableMap<K, WeakReference<T>>.strip() {
    val toRemoves = filter { it.value.get() == null }
    for(toRemove in toRemoves) {
        remove(toRemove.key)
    }
}

fun <T> MutableList<WeakReference<T> >.strip() {
    val toRemoves = filter { it.get() == null }
    for(toRemove in toRemoves)
        remove(toRemove)
//    this.removeIf { it.get() == null }
}

/** Incrémenter le compteur d'un dictionnaire utlisé pour compter les frequences. */
fun <K> MutableMap<K, Int>.incrCounterAt(key: K) {
    get(key)?.let { count ->
        put(key, count + 1)
    } ?: run {
        put(key, 1)
    }
}

//fun <T> checkArraySize(array: Array

fun IntRange.toIntArray(): IntArray {
    if(last < first) return IntArray(0)
    val arr = IntArray(last - first + 1)
    for((i, e) in this.withIndex())
        arr[i] = e
    return arr
}