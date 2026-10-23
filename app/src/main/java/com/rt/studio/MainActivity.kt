package com.rt.studio

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlin.math.abs
import kotlin.math.atan2
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Redo
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private enum class StudioTab(val title: String) { HOME("Главная"), EDITOR("Редактор"), GENERATOR("Генератор"), PACK("Набор") }
private data class ArchiveSummary(val count: Int, val hasAppFilter: Boolean, val names: List<String>, val background: Int?, val foreground: Int?)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RTStudioTheme { StudioApp() } }
    }
}

private val StudioLight = lightColorScheme(
    primary = Color(0xFF465D91), onPrimary = Color.White, primaryContainer = Color(0xFFD9E2FF), onPrimaryContainer = Color(0xFF001A42),
    secondary = Color(0xFF585E71), secondaryContainer = Color(0xFFDDE2F9), tertiary = Color(0xFF76546F), tertiaryContainer = Color(0xFFFFD7F2),
    background = Color(0xFFF7F8FC), surface = Color(0xFFF7F8FC), surfaceVariant = Color(0xFFE2E2EC)
)
private val StudioDark = darkColorScheme(
    primary = Color(0xFFB1C5FF), onPrimary = Color(0xFF142F60), primaryContainer = Color(0xFF2D4678), onPrimaryContainer = Color(0xFFD9E2FF),
    secondary = Color(0xFFC1C6DD), tertiary = Color(0xFFE5B9D9), background = Color(0xFF111318), surface = Color(0xFF111318), surfaceVariant = Color(0xFF44464F)
)
@Composable private fun RTStudioTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) StudioDark else StudioLight, content = content)
}

private fun loadDesign(context: android.content.Context): IconDesign = runCatching {
    designFromJson(JSONObject(context.getSharedPreferences("rt_studio", 0).getString("design", "{}") ?: "{}"))
}.getOrDefault(IconDesign())

private fun loadPack(context: android.content.Context): List<PackItem> = runCatching {
    val a = JSONArray(context.getSharedPreferences("rt_studio", 0).getString("pack", "[]") ?: "[]")
    (0 until a.length()).map { val o = a.getJSONObject(it); PackItem(o.optString("name"), o.optString("component"), designFromJson(o.getJSONObject("design"))) }
}.getOrDefault(emptyList())

private fun saveProject(context: android.content.Context, design: IconDesign, items: List<PackItem>, packTitle: String, packageId: String) {
    val a = JSONArray()
    items.forEach { item -> a.put(JSONObject().put("name", item.name).put("component", item.component).put("design", item.design.toJson())) }
    context.getSharedPreferences("rt_studio", 0).edit().putString("design", design.toJson().toString()).putString("pack", a.toString()).putString("packTitle", packTitle).putString("packApplicationId", packageId).apply()
}

private fun readLimited(input: ZipInputStream, maxBytes: Int): ByteArray {
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (out.size() < maxBytes) {
        val count = input.read(buffer, 0, minOf(buffer.size, maxBytes - out.size()))
        if (count <= 0) break
        out.write(buffer, 0, count)
    }
    return out.toByteArray()
}

private fun loadReferenceBitmap(context: android.content.Context, uri: Uri, maxDimension: Int = 1600): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (bounds.outWidth / sample > maxDimension || bounds.outHeight / sample > maxDimension) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.ARGB_8888 }
    return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
}

private fun cropReferenceSquare(source: Bitmap): Bitmap {
    val side = minOf(source.width, source.height)
    return Bitmap.createBitmap(source, (source.width - side) / 2, (source.height - side) / 2, side, side)
}

private fun removeSolidBackground(source: Bitmap, tolerance: Int = 110): Bitmap {
    val width = source.width
    val height = source.height
    val result = source.copy(Bitmap.Config.ARGB_8888, true)
    val pixels = IntArray(width * height)
    result.getPixels(pixels, 0, width, 0, 0, width, height)
    val cornerIndices = listOf(0, width - 1, (height - 1) * width, height * width - 1)
    val corners = cornerIndices.map { pixels[it] }
    if (corners.count { ((it ushr 24) and 255) < 100 } >= 2) return result
    val opaqueCorners = corners.filter { ((it ushr 24) and 255) >= 100 }.ifEmpty { corners }
    val red = opaqueCorners.map { (it shr 16) and 255 }.average()
    val green = opaqueCorners.map { (it shr 8) and 255 }.average()
    val blue = opaqueCorners.map { it and 255 }.average()
    val feather = (tolerance * 0.45f).coerceAtLeast(1f)
    for (i in pixels.indices) {
        val color = pixels[i]
        val alpha = (color ushr 24) and 255
        if (alpha == 0) continue
        val distance = abs(((color shr 16) and 255) - red) +
            abs(((color shr 8) and 255) - green) + abs((color and 255) - blue)
        if (distance <= tolerance) {
            pixels[i] = color and 0x00FFFFFF
        } else if (distance < tolerance + feather) {
            val edgeAlpha = (((distance - tolerance) / feather) * alpha).toInt().coerceIn(0, 255)
            pixels[i] = (color and 0x00FFFFFF) or (edgeAlpha shl 24)
        }
    }
    result.setPixels(pixels, 0, width, 0, 0, width, height)
    return result
}

private fun traceBitmapToPath(source: Bitmap): String? {
    val w = 128; val h = 128
    val bitmap = Bitmap.createScaledBitmap(source, w, h, true)
    try {
        val corners = listOf(bitmap.getPixel(1, 1), bitmap.getPixel(w - 2, 1), bitmap.getPixel(1, h - 2), bitmap.getPixel(w - 2, h - 2))
        val transparentBackground = corners.count { (it ushr 24) < 100 } >= 2
        val opaqueCorners = corners.filter { (it ushr 24) >= 100 }.ifEmpty { corners }
        val br = opaqueCorners.map { (it shr 16) and 255 }.average()
        val bg = opaqueCorners.map { (it shr 8) and 255 }.average()
        val bb = opaqueCorners.map { it and 255 }.average()
        val mask = BooleanArray(w * h)
        for (y in 0 until h) for (x in 0 until w) {
            val c = bitmap.getPixel(x, y)
            val a = (c ushr 24) and 255
            val distance = abs(((c shr 16) and 255) - br) + abs(((c shr 8) and 255) - bg) + abs((c and 255) - bb)
            mask[y * w + x] = a > 55 && (transparentBackground || distance > 66)
        }
        val visited = BooleanArray(mask.size)
        var largest = IntArray(0)
        val queue = IntArray(mask.size)
        val dx = intArrayOf(-1, 0, 1, -1, 1, -1, 0, 1)
        val dy = intArrayOf(-1, -1, -1, 0, 0, 1, 1, 1)
        for (start in mask.indices) {
            if (!mask[start] || visited[start]) continue
            var head = 0; var tail = 0
            val component = ArrayList<Int>()
            queue[tail++] = start; visited[start] = true
            while (head < tail) {
                val at = queue[head++]; component.add(at)
                val x = at % w; val y = at / w
                for (k in 0 until 8) {
                    val nx = x + dx[k]; val ny = y + dy[k]
                    if (nx in 0 until w && ny in 0 until h) {
                        val ni = ny * w + nx
                        if (mask[ni] && !visited[ni]) { visited[ni] = true; queue[tail++] = ni }
                    }
                }
            }
            if (component.size > largest.size) largest = component.toIntArray()
        }
        if (largest.size < 24) return null
        val keep = BooleanArray(mask.size)
        largest.forEach { keep[it] = true }
        val border = ArrayList<Pair<Int, Int>>()
        for (index in largest) {
            val x = index % w; val y = index / w
            var edge = false
            for (k in 0 until 8) {
                val nx = x + dx[k]; val ny = y + dy[k]
                if (nx !in 0 until w || ny !in 0 until h || !keep[ny * w + nx]) { edge = true; break }
            }
            if (edge) border.add(x to y)
        }
        if (border.size < 6) return null
        val minX = border.minOf { it.first }; val maxX = border.maxOf { it.first }
        val minY = border.minOf { it.second }; val maxY = border.maxOf { it.second }
        val cx = (minX + maxX) / 2f; val cy = (minY + maxY) / 2f
        border.sortBy { atan2(it.second - cy, it.first - cx) }
        val stride = maxOf(1, border.size / 100)
        val points = border.filterIndexed { index, _ -> index % stride == 0 }.take(140)
        val scale = 180f / maxOf(1, maxX - minX, maxY - minY)
        fun fmt(v: Float) = String.format(java.util.Locale.US, "%.1f", v)
        return buildString {
            points.forEachIndexed { index, point ->
                append(if (index == 0) "M" else " L").append(fmt((point.first - cx) * scale)).append(" ").append(fmt((point.second - cy) * scale))
            }
            append(" Z")
        }
    } finally {
        if (bitmap !== source) bitmap.recycle()
    }
}

private fun inspectArchive(context: android.content.Context, uri: Uri): ArchiveSummary {
    var count = 0
    var hasXml = false
    val names = mutableListOf<String>()
    val backgrounds = mutableListOf<Int>()
    val foregrounds = mutableListOf<Int>()
    context.contentResolver.openInputStream(uri)?.use { raw ->
        ZipInputStream(raw).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val lower = entry.name.lowercase()
                    if (lower.endsWith("appfilter.xml")) hasXml = true
                    if ((lower.endsWith(".png") || lower.endsWith(".webp")) && count < 60) {
                        val bytes = readLimited(zip, 1_000_000)
                        if (bytes.isNotEmpty()) {
                            count++
                            names.add(entry.name.substringAfterLast('/').substringBeforeLast('.'))
                            if (count <= 18) runCatching {
                                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                if (bitmap != null && bitmap.width > 4 && bitmap.height > 4) {
                                    val bg = bitmap.getPixel(2, 2)
                                    val fg = bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)
                                    if ((bg ushr 24) > 180) backgrounds.add(bg)
                                    if ((fg ushr 24) > 180) foregrounds.add(fg)
                                    bitmap.recycle()
                                }
                            }
                        }
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }
    fun common(list: List<Int>, fallback: Int): Int = list.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: fallback
    return ArchiveSummary(count, hasXml, names.distinct().take(24), common(backgrounds, 0xFF465D91.toInt()), common(foregrounds, -1))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudioApp() {
    val context = LocalContext.current
    var design by remember { mutableStateOf(loadDesign(context)) }
    val packItems = remember { mutableStateListOf<PackItem>().apply { addAll(loadPack(context)) } }
    val undo = remember { mutableStateListOf<IconDesign>() }
    val redo = remember { mutableStateListOf<IconDesign>() }
    var tab by remember { mutableStateOf(StudioTab.HOME) }
    var status by remember { mutableStateOf("Проекты сохраняются на устройстве автоматически.") }
    var imageReference by remember { mutableStateOf<Bitmap?>(null) }
    var packIconName by remember { mutableStateOf("my_icon") }
    val prefs = remember { context.getSharedPreferences("rt_studio", 0) }
    var packTitle by remember { mutableStateOf(prefs.getString("packTitle", "RT Studio Icon Pack") ?: "RT Studio Icon Pack") }
    var packApplicationId by remember { mutableStateOf(prefs.getString("packApplicationId", "com.rt.iconpack.generated") ?: "com.rt.iconpack.generated") }
    var packageComponent by remember { mutableStateOf("") }
    var importedNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var referenceDetails by remember { mutableStateOf("") }

    LaunchedEffect(design, packItems.toList(), packTitle, packApplicationId) { saveProject(context, design, packItems.toList(), packTitle, packApplicationId) }
    fun change(next: IconDesign) {
        if (next != design) {
            undo.add(design)
            if (undo.size > 40) undo.removeAt(0)
            redo.clear()
            design = next
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) runCatching {
            loadReferenceBitmap(context, uri)
        }.onSuccess {
            imageReference = it
            if (it != null) status = "Референс загружен: " + it.width + " × " + it.height
        }.onFailure { status = "Ошибка импорта изображения: " + (it.localizedMessage ?: "неизвестная ошибка") }
    }
    val packPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching { inspectArchive(context, uri) }.onSuccess { info ->
            importedNames = info.names
            referenceDetails = "Найдено изображений: " + info.count + " · appfilter.xml: " + if (info.hasAppFilter) "есть" else "нет"
            status = "Архив проанализирован. Цвета можно применить как основу нового стиля."
            change(design.copy(background = info.background ?: design.background, foreground = info.foreground ?: design.foreground))
        }.onFailure { status = "Не удалось прочитать архив: " + (it.localizedMessage ?: "ошибка формата") }
    }
    val pngExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        if (uri != null) runCatching {
            context.contentResolver.openOutputStream(uri)?.use { output -> renderIconBitmap(design, 512).compress(Bitmap.CompressFormat.PNG, 100, output) }
                ?: error("Не удалось открыть файл для записи")
        }.onSuccess { status = "PNG экспортирован." }.onFailure { status = "Ошибка экспорта PNG: " + it.message }
    }
    val svgExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/svg+xml")) { uri ->
        if (uri != null) runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(designToSvg(design)) }
                ?: error("Не удалось открыть файл для записи")
        }.onSuccess { status = "SVG экспортирован." }.onFailure { status = "Ошибка экспорта SVG: " + it.message }
    }
    val packExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) runCatching { exportPack(context, uri, packItems.toList(), design, packTitle, packApplicationId) }
            .onSuccess { status = "Проект Icon Pack с PNG, appfilter.xml и workflow экспортирован. Загрузите его в GitHub, чтобы получить APK через Actions." }
            .onFailure { status = "Ошибка экспорта набора: " + it.message }
    }
    val projectExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) runCatching {
            val root = JSONObject().put("format", "rt-studio-project").put("version", 1)
                .put("design", design.toJson()).put("packTitle", packTitle).put("packageId", packApplicationId)
            root.put("pack", JSONArray().apply {
                packItems.forEach { item -> put(JSONObject().put("name", item.name).put("component", item.component).put("design", item.design.toJson())) }
            })
            context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(root.toString(2)) }
                ?: error("Не удалось открыть файл проекта")
        }.onSuccess { status = "Проект RT Studio сохранён в JSON." }
            .onFailure { status = "Ошибка сохранения проекта: " + it.message }
    }
    val projectImporter = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching {
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                ?: error("Не удалось открыть файл проекта")
            require(text.length <= 5_000_000) { "Файл проекта слишком большой" }
            val root = JSONObject(text)
            require(root.optString("format") == "rt-studio-project") { "Это не проект RT Studio" }
            val importedDesign = designFromJson(root.getJSONObject("design"))
            val importedPack = root.optJSONArray("pack") ?: JSONArray()
            val items = (0 until importedPack.length()).map { index ->
                val item = importedPack.getJSONObject(index)
                PackItem(item.optString("name", "icon_$index"), item.optString("component", ""), designFromJson(item.getJSONObject("design")))
            }
            Triple(root, importedDesign, items)
        }.onSuccess { result ->
            change(result.second)
            packItems.clear()
            packItems.addAll(result.third)
            packTitle = result.first.optString("packTitle", "RT Studio Icon Pack")
            packApplicationId = result.first.optString("packageId", "com.rt.iconpack.generated")
            status = "Проект импортирован: " + result.third.size + " иконок в наборе."
        }.onFailure { status = "Ошибка импорта проекта: " + it.message }
    }
    var referenceToExport by remember { mutableStateOf<Bitmap?>(null) }
    val referenceExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        if (uri != null) runCatching {
            val image = referenceToExport ?: error("Нет изображения для экспорта")
            context.contentResolver.openOutputStream(uri)?.use { output -> image.compress(Bitmap.CompressFormat.PNG, 100, output) }
                ?: error("Не удалось открыть файл для записи")
        }.onSuccess { status = "Обработанный референс экспортирован в PNG." }
            .onFailure { status = "Ошибка экспорта референса: " + it.message }
    }
    var backgroundTolerance by remember { mutableStateOf(110f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Column { Text("RT Studio", fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.6).sp); Text("VECTOR ICON WORKSPACE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.2.sp) } },
                actions = {
                    IconButton(onClick = { packPicker.launch(arrayOf("application/zip", "application/vnd.android.package-archive", "*/*")) }) { Icon(Icons.Rounded.FileOpen, contentDescription = "Импортировать icon pack") }
                    IconButton(onClick = { projectImporter.launch(arrayOf("application/json", "text/*", "*/*")) }) { Icon(Icons.Rounded.FileOpen, contentDescription = "Открыть проект") }
                    IconButton(onClick = { projectExporter.launch("rt-studio-project.json") }) { Icon(Icons.Rounded.Save, contentDescription = "Сохранить проект") }
                    IconButton(onClick = { pngExporter.launch("rt-icon.png") }) { Icon(Icons.Rounded.Download, contentDescription = "Экспорт PNG") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            NavigationBar {
                StudioTab.entries.forEach { item ->
                    val icon = when (item) {
                        StudioTab.HOME -> Icons.Rounded.Home
                        StudioTab.EDITOR -> Icons.Rounded.Edit
                        StudioTab.GENERATOR -> Icons.Rounded.AutoAwesome
                        StudioTab.PACK -> Icons.Rounded.Collections
                    }
                    NavigationBarItem(selected = tab == item, onClick = { tab = item }, icon = { Icon(icon, contentDescription = null) }, label = { Text(item.title) })
                }
            }
        }
    ) { padding ->
        when (tab) {
            StudioTab.HOME -> HomeScreen(
                modifier = Modifier.fillMaxSize().padding(padding), design = design, image = imageReference, status = status,
                referenceDetails = referenceDetails, importedNames = importedNames,
                onImportImage = { imagePicker.launch("image/*") }, onImportPack = { packPicker.launch(arrayOf("application/zip", "application/vnd.android.package-archive", "*/*")) },
                onSaveProject = { projectExporter.launch("rt-studio-project.json") }, onLoadProject = { projectImporter.launch(arrayOf("application/json", "text/*", "*/*")) },
                onExportReference = { referenceToExport = imageReference; referenceExporter.launch("reference-processed.png") },
                onCropReference = { imageReference?.let { imageReference = cropReferenceSquare(it); status = "Референс обрезан до центрального квадрата." } },
                onRemoveBackground = { imageReference?.let { imageReference = removeSolidBackground(it, backgroundTolerance.toInt()); status = "Попытка удаления однотонного фона завершена. Проверь края и экспортируй PNG." } },
                backgroundTolerance = backgroundTolerance, onBackgroundTolerance = { backgroundTolerance = it },
                onEditor = { tab = StudioTab.EDITOR }, onGenerator = { tab = StudioTab.GENERATOR }, onAdd = { tab = StudioTab.PACK },
                onTraceImage = {
                    val source = imageReference
                    val path = source?.let { traceBitmapToPath(it) }
                    if (path == null) status = "Не удалось выделить контур. Попробуй изображение с однотонным фоном или прозрачностью."
                    else {
                        val traced = VectorObject(kind = ElementKind.PATH, color = design.foreground, pathData = path)
                        change(design.copy(elements = design.elements + traced))
                        tab = StudioTab.EDITOR
                        status = "Контур приближённо трассирован в редактируемый векторный путь. Проверь его геометрию в редакторе."
                    }
                },
                onUseSampleColors = {
                    val colors = studioPalette.shuffled().take(3)
                    change(design.copy(background = colors[0], foreground = colors[1], accent = colors[2]))
                    status = "Палитра собрана из встроенных цветовых образцов."
                }
            )
            StudioTab.EDITOR -> EditorScreen(
                modifier = Modifier.fillMaxSize().padding(padding), design = design, undoEnabled = undo.isNotEmpty(), redoEnabled = redo.isNotEmpty(),
                onChange = ::change,
                onUndo = { if (undo.isNotEmpty()) { redo.add(design); design = undo.removeAt(undo.lastIndex) } },
                onRedo = { if (redo.isNotEmpty()) { undo.add(design); design = redo.removeAt(redo.lastIndex) } },
                onPng = { pngExporter.launch("rt-icon.png") }, onSvg = { svgExporter.launch("rt-icon.svg") },
                onRandom = { change(randomDesign()); status = "Создан новый процедурный вариант без генеративного ИИ." }
            )
            StudioTab.GENERATOR -> GeneratorScreen(
                modifier = Modifier.fillMaxSize().padding(padding), design = design, onChange = ::change,
                onSaveToPack = { packIconName = "generated_" + packItems.size; tab = StudioTab.PACK },
                onSvg = { svgExporter.launch("generated-icon.svg") }, onPng = { pngExporter.launch("generated-icon.png") }
            )
            StudioTab.PACK -> PackScreen(
                modifier = Modifier.fillMaxSize().padding(padding), design = design, items = packItems,
                packTitle = packTitle, packageId = packApplicationId, onPackTitle = { packTitle = it }, onPackageId = { packApplicationId = it },
                iconName = packIconName, component = packageComponent,
                onName = { packIconName = it }, onComponent = { packageComponent = it },
                onAdd = {
                    val name = packIconName.trim().lowercase().replace(Regex("[^a-z0-9_]+"), "_").trim('_').ifBlank { "icon_" + packItems.size }
                    if (packItems.any { it.name == name }) status = "Иконка с именем " + name + " уже есть."
                    else { packItems.add(PackItem(name, packageComponent.trim(), design)); status = "Иконка " + name + " добавлена в набор." }
                },
                onDelete = { packItems.removeAt(it) }, onExport = { packExporter.launch("rt-studio-icon-pack-project.zip") },
                onImport = { packPicker.launch(arrayOf("application/zip", "application/vnd.android.package-archive", "*/*")) }, status = status
            )
        }
    }
}

@Composable
private fun HomeScreen(
    modifier: Modifier, design: IconDesign, image: Bitmap?, status: String, referenceDetails: String, importedNames: List<String>,
    onImportImage: () -> Unit, onImportPack: () -> Unit, onSaveProject: () -> Unit, onLoadProject: () -> Unit,
    onExportReference: () -> Unit, onCropReference: () -> Unit, onRemoveBackground: () -> Unit,
    backgroundTolerance: Float, onBackgroundTolerance: (Float) -> Unit,
    onEditor: () -> Unit, onGenerator: () -> Unit, onAdd: () -> Unit, onTraceImage: () -> Unit, onUseSampleColors: () -> Unit
) {
    LazyColumn(modifier, contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Векторная мастерская", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("Создавай иконки геометрией и правилами. Все варианты строятся локально, без генеративного ИИ.", color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .8f))
                    Box(Modifier.fillMaxWidth().height(205.dp).clip(RoundedCornerShape(22.dp)).background(Color(0xFF20232B)), contentAlignment = Alignment.Center) { IconCanvas(design, Modifier.size(176.dp)) }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = onEditor, modifier = Modifier.weight(1f)) { Icon(Icons.Rounded.Edit, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("Редактор") }
                        FilledTonalButton(onClick = onGenerator, modifier = Modifier.weight(1f)) { Icon(Icons.Rounded.AutoAwesome, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("Генератор") }
                    }
                }
            }
        }
        item {
            Text("Импорт и проекты", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                ActionTile("Референс", "PNG, JPG, WebP", Icons.Rounded.Image, Modifier.weight(1f), onImportImage)
                ActionTile("Icon pack", "APK или ZIP", Icons.Rounded.FileOpen, Modifier.weight(1f), onImportPack)
            }
        }
        if (image != null) item {
            Card { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Изображение-референс", fontWeight = FontWeight.SemiBold)
                Image(image.asImageBitmap(), contentDescription = "Референс для сравнения", modifier = Modifier.fillMaxWidth().height(180.dp))
                Text("Порог удаления фона: " + backgroundTolerance.toInt(), style = MaterialTheme.typography.bodySmall)
                Slider(value = backgroundTolerance, onValueChange = onBackgroundTolerance, valueRange = 30f..240f)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onCropReference) { Text("Кадрировать") }
                    OutlinedButton(onClick = onRemoveBackground) { Text("Удалить фон") }
                    OutlinedButton(onClick = onExportReference) { Text("Экспорт PNG") }
                }
                OutlinedButton(onClick = onUseSampleColors, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.Palette, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Применить палитру образцов") }
                Button(onClick = onTraceImage, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.AutoAwesome, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Автотрассировать контур в вектор") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onSaveProject, modifier = Modifier.weight(1f)) { Icon(Icons.Rounded.Save, contentDescription = null); Spacer(Modifier.width(4.dp)); Text("Проект JSON") }
                    OutlinedButton(onClick = onLoadProject, modifier = Modifier.weight(1f)) { Icon(Icons.Rounded.FileOpen, contentDescription = null); Spacer(Modifier.width(4.dp)); Text("Открыть JSON") }
                }
            } }
        }
        if (referenceDetails.isNotBlank()) item {
            Card { Column(Modifier.padding(14.dp)) {
                Text("Результат анализа архива", fontWeight = FontWeight.SemiBold)
                Text(referenceDetails, style = MaterialTheme.typography.bodyMedium)
                if (importedNames.isNotEmpty()) Text(importedNames.take(12).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } }
        }
        item {
            Text("Рабочий процесс", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            FeatureRow("01", "Создай основу", "Выбери силуэт, глиф, палитру, масштаб и поворот.")
            FeatureRow("02", "Собери стиль", "Включай и выключай слои фона, символа и декора.")
            FeatureRow("03", "Экспортируй", "Сохраняй текущую иконку как PNG или редактируемый SVG.")
            FeatureRow("04", "Собери набор", "Добавляй иконки и экспортируй ресурсный ZIP с appfilter.xml.")
        }
        item { Text(status, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun ActionTile(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f))) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(25.dp))
            Text(title, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
@Composable
private fun FeatureRow(number: String, title: String, detail: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(34.dp)) { Box(contentAlignment = Alignment.Center) { Text(number, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer) } }
        Column { Text(title, fontWeight = FontWeight.SemiBold); Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun IconCanvas(design: IconDesign, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val canvas = drawContext.canvas.nativeCanvas
        val saved = canvas.save()
        canvas.scale(size.width / 512f, size.height / 512f)
        renderIcon(canvas, design, 512)
        canvas.restoreToCount(saved)
    }
}

@Composable
private fun EditorScreen(
    modifier: Modifier, design: IconDesign, undoEnabled: Boolean, redoEnabled: Boolean, onChange: (IconDesign) -> Unit,
    onUndo: () -> Unit, onRedo: () -> Unit, onPng: () -> Unit, onSvg: () -> Unit, onRandom: () -> Unit
) {
    var selectedElementId by remember { mutableStateOf(design.elements.lastOrNull()?.id) }
    val selectedElement = design.elements.firstOrNull { it.id == selectedElementId }
    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column { Text("Редактор иконок", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("Векторные примитивы · живой предпросмотр", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                IconButton(onClick = onRandom) { Icon(Icons.Rounded.Refresh, contentDescription = "Случайный вариант") }
            }
        }
        item {
            Card(shape = RoundedCornerShape(25.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF20232B))) {
                Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    IconCanvas(design, Modifier.size(230.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onUndo, enabled = undoEnabled) { Icon(Icons.Rounded.Undo, contentDescription = null); Text("Назад") }
                        OutlinedButton(onClick = onRedo, enabled = redoEnabled) { Icon(Icons.Rounded.Redo, contentDescription = null); Text("Вперёд") }
                    }
                }
            }
        }
        item { Text("1 · Форма подложки", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconShape.entries.forEach { item -> FilterChip(selected = design.shape == item, onClick = { onChange(design.copy(shape = item)) }, label = { Text(item.title) }) }
            }
        }
        item { Text("2 · Символ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Glyph.entries.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { glyph -> FilterChip(selected = design.glyph == glyph, onClick = { onChange(design.copy(glyph = glyph)) }, label = { Text(glyph.title) }) }
                    }
                }
            }
        }
        item { ColorPicker("Цвет подложки", design.background) { onChange(design.copy(background = it)) } }
        item { ColorPicker("Цвет символа", design.foreground) { onChange(design.copy(foreground = it)) } }
        item { ColorPicker("Цвет акцентов", design.accent) { onChange(design.copy(accent = it)) } }
        item {
            Card { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Масштаб символа: " + (design.glyphScale * 100).toInt() + "%", fontWeight = FontWeight.SemiBold)
                Slider(value = design.glyphScale, onValueChange = { onChange(design.copy(glyphScale = it)) }, valueRange = .55f..1.4f)
                Text("Поворот: " + design.rotation.toInt() + "°", fontWeight = FontWeight.SemiBold)
                Slider(value = design.rotation, onValueChange = { onChange(design.copy(rotation = it)) }, valueRange = -45f..45f)
                Text("Скругление: " + (design.corner * 100).toInt() + "%", fontWeight = FontWeight.SemiBold)
                Slider(value = design.corner, onValueChange = { onChange(design.copy(corner = it)) }, valueRange = .08f.. .48f)
            } }
        }
        item {
            Card { Column(Modifier.padding(10.dp)) {
                Text("3 · Слои", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                LayerToggle("Фон", design.backgroundVisible) { onChange(design.copy(backgroundVisible = it)) }
                LayerToggle("Символ", design.glyphVisible) { onChange(design.copy(glyphVisible = it)) }
                LayerToggle("Декоративные элементы", design.accentVisible) { onChange(design.copy(accentVisible = it)) }
                LayerToggle("Контур подложки", design.outline) { onChange(design.copy(outline = it)) }
            } }
        }
        item { Text("4 · Векторные элементы", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        item {
            Text("Добавляй геометрические слои поверх иконки. Их можно перемещать, масштабировать, вращать, перекрашивать, скрывать и использовать как прозрачные вырезы.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                ElementKind.entries.filter { it != ElementKind.PATH }.forEach { kind ->
                    FilterChip(selected = false, onClick = {
                        val element = VectorObject(kind = kind, color = design.foreground)
                        selectedElementId = element.id
                        onChange(design.copy(elements = design.elements + element))
                    }, label = { Text(kind.title) })
                }
            }
        }
        if (design.elements.isEmpty()) item {
            Text("Нет дополнительных векторных слоёв. Добавь круг, линию, звезду или другую форму выше.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        itemsIndexed(design.elements, key = { _, element -> element.id }) { index, element ->
            Card(onClick = { selectedElementId = element.id }, colors = CardDefaults.cardColors(containerColor = if (element.id == selectedElementId) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Checkbox(checked = element.visible, onCheckedChange = { value -> onChange(design.copy(elements = design.elements.map { if (it.id == element.id) it.copy(visible = value) else it })) })
                    Column(Modifier.weight(1f).clickable { selectedElementId = element.id }) {
                        Text(element.kind.title, fontWeight = FontWeight.SemiBold)
                        Text(if (element.cutout) "Прозрачное отверстие" else "Векторный объект", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = {
                        if (index > 0) { val list = design.elements.toMutableList(); val previous = list[index - 1]; list[index - 1] = list[index]; list[index] = previous; onChange(design.copy(elements = list)) }
                    }, enabled = index > 0) { Text("↑") }
                    IconButton(onClick = {
                        if (index < design.elements.lastIndex) { val list = design.elements.toMutableList(); val next = list[index + 1]; list[index + 1] = list[index]; list[index] = next; onChange(design.copy(elements = list)) }
                    }, enabled = index < design.elements.lastIndex) { Text("↓") }
                    IconButton(onClick = { onChange(design.copy(elements = design.elements.filterNot { it.id == element.id })); if (selectedElementId == element.id) selectedElementId = null }) { Text("×", style = MaterialTheme.typography.titleLarge) }
                }
            }
        }
        if (selectedElement != null) item {
            Card { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Параметры слоя: " + selectedElement.kind.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Позиция X: " + (selectedElement.x * 100).toInt() + "%", fontWeight = FontWeight.SemiBold)
                Slider(value = selectedElement.x, onValueChange = { value -> onChange(design.copy(elements = design.elements.map { if (it.id == selectedElement.id) it.copy(x = value) else it })) }, valueRange = .08f.. .92f)
                Text("Позиция Y: " + (selectedElement.y * 100).toInt() + "%", fontWeight = FontWeight.SemiBold)
                Slider(value = selectedElement.y, onValueChange = { value -> onChange(design.copy(elements = design.elements.map { if (it.id == selectedElement.id) it.copy(y = value) else it })) }, valueRange = .08f.. .92f)
                Text("Масштаб: " + (selectedElement.scale * 100).toInt() + "%", fontWeight = FontWeight.SemiBold)
                Slider(value = selectedElement.scale, onValueChange = { value -> onChange(design.copy(elements = design.elements.map { if (it.id == selectedElement.id) it.copy(scale = value) else it })) }, valueRange = .15f..2.5f)
                Text("Поворот: " + selectedElement.rotation.toInt() + "°", fontWeight = FontWeight.SemiBold)
                Slider(value = selectedElement.rotation, onValueChange = { value -> onChange(design.copy(elements = design.elements.map { if (it.id == selectedElement.id) it.copy(rotation = value) else it })) }, valueRange = -180f..180f)
                Text("Непрозрачность: " + (selectedElement.opacity * 100).toInt() + "%", fontWeight = FontWeight.SemiBold)
                Slider(value = selectedElement.opacity, onValueChange = { value -> onChange(design.copy(elements = design.elements.map { if (it.id == selectedElement.id) it.copy(opacity = value) else it })) }, valueRange = 0f..1f)
                LayerToggle("Вырезать прозрачное отверстие", selectedElement.cutout) { value -> onChange(design.copy(elements = design.elements.map { if (it.id == selectedElement.id) it.copy(cutout = value) else it })) }
            } }
        }
        if (selectedElement != null && selectedElement.kind == ElementKind.PATH) item {
            Card { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Редактирование SVG-контура", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Поддерживаются команды M, L и Z. Изменение координат здесь меняет форму векторного слоя.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = selectedElement.pathData,
                    onValueChange = { path -> onChange(design.copy(elements = design.elements.map { if (it.id == selectedElement.id) it.copy(pathData = path) else it })) },
                    label = { Text("SVG pathData") }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 8
                )
            } }
        }
        if (selectedElement != null) item {
            ColorPicker("Цвет выбранного слоя", selectedElement.color) { color -> onChange(design.copy(elements = design.elements.map { if (it.id == selectedElement.id) it.copy(color = color) else it })) }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onPng, modifier = Modifier.weight(1f)) { Icon(Icons.Rounded.Download, contentDescription = null); Spacer(Modifier.width(5.dp)); Text("PNG") }
                FilledTonalButton(onClick = onSvg, modifier = Modifier.weight(1f)) { Icon(Icons.Rounded.Save, contentDescription = null); Spacer(Modifier.width(5.dp)); Text("SVG") }
            }
        }
    }
}

@Composable
private fun ColorPicker(title: String, selected: Int, onSelect: (Int) -> Unit) {
    Card { Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Box(Modifier.size(25.dp).clip(CircleShape).background(Color(selected)).border(1.dp, MaterialTheme.colorScheme.outline, CircleShape))
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            studioPalette.forEach { color ->
                Box(Modifier.size(31.dp).clip(CircleShape).background(Color(color)).border(if (selected == color) 3.dp else 1.dp, if (selected == color) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape).clickable { onSelect(color) })
            }
        }
    } }
}
@Composable
private fun LayerToggle(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Layers, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(title, modifier = Modifier.weight(1f).padding(start = 10.dp))
        Checkbox(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun GeneratorScreen(modifier: Modifier, design: IconDesign, onChange: (IconDesign) -> Unit, onSaveToPack: () -> Unit, onSvg: () -> Unit, onPng: () -> Unit) {
    LazyColumn(modifier, contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Процедурный генератор", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Варианты строятся из геометрии, параметров и цветовых правил, а не генерируются нейросетью.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF20232B))) {
                Box(Modifier.fillMaxWidth().height(270.dp), contentAlignment = Alignment.Center) { IconCanvas(design, Modifier.size(230.dp)) }
            }
        }
        item { Button(onClick = { onChange(randomDesign()) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.AutoAwesome, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Сгенерировать новый вариант") } }
        item { Text("Рецепты стиля", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PresetButton("Midnight", "Тёмная база · холодный акцент") { onChange(IconDesign(0xFF111827.toInt(), -1, 0xFF60A5FA.toInt(), IconShape.SQUIRCLE, Glyph.BOLT, .25f, 1.0f, 0f, true)) }
                PresetButton("Mint Glass", "Зелёная палитра · чистая геометрия") { onChange(IconDesign(0xFF064E3B.toInt(), 0xFFECFDF5.toInt(), 0xFF6EE7B7.toInt(), IconShape.CIRCLE, Glyph.GALLERY)) }
                PresetButton("Soft Pastel", "Светлая подложка · тёмный символ") { onChange(IconDesign(0xFFF1F5F9.toInt(), 0xFF0F172A.toInt(), 0xFF94A3B8.toInt(), IconShape.ROUNDED, Glyph.STAR, .32f, 1.0f, 0f, true)) }
                PresetButton("Electric Violet", "Контрастный акцент · диагональная композиция") { onChange(IconDesign(0xFF4A044E.toInt(), 0xFFFDF4FF.toInt(), 0xFFF0ABFC.toInt(), IconShape.HEXAGON, Glyph.PLAY, .25f, 1.0f, -8f, false)) }
                PresetButton("Amber Terminal", "Тёплая палитра · технический глиф") { onChange(IconDesign(0xFF7C2D12.toInt(), 0xFFFFF7ED.toInt(), 0xFFFDBA74.toInt(), IconShape.DIAMOND, Glyph.TERMINAL)) }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onPng, modifier = Modifier.weight(1f)) { Text("Экспорт PNG") }
                OutlinedButton(onClick = onSvg, modifier = Modifier.weight(1f)) { Text("Экспорт SVG") }
            }
            Button(onClick = onSaveToPack, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.Collections, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Добавить в набор") }
        }
    }
}
@Composable
private fun PresetButton(title: String, subtitle: String, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f))) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(43.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Palette, contentDescription = null) }
            Column { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun PackScreen(
    modifier: Modifier, design: IconDesign, items: List<PackItem>, packTitle: String, packageId: String,
    onPackTitle: (String) -> Unit, onPackageId: (String) -> Unit, iconName: String, component: String,
    onName: (String) -> Unit, onComponent: (String) -> Unit, onAdd: () -> Unit, onDelete: (Int) -> Unit,
    onExport: () -> Unit, onImport: () -> Unit, status: String
) {
    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("Сборщик Icon Pack", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Добавляй созданные иконки и ComponentInfo. Экспорт включает Android-проект, launcher-метаданные и GitHub Actions для сборки APK.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Card { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = packTitle, onValueChange = onPackTitle, label = { Text("Название Icon Pack") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = packageId, onValueChange = onPackageId, label = { Text("Application ID") }, supportingText = { Text("Например, com.example.myicons") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconCanvas(design, Modifier.size(76.dp))
                    Column(Modifier.padding(start = 12.dp)) { Text("Текущая иконка", fontWeight = FontWeight.Bold); Text(design.glyph.title + " · " + design.shape.title, style = MaterialTheme.typography.bodySmall) }
                }
                OutlinedTextField(value = iconName, onValueChange = onName, label = { Text("Имя ресурса") }, supportingText = { Text("Латиница, цифры и _; например, spotify") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = component, onValueChange = onComponent, label = { Text("ComponentInfo (необязательно)") }, placeholder = { Text("ComponentInfo{package/class}") }, supportingText = { Text("Точный компонент приложения, если известен") }, modifier = Modifier.fillMaxWidth(), minLines = 1)
                Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.Collections, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Добавить текущую иконку") }
            } }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Содержимое набора", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(items.size.toString() + " шт.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (items.isEmpty()) item {
            Card { Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Collections, contentDescription = null, modifier = Modifier.size(35.dp), tint = MaterialTheme.colorScheme.primary)
                Text("Набор пока пуст", fontWeight = FontWeight.SemiBold)
                Text("Добавь текущую иконку. Пакет сохраняется между запусками.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } }
        }
        itemsIndexed(items, key = { index, item -> item.name + "_" + index }) { index, item ->
            Card {
                Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconCanvas(item.design, Modifier.size(65.dp))
                    Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                        Text(item.name, fontWeight = FontWeight.Bold)
                        Text(item.design.glyph.title + " · " + item.design.shape.title, style = MaterialTheme.typography.bodySmall)
                        Text(if (item.component.isBlank()) "Нет mapping-компонента" else item.component, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { onDelete(index) }) { Text("×", style = MaterialTheme.typography.headlineMedium) }
                }
            }
        }
        item {
            Button(onClick = onExport, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.Download, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Экспорт проекта для APK") }
            OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.FileOpen, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Анализировать APK/ZIP референс") }
            Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun exportPack(context: android.content.Context, uri: Uri, items: List<PackItem>, current: IconDesign, requestedTitle: String, requestedPackageId: String) {
    val title = requestedTitle.trim().ifBlank { "RT Studio Icon Pack" }.take(80)
    val titleXml = title.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;")
    val candidatePackage = requestedPackageId.trim().split(".").joinToString(".") { part ->
        val cleaned = part.replace(Regex("[^A-Za-z0-9_]"), "_")
        if (cleaned.firstOrNull()?.isLetter() == true) cleaned else "p_" + cleaned
    }
    val packageId = if (candidatePackage.contains(".") && candidatePackage.split(".").all { it.isNotBlank() }) candidatePackage else "com.rt.iconpack.generated"
    val actual = if (items.isEmpty()) listOf(PackItem("rt_studio_preview", "", current)) else items
    fun resourceName(raw: String): String {
        val clean = raw.lowercase().replace(Regex("[^a-z0-9_]+"), "_").trim('_').ifBlank { "icon" }
        return if (clean.first().isDigit()) "icon_" + clean else clean
    }
    fun xmlEscape(value: String) = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;")
    val used = mutableSetOf<String>()
    val namedItems = actual.mapNotNull { item ->
        val name = resourceName(item.name)
        if (used.add(name)) item to name else null
    }
    val mappings = namedItems.filter { it.first.component.startsWith("ComponentInfo{") && it.first.component.endsWith("}") }
    val xml = buildString {
        append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n")
        mappings.forEach { (item, name) ->
            append("  <item component=\"").append(xmlEscape(item.component)).append("\" drawable=\"").append(name).append("\" />\n")
        }
        append("</resources>\n")
    }
    val activityTitle = title.replace("\\", "\\\\").replace("\"", "\\\"").replace("$", "\\$").replace("\n", " ").replace("\r", " ")
    val activitySource = "package com.rt.iconpack.generated\n\n" +
        "import android.app.Activity\nimport android.os.Bundle\nimport android.view.Gravity\nimport android.widget.LinearLayout\nimport android.widget.TextView\n\n" +
        "class MainActivity : Activity() {\n" +
        "    override fun onCreate(savedInstanceState: Bundle?) {\n        super.onCreate(savedInstanceState)\n" +
        "        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(32, 40, 32, 40) }\n" +
        "        val heading = TextView(this).apply { text = \"" + activityTitle + "\"; textSize = 24f; gravity = Gravity.CENTER; setPadding(0, 0, 0, 20) }\n" +
        "        val details = TextView(this).apply { text = \"Icon pack resources are included. Open your launcher's icon settings and select this pack if the launcher supports its theme format.\"; textSize = 16f; gravity = Gravity.CENTER }\n" +
        "        root.addView(heading); root.addView(details); setContentView(root)\n" +
        "    }\n}\n"
    val manifest = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
        "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">\n" +
        "  <application android:label=\"" + titleXml + "\" android:icon=\"@drawable/rt_pack_launcher\" android:allowBackup=\"false\" android:theme=\"@android:style/Theme.Material.Light.NoActionBar\">\n" +
        "    <activity android:name=\".MainActivity\" android:exported=\"true\">\n" +
        "      <intent-filter><action android:name=\"android.intent.action.MAIN\"/><category android:name=\"android.intent.category.LAUNCHER\"/></intent-filter>\n" +
        "      <intent-filter><action android:name=\"org.adw.launcher.THEMES\"/><category android:name=\"android.intent.category.DEFAULT\"/></intent-filter>\n" +
        "      <intent-filter><action android:name=\"com.teslacoilsw.launcher.THEMES\"/><category android:name=\"android.intent.category.DEFAULT\"/></intent-filter>\n" +
        "    </activity>\n" +
        "    <meta-data android:name=\"com.novalauncher\" android:resource=\"@xml/appfilter\"/>\n" +
        "    <meta-data android:name=\"com.teslacoilsw.launcher\" android:resource=\"@xml/appfilter\"/>\n" +
        "    <meta-data android:name=\"com.anddoes.launcher\" android:resource=\"@xml/appfilter\"/>\n" +
        "  </application>\n</manifest>\n"
    val appGradle = """
        plugins {
            id("com.android.application")
        }

        android {
            namespace = "com.rt.iconpack.generated"
            compileSdk = 35
            defaultConfig {
                applicationId = "$packageId"
                minSdk = 23
                targetSdk = 35
                versionCode = 1
                versionName = "1.0"
            }
        }
    """.trimIndent() + "\n"
    val settings = """
        pluginManagement {
            repositories { google(); mavenCentral(); gradlePluginPortal() }
        }
        dependencyResolutionManagement {
            repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
            repositories { google(); mavenCentral() }
        }
        rootProject.name = "RTIconPack"
        include(":app")
    """.trimIndent() + "\n"
    val workflow = "name: Build Icon Pack APK\n" +
        "on:\n  push:\n    branches: [ \"main\" ]\n  workflow_dispatch:\n" +
        "permissions:\n  contents: read\n" +
        "jobs:\n  build:\n    runs-on: ubuntu-latest\n    steps:\n" +
        "      - uses: actions/checkout@v4\n" +
        "      - uses: actions/setup-java@v4\n        with:\n          distribution: temurin\n          java-version: \"17\"\n" +
        "      - name: Install Gradle\n        run: |\n" +
        "          curl --fail --location --retry 3 https://services.gradle.org/distributions/gradle-8.11.1-bin.zip --output \"\$RUNNER_TEMP/gradle.zip\"\n" +
        "          unzip -q \"\$RUNNER_TEMP/gradle.zip\" -d \"\$RUNNER_TEMP\"\n" +
        "          echo \"\$RUNNER_TEMP/gradle-8.11.1/bin\" >> \"\$GITHUB_PATH\"\n" +
        "      - name: Assemble installable debug APK\n        run: gradle --no-daemon assembleDebug\n" +
        "      - uses: actions/upload-artifact@v4\n        with:\n          name: generated-icon-pack-apk\n          path: app/build/outputs/apk/debug/app-debug.apk\n          if-no-files-found: error\n          retention-days: 14\n"
    val readme = title + "\n\nGenerated by RT Studio.\n\n1. Upload the contents of this project ZIP to a GitHub repository on the main branch.\n2. Open the repository's Actions tab. The workflow builds a debug APK and publishes it as the generated-icon-pack-apk artifact.\n3. Download the APK artifact and install it. Then select the icon pack in a launcher that supports its theme metadata/appfilter format.\n\nMappings included: " + mappings.size + "\nResource icons included: " + namedItems.size + "\n\nThis uses common ADW/Nova-style metadata, but no icon-pack format works in every launcher. Missing ComponentInfo mappings must be added in RT Studio before export.\n"
    val output = context.contentResolver.openOutputStream(uri) ?: error("Не удалось открыть файл для записи")
    ZipOutputStream(output.buffered()).use { zip ->
        fun addText(path: String, text: String) {
            zip.putNextEntry(ZipEntry(path)); zip.write(text.toByteArray(Charsets.UTF_8)); zip.closeEntry()
        }
        fun addPng(path: String, icon: IconDesign) {
            zip.putNextEntry(ZipEntry(path)); renderIconBitmap(icon, 512).compress(Bitmap.CompressFormat.PNG, 100, zip); zip.closeEntry()
        }
        addPng("app/src/main/res/drawable-nodpi/rt_pack_launcher.png", namedItems.firstOrNull()?.first?.design ?: current)
        namedItems.forEach { (item, name) -> addPng("app/src/main/res/drawable-nodpi/" + name + ".png", item.design) }
        addText("app/src/main/res/xml/appfilter.xml", xml)
        addText("app/src/main/AndroidManifest.xml", manifest)
        addText("app/src/main/java/com/rt/iconpack/generated/MainActivity.kt", activitySource)
        addText("app/build.gradle.kts", appGradle)
        addText("settings.gradle.kts", settings)
        addText("build.gradle.kts", "plugins { id(\"com.android.application\") version \"8.8.2\" apply false }\n")
        addText(".github/workflows/build-apk.yml", workflow)
        addText("README.md", readme)
    }
}
