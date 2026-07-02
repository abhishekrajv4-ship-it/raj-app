const fs = require('fs');

function cleanFile(filename) {
    let content = fs.readFileSync(filename, 'utf8');
    
    // Remove suspend fun uploadBitmapToFirebase
    content = content.replace(/suspend fun uploadBitmapToFirebase[\s\S]*/, '');
    
    fs.writeFileSync(filename, content);
    console.log("Cleaned " + filename);
}

cleanFile('app/src/main/java/com/rajeducational/erp/ui/face/FaceAttendanceScreen.kt');
cleanFile('app/src/main/java/com/rajeducational/erp/ui/face/FaceAttendanceTeacherStaffScreen.kt');

