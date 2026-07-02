const fs = require('fs');
let file = fs.readFileSync('app/src/main/java/com/rajeducational/erp/ui/staff/StaffScreens.kt', 'utf8');

file = file.replace(/val doc = com\.google\.firebase\.firestore\.FirebaseFirestore\.getInstance\(\)\.collection\("staffs"\)\.document\(staffId\)\.get\(\)\.kotlinx\.coroutines\.tasks\.await\(\)/g,
`val doc = kotlinx.coroutines.tasks.await(com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("staffs").document(staffId).get())`);

fs.writeFileSync('app/src/main/java/com/rajeducational/erp/ui/staff/StaffScreens.kt', file);
