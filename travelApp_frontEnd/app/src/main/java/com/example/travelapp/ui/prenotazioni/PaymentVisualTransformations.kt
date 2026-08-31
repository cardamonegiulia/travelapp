package com.example.travelapp.ui.prenotazioni

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class NumeroCartaVisualTransformation : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val originale = text.text.take(16)

        val formattato = buildString {
            originale.forEachIndexed { index, char ->
                append(char)

                if (
                    (index + 1) % 4 == 0 &&
                    index != originale.lastIndex
                ) {
                    append(" ")
                }
            }
        }

        val offsetMapping = object : OffsetMapping {

            override fun originalToTransformed(offset: Int): Int {
                val spaziPrima = when {
                    offset <= 4 -> 0
                    offset <= 8 -> 1
                    offset <= 12 -> 2
                    else -> 3
                }

                return (offset + spaziPrima)
                    .coerceAtMost(formattato.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                val spaziPrima = when {
                    offset <= 4 -> 0
                    offset <= 9 -> 1
                    offset <= 14 -> 2
                    else -> 3
                }

                return (offset - spaziPrima)
                    .coerceIn(0, originale.length)
            }
        }

        return TransformedText(
            AnnotatedString(formattato),
            offsetMapping
        )
    }
}


class ScadenzaVisualTransformation : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val originale = text.text.take(4)

        val formattato =
            if (originale.length > 2) {
                "${originale.take(2)}/${originale.drop(2)}"
            } else {
                originale
            }

        val offsetMapping = object : OffsetMapping {

            override fun originalToTransformed(offset: Int): Int =
                if (offset <= 2) {
                    offset
                } else {
                    (offset + 1)
                        .coerceAtMost(formattato.length)
                }

            override fun transformedToOriginal(offset: Int): Int =
                if (offset <= 2) {
                    offset
                } else {
                    (offset - 1)
                        .coerceIn(0, originale.length)
                }
        }

        return TransformedText(
            AnnotatedString(formattato),
            offsetMapping
        )
    }
}