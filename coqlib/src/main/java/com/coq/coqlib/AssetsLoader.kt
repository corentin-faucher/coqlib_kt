package com.coq.coqlib

import android.content.Context
import org.json.JSONObject
import java.io.IOException

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
