const fs = require('fs');

function addPermission(filename) {
    let file = fs.readFileSync(filename, 'utf8');
    
    // Add OptIn to the composable function
    if (file.includes('fun FaceAttendanceScreen')) {
        file = file.replace('fun FaceAttendanceScreen', '@kotlin.OptIn(com.google.accompanist.permissions.ExperimentalPermissionsApi::class)\nfun FaceAttendanceScreen');
    }
    
    if (file.includes('fun FaceAttendanceTeacherStaffScreen')) {
        file = file.replace('fun FaceAttendanceTeacherStaffScreen', '@kotlin.OptIn(com.google.accompanist.permissions.ExperimentalPermissionsApi::class)\nfun FaceAttendanceTeacherStaffScreen');
    }
    
    fs.writeFileSync(filename, file);
    console.log("Patched permissions 2 in " + filename);
}

addPermission('app/src/main/java/com/rajeducational/erp/ui/face/FaceAttendanceScreen.kt');
addPermission('app/src/main/java/com/rajeducational/erp/ui/face/FaceAttendanceTeacherStaffScreen.kt');

