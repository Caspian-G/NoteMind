package com.caspian.notemind.ui.memo

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.caspian.notemind.data.Memo
import com.caspian.notemind.data.Store
import com.caspian.notemind.ui.common.ConfirmDialog
import com.caspian.notemind.ui.common.EmptyState
import com.caspian.notemind.ui.common.PageTopBar
import com.caspian.notemind.ui.common.SwipeToDeleteBox
import com.caspian.notemind.ui.theme.Hairline
import com.caspian.notemind.ui.theme.Ink
import com.caspian.notemind.ui.theme.InkGhost
import com.caspian.notemind.ui.theme.InkSoft
import com.caspian.notemind.ui.theme.Paper

/**
 * 模块一：备忘录。
 * 每条清单就是一段纯文本，列表里整段直接铺开展示，不做折叠、不做标题、
 * 不做勾选，长内容也只是把卡片撑高，滚动即可读完全文。
 */
@Composable
fun MemoScreen() {
    val data by Store.state.collectAsStateWithLifecycle()
    var editingId by remember { mutableStateOf<String?>(null) }

    val editing = data.memos.firstOrNull { it.id == editingId }

    if (editing != null) {
        MemoEditor(
            memo = editing,
            onSave = { text ->
                Store.updateMemo(editing.id, text)
                editingId = null
            },
            onDelete = {
                Store.deleteMemo(editing.id)
                editingId = null
            },
            onBack = { editingId = null }
        )
        return
    }

    Scaffold(
        containerColor = Paper,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            PageTopBar(title = "备忘录")
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { editingId = Store.addMemo() },
                containerColor = Ink,
                contentColor = Paper,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "新建清单")
            }
        }
    ) { inner ->
        if (data.memos.isEmpty()) {
            EmptyState(
                text = "还没有清单",
                hint = "点右下角 + 新建一条，想写多长都可以",
                modifier = Modifier.padding(inner)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(inner),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(data.memos, key = { it.id }) { memo ->
                    MemoCard(
                        memo = memo,
                        onOpen = { editingId = memo.id },
                        onDelete = { Store.deleteMemo(memo.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MemoCard(memo: Memo, onOpen: () -> Unit, onDelete: () -> Unit) {
    SwipeToDeleteBox(
        onDelete = onDelete,
        onClick = onOpen,
        modifier = Modifier.clip(RoundedCornerShape(14.dp))
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(Paper)
                .border(1.dp, Hairline, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            if (memo.text.isBlank()) {
                Text("（空清单）", style = MaterialTheme.typography.bodyLarge, color = InkGhost)
            } else {
                Text(
                    text = memo.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Ink
                )
            }
        }
    }
}

/** 编辑页：整页都是输入区，没有多余控件。 */
@Composable
private fun MemoEditor(
    memo: Memo,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit
) {
    var text by remember(memo.id) { mutableStateOf(memo.text) }
    var confirmDelete by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    BackHandler {
        onSave(text)
    }

    LaunchedEffect(memo.id) {
        if (memo.text.isEmpty()) runCatching { focusRequester.requestFocus() }
    }

    Scaffold(
        containerColor = Paper,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            PageTopBar(
                title = "编辑清单",
                onBack = { onSave(text) },
                actions = {
                    TextButton(onClick = { confirmDelete = true }) {
                        Text("删除", color = InkSoft, fontWeight = FontWeight.Medium)
                    }
                    TextButton(onClick = { onSave(text) }) {
                        Text("完成", color = Ink, fontWeight = FontWeight.Medium)
                    }
                }
            )
        }
    ) { inner ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .imePadding()
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxSize().focusRequester(focusRequester),
                textStyle = LocalTextStyle.current.merge(
                    MaterialTheme.typography.bodyLarge.copy(color = Ink)
                ),
                cursorBrush = SolidColor(Ink),
                decorationBox = { field ->
                    if (text.isEmpty()) {
                        Text(
                            "随手记点什么…",
                            style = MaterialTheme.typography.bodyLarge,
                            color = InkGhost
                        )
                    }
                    field()
                }
            )
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "删除这条清单？",
            message = "删除后无法恢复。",
            onConfirm = onDelete,
            onDismiss = { confirmDelete = false }
        )
    }
}
