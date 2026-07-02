const fs = require('fs');
let file = fs.readFileSync('app/src/main/java/com/rajeducational/erp/ui/admin/AdminViewStudentsScreen.kt', 'utf8');

file = file.replace(/Text\(name, fontWeight = FontWeight\.SemiBold\)/,
`Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
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

fs.writeFileSync('app/src/main/java/com/rajeducational/erp/ui/admin/AdminViewStudentsScreen.kt', file);
