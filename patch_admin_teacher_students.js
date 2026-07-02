const fs = require('fs');
let file = fs.readFileSync('app/src/main/java/com/rajeducational/erp/ui/admin/AdminTeacherStudentsScreen.kt', 'utf8');

file = file.replace(/Text\(\s*\(student\["fullName"\] as\? String\) \?: "Unknown",\s*fontWeight = FontWeight\.Bold,\s*fontSize = 18\.sp,\s*color = AppColors\.Navy\s*\)/,
`Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        (student["fullName"] as? String) ?: "Unknown",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = AppColors.Navy,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    val isAttending = student["isAttending"] as? Boolean ?: true
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
                                    }
                                }`);

fs.writeFileSync('app/src/main/java/com/rajeducational/erp/ui/admin/AdminTeacherStudentsScreen.kt', file);
