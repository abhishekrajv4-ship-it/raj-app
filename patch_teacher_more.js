const fs = require('fs');
let file = fs.readFileSync('app/src/main/java/com/rajeducational/erp/ui/teacher/TeacherScreens.kt', 'utf8');

const oldCard = `            Card(
                modifier = Modifier.fillMaxWidth().clickable { navController.navigate("teacher_student_qr_registration") },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.QrCode, "Student Registration", tint = AppColors.Teacher)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Student Registration Through QR Code", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, "Go", tint = AppColors.TextSecondary)
                }
            }`;

const newCards = `            Card(
                modifier = Modifier.fillMaxWidth().clickable { navController.navigate("teacher_attendance_control_center") },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.QrCodeScanner, "Students Attendance", tint = AppColors.Teacher)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Students Attendance Through QR Code", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, "Go", tint = AppColors.TextSecondary)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth().clickable { navController.navigate("teacher_attendance_report_control") },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PictureAsPdf, "Attendance Report Generation", tint = AppColors.Teacher)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Attendance Report Generation", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, "Go", tint = AppColors.TextSecondary)
                }
            }`;

if (file.includes(oldCard)) {
    file = file.replace(oldCard, newCards);
    fs.writeFileSync('app/src/main/java/com/rajeducational/erp/ui/teacher/TeacherScreens.kt', file);
    console.log("TeacherScreens patched!");
} else {
    console.log("Could not find the card to replace.");
}
