package ir.hesabyar.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
    @Query("SELECT * FROM transactions WHERE reviewStatus = 'CONFIRMED' ORDER BY occurredAt DESC")
    fun observeConfirmedTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE reviewStatus = 'PENDING' ORDER BY occurredAt DESC")
    fun observePendingTransactions(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(entity: TransactionEntity): Long

    @Query("UPDATE transactions SET reviewStatus = :status WHERE id = :id")
    suspend fun updateReviewStatus(id: Long, status: ReviewStatus)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransaction(id: Long)

    @Query("SELECT * FROM installments ORDER BY active DESC, firstDueAt ASC")
    fun observeInstallments(): Flow<List<InstallmentEntity>>

    @Insert
    suspend fun insertInstallment(entity: InstallmentEntity): Long

    @Update
    suspend fun updateInstallment(entity: InstallmentEntity)
}

@Database(
    entities = [TransactionEntity::class, InstallmentEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(DbConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "hesabyar.db"
            ).build()
    }
}
