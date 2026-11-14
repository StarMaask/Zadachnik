package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.PhotoReportItem
import com.example.ui.components.PhotoCaptureUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.RealObject
import org.robolectric.shadow.api.Shadow
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.util.WeakHashMap

@Implements(PdfDocument.Page::class)
class ShadowFakePdfPage {
  @RealObject
  private lateinit var realPage: PdfDocument.Page

  @Implementation
  protected fun getCanvas(): Canvas = pageCanvases[realPage]
    ?: error("No canvas registered for shadowed PdfDocument.Page")

  @Implementation
  protected fun getInfo(): PdfDocument.PageInfo = pageInfos[realPage]
    ?: error("No PageInfo registered for shadowed PdfDocument.Page")

  companion object {
    val pageCanvases = WeakHashMap<PdfDocument.Page, Canvas>()
    val pageInfos = WeakHashMap<PdfDocument.Page, PdfDocument.PageInfo>()
  }
}

@Implements(PdfDocument::class)
class ShadowFakePdfDocument {
  @Implementation
  protected fun __constructor__() {
    // Skip nativeCreateDocument() which is unavailable on host JVM
  }

  @Implementation
  protected fun startPage(pageInfo: PdfDocument.PageInfo): PdfDocument.Page {
    val page = Shadow.newInstanceOf(PdfDocument.Page::class.java)
    val bmp = Bitmap.createBitmap(
      pageInfo.pageWidth.coerceAtLeast(1),
      pageInfo.pageHeight.coerceAtLeast(1),
      Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bmp)
    ShadowFakePdfPage.pageCanvases[page] = canvas
    ShadowFakePdfPage.pageInfos[page] = pageInfo
    startedPageNumbers += pageInfo.pageNumber
    return page
  }

  @Implementation
  protected fun finishPage(page: PdfDocument.Page) {
    finishedPageCount++
  }

  @Implementation
  protected fun writeTo(out: OutputStream) {
    val payload = buildString {
      append("%PDF-1.4\n")
      append("% ShadowRobolectricPdf pages=$finishedPageCount\n")
      append("1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n")
      append("2 0 obj << /Type /Pages /Count $finishedPageCount >> endobj\n")
      append("% Padding for test size assertion: ")
      append("0123456789".repeat(120))
      append("\n%%EOF\n")
    }.toByteArray(Charsets.UTF_8)
    out.write(payload)
    out.flush()
  }

  @Implementation
  protected fun close() {
    isClosed = true
  }

  companion object {
    val startedPageNumbers = mutableListOf<Int>()
    var finishedPageCount: Int = 0
    var isClosed: Boolean = false

    fun reset() {
      startedPageNumbers.clear()
      finishedPageCount = 0
      isClosed = false
    }
  }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], shadows = [ShadowFakePdfDocument::class, ShadowFakePdfPage::class])
class ExampleRobolectricTest {

  @Before
  fun resetPdfShadowState() {
    ShadowFakePdfDocument.reset()
  }

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

  @Test
  fun `pdf report paginates all attached photos beyond six without truncation and preserves org and signature format`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val today = com.example.data.seed.CorporateSeedData.BASE_EPOCH_DAY
    val actor = com.example.data.model.CorporateEmployee(
      id = "emp_itr_pdf",
      fullName = "Плиткин Дмитрий Александрович",
      shortName = "Плиткин Д.А.",
      position = "Инженер по КИПиА",
      role = com.example.data.model.CorporateRole.ITR,
      groupId = "grp_1",
      groupName = "ЛПДС \"Быково\"",
      siteId = "site_1",
      siteName = "УЭСАиТМ",
      supervisorId = null,
      phone = "",
      email = "itr@site.ru"
    )

    val photoItems = (1..8).map { idx ->
      val isPortrait = idx % 2 == 1
      val bmp = Bitmap.createBitmap(
        if (isPortrait) 300 else 640,
        if (isPortrait) 500 else 360,
        Bitmap.Config.ARGB_8888
      ).apply {
        Canvas(this).drawColor(Color.rgb(30 * idx, 90, 170))
      }
      val uri = PhotoCaptureUtils.saveCameraBitmapWithStamp(
        context = context,
        sourceBitmap = bmp,
        taskTitle = "Проверка оборудования №$idx",
        authorName = actor.shortName,
        gpsCoords = "УЭСАиТМ",
        comment = "Фото #$idx"
      )
      val photoId = "ph_pdf_$idx"
      PhotoReportItem(
        id = photoId,
        uriString = PhotoCaptureUtils.attachCloudDocId(uri, photoId),
        authorId = actor.id,
        authorName = actor.shortName,
        capturedAtText = "14.11 19:0$idx",
        gpsCoords = "УЭСАиТМ",
        comment = "Фотофиксация #$idx"
      ).encode()
    }

    val taskWithEightPhotos = com.example.data.model.TaskDocument(
      id = "task_pdf_8_photos",
      userId = "u1",
      title = "Защита шкафа системы откачки",
      assigneeId = actor.id,
      assigneeName = actor.shortName,
      groupId = actor.groupId,
      groupName = actor.groupName,
      dueDateEpochDay = today,
      photoItems = photoItems,
      geoLocation = "УЭСАиТМ"
    )

    val template = com.example.data.model.ReportTemplateDocument(
      id = "tpl_pdf_test",
      name = "Отчет за текущий день",
      reportType = com.example.data.model.ReportType.DAILY_REPORT.code,
      sheetLayout = com.example.data.model.SheetLayout.SINGLE_SHEET.code,
      exportFormat = com.example.data.model.ExportFormat.PDF.code,
      orgHeader = com.example.domain.reporting.ReportDocumentGenerator.formatOrgHeader(
        organizationName = "ООО «Транснефть-Балтика»",
        subdivisionName = actor.groupName,
        siteName = actor.siteName
      )
    )

    val generated = com.example.domain.reporting.ReportDocumentGenerator.generateAndSaveDocument(
      context = context,
      actor = actor,
      template = template,
      allTasks = listOf(taskWithEightPhotos),
      currentEpochDay = today,
      overrideLayout = com.example.data.model.SheetLayout.SINGLE_SHEET,
      overrideFormat = com.example.data.model.ExportFormat.PDF,
      includePhotos = true,
      saveToDownloads = false
    )

    assertTrue("PDF file must exist and be non-empty", generated.file.exists() && generated.file.length() > 1000L)
    assertEquals(8, generated.attachedPhotos.size)
    assertEquals("Expected 1 report page + 2 photo attachment pages for 8 photos", 3, ShadowFakePdfDocument.finishedPageCount)
    assertEquals(listOf(1, 2, 3), ShadowFakePdfDocument.startedPageNumbers)
    assertTrue("PdfDocument must be closed after export", ShadowFakePdfDocument.isClosed)
    assertEquals("ООО «Транснефть-Балтика», ЛПДС \"Быково\", УЭСАиТМ", generated.orgHeader)
    assertTrue(generated.preparedByLabel.contains("Плиткин Дмитрий Александрович"))
    assertTrue(generated.preparedByLabel.contains("Инженер по КИПиА"))
    assertTrue(generated.preparedByLabel.contains("Участок: УЭСАиТМ"))
    assertTrue(generated.signatureBlock.first().contains("____________________ (подпись), Плиткин Дмитрий Александрович, Инженер по КИПиА, Участок: УЭСАиТМ"))
  }
}
