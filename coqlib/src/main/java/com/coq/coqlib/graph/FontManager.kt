@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package com.coq.coqlib.graph

import android.content.Context

import android.graphics.Typeface
import androidx.annotation.FontRes
import androidx.core.content.res.ResourcesCompat
import com.coq.coqlib.Language
import com.coq.coqlib.R
import com.coq.coqlib.maths.Vector2
import com.coq.coqlib.printerror
import com.coq.coqlib.printwarning

object FontManager {
    // Font par défaut.
    private val defaultTypeface = Typeface.SERIF
    private val defaultRatio = Vector2(0.3f, 0.60f)
    /** La police de caractère à utiliser par défaut lorsque l'on crée la texture d'une string.
     * Voir Texture drawAsString. */
    private var currentTypeface: Typeface = defaultTypeface
        private set
    private var currentRatios: Vector2 = defaultRatio
        private set

    /** Mettre à jour la police par défaut pour dessiner les strings. */
    fun setCurrent(@FontRes fontRes: Int?, ctx: Context) {
        val oldTypeface = currentTypeface
        if(fontRes == null) {
            currentTypeface = defaultTypeface
            currentRatios = defaultRatio
        } else {
            currentTypeface = ResourcesCompat.getFont(ctx, fontRes) ?: run {
                printwarning("Cannot load font for resId $fontRes.")
                defaultTypeface
            }
            currentRatios = ratiosOfFont[fontRes] ?: defaultRatio
        }
        if(currentTypeface == oldTypeface)
            return
        Texture.redrawAllStrings()
    }
    fun getNameOfFont(@FontRes fontRes: Int) : String
        = nameOfFont[fontRes] ?: "Error: no font with id $fontRes."
    fun getFontNamesAndIdsForLanguage(language: Language) : List<Pair<String, Int> > {
        val fontIdList = fontsOfLanguage[language] ?: availableFontsDefault
        return fontIdList.map { fontId ->
            (nameOfFont[fontId] ?: "Err.: Font no $fontId") to fontId
        }
    }

    internal fun getTypefaceAndRatio(@FontRes fontRes: Int?, ctx: Context?) : Pair<Typeface, Vector2> {
        // TODO check...
        if(fontRes == null || ctx == null) {
//            return Pair(Typeface.create("monospace", Typeface.NORMAL), currentRatios)
            if (ctx == null)
                printerror("No context to get typeface.")
            return Pair(currentTypeface, currentRatios)
        }
        val typeface = ResourcesCompat.getFont(ctx, fontRes) ?: run {
            printerror("No font associate with resource id $fontRes.")
            return Pair(currentTypeface, currentRatios)
        }
        val ratios = ratiosOfFont[fontRes] ?: defaultRatio
        return Pair(typeface, ratios)
    }
    private val nameOfFont: Map<Int, String> = mapOf(
        /** Pour l'arabe */
        R.font.amiri to "Amiri",
        R.font.reem_kufi to "Reem Kufi",
        R.font.tajawal_medium to "Tajawal",
        /** Pour le coréen */
        R.font.jua to "Jua",
        R.font.nanum_gothic to "Nanum Gothic",
        R.font.nanum_myeongjo to "Nanum Gothic",
        R.font.nanum_pen_script to "Nanum Pen Script",
        /** Pour le japonais */
        R.font.m_plus_rounded_1c_medium to "M PLUS Rounded", // jap, viet, latin
        R.font.noto_sans to "Noto Sans",      // jap ?
        R.font.noto_serif to "Noto Serif",     // jap ?
        R.font.nunito_sans to "Nunito Sans",    // jap ?
        R.font.rocknroll_one to "RocknRoll One",  // jap, no viet, latin
        R.font.yusei_magic to "Yusei Magic",    // no viet, jap, latin
        /** Pour le chinois ? (pas de chinois pour Android de toute façon...) */
        // ...
        /** Standard (latin) */
        R.font.comic_neue to "Comic Neue",    // no viet
        R.font.courier_prime to "Courier Prime", // no viet
        R.font.great_vibes to "Great Vibes",   // no viet
        R.font.kite_one to "Kite One",      // no viet
        R.font.luciole to "Luciole",        // no viet
        R.font.neucha to "Neucha",         // no viet
        R.font.open_dyslexic3 to "Open Dyslexic", //
        R.font.schoolbell to "Schoolbell",     // no viet
        R.font.special_elite to "Special Elite",  // no viet
        R.font.tinos to "Tinos",          //
    )
    /** Fine tuning pour le cadrage des fonts... TODO : À réviser dans vrai situation... */
    private var ratiosOfFont: MutableMap<Int, Vector2> = mutableMapOf(
        /** Pour l'arabe */
        R.font.amiri to Vector2(0.40f, 0.38f),         // Arabic
        R.font.reem_kufi to Vector2(0.15f, 0.40f),     // arabic, latin, no viet
        R.font.tajawal_medium to Vector2(0.40f, 0.75f), // Arabic
        /** Pour le coréen */
        R.font.jua to Vector2(0.10f, 0.75f),           // korean
        R.font.nanum_gothic to Vector2(0.20f, 0.55f),  // korean
        R.font.nanum_myeongjo to Vector2(0.10f, 0.55f), // korean
        R.font.nanum_pen_script to Vector2(0.10f, 0.55f), // korean
        /** Pour le japonais */
        R.font.m_plus_rounded_1c_medium to Vector2(0.15f, 0.48f), // jap, viet, latin
        R.font.noto_sans to Vector2(0.20f, 0.53f),      // jap ?
        R.font.noto_serif to Vector2(0.20f, 0.53f),     // jap ?
        R.font.nunito_sans to Vector2(0.25f, 0.53f),    // jap ?
        R.font.rocknroll_one to Vector2(0.20f, 0.49f),  // jap, no viet, latin
        R.font.yusei_magic to Vector2(0.25f, 0.44f),    // no viet, jap, latin
        /** Pour le chinois ? (pas de chinois pour Android de toute façon...) */
        // ...
        /** Standard (latin) */
        R.font.comic_neue to Vector2(0.35f, 0.55f),    // no viet
        R.font.courier_prime to Vector2(0.10f, 0.60f), // no viet
        R.font.great_vibes to Vector2(1.10f, 0.42f),   // no viet
        R.font.kite_one to Vector2(0.50f, 0.47f),      // no viet
        R.font.luciole to Vector2(0.33f, 0.7f),        // no viet
        R.font.neucha to Vector2(0.35f, 0.70f),         // no viet
        R.font.open_dyslexic3 to Vector2(0.10f, 0.37f), //
        R.font.schoolbell to Vector2(0.55f, 0.45f),     // no viet
        R.font.special_elite to Vector2(0.15f, 0.72f),  // no viet
        R.font.tinos to Vector2(0.20f, 0.50f),          //
    )
    /** Les polices disponible pour la plupart des langues. */
    private val availableFontsDefault: IntArray = intArrayOf(
        R.font.comic_neue,
        R.font.courier_prime,
        R.font.great_vibes,
        R.font.kite_one,
        R.font.luciole,
        R.font.m_plus_rounded_1c_medium,
        R.font.neucha,
        R.font.noto_sans,
        R.font.noto_serif,
        R.font.nunito_sans,
        R.font.open_dyslexic3,
        R.font.reem_kufi,
        R.font.rocknroll_one,
        R.font.schoolbell,
        R.font.special_elite,
        R.font.tinos,
        R.font.yusei_magic,
    )
    /** Les polices pour les langues "particulières"... */
    private val fontsOfLanguage: Map<Language, IntArray> = mapOf(
        Language.Arabic to intArrayOf(
            R.font.amiri,
            R.font.reem_kufi,
            R.font.tajawal_medium,
        ),
        Language.Japanese to intArrayOf(
            R.font.m_plus_rounded_1c_medium,
            R.font.noto_sans,
            R.font.noto_serif,
            R.font.rocknroll_one,
            R.font.yusei_magic,
        ),
        Language.Korean to intArrayOf(
            R.font.jua,
            R.font.nanum_gothic,
            R.font.nanum_myeongjo,
            R.font.nanum_pen_script,
        ),
        Language.Vietnamese to intArrayOf(
            R.font.m_plus_rounded_1c_medium,
            R.font.noto_sans,
            R.font.noto_serif,
            R.font.nunito_sans,
            R.font.open_dyslexic3,
            R.font.tinos,
        ),
    )

}
