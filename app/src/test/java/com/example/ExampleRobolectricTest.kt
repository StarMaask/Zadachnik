package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.PhotoReportItem
import com.example.ui.components.PhotoCaptureUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Ежедневник", appName)
  }

  @Test
  fun `camera bitmap survives local file deletion via inline base64 across devices`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val srcBitmap = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888).apply {
      val canvas = Canvas(this)
      canvas.drawColor(Color.rgb(40, 110, 180))
    }

    val combinedUri = PhotoCaptureUtils.saveCameraBitmapWithStamp(
      context = context,
      sourceBitmap = srcBitmap,
      taskTitle = "Защита шкафа МНС-4",
      authorName = "Плиткин Д.",
      gpsCoords = "УЭСАиТМ",
      comment = "Фотофиксация #1"
    )
    assertTrue("Expected #b64= inline thumbnail in combined URI", PhotoCaptureUtils.hasInlineBase64(combinedUri))

    val photoId = "ph_1700000100001_101"
    val withDocId = PhotoCaptureUtils.attachCloudDocId(combinedUri, photoId)
    val encoded = PhotoReportItem(
      id = photoId,
      uriString = withDocId,
      authorId = "worker_1",
      authorName = "Плиткин Д.",
      capturedAtText = "14.11 18:21",
      gpsCoords = "УЭСАиТМ",
      comment = "Фотофиксация #1"
    ).encode()

    // Simulate receiving on Supervisor device where original local file does not exist
    val localPath = PhotoCaptureUtils.extractLocalPath(withDocId)
    if (localPath.isNotBlank()) {
      File(localPath).delete()
    }

    val decodedItem = PhotoReportItem.decode(encoded)
    val loaded = PhotoCaptureUtils.loadBitmapFromUriOrPath(
      uriOrPath = decodedItem.uriString,
      fallbackComment = decodedItem.comment,
      fallbackAuthor = decodedItem.authorName,
      fallbackTime = decodedItem.capturedAtText,
      fallbackGps = decodedItem.gpsCoords,
      fallbackTaskTitle = "Защита шкафа МНС-4",
      context = context,
      photoId = decodedItem.id
    )
    assertNotNull("Supervisor phone must render bitmap from inline Base64 even without local file", loaded)
    assertTrue(loaded!!.width > 0 && loaded.height > 0)
  }

  @Test
  fun `cloud photo cache hydrates photo even when inline base64 is stripped`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val photoId = "ph_1700000200002_202"
    val fullBmp = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888).apply {
      Canvas(this).drawColor(Color.rgb(16, 185, 129))
    }
    val baos = ByteArrayOutputStream()
    fullBmp.compress(Bitmap.CompressFormat.JPEG, 85, baos)
    val cloudB64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)

    val cached = PhotoCaptureUtils.saveCloudPhotoBytesToLocalCache(context, photoId, cloudB64)
    assertNotNull(cached)
    assertTrue(PhotoCaptureUtils.hasCachedCloudPhoto(context, photoId))

    val remoteUriWithoutB64 = "/data/user/0/other.device/files/task_photos/cam_missing.jpg#pdoc=$photoId"
    val loadedFromCache = PhotoCaptureUtils.loadBitmapFromUriOrPath(
      uriOrPath = remoteUriWithoutB64,
      fallbackComment = "Фотофиксация #2",
      context = context,
      photoId = photoId
    )
    assertNotNull("Must load photo from cloud cache even when inline Base64 is omitted", loadedFromCache)
  }
}
