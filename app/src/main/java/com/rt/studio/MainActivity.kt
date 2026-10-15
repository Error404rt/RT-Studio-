package com.rt.studio

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.CropFree
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RTStudioTheme { StudioHome() } }
    }
}

private val StudioLight = lightColorScheme(
    primary = Color(0xFF465D91), onPrimary = Color.White,
    primaryContainer = Color(0xFFD9E2FF), onPrimaryContainer = Color(0xFF001A42),
    secondary = Color(0xFF585E71), secondaryContainer = Color(0xFFDDE2F9),
    tertiary = Color(0xFF76546F), tertiaryContainer = Color(0xFFFFD7F2),
    background = Color(0xFFF8F9FF), surface = Color(0xFFF8F9FF),
    surfaceVariant = Color(0xFFE2E2EC)
)
private val StudioDark = darkColorScheme(
    primary = Color(0xFFB1C5FF), onPrimary = Color(0xFF142F60),
    primaryContainer = Color(0xFF2D4678), onPrimaryContainer = Color(0xFFD9E2FF),
    secondary = Color(0xFFC1C6DD), tertiary = Color(0xFFE5B9D9),
    background = Color(0xFF111318), surface = Color(0xFF111318),
    surfaceVariant = Color(0xFF44464F)
)

@Composable
private fun RTStudioTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) StudioDark else StudioLight, content = content)
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun StudioHome() {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var status by remember { mutableStateOf("Ваша следующая иконка начинается здесь") }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            }.onSuccess { decoded ->
                if (decoded != null) {
                    bitmap = decoded
                    status = "Изображение загружено · ${decoded.width} × ${decoded.height}"
                } else status = "Не удалось прочитать изображение. Попробуйте другой файл."
            }.onFailure { status = "Ошибка открытия: ${it.localizedMessage ?: "неизвестная ошибка"}" }
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("RT", fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp)
                    Text("Studio", fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp)
                    Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(50), modifier = Modifier.padding(start = 8.dp)) {
                        Text("BETA", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            actions = {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(end = 12.dp).size(42.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Tune, contentDescription = "Настройки") }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )
    }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Card(shape = RoundedCornerShape(30.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f), modifier = Modifier.size(44.dp)) {
                                Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
                            }
                            Column(Modifier.weight(1f)) {
                                Text("Icon Studio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("Быстрый старт", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.76f))
                            }
                        }
                        if (bitmap == null) {
                            Box(
                                modifier = Modifier.fillMaxWidth().aspectRatio(1.8f).clip(RoundedCornerShape(22.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.58f))
                                    .border(1.dp, MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f), RoundedCornerShape(22.dp))
                                    .clickable { imagePicker.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(34.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Text("Импортировать изображение", color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.SemiBold)
                                    Text("PNG · WebP · JPG", color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.68f), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        } else {
                            Box(Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)), contentAlignment = Alignment.Center) {
                                Image(bitmap!!.asImageBitmap(), contentDescription = "Выбранное изображение", modifier = Modifier.fillMaxSize().padding(12.dp))
                            }
                            Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                FilledTonalButton(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Rounded.Image, contentDescription = null); Spacer(Modifier.size(8.dp)); Text("Заменить")
                                }
                                Button(onClick = { status = "Предпросмотр работает. Инструменты редактирования добавляются следующими этапами." }, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Rounded.CropFree, contentDescription = null); Spacer(Modifier.size(8.dp)); Text("Открыть")
                                }
                            }
                        }
                    }
                }
            }
            item { Text("С чего начнём?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            item {
                ScenarioCard({ Icon(Icons.Rounded.Layers, contentDescription = null) }, "Дополнить Icon Pack", "Импортируй набор и найди недостающие иконки.", MaterialTheme.colorScheme.secondaryContainer) {
                    status = "Импорт Icon Pack APK будет добавлен отдельным этапом."
                }
            }
            item {
                ScenarioCard({ Icon(Icons.Rounded.Collections, contentDescription = null) }, "Создать набор", "Задай палитру, стиль и правила оформления.", MaterialTheme.colorScheme.tertiaryContainer) {
                    status = "Создание набора — следующий этап бета-разработки."
                }
            }
            item {
                ScenarioCard({ Icon(Icons.Rounded.Palette, contentDescription = null) }, "Редактор иконок", "Импорт, холст и подготовка изображения к обработке.", MaterialTheme.colorScheme.primaryContainer) {
                    imagePicker.launch("image/*")
                }
            }
            item {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f), shape = RoundedCornerShape(18.dp)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScenarioCard(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit
) {
    Card(onClick = onClick, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(color = accent, shape = RoundedCornerShape(17.dp), modifier = Modifier.size(54.dp)) {
                Box(contentAlignment = Alignment.Center) { icon() }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("›", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
