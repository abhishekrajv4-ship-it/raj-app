const fs = require('fs');

let regCode = fs.readFileSync('app/src/main/java/com/rajeducational/erp/ui/face/FaceRegistrationScreen.kt', 'utf8');

let attendanceCode = regCode.replace('fun FaceRegistrationScreen(navController: NavController, userType: String, userId: String)', 'fun FaceAttendanceScreen(navController: NavController, userType: String)');
// In AppNavigation: com.rajeducational.erp.ui.face.FaceAttendanceScreen(navController, type)

attendanceCode = attendanceCode.replace(/val collection = if \\(userType == "teacher"\\).*\\s*val url = .*\\s*firestore\\.collection\\(collection\\)\\.document\\(userId\\)\\n\\s*\\.set\\(mapOf\\("faceEmbedding" to url\\), SetOptions\\.merge\\(\\)\\)\\.await\\(\\)/g,
    `val collection = "attendance"
                                        val data = hashMapOf(
                                            "studentId" to "some_student_id",
                                            "userType" to userType,
                                            "timestamp" to System.currentTimeMillis()
                                        )
                                        firestore.collection("attendance").add(data).await()`);

attendanceCode = attendanceCode.replace(/FaceRegState/g, 'FaceAttState');

fs.writeFileSync('app/src/main/java/com/rajeducational/erp/ui/face/FaceAttendanceScreen.kt', attendanceCode);

let teacherStaffCode = regCode.replace('fun FaceRegistrationScreen(navController: NavController, userType: String, userId: String)', 'fun FaceAttendanceTeacherStaffScreen(navController: NavController, userType: String)');

teacherStaffCode = teacherStaffCode.replace(/val collection = if \\(userType == "teacher"\\).*\\s*val url = .*\\s*firestore\\.collection\\(collection\\)\\.document\\(userId\\)\\n\\s*\\.set\\(mapOf\\("faceEmbedding" to url\\), SetOptions\\.merge\\(\\)\\)\\.await\\(\\)/g,
    `val collection = "attendance"
                                        val data = hashMapOf(
                                            "studentId" to "some_id",
                                            "userType" to userType,
                                            "timestamp" to System.currentTimeMillis()
                                        )
                                        firestore.collection("attendance").add(data).await()`);
                                        
teacherStaffCode = teacherStaffCode.replace(/FaceRegState/g, 'FaceAttTSState');

fs.writeFileSync('app/src/main/java/com/rajeducational/erp/ui/face/FaceAttendanceTeacherStaffScreen.kt', teacherStaffCode);

