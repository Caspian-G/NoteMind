package com.caspian.notemind

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.caspian.notemind.data.Store
import com.caspian.notemind.ui.common.ZoomableImage
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 全屏看图的双指缩放只能靠真手势验证，adb 的 input 命令不支持多点触控，
 * 所以这里用 Compose 的 touch injection 直接对 ZoomableImage 做捏合。
 */
@RunWith(AndroidJUnit4::class)
class ImageViewerZoomTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun pinchOut_zoomsInAndReportsZoomedState() {
        Store.init(InstrumentationRegistry.getInstrumentation().targetContext)
        var zoomed = false

        compose.setContent {
            ZoomableImage(
                name = "does-not-exist.jpg",
                onZoomChanged = { zoomed = it },
                modifier = Modifier.fillMaxSize().testTag("zoomable")
            )
        }

        compose.onNodeWithTag("zoomable").performTouchInput {
            pinch(
                start0 = Offset(centerX - 100f, centerY),
                end0 = Offset(centerX - 300f, centerY),
                start1 = Offset(centerX + 100f, centerY),
                end1 = Offset(centerX + 300f, centerY)
            )
        }

        compose.waitUntil(timeoutMillis = 5_000) { zoomed }
        assertTrue("双指张开后应当进入放大状态", zoomed)
    }
}
