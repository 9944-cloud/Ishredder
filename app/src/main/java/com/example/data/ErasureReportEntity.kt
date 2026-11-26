package com.example.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "erasure_reports")
data class ErasureReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reportCode: String,
    val timestampMillis: Long,
    val methodId: String,
    val methodName: String,
    val methodTier: String,
    val cycles: Int,
    val itemsShreddedCount: Int,
    val itemsFailedCount: Int,
    val totalBytesShredded: Long,
    val durationMillis: Long,
    val status: String,
    val targetSummary: String,
    val itemsDetailLog: String,
    val verificationSha256: String
)

@Dao
interface ErasureReportDao {
    @Query("SELECT * FROM erasure_reports ORDER BY timestampMillis DESC")
    fun observeAllReports(): Flow<List<ErasureReportEntity>>

    @Query("SELECT * FROM erasure_reports WHERE id = :id LIMIT 1")
    suspend fun getReportById(id: Long): ErasureReportEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ErasureReportEntity): Long

    @Query("DELETE FROM erasure_reports WHERE id = :id")
    suspend fun deleteReportById(id: Long)

    @Query("DELETE FROM erasure_reports")
    suspend fun clearAllReports()
}
