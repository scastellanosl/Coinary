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
import com.example.coinary.model.SavingsGoal
import com.example.coinary.model.Debt
import java.io.OutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.core.graphics.toColorInt

/**
 * Utility class responsible for generating PDF financial reports.
 * Now includes sections for Savings Goals and Debts with detailed progress information.
 */
class PdfGenerator(private val context: Context) {

    /**
     * Generates a PDF report and writes it to the provided OutputStream.
     */
    fun generateReport(
        outputStream: OutputStream,
        transactions: List<Any>,
        savingsGoals: List<SavingsGoal> = emptyList(),
        debts: List<Debt> = emptyList(),
        month: String,
        year: String,
        filterType: String
    ) {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val titlePaint = Paint()

        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val expenseCategories = context.resources.getStringArray(R.array.expense_categories)
        val incomeCategories = context.resources.getStringArray(R.array.income_categories)

        val categoryNameMap = mapOf(
            "Comida" to expenseCategories[0], "Food" to expenseCategories[0], "Nourriture" to expenseCategories[0], "Alimentação" to expenseCategories[0],
            "Transporte" to expenseCategories[1], "Transport" to expenseCategories[1], "Transports" to expenseCategories[1],
            "Vivienda" to expenseCategories[2], "Housing" to expenseCategories[2], "Habitação" to expenseCategories[2], "Logement" to expenseCategories[2],
            "Ocio" to expenseCategories[3], "Entertainment" to expenseCategories[3], "Lazer" to expenseCategories[3], "Divertissement" to expenseCategories[3],
            "Servicios" to expenseCategories[4], "Services" to expenseCategories[4], "Serviços" to expenseCategories[4],
            "Compras" to expenseCategories[5], "Shopping" to expenseCategories[5], "Achats" to expenseCategories[5],
            "Salud" to expenseCategories[6], "Health" to expenseCategories[6], "Saúde" to expenseCategories[6], "Santé" to expenseCategories[6],
            "Educación" to expenseCategories[7], "Education" to expenseCategories[7], "Educação" to expenseCategories[7],
            "Otros Gastos" to expenseCategories[8], "Other Expenses" to expenseCategories[8], "Outros" to expenseCategories[8], "Autres" to expenseCategories[8],
            "Pago Deuda" to "Pago Deuda",
            "Ahorro" to "Ahorro",
            "Salario" to incomeCategories[0], "Salary" to incomeCategories[0], "Salário" to incomeCategories[0], "Salaire" to incomeCategories[0],
            "Regalo" to incomeCategories[1], "Gift" to incomeCategories[1], "Presente" to incomeCategories[1], "Cadeau" to incomeCategories[1],
            "Ventas" to incomeCategories[2], "Sales" to incomeCategories[2], "Vendas" to incomeCategories[2], "Ventes" to incomeCategories[2],
            "Inversión" to incomeCategories[3], "Investment" to incomeCategories[3], "Investimento" to incomeCategories[3], "Investissement" to incomeCategories[3],
            "Otros Ingresos" to incomeCategories[4], "Other Income" to incomeCategories[4], "Outros" to incomeCategories[4], "Autres revenus" to incomeCategories[4]
        )

        titlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        titlePaint.textSize = 24f
        titlePaint.color = Color.BLACK

        paint.textSize = 12f
        paint.color = Color.BLACK

        var yPosition = 50f
        val xMargin = 40f

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())

        fun checkPagination(neededSpace: Float = 50f) {
            if (yPosition > pageInfo.pageHeight - neededSpace) {
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPosition = 50f
            }
        }

        // Header
        val appName = context.getString(R.string.app_name)
        val reportTitle = context.getString(R.string.pdf_report_title)
        canvas.drawText("$appName - $reportTitle", xMargin, yPosition, titlePaint)
        yPosition += 40f

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 14f

        canvas.drawText("${context.getString(R.string.pdf_period_label)} $month $year", xMargin, yPosition, paint)
        yPosition += 20f
        canvas.drawText("${context.getString(R.string.pdf_type_label)} $filterType", xMargin, yPosition, paint)
        yPosition += 40f

        // TRANSACTIONS SECTION
        paint.textSize = 16f
        paint.color = "#4D54BF".toColorInt()
        canvas.drawText(" TRANSACCIONES", xMargin, yPosition, paint)
        yPosition += 30f

        paint.textSize = 12f
        paint.color = Color.DKGRAY
        canvas.drawText(context.getString(R.string.pdf_header_date), xMargin, yPosition, paint)
        canvas.drawText(context.getString(R.string.pdf_header_category), xMargin + 80, yPosition, paint)
        canvas.drawText(context.getString(R.string.pdf_header_description), xMargin + 200, yPosition, paint)
        canvas.drawText(context.getString(R.string.pdf_header_amount), xMargin + 400, yPosition, paint)

        paint.strokeWidth = 1f
        canvas.drawLine(xMargin, yPosition + 5, pageInfo.pageWidth - xMargin, yPosition + 5, paint)
        yPosition += 25f

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.BLACK
        paint.textSize = 11f

        var totalIncome = 0.0
        var totalExpense = 0.0

        for (item in transactions) {
            checkPagination()

            var dateStr = ""
            var originalCategory = ""
            var description = ""
            var amount = 0.0

            if (item is Income) {
                dateStr = dateFormat.format(item.date.toDate())
                originalCategory = item.category
                description = item.description
                amount = item.amount
                totalIncome += amount
                paint.color = "#2E7D32".toColorInt()
            } else if (item is Expense) {
                dateStr = dateFormat.format(item.date.toDate())
                originalCategory = item.category
                description = item.description
                amount = item.amount
                totalExpense += amount
                paint.color = "#C62828".toColorInt()
            }

            val displayCategory = categoryNameMap[originalCategory] ?: originalCategory

            canvas.drawText(dateStr, xMargin, yPosition, paint)
            val safeCategory = if (displayCategory.length > 15) displayCategory.substring(0, 15) + "..." else displayCategory
            canvas.drawText(safeCategory, xMargin + 80, yPosition, paint)
            val safeDesc = if (description.length > 25) description.substring(0, 25) + "..." else description
            canvas.drawText(safeDesc, xMargin + 200, yPosition, paint)
            canvas.drawText(currencyFormat.format(amount), xMargin + 400, yPosition, paint)

            yPosition += 20f
        }

        // Totals
        yPosition += 20f
        paint.strokeWidth = 1f
        paint.color = Color.BLACK
        canvas.drawLine(xMargin, yPosition, pageInfo.pageWidth - xMargin, yPosition, paint)
        yPosition += 30f

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 12f

        if (totalIncome > 0) {
            paint.color = "#2E7D32".toColorInt()
            canvas.drawText("${context.getString(R.string.pdf_total_income)} ${currencyFormat.format(totalIncome)}", xMargin, yPosition, paint)
            yPosition += 20f
        }
        if (totalExpense > 0) {
            paint.color = "#C62828".toColorInt()
            canvas.drawText("${context.getString(R.string.pdf_total_expense)} ${currencyFormat.format(totalExpense)}", xMargin, yPosition, paint)
            yPosition += 20f
        }

        val balance = totalIncome - totalExpense
        paint.color = Color.BLACK
        canvas.drawText("${context.getString(R.string.pdf_net_balance)} ${currencyFormat.format(balance)}", xMargin, yPosition, paint)
        yPosition += 50f

        // SAVINGS GOALS SECTION
        if (savingsGoals.isNotEmpty()) {
            checkPagination(150f)

            paint.textSize = 16f
            paint.color = "#4CAF50".toColorInt()
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("METAS DE AHORRO", xMargin, yPosition, paint)
            yPosition += 30f

            paint.textSize = 11f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            for (goal in savingsGoals) {
                checkPagination(100f)

                val percentage = if (goal.targetAmount > 0) {
                    ((goal.currentAmount / goal.targetAmount) * 100).toInt()
                } else 0

                // Box background
                paint.color = "#E8F5E9".toColorInt()
                paint.style = Paint.Style.FILL
                canvas.drawRect(xMargin, yPosition - 15, pageInfo.pageWidth - xMargin, yPosition + 70, paint)

                // Border
                paint.color = "#4CAF50".toColorInt()
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 2f
                canvas.drawRect(xMargin, yPosition - 15, pageInfo.pageWidth - xMargin, yPosition + 70, paint)

                paint.style = Paint.Style.FILL
                paint.color = Color.BLACK

                // Title
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 12f
                canvas.drawText(" ${goal.name}", xMargin + 10, yPosition, paint)

                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.textSize = 10f

                // Target
                canvas.drawText("Meta: ${currencyFormat.format(goal.targetAmount)}", xMargin + 10, yPosition + 18, paint)

                // Current
                paint.color = "#2E7D32".toColorInt()
                canvas.drawText("Aportado: ${currencyFormat.format(goal.currentAmount)}", xMargin + 10, yPosition + 33, paint)

                // Progress
                paint.color = "#1976D2".toColorInt()
                canvas.drawText("Progreso: $percentage%", xMargin + 10, yPosition + 48, paint)

                // Progress Bar
                val barWidth = 150f
                val barHeight = 8f
                val barX = xMargin + 250
                val barY = yPosition + 20

                paint.color = "#E0E0E0".toColorInt()
                paint.style = Paint.Style.FILL
                canvas.drawRect(barX, barY, barX + barWidth, barY + barHeight, paint)

                val progressWidth = (barWidth * percentage / 100f).coerceAtMost(barWidth)
                paint.color = "#4CAF50".toColorInt()
                canvas.drawRect(barX, barY, barX + progressWidth, barY + barHeight, paint)

                paint.style = Paint.Style.FILL
                paint.color = Color.BLACK

                yPosition += 85f
            }

            yPosition += 20f
        }

        // DEBTS SECTION
        if (debts.isNotEmpty()) {
            checkPagination(150f)

            paint.textSize = 16f
            paint.color = "#FF5722".toColorInt()
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("DEUDAS", xMargin, yPosition, paint)
            yPosition += 30f

            paint.textSize = 11f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            for (debt in debts) {
                checkPagination(120f)

                val percentage = if (debt.amount > 0) {
                    ((debt.amountPaid / debt.amount) * 100).toInt()
                } else 0

                val remaining = debt.amount - debt.amountPaid

                // Box background
                paint.color = "#FFEBEE".toColorInt()
                paint.style = Paint.Style.FILL
                canvas.drawRect(xMargin, yPosition - 15, pageInfo.pageWidth - xMargin, yPosition + 85, paint)

                // Border
                paint.color = "#FF5722".toColorInt()
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 2f
                canvas.drawRect(xMargin, yPosition - 15, pageInfo.pageWidth - xMargin, yPosition + 85, paint)

                paint.style = Paint.Style.FILL
                paint.color = Color.BLACK

                // Title
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 12f
                canvas.drawText(" ${debt.description}", xMargin + 10, yPosition, paint)

                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.textSize = 10f

                // Total
                canvas.drawText("Total: ${currencyFormat.format(debt.amount)}", xMargin + 10, yPosition + 18, paint)

                // Paid
                paint.color = Color.parseColor("#2E7D32")
                canvas.drawText("Pagado: ${currencyFormat.format(debt.amountPaid)}", xMargin + 10, yPosition + 33, paint)

                // Remaining
                paint.color = "#C62828".toColorInt()
                canvas.drawText("Restante: ${currencyFormat.format(remaining)}", xMargin + 10, yPosition + 48, paint)

                // Deadline
                paint.color = "#F57C00".toColorInt()
                val deadlineStr = dateFormat.format(debt.dueDate.toDate())
                canvas.drawText("Fecha límite: $deadlineStr", xMargin + 10, yPosition + 63, paint)

                // Progress Bar
                val barWidth = 120f
                val barHeight = 8f
                val barX = xMargin + 280
                val barY = yPosition + 20

                paint.color = "#E0E0E0".toColorInt()
                paint.style = Paint.Style.FILL
                canvas.drawRect(barX, barY, barX + barWidth, barY + barHeight, paint)

                val progressWidth = (barWidth * percentage / 100f).coerceAtMost(barWidth)
                paint.color = "#FF5722".toColorInt()
                canvas.drawRect(barX, barY, barX + progressWidth, barY + barHeight, paint)

                paint.style = Paint.Style.FILL
                paint.color = Color.BLACK
                paint.textSize = 9f
                canvas.drawText("$percentage%", barX + barWidth + 10, barY + 6, paint)

                paint.color = Color.BLACK
                yPosition += 100f
            }
        }

        pdfDocument.finishPage(page)

        try {
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