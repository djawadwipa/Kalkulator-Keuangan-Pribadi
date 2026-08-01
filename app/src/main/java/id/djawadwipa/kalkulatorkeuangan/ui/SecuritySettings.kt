package id.djawadwipa.kalkulatorkeuangan.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
internal fun SecuritySettingsSection(
    hasPin: Boolean,
    biometricEnabled: Boolean,
    biometricAvailable: Boolean,
    onSetPin: (String) -> Unit,
    onClearPin: () -> Unit,
    onVerifyPin: (String) -> Boolean,
    onBiometricEnabledChange: (Boolean) -> Unit,
) {
    var dialogMode by rememberSaveable {
        mutableStateOf<SecurityDialogMode?>(null)
    }

    SectionTitle("Keamanan")
    MasterDataRow(
        title = if (hasPin) "Kunci aplikasi aktif" else "Kunci aplikasi belum aktif",
        subtitle = if (hasPin) {
            "Data keuangan dilindungi dengan PIN 4–8 angka."
        } else {
            "Buat PIN untuk melindungi aplikasi dan mengaktifkan biometrik."
        },
        onClick = {
            dialogMode = if (hasPin) {
                SecurityDialogMode.CHANGE
            } else {
                SecurityDialogMode.CREATE
            }
        },
    )

    if (hasPin) {
        OutlinedButton(
            onClick = { dialogMode = SecurityDialogMode.CHANGE },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Ubah PIN")
        }
        OutlinedButton(
            onClick = { dialogMode = SecurityDialogMode.REMOVE },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Hapus PIN")
        }
    } else {
        Button(
            onClick = { dialogMode = SecurityDialogMode.CREATE },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Buat PIN")
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Buka dengan biometrik", fontWeight = FontWeight.SemiBold)
            Text(
                text = when {
                    !hasPin -> "Buat PIN terlebih dahulu."
                    !biometricAvailable -> "Biometrik kuat belum tersedia di perangkat."
                    else -> "Gunakan sidik jari atau biometrik perangkat."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Switch(
            checked = biometricEnabled,
            onCheckedChange = onBiometricEnabledChange,
            enabled = hasPin && biometricAvailable,
        )
    }

    dialogMode?.let { mode ->
        PinSettingsDialog(
            mode = mode,
            onDismiss = { dialogMode = null },
            onVerifyPin = onVerifyPin,
            onConfirm = { newPin ->
                when (mode) {
                    SecurityDialogMode.CREATE,
                    SecurityDialogMode.CHANGE,
                    -> onSetPin(requireNotNull(newPin))

                    SecurityDialogMode.REMOVE -> onClearPin()
                }
                dialogMode = null
            },
        )
    }
}

private enum class SecurityDialogMode {
    CREATE,
    CHANGE,
    REMOVE,
}

@Composable
private fun PinSettingsDialog(
    mode: SecurityDialogMode,
    onDismiss: () -> Unit,
    onVerifyPin: (String) -> Boolean,
    onConfirm: (String?) -> Unit,
) {
    var currentPin by rememberSaveable { mutableStateOf("") }
    var newPin by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }
    var showCurrentPin by rememberSaveable { mutableStateOf(false) }
    var showNewPin by rememberSaveable { mutableStateOf(false) }
    var showConfirmation by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    val requiresCurrentPin = mode != SecurityDialogMode.CREATE
    val requiresNewPin = mode != SecurityDialogMode.REMOVE
    val newPinValid = newPin.length in 4..8 && newPin.all(Char::isDigit)
    val confirmationValid = newPin == confirmation

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when (mode) {
                    SecurityDialogMode.CREATE -> "Buat PIN"
                    SecurityDialogMode.CHANGE -> "Ubah PIN"
                    SecurityDialogMode.REMOVE -> "Hapus PIN"
                },
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (requiresCurrentPin) {
                    PinField(
                        value = currentPin,
                        onValueChange = {
                            currentPin = it
                            errorMessage = null
                        },
                        label = "PIN saat ini",
                        visible = showCurrentPin,
                        onVisibilityChange = { showCurrentPin = !showCurrentPin },
                    )
                }

                if (requiresNewPin) {
                    PinField(
                        value = newPin,
                        onValueChange = {
                            newPin = it
                            errorMessage = null
                        },
                        label = "PIN baru",
                        visible = showNewPin,
                        onVisibilityChange = { showNewPin = !showNewPin },
                    )
                    PinField(
                        value = confirmation,
                        onValueChange = {
                            confirmation = it
                            errorMessage = null
                        },
                        label = "Ulangi PIN baru",
                        visible = showConfirmation,
                        onVisibilityChange = {
                            showConfirmation = !showConfirmation
                        },
                    )
                }

                Text(
                    text = "PIN harus terdiri dari 4 sampai 8 angka.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )

                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = when (mode) {
                    SecurityDialogMode.CREATE ->
                        newPinValid && confirmationValid

                    SecurityDialogMode.CHANGE ->
                        currentPin.length in 4..8 &&
                            newPinValid &&
                            confirmationValid

                    SecurityDialogMode.REMOVE ->
                        currentPin.length in 4..8
                },
                onClick = {
                    if (requiresCurrentPin && !onVerifyPin(currentPin)) {
                        errorMessage = "PIN saat ini salah"
                        currentPin = ""
                        return@Button
                    }

                    if (requiresNewPin && !confirmationValid) {
                        errorMessage = "PIN baru tidak sama"
                        return@Button
                    }

                    onConfirm(newPin.takeIf { requiresNewPin })
                },
            ) {
                Text(
                    when (mode) {
                        SecurityDialogMode.CREATE -> "Simpan PIN"
                        SecurityDialogMode.CHANGE -> "Ubah PIN"
                        SecurityDialogMode.REMOVE -> "Hapus PIN"
                    },
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        },
    )
}

@Composable
private fun PinField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visible: Boolean,
    onVisibilityChange: () -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = {
            onValueChange(it.filter(Char::isDigit).take(8))
        },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword,
        ),
        visualTransformation = if (visible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingIcon = {
            IconButton(onClick = onVisibilityChange) {
                Icon(
                    imageVector = if (visible) {
                        Icons.Filled.VisibilityOff
                    } else {
                        Icons.Filled.Visibility
                    },
                    contentDescription = if (visible) {
                        "Sembunyikan PIN"
                    } else {
                        "Tampilkan PIN"
                    },
                )
            }
        },
    )
}
