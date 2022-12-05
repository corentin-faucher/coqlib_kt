/** Helpers.kt
 * Fonctions diverses utiles...
 * Corentin Faucher
 * 28 novembre 2022
 * */
package com.coq.coqlib

import android.util.Log
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

fun printerror(message: String, depth: Int = 3) {
    val stes = Exception().stackTrace
    if (stes.size < 3) {
        Log.e("🐔coq", "❌ Error: $message")
        return
    }
    var str = "❌ Error: $message  → ${stes[2]}"
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

fun <K, T> MutableMap<K, WeakReference<T>>.strip() {
    forEach { (k, v) ->
        if(v.get() == null) {
            this.remove(k)
        }
    }
}

fun <T> MutableList<WeakReference<T> >.strip() {
    this.removeIf { it.get() == null }
}

