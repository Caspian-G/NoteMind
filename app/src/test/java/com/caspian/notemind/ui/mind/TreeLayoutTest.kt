package com.caspian.notemind.ui.mind

import com.caspian.notemind.data.MindNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 自动排版是纯函数，用 JVM 单测覆盖，不必起模拟器。 */
class TreeLayoutTest {

    private fun node(
        id: String,
        parentId: String? = null,
        x: Float = 0f,
        y: Float = 0f,
        w: Float = 190f,
        h: Float = 190f,
        createdAt: Long = 0L
    ) = MindNode(
        id = id,
        parentId = parentId,
        x = x,
        y = y,
        w = w,
        h = h,
        title = id,
        content = "",
        images = emptyList(),
        expanded = false,
        restW = w,
        restH = h,
        createdAt = createdAt
    )

    @Test
    fun root_keepsItsOriginalPosition() {
        val nodes = listOf(node("root", y = 500f), node("a", "root"), node("b", "root"))
        val layout = layoutTree(nodes, "root")
        assertEquals(0f, layout.getValue("root").first, 0.01f)
        assertEquals(500f, layout.getValue("root").second, 0.01f)
    }

    @Test
    fun children_arePlacedToTheRightAndNeverOverlap() {
        val nodes = listOf(
            node("root", createdAt = 0),
            node("a", "root", createdAt = 1),
            node("b", "root", createdAt = 2),
            node("c", "root", createdAt = 3)
        )
        val layout = layoutTree(nodes, "root")
        val rootX = layout.getValue("root").first

        listOf("a", "b", "c").forEach { id ->
            assertTrue("$id 应排在父节点右侧", layout.getValue(id).first >= rootX + 190f)
        }

        // 兄弟子树各占一条垂直带，不能叠在一起。
        val tops = listOf("a", "b", "c").map { layout.getValue(it).second }.sorted()
        for (i in 1 until tops.size) {
            assertTrue("相邻子树重叠了", tops[i] - tops[i - 1] >= 190f)
        }
    }

    @Test
    fun grandchildren_sitOneColumnFurtherRight() {
        val nodes = listOf(
            node("root", createdAt = 0),
            node("child", "root", createdAt = 1),
            node("grand", "child", createdAt = 2)
        )
        val layout = layoutTree(nodes, "root")
        assertTrue(layout.getValue("child").first > layout.getValue("root").first)
        assertTrue(layout.getValue("grand").first > layout.getValue("child").first)
    }

    @Test
    fun nodesFromAnotherTree_areLeftAlone() {
        val nodes = listOf(
            node("root", createdAt = 0),
            node("a", "root", createdAt = 1),
            node("other", x = 900f, y = 900f, createdAt = 2)
        )
        val layout = layoutTree(nodes, "root")
        assertFalse("别的索引树不该被排版动到", layout.containsKey("other"))
    }

    @Test
    fun layoutTrees_coversEveryTreeIndependently() {
        val nodes = listOf(
            node("r1", x = 0f, y = 0f, createdAt = 0),
            node("r1a", "r1", createdAt = 1),
            node("r2", x = 800f, y = 400f, createdAt = 2),
            node("r2a", "r2", createdAt = 3)
        )
        val layout = layoutTrees(nodes, listOf("r1", "r2"))
        assertEquals(0f, layout.getValue("r1").first, 0.01f)
        assertEquals(800f, layout.getValue("r2").first, 0.01f)
        assertEquals(400f, layout.getValue("r2").second, 0.01f)
        assertTrue(layout.getValue("r1a").first > 0f)
        assertTrue(layout.getValue("r2a").first > 800f)
    }
}
