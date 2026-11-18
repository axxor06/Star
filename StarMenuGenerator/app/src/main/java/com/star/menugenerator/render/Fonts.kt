package com.star.menugenerator.render

import android.content.Context
import android.graphics.Typeface
import com.star.menugenerator.domain.FONT_CLEAN
import com.star.menugenerator.domain.FONT_TRADITIONAL

class FontFamilySet(
    val regular: Typeface,
    val bold: Typeface,
    val italic: Typeface,
    val boldItalic: Typeface,
) {
    fun pick(bold: Boolean, italic: Boolean): Typeface = when {
        bold && italic -> boldItalic
        bold -> this.bold
        italic -> this.italic
        else -> regular
    }
}

/**
 * The three card fonts of the original project: Elegant (Playfair Display), Clean (DM Sans) and
 * Traditional (Noto Serif Malayalam).
 *
 * The app must work offline and the font files were not part of the supplied assets, so the fonts
 * are loaded from app/src/main/assets/fonts/ when present and otherwise fall back to the phone's
 * built-in serif / sans-serif families (which already contain Malayalam glyphs). See README for
 * the exact file names to drop in.
 */
class Fonts(private val context: Context) {
    private val cache = HashMap<String, FontFamilySet>()

    @Synchronized
    fun family(style: String): FontFamilySet = cache.getOrPut(style) { build(style) }

    private fun asset(prefix: String, variant: String): Typeface? = try {
        Typeface.createFromAsset(context.assets, "fonts/$prefix-$variant.ttf")
    } catch (e: Exception) {
        null
    }

    private fun build(style: String): FontFamilySet {
        val base: Typeface
        val prefix: String
        when (style) {
            FONT_CLEAN -> { base = Typeface.SANS_SERIF; prefix = "DMSans" }
            FONT_TRADITIONAL -> { base = Typeface.SERIF; prefix = "NotoSerifMalayalam" }
            else -> { base = Typeface.SERIF; prefix = "PlayfairDisplay" }
        }
        val regular = asset(prefix, "Regular") ?: base
        return FontFamilySet(
            regular = regular,
            bold = asset(prefix, "Bold") ?: Typeface.create(regular, Typeface.BOLD),
            italic = asset(prefix, "Italic") ?: Typeface.create(regular, Typeface.ITALIC),
            boldItalic = asset(prefix, "BoldItalic") ?: Typeface.create(regular, Typeface.BOLD_ITALIC),
        )
    }
}
