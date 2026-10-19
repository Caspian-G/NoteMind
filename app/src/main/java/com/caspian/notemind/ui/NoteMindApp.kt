package com.caspian.notemind.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.caspian.notemind.ui.memo.MemoScreen
import com.caspian.notemind.ui.mind.MindMapScreen
import com.caspian.notemind.ui.mistake.MistakeScreen
import com.caspian.notemind.ui.theme.Ink
import com.caspian.notemind.ui.theme.InkFaint
import com.caspian.notemind.ui.theme.Paper

private enum class Tab(val label: String) {
    MEMO("备忘录"),
    MISTAKE("错题集"),
    MIND("思维导图")
}

/** 三个主功能模块的壳：底部只有三个入口，没有多余层级。 */
@Composable
fun NoteMindApp() {
    var tab by remember { mutableStateOf(Tab.MEMO) }

    Scaffold(
        containerColor = Paper,
        bottomBar = {
            NavigationBar(containerColor = Paper, tonalElevation = 0.dp) {
                Tab.entries.forEach { entry ->
                    NavigationBarItem(
                        selected = tab == entry,
                        onClick = { tab = entry },
                        icon = {
                            Icon(
                                imageVector = when (entry) {
                                    Tab.MEMO -> Icons.Filled.EditNote
                                    Tab.MISTAKE -> Icons.Filled.Book
                                    Tab.MIND -> Icons.Filled.AccountTree
                                },
                                contentDescription = entry.label
                            )
                        },
                        label = null,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Ink,
                            selectedTextColor = Ink,
                            unselectedIconColor = InkFaint,
                            unselectedTextColor = InkFaint,
                            indicatorColor = com.caspian.notemind.ui.theme.Panel
                        )
                    )
                }
            }
        }
    ) { inner ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            when (tab) {
                Tab.MEMO -> MemoScreen()
                Tab.MISTAKE -> MistakeScreen()
                Tab.MIND -> MindMapScreen()
            }
        }
    }
}
