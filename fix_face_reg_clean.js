const fs = require('fs');
let content = fs.readFileSync('app/src/main/java/com/rajeducational/erp/ui/face/FaceRegistrationScreen.kt', 'utf8');

// There might be duplicates within FaceRegistrationScreen.kt as well
// Let's ensure only one declaration exists for uploadBitmapToFirebase, rotateBitmap, flipBitmapHorizontal
const idx = content.indexOf('suspend fun uploadBitmapToFirebase');
if (idx !== -1) {
    const firstPart = content.substring(0, idx);
    let secondPart = content.substring(idx);
    
    // In secondPart, we'll keep the first occurrence of each function, but let's just replace all occurrences except the first
    // Actually, maybe I appended them multiple times in previous tasks when I created the file.
    // Let's just find the very first declaration of uploadBitmapToFirebase and keep everything up to the next suspend fun uploadBitmapToFirebase
    const idx2 = secondPart.indexOf('suspend fun uploadBitmapToFirebase', 10);
    if (idx2 !== -1) {
        secondPart = secondPart.substring(0, idx2);
    }
    fs.writeFileSync('app/src/main/java/com/rajeducational/erp/ui/face/FaceRegistrationScreen.kt', firstPart + secondPart);
    console.log("Cleaned FaceRegistrationScreen.kt duplicates");
}
