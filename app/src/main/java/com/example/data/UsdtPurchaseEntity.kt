package com.example.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "usdt_purchases")
data class UsdtPurchaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: String,
    val itemTitle: String,
    val usdtAmount: String,
    val networkCode: String,
    val txHash: String,
    val merchantAddress: String,
    val status: String, // VERIFIED
    val blockNumber: String,
    val purchasedAtMillis: Long
)

@Dao
interface UsdtPurchaseDao {
    @Query("SELECT * FROM usdt_purchases ORDER BY purchasedAtMillis DESC")
    fun observePurchases(): Flow<List<UsdtPurchaseEntity>>

    @Query("SELECT * FROM usdt_purchases WHERE LOWER(txHash) = LOWER(:txHash) LIMIT 1")
    suspend fun findByTxHash(txHash: String): UsdtPurchaseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: UsdtPurchaseEntity): Long

    @Query("DELETE FROM usdt_purchases")
    suspend fun clearAllPurchases()
}
