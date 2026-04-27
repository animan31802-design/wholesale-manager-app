package com.animan.wholesalemanager.data.local

import java.util.UUID

data class Supplier(
    val id      : String = UUID.randomUUID().toString(),
    val name    : String = "",
    val phone   : String = "",
    val address : String = "",
    val balance : Double = 0.0,   // amount owed TO supplier (credit purchases)
    val latitude  : Double? = null,
    val longitude : Double? = null
)