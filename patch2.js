const fs = require('fs');
let file = fs.readFileSync('app/src/main/java/com/rajeducational/erp/ui/teacher/TeacherNonAttendingApprovalsScreen.kt', 'utf8');

const dialogs = `
    studentToApprove?.let { student ->
        AlertDialog(
            onDismissRequest = { studentToApprove = null },
            title = { Text("Accept this student?", fontWeight = FontWeight.Bold, color = AppColors.Navy) },
            text = { Text("Confirm Accept this student.") },
            confirmButton = {
                TextButton(onClick = {
                    coroutineScope.launch {
                        firestore.collection("students").document(student.id)
                            .update("approvalStatus", "approved").await()
                        studentToApprove = null
                    }
                }) {
                    Text("Accept this student", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { studentToApprove = null }) {
                    Text("Cancel", color = AppColors.TextSecondary)
                }
            }
        )
    }

    studentToDecline?.let { student ->
        AlertDialog(
            onDismissRequest = { studentToDecline = null },
            title = { Text("Decline this student?", fontWeight = FontWeight.Bold, color = AppColors.Navy) },
            text = { Text("Confirm Decline this student.") },
            confirmButton = {
                TextButton(onClick = {
                    coroutineScope.launch {
                        firestore.collection("students").document(student.id)
                            .update("approvalStatus", "declined").await()
                        studentToDecline = null
                    }
                }) {
                    Text("Decline this student", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { studentToDecline = null }) {
                    Text("Cancel", color = AppColors.TextSecondary)
                }
            }
        )
    }
`;

file = file.replace('selectedStudent?.let { student ->', dialogs + '\n    selectedStudent?.let { student ->');
fs.writeFileSync('app/src/main/java/com/rajeducational/erp/ui/teacher/TeacherNonAttendingApprovalsScreen.kt', file);

// Also do TeacherAttendingApprovalsScreen.kt
let file2 = fs.readFileSync('app/src/main/java/com/rajeducational/erp/ui/teacher/TeacherAttendingApprovalsScreen.kt', 'utf8');
file2 = file2.replace(/IconButton\(onClick = \{\s*coroutineScope\.launch \{\s*firestore\.collection\("students"\)\.document\(student\.id\)\s*\.update\("approvalStatus", "declined"\)\.await\(\)\s*\}\s*\}\) \{/g, 'IconButton(onClick = { studentToDecline = student }) {');
file2 = file2.replace(/IconButton\(onClick = \{\s*coroutineScope\.launch \{\s*firestore\.collection\("students"\)\.document\(student\.id\)\s*\.update\("approvalStatus", "approved"\)\.await\(\)\s*\}\s*\}\) \{/g, 'IconButton(onClick = { studentToApprove = student }) {');
file2 = file2.replace('selectedStudent?.let { student ->', dialogs + '\n    selectedStudent?.let { student ->');

// Add the variables
file2 = file2.replace('var selectedStudent by remember { mutableStateOf<PendingStudent?>(null) }', 'var selectedStudent by remember { mutableStateOf<PendingStudent?>(null) }\n    var studentToApprove by remember { mutableStateOf<PendingStudent?>(null) }\n    var studentToDecline by remember { mutableStateOf<PendingStudent?>(null) }');

fs.writeFileSync('app/src/main/java/com/rajeducational/erp/ui/teacher/TeacherAttendingApprovalsScreen.kt', file2);

