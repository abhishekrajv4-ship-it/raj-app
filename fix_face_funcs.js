const fs = require('fs');

function removeFuncs(filename) {
    let file = fs.readFileSync(filename, 'utf8');
    
    // Replace the duplicated functions
    file = file.replace(/suspend fun uploadBitmapToFirebase[\s\S]*/, '');
    
    fs.writeFileSync(filename, file);
    console.log("Removed duplicated funcs from " + filename);
}

removeFuncs('app/src/main/java/com/rajeducational/erp/ui/face/FaceAttendanceScreen.kt');
removeFuncs('app/src/main/java/com/rajeducational/erp/ui/face/FaceAttendanceTeacherStaffScreen.kt');

