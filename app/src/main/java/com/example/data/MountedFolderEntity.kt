package com.example.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "mounted_folders")
data class MountedFolderEntity(
    @PrimaryKey val uriString: String,
    val displayName: String,
    val addedAtMillis: Long
)

@Dao
interface MountedFolderDao {
    @Query("SELECT * FROM mounted_folders ORDER BY addedAtMillis DESC")
    fun observeMountedFolders(): Flow<List<MountedFolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: MountedFolderEntity)

    @Query("DELETE FROM mounted_folders WHERE uriString = :uriString")
    suspend fun deleteFolder(uriString: String)

    @Query("DELETE FROM mounted_folders")
    suspend fun clearAllFolders()
}
