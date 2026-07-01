package com.rajeducational.erp.data

data class Announcement(
    val id: String = "",
    val subject: String = "",
    val description: String = "",
    val attachmentUrl: String = "",
    val url: String = "",
    val timestamp: Long = 0L,
    val senderName: String = "",
    val senderRole: String = "",
    val isLocal: Boolean = false,
    val targetCollege: String = "",
    val targetCourse: String = "",
    val targetSession: String = ""
)
