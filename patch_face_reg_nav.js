const fs = require('fs');

let file = fs.readFileSync('app/src/main/java/com/rajeducational/erp/ui/face/FaceRegistrationScreen.kt', 'utf8');

const target = `                            if (userType == "non_attending") {
                                android.widget.Toast.makeText(context, "Registration Submitted. Wait for your approval. And remember your password.", android.widget.Toast.LENGTH_LONG).show()
                                navController.navigate("landing") { popUpTo(0) }
                            } else if (!navController.popBackStack()) {
                                if (userType == "teacher") {
                                    navController.navigate("teacher_announcements") { popUpTo(0) }
                                } else if (userType == "staff") {
                                    navController.navigate("staff_announcements") { popUpTo(0) }
                                } else {
                                    navController.navigate("student_dashboard") { popUpTo(0) }
                                }
                            }`;
                            
const replacement = `                            if (userType == "non_attending" || userType == "student") {
                                android.widget.Toast.makeText(context, "Registration Submitted. Wait for your approval. And remember your password.", android.widget.Toast.LENGTH_LONG).show()
                                navController.navigate("landing") { popUpTo(0) }
                            } else if (!navController.popBackStack()) {
                                if (userType == "teacher") {
                                    navController.navigate("teacher_announcements") { popUpTo(0) }
                                } else if (userType == "staff") {
                                    navController.navigate("staff_announcements") { popUpTo(0) }
                                } else {
                                    navController.navigate("student_dashboard") { popUpTo(0) }
                                }
                            }`;
                            
if (file.includes(target)) {
    file = file.replace(target, replacement);
    fs.writeFileSync('app/src/main/java/com/rajeducational/erp/ui/face/FaceRegistrationScreen.kt', file);
    console.log("Patched FaceRegistrationScreen");
} else {
    console.log("Could not find target in FaceRegistrationScreen");
}
