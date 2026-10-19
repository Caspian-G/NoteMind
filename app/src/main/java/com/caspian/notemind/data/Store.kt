package com.caspian.notemind.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * 全局数据仓库。整个应用的数据量很小（个人笔记），
 * 因此采用单文件 JSON 持久化 + 内存 StateFlow，避免引入数据库带来的构建与迁移成本。
 */
object Store {

    private lateinit var dataFile: File

    /** 照片统一存放在这里，只保存文件名，便于整目录备份。 */
    lateinit var imageDir: File
        private set

    private val _state = MutableStateFlow(AppData())
    val state: StateFlow<AppData> = _state.asStateFlow()

    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val saveSignal = Channel<Unit>(Channel.CONFLATED)

    fun init(context: Context) {
        val dir = context.applicationContext.filesDir
        dataFile = File(dir, "notemind.json")
        imageDir = File(dir, "images").apply { mkdirs() }
        _state.value = runCatching { read(dataFile) }.getOrElse { AppData() }
        if (!_state.value.seeded) seedDefaults()
        io.launch { saveLoop() }
    }

    /** 首次启动时预置用户提到的四门科目文件夹。 */
    private fun seedDefaults() {
        val now = System.currentTimeMillis()
        val names = listOf("数学", "408", "英语", "政治")
        val folders = names.mapIndexed { index, name ->
            MistakeFolder(newId(), name, null, now + index)
        }
        _state.value = _state.value.copy(folders = _state.value.folders + folders, seeded = true)
        saveSignal.trySend(Unit)
    }

    private suspend fun saveLoop() {
        for (ignored in saveSignal) {
            delay(150)
            while (saveSignal.tryReceive().isSuccess) { /* 合并高频写入 */ }
            val snapshot = _state.value
            runCatching {
                val tmp = File(dataFile.parentFile, "notemind.json.tmp")
                tmp.writeText(encode(snapshot).toString())
                if (dataFile.exists()) dataFile.delete()
                tmp.renameTo(dataFile)
            }
        }
    }

    private fun update(block: (AppData) -> AppData) {
        _state.value = block(_state.value)
        saveSignal.trySend(Unit)
    }

    // ---------------------------------------------------------------- 备忘录

    fun addMemo(text: String = ""): String {
        val id = newId()
        val now = System.currentTimeMillis()
        update { it.copy(memos = listOf(Memo(id, text, now, now)) + it.memos) }
        return id
    }

    fun updateMemo(id: String, text: String) = update { data ->
        data.copy(memos = data.memos.map {
            if (it.id == id) it.copy(text = text, updatedAt = System.currentTimeMillis()) else it
        })
    }

    fun deleteMemo(id: String) = update { data ->
        data.copy(memos = data.memos.filterNot { it.id == id })
    }

    // ---------------------------------------------------------------- 错题集

    fun addFolder(name: String, parentId: String?): String {
        val id = newId()
        update { it.copy(folders = it.folders + MistakeFolder(id, name, parentId, System.currentTimeMillis())) }
        return id
    }

    fun renameFolder(id: String, name: String) = update { data ->
        data.copy(folders = data.folders.map { if (it.id == id) it.copy(name = name) else it })
    }

    /** 删除文件夹会连同其所有子文件夹和内部错题一起删除。 */
    fun deleteFolder(id: String) = update { data ->
        val doomed = data.descendantFolderIds(id)
        val images = data.entries.filter { it.folderId in doomed }.flatMap { it.images }
        deleteImageFiles(images)
        data.copy(
            folders = data.folders.filterNot { it.id in doomed },
            entries = data.entries.filterNot { it.folderId in doomed }
        )
    }

    /** 移动文件夹到新的父级；newParentId == null 表示移动到最外层。 */
    fun moveFolder(id: String, newParentId: String?): Boolean {
        val data = _state.value
        val folder = data.folder(id) ?: return false
        if (newParentId != null && newParentId in data.descendantFolderIds(id)) return false
        if (folder.parentId == newParentId) return false
        update { d -> d.copy(folders = d.folders.map { if (it.id == id) it.copy(parentId = newParentId) else it }) }
        return true
    }

    fun addEntry(folderId: String, title: String, description: String): String {
        val id = newId()
        val now = System.currentTimeMillis()
        update {
            it.copy(
                entries = it.entries + MistakeEntry(id, folderId, title, description, emptyList(), now, now)
            )
        }
        return id
    }

    fun updateEntry(id: String, title: String, description: String, images: List<String>) = update { data ->
        data.copy(entries = data.entries.map {
            if (it.id == id) it.copy(
                title = title,
                description = description,
                images = images,
                updatedAt = System.currentTimeMillis()
            ) else it
        })
    }

    fun deleteEntry(id: String) = update { data ->
        val entry = data.entries.firstOrNull { it.id == id }
        entry?.let { deleteImageFiles(it.images) }
        data.copy(entries = data.entries.filterNot { it.id == id })
    }

    fun moveEntry(id: String, folderId: String) = update { data ->
        data.copy(entries = data.entries.map {
            if (it.id == id) it.copy(folderId = folderId, updatedAt = System.currentTimeMillis()) else it
        })
    }

    // ---------------------------------------------------------------- 思维导图

    fun addMindNode(
        x: Float,
        y: Float,
        title: String = "笔记主索引",
        parentId: String? = null,
        w: Float = 200f,
        h: Float = 200f
    ): String {
        val id = newId()
        update {
            it.copy(
                nodes = it.nodes + MindNode(
                    id = id,
                    parentId = parentId,
                    x = x,
                    y = y,
                    w = w,
                    h = h,
                    title = title,
                    content = "",
                    images = emptyList(),
                    expanded = false,
                    restW = w,
                    restH = h,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
        return id
    }

    /** 拖动结束后落盘，拖动过程中只改内存状态。 */
    fun setNodeFrame(id: String, x: Float, y: Float, w: Float, h: Float) = update { data ->
        data.copy(nodes = data.nodes.map {
            if (it.id == id) it.copy(x = x, y = y, w = w, h = h) else it
        })
    }

    fun setNodeExpanded(id: String, expanded: Boolean) = update { data ->
        data.copy(nodes = data.nodes.map { node ->
            if (node.id != id) return@map node
            if (expanded) {
                node.copy(expanded = true, restW = node.w, restH = node.h)
            } else {
                node.copy(expanded = false, w = node.restW, h = node.restH)
            }
        })
    }

    fun updateNodeContent(id: String, title: String, content: String, images: List<String>) = update { data ->
        data.copy(nodes = data.nodes.map {
            if (it.id == id) it.copy(title = title, content = content, images = images) else it
        })
    }

    /**
     * 重新指定父索引。[newParentId] 为 null 表示断开连接，自己变成一棵独立的主索引。
     * 不允许认自己或自己的后代当父索引（会成环），被拒绝时返回 false。
     */
    fun setNodeParent(id: String, newParentId: String?): Boolean {
        val data = _state.value
        val node = data.node(id) ?: return false
        if (node.parentId == newParentId) return false
        if (newParentId != null && (newParentId == id || newParentId in data.descendantNodeIds(id))) {
            return false
        }
        update { d ->
            d.copy(nodes = d.nodes.map { if (it.id == id) it.copy(parentId = newParentId) else it })
        }
        return true
    }

    /** 批量改坐标，供自动排版使用。 */
    fun setNodePositions(positions: Map<String, Pair<Float, Float>>) = update { data ->
        data.copy(nodes = data.nodes.map { n ->
            val p = positions[n.id] ?: return@map n
            n.copy(x = p.first, y = p.second)
        })
    }

    /** 删除节点时一并删除其所有后代节点。 */
    fun deleteMindNode(id: String) = update { data ->
        val doomed = data.descendantNodeIds(id) + id
        deleteImageFiles(data.nodes.filter { it.id in doomed }.flatMap { it.images })
        data.copy(nodes = data.nodes.filterNot { it.id in doomed })
    }

    fun setViewport(viewport: Viewport) = update { it.copy(viewport = viewport) }

    // ---------------------------------------------------------------- 图片

    fun newImageFile(): File =
        File(imageDir, "IMG_${System.currentTimeMillis()}_${newId().take(6)}.jpg")

    fun deleteImageFiles(names: List<String>) {
        names.forEach { name -> runCatching { File(imageDir, name).takeIf { it.exists() }?.delete() } }
    }

    // ---------------------------------------------------------------- JSON

    private fun encode(data: AppData): JSONObject = JSONObject().apply {
        put("version", 1)
        put("seeded", data.seeded)
        put("memos", JSONArray().apply {
            data.memos.forEach { m ->
                put(JSONObject().apply {
                    put("id", m.id)
                    put("text", m.text)
                    put("createdAt", m.createdAt)
                    put("updatedAt", m.updatedAt)
                })
            }
        })
        put("folders", JSONArray().apply {
            data.folders.forEach { f ->
                put(JSONObject().apply {
                    put("id", f.id)
                    put("name", f.name)
                    put("parentId", f.parentId ?: JSONObject.NULL)
                    put("createdAt", f.createdAt)
                })
            }
        })
        put("entries", JSONArray().apply {
            data.entries.forEach { e ->
                put(JSONObject().apply {
                    put("id", e.id)
                    put("folderId", e.folderId)
                    put("title", e.title)
                    put("description", e.description)
                    put("images", JSONArray(e.images))
                    put("createdAt", e.createdAt)
                    put("updatedAt", e.updatedAt)
                })
            }
        })
        put("nodes", JSONArray().apply {
            data.nodes.forEach { n ->
                put(JSONObject().apply {
                    put("id", n.id)
                    put("parentId", n.parentId ?: JSONObject.NULL)
                    put("x", n.x.toDouble())
                    put("y", n.y.toDouble())
                    put("w", n.w.toDouble())
                    put("h", n.h.toDouble())
                    put("title", n.title)
                    put("content", n.content)
                    put("images", JSONArray(n.images))
                    put("expanded", n.expanded)
                    put("restW", n.restW.toDouble())
                    put("restH", n.restH.toDouble())
                    put("createdAt", n.createdAt)
                })
            }
        })
        put("viewport", JSONObject().apply {
            put("scale", data.viewport.scale.toDouble())
            put("offsetX", data.viewport.offsetX.toDouble())
            put("offsetY", data.viewport.offsetY.toDouble())
        })
    }

    private fun read(file: File): AppData {
        if (!file.exists()) return AppData()
        val root = JSONObject(file.readText())
        val memos = root.optJSONArray("memos").objects { o ->
            Memo(
                id = o.getString("id"),
                text = o.optString("text"),
                createdAt = o.optLong("createdAt"),
                updatedAt = o.optLong("updatedAt")
            )
        }
        val folders = root.optJSONArray("folders").objects { o ->
            MistakeFolder(
                id = o.getString("id"),
                name = o.optString("name"),
                parentId = o.stringOrNull("parentId"),
                createdAt = o.optLong("createdAt")
            )
        }
        val entries = root.optJSONArray("entries").objects { o ->
            MistakeEntry(
                id = o.getString("id"),
                folderId = o.optString("folderId"),
                title = o.optString("title"),
                description = o.optString("description"),
                images = o.optJSONArray("images").strings(),
                createdAt = o.optLong("createdAt"),
                updatedAt = o.optLong("updatedAt")
            )
        }
        val nodes = root.optJSONArray("nodes").objects { o ->
            val w = o.optDouble("w", 200.0).toFloat()
            val h = o.optDouble("h", 200.0).toFloat()
            MindNode(
                id = o.getString("id"),
                parentId = o.stringOrNull("parentId"),
                x = o.optDouble("x", 0.0).toFloat(),
                y = o.optDouble("y", 0.0).toFloat(),
                w = w,
                h = h,
                title = o.optString("title"),
                content = o.optString("content"),
                images = o.optJSONArray("images").strings(),
                expanded = o.optBoolean("expanded", false),
                restW = o.optDouble("restW", w.toDouble()).toFloat(),
                restH = o.optDouble("restH", h.toDouble()).toFloat(),
                createdAt = o.optLong("createdAt")
            )
        }
        val vp = root.optJSONObject("viewport")
        val viewport = Viewport(
            scale = vp?.optDouble("scale", 1.0)?.toFloat()?.coerceIn(0.25f, 3f) ?: 1f,
            offsetX = vp?.optDouble("offsetX", 0.0)?.toFloat() ?: 0f,
            offsetY = vp?.optDouble("offsetY", 0.0)?.toFloat() ?: 0f
        )
        return AppData(memos, folders, entries, nodes, viewport, root.optBoolean("seeded", false))
    }

    private fun JSONObject.stringOrNull(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotEmpty() }

    private fun JSONArray?.strings(): List<String> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { optString(it).takeIf { s -> s.isNotEmpty() } }
    }

    private fun <T> JSONArray?.objects(map: (JSONObject) -> T): List<T> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { i -> optJSONObject(i)?.let(map) }
    }
}
