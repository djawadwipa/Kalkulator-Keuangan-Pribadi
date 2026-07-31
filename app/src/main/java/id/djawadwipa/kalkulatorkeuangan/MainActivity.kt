package id.djawadwipa.kalkulatorkeuangan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import id.djawadwipa.kalkulatorkeuangan.ui.FinanceApp
import id.djawadwipa.kalkulatorkeuangan.ui.MainViewModel
import id.djawadwipa.kalkulatorkeuangan.ui.theme.KalkulatorKeuanganTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = (application as FinanceApplication).repository
        setContent {
            val viewModel: MainViewModel = viewModel(
                factory = MainViewModel.Factory(repository),
            )
            KalkulatorKeuanganTheme {
                FinanceApp(viewModel = viewModel)
            }
        }
    }
}
