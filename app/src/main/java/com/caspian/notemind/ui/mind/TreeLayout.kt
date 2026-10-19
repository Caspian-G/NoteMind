package com.caspian.notemind.ui.mind

import com.caspian.notemind.data.MindNode
import kotlin.math.max

/** 父子之间的水平间距（dp 世界坐标）。 */
private const val H_GAP = 72f

/** 相邻子树之间的垂直间距。 */
private const val V_GAP = 28f

/**
 * 以 [rootId] 为根，把这棵索引树排成从左到右的层叠树。
 *
 * 规则：
 * - **根节点（主索引）保持原来的 x / y 不动**，其余节点围绕它生长；
 * - 子节点一律排在父节点右侧，一个父节点的所有子节点整体相对父节点垂直居中；
 * - 每棵子树独占一条垂直带，宽度取整棵子树所需的实际高度，所以不会互相重叠；
 * - 不属于这棵树的节点原样不动（返回结果里不会出现它们）。
 *
 * 返回 id -> (x, y) 的新坐标。
 */
fun layoutTree(nodes: List<MindNode>, rootId: String): Map<String, Pair<Float, Float>> {
    val byId = nodes.associateBy { it.id }
    val root = byId[rootId] ?: return emptyMap()
    val childrenOf = nodes.filter { it.parentId != null }.groupBy { it.parentId }

    // 先把自己这棵子树圈出来，避免把别人家的节点也一起搬走。
    val inTree = LinkedHashSet<String>()
    fun collect(id: String) {
        if (!inTree.add(id)) return
        childrenOf[id]?.sortedBy { it.createdAt }?.forEach { collect(it.id) }
    }
    collect(rootId)

    fun kidsOf(id: String): List<MindNode> =
        childrenOf[id].orEmpty().filter { it.id in inTree }.sortedBy { it.createdAt }

    // 第一遍：自底向上算出每棵子树需要的垂直高度。
    val subtreeHeight = HashMap<String, Float>()
    fun measure(id: String): Float {
        val node = byId[id] ?: return 0f
        val kids = kidsOf(id)
        val height = if (kids.isEmpty()) {
            node.h
        } else {
            val childrenTotal = kids.sumOf { measure(it.id).toDouble() }.toFloat() + V_GAP * (kids.size - 1)
            max(node.h, childrenTotal)
        }
        subtreeHeight[id] = height
        return height
    }
    measure(rootId)

    // 第二遍：自顶向下定位，父节点垂直居中于自己的子节点块。
    val result = HashMap<String, Pair<Float, Float>>(inTree.size)
    fun place(id: String, x: Float, centerY: Float) {
        val node = byId[id] ?: return
        result[id] = x to (centerY - node.h / 2f)

        val kids = kidsOf(id)
        if (kids.isEmpty()) return
        val childrenTotal =
            kids.sumOf { subtreeHeight[it.id]?.toDouble() ?: it.h.toDouble() }.toFloat() +
                V_GAP * (kids.size - 1)
        var cursor = centerY - childrenTotal / 2f
        kids.forEach { child ->
            val childHeight = subtreeHeight[child.id] ?: child.h
            place(child.id, x + node.w + H_GAP, cursor + childHeight / 2f)
            cursor += childHeight + V_GAP
        }
    }
    place(rootId, root.x, root.y + root.h / 2f)

    return result
}

/**
 * 整理多棵树：每棵树的根都留在原位，各自向右生长。
 * [rootIds] 为空时返回空表。
 */
fun layoutTrees(nodes: List<MindNode>, rootIds: List<String>): Map<String, Pair<Float, Float>> {
    val merged = HashMap<String, Pair<Float, Float>>()
    rootIds.forEach { merged.putAll(layoutTree(nodes, it)) }
    return merged
}
