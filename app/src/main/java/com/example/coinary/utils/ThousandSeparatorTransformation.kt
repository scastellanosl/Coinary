package com.example.coinary.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * VisualTransformation para formatear números con separadores de miles.
 * Ejemplo: 1000000 -> 1.000.000
 *
 * Uso:
 * ```
 * TextField(
 *     value = amount,
 *     onValueChange = { amount = it },
 *     visualTransformation = ThousandSeparatorTransformation()
 * )
 * ```
 */
class ThousandSeparatorTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text

        if (originalText.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        // Separar parte entera y decimal
        val parts = originalText.split(".")
        val integerPart = parts[0]
        val decimalPart = if (parts.size > 1) parts[1] else ""

        // Formatear la parte entera con separadores de miles
        val formattedInteger = if (integerPart.isNotEmpty()) {
            integerPart.reversed()
                .chunked(3)
                .joinToString(".")
                .reversed()
        } else {
            ""
        }

        // Reconstruir el número completo
        val formattedText = when {
            decimalPart.isNotEmpty() -> "$formattedInteger.$decimalPart"
            originalText.endsWith(".") -> "$formattedInteger."
            else -> formattedInteger
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset == 0) return 0
                if (originalText.isEmpty()) return 0

                val decimalIndex = originalText.indexOf('.')
                val isAfterDecimal = decimalIndex >= 0 && offset > decimalIndex

                return if (isAfterDecimal) {
                    val integerLength = decimalIndex
                    val separatorsBeforeDecimal = if (integerLength > 0) {
                        (integerLength - 1) / 3
                    } else {
                        0
                    }
                    offset + separatorsBeforeDecimal
                } else {
                    val charsFromEnd = (decimalIndex.takeIf { it >= 0 } ?: originalText.length) - offset
                    val separatorsAfter = maxOf(0, (charsFromEnd - 1) / 3)
                    val totalSeparators = if (decimalIndex >= 0) {
                        maxOf(0, (decimalIndex - 1) / 3)
                    } else {
                        maxOf(0, (originalText.length - 1) / 3)
                    }
                    offset + (totalSeparators - separatorsAfter)
                }
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset == 0) return 0
                if (formattedText.isEmpty()) return 0

                val lastDotIndex = formattedText.lastIndexOf('.')
                val firstDotIndex = formattedText.indexOf('.')

                val decimalPointInFormatted = if (firstDotIndex != lastDotIndex) {
                    lastDotIndex
                } else if (originalText.contains('.')) {
                    firstDotIndex
                } else {
                    -1
                }

                val isAfterDecimal = decimalPointInFormatted >= 0 && offset > decimalPointInFormatted

                return if (isAfterDecimal) {
                    val separatorsBeforeDecimal = formattedText.substring(0, decimalPointInFormatted).count { it == '.' }
                    (offset - separatorsBeforeDecimal).coerceIn(0, originalText.length)
                } else {
                    val separatorCount = formattedText.substring(0, offset.coerceAtMost(formattedText.length)).count { it == '.' }
                    (offset - separatorCount).coerceIn(0, originalText.length)
                }
            }
        }

        return TransformedText(
            AnnotatedString(formattedText),
            offsetMapping
        )
    }
}
