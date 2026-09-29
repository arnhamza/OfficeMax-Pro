package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.CellCoord
import com.example.util.OcrEngine
import com.example.util.SpreadsheetEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("OfficePro", appName)
    }

    @Test
    fun `test spreadsheet formula evaluation`() {
        val grid = mapOf(
            CellCoord(0, 0) to "10",
            CellCoord(0, 1) to "20",
            CellCoord(0, 2) to "30",
            CellCoord(1, 0) to "=SUM(A1:C1)"
        )
        val result = SpreadsheetEngine.evaluateCell("=SUM(A1:C1)", grid)
        assertEquals("60", result)
    }

    @Test
    fun `test spreadsheet average formula`() {
        val grid = mapOf(
            CellCoord(0, 0) to "10",
            CellCoord(0, 1) to "30",
            CellCoord(1, 0) to "=AVG(A1:B1)"
        )
        val result = SpreadsheetEngine.evaluateCell("=AVG(A1:B1)", grid)
        assertEquals("20", result)
    }

    @Test
    fun `test ocr entity extraction`() {
        val text = OcrEngine.sampleReceiptOcr
        val analysis = OcrEngine.analyzeText(text)
        assertNotNull(analysis)
        assertTrue(analysis.detectedAmounts.isNotEmpty())
        assertTrue(analysis.detectedEmails.contains("support@metropolitanoffice.com"))
        assertTrue(analysis.confidenceScore > 80)
    }
}
