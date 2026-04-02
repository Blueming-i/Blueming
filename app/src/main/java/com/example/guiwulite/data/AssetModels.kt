package com.example.guiwulite.data

import java.util.UUID

enum class AssetStatus {
    ALL,
    IN_USE,
    STORED,
    RETIRED,
    SOLD
}

enum class SortMode {
    DEFAULT,
    AMOUNT,
    DAYS,
    COUNT,
    PURCHASE_DATE,
    DAILY_COST
}

enum class DateRange {
    WEEK,
    MONTH,
    YEAR,
    ALL
}

data class AssetItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String,
    val price: Double,
    val purchaseDate: Long,
    val warrantyEnabled: Boolean,
    val countByUsage: Boolean,
    val targetDailyEnabled: Boolean,
    val inService: Boolean,
    val extraCost: Double,
    val note: String,
    val imageUri: String = "",
    val iconName: String = "手机",
    val status: AssetStatus = AssetStatus.IN_USE,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun totalAmount(): Double = price + extraCost
}

data class WishItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val targetAmount: Double,
    val fulfilled: Boolean
)
