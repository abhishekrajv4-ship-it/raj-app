const fs = require('fs');
let file = fs.readFileSync('app/src/main/java/com/rajeducational/erp/ui/student/StudentScreens.kt', 'utf8');

const target = `                                Text("Fees Pending", fontWeight = FontWeight.Bold, color = AppColors.Error, fontSize = 18.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(feeReminderText.ifBlank { "Your fees payment is pending" }, color = AppColors.TextPrimary, fontSize = 16.sp)
                    }
                }`;
                
const replacement = `                                Text("Fees Pending", fontWeight = FontWeight.Bold, color = AppColors.Error, fontSize = 18.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(feeReminderText.ifBlank { "Your fees payment is pending" }, color = AppColors.TextPrimary, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        val dateFormat = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
                        val expiryDate = dateFormat.format(java.util.Date(feeReminderExpiry))
                        Text("Deadline: $expiryDate", color = AppColors.Error, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }`;

if (file.includes(target)) {
    file = file.replace(target, replacement);
    fs.writeFileSync('app/src/main/java/com/rajeducational/erp/ui/student/StudentScreens.kt', file);
    console.log("Patched StudentFeesScreen");
} else {
    console.log("Could not find target in StudentFeesScreen");
}
