@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.coq.coqlib

import android.content.Context
import androidx.annotation.StringRes
import com.coq.coqlib.graph.Texture
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.*

enum class Language(val iso: String) {
    French("fr"),
    English("en"),
    Japanese("ja"),
    German("de"),
    ChineseSimpl("zh-Hans"),
    Italian("it"),
    Spanish("es"),
    Arabic("ar"),
    Greek("el"),
    Russian("ru"),
    Swedish("sv"),
    ChineseTrad("zh-Hant"),
    Portuguese("pt"),
    Korean("ko"),
    Vietnamese("vi");

    companion object {
        fun initWith(coqActivity: CoqActivity) {
            coqAct = WeakReference(coqActivity)
            current = getSystemLanguage()
        }
        fun getSystemLanguage(): Language {
            val iso = coqAct.get()?.let {
                it.resources.configuration.locales.get(0).language ?: "en"
            } ?: run {
                printerror("Language not init.")
                return English
            }
            return languageOfIso[iso] ?: English
        }
        var current: Language = English
            set(newLanguage) {
                val coqActivity = coqAct.get() ?: run {
                    printerror("Language not init.")
                    return
                }
                if(coqActivity.localizedCtx != null && newLanguage == current)
                    return
                coqActivity.setLocalizedContextTo(newLanguage.iso)
                Texture.updateAllLocalizedStrings()
                field = newLanguage
            }
        fun setCurrentTo(iso: String) {
            current = languageOfIso[iso] ?: English
        }
        fun currentIs(language: Language) : Boolean
                = current == language
        val currentTileId: Int
            get() = current.ordinal
        val currentIsRightToLeft: Boolean
            get() = currentIs(Arabic)
        val currentDirectionFactor: Float
            get() = if(current == Arabic) -1f else 1f
        internal val currentCtx: Context?
            get() = coqAct.get()?.localizedCtx

        fun localizedStringForCurrent(@StringRes locStrId: Int) : String? {
            coqAct.get()?.let { coqAct ->
                coqAct.localizedCtx?.let { locAct ->
                    return locAct.resources.getString(locStrId)
                }
                printwarning("No localized context.")
                return coqAct.resources.getString(locStrId)
            }
            printerror("No coqActivity for Language.")
            return null
        }
        /** Obtenir le contenue d'un texte.
         * Les fichier "quelconques" sont dans le dossier "assets" du projet. */
        fun contentOfAssetFileForCurrent(fileName: String, showError: Boolean = true) : String? {
            coqAct.get()?.run {
                return try {
                    assets.open("${current.iso}/$fileName").use { inputStream ->
                        inputStream.bufferedReader().use { bufferedReader ->
                            bufferedReader.readText()
                        }
                    }
                } catch (e: IOException) {
                    if (showError)
                        printerror("Ne peut charger \"$fileName\" pour la langue \"${current.iso}\".")
                    null
                }
            }
            printerror("coqActivity not set.")
            return null
        }

        private var coqAct: WeakReference<CoqActivity> = WeakReference(null)
//        private var coqAct: CoqActivity? = null
        private val languageOfIso = mapOf(
            "fr" to  French,
            "en" to  English,
            "ja" to  Japanese,
            "de" to  German,
            "it" to  Italian,
            "es" to  Spanish,
            "ar" to  Arabic,
            "el" to  Greek,
            "ru" to  Russian,
            "sv" to  Swedish,
            "zh-Hans" to ChineseSimpl,
            "zh-Hant" to ChineseTrad,
            "pt" to    Portuguese,
            "ko" to  Korean,
            "vi" to  Vietnamese,
        )
    }
}


/** GARBAGE
        fun getContextForLanguage(context: Context, language: String) : Context {
            val locale = Locale(language)
            Locale.setDefault(locale)
            val config = context.resources.configuration
            val localList = LocaleList(locale)
            config.setLocales(localList)
            config.setLayoutDirection(locale)
            return context.createConfigurationContext(config)
        }

fun Context.getLocStr(@StringRes locStrId: Int, language: String) : String {
    val config = Configuration(resources.configuration)
    val locale = Locale(language)
    config.setLocale(locale)
    return createConfigurationContext(config).resources.getString(locStrId)
}

 */