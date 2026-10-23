package com.rt.studio

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import org.json.JSONObject
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class IconShape(val title: String) { ROUNDED("Скруглённая"), SQUIRCLE("Squircle"), CIRCLE("Круг"), HEXAGON("Шестиугольник"), DIAMOND("Ромб") }
enum class Glyph(val title: String) { BOLT("Молния"), CAMERA("Камера"), MUSIC("Музыка"), PLAY("Плей"), FOLDER("Папка"), MAIL("Письмо"), SETTINGS("Настройки"), STAR("Звезда"), TERMINAL("Терминал"), GALLERY("Галерея"), HEART("Сердце"), SHIELD("Щит") }
enum class ElementKind(val title: String) { CIRCLE("Круг"), RECTANGLE("Прямоугольник"), TRIANGLE("Треугольник"), STAR("Звезда"), LINE("Линия"), DIAMOND("Ромб"), PATH("Автотрассировка") }
data class VectorObject(
    val id: String = java.util.UUID.randomUUID().toString(), val kind: ElementKind = ElementKind.CIRCLE,
    val color: Int = 0xFFFFFFFF.toInt(), val x: Float = .5f, val y: Float = .5f, val scale: Float = 1f,
    val rotation: Float = 0f, val opacity: Float = 1f, val visible: Boolean = true, val cutout: Boolean = false, val pathData: String = ""
)

data class IconDesign(
    val background: Int = 0xFF465D91.toInt(), val foreground: Int = 0xFFFFFFFF.toInt(), val accent: Int = 0xFFD9E2FF.toInt(),
    val shape: IconShape = IconShape.SQUIRCLE, val glyph: Glyph = Glyph.BOLT,
    val corner: Float = 0.25f, val glyphScale: Float = 1f, val rotation: Float = 0f,
    val outline: Boolean = false, val backgroundVisible: Boolean = true, val glyphVisible: Boolean = true, val accentVisible: Boolean = true, val elements: List<VectorObject> = emptyList()
)
data class PackItem(val name: String, val component: String, val design: IconDesign)

val studioPalette = listOf(0xFF465D91.toInt(), 0xFF171923.toInt(), 0xFF0D9488.toInt(), 0xFF7C3AED.toInt(), 0xFFDB2777.toInt(), 0xFFEA580C.toInt(), 0xFF2563EB.toInt(), 0xFF15803D.toInt(), 0xFFF8FAFC.toInt(), 0xFFFDE68A.toInt(), 0xFFB45309.toInt(), 0xFF64748B.toInt(), 0xFF111827.toInt(), 0xFFEF4444.toInt(), 0xFF38BDF8.toInt(), 0xFFC4B5FD.toInt())

fun randomDesign(seed: Int = Random.nextInt()): IconDesign {
    val r = Random(seed)
    val palettes = listOf(
        intArrayOf(0xFF172554.toInt(), -1, 0xFF93C5FD.toInt()), intArrayOf(0xFF7C2D12.toInt(), 0xFFFFF7ED.toInt(), 0xFFFDBA74.toInt()),
        intArrayOf(0xFF064E3B.toInt(), 0xFFECFDF5.toInt(), 0xFF6EE7B7.toInt()), intArrayOf(0xFF4A044E.toInt(), 0xFFFDF4FF.toInt(), 0xFFF0ABFC.toInt()),
        intArrayOf(0xFF18181B.toInt(), 0xFFF4F4F5.toInt(), 0xFFA1A1AA.toInt()), intArrayOf(0xFF1E3A8A.toInt(), 0xFFFFFBEB.toInt(), 0xFFFBBF24.toInt()),
        intArrayOf(0xFFF1F5F9.toInt(), 0xFF0F172A.toInt(), 0xFF64748B.toInt())
    )
    val p = palettes[r.nextInt(palettes.size)]
    return IconDesign(p[0], p[1], p[2], IconShape.entries.toTypedArray()[r.nextInt(5)], Glyph.entries.toTypedArray()[r.nextInt(12)],
        r.nextFloat() * .22f + .14f, r.nextFloat() * .24f + .88f, listOf(0f, 0f, 0f, -8f, 8f, 15f)[r.nextInt(6)], r.nextBoolean())
}

fun IconDesign.toJson() = JSONObject().apply {
    put("background", background); put("foreground", foreground); put("accent", accent); put("shape", shape.name); put("glyph", glyph.name)
    put("corner", corner); put("glyphScale", glyphScale); put("rotation", rotation); put("outline", outline)
    put("backgroundVisible", backgroundVisible); put("glyphVisible", glyphVisible); put("accentVisible", accentVisible)
    put("elements", org.json.JSONArray().apply { elements.forEach { e -> put(JSONObject().put("id", e.id).put("kind", e.kind.name).put("color", e.color).put("x", e.x).put("y", e.y).put("scale", e.scale).put("rotation", e.rotation).put("opacity", e.opacity).put("visible", e.visible).put("cutout", e.cutout).put("pathData", e.pathData)) } })
}
fun designFromJson(j: JSONObject) = IconDesign(
    j.optInt("background", 0xFF465D91.toInt()), j.optInt("foreground", -1), j.optInt("accent", 0xFFD9E2FF.toInt()),
    runCatching { IconShape.valueOf(j.optString("shape", "SQUIRCLE")) }.getOrDefault(IconShape.SQUIRCLE),
    runCatching { Glyph.valueOf(j.optString("glyph", "BOLT")) }.getOrDefault(Glyph.BOLT),
    j.optDouble("corner", .25).toFloat(), j.optDouble("glyphScale", 1.0).toFloat(), j.optDouble("rotation", 0.0).toFloat(),
    j.optBoolean("outline", false), j.optBoolean("backgroundVisible", true), j.optBoolean("glyphVisible", true), j.optBoolean("accentVisible", true),
    runCatching {
        val a = j.optJSONArray("elements") ?: org.json.JSONArray()
        (0 until a.length()).map { i ->
            val e = a.getJSONObject(i)
            VectorObject(
                id = e.optString("id", java.util.UUID.randomUUID().toString()),
                kind = runCatching { ElementKind.valueOf(e.optString("kind", "CIRCLE")) }.getOrDefault(ElementKind.CIRCLE),
                color = e.optInt("color", -1), x = e.optDouble("x", .5).toFloat(), y = e.optDouble("y", .5).toFloat(),
                scale = e.optDouble("scale", 1.0).toFloat(), rotation = e.optDouble("rotation", 0.0).toFloat(),
                opacity = e.optDouble("opacity", 1.0).toFloat(), visible = e.optBoolean("visible", true), cutout = e.optBoolean("cutout", false), pathData = e.optString("pathData", "")
            )
        }
    }.getOrDefault(emptyList())
)

private fun paint(c: Int, style: Paint.Style = Paint.Style.FILL, stroke: Float = 12f) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = c; this.style = style; strokeWidth = stroke; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
}
private fun polygon(points: List<Pair<Float, Float>>) = Path().apply {
    points.forEachIndexed { i, p -> if (i == 0) moveTo(p.first, p.second) else lineTo(p.first, p.second) }; close()
}
private fun simpleSvgPath(data: String): Path = Path().apply {
    val tokens = Regex("[MLZ]|-?(?:\\d+\\.?\\d*|\\.\\d+)").findAll(data).map { it.value }.toList()
    var command = ' '
    var i = 0
    while (i < tokens.size) {
        val token = tokens[i++]
        if (token.length == 1 && token[0].isLetter()) {
            command = token[0]
            if (command == 'Z') { close(); continue }
        } else {
            val x = token.toFloatOrNull() ?: continue
            if (i >= tokens.size) break
            val y = tokens[i++].toFloatOrNull() ?: break
            when (command) { 'M' -> { moveTo(x, y); command = 'L' }; 'L' -> lineTo(x, y) }
        }
    }
}

private fun starPath() = Path().apply {
    for (i in 0 until 10) { val r = if (i % 2 == 0) 116.0 else 52.0; val a = -Math.PI / 2 + i * Math.PI / 5
        val x = 256 + cos(a) * r; val y = 256 + sin(a) * r
        if (i == 0) moveTo(x.toFloat(), y.toFloat()) else lineTo(x.toFloat(), y.toFloat()) }; close()
}

fun renderIcon(canvas: AndroidCanvas, d: IconDesign, size: Int) {
    val top = canvas.save(); canvas.scale(size / 512f, size / 512f)
    val rect = RectF(28f, 28f, 484f, 484f)
    val outer = polygon(listOf(256f to 28f, 454f to 142f, 454f to 370f, 256f to 484f, 58f to 370f, 58f to 142f))
    val diamond = polygon(listOf(256f to 24f, 488f to 256f, 256f to 488f, 24f to 256f))
    fun shape(p: Paint) { when (d.shape) {
        IconShape.ROUNDED -> canvas.drawRoundRect(rect, d.corner.coerceIn(.08f,.48f)*512f, d.corner.coerceIn(.08f,.48f)*512f, p)
        IconShape.SQUIRCLE -> canvas.drawRoundRect(rect, 140f, 140f, p)
        IconShape.CIRCLE -> canvas.drawCircle(256f, 256f, 228f, p)
        IconShape.HEXAGON -> canvas.drawPath(outer, p)
        IconShape.DIAMOND -> canvas.drawPath(diamond, p)
    } }
    if (d.backgroundVisible) {
        shape(paint(d.background))
        if (d.outline) shape(paint(d.accent, Paint.Style.STROKE, 9f))
    }
    if (d.accentVisible) { val p = paint(d.accent); canvas.drawCircle(394f,120f,13f,p); canvas.drawCircle(118f,390f,8f,p); canvas.drawCircle(395f,388f,6f,p) }
    if (d.glyphVisible) {
        val gs = canvas.save(); canvas.rotate(d.rotation,256f,256f); canvas.scale(d.glyphScale.coerceIn(.45f,1.5f),d.glyphScale.coerceIn(.45f,1.5f),256f,256f)
        val f = paint(d.foreground); val l = paint(d.foreground,Paint.Style.STROKE,22f)
        when(d.glyph) {
            Glyph.BOLT -> canvas.drawPath(polygon(listOf(285f to 112f,190f to 258f,245f to 258f,218f to 399f,329f to 228f,270f to 228f)),f)
            Glyph.CAMERA -> { canvas.drawRoundRect(RectF(140f,184f,372f,344f),32f,32f,f); canvas.drawRoundRect(RectF(188f,160f,252f,204f),14f,14f,f); canvas.drawCircle(256f,264f,48f,paint(d.background)); canvas.drawCircle(256f,264f,30f,paint(d.foreground,Paint.Style.STROKE,13f)) }
            Glyph.MUSIC -> { canvas.drawLine(286f,137f,286f,312f,l); canvas.drawLine(210f,188f,210f,342f,l); canvas.drawLine(210f,188f,286f,166f,l); canvas.drawOval(RectF(164f,314f,228f,363f),f); canvas.drawOval(RectF(240f,284f,304f,333f),f) }
            Glyph.PLAY -> canvas.drawPath(polygon(listOf(190f to 142f,360f to 256f,190f to 370f)),f)
            Glyph.FOLDER -> canvas.drawPath(polygon(listOf(126f to 185f,220f to 185f,249f to 215f,371f to 215f,371f to 350f,126f to 350f)),f)
            Glyph.MAIL -> { canvas.drawRoundRect(RectF(132f,182f,380f,342f),20f,20f,l); canvas.drawPath(polygon(listOf(142f to 200f,256f to 275f,370f to 200f)),l) }
            Glyph.SETTINGS -> { canvas.drawCircle(256f,256f,84f,l); canvas.drawCircle(256f,256f,34f,l); for(i in 0..7){val a=i*Math.PI/4;canvas.drawLine((256+cos(a)*96).toFloat(),(256+sin(a)*96).toFloat(),(256+cos(a)*122).toFloat(),(256+sin(a)*122).toFloat(),paint(d.foreground,Paint.Style.STROKE,20f))} }
            Glyph.STAR -> canvas.drawPath(starPath(),f)
            Glyph.TERMINAL -> { canvas.drawPath(polygon(listOf(170f to 200f,232f to 256f,170f to 312f)),l); canvas.drawLine(250f,315f,335f,315f,l) }
            Glyph.GALLERY -> { canvas.drawCircle(310f,199f,25f,f); canvas.drawPath(polygon(listOf(135f to 345f,221f to 238f,268f to 292f,305f to 260f,377f to 345f)),l) }
            Glyph.HEART -> { val p=Path().apply{moveTo(256f,355f);cubicTo(226f,327f,150f,270f,150f,218f);cubicTo(150f,157f,222f,145f,256f,199f);cubicTo(290f,145f,362f,157f,362f,218f);cubicTo(362f,270f,286f,327f,256f,355f);close()};canvas.drawPath(p,f) }
            Glyph.SHIELD -> { val p=Path().apply{moveTo(256f,132f);lineTo(354f,169f);lineTo(340f,284f);lineTo(256f,375f);lineTo(172f,284f);lineTo(158f,169f);close()};canvas.drawPath(p,l);canvas.drawPath(polygon(listOf(205f to 253f,242f to 290f,309f to 219f)),paint(d.foreground,Paint.Style.STROKE,24f)) }
        }
        canvas.restoreToCount(gs)
    }
    d.elements.filter { it.visible }.forEach { element ->
        val es = canvas.save()
        canvas.translate(element.x.coerceIn(0f, 1f) * 512f, element.y.coerceIn(0f, 1f) * 512f)
        canvas.rotate(element.rotation)
        canvas.scale(element.scale.coerceIn(.15f, 2.5f), element.scale.coerceIn(.15f, 2.5f))
        val p = paint(element.color).apply { alpha = (element.opacity.coerceIn(0f, 1f) * 255).toInt() }
        if (element.cutout) { p.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR); p.color = android.graphics.Color.TRANSPARENT; p.alpha = 255 }
        when (element.kind) {
            ElementKind.CIRCLE -> canvas.drawCircle(0f, 0f, 76f, p)
            ElementKind.RECTANGLE -> canvas.drawRoundRect(RectF(-78f, -58f, 78f, 58f), 10f, 10f, p)
            ElementKind.TRIANGLE -> canvas.drawPath(polygon(listOf(0f to -88f, 78f to 65f, -78f to 65f)), p)
            ElementKind.STAR -> canvas.drawPath(Path().apply {
                for (i in 0 until 10) { val rr = if (i % 2 == 0) 86.0 else 39.0; val a = -Math.PI / 2 + i * Math.PI / 5
                    val x = cos(a) * rr; val y = sin(a) * rr; if (i == 0) moveTo(x.toFloat(), y.toFloat()) else lineTo(x.toFloat(), y.toFloat()) }; close()
            }, p)
            ElementKind.LINE -> canvas.drawLine(-95f, 0f, 95f, 0f, paint(element.color, Paint.Style.STROKE, 15f).apply { alpha = (element.opacity.coerceIn(0f, 1f) * 255).toInt(); if (element.cutout) xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) })
            ElementKind.DIAMOND -> canvas.drawPath(polygon(listOf(0f to -90f, 75f to 0f, 0f to 90f, -75f to 0f)), p)
            ElementKind.PATH -> canvas.drawPath(simpleSvgPath(element.pathData), p)
        }
        canvas.restoreToCount(es)
    }
    canvas.restoreToCount(top)
}
fun renderIconBitmap(design: IconDesign, size: Int = 512): Bitmap = Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888).also { renderIcon(AndroidCanvas(it),design,size) }

fun designToSvg(d: IconDesign): String {
    fun hex(c:Int) = String.format("#%06X",0xFFFFFF and c)
    val bg=hex(d.background); val fg=hex(d.foreground); val ac=hex(d.accent)
    val shape=when(d.shape) {
        IconShape.ROUNDED -> "<rect x=\"28\" y=\"28\" width=\"456\" height=\"456\" rx=\"" + (d.corner*512) + "\"/>"
        IconShape.SQUIRCLE -> "<rect x=\"28\" y=\"28\" width=\"456\" height=\"456\" rx=\"140\"/>"
        IconShape.CIRCLE -> "<circle cx=\"256\" cy=\"256\" r=\"228\"/>"
        IconShape.HEXAGON -> "<path d=\"M256 28 L454 142 V370 L256 484 L58 370 V142 Z\"/>"
        IconShape.DIAMOND -> "<path d=\"M256 24 L488 256 L256 488 L24 256 Z\"/>"
    }
    val g=when(d.glyph) {
        Glyph.BOLT -> "<path d=\"M285 112 L190 258 H245 L218 399 L329 228 H270 Z\"/>"
        Glyph.CAMERA -> "<rect x=\"140\" y=\"184\" width=\"232\" height=\"160\" rx=\"32\"/><rect x=\"188\" y=\"160\" width=\"64\" height=\"44\" rx=\"14\"/><circle cx=\"256\" cy=\"264\" r=\"30\" fill=\"none\" stroke=\"$bg\" stroke-width=\"15\"/>"
        Glyph.MUSIC -> "<path d=\"M286 137 V312 M210 188 V342 M210 188 L286 166\" fill=\"none\" stroke=\"$fg\" stroke-width=\"22\" stroke-linecap=\"round\"/><ellipse cx=\"196\" cy=\"338\" rx=\"32\" ry=\"24\"/><ellipse cx=\"272\" cy=\"308\" rx=\"32\" ry=\"24\"/>"
        Glyph.PLAY -> "<path d=\"M190 142 L360 256 L190 370 Z\"/>"
        Glyph.FOLDER -> "<path d=\"M126 185 H220 L249 215 H371 V350 H126 Z\"/>"
        Glyph.MAIL -> "<rect x=\"132\" y=\"182\" width=\"248\" height=\"160\" rx=\"20\" fill=\"none\" stroke=\"$fg\" stroke-width=\"22\"/><path d=\"M142 200 L256 275 L370 200\" fill=\"none\" stroke=\"$fg\" stroke-width=\"22\"/>"
        Glyph.SETTINGS -> "<circle cx=\"256\" cy=\"256\" r=\"84\" fill=\"none\" stroke=\"$fg\" stroke-width=\"22\"/><circle cx=\"256\" cy=\"256\" r=\"34\" fill=\"none\" stroke=\"$fg\" stroke-width=\"22\"/><path d=\"M256 112 V145 M256 367 V400 M112 256 H145 M367 256 H400 M154 154 L177 177 M335 335 L358 358 M358 154 L335 177 M177 335 L154 358\" fill=\"none\" stroke=\"$fg\" stroke-width=\"20\"/>"
        Glyph.STAR -> "<path d=\"M256 140 L283 222 L369 222 L300 273 L326 357 L256 308 L186 357 L212 273 L143 222 L229 222 Z\"/>"
        Glyph.TERMINAL -> "<path d=\"M170 200 L232 256 L170 312 M250 315 H335\" fill=\"none\" stroke=\"$fg\" stroke-width=\"22\" stroke-linecap=\"round\"/>"
        Glyph.GALLERY -> "<circle cx=\"310\" cy=\"199\" r=\"25\"/><path d=\"M135 345 L221 238 L268 292 L305 260 L377 345 Z\" fill=\"none\" stroke=\"$fg\" stroke-width=\"20\"/>"
        Glyph.HEART -> "<path d=\"M256 355 C226 327 150 270 150 218 C150 157 222 145 256 199 C290 145 362 157 362 218 C362 270 286 327 256 355 Z\"/>"
        Glyph.SHIELD -> "<path d=\"M256 132 L354 169 L340 284 L256 375 L172 284 L158 169 Z\" fill=\"none\" stroke=\"$fg\" stroke-width=\"22\"/><path d=\"M205 253 L242 290 L309 219\" fill=\"none\" stroke=\"$fg\" stroke-width=\"24\"/>"
    }
    return buildString {
        append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"512\" height=\"512\" viewBox=\"0 0 512 512\">")
        if(d.backgroundVisible) { append("<g fill=\"$bg\">$shape</g>"); if(d.outline) append("<g fill=\"none\" stroke=\"$ac\" stroke-width=\"9\">$shape</g>") }
        if(d.accentVisible) append("<g fill=\"$ac\"><circle cx=\"394\" cy=\"120\" r=\"13\"/><circle cx=\"118\" cy=\"390\" r=\"8\"/><circle cx=\"395\" cy=\"388\" r=\"6\"/></g>")
        if(d.glyphVisible) append("<g fill=\"$fg\" transform=\"rotate(" + d.rotation + " 256 256) translate(256 256) scale(" + d.glyphScale + ") translate(-256 -256)\">$g</g>")
        d.elements.filter { it.visible }.forEach { e ->
            val color = if (e.cutout) bg else hex(e.color)
            val geom = when (e.kind) {
                ElementKind.CIRCLE -> "<circle cx=\"0\" cy=\"0\" r=\"76\"/>"
                ElementKind.RECTANGLE -> "<rect x=\"-78\" y=\"-58\" width=\"156\" height=\"116\" rx=\"10\"/>"
                ElementKind.TRIANGLE -> "<path d=\"M0 -88 L78 65 L-78 65 Z\"/>"
                ElementKind.STAR -> "<path d=\"M0 -86 L23 -30 L82 -27 L37 10 L52 68 L0 37 L-52 68 L-37 10 L-82 -27 L-23 -30 Z\"/>"
                ElementKind.LINE -> "<path d=\"M-95 0 H95\" fill=\"none\" stroke=\"$color\" stroke-width=\"15\"/>"
                ElementKind.DIAMOND -> "<path d=\"M0 -90 L75 0 L0 90 L-75 0 Z\"/>"
                ElementKind.PATH -> "<path d=" + '"' + e.pathData + '"' + "/>"
            }
            append("<g opacity=\"" + e.opacity.coerceIn(0f, 1f) + "\" transform=\"translate(" + (e.x * 512f) + " " + (e.y * 512f) + ") rotate(" + e.rotation + ") scale(" + e.scale + ")\" fill=\"" + color + "\">" + geom + "</g>")
        }
        append("</svg>")
    }
}
