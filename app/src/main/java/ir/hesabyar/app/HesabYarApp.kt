package ir.hesabyar.app

import android.app.Application
import ir.hesabyar.app.data.AppDatabase
import ir.hesabyar.app.data.CryptoManager
import ir.hesabyar.app.data.FinanceRepository

class HesabYarApp : Application() {
    val cryptoManager: CryptoManager by lazy { CryptoManager() }
    val database: AppDatabase by lazy { AppDatabase.build(this) }
    val repository: FinanceRepository by lazy {
        FinanceRepository(database.financeDao(), cryptoManager)
    }
}
