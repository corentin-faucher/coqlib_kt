package com.coq.coqlib

import android.content.Context
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.IOException

/*== NOTE : Pour la lecture de fichier on utilise "use" qui "close" les input/output streams. */
/** Lecture d'un fichier de l'app, i.e. dans "assets". */
fun Context.readAssetsString(fileName: String, showError: Boolean = true) : String? {
    return try {
        assets.open(fileName).use { inputStream ->
            inputStream.bufferedReader().use { bufferedReader ->
                bufferedReader.readText()
            }
        }
    } catch (e : IOException) {
        if(showError) printerror(e.toString(), depth = 4)
        null
    }
}
/** Lecture d'un fichier ordinaire "File" sous forme binaire.
 * e.g. données de l'usager de ctx.filesDir. */
fun File.readFileByteArray(showError: Boolean = true): ByteArray? {
    return try {
        FileInputStream(this).use {
            it.readBytes()
        }
    } catch (e : IOException) {
        if(showError) printerror("Cannot load \"${name}\".", e)
        null
    }
}
fun File.readFileString(showError: Boolean = true): String? {
    // readText() de File semble être un racourci pour cette méthode ?
    return try {
        FileInputStream(this).use { fis ->
            fis.bufferedReader().use { br ->
                br.readText()
            }
        }
    } catch (e : IOException) {
        if(showError) printerror("Cannot load \"${name}\".", e)
        null
    }
}

///** Lecture d'un fichier ordinaire "File", e.g. données de l'usager de ctx.filesDir. */
//fun File.readFileString(showError: Boolean = true): String? {
//    return try {
//        FileInputStream(this).bufferedReader().use { bufferedReader ->
//            bufferedReader.readText()
//        }
//    } catch (e : IOException) {
//        if(showError) printerror("Cannot load \"${name}\".", e)
//        null
//    }
//}

fun JSONObject.getIntOrNull(name: String, showError: Boolean = false) : Int? {
    if(isNull(name)) return null
    return try {
        getInt(name)
    } catch(e: JSONException) {
        if(showError) printerror(e.toString())
        null
    }
}

fun JSONObject.getDoubleOrNull(name: String, showError: Boolean = false) : Double? {
    if(isNull(name)) return null
    return try {
        getDouble(name)
    } catch(e: JSONException) {
        if(showError) printerror(e.toString())
        null
    }
}

fun JSONObject.getStringOrNull(name: String, showError: Boolean = false) : String? {
    if(isNull(name)) return null
    return try {
        getString(name)
    } catch(e: JSONException) {
        if(showError) printerror(e.toString())
        null
    }
}

fun JSONObject.getBooleanOrNull(name: String, showError: Boolean = false) : Boolean? {
    if(isNull(name)) return null
    return try {
        getBoolean(name)
    } catch(e: JSONException) {
        if(showError) printerror(e.toString())
        null
    }
}

fun JSONObject.getStringArrayOrNull(name: String, showError: Boolean = false) : Array<String>? {
    return try {
        val joArr = getJSONArray(name)
        Array(joArr.length()) { index ->
            joArr.getString(index)
        }
    } catch(e: JSONException) {
        if(showError) printerror(e.toString())
        null
    }
}

fun JSONObject.getIntArrayOrNull(name: String, showError: Boolean = false) : IntArray? {
    return try {
        val joArr = getJSONArray(name)
        IntArray(joArr.length()) { index ->
            joArr.getInt(index)
        }
    } catch(e: JSONException) {
        if(showError) printerror(e.toString())
        null
    }
}

fun JSONObject.getBooleanArrayOrNull(name: String, showError: Boolean = false) : BooleanArray? {
    return try {
        val joArr = getJSONArray(name)
        BooleanArray(joArr.length()) { index ->
            joArr.getBoolean(index)
        }
    } catch(e: JSONException) {
        if(showError) printerror(e.toString())
        null
    }
}



// Superflu ?
/*
fun Context.readAssetsJSON(fileName: String, showError: Boolean = true): JSONObject? {
    return try {
        val jsonString = assets.open(fileName).use { inputStream ->
            inputStream.bufferedReader().use { bufferedReader ->
                bufferedReader.readText()

            }
        }
        JSONObject(jsonString)
    } catch (e : IOException) {
        if(showError) printerror("Cannot load \"$fileName\"", e)
        null
    }
}

// Superflu ?
fun Context.readAssetsStringForCurrentLanguage(fileName: String, showError: Boolean = true) : String? {
    return try {
        assets.open("${Language.current.iso}/$fileName").use { inputStream ->
            inputStream.bufferedReader().use { bufferedReader ->
                bufferedReader.readText()
            }
        }
    } catch (e : IOException) {
        if(showError)
            printerror("Cannot load \"$fileName\" in language \"${Language.current.iso}\".")
        null
    }
}
*/
