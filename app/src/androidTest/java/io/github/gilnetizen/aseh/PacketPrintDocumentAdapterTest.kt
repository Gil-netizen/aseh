package io.github.gilnetizen.aseh

import android.graphics.pdf.PdfRenderer
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PacketPrintDocumentAdapterTest {
  @Test
  fun highDpiPrinterStillProducesAPracticalPageCount() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val attributes = PrintAttributes.Builder()
      .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
      .setResolution(PrintAttributes.Resolution("test-600", "600 dpi", 600, 600))
      .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
      .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
      .build()
    val output = File(context.cacheDir, "packet-print-high-dpi.pdf").apply { delete() }
    val writtenPages = output.outputStream().use { stream ->
      writePacketPdf(
        context = context,
        attributes = attributes,
        documentTitle = "ASEH packet",
        packet = List(80) { index ->
          "Segment ${index + 1}: assignment, source trace, access cue, and preparation status."
        }.joinToString("\n"),
        largeText = false,
        requestedPages = arrayOf(PageRange.ALL_PAGES),
        cancellationSignal = CancellationSignal(),
        output = stream,
      )
    }
    assertTrue(writtenPages != null && writtenPages.isNotEmpty())

    ParcelFileDescriptor.open(output, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
      PdfRenderer(descriptor).use { renderer ->
        assertTrue("Expected at least one printed page", renderer.pageCount > 0)
        assertTrue(
          "Printer DPI must not inflate the packet into excessive pages",
          renderer.pageCount <= 10,
        )
      }
    }
  }
}
