package com.star.menugenerator.domain

/** The blank content area of a template, in template-image pixels. */
data class ContentBox(val left: Int, val top: Int, val width: Int, val height: Int)

/**
 * Geometry and text colours of a supplied template image. W/H are the image's real pixel size and
 * `box` is its blank content area, copied from the original project (templates/index.js + templates.css).
 */
data class TemplateSpec(
    val id: String,
    val name: String,
    val w: Int,
    val h: Int,
    val box: ContentBox,
    val pageNumTop: Int,
    val head: Int,
    val item: Int,
    val price: Int,
    val muted: Int,
    val rule: Int,
)

val TEMPLATES: List<TemplateSpec> = listOf(
    TemplateSpec(
        id = "purple", name = "Purple Arch Floral", w = 1054, h = 1492,
        box = ContentBox(left = 140, top = 430, width = 774, height = 880), pageNumTop = 1345,
        head = 0xFFE91E85.toInt(), item = 0xFF351448.toInt(), price = 0xFF57266F.toInt(),
        muted = 0xFF5B4A68.toInt(), rule = 0xFFFFC928.toInt(),
    ),
    TemplateSpec(
        id = "green", name = "Green & Gold Reception", w = 1054, h = 1492,
        box = ContentBox(left = 130, top = 335, width = 794, height = 950), pageNumTop = 1300,
        head = 0xFFF3D37A.toInt(), item = 0xFFFBF3DC.toInt(), price = 0xFFF3D37A.toInt(),
        muted = 0xFFD8CFA8.toInt(), rule = 0xFFC9A646.toInt(),
    ),
)

fun getTemplate(id: String?): TemplateSpec = TEMPLATES.firstOrNull { it.id == id } ?: TEMPLATES[0]
