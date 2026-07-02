package com.rajeducational.erp.utils

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

object HolidayHelper {
    
    data class HolidayCheckResult(
        val isHoliday: Boolean,
        val holidayName: String = ""
    )

    suspend fun checkHolidayForStudent(
        college: String,
        course: String,
        batch: String
    ): HolidayCheckResult {
        try {
            // Get today's date in Indian timezone format dd/MM/yyyy
            val tz = TimeZone.getTimeZone("Asia/Kolkata")
            val cal = Calendar.getInstance(tz)
            if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                return HolidayCheckResult(true, "Sunday")
            }
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            sdf.timeZone = tz
            val todayStr = sdf.format(Date())

            val db = FirebaseFirestore.getInstance()
            val snapshot = db.collection("holidays")
                .whereEqualTo("date", todayStr)
                .get().await()

            for (doc in snapshot.documents) {
                val isAll = doc.getBoolean("isAll") ?: false
                val name = doc.getString("name") ?: "Holiday"
                if (isAll) {
                    return HolidayCheckResult(true, name)
                }

                // Check specific matching
                val selectedColleges = doc.get("selectedColleges") as? List<*>
                val selectedCourses = doc.get("selectedCourses") as? List<*>
                val selectedBatches = doc.get("selectedBatches") as? List<*>

                val collegeMatch = selectedColleges == null || selectedColleges.isEmpty() || selectedColleges.any { it.toString().equals(college, ignoreCase = true) }
                val courseMatch = selectedCourses == null || selectedCourses.isEmpty() || selectedCourses.any { it.toString().equals(course, ignoreCase = true) }
                val batchMatch = selectedBatches == null || selectedBatches.isEmpty() || selectedBatches.any { it.toString().equals(batch, ignoreCase = true) }

                if (collegeMatch && courseMatch && batchMatch) {
                    return HolidayCheckResult(true, name)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return HolidayCheckResult(false)
    }

    suspend fun checkHolidayForTeacherStaff(
        college: String
    ): HolidayCheckResult {
        try {
            val tz = TimeZone.getTimeZone("Asia/Kolkata")
            val cal = Calendar.getInstance(tz)
            if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                return HolidayCheckResult(true, "Sunday")
            }
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            sdf.timeZone = tz
            val todayStr = sdf.format(Date())

            val db = FirebaseFirestore.getInstance()
            val snapshot = db.collection("holidays")
                .whereEqualTo("date", todayStr)
                .get().await()

            for (doc in snapshot.documents) {
                val isAll = doc.getBoolean("isAll") ?: false
                val name = doc.getString("name") ?: "Holiday"
                if (isAll) {
                    return HolidayCheckResult(true, name)
                }

                val selectedColleges = doc.get("selectedColleges") as? List<*>
                val collegeMatch = selectedColleges == null || selectedColleges.isEmpty() || selectedColleges.any { it.toString().equals(college, ignoreCase = true) }

                if (collegeMatch) {
                    return HolidayCheckResult(true, name)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return HolidayCheckResult(false)
    }

    suspend fun checkHolidayByStudentId(studentId: String): HolidayCheckResult {
        try {
            val db = FirebaseFirestore.getInstance()
            val studentDoc = db.collection("students").document(studentId).get().await()
            if (studentDoc.exists()) {
                val college = studentDoc.getString("college") ?: ""
                val course = studentDoc.getString("course") ?: ""
                val session = studentDoc.getString("session") ?: ""
                return checkHolidayForStudent(college, course, session)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return HolidayCheckResult(false)
    }

    suspend fun checkHolidayByTeacherStaffId(userId: String, role: String): HolidayCheckResult {
        try {
            val db = FirebaseFirestore.getInstance()
            val collectionName = if (role.equals("Teacher", ignoreCase = true)) "teachers" else "staffs"
            val doc = db.collection(collectionName).document(userId).get().await()
            if (doc.exists()) {
                val college = doc.getString("collegeName") ?: ""
                return checkHolidayForTeacherStaff(college)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return HolidayCheckResult(false)
    }

    fun isHolidayForStudent(
        dateStr: String,
        college: String,
        course: String,
        batch: String,
        holidayList: List<Map<String, Any?>>
    ): Boolean {
        try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = sdf.parse(dateStr)
            if (date != null) {
                val cal = Calendar.getInstance()
                cal.time = date
                if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) return true
            }
        } catch(e: Exception) {}
        for (h in holidayList) {
            val hDate = h["date"] as? String ?: ""
            if (hDate != dateStr) continue

            val isAll = h["isAll"] as? Boolean ?: false
            if (isAll) return true

            val selectedColleges = h["colleges"] as? List<*>
            val selectedCourses = h["courses"] as? List<*>
            val selectedBatches = h["batches"] as? List<*>

            val collegeMatch = selectedColleges == null || selectedColleges.isEmpty() || selectedColleges.any { it.toString().equals(college, ignoreCase = true) }
            val courseMatch = selectedCourses == null || selectedCourses.isEmpty() || selectedCourses.any { it.toString().equals(course, ignoreCase = true) }
            val batchMatch = selectedBatches == null || selectedBatches.isEmpty() || selectedBatches.any { it.toString().equals(batch, ignoreCase = true) }

            if (collegeMatch && courseMatch && batchMatch) {
                return true
            }
        }
        return false
    }

    fun isHolidayForTeacherStaff(
        dateStr: String,
        college: String,
        holidayList: List<Map<String, Any?>>
    ): Boolean {
        try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = sdf.parse(dateStr)
            if (date != null) {
                val cal = Calendar.getInstance()
                cal.time = date
                if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) return true
            }
        } catch(e: Exception) {}
        for (h in holidayList) {
            val hDate = h["date"] as? String ?: ""
            if (hDate != dateStr) continue

            val isAll = h["isAll"] as? Boolean ?: false
            if (isAll) return true

            val selectedColleges = h["colleges"] as? List<*>
            val collegeMatch = selectedColleges == null || selectedColleges.isEmpty() || selectedColleges.any { it.toString().equals(college, ignoreCase = true) }

            if (collegeMatch) {
                return true
            }
        }
        return false
    }
}
