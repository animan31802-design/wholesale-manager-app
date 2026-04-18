package com.animan.wholesalemanager.data.local

data class Expense(
    val id: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val date: Long = System.currentTimeMillis()
)