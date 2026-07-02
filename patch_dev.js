const fs = require('fs');
let file = fs.readFileSync('app/src/main/java/com/rajeducational/erp/ui/admin/DeveloperOptionsScreen.kt', 'utf8');

file = file.replace(/val snapshot = firestore\.collection\("attendance"\)\.whereEqualTo\("role", "Student"\)\.get\(\)\.await\(\)\s*for \(doc in snapshot\.documents\) \{\s*doc\.reference\.delete\(\)\.await\(\)\s*\}/, 
`val snapshot = firestore.collection("attendance").get().await()
                                        for (doc in snapshot.documents) {
                                            val role = doc.getString("role")
                                            if (role == "Student" || role == null) {
                                                doc.reference.delete().await()
                                            }
                                        }`);

file = file.replace(/val teacherSnapshot = firestore\.collection\("attendance"\)\.whereEqualTo\("role", "Teacher"\)\.get\(\)\.await\(\)\s*for \(doc in teacherSnapshot\.documents\) \{ doc\.reference\.delete\(\)\.await\(\) \}\s*val staffSnapshot = firestore\.collection\("attendance"\)\.whereEqualTo\("role", "Staff"\)\.get\(\)\.await\(\)\s*for \(doc in staffSnapshot\.documents\) \{ doc\.reference\.delete\(\)\.await\(\) \}/, 
`val snapshot = firestore.collection("attendance").get().await()
                                        for (doc in snapshot.documents) {
                                            val role = doc.getString("role")
                                            if (role == "Teacher" || role == "Staff") {
                                                doc.reference.delete().await()
                                            }
                                        }`);

file = file.replace(/val collections = listOf\("announcements_student", "announcements_teacher", "announcements_staff", "announcements_local"\)/, `val collections = listOf("announcements")`);

fs.writeFileSync('app/src/main/java/com/rajeducational/erp/ui/admin/DeveloperOptionsScreen.kt', file);
