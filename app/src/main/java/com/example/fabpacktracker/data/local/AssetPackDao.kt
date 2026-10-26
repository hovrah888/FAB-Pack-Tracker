package com.example.fabpacktracker.data.local
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// data access object (sql запросы)
@Dao
interface AssetPackDao {

    // получение сущностей по нужным критериям
    @Query("SELECT * FROM asset_packs ORDER BY publishedDate DESC")
    fun getAllPacks(): Flow<List<AssetPack>>

    @Query("SELECT * FROM asset_packs WHERE id = :packId")
    //fun getPackById(packId: Int): Flow<AssetPack?>
    suspend fun getPackById(packId: Int): AssetPack? // для получения одного пака можно обойтись и без Flow, просто работать с локальной формой вручную

    // вставка
    //@Insert(onConflict = OnConflictStrategy.REPLACE)
    @Insert(onConflict = OnConflictStrategy.ABORT) // лучше НЕ вставлять новые данные вместо старых!!!
    suspend fun insertPack(pack: AssetPack): Long

    // обновление
    @Update
    suspend fun updatePack(pack: AssetPack)

    // удаление
    @Delete
    suspend fun deletePack(pack: AssetPack)
}