const fs = require('fs');
let file = fs.readFileSync('app/src/main/java/com/rajeducational/erp/ui/admin/AdminStudentMessagesScreen.kt', 'utf8');

const searchDataClass = `data class AdminStudentContact(
    val id: String,
    val name: String,
    val college: String,
    val course: String,
    val session: String
)`;

const replaceDataClass = `data class AdminStudentContact(
    val id: String,
    val name: String,
    val college: String,
    val course: String,
    val session: String,
    val isAttending: Boolean = true
)`;

file = file.replace(searchDataClass, replaceDataClass);

const searchCreation = `AdminStudentContact(
                            id = doc.id,
                            name = doc.getString("fullName") ?: "Unknown Student",
                            college = doc.getString("college") ?: "N/A",
                            course = doc.getString("course") ?: "N/A",
                            session = doc.getString("session") ?: "N/A"
                        )`;

const replaceCreation = `AdminStudentContact(
                            id = doc.id,
                            name = doc.getString("fullName") ?: "Unknown Student",
                            college = doc.getString("college") ?: "N/A",
                            course = doc.getString("course") ?: "N/A",
                            session = doc.getString("session") ?: "N/A",
                            isAttending = doc.getBoolean("isAttending") ?: true
                        )`;

file = file.replace(searchCreation, replaceCreation);

fs.writeFileSync('app/src/main/java/com/rajeducational/erp/ui/admin/AdminStudentMessagesScreen.kt', file);
