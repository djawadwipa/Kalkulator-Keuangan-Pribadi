package id.djawadwipa.kalkulatorkeuangan.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import id.djawadwipa.kalkulatorkeuangan.data.EncryptedBackupCodec
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun DataBackupRouteScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val factory = remember(context) { DataBackupViewModel.Factory(context) }
    val viewModel: DataBackupViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingPlainImport by remember { mutableStateOf<Uri?>(null) }
    var pendingEncryptedImport by remember { mutableStateOf<Uri?>(null) }
    var exportPassword by rememberSaveable { mutableStateOf<String?>(null) }
    var passwordMode by rememberSaveable { mutableStateOf<PasswordMode?>(null) }

    val exportPlainLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.sqlite3"),
    ) { uri -> uri?.let(viewModel::exportPlain) }
    val importPlainLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> pendingPlainImport = uri }
    val exportEncryptedLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        val password = exportPassword
        exportPassword = null
        if (uri != null && password != null) viewModel.exportEncrypted(uri, password)
    }
    val importEncryptedLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            pendingEncryptedImport = uri
            passwordMode = PasswordMode.IMPORT
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            TextButton(onClick = onBack) { Text("‹ Kembali ke Lainnya") }
            Text(
                "Ekspor, impor, dan backup",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Semua operasi menggunakan pemilih dokumen Android dan tidak membutuhkan permission penyimpanan umum.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        state.message?.let { message ->
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(message, modifier = Modifier.weight(1f))
                        TextButton(onClick = viewModel::clearMessage) { Text("Tutup") }
                    }
                }
            }
        }
        item {
            SectionTitle("Backup terenkripsi")
            MasterDataRow(
                title = "Format .kkpbak",
                subtitle = "AES-256-GCM, PBKDF2, salt dan nonce acak. Password tidak pernah disimpan.",
            )
            Button(
                onClick = { passwordMode = PasswordMode.EXPORT },
                enabled = !state.isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Buat backup terenkripsi")
            }
            OutlinedButton(
                onClick = {
                    importEncryptedLauncher.launch(arrayOf("application/octet-stream", "*/*"))
                },
                enabled = !state.isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Pulihkan backup terenkripsi")
            }
        }
        item {
            SectionTitle("Portabilitas database")
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text("Perhatian", fontWeight = FontWeight.Bold)
                    Text(
                        "File .kkpdb tidak terenkripsi dan dapat memuat data keuangan pribadi. Simpan hanya di lokasi tepercaya.",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            OutlinedButton(
                onClick = { exportPlainLauncher.launch(exportName("kkpdb")) },
                enabled = !state.isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Ekspor database tanpa enkripsi")
            }
            OutlinedButton(
                onClick = {
                    importPlainLauncher.launch(arrayOf("application/vnd.sqlite3", "application/octet-stream", "*/*"))
                },
                enabled = !state.isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Impor database")
            }
        }
        item {
            SectionTitle("Perlindungan impor")
            Text(
                "Aplikasi memeriksa header SQLite, ukuran file, dan versi database sebelum mengganti data. Database lama disimpan sementara sebagai file pemulihan internal sampai impor berikutnya.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Text(
                if (state.isBusy) "Sedang memproses dokumen…" else "Jangan tutup aplikasi ketika proses impor sedang berjalan.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (pendingPlainImport != null) {
        AlertDialog(
            onDismissRequest = { pendingPlainImport = null },
            title = { Text("Ganti seluruh data lokal?") },
            text = {
                Text("Impor akan mengganti database aktif. Pastikan Anda memiliki backup terbaru sebelum melanjutkan.")
            },
            confirmButton = {
                Button(onClick = {
                    val uri = requireNotNull(pendingPlainImport)
                    pendingPlainImport = null
                    viewModel.importPlain(uri)
                }) { Text("Impor dan ganti data") }
            },
            dismissButton = {
                TextButton(onClick = { pendingPlainImport = null }) { Text("Batal") }
            },
        )
    }

    passwordMode?.let { mode ->
        BackupPasswordDialog(
            mode = mode,
            onDismiss = {
                passwordMode = null
                pendingEncryptedImport = null
            },
            onConfirm = { password ->
                passwordMode = null
                when (mode) {
                    PasswordMode.EXPORT -> {
                        exportPassword = password
                        exportEncryptedLauncher.launch(exportName("kkpbak"))
                    }
                    PasswordMode.IMPORT -> {
                        val uri = requireNotNull(pendingEncryptedImport)
                        pendingEncryptedImport = null
                        viewModel.importEncrypted(uri, password)
                    }
                }
            },
        )
    }

    if (state.restartRequired) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Impor berhasil") },
            text = { Text("Aplikasi perlu dimulai ulang agar seluruh layar membaca database yang baru.") },
            confirmButton = {
                Button(onClick = { viewModel.restartApplication(context) }) {
                    Text("Mulai ulang sekarang")
                }
            },
        )
    }
}

private enum class PasswordMode { EXPORT, IMPORT }

@Composable
private fun BackupPasswordDialog(
    mode: PasswordMode,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var password by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var showConfirmation by rememberSaveable { mutableStateOf(false) }
    val valid = password.length >= EncryptedBackupCodec.MIN_PASSWORD_LENGTH &&
        (mode == PasswordMode.IMPORT || password == confirmation)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (mode == PasswordMode.EXPORT) "Password backup" else "Buka backup terenkripsi")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (mode == PasswordMode.EXPORT) {
                        "Gunakan minimal ${EncryptedBackupCodec.MIN_PASSWORD_LENGTH} karakter. Password tidak dapat dipulihkan."
                    } else {
                        "Masukkan password yang dipakai saat backup dibuat."
                    },
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it.take(128) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Password") },
                    visualTransformation = if (showPassword) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) {
                                    Icons.Filled.VisibilityOff
                                } else {
                                    Icons.Filled.Visibility
                                },
                                contentDescription = if (showPassword) {
                                    "Sembunyikan password"
                                } else {
                                    "Tampilkan password"
                                },
                            )
                        }
                    },
                    singleLine = true,
                )
                if (mode == PasswordMode.EXPORT) {
                    OutlinedTextField(
                        value = confirmation,
                        onValueChange = { confirmation = it.take(128) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Ulangi password") },
                        visualTransformation = if (showConfirmation) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    showConfirmation = !showConfirmation
                                },
                            ) {
                                Icon(
                                    imageVector = if (showConfirmation) {
                                        Icons.Filled.VisibilityOff
                                    } else {
                                        Icons.Filled.Visibility
                                    },
                                    contentDescription = if (showConfirmation) {
                                        "Sembunyikan ulangi password"
                                    } else {
                                        "Tampilkan ulangi password"
                                    },
                                )
                            }
                        },
                        singleLine = true,
                        supportingText = {
                            if (confirmation.isNotEmpty() && confirmation != password) {
                                Text("Password tidak sama")
                            }
                        },
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = valid,
                onClick = {
                    val value = password
                    password = ""
                    confirmation = ""
                    onConfirm(value)
                },
            ) {
                Text(if (mode == PasswordMode.EXPORT) "Pilih lokasi" else "Pulihkan")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

private fun exportName(extension: String): String {
    val timestamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
    return "kalkulator-keuangan-$timestamp.$extension"
}
