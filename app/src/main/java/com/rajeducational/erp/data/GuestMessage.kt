package com.rajeducational.erp.data

data class GuestMessage(
    val id: String = "",
    val guestId: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    val reply: String = "",
    val readByAdmin: Boolean = false,
    val readByGuest: Boolean = true
)

