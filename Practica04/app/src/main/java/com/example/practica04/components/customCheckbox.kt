package com.example.practica04.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment

@Composable
fun CustomCheckbox(isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = isChecked, onCheckedChange = onCheckedChange)
        Text("Acepto los términos y condiciones")
    }
}