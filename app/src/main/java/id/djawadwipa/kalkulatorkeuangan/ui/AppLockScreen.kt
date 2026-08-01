package id.djawadwipa.kalkulatorkeuangan.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
internal fun AppLockScreen(
    biometricEnabled: Boolean,
    biometricAvailable: Boolean,
    message: String?,
    onVerifyPin: (String) -> Boolean,
    onBiometric: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pin by rememberSaveable { mutableStateOf("") }
    var showPin by rememberSaveable { mutableStateOf(false) }
    var localMessage by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(message) {
        if (!message.isNullOrBlank()) localMessage = message
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Kalkulator Keuangan Pribadi",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Masukkan PIN untuk membuka data keuangan lokal.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = {
                pin = it.filter(Char::isDigit).take(8)
                localMessage = null
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("PIN") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = if (showPin) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(
                    onClick = { showPin = !showPin },
                ) {
                    Icon(
                        imageVector = if (showPin) {
                            Icons.Filled.VisibilityOff
                        } else {
                            Icons.Filled.Visibility
                        },
                        contentDescription = if (showPin) {
                            "Sembunyikan PIN"
                        } else {
                            "Tampilkan PIN"
                        },
                    )
                }
            },
            supportingText = {
                Text("PIN terdiri dari 4 sampai 8 angka")
            },
            isError = localMessage != null,
        )
        localMessage?.let {
            Text(
                text = it,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                if (pin.length !in 4..8) {
                    localMessage = "Masukkan PIN 4 sampai 8 angka"
                } else if (!onVerifyPin(pin)) {
                    localMessage = "PIN salah"
                    pin = ""
                }
            },
            enabled = pin.length in 4..8,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Buka aplikasi")
        }
        if (biometricEnabled) {
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onBiometric,
                enabled = biometricAvailable,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (biometricAvailable) {
                        "Buka dengan biometrik"
                    } else {
                        "Biometrik tidak tersedia"
                    },
                )
            }
        }
    }
}
