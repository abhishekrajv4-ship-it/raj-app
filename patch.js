const fs = require('fs');
let file = fs.readFileSync('app/src/main/java/com/rajeducational/erp/ui/teacher/TeacherNonAttendingApprovalsScreen.kt', 'utf8');
file = file.replace(/IconButton\(onClick = \{\s*coroutineScope\.launch \{\s*firestore\.collection\("students"\)\.document\(student\.id\)\s*\.update\("approvalStatus", "declined"\)\.await\(\)\s*\}\s*\}\) \{/g, 'IconButton(onClick = { studentToDecline = student }) {');
file = file.replace(/IconButton\(onClick = \{\s*coroutineScope\.launch \{\s*firestore\.collection\("students"\)\.document\(student\.id\)\s*\.update\("approvalStatus", "approved"\)\.await\(\)\s*\}\s*\}\) \{/g, 'IconButton(onClick = { studentToApprove = student }) {');
fs.writeFileSync('app/src/main/java/com/rajeducational/erp/ui/teacher/TeacherNonAttendingApprovalsScreen.kt', file);
