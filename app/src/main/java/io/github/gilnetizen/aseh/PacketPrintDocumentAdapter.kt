package io.github.gilnetizen.aseh

import android.content.Context
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import java.io.OutputStream

/** Opens Android's print UI for a packet generated entirely on this device. */
internal fun printServicePacket(
  context: Context,
  title: String,
  packet: String,
  largeText: Boolean = false,
) {
  val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
  printManager.print(
    title,
    PacketPrintDocumentAdapter(
      context = context,
      documentTitle = title,
      packet = packet,
      largeText = largeText,
    ),
    PrintAttributes.Builder()
      .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
      .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
      .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
      .build(),
  )
}

internal class PacketPrintDocumentAdapter(
  private val context: Context,
  private val documentTitle: String,
  private val packet: String,
  private val largeText: Boolean,
) : PrintDocumentAdapter() {
  private var attributes: PrintAttributes? = null

  override fun onLayout(
    oldAttributes: PrintAttributes?,
    newAttributes: PrintAttributes,
    cancellationSignal: CancellationSignal,
    callback: LayoutResultCallback,
    extras: Bundle?,
  ) {
    if (cancellationSignal.isCanceled) {
      callback.onLayoutCancelled()
      return
    }
    attributes = newAttributes
    callback.onLayoutFinished(
      PrintDocumentInfo.Builder("aseh-service-packet.pdf")
        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
        .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
        .build(),
      oldAttributes != newAttributes,
    )
  }

  override fun onWrite(
    pages: Array<out PageRange>,
    destination: ParcelFileDescriptor,
    cancellationSignal: CancellationSignal,
    callback: WriteResultCallback,
  ) {
    val currentAttributes = attributes
    if (currentAttributes == null) {
      callback.onWriteFailed("Print layout is unavailable.")
      return
    }

    try {
      val writtenPages = ParcelFileDescriptor.AutoCloseOutputStream(destination).use { output ->
        writePacketPdf(
          context = context,
          attributes = currentAttributes,
          documentTitle = documentTitle,
          packet = packet,
          largeText = largeText,
          requestedPages = pages,
          cancellationSignal = cancellationSignal,
          output = output,
        )
      }
      if (writtenPages == null) {
        callback.onWriteCancelled()
      } else {
        callback.onWriteFinished(writtenPages)
      }
    } catch (error: Exception) {
      callback.onWriteFailed(error.message ?: "The packet could not be printed.")
    }
  }
}

/** Writes a packet PDF. A null result means cancellation was observed. */
internal fun writePacketPdf(
  context: Context,
  attributes: PrintAttributes,
  documentTitle: String,
  packet: String,
  largeText: Boolean,
  requestedPages: Array<out PageRange>,
  cancellationSignal: CancellationSignal,
  output: OutputStream,
): Array<PageRange>? {
  val document = PrintedPdfDocument(context, attributes)
  try {
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = android.graphics.Color.BLACK
      // PrintedPdfDocument exposes its canvas in PostScript points (1/72 inch),
      // independent of the printer's raster DPI.
      textSize = if (largeText) LARGE_BODY_TEXT_POINTS else BODY_TEXT_POINTS
      typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
    }
    val titlePaint = Paint(bodyPaint).apply {
      textSize = if (largeText) LARGE_TITLE_TEXT_POINTS else TITLE_TEXT_POINTS
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val footerPaint = Paint(bodyPaint).apply {
      color = android.graphics.Color.DKGRAY
      textSize = FOOTER_TEXT_POINTS
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
      textAlign = Paint.Align.RIGHT
    }

    val layoutDocument = PrintedPdfDocument(context, attributes)
    val samplePage = layoutDocument.startPage(0)
    val content = Rect(samplePage.info.contentRect)
    layoutDocument.finishPage(samplePage)
    layoutDocument.close()
    val horizontalPadding = PAGE_PADDING_POINTS.toInt()
    val verticalPadding = horizontalPadding
    val textWidth = (content.width() - horizontalPadding * 2).toFloat().coerceAtLeast(1f)
    val bodyLineHeight = bodyPaint.fontSpacing
    val titleHeight = titlePaint.fontMetrics.run { bottom - top }
    val footerHeight = footerPaint.fontMetrics.run { bottom - top }
    val sectionGap = SECTION_GAP_POINTS
    val availableHeight = content.height() - verticalPadding * 2f
    val wrappedBody = packet.lineSequence().flatMap { paragraph ->
      wrapLine(paragraph, bodyPaint, textWidth).asSequence()
    }.toList()
    val bodyHeight = availableHeight - titleHeight - footerHeight - sectionGap * 2f
    val linesPerPage = (bodyHeight / bodyLineHeight).toInt().coerceAtLeast(1)
    val pageCount = (wrappedBody.size.coerceAtLeast(1) + linesPerPage - 1) / linesPerPage

    val writtenPages = mutableListOf<PageRange>()
    repeat(pageCount) { pageIndex ->
      if (cancellationSignal.isCanceled) return null
      if (!pageIndex.isRequested(requestedPages)) return@repeat

      val page = document.startPage(pageIndex)
      val canvas = page.canvas
      val pageContent = page.info.contentRect
      val left = pageContent.left + horizontalPadding.toFloat()
      val right = pageContent.right - horizontalPadding.toFloat()
      val top = pageContent.top + verticalPadding.toFloat()
      val bottom = pageContent.bottom - verticalPadding.toFloat()
      val titleBaseline = top - titlePaint.fontMetrics.top
      canvas.drawText(documentTitle, left, titleBaseline, titlePaint)

      val bodyTop = top + titleHeight + sectionGap
      var baseline = bodyTop - bodyPaint.fontMetrics.top
      val start = pageIndex * linesPerPage
      val end = minOf(start + linesPerPage, wrappedBody.size)
      for (lineIndex in start until end) {
        canvas.drawText(wrappedBody[lineIndex], left, baseline, bodyPaint)
        baseline += bodyLineHeight
      }

      val footer = "Page ${pageIndex + 1} of $pageCount"
      val footerBaseline = bottom - footerPaint.fontMetrics.bottom
      canvas.drawText(footer, right, footerBaseline, footerPaint)
      document.finishPage(page)
      writtenPages += PageRange(pageIndex, pageIndex)
    }

    document.writeTo(output)
    return writtenPages.toTypedArray()
  } finally {
    document.close()
  }
}

private fun wrapLine(
  paragraph: String,
  paint: Paint,
  maxWidth: Float,
): List<String> {
  if (paragraph.isBlank()) return listOf("")
  val result = mutableListOf<String>()
  var remaining = paragraph.trimEnd()
  while (remaining.isNotEmpty()) {
    val measured = paint.breakText(remaining, true, maxWidth, null).coerceAtLeast(1)
    val cut = if (measured < remaining.length) {
      remaining.substring(0, measured).lastIndexOf(' ').takeIf { it > 0 } ?: measured
    } else {
      measured
    }
    result += remaining.substring(0, cut).trimEnd()
    remaining = remaining.substring(cut).trimStart()
  }
  return result
}

private fun Int.isRequested(pageRanges: Array<out PageRange>): Boolean =
  pageRanges.any { range ->
    range == PageRange.ALL_PAGES || this in range.start..range.end
  }

private const val BODY_TEXT_POINTS = 10.5f
private const val LARGE_BODY_TEXT_POINTS = 14f
private const val TITLE_TEXT_POINTS = 16f
private const val LARGE_TITLE_TEXT_POINTS = 20f
private const val FOOTER_TEXT_POINTS = 9f
private const val PAGE_PADDING_POINTS = 36f
private const val SECTION_GAP_POINTS = 10f
