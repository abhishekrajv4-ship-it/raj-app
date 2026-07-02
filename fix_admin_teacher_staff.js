const fs = require('fs');

let file = fs.readFileSync('app/src/main/java/com/rajeducational/erp/ui/admin/AdminTeacherStaffAttendanceControlScreen.kt', 'utf8');

// remove import
file = file.replace('import com.rajeducational.erp.ui.face.TeacherStaffData', '');

// add data class
file = file.replace('data class RawRecordTS(', 'data class TeacherStaffData(val id: String, val name: String, val role: String)\n\ndata class RawRecordTS(');

fs.writeFileSync('app/src/main/java/com/rajeducational/erp/ui/admin/AdminTeacherStaffAttendanceControlScreen.kt', file);

