const fs = require('fs');

function fixFile(filename) {
    let file = fs.readFileSync(filename, 'utf8');
    const target = `        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, android.content.IntentFilter(android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE), android.content.Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, android.content.IntentFilter(android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }`;
        
    const replacement = `        androidx.core.content.ContextCompat.registerReceiver(context, receiver, android.content.IntentFilter(android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE), androidx.core.content.ContextCompat.RECEIVER_EXPORTED)`;
    
    if (file.includes(target)) {
        file = file.replace(target, replacement);
        fs.writeFileSync(filename, file);
        console.log("Fixed " + filename);
    }
}

fixFile('app/src/main/java/com/rajeducational/erp/ui/guest/GuestScreens.kt');
fixFile('app/src/main/java/com/rajeducational/erp/ui/teacher/TeacherScreens.kt');
fixFile('app/src/main/java/com/rajeducational/erp/ui/staff/StaffScreens.kt');
fixFile('app/src/main/java/com/rajeducational/erp/ui/student/StudentScreens.kt');
