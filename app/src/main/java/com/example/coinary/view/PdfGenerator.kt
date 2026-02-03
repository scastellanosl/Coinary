package com.example.coinary.view

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import com.example.coinary.R
import com.example.coinary.model.Expense
import com.example.coinary.model.Income
import java.io.OutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Utility class responsible for generating PDF financial reports.
 * Uses Android's native PdfDocument API to draw text, tables, and financial data onto a Canvas.
 */
class PdfGenerator(private val context: Context) {

    /**
     * Generates a PDF report and writes it to the provided OutputStream.
     *
     * @param outputStream The stream where the PDF file will be written.
     * @param transactions A list containing [Income] and [Expense] objects to be reported.
     * @param month The name of the month for the report header.
     * @param year The year for the report header.
     * @param filterType A localized string describing the filter applied (e.g., "Income Only").
     */
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

        // --- Page Configuration (Standard A4 Size) ---
        // Width: 595, Height: 842 points (1/72 inch)
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        // --- 1. Resource Preparation & Localization ---

        // Load localized category arrays from resources
        val expenseCategories = context.resources.getStringArray(R.array.expense_categories)
        val incomeCategories = context.resources.getStringArray(R.array.income_categories)

        // Translation Map: Maps potential database values (stored in various languages)
        // to the current device locale to ensure consistency in the report.
        val categoryNameMap = mapOf(
            // Expense Translations
            "Comida" to expenseCategories[0], "Food" to expenseCategories[0], "Nourriture" to expenseCategories[0], "Alimentação" to expenseCategories[0],
            "Transporte" to expenseCategories[1], "Transport" to expenseCategories[1], "Transports" to expenseCategories[1],
            "Vivienda" to expenseCategories[2], "Housing" to expenseCategories[2], "Habitação" to expenseCategories[2], "Logement" to expenseCategories[2],
            "Ocio" to expenseCategories[3], "Entertainment" to expenseCategories[3], "Lazer" to expenseCategories[3], "Divertissement" to expenseCategories[3],
            "Servicios" to expenseCategories[4], "Services" to expenseCategories[4], "Serviços" to expenseCategories[4],
            "Compras" to expenseCategories[5], "Shopping" to expenseCategories[5], "Achats" to expenseCategories[5],
            "Salud" to expenseCategories[6], "Health" to expenseCategories[6], "Saúde" to expenseCategories[6], "Santé" to expenseCategories[6],
            "Educación" to expenseCategories[7], "Education" to expenseCategories[7], "Educação" to expenseCategories[7],
            "Otros Gastos" to expenseCategories[8], "Other Expenses" to expenseCategories[8], "Outros" to expenseCategories[8], "Autres" to expenseCategories[8],

            // Income Translations
            "Salario" to incomeCategories[0], "Salary" to incomeCategories[0], "Salário" to incomeCategories[0], "Salaire" to incomeCategories[0],
            "Regalo" to incomeCategories[1], "Gift" to incomeCategories[1], "Presente" to incomeCategories[1], "Cadeau" to incomeCategories[1],
            "Ventas" to incomeCategories[2], "Sales" to incomeCategories[2], "Vendas" to incomeCategories[2], "Ventes" to incomeCategories[2],
            "Inversión" to incomeCategories[3], "Investment" to incomeCategories[3], "Investimento" to incomeCategories[3], "Investissement" to incomeCategories[3],
            "Otros Ingresos" to incomeCategories[4], "Other Income" to incomeCategories[4], "Outros" to incomeCategories[4], "Autres revenus" to incomeCategories[4]
        )

        // --- Paint & Style Configuration ---
        titlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        titlePaint.textSize = 24f
        titlePaint.color = Color.BLACK

        paint.textSize = 12f
        paint.color = Color.BLACK

        var yPosition = 50f
        val xMargin = 40f

        // --- 2. Header Drawing ---
        val appName = context.getString(R.string.app_name)
        val reportTitle = context.getString(R.string.pdf_report_title)
        canvas.drawText("$appName - $reportTitle", xMargin, yPosition, titlePaint)
        yPosition += 40f

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 14f

        // Draw Period: "Period: January 2026"
        canvas.drawText("${context.getString(R.string.pdf_period_label)} $month $year", xMargin, yPosition, paint)
        yPosition += 20f

        // Draw Report Type: "Report Type: Expenses Only"
        canvas.drawText("${context.getString(R.string.pdf_type_label)} $filterType", xMargin, yPosition, paint)
        yPosition += 40f

        // --- 3. Table Headers ---
        paint.color = Color.DKGRAY
        canvas.drawText(context.getString(R.string.pdf_header_date), xMargin, yPosition, paint)
        canvas.drawText(context.getString(R.string.pdf_header_category), xMargin + 80, yPosition, paint)
        canvas.drawText(context.getString(R.string.pdf_header_description), xMargin + 200, yPosition, paint)
        canvas.drawText(context.getString(R.string.pdf_header_amount), xMargin + 400, yPosition, paint)

        // Separator Line
        paint.strokeWidth = 1f
        canvas.drawLine(xMargin, yPosition + 5, pageInfo.pageWidth - xMargin, yPosition + 5, paint)
        yPosition += 25f

        // --- 4. Transaction Data Loop ---
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.BLACK
        paint.textSize = 11f

        // Use default Locale for date and currency formatting based on user preference
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())

        var totalIncome = 0.0
        var totalExpense = 0.0

        for (item in transactions) {
            // Pagination Check: Start a new page if the current one is full
            if (yPosition > pageInfo.pageHeight - 50) {
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPosition = 50f
            }

            var dateStr = ""
            var originalCategory = ""
            var description = ""
            var amount = 0.0

            // Determine transaction type and assign color/values
            if (item is Income) {
                dateStr = dateFormat.format(item.date.toDate())
                originalCategory = item.category
                description = item.description
                amount = item.amount
                totalIncome += amount
                paint.color = Color.parseColor("#2E7D32") // Dark Green
            } else if (item is Expense) {
                dateStr = dateFormat.format(item.date.toDate())
                originalCategory = item.category
                description = item.description
                amount = item.amount
                totalExpense += amount
                paint.color = Color.parseColor("#C62828") // Dark Red
            }

            // --- Category Translation ---
            // Resolve the category name using the translation map created earlier
            val displayCategory = categoryNameMap[originalCategory] ?: originalCategory

            // Draw Row Data
            canvas.drawText(dateStr, xMargin, yPosition, paint)

            // Truncate category text to prevent overlap if too long
            val safeCategory = if (displayCategory.length > 15) displayCategory.substring(0, 15) + "..." else displayCategory
            canvas.drawText(safeCategory, xMargin + 80, yPosition, paint)

            // Truncate description text
            val safeDesc = if (description.length > 25) description.substring(0, 25) + "..." else description
            canvas.drawText(safeDesc, xMargin + 200, yPosition, paint)

            val amountStr = currencyFormat.format(amount)
            canvas.drawText(amountStr, xMargin + 400, yPosition, paint)

            yPosition += 20f
        }

        // --- 5. Totals Section ---
        yPosition += 20f
        paint.strokeWidth = 1f
        paint.color = Color.BLACK
        canvas.drawLine(xMargin, yPosition, pageInfo.pageWidth - xMargin, yPosition, paint)
        yPosition += 30f

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 12f

        if (totalIncome > 0) {
            paint.color = Color.parseColor("#2E7D32")
            // Example: "Total Income: $1,000.00"
            val label = "${context.getString(R.string.pdf_total_income)} ${currencyFormat.format(totalIncome)}"
            canvas.drawText(label, xMargin, yPosition, paint)
            yPosition += 20f
        }
        if (totalExpense > 0) {
            paint.color = Color.parseColor("#C62828")
            // Example: "Total Expenses: $500.00"
            val label = "${context.getString(R.string.pdf_total_expense)} ${currencyFormat.format(totalExpense)}"
            canvas.drawText(label, xMargin, yPosition, paint)
            yPosition += 20f
        }

        // Net Balance Calculation
        val balance = totalIncome - totalExpense
        paint.color = Color.BLACK
        // Example: "Net Balance: $500.00"
        val balanceLabel = "${context.getString(R.string.pdf_net_balance)} ${currencyFormat.format(balance)}"
        canvas.drawText(balanceLabel, xMargin, yPosition, paint)

        // Finish the final page
        pdfDocument.finishPage(page)

        try {
            // Write to stream
            pdfDocument.writeTo(outputStream)
            Toast.makeText(context, context.getString(R.string.pdf_generated_msg), Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, context.getString(R.string.pdf_error_msg, e.message), Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
            outputStream.close()
        }
    }
}