const fs = require('fs');
let file = fs.readFileSync('app/src/main/java/com/rajeducational/erp/ui/teacher/TeacherScreens.kt', 'utf8');

// Add import
if (!file.includes('import kotlinx.coroutines.tasks.await')) {
    file = file.replace('import androidx.compose.ui.unit.sp', 'import androidx.compose.ui.unit.sp\nimport kotlinx.coroutines.tasks.await');
}

// Add variables to TeacherMoreScreen
const searchStr = `fun TeacherMoreScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("TeacherPrefs", android.content.Context.MODE_PRIVATE) }
    val teacherId = prefs.getString("teacher_id", "") ?: ""
    var unreadCount by remember { mutableStateOf(0) }
    var showQrDialog by remember { mutableStateOf(false) }
    var qrContentString by remember { mutableStateOf("") }
    var qrBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var secondsRemaining by remember { mutableIntStateOf(15) }`;

const replaceStr = `fun TeacherMoreScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("TeacherPrefs", android.content.Context.MODE_PRIVATE) }
    val teacherId = prefs.getString("teacher_id", "") ?: ""
    var unreadCount by remember { mutableStateOf(0) }
    var showQrDialog by remember { mutableStateOf(false) }
    var qrContentString by remember { mutableStateOf("") }
    var qrBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var secondsRemaining by remember { mutableIntStateOf(15) }
    var expandedApprovals by remember { mutableStateOf(false) }
    var expandedAnnouncements by remember { mutableStateOf(false) }
    var expandedAdminFeatures by remember { mutableStateOf(false) }
    var adminFeatures by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }

    LaunchedEffect(teacherId) {
        if (teacherId.isNotEmpty()) {
            try {
                val doc = com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("teachers").document(teacherId).get().await()
                val features = doc.get("adminFeatures") as? Map<String, Boolean> ?: emptyMap()
                adminFeatures = features
            } catch(e: Exception) {}
        }
    }`;

file = file.replace(searchStr, replaceStr);

// Clean up await syntax
file = file.replace(/kotlinx\.coroutines\.tasks\.await\(com\.google\.firebase\.firestore\.FirebaseFirestore\.getInstance\(\)\.collection\("teachers"\)\.document\(teacherId\)\.get\(\)\)/g, 
    'com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("teachers").document(teacherId).get().await()');

fs.writeFileSync('app/src/main/java/com/rajeducational/erp/ui/teacher/TeacherScreens.kt', file);

// Same for StaffScreens
let staffFile = fs.readFileSync('app/src/main/java/com/rajeducational/erp/ui/staff/StaffScreens.kt', 'utf8');

if (!staffFile.includes('import kotlinx.coroutines.tasks.await')) {
    staffFile = staffFile.replace('import androidx.compose.ui.unit.sp', 'import androidx.compose.ui.unit.sp\nimport kotlinx.coroutines.tasks.await');
}

const staffSearchStr = `fun StaffMoreScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("StaffPrefs", android.content.Context.MODE_PRIVATE) }
    val staffId = prefs.getString("staff_id", "") ?: ""
    var unreadCount by remember { mutableStateOf(0) }
    var showQrDialog by remember { mutableStateOf(false) }
    var qrContentString by remember { mutableStateOf("") }
    var qrBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var secondsRemaining by remember { mutableIntStateOf(15) }`;

const staffReplaceStr = `fun StaffMoreScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("StaffPrefs", android.content.Context.MODE_PRIVATE) }
    val staffId = prefs.getString("staff_id", "") ?: ""
    var unreadCount by remember { mutableStateOf(0) }
    var showQrDialog by remember { mutableStateOf(false) }
    var qrContentString by remember { mutableStateOf("") }
    var qrBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var secondsRemaining by remember { mutableIntStateOf(15) }
    var expandedAdminFeatures by remember { mutableStateOf(false) }
    var adminFeatures by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }

    LaunchedEffect(staffId) {
        if (staffId.isNotEmpty()) {
            try {
                val doc = com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("staffs").document(staffId).get().await()
                val features = doc.get("adminFeatures") as? Map<String, Boolean> ?: emptyMap()
                adminFeatures = features
            } catch(e: Exception) {}
        }
    }`;

staffFile = staffFile.replace(staffSearchStr, staffReplaceStr);

staffFile = staffFile.replace(/kotlinx\.coroutines\.tasks\.await\(com\.google\.firebase\.firestore\.FirebaseFirestore\.getInstance\(\)\.collection\("staffs"\)\.document\(staffId\)\.get\(\)\)/g, 
    'com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("staffs").document(staffId).get().await()');

fs.writeFileSync('app/src/main/java/com/rajeducational/erp/ui/staff/StaffScreens.kt', staffFile);
