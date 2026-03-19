package com.ovzenproject.voicebridgeclient.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class CustomPasswordVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val mask = "•".repeat(text.length)
        return TransformedText(
            AnnotatedString(mask),
            OffsetMapping.Identity
        )
    }
}