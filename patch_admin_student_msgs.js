const fs = require('fs');
let file = fs.readFileSync('app/src/main/java/com/rajeducational/erp/ui/admin/AdminStudentMessagesScreen.kt', 'utf8');

file = file.replace(/Text\(\s*text = student\.name,\s*fontWeight = FontWeight\.Bold,\s*fontSize = 15\.sp,\s*color = AppColors\.Navy\s*\)/,
`Text(
                                                text = student.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = AppColors.Navy,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            val isAttending = student.isAttending
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                color = if (isAttending) AppColors.Student.copy(alpha = 0.1f) else Color.Red.copy(alpha = 0.1f),
                                                contentColor = if (isAttending) AppColors.Student else Color.Red,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = if (isAttending) "Attending" else "Non-attending",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }`);

fs.writeFileSync('app/src/main/java/com/rajeducational/erp/ui/admin/AdminStudentMessagesScreen.kt', file);
