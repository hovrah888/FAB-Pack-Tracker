package com.example.fabpacktracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

// объект для бд
@Entity(tableName = "asset_packs")
data class AssetPack(
    // поля
    @PrimaryKey(autoGenerate = true) val id : Int = 0, // первичный ключ
    val name: String,
    val price: Double,
    // дефолтные з-ния
    val salesCount: Int = 0,
    //val revenue: Double = 0.0,
    val url: String = "",
    val description: String = "",
    val publishedDate: Long = System.currentTimeMillis()
){
    val revenue : Double
        get() = price * salesCount

}



