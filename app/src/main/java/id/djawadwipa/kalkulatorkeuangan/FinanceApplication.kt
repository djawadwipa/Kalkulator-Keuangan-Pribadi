package id.djawadwipa.kalkulatorkeuangan

import android.app.Application
import id.djawadwipa.kalkulatorkeuangan.data.FinanceRepository
import id.djawadwipa.kalkulatorkeuangan.data.local.FinanceDatabaseHelper

class FinanceApplication : Application() {
    lateinit var repository: FinanceRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = FinanceRepository(FinanceDatabaseHelper(this))
    }
}
