package com.star.menugenerator.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.ForegroundColorSpan
import android.text.style.MetricAffectingSpan
import com.star.menugenerator.R
import com.star.menugenerator.domain.Block
import com.star.menugenerator.domain.BlockType
import com.star.menugenerator.domain.ALIGN_LEFT
import com.star.menugenerator.domain.Menu
import com.star.menugenerator.domain.MenuSettings
import com.star.menugenerator.domain.TemplateSpec
import com.star.menugenerator.domain.buildBlocks
import com.star.menugenerator.domain.paginate
import kotlin.math.roundToInt

/**
 * Draws menu pages onto the supplied template artwork, in template-pixel coordinates
 * (1054 x 1492). Text sizes, line heights, paddings and colours come straight from the original
 * project's templates.css. The same code path produces the on-screen preview, the PNG and the PDF.
 */
class MenuRenderer(private val context: Context) {
    private val fonts = Fonts(context)
    private val templateBitmaps = HashMap<String, Bitmap>()
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    // ---- template artwork ----

    @Synchronized
    fun templateBitmap(spec: TemplateSpec): Bitmap = templateBitmaps.getOrPut(spec.id) {
        val resId = if (spec.id == "green") R.drawable.template_green else R.drawable.template_purple
        val opts = BitmapFactory.Options().apply { inScaled = false }
        BitmapFactory.decodeResource(context.resources, resId, opts)
            ?: error("Could not load template ${spec.id}")
    }

    // ---- block layout ----

    private class Part(val layout: StaticLayout, val gapAbove: Float)

    private class BlockBox(
        val marginTop: Float = 0f,
        val borderTop: Float = 0f,
        val borderColor: Int = 0,
        val padTop: Float = 0f,
        val padBottom: Float = 0f,
        val dropTopOnFirst: Boolean = false,
        val parts: List<Part>,
    ) {
        fun height(first: Boolean): Float {
            var h = marginTop + borderTop + (if (first && dropTopOnFirst) 0f else padTop) + padBottom
            for (p in parts) h += p.gapAbove + p.layout.height
            return h
        }

        fun draw(canvas: Canvas, first: Boolean, width: Int) {
            var y = marginTop
            if (borderTop > 0f) {
                val line = Paint().apply { color = borderColor; style = Paint.Style.FILL }
                canvas.drawRect(0f, y, width.toFloat(), y + borderTop, line)
            }
            y += borderTop + (if (first && dropTopOnFirst) 0f else padTop)
            for (p in parts) {
                y += p.gapAbove
                canvas.save()
                canvas.translate(0f, y)
                p.layout.draw(canvas)
                canvas.restore()
                y += p.layout.height
            }
        }
    }

    /** Applies a typeface to part of a text (used for the bold price inside an item line). */
    private class TypefaceSpanCompat(private val tf: Typeface) : MetricAffectingSpan() {
        override fun updateDrawState(ds: TextPaint) { ds.typeface = tf }
        override fun updateMeasureState(textPaint: TextPaint) { textPaint.typeface = tf }
    }

    private class Styles(val spec: TemplateSpec, val st: MenuSettings, val fam: FontFamilySet) {
        val fs: Float = st.fontScale.toFloat()
        val align: Layout.Alignment =
            if (st.align == ALIGN_LEFT) Layout.Alignment.ALIGN_NORMAL else Layout.Alignment.ALIGN_CENTER
    }

    private fun styles(spec: TemplateSpec, st: MenuSettings) = Styles(spec, st, fonts.family(st.font))

    private fun paint(
        s: Styles, size: Float, bold: Boolean, italic: Boolean, color: Int, letterSpacingEm: Float = 0f,
    ): TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = s.fam.pick(bold, italic)
        textSize = size
        this.color = color
        letterSpacing = letterSpacingEm
    }

    /** CSS-like line-height: the line box is exactly [lineHeight] x font size tall. */
    private fun layout(
        text: CharSequence, p: TextPaint, width: Int, lineHeight: Float, align: Layout.Alignment,
    ): StaticLayout {
        val fm = p.fontMetrics
        val natural = (fm.descent - fm.ascent).coerceAtLeast(1f)
        val mult = (lineHeight * p.textSize) / natural
        return StaticLayout.Builder.obtain(text, 0, text.length, p, width)
            .setAlignment(align)
            .setLineSpacing(0f, mult)
            .setIncludePad(false)
            .build()
    }

    private fun withAlpha(color: Int, alpha: Float): Int =
        (color and 0x00FFFFFF) or ((alpha * 255).roundToInt() shl 24)

    private fun priceLine(s: Styles, price: String, width: Int): Part {
        val p = paint(s, 28f * s.fs, true, false, s.spec.price)
        return Part(layout(price, p, width, 1.4f, s.align), 8f)
    }

    private fun boxFor(b: Block, s: Styles, width: Int): BlockBox {
        val fs = s.fs
        val spec = s.spec
        return when (b.type) {
            BlockType.HEAD -> BlockBox(
                padTop = 26f * fs, padBottom = 6f, dropTopOnFirst = true,
                parts = listOf(Part(layout(b.text, paint(s, 30f * fs, true, false, spec.head, 0.04f), width, 1.4f, s.align), 0f)),
            )
            BlockType.NAME -> {
                val parts = mutableListOf(
                    Part(layout(b.text, paint(s, 30f * fs, true, false, spec.item), width, 1.35f, s.align), 0f),
                )
                if (b.price.isNotEmpty()) parts += priceLine(s, b.price, width)
                BlockBox(padTop = 18f * fs, padBottom = 4f, dropTopOnFirst = true, parts = parts)
            }
            BlockType.SEC -> BlockBox(
                padTop = 12f * fs, padBottom = 2f,
                parts = listOf(Part(layout(b.text, paint(s, 24f * fs, true, true, spec.head), width, 1.35f, s.align), 0f)),
            )
            BlockType.LIST -> {
                val parts = mutableListOf(
                    Part(layout(b.text, paint(s, 26f * fs, false, false, spec.item), width, 1.5f, s.align), 0f),
                )
                if (b.price.isNotEmpty()) parts += priceLine(s, b.price, width)
                BlockBox(padTop = 2f, padBottom = 2f, parts = parts)
            }
            BlockType.ITEM -> {
                val base = paint(s, 28f * fs, false, false, spec.item)
                val sb = SpannableStringBuilder(b.name)
                if (b.qty.isNotEmpty()) {
                    val start = sb.length
                    sb.append(" (").append(b.qty).append(")")
                    sb.setSpan(ForegroundColorSpan(withAlpha(spec.item, 0.85f)), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                if (b.price.isNotEmpty()) {
                    val start = sb.length
                    sb.append("\u00A0\u00A0").append(b.price)
                    sb.setSpan(TypefaceSpanCompat(s.fam.bold), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    sb.setSpan(ForegroundColorSpan(spec.price), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                val parts = mutableListOf(Part(layout(sb, base, width, 1.45f, s.align), 0f))
                if (b.desc.isNotEmpty()) {
                    parts += Part(layout(b.desc, paint(s, 21f * fs, false, false, spec.muted), width, 1.35f, s.align), 0f)
                }
                if (b.note.isNotEmpty()) {
                    parts += Part(layout(b.note, paint(s, 21f * fs, false, true, spec.muted), width, 1.35f, s.align), 0f)
                }
                BlockBox(padTop = 3f, padBottom = 3f, parts = parts)
            }
            BlockType.GRAND -> {
                val center = Layout.Alignment.ALIGN_CENTER
                val parts = mutableListOf(
                    Part(layout(b.text, paint(s, 32f * fs, true, false, spec.price), width, 1.4f, center), 0f),
                )
                if (b.note.isNotEmpty()) {
                    parts += Part(layout(b.note, paint(s, 21f * fs, false, true, spec.muted), width, 1.35f, center), 0f)
                }
                BlockBox(marginTop = 24f, borderTop = 2f, borderColor = spec.rule, padTop = 16f, parts = parts)
            }
        }
    }

    // ---- pagination (measures real text heights, then runs the original greedy algorithm) ----

    fun layoutPages(menu: Menu, spec: TemplateSpec): List<List<Block>> {
        val blocks = buildBlocks(menu)
        val s = styles(spec, menu.settings)
        val heights = HashMap<String, Float>(blocks.size * 2)
        for (b in blocks) heights[b.k] = boxFor(b, s, spec.box.width).height(false)
        return paginate(blocks, heights, spec.box.height.toFloat())
    }

    // ---- drawing ----

    /** Draws one page in template pixel space (0..spec.w x 0..spec.h). Scale the canvas before calling. */
    fun drawPage(
        canvas: Canvas, spec: TemplateSpec, settings: MenuSettings,
        blocks: List<Block>, pageIndex: Int, pageCount: Int,
    ) {
        val s = styles(spec, settings)
        canvas.drawBitmap(templateBitmap(spec), null, Rect(0, 0, spec.w, spec.h), bitmapPaint)

        val box = spec.box
        canvas.save()
        canvas.clipRect(box.left.toFloat(), box.top.toFloat(), (box.left + box.width).toFloat(), (box.top + box.height).toFloat())
        canvas.translate(box.left.toFloat(), box.top.toFloat())
        var y = 0f
        blocks.forEachIndexed { i, b ->
            val bb = boxFor(b, s, box.width)
            canvas.save()
            canvas.translate(0f, y)
            bb.draw(canvas, i == 0, box.width)
            canvas.restore()
            y += bb.height(i == 0)
        }
        canvas.restore()

        if (pageCount > 1) {
            val p = paint(s, 18f, false, false, spec.head)
            val num = layout("${pageIndex + 1} / $pageCount", p, spec.w, 1.4f, Layout.Alignment.ALIGN_CENTER)
            canvas.save()
            canvas.translate(0f, spec.pageNumTop.toFloat())
            num.draw(canvas)
            canvas.restore()
        }
    }

    /** Renders a page to a bitmap. scale 1 = 1054 x 1492 px, scale 2 = 2108 x 2984 px (PNG export). */
    fun renderBitmap(
        spec: TemplateSpec, settings: MenuSettings, blocks: List<Block>,
        pageIndex: Int, pageCount: Int, scale: Float,
    ): Bitmap {
        val bmp = Bitmap.createBitmap((spec.w * scale).roundToInt(), (spec.h * scale).roundToInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.scale(scale, scale)
        drawPage(canvas, spec, settings, blocks, pageIndex, pageCount)
        return bmp
    }
}
