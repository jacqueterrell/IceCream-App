package com.icecreamapp.sweethearts.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Formats phone as XXX-XXX-XXXX (dash after 3rd and 6th digit).
 */
class PhoneNumberVisualTransformation : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text.filter { it.isDigit() }.take(10)
        val formatted = buildString {
            digits.forEachIndexed { index, c ->
                if (index == 3 || index == 6) append('-')
                append(c)
            }
        }
        return TransformedText(
            AnnotatedString(formatted),
            PhoneOffsetMapping(digits.length),
        )
    }
}

private class PhoneOffsetMapping(private val digitCount: Int) : OffsetMapping {

    override fun originalToTransformed(offset: Int): Int {
        if (offset <= 3) return offset
        if (offset <= 6) return offset + 1
        return minOf(offset + 2, digitCount + 2)
    }

    override fun transformedToOriginal(offset: Int): Int {
        if (offset <= 3) return offset
        if (offset <= 6) return offset - 1  // after first dash
        return minOf(offset - 2, digitCount)
    }
}
