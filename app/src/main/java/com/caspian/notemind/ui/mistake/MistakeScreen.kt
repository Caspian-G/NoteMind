package com.caspian.notemind.ui.mistake

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.caspian.notemind.data.AppData
import com.caspian.notemind.data.MistakeEntry
import com.caspian.notemind.data.MistakeFolder
import com.caspian.notemind.data.Store
import com.caspian.notemind.ui.common.ConfirmDialog
import com.caspian.notemind.ui.common.EmptyState
import com.caspian.notemind.ui.common.FolderPickerDialog
import com.caspian.notemind.ui.common.ImagePreviewDialog
import com.caspian.notemind.ui.common.InputDialog
import com.caspian.notemind.ui.common.PageTopBar
import com.caspian.notemind.ui.common.PhotoStrip
import com.caspian.notemind.ui.common.StoredImage
import com.caspian.notemind.ui.common.SwipeToDeleteBox
import com.caspian.notemind.ui.common.ThinDivider
import com.caspian.notemind.ui.theme.AlertRed
import com.caspian.notemind.ui.theme.Hairline
import com.caspian.notemind.ui.theme.Ink
import com.caspian.notemind.ui.theme.InkFaint
import com.caspian.notemind.ui.theme.InkGhost
import com.caspian.notemind.ui.theme.InkSoft
import com.caspian.notemind.ui.theme.Panel
import com.caspian.notemind.ui.theme.Paper
import com.caspian.notemind.util.formatTimestamp
import com.caspian.notemind.util.rememberCameraCapture
import com.caspian.notemind.util.rememberGalleryPicker

/**
 * 模块二：错题集。
 * 文件夹可以无限层级嵌套，也可以直接在任意层级下创建错题文件；
 * 文件和文件夹都支持“移动到”，因此层级是可编辑的而不是一次写死的。
 */
@Composable
fun MistakeScreen() {
    val data by Store.state.collectAsStateWithLifecycle()

    // 文件夹导航栈，只保存 id，避免文件夹改名/移动后路径失效。
    var path by remember { mutableStateOf(listOf<String>()) }
    // 过滤掉已经被移动或删除的层级（删除文件夹后自动退回上一层）
    val validPath = path.filter { data.folder(it) != null }
    LaunchedEffect(validPath, path) {
        if (validPath != path) path = validPath
    }
    val currentFolderId = validPath.lastOrNull()

    var editingEntryId by remember { mutableStateOf<String?>(null) }
    var createMenu by remember { mutableStateOf(false) }
    var newFolderFor by remember { mutableStateOf<String?>(null) }
    var renaming by remember { mutableStateOf<MistakeFolder?>(null) }
    var movingFolder by remember { mutableStateOf<MistakeFolder?>(null) }
    var movingEntry by remember { mutableStateOf<MistakeEntry?>(null) }
    var deletingFolder by remember { mutableStateOf<MistakeFolder?>(null) }
    var previewing by remember { mutableStateOf<Pair<List<String>, Int>?>(null) }

    val editingEntry = data.entries.firstOrNull { it.id == editingEntryId }
    if (editingEntry != null) {
        EntryEditor(
            entry = editingEntry,
            onDone = { title, desc, images ->
                Store.updateEntry(editingEntry.id, title, desc, images)
                editingEntryId = null
            },
            onDelete = {
                Store.deleteEntry(editingEntry.id)
                editingEntryId = null
            },
            onBack = { editingEntryId = null }
        )
        return
    }

    val folders = data.childFolders(currentFolderId)
    val entries = currentFolderId?.let { data.entriesIn(it) } ?: emptyList()
    val folder = data.folder(currentFolderId)

    Scaffold(
        containerColor = Paper,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column {
                PageTopBar(
                    title = folder?.name ?: "错题集",
                    onBack = if (currentFolderId != null) {
                        { path = validPath.dropLast(1) }
                    } else null
                )
                if (currentFolderId != null) {
                    BreadcrumbBar(
                        data = data,
                        path = validPath,
                        onJump = { index -> path = validPath.take(index + 1) },
                        onRoot = { path = emptyList() }
                    )
                }
            }
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(
                    onClick = { createMenu = true },
                    containerColor = Ink,
                    contentColor = Paper,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "新建")
                }
                DropdownMenu(expanded = createMenu, onDismissRequest = { createMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("新建文件夹") },
                        leadingIcon = { Icon(Icons.Filled.Folder, null, Modifier.size(18.dp)) },
                        onClick = { createMenu = false; newFolderFor = "" }
                    )
                    if (currentFolderId != null) {
                        DropdownMenuItem(
                            text = { Text("新建错题") },
                            leadingIcon = { Icon(Icons.Filled.EditNote, null, Modifier.size(18.dp)) },
                            onClick = {
                                createMenu = false
                                editingEntryId = Store.addEntry(currentFolderId, "", "")
                            }
                        )
                    }
                }
            }
        }
    ) { inner ->
        BackHandler(enabled = currentFolderId != null) { path = validPath.dropLast(1) }

        if (folders.isEmpty() && entries.isEmpty()) {
            EmptyState(
                text = if (currentFolderId == null) "还没有科目文件夹" else "这个文件夹是空的",
                hint = if (currentFolderId == null) {
                    "点右下角 + 新建文件夹，例如 数学、408"
                } else {
                    "点右下角 + 可以新建子文件夹或这道错题"
                },
                modifier = Modifier.padding(inner)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(inner),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(folders, key = { "f_${it.id}" }) { f ->
                    FolderRow(
                        folder = f,
                        subFolders = data.childFolders(f.id).size,
                        files = data.entriesIn(f.id).size,
                        onOpen = { path = path + f.id },
                        onRename = { renaming = f },
                        onMove = { movingFolder = f },
                        onDelete = { deletingFolder = f }
                    )
                }
                if (folders.isNotEmpty() && entries.isNotEmpty()) {
                    item { ThinDivider(Modifier.padding(vertical = 2.dp)) }
                }
                items(entries, key = { "e_${it.id}" }) { e ->
                    EntryRow(
                        entry = e,
                        onOpen = { editingEntryId = e.id },
                        onMove = { movingEntry = e },
                        onDelete = { Store.deleteEntry(e.id) },
                        onPreview = { index -> previewing = e.images to index }
                    )
                }
            }
        }
    }

    if (newFolderFor != null) {
        InputDialog(
            title = "新建文件夹",
            placeholder = "例如：高数 · 极限",
            onConfirm = { name -> Store.addFolder(name, currentFolderId) },
            onDismiss = { newFolderFor = null }
        )
    }

    renaming?.let { f ->
        InputDialog(
            title = "重命名文件夹",
            initial = f.name,
            onConfirm = { name -> Store.renameFolder(f.id, name) },
            onDismiss = { renaming = null }
        )
    }

    movingFolder?.let { f ->
        FolderPickerDialog(
            data = data,
            title = "移动「${f.name}」到",
            disabledIds = data.descendantFolderIds(f.id),
            onPick = { target -> Store.moveFolder(f.id, target) },
            onDismiss = { movingFolder = null }
        )
    }

    movingEntry?.let { e ->
        FolderPickerDialog(
            data = data,
            title = "移动「${e.title.ifBlank { "未命名错题" }}」到",
            onPick = { target -> Store.moveEntry(e.id, target ?: return@FolderPickerDialog) },
            onDismiss = { movingEntry = null }
        )
    }

    deletingFolder?.let { f ->
        val count = data.descendantFolderIds(f.id).size - 1
        ConfirmDialog(
            title = "删除文件夹「${f.name}」？",
            message = if (count > 0) {
                "其中包含 $count 个子文件夹，所有内容都会被一起删除。"
            } else {
                "文件夹内的错题会一并删除。"
            },
            onConfirm = { Store.deleteFolder(f.id) },
            onDismiss = { deletingFolder = null }
        )
    }

    previewing?.let { (names, index) ->
        ImagePreviewDialog(names = names, startIndex = index, onDismiss = { previewing = null })
    }
}

@Composable
private fun BreadcrumbBar(
    data: AppData,
    path: List<String>,
    onJump: (Int) -> Unit,
    onRoot: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onRoot, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
            Text("全部", style = MaterialTheme.typography.labelMedium, color = InkSoft)
        }
        path.forEachIndexed { index, id ->
            val f = data.folder(id) ?: return@forEachIndexed
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = InkGhost,
                modifier = Modifier.size(14.dp)
            )
            val isLast = index == path.lastIndex
            TextButton(
                onClick = { onJump(index) },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
            ) {
                Text(
                    text = f.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isLast) Ink else InkSoft,
                    fontWeight = if (isLast) FontWeight.Medium else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun FolderRow(
    folder: MistakeFolder,
    subFolders: Int,
    files: Int,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Panel.copy(alpha = 0.6f))
            .border(1.dp, Hairline, RoundedCornerShape(14.dp))
            .clickable(onClick = onOpen)
            .padding(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Folder, contentDescription = null, tint = InkSoft, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(folder.name, style = MaterialTheme.typography.bodyLarge, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                buildString {
                    if (subFolders > 0) append("$subFolders 个子文件夹")
                    if (subFolders > 0 && files > 0) append(" · ")
                    if (files > 0) append("$files 道错题")
                    if (subFolders == 0 && files == 0) append("空文件夹")
                },
                style = MaterialTheme.typography.labelSmall,
                color = InkFaint
            )
        }
        Box {
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = "更多",
                tint = InkFaint,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { menu = true }
                    .padding(8.dp)
                    .size(18.dp)
            )
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("重命名") },
                    onClick = { menu = false; onRename() }
                )
                DropdownMenuItem(
                    text = { Text("移动到…") },
                    leadingIcon = { Icon(Icons.Filled.DriveFileMove, null, Modifier.size(18.dp)) },
                    onClick = { menu = false; onMove() }
                )
                DropdownMenuItem(
                    text = { Text("删除", color = AlertRed) },
                    leadingIcon = { Icon(Icons.Filled.DeleteOutline, null, Modifier.size(18.dp), tint = AlertRed) },
                    onClick = { menu = false; onDelete() }
                )
            }
        }
    }
}

@Composable
private fun EntryRow(
    entry: MistakeEntry,
    onOpen: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    onPreview: (Int) -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    SwipeToDeleteBox(
        onDelete = onDelete,
        onClick = onOpen,
        modifier = Modifier.clip(RoundedCornerShape(14.dp))
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Paper)
                .border(1.dp, Hairline, RoundedCornerShape(14.dp))
                .padding(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = entry.title.ifBlank { "未命名错题" },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = Ink,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            formatTimestamp(entry.updatedAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = InkFaint
                        )
                        if (entry.images.isNotEmpty()) {
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Filled.Image, null, tint = InkGhost, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(2.dp))
                            Text(
                                "${entry.images.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = InkFaint
                            )
                        }
                    }
                }
                Box {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = "更多",
                        tint = InkFaint,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { menu = true }
                            .padding(8.dp)
                            .size(18.dp)
                    )
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("移动到…") },
                            leadingIcon = { Icon(Icons.Filled.DriveFileMove, null, Modifier.size(18.dp)) },
                            onClick = { menu = false; onMove() }
                        )
                    }
                }
            }
            if (entry.description.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = entry.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSoft,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 10.dp)
                )
            }
            if (entry.images.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(end = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    entry.images.take(6).forEachIndexed { index, name ->
                        Box(
                            Modifier
                                .size(84.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, Hairline, RoundedCornerShape(10.dp))
                                .clickable { onPreview(index) }
                        ) {
                            StoredImage(name, Modifier.fillMaxSize(), maxPx = 360)
                        }
                    }
                    if (entry.images.size > 6) {
                        Box(
                            Modifier
                                .size(84.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Panel)
                                .clickable { onPreview(6) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+${entry.images.size - 6}", color = InkSoft)
                        }
                    }
                }
            }
        }
    }
}

/** 错题编辑页：文字描述 + 拍照 / 相册。 */
@Composable
private fun EntryEditor(
    entry: MistakeEntry,
    onDone: (String, String, List<String>) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit
) {
    var title by remember(entry.id) { mutableStateOf(entry.title) }
    var description by remember(entry.id) { mutableStateOf(entry.description) }
    var images by remember(entry.id) { mutableStateOf(entry.images) }
    var confirmDelete by remember { mutableStateOf(false) }
    var previewIndex by remember { mutableStateOf<Int?>(null) }

    val takePhoto = rememberCameraCapture { name ->
        if (name != null) images = images + name
    }
    val pickPhotos = rememberGalleryPicker { names ->
        if (names.isNotEmpty()) images = images + names
    }

    fun finish() = onDone(title, description, images)

    BackHandler { finish() }

    Scaffold(
        containerColor = Paper,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            PageTopBar(
                title = "错题详情",
                subtitle = formatTimestamp(entry.updatedAt),
                onBack = { finish() },
                actions = {
                    TextButton(onClick = { confirmDelete = true }) {
                        Text("删除", color = InkSoft, fontWeight = FontWeight.Medium)
                    }
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
                label = { Text("题目 / 考点") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            // 图片放在标题和正文之间，并放大缩略图，方便一眼确认拍了什么。
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionButton("拍照", Icons.Filled.PhotoCamera, Modifier.weight(1f), onClick = takePhoto)
                ActionButton("从相册选", Icons.Filled.PhotoLibrary, Modifier.weight(1f), onClick = pickPhotos)
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
                value = description,
                onValueChange = { description = it },
                label = { Text("描述、错因、正确解法") },
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
            Spacer(Modifier.height(14.dp))
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "删除这道错题？",
            message = "题目描述和照片都会被删除。",
            onConfirm = onDelete,
            onDismiss = { confirmDelete = false }
        )
    }

    previewIndex?.let { index ->
        ImagePreviewDialog(names = images, startIndex = index, onDismiss = { previewIndex = null })
    }
}

@Composable
private fun ActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
