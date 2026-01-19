package com.example.coinary.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import com.example.coinary.model.Expense
import com.example.coinary.model.Income
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfGenerator(private val context: Context) {

    fun generateReport(
        outputStream: OutputStream,
        transactions: List<Any>,
        month: String,
        year: String,
        filterType: String
    ) {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val titlePaint = Paint()

        // Configuración de la página (A4 estándar)
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        // Configuración de Estilos
        titlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        titlePaint.textSize = 24f
        titlePaint.color = Color.BLACK

        paint.textSize = 12f
        paint.color = Color.BLACK

        var yPosition = 50f
        val xMargin = 40f

        // --- ENCABEZADO ---
        canvas.drawText("Reporte Financiero - Coinary", xMargin, yPosition, titlePaint)
        yPosition += 40f

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 14f
        canvas.drawText("Periodo: $month $year", xMargin, yPosition, paint)
        yPosition += 20f
        canvas.drawText("Tipo de Reporte: $filterType", xMargin, yPosition, paint)
        yPosition += 40f

        // --- TABLA ---
        // Encabezados de tabla
        paint.color = Color.DKGRAY
        canvas.drawText("Fecha", xMargin, yPosition, paint)
        canvas.drawText("Categoría", xMargin + 80, yPosition, paint)
        canvas.drawText("Descripción", xMargin + 200, yPosition, paint)
        canvas.drawText("Monto", xMargin + 400, yPosition, paint)

        // Línea separadora
        paint.strokeWidth = 1f
        canvas.drawLine(xMargin, yPosition + 5, pageInfo.pageWidth - xMargin, yPosition + 5, paint)
        yPosition += 25f

        // --- DATOS ---
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.BLACK
        paint.textSize = 11f

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO"))

        var totalIncome = 0.0
        var totalExpense = 0.0

        for (item in transactions) {
            // Chequeo de fin de página
            if (yPosition > pageInfo.pageHeight - 50) {
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPosition = 50f // Reiniciar posición
            }

            var dateStr = ""
            var category = ""
            var description = ""
            var amount = 0.0
            var isIncome = false

            if (item is Income) {
                dateStr = dateFormat.format(item.date.toDate())
                category = item.category
                description = item.description
                amount = item.amount
                totalIncome += amount
                isIncome = true
                paint.color = Color.parseColor("#2E7D32") // Verde oscuro
            } else if (item is Expense) {
                dateStr = dateFormat.format(item.date.toDate())
                category = item.category
                description = item.description
                amount = item.amount
                totalExpense += amount
                paint.color = Color.parseColor("#C62828") // Rojo oscuro
            }

            // Dibujar fila
            canvas.drawText(dateStr, xMargin, yPosition, paint)

            // Cortar texto largo de categoría
            val safeCategory = if (category.length > 15) category.substring(0, 15) + "..." else category
            canvas.drawText(safeCategory, xMargin + 80, yPosition, paint)

            // Cortar texto largo de descripción
            val safeDesc = if (description.length > 25) description.substring(0, 25) + "..." else description
            canvas.drawText(safeDesc, xMargin + 200, yPosition, paint)

            val amountStr = currencyFormat.format(amount).replace("RSP", "R$") // Ajuste moneda si es necesario
            canvas.drawText(amountStr, xMargin + 400, yPosition, paint)

            yPosition += 20f
        }

        // --- TOTALES ---
        yPosition += 20f
        paint.strokeWidth = 1f
        paint.color = Color.BLACK
        canvas.drawLine(xMargin, yPosition, pageInfo.pageWidth - xMargin, yPosition, paint)
        yPosition += 30f

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 12f

        if (totalIncome > 0) {
            paint.color = Color.parseColor("#2E7D32")
            canvas.drawText("Total Ingresos: ${currencyFormat.format(totalIncome)}", xMargin, yPosition, paint)
            yPosition += 20f
        }
        if (totalExpense > 0) {
            paint.color = Color.parseColor("#C62828")
            canvas.drawText("Total Gastos: ${currencyFormat.format(totalExpense)}", xMargin, yPosition, paint)
            yPosition += 20f
        }

        // Balance
        val balance = totalIncome - totalExpense
        paint.color = Color.BLACK
        canvas.drawText("Balance Neto: ${currencyFormat.format(balance)}", xMargin, yPosition, paint)

        pdfDocument.finishPage(page)

        try {
            pdfDocument.writeTo(outputStream)
            Toast.makeText(context, "PDF generado exitosamente", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error al generar PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
            outputStream.close()
        }
    }
}