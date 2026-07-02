const fs = require('fs');

let sFile = fs.readFileSync('app/src/main/java/com/rajeducational/erp/ui/student/StudentScreens.kt', 'utf8');

// Remove all getStudentThemeColor functions
sFile = sFile.replace(/@Composable\s*fun getStudentThemeColor\(\)[\s\S]*?\n\}/g, '');

const themeFunc = `
@Composable
fun getStudentThemeColor(): androidx.compose.ui.graphics.Color {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPrefs = androidx.compose.runtime.remember { context.getSharedPreferences("StudentPrefs", android.content.Context.MODE_PRIVATE) }
    val isAttending = sharedPrefs.getBoolean("is_attending", true)
    return if (isAttending) com.rajeducational.erp.theme.AppColors.Student else androidx.compose.ui.graphics.Color.Red
}
`;

sFile += themeFunc;

// Restore missing AppColors imports if needed, though fully qualifying was used.
// Wait! The errors were "Unresolved reference 'AppColors'".
// Let's add back the import if missing.
if (!sFile.includes('import com.rajeducational.erp.theme.AppColors')) {
    sFile = sFile.replace('import androidx.compose.ui.unit.sp', 'import androidx.compose.ui.unit.sp\nimport com.rajeducational.erp.theme.AppColors');
}

fs.writeFileSync('app/src/main/java/com/rajeducational/erp/ui/student/StudentScreens.kt', sFile);
