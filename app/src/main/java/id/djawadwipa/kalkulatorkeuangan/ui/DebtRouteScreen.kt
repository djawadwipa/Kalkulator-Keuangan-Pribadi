package id.djawadwipa.kalkulatorkeuangan.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun DebtRouteScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        TextButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        ) {
            Text("← Kembali ke menu Lainnya")
        }
        DebtScreen(modifier = Modifier.weight(1f))
    }
}
