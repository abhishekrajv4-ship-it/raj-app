const fs = require('fs');

function addPermission(filename) {
    let file = fs.readFileSync(filename, 'utf8');
    
    // Add import
    if (!file.includes('com.google.accompanist.permissions.rememberPermissionState')) {
        file = file.replace('import androidx.compose.runtime.*', 'import androidx.compose.runtime.*\nimport com.google.accompanist.permissions.ExperimentalPermissionsApi\nimport com.google.accompanist.permissions.isGranted\nimport com.google.accompanist.permissions.rememberPermissionState');
    }
    
    // Add annotation
    if (!file.includes('@kotlin.OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)')) {
        file = file.replace('@kotlin.OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)', '@kotlin.OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)');
    }
    if (!file.includes('@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)')) {
        file = file.replace('@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)', '@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)');
    }
    
    // Add permission state and effect
    const searchString = `    val context = LocalContext.current`;
    const replaceString = `    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }`;
    
    if (!file.includes('val cameraPermissionState')) {
        file = file.replace(searchString, replaceString);
        fs.writeFileSync(filename, file);
        console.log("Patched permissions in " + filename);
    }
}

addPermission('app/src/main/java/com/rajeducational/erp/ui/face/FaceAttendanceScreen.kt');
addPermission('app/src/main/java/com/rajeducational/erp/ui/face/FaceAttendanceTeacherStaffScreen.kt');

