package com.coq.coqlib

import android.content.Context
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
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
        if(showError) printerror("Cannot load \"$fileName\".", e)
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
    val theInt = if(isNull(name)) null
        else get(name) as? Int
    if(theInt == null && showError)
        printerror("No int for $name.")
    return theInt
}

fun JSONObject.getFloatOrNull(name: String, showError: Boolean = false) : Float? {
    val theFloat = if(isNull(name)) null
        else get(name) as? Float
    if(theFloat == null && showError)
        printerror("No int for $name.")
    return theFloat
}

fun JSONObject.getStringOrNull(name: String, showError: Boolean = false) : String? {
    val theString = if(isNull(name)) null
        else get(name) as? String
    if(theString == null && showError)
        printerror("No string for $name.")
    return theString
}

fun JSONObject.getBooleanOrNull(name: String, showError: Boolean = false) : Boolean? {
    val theBool = if(isNull(name)) null
        else get(name) as? Boolean
    if(theBool == null && showError)
        printerror("No bool for $name.")
    return theBool
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
