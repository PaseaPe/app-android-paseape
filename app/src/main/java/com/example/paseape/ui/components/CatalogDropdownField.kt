package com.example.paseape.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Combo genérico para catálogos del backend.
 *
 * Se reutiliza en el registro, en el formulario de mascotas y en el perfil del paseador
 * para que todos los selectores tengan el mismo comportamiento visual.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> CatalogDropdownField(
    label: String,
    items: List<T>,
    selectedItem: T?,
    itemLabel: (T) -> String,
    placeholder: String,
    enabled: Boolean,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: (@Composable () -> Unit)? = null,
    errorText: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val menuEnabled = enabled && items.isNotEmpty()

    ExposedDropdownMenuBox(
        expanded = expanded && menuEnabled,
        onExpandedChange = { if (menuEnabled) expanded = it },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedItem?.let(itemLabel) ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text(placeholder, fontSize = 13.sp, color = Color.Gray) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && menuEnabled)
            },
            supportingText = if (errorText != null) {
                { Text(text = errorText, fontSize = 11.sp, color = Color(0xFFB3261E)) }
            } else {
                supportingText
            },
            isError = errorText != null,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = menuEnabled),
            shape = RoundedCornerShape(12.dp),
            enabled = enabled,
            colors = OutlinedTextFieldDefaults.colors()
        )

        ExposedDropdownMenu(
            expanded = expanded && menuEnabled,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White)
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = itemLabel(item),
                            fontSize = 14.sp,
                            color = Color(0xFF1E293B)
                        )
                    },
                    onClick = {
                        onItemSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}
