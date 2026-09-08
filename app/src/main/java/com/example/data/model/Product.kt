package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    indices = [
        Index(value = ["barcode"], unique = true),
        Index(value = ["category"]),
        Index(value = ["name"])
    ]
)
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val barcode: String,
    val sku: String = "",
    val name: String,
    val category: String = "General",
    val unit: String = "1 Pc",
    val currentMrp: Double,
    val costPrice: Double = 0.0,
    val notes: String = "",
    val imageUri: String? = null,
    val lastUpdated: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING_INSERT,
    val sheetRowId: String = ""
)
