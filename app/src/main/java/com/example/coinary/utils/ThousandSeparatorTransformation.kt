package com.example.coinary.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * ThousandSeparatorTransformation: A professional implementation of [VisualTransformation].
 * Specifically designed to format numeric input strings with thousands separators (dots)
 * in real-time, while preserving the integrity of the underlying raw data.
 *
 * Example: "1000000.50" -> "1.000.000.50"
 */
class ThousandSeparatorTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text

        // Return identity mapping if the input is empty to avoid unnecessary processing
        if (originalText.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        // Split the input into integer and decimal components to handle formatting separately
        val parts = originalText.split(".")
        val integerPart = parts[0]
        val decimalPart = if (parts.size > 1) parts[1] else ""

        // Format the integer part by reversing, chunking by 3, and joining with dots
        val formattedInteger = if (integerPart.isNotEmpty()) {
            integerPart.reversed()
                .chunked(3)
                .joinToString(".")
                .reversed()
        } else {
            ""
        }

        // Reconstruct the full string based on the presence of decimal points or parts
        val formattedText = when {
            decimalPart.isNotEmpty() -> "$formattedInteger.$decimalPart"
            originalText.endsWith(".") -> "$formattedInteger."
            else -> formattedInteger
        }

        /**
         * Custom OffsetMapping to handle cursor positioning.
         * Translates indices between the raw numeric string and the dot-separated display string.
         */
        val offsetMapping = object : OffsetMapping {

            /**
             * Maps a position from the raw input to the formatted output.
             * Adds the count of separators injected before the current cursor position.
             */
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
                        0
                    }
                    // Adjusted logic for pure integer formatting calculation
                    val actualTotalSeparators = if (decimalIndex < 0) {
                        maxOf(0, (originalText.length - 1) / 3)
                    } else {
                        totalSeparators
                    }
                    offset + (actualTotalSeparators - separatorsAfter)
                }
            }

            /**
             * Maps a position from the formatted output back to the raw input.
             * Subtracts the count of separators (dots) to find the actual data index.
             */
            override fun transformedToOriginal(offset: Int): Int {
                if (offset == 0) return 0
                if (formattedText.isEmpty()) return 0

                val lastDotIndex = formattedText.lastIndexOf('.')
                val firstDotIndex = formattedText.indexOf('.')

                // Determine the true decimal point index in the formatted string
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