package com.example.practica04

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.practica04.components.*

@Composable
fun FormScreen() {
    var switchState by remember { mutableStateOf(false) }
    var radioState by remember { mutableStateOf("Opción 1") }
    var checkboxState by remember { mutableStateOf(false) }
    var spinnerState by remember { mutableStateOf("Seleccionar Opción") }
    var dateState by remember { mutableStateOf("Seleccionar Fecha") }

    var resumen by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Práctica 4: Componentes Avanzados", fontSize = 22.sp, style = MaterialTheme.typography.headlineMedium)
        HorizontalDivider()

        Text("1. Switch", style = MaterialTheme.typography.titleMedium)
        CustomSwitch(isChecked = switchState, onCheckedChange = { switchState = it })
        HorizontalDivider()

        Text("2. RadioButton", style = MaterialTheme.typography.titleMedium)
        CustomRadioButton(selectedOption = radioState, onOptionSelected = { radioState = it })
        HorizontalDivider()

        Text("3. Checkbox", style = MaterialTheme.typography.titleMedium)
        CustomCheckbox(isChecked = checkboxState, onCheckedChange = { checkboxState = it })
        HorizontalDivider()

        Text("4. Spinner", style = MaterialTheme.typography.titleMedium)
        CustomSpinner(selectedText = spinnerState, onTextSelected = { spinnerState = it })
        HorizontalDivider()

        Text("5. DatePicker", style = MaterialTheme.typography.titleMedium)
        CustomDatePicker(selectedDateText = dateState, onDateSelected = { dateState = it })

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                resumen = """
                    --- Resumen de Datos ---
                    Switch: ${if(switchState) "Activado" else "Desactivado"}
                    Radio: $radioState
                    Términos: ${if(checkboxState) "Aceptados" else "No aceptados"}
                    Spinner: $spinnerState
                    Fecha: $dateState
                """.trimIndent()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Probar Formulario")
        }

        if (resumen.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = resumen,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}