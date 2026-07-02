const fs = require('fs');
let file = fs.readFileSync('app/src/main/java/com/rajeducational/erp/ui/student/StudentScreens.kt', 'utf8');

const themeFunc = `
@Composable
fun getStudentThemeColor(): androidx.compose.ui.graphics.Color {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPrefs = androidx.compose.runtime.remember { context.getSharedPreferences("StudentPrefs", android.content.Context.MODE_PRIVATE) }
    val isAttending = sharedPrefs.getBoolean("is_attending", true)
    return if (isAttending) com.rajeducational.erp.theme.AppColors.Student else androidx.compose.ui.graphics.Color.Red
}
`;

file = file.replace('import androidx.compose.ui.unit.sp', 'import androidx.compose.ui.unit.sp\n' + themeFunc);

// Find all functions and insert `val themeColor = getStudentThemeColor()` at the top
// This might be tricky. Let's just replace `AppColors.Student` with `getStudentThemeColor()`
file = file.replace(/AppColors\.Student/g, 'getStudentThemeColor()');

fs.writeFileSync('app/src/main/java/com/rajeducational/erp/ui/student/StudentScreens.kt', file);

// Also patch StudentDashboardScreen
let dashFile = fs.readFileSync('app/src/main/java/com/rajeducational/erp/ui/student/StudentDashboardScreen.kt', 'utf8');
dashFile = dashFile.replace(/val isAttending = studentProfile\?\.get\("isAttending"\) as\? Boolean \?: true\n\s*val themeColor = if \(isAttending\) com\.rajeducational\.erp\.theme\.AppColors\.Student else Color\.Red/,
`val sharedPrefs = context.getSharedPreferences("StudentPrefs", android.content.Context.MODE_PRIVATE)
    val isAttending = sharedPrefs.getBoolean("is_attending", true)
    val themeColor = if (isAttending) com.rajeducational.erp.theme.AppColors.Student else Color.Red`);
dashFile = dashFile.replace(/if \(isAttending\) com\.rajeducational\.erp\.theme\.AppColors\.Student\.copy/g, 'themeColor.copy');
dashFile = dashFile.replace(/if \(isAttending\) com\.rajeducational\.erp\.theme\.AppColors\.Student else Color\.Red/g, 'themeColor');

fs.writeFileSync('app/src/main/java/com/rajeducational/erp/ui/student/StudentDashboardScreen.kt', dashFile);
