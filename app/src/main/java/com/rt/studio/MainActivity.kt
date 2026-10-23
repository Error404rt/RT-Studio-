package com.rt.studio

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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

private fun saveProject(context: android.content.Context, design: IconDesign, items: List<PackItem>) {
    val a = JSONArray()
    items.forEach { item -> a.put(JSONObject().put("name", item.name).put("component", item.component).put("design", item.design.toJson())) }
    context.getSharedPreferences("rt_studio", 0).edit().putString("design", design.toJson().toString()).putString("pack", a.toString()).apply()
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
    var packageComponent by remember { mutableStateOf("") }
    var importedNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var referenceDetails by remember { mutableStateOf("") }

    LaunchedEffect(design, packItems.toList()) { saveProject(context, design, packItems.toList()) }
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
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
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
        if (uri != null) runCatching { exportPack(context, uri, packItems.toList(), design) }
            .onSuccess { status = "ZIP с PNG, appfilter.xml и инструкцией экспортирован." }
            .onFailure { status = "Ошибка экспорта набора: " + it.message }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Column { Text("RT Studio", fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.6).sp); Text("VECTOR ICON WORKSPACE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.2.sp) } },
                actions = {
                    IconButton(onClick = { packPicker.launch(arrayOf("application/zip", "application/vnd.android.package-archive", "*/*")) }) { Icon(Icons.Rounded.FileOpen, contentDescription = "Импортировать icon pack") }
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
                onEditor = { tab = StudioTab.EDITOR }, onGenerator = { tab = StudioTab.GENERATOR }, onAdd = { tab = StudioTab.PACK },
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
                iconName = packIconName, component = packageComponent,
                onName = { packIconName = it }, onComponent = { packageComponent = it },
                onAdd = {
                    val name = packIconName.trim().lowercase().replace(Regex("[^a-z0-9_]+"), "_").trim('_').ifBlank { "icon_" + packItems.size }
                    if (packItems.any { it.name == name }) status = "Иконка с именем " + name + " уже есть."
                    else { packItems.add(PackItem(name, packageComponent.trim(), design)); status = "Иконка " + name + " добавлена в набор." }
                },
                onDelete = { packItems.removeAt(it) }, onExport = { packExporter.launch("rt-studio-icon-pack.zip") },
                onImport = { packPicker.launch(arrayOf("application/zip", "application/vnd.android.package-archive", "*/*")) }, status = status
            )
        }
    }
}

@Composable
private fun HomeScreen(
    modifier: Modifier, design: IconDesign, image: Bitmap?, status: String, referenceDetails: String, importedNames: List<String>,
    onImportImage: () -> Unit, onImportPack: () -> Unit, onEditor: () -> Unit, onGenerator: () -> Unit, onAdd: () -> Unit, onUseSampleColors: () -> Unit
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
                OutlinedButton(onClick = onUseSampleColors, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.Palette, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Применить палитру образцов") }
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
    modifier: Modifier, design: IconDesign, items: List<PackItem>, iconName: String, component: String,
    onName: (String) -> Unit, onComponent: (String) -> Unit, onAdd: () -> Unit, onDelete: (Int) -> Unit,
    onExport: () -> Unit, onImport: () -> Unit, status: String
) {
    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("Сборщик Icon Pack", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Добавляй созданные иконки и их ComponentInfo для appfilter.xml. Экспорт сохраняет ресурсы, но не выдаёт себя за собранный APK.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Card { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
            Button(onClick = onExport, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.Download, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Экспортировать ZIP-ресурсы") }
            OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.FileOpen, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Анализировать APK/ZIP референс") }
            Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun exportPack(context: android.content.Context, uri: Uri, items: List<PackItem>, current: IconDesign) {
    val output = context.contentResolver.openOutputStream(uri) ?: error("Не удалось открыть файл для записи")
    ZipOutputStream(output.buffered()).use { zip ->
        val actual = if (items.isEmpty()) listOf(PackItem("rt_studio_preview", "", current)) else items
        val used = mutableSetOf<String>()
        actual.forEach { item ->
            val name = item.name.lowercase().replace(Regex("[^a-z0-9_]+"), "_").ifBlank { "icon" }.let {
                if (it.first().isDigit()) "icon_" + it else it
            }
            if (used.add(name)) {
                zip.putNextEntry(ZipEntry("res/drawable-nodpi/" + name + ".png"))
                renderIconBitmap(item.design, 512).compress(Bitmap.CompressFormat.PNG, 100, zip)
                zip.closeEntry()
            }
        }
        val mappings = actual.filter { it.component.startsWith("ComponentInfo{") && it.component.endsWith("}") }
        val xml = buildString {
            append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n")
            actual.forEach { item ->
                if (item.component.startsWith("ComponentInfo{") && item.component.endsWith("}")) {
                    val name = item.name.lowercase().replace(Regex("[^a-z0-9_]+"), "_").ifBlank { "icon" }.let { if (it.first().isDigit()) "icon_" + it else it }
                    append("  <item component=\"").append(item.component.replace("&", "&amp;").replace("\"", "&quot;")).append("\" drawable=\"").append(name).append("\" />\n")
                }
            }
            append("</resources>\n")
        }
        zip.putNextEntry(ZipEntry("res/xml/appfilter.xml")); zip.write(xml.toByteArray(Charsets.UTF_8)); zip.closeEntry()
        val readme = "RT Studio icon pack resources\n\nIncludes procedural PNG icons in res/drawable-nodpi and an appfilter.xml mapping for entries with a valid ComponentInfo{package/class}.\nThis archive is a resource bundle, not an installable APK. Launcher compatibility and APK packaging require a launcher-specific Android project/build.\nMapping entries: " + mappings.size + "\n"
        zip.putNextEntry(ZipEntry("README.txt")); zip.write(readme.toByteArray(Charsets.UTF_8)); zip.closeEntry()
    }
}
