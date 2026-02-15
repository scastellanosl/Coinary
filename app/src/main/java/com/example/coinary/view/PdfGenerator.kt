package com.example.coinary.view

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import com.example.coinary.R
import com.example.coinary.model.Debt
import com.example.coinary.model.Expense
import com.example.coinary.model.Income
import com.example.coinary.model.SavingsGoal
import java.io.OutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * PdfGenerator: Utility class responsible for creating PDF financial reports.
 * It renders transactions, savings goals, and debts into a paginated PDF document.
 *
 * @param context The application context used to access resources and strings.
 */
class PdfGenerator(private val context: Context) {

    /**
     * Generates a PDF report and writes it to the provided OutputStream.
     *
     * @param outputStream The stream where the PDF file will be written.
     * @param transactions Mixed list of Income and Expense objects to list in the report.
     * @param savingsGoals List of active savings goals to visualize progress.
     * @param debts List of outstanding debts with payment progress.
     * @param month The target month for the report context.
     * @param year The target year for the report context.
     * @param filterType Description of the applied filter (e.g., "Monthly", "Yearly").
     */
    fun generateReport(
        outputStream: OutputStream,
        transactions: List<Any>,
        savingsGoals: List<SavingsGoal>,
        debts: List<Debt>,
        month: String,
        year: String,
        filterType: String
    ) {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val titlePaint = Paint()

        // Page Configuration (Standard A4 size approx: 595x842 points)
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        // --- Category Translation Map ---
        val expenseCategories = context.resources.getStringArray(R.array.expense_categories)
        val incomeCategories = context.resources.getStringArray(R.array.income_categories)
        val categoryNameMap = mapOf(
            "Comida" to expenseCategories[0], "Food" to expenseCategories[0],
            "Transporte" to expenseCategories[1], "Transport" to expenseCategories[1],
            "Vivienda" to expenseCategories[2], "Housing" to expenseCategories[2],
            "Ocio" to expenseCategories[3], "Entertainment" to expenseCategories[3],
            "Servicios" to expenseCategories[4], "Services" to expenseCategories[4],
            "Compras" to expenseCategories[5], "Shopping" to expenseCategories[5],
            "Salud" to expenseCategories[6], "Health" to expenseCategories[6],
            "Educación" to expenseCategories[7], "Education" to expenseCategories[7],
            "Otros Gastos" to expenseCategories[8], "Other Expenses" to expenseCategories[8],
            "Pago Deuda" to "Debt Payment", "Ahorro" to "Savings",
            "Salario" to incomeCategories[0], "Salary" to incomeCategories[0],
            "Regalo" to incomeCategories[1], "Gift" to incomeCategories[1],
            "Ventas" to incomeCategories[2], "Sales" to incomeCategories[2],
            "Inversión" to incomeCategories[3], "Investment" to incomeCategories[3],
            "Otros Ingresos" to incomeCategories[4], "Other Income" to incomeCategories[4]
        )

        // Font Configuration
        titlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        titlePaint.textSize = 24f
        titlePaint.color = Color.BLACK

        paint.textSize = 12f
        paint.color = Color.BLACK

        var yPosition = 50f
        val xMargin = 40f

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())

        // --- Pagination Helper ---
        fun checkPagination(neededSpace: Float = 50f) {
            if (yPosition > pageInfo.pageHeight - neededSpace) {
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPosition = 50f // Reset Y position for new page
            }
        }

        // --- HEADER SECTION ---
        val appName = context.getString(R.string.app_name)
        val reportTitle = "Financial Report"
        canvas.drawText("$appName - $reportTitle", xMargin, yPosition, titlePaint)
        yPosition += 40f

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 14f
        canvas.drawText("Period: $month $year", xMargin, yPosition, paint)
        yPosition += 20f
        canvas.drawText("Filter: $filterType", xMargin, yPosition, paint)
        yPosition += 40f

        // --- SECTION: TRANSACTIONS ---
        paint.textSize = 16f
        paint.color = Color.parseColor("#4D54BF") // Brand Blue
        canvas.drawText("TRANSACTIONS", xMargin, yPosition, paint)
        yPosition += 30f

        // Table Headers
        paint.textSize = 12f
        paint.color = Color.DKGRAY
        canvas.drawText("Date", xMargin, yPosition, paint)
        canvas.drawText("Category", xMargin + 80, yPosition, paint)
        canvas.drawText("Description", xMargin + 200, yPosition, paint)
        canvas.drawText("Amount", xMargin + 400, yPosition, paint)

        paint.strokeWidth = 1f
        canvas.drawLine(xMargin, yPosition + 5, pageInfo.pageWidth - xMargin, yPosition + 5, paint)
        yPosition += 25f

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 11f

        var totalIncome = 0.0
        var totalExpense = 0.0

        for (item in transactions) {
            checkPagination()

            var dateStr = ""
            var originalCategory = ""
            var description = ""
            var amount = 0.0
            var amountColor = Color.BLACK

            if (item is Income) {
                dateStr = dateFormat.format(item.date.toDate())
                originalCategory = item.category
                description = item.description
                amount = item.amount
                totalIncome += amount
                amountColor = Color.parseColor("#2E7D32") // Green
            } else if (item is Expense) {
                dateStr = dateFormat.format(item.date.toDate())
                originalCategory = item.category
                description = item.description
                amount = item.amount
                totalExpense += amount
                amountColor = Color.parseColor("#C62828") // Red
            }

            val displayCategory = categoryNameMap[originalCategory] ?: originalCategory

            paint.color = Color.BLACK
            canvas.drawText(dateStr, xMargin, yPosition, paint)

            val safeCategory = if (displayCategory.length > 15) displayCategory.substring(0, 15) + "..." else displayCategory
            canvas.drawText(safeCategory, xMargin + 80, yPosition, paint)

            val safeDesc = if (description.length > 25) description.substring(0, 25) + "..." else description
            canvas.drawText(safeDesc, xMargin + 200, yPosition, paint)

            paint.color = amountColor
            canvas.drawText(currencyFormat.format(amount), xMargin + 400, yPosition, paint)

            yPosition += 20f
        }

        // --- TOTALS SUMMARY ---
        yPosition += 20f
        paint.color = Color.BLACK
        canvas.drawLine(xMargin, yPosition, pageInfo.pageWidth - xMargin, yPosition, paint)
        yPosition += 30f

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 12f

        if (totalIncome > 0) {
            paint.color = Color.parseColor("#2E7D32")
            canvas.drawText("Total Income: ${currencyFormat.format(totalIncome)}", xMargin, yPosition, paint)
            yPosition += 20f
        }
        if (totalExpense > 0) {
            paint.color = Color.parseColor("#C62828")
            canvas.drawText("Total Expenses: ${currencyFormat.format(totalExpense)}", xMargin, yPosition, paint)
            yPosition += 20f
        }

        val balance = totalIncome - totalExpense
        paint.color = Color.BLACK
        canvas.drawText("Net Balance: ${currencyFormat.format(balance)}", xMargin, yPosition, paint)
        yPosition += 50f

        // --- SECTION: SAVINGS GOALS ---
        if (savingsGoals.isNotEmpty()) {
            checkPagination(100f) // Ensure space for header + at least one item

            paint.textSize = 16f
            paint.color = Color.parseColor("#4CAF50") // Material Green
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("SAVINGS GOALS", xMargin, yPosition, paint)
            yPosition += 30f

            for (goal in savingsGoals) {
                checkPagination(100f) // Space for one goal card

                val percentage = if (goal.targetAmount > 0) {
                    ((goal.currentAmount / goal.targetAmount) * 100).toInt()
                } else 0

                // Card Background
                val boxTop = yPosition - 15
                val boxBottom = yPosition + 70

                paint.color = Color.parseColor("#E8F5E9") // Very light green
                paint.style = Paint.Style.FILL
                canvas.drawRect(xMargin, boxTop, pageInfo.pageWidth - xMargin, boxBottom, paint)

                // Card Border
                paint.color = Color.parseColor("#4CAF50")
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 2f
                canvas.drawRect(xMargin, boxTop, pageInfo.pageWidth - xMargin, boxBottom, paint)

                // Text Content
                paint.style = Paint.Style.FILL
                paint.strokeWidth = 0f
                paint.color = Color.BLACK
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 12f
                canvas.drawText(" ${goal.name}", xMargin + 10, yPosition, paint)

                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.textSize = 10f
                canvas.drawText("Target: ${currencyFormat.format(goal.targetAmount)}", xMargin + 10, yPosition + 18, paint)

                paint.color = Color.parseColor("#2E7D32")
                canvas.drawText("Saved: ${currencyFormat.format(goal.currentAmount)}", xMargin + 10, yPosition + 33, paint)

                paint.color = Color.parseColor("#1976D2")
                canvas.drawText("Progress: $percentage%", xMargin + 10, yPosition + 48, paint)

                // Visual Progress Bar
                val barWidth = 150f
                val barHeight = 8f
                val barX = xMargin + 250
                val barY = yPosition + 20

                // Bar Background
                paint.color = Color.LTGRAY
                canvas.drawRect(barX, barY, barX + barWidth, barY + barHeight, paint)

                // Bar Progress
                val progressWidth = (barWidth * percentage / 100f).coerceAtMost(barWidth)
                paint.color = Color.parseColor("#4CAF50")
                canvas.drawRect(barX, barY, barX + progressWidth, barY + barHeight, paint)

                yPosition += 95f // Space for next item
            }
        }

        // --- SECTION: DEBTS ---
        if (debts.isNotEmpty()) {
            checkPagination(100f)

            paint.textSize = 16f
            paint.color = Color.parseColor("#FF5722") // Deep Orange/Red
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("DEBTS", xMargin, yPosition, paint)
            yPosition += 30f

            for (debt in debts) {
                checkPagination(120f) // Space for one debt card

                val percentage = if (debt.amount > 0) {
                    ((debt.amountPaid / debt.amount) * 100).toInt()
                } else 0
                val remaining = debt.amount - debt.amountPaid

                // Card Background
                val boxTop = yPosition - 15
                val boxBottom = yPosition + 85

                paint.color = Color.parseColor("#FFEBEE") // Very light red
                paint.style = Paint.Style.FILL
                canvas.drawRect(xMargin, boxTop, pageInfo.pageWidth - xMargin, boxBottom, paint)

                // Card Border
                paint.color = Color.parseColor("#FF5722")
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 2f
                canvas.drawRect(xMargin, boxTop, pageInfo.pageWidth - xMargin, boxBottom, paint)

                // Text Content
                paint.style = Paint.Style.FILL
                paint.strokeWidth = 0f
                paint.color = Color.BLACK
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 12f
                canvas.drawText(" ${debt.description}", xMargin + 10, yPosition, paint)

                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.textSize = 10f

                canvas.drawText("Total: ${currencyFormat.format(debt.amount)}", xMargin + 10, yPosition + 18, paint)

                paint.color = Color.parseColor("#2E7D32")
                canvas.drawText("Paid: ${currencyFormat.format(debt.amountPaid)}", xMargin + 10, yPosition + 33, paint)

                paint.color = Color.parseColor("#C62828")
                canvas.drawText("Remaining: ${currencyFormat.format(remaining)}", xMargin + 10, yPosition + 48, paint)

                paint.color = Color.parseColor("#F57C00")
                val deadlineStr = try {
                    dateFormat.format(debt.dueDate.toDate())
                } catch (e: Exception) { "No Date" }
                canvas.drawText("Deadline: $deadlineStr", xMargin + 10, yPosition + 63, paint)

                // Progress Bar
                val barWidth = 120f
                val barHeight = 8f
                val barX = xMargin + 280
                val barY = yPosition + 20

                paint.color = Color.LTGRAY
                canvas.drawRect(barX, barY, barX + barWidth, barY + barHeight, paint)

                val progressWidth = (barWidth * percentage / 100f).coerceAtMost(barWidth)
                paint.color = Color.parseColor("#FF5722")
                canvas.drawRect(barX, barY, barX + progressWidth, barY + barHeight, paint)

                paint.color = Color.BLACK
                paint.textSize = 9f
                canvas.drawText("$percentage%", barX + barWidth + 10, barY + 6, paint)

                yPosition += 110f
            }
        }

        // Finish the last page
        pdfDocument.finishPage(page)

        try {
            pdfDocument.writeTo(outputStream)
            Toast.makeText(context, "PDF Report generated successfully", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            // Kept as Toast because PDF generation errors are critical and user-facing for immediate feedback
            Toast.makeText(context, "Error generating PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
            outputStream.close()
        }
    }
}