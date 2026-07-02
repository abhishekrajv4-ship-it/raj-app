const fs = require('fs');
let file = fs.readFileSync('app/src/main/java/com/rajeducational/erp/ui/teacher/TeacherScreens.kt', 'utf8');

file = file.replace(/Text\(\s*text = student\.name,\s*fontWeight = FontWeight\.Bold,\s*fontSize = 15\.sp,\s*color = AppColors\.Navy\s*\)/,
`Text(
    text = student.name,
    fontWeight = FontWeight.Bold,
    fontSize = 15.sp,
    color = AppColors.Navy,
    maxLines = 1,
    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
    modifier = Modifier.weight(1f)
)`);

fs.writeFileSync('app/src/main/java/com/rajeducational/erp/ui/teacher/TeacherScreens.kt', file);
