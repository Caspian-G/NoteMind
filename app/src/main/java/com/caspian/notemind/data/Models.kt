package com.caspian.notemind.data

import java.util.UUID

fun newId(): String = UUID.randomUUID().toString().replace("-", "").take(16)

/** 备忘录：扁平的一条长文本，没有标题、没有勾选、没有层级。 */
data class Memo(
    val id: String,
    val text: String,
    val createdAt: Long,
    val updatedAt: Long
)

/** 错题集文件夹：靠 parentId 组成多级结构，parentId == null 表示最外层。 */
data class MistakeFolder(
    val id: String,
    val name: String,
    val parentId: String?,
    val createdAt: Long
)

/** 错题文件：文字描述 + 若干张照片，照片存为应用私有目录下的文件名。 */
data class MistakeEntry(
    val id: String,
    val folderId: String,
    val title: String,
    val description: String,
    val images: List<String>,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * 思维导图节点。
 * x/y/w/h 使用画布世界坐标（dp），与缩放无关，保证缩放时布局稳定。
 */
data class MindNode(
    val id: String,
    val parentId: String?,
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
    val title: String,
    val content: String,
    val images: List<String>,
    /** 是否已原地展开为详细内容面板 */
    val expanded: Boolean,
    /** 展开前的尺寸，收起时还原 */
    val restW: Float,
    val restH: Float,
    val createdAt: Long
)

data class Viewport(
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
)

data class AppData(
    val memos: List<Memo> = emptyList(),
    val folders: List<MistakeFolder> = emptyList(),
    val entries: List<MistakeEntry> = emptyList(),
    val nodes: List<MindNode> = emptyList(),
    val viewport: Viewport = Viewport(),
    val seeded: Boolean = false
) {
    fun childFolders(parentId: String?): List<MistakeFolder> =
        folders.filter { it.parentId == parentId }.sortedBy { it.createdAt }

    fun entriesIn(folderId: String): List<MistakeEntry> =
        entries.filter { it.folderId == folderId }.sortedByDescending { it.updatedAt }

    fun folder(id: String?): MistakeFolder? = id?.let { fid -> folders.firstOrNull { it.id == fid } }

    /** [id] 自身及其所有后代文件夹的 id，移动时用于防止把文件夹拖进自己内部。 */
    fun descendantFolderIds(id: String): Set<String> {
        val result = mutableSetOf(id)
        var frontier = listOf(id)
        var guard = 0
        while (frontier.isNotEmpty() && guard++ < 64) {
            val next = folders.filter { it.parentId in frontier }.map { it.id }
            result += next
            frontier = next
        }
        return result
    }

    fun node(id: String?): MindNode? = id?.let { nid -> nodes.firstOrNull { it.id == nid } }

    /** [id] 的所有后代节点 id，**不含**自身。用于成环判断与整树排版。 */
    fun descendantNodeIds(id: String): Set<String> {
        val result = mutableSetOf<String>()
        var frontier = listOf(id)
        var guard = 0
        while (frontier.isNotEmpty() && guard++ < 64) {
            val next = nodes.filter { it.parentId in frontier }.map { it.id }
            result += next
            frontier = next
        }
        return result
    }

    /** 沿 parentId 一路向上，找到 [id] 所在索引树的根（也就是主索引）。 */
    fun nodeTreeRootId(id: String): String {
        var current = node(id) ?: return id
        var guard = 0
        while (guard++ < 64) {
            val parent = current.parentId?.let { node(it) } ?: break
            current = parent
        }
        return current.id
    }

    /** 每棵索引树的根：没有父节点，或父节点已经不存在了。 */
    fun treeRootIds(): List<String> {
        val ids = nodes.map { it.id }.toSet()
        return nodes.filter { it.parentId == null || it.parentId !in ids }.map { it.id }
    }
}
