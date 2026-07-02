const fs = require('fs');
let file = fs.readFileSync('app/src/main/java/com/rajeducational/erp/ui/admin/DeveloperOptionsScreen.kt', 'utf8');

file = file.replace(/for \(i in 1\.\.15\) \{\s*val docRef = firestore\.collection\("announcements_student"\)\.document\(\)\s*batch\.set\(docRef, mapOf\(\s*"title" to "Dummy Student Announcement \$i",\s*"content" to "This is a testing announcement for students\. Description \$i\.",\s*"timestamp" to System\.currentTimeMillis\(\) - \(i \* 500000\)\s*\)\)\s*\}\s*\/\/ Add Announcements for Teachers\s*for \(i in 1\.\.15\) \{\s*val docRef = firestore\.collection\("announcements_teacher"\)\.document\(\)\s*batch\.set\(docRef, mapOf\(\s*"title" to "Dummy Teacher Announcement \$i",\s*"content" to "This is a testing announcement for teachers\. Description \$i\.",\s*"timestamp" to System\.currentTimeMillis\(\) - \(i \* 500000\)\s*\)\)\s*\}/, 
`for (i in 1..15) {
        val docRef = firestore.collection("announcements").document()
        batch.set(docRef, mapOf(
            "id" to docRef.id,
            "subject" to "Dummy Student Announcement $i",
            "description" to "This is a testing announcement for students. Description $i.",
            "timestamp" to System.currentTimeMillis() - (i * 500000),
            "senderName" to "Admin",
            "senderRole" to "Admin",
            "isLocal" to false
        ))
    }
    
    // Add Announcements for Teachers
    for (i in 1..15) {
        val docRef = firestore.collection("announcements").document()
        batch.set(docRef, mapOf(
            "id" to docRef.id,
            "subject" to "Dummy Teacher Announcement $i",
            "description" to "This is a testing announcement for teachers. Description $i.",
            "timestamp" to System.currentTimeMillis() - (i * 500000),
            "senderName" to "Admin",
            "senderRole" to "Admin",
            "isLocal" to false
        ))
    }`);

file = file.replace(/"announcements_student",\s*"announcements_teacher",/, '"announcements",');

fs.writeFileSync('app/src/main/java/com/rajeducational/erp/ui/admin/DeveloperOptionsScreen.kt', file);
