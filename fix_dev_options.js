const fs = require('fs');

let file = fs.readFileSync('app/src/main/java/com/rajeducational/erp/ui/admin/DeveloperOptionsScreen.kt', 'utf8');

file = file.replace(/val snapshot = firestore\.collection\("attendance"\)\.get\(\)\.await\(\)\s+for \(doc in snapshot\.documents\) \{\s+val role = doc\.getString\("role"\)\s+if \(role == "Student" \|\| role == null\) \{\s+doc\.reference\.delete\(\)\.await\(\)\s+\}\s+\}/g,
`val snapshot = firestore.collection("attendance").get().await()
                                        val batch = firestore.batch()
                                        var count = 0
                                        for (doc in snapshot.documents) {
                                            val role = doc.getString("role")
                                            if (role == "Student" || role == null) {
                                                batch.delete(doc.reference)
                                                count++
                                                if (count >= 400) {
                                                    batch.commit().await()
                                                    count = 0
                                                }
                                            }
                                        }
                                        if (count > 0) batch.commit().await()`);


file = file.replace(/val snapshot = firestore\.collection\("attendance"\)\.get\(\)\.await\(\)\s+for \(doc in snapshot\.documents\) \{\s+val role = doc\.getString\("role"\)\s+if \(role == "Teacher" \|\| role == "Staff"\) \{\s+doc\.reference\.delete\(\)\.await\(\)\s+\}\s+\}/g,
`val snapshot = firestore.collection("attendance").get().await()
                                        val batch = firestore.batch()
                                        var count = 0
                                        for (doc in snapshot.documents) {
                                            val role = doc.getString("role")
                                            if (role == "Teacher" || role == "Staff") {
                                                batch.delete(doc.reference)
                                                count++
                                                if (count >= 400) {
                                                    batch.commit().await()
                                                    count = 0
                                                }
                                            }
                                        }
                                        if (count > 0) batch.commit().await()`);

file = file.replace(/val snapshot = firestore\.collection\(col\)\.get\(\)\.await\(\)\s+for \(doc in snapshot\.documents\) \{ doc\.reference\.delete\(\)\.await\(\) \}/g,
`val snapshot = firestore.collection(col).get().await()
                                            val batch = firestore.batch()
                                            var count = 0
                                            for (doc in snapshot.documents) {
                                                batch.delete(doc.reference)
                                                count++
                                                if (count >= 400) {
                                                    batch.commit().await()
                                                    count = 0
                                                }
                                            }
                                            if (count > 0) batch.commit().await()`);

fs.writeFileSync('app/src/main/java/com/rajeducational/erp/ui/admin/DeveloperOptionsScreen.kt', file);
