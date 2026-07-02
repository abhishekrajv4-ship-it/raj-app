const fs = require('fs');
let file = fs.readFileSync('app/src/main/java/com/rajeducational/erp/ui/student/NonAttendingStudentAuthScreen.kt', 'utf8');

file = file.replace(/putString\("student_session", doc\.getString\("session"\)\)/,
`putString("student_session", doc.getString("session"))
                                                        putBoolean("is_attending", false)`);

fs.writeFileSync('app/src/main/java/com/rajeducational/erp/ui/student/NonAttendingStudentAuthScreen.kt', file);
