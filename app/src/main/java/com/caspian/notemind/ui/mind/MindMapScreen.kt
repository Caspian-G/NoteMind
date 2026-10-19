package com.caspian.notemind.ui.mind

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.caspian.notemind.data.AppData
import com.caspian.notemind.data.MindNode
import com.caspian.notemind.data.Store
import com.caspian.notemind.data.Viewport
import com.caspian.notemind.ui.common.ConfirmDialog
import com.caspian.notemind.ui.common.ImagePreviewDialog
import com.caspian.notemind.ui.common.PageTopBar
import com.caspian.notemind.ui.common.PhotoStrip
import com.caspian.notemind.ui.common.StoredImage
import com.caspian.notemind.ui.theme.AlertRed
import com.caspian.notemind.ui.theme.GridLine
import com.caspian.notemind.ui.theme.Hairline
import com.caspian.notemind.ui.theme.Ink
import com.caspian.notemind.ui.theme.InkFaint
import com.caspian.notemind.ui.theme.InkGhost
import com.caspian.notemind.ui.theme.InkSoft
import com.caspian.notemind.ui.theme.Panel
import com.caspian.notemind.ui.theme.Paper
import com.caspian.notemind.util.rememberCameraCapture
import com.caspian.notemind.util.rememberGalleryPicker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val MIN_SCALE = 0.3f
private const val MAX_SCALE = 3.0f
private const val MIN_NODE = 90f
private const val MAX_NODE = 1400f
private const val GRID_STEP_DP = 28f
private const val DEFAULT_NODE = 190f
private const val EXPANDED_H = 380f

/**
 * 模块三：思维导图 / 笔记本。
 *
 * 整页白底 + 灰色十字方格，画布支持单指拖动、双指缩放。
 * 节点默认是圆角正方形；点一下选中，双击选中并直接调出尺寸面板，
 * 选中后通过底部工具栏「展开」把节点原地撑开成详细内容面板，
 * 里面可以继续改文字、继续拍照加图。
 */
@Composable
fun MindMapScreen() {
    val data by Store.state.collectAsStateWithLifecycle()
    // 画布世界坐标统一使用 dp，落盘后在不同屏幕密度下尺寸一致；
    // 手势增量与 Canvas 绘制是像素，需要按密度换算。
    val densityValue = LocalDensity.current.density

    var scale by remember { mutableStateOf(data.viewport.scale) }
    var offset by remember {
        mutableStateOf(Offset(data.viewport.offsetX, data.viewport.offsetY))
    }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var showSizePanel by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var deletingId by remember { mutableStateOf<String?>(null) }
    var previewing by remember { mutableStateOf<Pair<List<String>, Int>?>(null) }
    var connectingId by remember { mutableStateOf<String?>(null) }
    var organizing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val selected = data.node(selectedId)
    val editing = data.node(editingId)
    val connecting = data.node(connectingId)

    /**
     * 自动排版：一棵树从它的主索引位置出发向右生长，根节点自己不动。
     * 坐标是插值过去的，直接跳会让人看不清节点跑哪去了。
     */
    fun organizeTrees() {
        if (organizing || data.nodes.isEmpty()) return
        val roots = selected
            ?.let { listOf(data.nodeTreeRootId(it.id)) }
            ?: data.treeRootIds()
        val target = layoutTrees(data.nodes, roots)
        if (target.isEmpty()) return
        val start = data.nodes.associate { it.id to (it.x to it.y) }

        scope.launch {
            organizing = true
            val steps = 18
            repeat(steps) { step ->
                val t = (step + 1) / steps.toFloat()
                val eased = 1f - (1f - t) * (1f - t)
                Store.setNodePositions(
                    target.mapValues { (id, to) ->
                        val from = start[id] ?: to
                        (from.first + (to.first - from.first) * eased) to
                            (from.second + (to.second - from.second) * eased)
                    }
                )
                delay(16)
            }
            Store.setNodePositions(target)
            organizing = false
        }
    }

    // 视图参数做防抖落盘，拖动过程中只更新内存状态。
    LaunchedEffect(Unit) {
        snapshotFlow { Triple(scale, offset.x, offset.y) }
            .collectLatest { (s, ox, oy) ->
                delay(500)
                Store.setViewport(Viewport(s, ox, oy))
            }
    }

    if (editing != null) {
        NodeEditor(
            node = editing,
            onDone = { title, content, images ->
                Store.updateNodeContent(editing.id, title, content, images)
                editingId = null
            },
            onBack = { editingId = null }
        )
        return
    }

    val takePhotoForSelected = rememberCameraCapture { name ->
        val node = Store.state.value.node(selectedId) ?: return@rememberCameraCapture
        if (name != null) {
            Store.updateNodeContent(node.id, node.title, node.content, node.images + name)
        }
    }
    val pickPhotoForSelected = rememberGalleryPicker { names ->
        val node = Store.state.value.node(selectedId) ?: return@rememberGalleryPicker
        if (names.isNotEmpty()) {
            Store.updateNodeContent(node.id, node.title, node.content, node.images + names)
        }
    }

    fun worldToScreen(p: Offset) = p * scale + offset

    fun centerOfViewportInWorld(): Offset {
        if (canvasSize == IntSize.Zero) return Offset.Zero
        return Offset(
            (canvasSize.width / 2f - offset.x) / scale / densityValue,
            (canvasSize.height / 2f - offset.y) / scale / densityValue
        )
    }

    // 选中的节点画在最上层
    val ordered = remember(data.nodes, selectedId) {
        data.nodes.sortedBy { if (it.id == selectedId) 1 else 0 }
    }

    Scaffold(
        containerColor = Paper,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            PageTopBar(
                title = "思维导图",
                subtitle = selected?.let { "已选中「${it.title.ifBlank { "未命名" }}」" },
                actions = {
                    TextButton(onClick = { organizeTrees() }) {
                        Text(
                            text = "整理",
                            color = if (data.nodes.isEmpty()) InkFaint else InkSoft
                        )
                    }
                    TextButton(onClick = {
                        scale = 1f
                        offset = Offset.Zero
                    }) {
                        Text("复位视图", color = InkSoft)
                    }
                }
            )
        },
        floatingActionButton = {
            androidx.compose.material3.FloatingActionButton(
                // 选中节点时把 + 抬到工具栏上方，避免遮挡工具栏按钮。
                modifier = Modifier.padding(
                    bottom = if (selected != null) (if (showSizePanel) 150.dp else 78.dp) else 0.dp
                ),
                onClick = {
                    val center = centerOfViewportInWorld()
                    val id = Store.addMindNode(
                        x = center.x - DEFAULT_NODE / 2f,
                        y = center.y - DEFAULT_NODE / 2f,
                        title = "笔记主索引",
                        w = DEFAULT_NODE,
                        h = DEFAULT_NODE
                    )
                    selectedId = id
                },
                containerColor = Ink,
                contentColor = Paper,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "新建笔记主索引")
            }
        }
    ) { inner ->
        // 画布与工具栏是并列的两块：工具栏不在画布的手势区域内，
        // 否则“点空白处取消选中”的 tap 监听会抢走工具栏按钮的点击。
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Paper)
                    .onSizeChanged { canvasSize = it }
                    .drawBehind {
                        val step = GRID_STEP_DP.dp.toPx() * scale
                        if (step < 9f) return@drawBehind
                        val arm = (step * 0.16f).coerceIn(1.2f, 4.dp.toPx())
                        val stroke = 1.dp.toPx()
                        var x = offset.x % step
                        if (x > 0f) x -= step
                        while (x <= size.width + step) {
                            var y = offset.y % step
                            if (y > 0f) y -= step
                            while (y <= size.height + step) {
                                drawLine(GridLine, Offset(x - arm, y), Offset(x + arm, y), stroke)
                                drawLine(GridLine, Offset(x, y - arm), Offset(x, y + arm), stroke)
                                y += step
                            }
                            x += step
                        }
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                            val k = newScale / scale
                            val moved = offset + pan
                            offset = centroid - (centroid - moved) * k
                            scale = newScale
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            selectedId = null
                            showSizePanel = false
                        })
                    }
            ) {
            Box(
                Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
                    .drawBehind { drawLinks(ordered, densityValue) }
            ) {
                ordered.forEach { node ->
                    MindNodeView(
                        node = node,
                        selected = node.id == selectedId,
                        scale = scale,
                        onSelect = { selectedId = node.id },
                        onDoubleTap = {
                            selectedId = node.id
                            showSizePanel = true
                        },
                        onDragBy = { delta ->
                            Store.setNodeFrame(
                                node.id,
                                node.x + delta.x,
                                node.y + delta.y,
                                node.w,
                                node.h
                            )
                        },
                        onResizeBy = { dx, dy, growW, growH ->
                            val newW = if (growW) (node.w + dx).coerceIn(MIN_NODE, MAX_NODE) else node.w
                            val newH = if (growH) (node.h + dy).coerceIn(MIN_NODE, MAX_NODE) else node.h
                            Store.setNodeFrame(node.id, node.x, node.y, newW, newH)
                        }
                    )
                }
            }
        }

        if (selected != null) {
            NodeToolbar(
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                node = selected,
                showSizePanel = showSizePanel,
                onToggleSizePanel = { showSizePanel = !showSizePanel },
                onEdit = { editingId = selected.id },
                onExpand = {
                    val target = !selected.expanded
                    Store.setNodeExpanded(selected.id, target)
                    if (target) {
                        // 展开时顺手把节点撑到能看见内容的高度，收起时由 restH 自动还原。
                        val newH = if (selected.h < EXPANDED_H) EXPANDED_H else selected.h
                        val newW =
                            if (selected.h < EXPANDED_H) selected.w.coerceAtLeast(240f) else selected.w
                        if (selected.h < EXPANDED_H) {
                            Store.setNodeFrame(selected.id, selected.x, selected.y, newW, newH)
                        }
                        // 展开后如果超出可视区，顺手把画布平移一点，避免内容被工具栏挡住。
                        val margin = 16f * densityValue
                        val top = selected.y * densityValue * scale + offset.y
                        val bottom = (selected.y + newH) * densityValue * scale + offset.y
                        if (bottom > canvasSize.height - margin) {
                            offset = offset - Offset(0f, bottom - (canvasSize.height - margin))
                        } else if (top < margin) {
                            offset = offset + Offset(0f, margin - top)
                        }
                    }
                },
                onCamera = takePhotoForSelected,
                onGallery = pickPhotoForSelected,
                onAddChild = {
                    val childId = Store.addMindNode(
                        x = selected.x + selected.w + 60f,
                        y = selected.y,
                        title = "子索引",
                        parentId = selected.id,
                        w = 160f,
                        h = 160f
                    )
                    selectedId = childId
                },
                onConnect = { connectingId = selected.id },
                onDetach = { Store.setNodeParent(selected.id, null) },
                onDelete = { deletingId = selected.id },
                onSize = { width, height ->
                    Store.setNodeFrame(selected.id, selected.x, selected.y, width, height)
                },
                onCenter = {
                    val screen = worldToScreen(
                        Offset(
                            (selected.x + selected.w / 2f) * densityValue,
                            (selected.y + selected.h / 2f) * densityValue
                        )
                    )
                    offset = offset + Offset(
                        canvasSize.width / 2f - screen.x,
                        canvasSize.height / 2f - screen.y
                    )
                }
            )
        }

            BackHandler(enabled = selectedId != null) {
                selectedId = null
                showSizePanel = false
            }
        }
    }

    deletingId?.let { id ->
        ConfirmDialog(
            title = "删除这个节点？",
            message = "节点里的文字和照片都会被删除。",
            onConfirm = { Store.deleteMindNode(id); selectedId = null },
            onDismiss = { deletingId = null }
        )
    }

    connecting?.let { node ->
        ParentPickerDialog(
            data = data,
            child = node,
            onPick = { parentId ->
                Store.setNodeParent(node.id, parentId)
                connectingId = null
            },
            onDismiss = { connectingId = null }
        )
    }

    previewing?.let { (names, index) ->
        ImagePreviewDialog(names = names, startIndex = index, onDismiss = { previewing = null })
    }
}

/** 在世界坐标系里画父子节点的连线。[density] 用于把 dp 世界坐标换成绘制用的像素。 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLinks(
    nodes: List<MindNode>,
    density: Float
) {
    val byId = nodes.associateBy { it.id }
    nodes.forEach { node ->
        val parent = node.parentId?.let { byId[it] } ?: return@forEach
        val from = Offset((parent.x + parent.w) * density, (parent.y + parent.h / 2f) * density)
        val to = Offset(node.x * density, (node.y + node.h / 2f) * density)
        val midX = (from.x + to.x) / 2f
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(from.x, from.y)
            cubicTo(midX, from.y, midX, to.y, to.x, to.y)
        }
        drawPath(
            path = path,
            color = Color(0xFFC8C8C8),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
        )
    }
}

@Composable
private fun MindNodeView(
    node: MindNode,
    selected: Boolean,
    scale: Float,
    onSelect: () -> Unit,
    onDoubleTap: () -> Unit,
    onDragBy: (Offset) -> Unit,
    onResizeBy: (dx: Float, dy: Float, growW: Boolean, growH: Boolean) -> Unit
) {
    // 拖动/缩放过程中节点尺寸会持续变化，pointerInput 的 key 必须保持稳定，
    // 因此回调统一通过 rememberUpdatedState 取最新值，否则手势会被中途打断。
    val currentDrag by rememberUpdatedState(onDragBy)
    val currentResize by rememberUpdatedState(onResizeBy)
    val currentSelect by rememberUpdatedState(onSelect)
    val currentDoubleTap by rememberUpdatedState(onDoubleTap)

    // 手柄尺寸按缩放反向补偿，保证在任何缩放下屏幕上看起来一样大。
    val handleSize = (10f / scale).coerceIn(6f, 26f)
    val handleTouch = (26f / scale).coerceIn(16f, 60f)
    val shape = RoundedCornerShape((16f / scale).coerceIn(8f, 20f))
    val densityValue = LocalDensity.current.density

    Box(
        Modifier
            .offset(node.x.dp, node.y.dp)
            .size(node.w.dp, node.h.dp)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .clip(shape)
                .background(Paper)
                .border(
                    width = (if (selected) 2f else 1f).dp,
                    color = if (selected) Ink else Hairline,
                    shape = shape
                )
                .pointerInput(node.id) {
                    detectTapGestures(
                        onTap = { currentSelect() },
                        onDoubleTap = { currentDoubleTap() }
                    )
                }
                .pointerInput(node.id) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        currentDrag(dragAmount / scale / densityValue)
                    }
                }
                .padding((12f).dp)
        ) {
            if (!node.expanded) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = node.title.ifBlank { "未命名" },
                        style = MaterialTheme.typography.titleMedium,
                        color = Ink,
                        textAlign = TextAlign.Center,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Text(
                    text = node.title.ifBlank { "未命名" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = Ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(Hairline))
                Spacer(Modifier.height(6.dp))
                if (node.content.isNotBlank()) {
                    Text(
                        text = node.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    )
                } else {
                    Text(
                        "还没有内容，点下方「编辑内容」写点什么",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkGhost,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (node.images.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        Modifier.fillMaxWidth().height((54f).dp),
                        horizontalArrangement = Arrangement.spacedBy((6f).dp)
                    ) {
                        node.images.take(3).forEach { name ->
                            StoredImage(
                                name = name,
                                modifier = Modifier
                                    .size((54f).dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(1.dp, Hairline, RoundedCornerShape(6.dp)),
                                maxPx = 240
                            )
                        }
                        if (node.images.size > 3) {
                            Text(
                                "+${node.images.size - 3}",
                                style = MaterialTheme.typography.labelSmall,
                                color = InkFaint,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "点「编辑内容」查看全部 ${node.images.size} 张照片",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkGhost
                    )
                }
            }
        }

        if (selected) {
            // 右边缘：改宽度
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .offset { IntOffset(handleTouch.toInt() / 2, 0) }
                    .size(handleTouch.dp, (handleTouch * 2).dp)
                    .pointerInput(node.id) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            currentResize(drag.x / scale / densityValue, 0f, true, false)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.size(handleSize.dp).clip(RoundedCornerShape(50)).background(Ink))
            }
            // 下边缘：改高度
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .offset { IntOffset(0, handleTouch.toInt() / 2) }
                    .size((handleTouch * 2).dp, handleTouch.dp)
                    .pointerInput(node.id) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            currentResize(0f, drag.y / scale / densityValue, false, true)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.size(handleSize.dp).clip(RoundedCornerShape(50)).background(Ink))
            }
            // 右下角：同时改长宽
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset { IntOffset(handleTouch.toInt() / 2, handleTouch.toInt() / 2) }
                    .size(handleTouch.dp)
                    .pointerInput(node.id) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            currentResize(
                                drag.x / scale / densityValue,
                                drag.y / scale / densityValue,
                                true,
                                true
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size((handleSize * 1.3f).dp)
                        .clip(RoundedCornerShape(50))
                        .background(Ink)
                )
            }
        }
    }
}

@Composable
private fun NodeToolbar(
    modifier: Modifier,
    node: MindNode,
    showSizePanel: Boolean,
    onToggleSizePanel: () -> Unit,
    onEdit: () -> Unit,
    onExpand: () -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onAddChild: () -> Unit,
    onConnect: () -> Unit,
    onDetach: () -> Unit,
    onDelete: () -> Unit,
    onSize: (Float, Float) -> Unit,
    onCenter: () -> Unit
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xF7FFFFFF))
            .border(1.dp, Hairline, RoundedCornerShape(16.dp))
            .padding(vertical = 8.dp)
    ) {
        if (showSizePanel) {
            SizePanel(node = node, onSize = onSize)
            Box(Modifier.fillMaxWidth().height(1.dp).background(Hairline))
            Spacer(Modifier.height(6.dp))
        }
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolButton("编辑内容", Icons.Filled.EditNote, onEdit)
            ToolButton(if (node.expanded) "收起" else "展开", Icons.Filled.OpenInFull, onExpand)
            ToolButton("拍照", Icons.Filled.PhotoCamera, onCamera)
            ToolButton("相册", Icons.Filled.PhotoLibrary, onGallery)
            ToolButton("长宽", Icons.Filled.Straighten, onToggleSizePanel, active = showSizePanel)
            ToolButton("子索引", Icons.Filled.SubdirectoryArrowRight, onAddChild)
            ToolButton("连接", Icons.Filled.Link, onConnect)
            // 没有父节点时「断开」没有意义，直接不显示。
            if (node.parentId != null) {
                ToolButton("断开", Icons.Filled.LinkOff, onDetach)
            }
            ToolButton("居中", Icons.Filled.CenterFocusStrong, onCenter)
            ToolButton("删除", Icons.Filled.DeleteOutline, onDelete, danger = true)
        }
    }
}

@Composable
private fun SizePanel(node: MindNode, onSize: (Float, Float) -> Unit) {    var w by remember(node.id) { mutableStateOf(node.w.roundToInt().toString()) }
    var h by remember(node.id) { mutableStateOf(node.h.roundToInt().toString()) }

    fun commit() {
        val nw = w.toFloatOrNull()?.coerceIn(MIN_NODE, MAX_NODE) ?: node.w
        val nh = h.toFloatOrNull()?.coerceIn(MIN_NODE, MAX_NODE) ?: node.h
        onSize(nw, nh)
    }

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = w,
            onValueChange = { w = it.filter { c -> c.isDigit() }.take(4) },
            label = { Text("宽 dp") },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = h,
            onValueChange = { h = it.filter { c -> c.isDigit() }.take(4) },
            label = { Text("高 dp") },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = { onSize(node.h, node.h); w = node.h.roundToInt().toString(); h = node.h.roundToInt().toString() }) {
            Text("方形", color = InkSoft)
        }
        TextButton(onClick = { commit() }) {
            Text("应用", color = Ink, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ToolButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    active: Boolean = false,
    danger: Boolean = false
) {
    val tint = when {
        danger -> AlertRed
        active -> Ink
        else -> InkSoft
    }
    Column(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) Panel else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(19.dp))
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}

/** 节点详细内容编辑页：改文字 + 拍照加图。 */
@Composable
private fun NodeEditor(
    node: MindNode,
    onDone: (String, String, List<String>) -> Unit,
    onBack: () -> Unit
) {
    var title by remember(node.id) { mutableStateOf(node.title) }
    var content by remember(node.id) { mutableStateOf(node.content) }
    var images by remember(node.id) { mutableStateOf(node.images) }
    var previewIndex by remember { mutableStateOf<Int?>(null) }

    val takePhoto = rememberCameraCapture { name -> if (name != null) images = images + name }
    val pickPhotos = rememberGalleryPicker { names -> if (names.isNotEmpty()) images = images + names }

    fun finish() = onDone(title, content, images)

    BackHandler { finish() }

    Scaffold(
        containerColor = Paper,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            PageTopBar(
                title = "节点内容",
                subtitle = node.title.ifBlank { "未命名" },
                onBack = { finish() },
                actions = {
                    TextButton(onClick = { finish() }) {
                        Text("完成", color = Ink, fontWeight = FontWeight.Medium)
                    }
                }
            )
        }
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .imePadding()
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("索引名") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            // 图片放在索引名和正文之间，并放大缩略图，让它成为页面上最显眼的部分。
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ToolbarWideButton("拍照", Icons.Filled.PhotoCamera, Modifier.weight(1f), takePhoto)
                ToolbarWideButton("从相册选", Icons.Filled.PhotoLibrary, Modifier.weight(1f), pickPhotos)
            }
            if (images.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                PhotoStrip(
                    names = images,
                    thumbnailSize = 118,
                    onRemove = { name -> images = images - name },
                    onOpen = { index -> previewIndex = index }
                )
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("正文") },
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
            Spacer(Modifier.height(14.dp))
        }
    }

    previewIndex?.let { index ->
        ImagePreviewDialog(names = images, startIndex = index, onDismiss = { previewIndex = null })
    }
}

@Composable
private fun ToolbarWideButton(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier
            .height(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Panel)
            .border(1.dp, Hairline, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = InkSoft, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = Ink)
    }
}

/**
 * 选择父索引。点一行就立即挂上去，[onPick] 传 null 表示断开当前连接。
 *
 * 自己和自己所有的后代都会被禁选 —— 认它们当父索引会让树成环。
 */
@Composable
private fun ParentPickerDialog(
    data: AppData,
    child: MindNode,
    onPick: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val forbidden = remember(data.nodes, child.id) {
        data.descendantNodeIds(child.id) + child.id
    }
    val rows = remember(data.nodes) { flattenIndexTree(data.nodes) }
    val currentParent = data.node(child.parentId)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "「${child.title.ifBlank { "未命名" }}」的父索引",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    text = "当前父索引：${currentParent?.title?.ifBlank { "未命名" } ?: "无（它自己就是主索引）"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkFaint
                )
                Spacer(Modifier.height(8.dp))
                if (currentParent != null) {
                    ParentPickRow(
                        label = "断开与「${currentParent.title.ifBlank { "未命名" }}」的连接",
                        indent = 0,
                        enabled = true,
                        selected = false,
                        danger = true,
                        onClick = { onPick(null) }
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Hairline))
                }
                if (rows.isEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "画布上还没有别的索引",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkFaint
                    )
                } else {
                    LazyColumn(Modifier.fillMaxWidth().height(300.dp)) {
                        items(rows.size) { i ->
                            val (node, depth) = rows[i]
                            val isSelf = node.id == child.id
                            val isDescendant = node.id in forbidden && !isSelf
                            ParentPickRow(
                                label = node.title.ifBlank { "未命名" },
                                indent = depth,
                                enabled = !isSelf && !isDescendant,
                                selected = node.id == child.parentId,
                                danger = false,
                                note = when {
                                    isSelf -> "自身"
                                    isDescendant -> "子节点"
                                    else -> null
                                },
                                onClick = { onPick(node.id) }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "连接只改父子关系，不会挪动节点；排位置可以点顶栏「整理」。",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkGhost
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭", color = InkSoft) }
        },
        containerColor = Paper
    )
}

@Composable
private fun ParentPickRow(
    label: String,
    indent: Int,
    enabled: Boolean,
    selected: Boolean,
    danger: Boolean,
    note: String? = null,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Panel else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(start = (10 + indent * 16).dp, top = 10.dp, bottom = 10.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (danger) Icons.Filled.LinkOff else Icons.Filled.AccountTree,
            contentDescription = null,
            tint = when {
                danger -> AlertRed
                !enabled -> InkFaint.copy(alpha = 0.35f)
                else -> InkSoft
            },
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = when {
                danger -> AlertRed
                !enabled -> InkFaint.copy(alpha = 0.35f)
                else -> Ink
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (note != null) {
            Text(note, style = MaterialTheme.typography.labelSmall, color = InkFaint)
        }
        if (selected) {
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Filled.Check, null, tint = Ink, modifier = Modifier.size(15.dp))
        }
    }
}

/** 按树形结构把节点拍平，带上缩进层级，让选择列表能看出父子关系。 */
private fun flattenIndexTree(nodes: List<MindNode>): List<Pair<MindNode, Int>> {
    val childrenOf = nodes.filter { it.parentId != null }.groupBy { it.parentId }
    val out = mutableListOf<Pair<MindNode, Int>>()
    val visited = mutableSetOf<String>()

    fun walk(parentId: String?, depth: Int) {
        childrenOf[parentId].orEmpty().sortedBy { it.createdAt }.forEach { node ->
            if (!visited.add(node.id)) return@forEach
            out += node to depth.coerceAtMost(8)
            walk(node.id, depth + 1)
        }
    }
    walk(null, 0)

    // 兜底：父节点已被删掉的孤儿节点也要能选到。
    nodes.filterNot { it.id in visited }.sortedBy { it.createdAt }.forEach { out += it to 0 }
    return out
}
