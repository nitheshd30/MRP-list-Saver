package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "mrp_history",
    foreignKeys = [
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["productId"]),
        Index(value = ["barcode"]),
        Index(value = ["changeDate"])
    ]
)
data class MrpHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long,
    val barcode: String,
    val previousMrp: Double,
    val newMrp: Double,
    val changeDate: Long = System.currentTimeMillis(),
    val reason: String = "Price Revision",
    val changedBy: String = "Label Incharge",
    val notes: String = "",
    val syncStatus: SyncStatus = SyncStatus.PENDING_INSERT
)
