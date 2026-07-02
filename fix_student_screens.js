const fs = require('fs');

// Fix StudentScreens.kt
let sFile = fs.readFileSync('app/src/main/java/com/rajeducational/erp/ui/student/StudentScreens.kt', 'utf8');

const themeFunc = `
@Composable
fun getStudentThemeColor(): androidx.compose.ui.graphics.Color {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPrefs = androidx.compose.runtime.remember { context.getSharedPreferences("StudentPrefs", android.content.Context.MODE_PRIVATE) }
    val isAttending = sharedPrefs.getBoolean("is_attending", true)
    return if (isAttending) com.rajeducational.erp.theme.AppColors.Student else androidx.compose.ui.graphics.Color.Red
}
`;

sFile = sFile.replace(themeFunc, '');
sFile += themeFunc;

fs.writeFileSync('app/src/main/java/com/rajeducational/erp/ui/student/StudentScreens.kt', sFile);


// Fix StudentDashboardScreen.kt
let dFile = fs.readFileSync('app/src/main/java/com/rajeducational/erp/ui/student/StudentDashboardScreen.kt', 'utf8');

// I will just replace the specific section that is broken.
// The broken section is:
// color = themeColor.copy(alpha = 0.15f) else Color.Red.copy(alpha = 0.15f),
// Let's replace it manually

dFile = dFile.replace(/color = themeColor\.copy\(alpha = 0\.15f\) else Color\.Red\.copy\(alpha = 0\.15f\),/g, 'color = themeColor.copy(alpha = 0.15f),');

dFile = dFile.replace(/contentColor = themeColor else Color\.Red,/g, 'contentColor = themeColor,');

fs.writeFileSync('app/src/main/java/com/rajeducational/erp/ui/student/StudentDashboardScreen.kt', dFile);
