const fs = require('fs');
let file = fs.readFileSync('app/src/main/java/com/rajeducational/erp/ui/teacher/TeacherLoginScreen.kt', 'utf8');

file = file.replace(
    `putString("teacher_course", doc.getString("course"))`,
    `putString("teacher_course", doc.getString("course") ?: doc.getString("departmentName"))
                                        val courses = doc.get("courses") as? List<String> ?: emptyList()
                                        putStringSet("teacher_courses", courses.toSet())`
);

fs.writeFileSync('app/src/main/java/com/rajeducational/erp/ui/teacher/TeacherLoginScreen.kt', file);
