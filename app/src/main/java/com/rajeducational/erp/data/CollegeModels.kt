package com.rajeducational.erp.data

data class College(
    val id: String = "",
    val name: String = "",
    val courses: List<Course> = emptyList(),
    val feeStructureText: String = "",
    val feeFiles: List<FeeFile> = emptyList()
)

data class Course(
    val name: String = "",
    val yearBatches: List<String> = emptyList(),
    val feeStructureText: String = "",
    val feeFiles: List<FeeFile> = emptyList()
)

data class FeeFile(
    val id: Int = 0,
    val originalName: String = "",
    val fileType: String = "",
    val viewUrl: String = "",
    val deleteUrl: String = ""
)

data class GalleryPhoto(
    val id: String = "",
    val name: String = "",
    val viewUrl: String = "",
    val deleteUrl: String = "",
    val fileId: Int = 0
)

data class Event(
    val id: String = "",
    val name: String = "",
    val date: String = "",
    val month: String = "",
    val place: String = "",
    val description: String = "",
    val photos: List<GalleryPhoto> = emptyList()
)
