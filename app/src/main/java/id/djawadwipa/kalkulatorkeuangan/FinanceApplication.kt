package id.djawadwipa.kalkulatorkeuangan

import android.app.Application
import id.djawadwipa.kalkulatorkeuangan.data.FinanceRepository
import id.djawadwipa.kalkulatorkeuangan.data.local.FinanceDatabase

class FinanceApplication : Application() {
    lateinit var repository: FinanceRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val database = FinanceDatabase.getInstance(this)
        repository = FinanceRepository(database.financeDao())
    }
}
