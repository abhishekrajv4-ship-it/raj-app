const fs = require('fs');

function fix(filename) {
    let file = fs.readFileSync(filename, 'utf8');
    
    // Remove the one I added
    file = file.replace('@kotlin.OptIn(com.google.accompanist.permissions.ExperimentalPermissionsApi::class)\n@OptIn', '@OptIn');
    file = file.replace('@kotlin.OptIn(com.google.accompanist.permissions.ExperimentalPermissionsApi::class)\n@kotlin.OptIn', '@OptIn');
    
    // Make sure they are combined
    const wrong1 = `@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)`;
    if (file.includes(wrong1)) {
        file = file.replace(wrong1, `@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)`);
    }
    
    const wrong2 = `@kotlin.OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@kotlin.OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)`;
    if (file.includes(wrong2)) {
        file = file.replace(wrong2, `@kotlin.OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)`);
    }
    
    // Actually, let's just use regex to remove multiple @OptIn or @kotlin.OptIn
    const lines = file.split('\\n');
    const outLines = [];
    let hasOptIn = false;
    for(let line of lines) {
        if (line.includes('@kotlin.OptIn') || line.includes('@OptIn')) {
            if (!hasOptIn) {
                outLines.push('@kotlin.OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, com.google.accompanist.permissions.ExperimentalPermissionsApi::class)');
                hasOptIn = true;
            }
        } else {
            outLines.push(line);
        }
    }
    
    fs.writeFileSync(filename, outLines.join('\\n'));
}

fix('app/src/main/java/com/rajeducational/erp/ui/face/FaceAttendanceScreen.kt');
fix('app/src/main/java/com/rajeducational/erp/ui/face/FaceAttendanceTeacherStaffScreen.kt');
