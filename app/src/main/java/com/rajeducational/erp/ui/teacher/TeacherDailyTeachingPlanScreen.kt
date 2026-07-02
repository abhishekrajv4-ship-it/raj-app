package com.rajeducational.erp.ui.teacher

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import com.rajeducational.erp.theme.AppColors
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

data class TeachingPlanRow(
    var time: String = "",
    var className: String = "",
    var topicName: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherDailyTeachingPlanScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("TeacherPrefs", android.content.Context.MODE_PRIVATE) }
    val teacherId = prefs.getString("teacher_id", "") ?: ""
    val teacherName = prefs.getString("teacher_name", "Teacher") ?: "Teacher"
    val teacherCollege = prefs.getString("teacher_college", "") ?: ""

    val firestore = remember { FirebaseFirestore.getInstance() }

    var calendarState by remember { mutableStateOf(Calendar.getInstance()) }
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val dateString = dateFormatter.format(calendarState.time)

    var planRows = remember { mutableStateListOf<TeachingPlanRow>() }
    var isLoading by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // Fetch plans for selected date
    LaunchedEffect(teacherId, dateString) {
        if (teacherId.isNotEmpty()) {
            isLoading = true
            try {
                val docId = "${teacherId}_${dateString.replace("/", "-")}"
                val doc = firestore.collection("daily_teaching_plans").document(docId).get().await()
                planRows.clear()
                if (doc.exists()) {
                    val plansList = doc.get("plans") as? List<Map<String, Any>>
                    if (plansList != null && plansList.isNotEmpty()) {
                        plansList.forEach { item ->
                            planRows.add(
                                TeachingPlanRow(
                                    time = item["time"] as? String ?: "",
                                    className = item["className"] as? String ?: item["class"] as? String ?: "",
                                    topicName = item["topicName"] as? String ?: item["topic"] as? String ?: ""
                                )
                            )
                        }
                    } else {
                        planRows.add(TeachingPlanRow())
                    }
                } else {
                    planRows.add(TeachingPlanRow())
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error loading plan: ${e.message}", Toast.LENGTH_SHORT).show()
                if (planRows.isEmpty()) {
                    planRows.add(TeachingPlanRow())
                }
            } finally {
                isLoading = false
            }
        }
    }

    // Date Picker Dialog Function
    fun showDatePicker() {
        val year = calendarState.get(Calendar.YEAR)
        val month = calendarState.get(Calendar.MONTH)
        val day = calendarState.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(context, { _, selectedYear, selectedMonth, selectedDay ->
            val newCal = Calendar.getInstance()
            newCal.set(Calendar.YEAR, selectedYear)
            newCal.set(Calendar.MONTH, selectedMonth)
            newCal.set(Calendar.DAY_OF_MONTH, selectedDay)
            calendarState = newCal
        }, year, month, day).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Teaching Plan", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Teacher,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(AppColors.Background)
                .padding(16.dp)
        ) {
            // Date Selector Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = {
                        val newCal = calendarState.clone() as Calendar
                        newCal.add(Calendar.DATE, -1)
                        calendarState = newCal
                    }) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Prev Day", tint = AppColors.Teacher)
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showDatePicker() }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Calendar", tint = AppColors.Teacher, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = dateString,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.Navy
                        )
                    }

                    IconButton(onClick = {
                        val newCal = calendarState.clone() as Calendar
                        newCal.add(Calendar.DATE, 1)
                        calendarState = newCal
                    }) {
                        Icon(Icons.Default.ArrowForwardIos, contentDescription = "Next Day", tint = AppColors.Teacher)
                    }
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppColors.Teacher)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(planRows) { index, item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Class Plan #${index + 1}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = AppColors.Teacher
                                    )
                                    if (planRows.size > 1) {
                                        IconButton(onClick = {
                                            planRows.removeAt(index)
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove Plan", tint = Color.Red)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = item.time,
                                    onValueChange = {
                                        planRows[index] = planRows[index].copy(time = it)
                                    },
                                    label = { Text("Time (e.g. 10:00 AM - 11:00 AM)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = "Time") }
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = item.className,
                                    onValueChange = {
                                        planRows[index] = planRows[index].copy(className = it)
                                    },
                                    label = { Text("Class (e.g. B.Sc Nursing 1st Year)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = { Icon(Icons.Default.Class, contentDescription = "Class") }
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = item.topicName,
                                    onValueChange = {
                                        planRows[index] = planRows[index].copy(topicName = it)
                                    },
                                    label = { Text("Topic Name (e.g. Anatomy of Heart)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = { Icon(Icons.Default.Book, contentDescription = "Topic") }
                                )
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                planRows.add(TeachingPlanRow())
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Teacher.copy(alpha = 0.15f), contentColor = AppColors.Teacher),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Row")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Class Plan Row", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        scope.launch {
                            isSaving = true
                            try {
                                val docId = "${teacherId}_${dateString.replace("/", "-")}"
                                val data = mapOf(
                                    "teacherId" to teacherId,
                                    "teacherName" to teacherName,
                                    "collegeName" to teacherCollege,
                                    "date" to dateString,
                                    "plans" to planRows.map {
                                        mapOf(
                                            "time" to it.time,
                                            "className" to it.className,
                                            "topicName" to it.topicName
                                        )
                                    },
                                    "timestamp" to System.currentTimeMillis()
                                )
                                firestore.collection("daily_teaching_plans").document(docId).set(data).await()
                                Toast.makeText(context, "Teaching Plan saved successfully!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Teacher),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Daily Teaching Plan", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
