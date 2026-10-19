package com.caspian.notemind.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.caspian.notemind.data.Store
import java.io.File
import java.util.concurrent.ConcurrentHashMap

private val memCache = ConcurrentHashMap<String, ImageBitmap>()

private fun cacheKey(name: String, maxPx: Int) = "$name@$maxPx"

/** 只保存文件名，实际文件位于 filesDir/images。 */
fun storedImageFile(name: String): File = File(Store.imageDir, name)

private fun decodeScaled(file: File, maxPx: Int): Bitmap? {
    if (!file.exists()) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= maxPx || bounds.outHeight / (sample * 2) >= maxPx) {
        sample *= 2
    }
    val opts = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.RGB_565
    }
    return runCatching { BitmapFactory.decodeFile(file.absolutePath, opts) }.getOrNull()
}

/** 读取照片并按需缩放，避免大图直接进内存。加了内存缓存，列表滚动不会反复解码。 */
fun loadStoredImage(name: String, maxPx: Int = 1080): ImageBitmap? {
    val key = cacheKey(name, maxPx)
    memCache[key]?.let { return it }
    val bitmap = decodeScaled(storedImageFile(name), maxPx) ?: return null
    val image = bitmap.asImageBitmap()
    memCache[key] = image
    return image
}

/** 保存图片后清掉旧缓存，否则同一文件名（极少见）会读到旧图。 */
fun invalidateImage(name: String) {
    memCache.keys.filter { it.startsWith("$name@") }.forEach { memCache.remove(it) }
}

/** 把相册/文件选择器返回的 Uri 复制进应用私有目录，返回新的文件名。 */
fun copyUriToStore(context: Context, uri: Uri): String? = runCatching {
    val target = Store.newImageFile()
    context.contentResolver.openInputStream(uri)?.use { input ->
        target.outputStream().use { output -> input.copyTo(output) }
    } ?: return null
    if (target.length() <= 0L) {
        target.delete()
        return null
    }
    // 相册图片可能带有 EXIF 旋转信息，这里按 EXIF 方向做一次物理矫正，
    // 否则 Compose 直接解码会显示成横躺的图片。
    runCatching {
        val orientation = context.contentResolver
            .openInputStream(uri)
            ?.use { androidx.exifinterface.media.ExifInterface(it) }
            ?.getAttributeInt(
                androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
            ) ?: androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
        if (orientation != androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL) {
            val bitmap = BitmapFactory.decodeFile(target.absolutePath) ?: return@runCatching
            val matrix = Matrix()
            when (orientation) {
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                androidx.exifinterface.media.ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                androidx.exifinterface.media.ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            target.outputStream().use { rotated.compress(Bitmap.CompressFormat.JPEG, 92, it) }
            if (rotated !== bitmap) bitmap.recycle()
            rotated.recycle()
        }
    }
    target.name
}.getOrNull()

/**
 * 拍照。返回一个无参函数，调用即拉起系统相机；
 * 拍完把文件名回调出去，取消或失败回调 null。
 */
@Composable
fun rememberCameraCapture(onResult: (String?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val current by rememberUpdatedState(onResult)
    var pending by remember { mutableStateOf<File?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val file = pending
        pending = null
        if (ok && file != null && file.exists() && file.length() > 0) {
            current(file.name)
        } else {
            file?.delete()
            current(null)
        }
    }

    return remember(context, launcher) {
        {
            val file = Store.newImageFile()
            pending = file
            runCatching {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                launcher.launch(uri)
            }.onFailure {
                pending = null
                file.delete()
                current(null)
            }
        }
    }
}

/** 从相册多选，复制进应用目录后回调文件名列表。 */
@Composable
fun rememberGalleryPicker(onResult: (List<String>) -> Unit): () -> Unit {
    val context = LocalContext.current
    val current by rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(12)
    ) { uris ->
        if (uris.isEmpty()) {
            current(emptyList())
            return@rememberLauncherForActivityResult
        }
        val names = uris.mapNotNull { copyUriToStore(context, it) }
        current(names)
    }
    return remember(context, launcher) {
        {
            runCatching {
                launcher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }.onFailure { current(emptyList()) }
        }
    }
}
