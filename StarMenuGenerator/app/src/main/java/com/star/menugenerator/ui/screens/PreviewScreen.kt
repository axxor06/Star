package com.star.menugenerator.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Image as ImageIcon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.star.menugenerator.StarApp
import com.star.menugenerator.domain.ALIGN_CENTER
import com.star.menugenerator.domain.ALIGN_LEFT
import com.star.menugenerator.domain.Block
import com.star.menugenerator.domain.FONT_CLEAN
import com.star.menugenerator.domain.FONT_ELEGANT
import com.star.menugenerator.domain.FONT_TRADITIONAL
import com.star.menugenerator.domain.MenuSettings
import com.star.menugenerator.domain.TEMPLATES
import com.star.menugenerator.domain.TemplateSpec
import com.star.menugenerator.domain.getTemplate
import com.star.menugenerator.domain.summarize
import com.star.menugenerator.render.ExportKind
import com.star.menugenerator.render.ExportResult
import com.star.menugenerator.render.MenuRenderer
import com.star.menugenerator.ui.AppViewModel
import com.star.menugenerator.ui.Screen
import com.star.menugenerator.ui.components.AccentButton
import com.star.menugenerator.ui.components.PrimaryButton
import com.star.menugenerator.ui.components.StarHeader
import com.star.menugenerator.ui.components.StepIndicator
import com.star.menugenerator.ui.theme.StarDeep
import com.star.menugenerator.ui.theme.StarPurple
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Preview scale: 0.8 = 843 x 1194 px on screen. Export uses its own, higher resolution. */
private const val PREVIEW_SCALE = 0.8f

@Composable
private fun PageImage(
    renderer: MenuRenderer, spec: TemplateSpec, settings: MenuSettings,
    blocks: List<Block>, index: Int, count: Int, onClick: () -> Unit,
) {
    val bitmap by produceState<ImageBitmap?>(null, spec.id, settings, blocks, index, count) {
        value = withContext(Dispatchers.Default) {
            renderer.renderBitmap(spec, settings, blocks, index, count, PREVIEW_SCALE).asImageBitmap()
        }
    }
    Surface(
        shape = RoundedCornerShape(6.dp), shadowElevation = 6.dp, color = Color.White,
        modifier = Modifier.fillMaxWidth().aspectRatio(spec.w.toFloat() / spec.h).clickable(onClick = onClick),
    ) {
        val b = bitmap
        if (b == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = StarPurple) }
        } else {
            Image(bitmap = b, contentDescription = "Menu page ${index + 1} of $count", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
        }
    }
}

/** Full-screen page with pinch-to-zoom and drag, rendered at full template resolution. */
@Composable
private fun ZoomDialog(
    renderer: MenuRenderer, spec: TemplateSpec, settings: MenuSettings,
    blocks: List<Block>, index: Int, count: Int, onDismiss: () -> Unit,
) {
    val bitmap by produceState<ImageBitmap?>(null, spec.id, settings, blocks, index, count) {
        value = withContext(Dispatchers.Default) { renderer.renderBitmap(spec, settings, blocks, index, count, 1.2f).asImageBitmap() }
    }
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.92f))) {
            val b = bitmap
            if (b == null) {
                CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
            } else {
                Image(
                    bitmap = b, contentDescription = "Menu page ${index + 1} zoomed", contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                if (scale == 1f) { offsetX = 0f; offsetY = 0f } else { offsetX += pan.x; offsetY += pan.y }
                            }
                        }
                        .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offsetX, translationY = offsetY),
                )
            }
            IconButton(onClick = onDismiss, modifier = Modifier.statusBarsPadding().align(Alignment.TopEnd).padding(8.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = StarDeep)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CustomizeSheet(vm: AppViewModel, onDismiss: () -> Unit) {
    val st = vm.menu.settings
    fun update(f: (MenuSettings) -> MenuSettings) = vm.editMenu { it.copy(settings = f(it.settings)) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Customize the menu card", style = MaterialTheme.typography.titleLarge, color = StarDeep)
            Text("These only change how the card looks. Your entries and totals stay exactly the same.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Text("Template", fontWeight = FontWeight.Bold, color = StarDeep)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TEMPLATES.forEach { t ->
                    TemplateThumb(t, selected = vm.templateId == t.id, onClick = { vm.selectTemplate(t.id) }, modifier = Modifier.weight(1f))
                }
            }

            Text("Font", fontWeight = FontWeight.Bold, color = StarDeep)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(FONT_ELEGANT to "Elegant", FONT_CLEAN to "Clean", FONT_TRADITIONAL to "Traditional").forEach { (id, label) ->
                    FilterChip(selected = st.font == id, onClick = { update { it.copy(font = id) } }, label = { Text(label) })
                }
            }

            Text("Text size  ${(st.fontScale * 100).toInt()}%", fontWeight = FontWeight.Bold, color = StarDeep)
            Slider(
                value = st.fontScale.toFloat(), onValueChange = { v -> update { it.copy(fontScale = (Math.round(v * 20f) / 20.0)) } },
                valueRange = 0.7f..1.4f, steps = 13,
            )

            Text("Alignment", fontWeight = FontWeight.Bold, color = StarDeep)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = st.align == ALIGN_LEFT, onClick = { update { it.copy(align = ALIGN_LEFT) } }, label = { Text("Left") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.FormatAlignLeft, contentDescription = null, modifier = Modifier.size(18.dp)) },
                )
                FilterChip(
                    selected = st.align == ALIGN_CENTER, onClick = { update { it.copy(align = ALIGN_CENTER) } }, label = { Text("Center") },
                    leadingIcon = { Icon(Icons.Filled.FormatAlignCenter, contentDescription = null, modifier = Modifier.size(18.dp)) },
                )
            }

            Text("Show on the card", fontWeight = FontWeight.Bold, color = StarDeep)
            ToggleRow("CAPITAL meal headings", st.caps) { v -> update { it.copy(caps = v) } }
            ToggleRow("Quantities", st.showQty) { v -> update { it.copy(showQty = v) } }
            ToggleRow("Item prices", st.showPrice) { v -> update { it.copy(showPrice = v) } }
            ToggleRow("Package prices", st.showPackagePrice) { v -> update { it.copy(showPackagePrice = v) } }
            ToggleRow("Buffet prices", st.showBuffetPrice) { v -> update { it.copy(showBuffetPrice = v) } }
            ToggleRow("Buffet section names", st.showBuffetSections) { v -> update { it.copy(showBuffetSections = v) } }
            ToggleRow("Descriptions", st.showDesc) { v -> update { it.copy(showDesc = v) } }
            ToggleRow("Notes", st.showNotes) { v -> update { it.copy(showNotes = v) } }
            ToggleRow("Grand total at the end", st.showGrandTotal) { v -> update { it.copy(showGrandTotal = v) } }
            PrimaryButton("Done", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** Step 3 of 3: live preview of every page, customization, and PNG / PDF export, share and print. */
@Composable
fun PreviewScreen(vm: AppViewModel) {
    val context = LocalContext.current
    val app = context.applicationContext as StarApp
    val renderer = app.renderer
    val exporter = app.exporter
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    val menu = vm.menu
    val spec = getTemplate(vm.templateId)
    val settings = menu.settings

    var pages by remember { mutableStateOf<List<List<Block>>?>(null) }
    var layoutError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(menu, spec.id) {
        try {
            pages = withContext(Dispatchers.Default) { renderer.layoutPages(menu, spec) }
            layoutError = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            layoutError = e.message ?: "Could not lay out the menu"
        }
    }

    var busy by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<ExportResult?>(null) }
    var showCustomize by remember { mutableStateOf(false) }
    var zoomIndex by remember { mutableStateOf<Int?>(null) }

    fun export(kind: ExportKind, saveCopy: Boolean = true, then: ((ExportResult) -> Unit)? = null) {
        val p = pages ?: return
        if (busy != null) return
        scope.launch {
            busy = if (kind == ExportKind.PDF) "Creating PDF…" else "Creating PNG images…"
            progress = ""
            try {
                val base = exporter.baseName(menu.title)
                val onProgress: (Int, Int) -> Unit = { i, n -> progress = "Page $i of $n" }
                val r = if (kind == ExportKind.PDF) {
                    exporter.exportPdf(spec, settings, p, base, saveCopy, onProgress)
                } else {
                    exporter.exportPng(spec, settings, p, base, saveCopy, onProgress)
                }
                if (then != null) then(r) else result = r
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                snackbar.showSnackbar("Export failed: ${e.message ?: "unknown error"}")
            } catch (e: OutOfMemoryError) {
                snackbar.showSnackbar("Export failed: not enough memory. Try a menu with fewer pages.")
            }
            busy = null
        }
    }

    if (showCustomize) CustomizeSheet(vm) { showCustomize = false }

    zoomIndex?.let { zi ->
        pages?.getOrNull(zi)?.let { blocks ->
            ZoomDialog(renderer, spec, settings, blocks, zi, pages?.size ?: 1) { zoomIndex = null }
        }
    }

    result?.let { r ->
        AlertDialog(
            onDismissRequest = { result = null },
            containerColor = Color.White,
            title = { Text(if (r.kind == ExportKind.PDF) "PDF ready" else "PNG images ready") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (r.savedToDownloads) "Saved to your phone: Downloads › STAR Menus"
                        else "Created. Use Share to save it to Files, Drive or send it on WhatsApp.",
                    )
                    r.files.forEach { Text(it.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    if (r.kind == ExportKind.PDF) {
                        TextButton(onClick = { exporter.print(context, r.files.first().file, r.files.first().name) }) {
                            Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Print")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { exporter.share(context, r) }) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Share", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { result = null }) { Text("Close") } },
        )
    }

    Scaffold(
        containerColor = Color(0xFFEDE7F1),
        topBar = {
            Column {
                StarHeader("Preview & export", onBack = { vm.back() })
                Surface(color = Color.White) { StepIndicator(3) }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Surface(color = Color.White, shadowElevation = 8.dp) {
                Column(Modifier.navigationBarsPadding().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (busy != null) {
                        Text("${busy}  $progress", style = MaterialTheme.typography.labelMedium, color = StarPurple)
                        LinearProgressIndicator(Modifier.fillMaxWidth(), color = StarPurple)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AccentButton("Save PDF", onClick = { export(ExportKind.PDF) }, icon = Icons.Filled.PictureAsPdf, enabled = pages != null && busy == null, modifier = Modifier.weight(1f))
                        PrimaryButton("Save PNG", onClick = { export(ExportKind.PNG) }, icon = Icons.Filled.ImageIcon, enabled = pages != null && busy == null, modifier = Modifier.weight(1f))
                    }
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = { vm.popTo(Screen.Editor) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Edit menu")
                }
                OutlinedButton(onClick = { showCustomize = true }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Customize")
                }
            }
            val list = pages
            when {
                layoutError != null -> Text("Could not build the preview: $layoutError", Modifier.padding(24.dp), color = MaterialTheme.colorScheme.error)
                list == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = StarPurple)
                        Spacer(Modifier.height(8.dp))
                        Text("Laying out your menu…")
                    }
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item(key = "info") {
                        val sum = remember(menu) { summarize(menu) }
                        Text(
                            "${list.size} page${if (list.size == 1) "" else "s"} · ${spec.name}" +
                                (if (!sum.complete) " · totals are partial" else "") + "\nTap a page to zoom.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    itemsIndexed(list, key = { i, _ -> "page$i" }) { i, blocks ->
                        PageImage(renderer, spec, settings, blocks, i, list.size, onClick = { zoomIndex = i })
                    }
                }
            }
        }
    }
}
