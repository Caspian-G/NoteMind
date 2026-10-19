package com.caspian.notemind.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.caspian.notemind.data.AppData
import com.caspian.notemind.data.MistakeFolder
import com.caspian.notemind.ui.theme.AlertRed
import com.caspian.notemind.ui.theme.Hairline
import com.caspian.notemind.ui.theme.HairlineSoft
import com.caspian.notemind.ui.theme.Ink
import com.caspian.notemind.ui.theme.InkFaint
import com.caspian.notemind.ui.theme.InkSoft
import com.caspian.notemind.ui.theme.Panel
import com.caspian.notemind.ui.theme.PanelDeep
import com.caspian.notemind.ui.theme.Paper
import com.caspian.notemind.util.loadStoredImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {}
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = InkFaint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = Ink)
                }
            }
        },
        actions = { actions() },
        windowInsets = WindowInsets(0, 0, 0, 0),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Paper,
            titleContentColor = Ink,
            actionIconContentColor = Ink,
            navigationIconContentColor = Ink
        )
    )
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier, hint: String? = null) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = InkFaint,
                textAlign = TextAlign.Center
            )
            if (hint != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = InkFaint.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = "删除",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium, color = InkSoft) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(confirmLabel, color = AlertRed, fontWeight = FontWeight.Medium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = InkSoft) }
        },
        containerColor = Paper
    )
}

@Composable
fun InputDialog(
    title: String,
    initial: String = "",
    placeholder: String = "",
    confirmLabel: String = "确定",
    singleLine: Boolean = true,
    allowEmpty: Boolean = false,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                placeholder = { Text(placeholder, color = InkFaint) },
                singleLine = singleLine,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (allowEmpty || value.isNotBlank()) { onConfirm(value); onDismiss() } }
            ) { Text(confirmLabel, color = Ink, fontWeight = FontWeight.Medium) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = InkSoft) }
        },
        containerColor = Paper
    )
}

/** 通用的图片异步加载，按文件名从应用私有目录读取。 */
@Composable
fun StoredImage(
    name: String,
    modifier: Modifier = Modifier,
    maxPx: Int = 720,
    contentScale: ContentScale = ContentScale.Crop
) {
    var bitmap by remember(name, maxPx) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(name, maxPx) {
        bitmap = withContext(Dispatchers.IO) { loadStoredImage(name, maxPx) }
    }
    val current = bitmap
    if (current == null) {
        Box(modifier.background(PanelDeep))
    } else {
        Image(
            bitmap = current,
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale
        )
    }
}

/** 横向缩略图条。onRemove 为 null 时只读。 */
@Composable
fun PhotoStrip(
    names: List<String>,
    modifier: Modifier = Modifier,
    thumbnailSize: Int = 84,
    onRemove: ((String) -> Unit)? = null,
    onOpen: ((Int) -> Unit)? = null
) {
    if (names.isEmpty()) return
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(names.size) { index ->
            val name = names[index]
            Box(
                Modifier
                    .size(thumbnailSize.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, Hairline, RoundedCornerShape(10.dp))
                    .clickable(enabled = onOpen != null) { onOpen?.invoke(index) }
            ) {
                StoredImage(name, Modifier.fillMaxSize(), maxPx = 360)
                if (onRemove != null) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(3.dp)
                            .size(20.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xCCFFFFFF))
                            .clickable { onRemove(name) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "删除照片",
                            tint = AlertRed,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 全屏看图：左右拖动切换上一张/下一张，双指捏合缩放，双击在图内放大与还原。
 * 没有上一张/下一张按钮，只留一个页码提示。
 */
@Composable
fun ImagePreviewDialog(names: List<String>, startIndex: Int, onDismiss: () -> Unit) {
    if (names.isEmpty()) return
    val pagerState = rememberPagerState(
        initialPage = startIndex.coerceIn(0, names.size - 1)
    ) { names.size }
    // 图片被放大时锁住翻页，否则拖动会变成切换图片而不是平移图片。
    var zoomed by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(color = Color(0xFF101010), modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = !zoomed
                ) { page ->
                    ZoomableImage(
                        name = names[page],
                        onZoomChanged = { zoomed = it }
                    )
                }

                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(46.dp)
                        .clip(RoundedCornerShape(23.dp))
                        .background(Color(0x73000000))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "关闭",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                if (names.size > 1) {
                    Text(
                        text = "${pagerState.currentPage + 1} / ${names.size}",
                        color = Color(0xDDFFFFFF),
                        fontSize = 14.sp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 30.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x73000000))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

/**
 * 单张可缩放图片。
 * 单指平移只在已放大时生效，否则把手势让给 Pager 去做左右翻页。
 * 非 private 是为了让 androidTest 能直接对它做捏合手势验证。
 */
@Composable
internal fun ZoomableImage(
    name: String,
    onZoomChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember(name) { mutableFloatStateOf(1f) }
    var offset by remember(name) { mutableStateOf(Offset.Zero) }

    LaunchedEffect(scale) { onZoomChanged(scale > 1.01f) }
    LaunchedEffect(name) {
        scale = 1f
        offset = Offset.Zero
    }

    Box(
        modifier
            .fillMaxSize()
            .pointerInput(name) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var multiTouch = false
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.count { it.pressed } >= 2) multiTouch = true
                        val zoom = event.calculateZoom()
                        val pan = event.calculatePan()
                        // 未放大且是单指拖动时完全不消费事件，交给 Pager 翻页。
                        if (multiTouch || scale > 1.001f) {
                            val next = (scale * zoom).coerceIn(1f, 6f)
                            if (next != scale || pan != Offset.Zero) {
                                scale = next
                                offset = if (next <= 1.001f) Offset.Zero else offset + pan
                                event.changes.forEach { it.consume() }
                            }
                        }
                        if (event.changes.none { it.pressed }) break
                    }
                }
            }
            .pointerInput(name) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1.01f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        StoredImage(
            name = name,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
            maxPx = 2048,
            contentScale = ContentScale.Fit
        )
    }
}

/**
 * 向左拖动露出右侧的红色删除键，点它才真的执行删除。
 * 只负责手势、动画和一个"点一下关闭"的行为，删什么由调用方决定。
 */
@Composable
fun SwipeToDeleteBox(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    deleteLabel: String = "删除",
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val actionWidth = 84.dp
    // 滑开到位后，卡片右缘和红色按钮之间留一道缝，避免两个色块贴死。
    val actionGap = 10.dp
    // 四个角都收圆弧，和右侧对称；滑开到底时左侧也不会是直角。
    val actionCorner = RoundedCornerShape(14.dp)
    val density = LocalDensity.current
    val revealPx = with(density) { (actionWidth + actionGap).toPx() }
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }

    val dragState = rememberDraggableState { delta ->
        scope.launch {
            offsetX.snapTo((offsetX.value + delta).coerceIn(-revealPx, 0f))
        }
    }

    Box(modifier.fillMaxWidth()) {
        Row(
            Modifier.matchParentSize(),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                Modifier
                    .width(actionWidth)
                    .fillMaxHeight()
                    .clip(actionCorner)
                    .background(AlertRed)
                    .clickable {
                        scope.launch { offsetX.animateTo(0f, tween(160)) }
                        onDelete()
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.DeleteOutline,
                        contentDescription = deleteLabel,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = deleteLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }

        Box(
            Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .draggable(
                    state = dragState,
                    orientation = Orientation.Horizontal,
                    onDragStopped = { velocity ->
                        val open = when {
                            velocity < -600f -> true
                            velocity > 600f -> false
                            else -> offsetX.value <= -revealPx * 0.5f
                        }
                        offsetX.animateTo(if (open) -revealPx else 0f, tween(180))
                    }
                )
                .then(
                    if (onClick == null) Modifier else Modifier.clickable {
                        // 已经滑开时，点内容只是收起来，避免误触打开。
                        if (offsetX.value < -1f) {
                            scope.launch { offsetX.animateTo(0f, tween(160)) }
                        } else {
                            onClick()
                        }
                    }
                )
        ) {
            content()
        }
    }
}

/**
 * 目标文件夹选择器：展示完整目录树，用于移动文件或文件夹。
 * [disabledIds] 内的条目不可选（移动文件夹时排除自身与后代）。
 */
@Composable
fun FolderPickerDialog(
    data: AppData,
    title: String,
    disabledIds: Set<String> = emptySet(),
    allowRoot: Boolean = true,
    rootLabel: String = "最外层",
    onPick: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf<String?>(null) }
    var confirmed by remember { mutableStateOf(false) }

    fun flatten(parentId: String?, depth: Int): List<Pair<MistakeFolder, Int>> =
        data.childFolders(parentId).flatMap { folder ->
            listOf(folder to depth) + flatten(folder.id, depth + 1)
        }

    val rows = flatten(null, 0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                if (allowRoot) {
                    PickerRow(
                        label = rootLabel,
                        selected = selected == null,
                        enabled = true,
                        depth = 0,
                        onClick = { selected = null; confirmed = true }
                    )
                }
                if (rows.isEmpty() && !allowRoot) {
                    Text("还没有任何文件夹", color = InkFaint, style = MaterialTheme.typography.bodySmall)
                }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(300.dp)
                ) {
                    items(rows.size) { i ->
                        val (folder, depth) = rows[i]
                        PickerRow(
                            label = folder.name,
                            selected = selected == folder.id,
                            enabled = folder.id !in disabledIds,
                            depth = depth + if (allowRoot) 1 else 0,
                            onClick = { selected = folder.id; confirmed = true }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onPick(selected); onDismiss() },
                enabled = confirmed
            ) { Text("移动到这里", color = if (confirmed) Ink else InkFaint) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = InkSoft) }
        },
        containerColor = Paper
    )
}

@Composable
private fun PickerRow(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    depth: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Panel else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(start = (10 + depth * 16).dp, top = 10.dp, bottom = 10.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (depth == 0) Icons.Filled.Home else Icons.Filled.Folder,
            contentDescription = null,
            tint = when {
                !enabled -> InkFaint.copy(alpha = 0.35f)
                selected -> Ink
                else -> InkSoft
            },
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = when {
                !enabled -> InkFaint.copy(alpha = 0.35f)
                selected -> Ink
                else -> Ink
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!enabled) {
            Spacer(Modifier.width(6.dp))
            Text("不可选", style = MaterialTheme.typography.labelSmall, color = InkFaint)
        }
    }
}

/** 分隔用的细线，统一 1px 观感。 */
@Composable
fun ThinDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(HairlineSoft))
}
