package com.rajeducational.erp.ui.admin

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import com.rajeducational.erp.data.College
import com.rajeducational.erp.theme.AppColors
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminGiveHolidayScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val firestore = FirebaseFirestore.getInstance()

    var holidayName by remember { mutableStateOf("") }
    var holidayType by remember { mutableStateOf("One Day") }
    var toDate by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf("") }
    var isAllHoliday by remember { mutableStateOf(false) }

    var collegesList by remember { mutableStateOf<List<College>>(emptyList()) }
    var selectedColleges by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedCourses by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedBatches by remember { mutableStateOf<Set<String>>(emptySet()) }

    var declaredHolidays by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoadingColleges by remember { mutableStateOf(true) }
    var isLoadingHolidays by remember { mutableStateOf(true) }

    // Initialize with today's date
    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        selectedDate = sdf.format(Date())
        toDate = sdf.format(Date())
        
        // Load colleges
        try {
            val snapshot = firestore.collection("colleges").get().await()
            collegesList = snapshot.documents.mapNotNull { doc ->
                doc.toObject(College::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error loading colleges: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            isLoadingColleges = false
        }

        // Load declared holidays
        loadHolidays(firestore) { list ->
            declaredHolidays = list
            isLoadingHolidays = false
        }
    }

    // Helper to refresh holidays
    val refreshHolidays = {
        coroutineScope.launch {
            loadHolidays(firestore) { list ->
                declaredHolidays = list
            }
        }
    }

    // Date Picker Dialog
    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            selectedDate = sdf.format(cal.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )
    val toDatePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            toDate = sdf.format(cal.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    // Derived lists based on selected colleges
    val availableCourses = remember(selectedColleges, collegesList) {
        if (isAllHoliday) emptyList() else {
            collegesList.filter { selectedColleges.contains(it.name) }
                .flatMap { it.courses }
                .map { it.name }
                .distinct()
        }
    }

    val availableBatches = remember(selectedColleges, selectedCourses, collegesList) {
        if (isAllHoliday) emptyList() else {
            collegesList.filter { selectedColleges.contains(it.name) }
                .flatMap { it.courses }
                .filter { selectedCourses.contains(it.name) }
                .flatMap { it.yearBatches }
                .distinct()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Give a Holiday", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Admin)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(AppColors.Background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Declare New Holiday",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = AppColors.Navy
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Holiday Title Input
                        OutlinedTextField(
                            value = holidayName,
                            onValueChange = { holidayName = it },
                            label = { Text("Holiday Title / Reason") },
                            placeholder = { Text("e.g. Independence Day, Diwali") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Celebration, contentDescription = "Celebration") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AppColors.Admin,
                                focusedLabelColor = AppColors.Admin
                            ),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Holiday Type Selection
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).clickable { holidayType = "One Day" }) {
                                RadioButton(selected = holidayType == "One Day", onClick = { holidayType = "One Day" }, colors = RadioButtonDefaults.colors(selectedColor = AppColors.Admin))
                                Text("One Day")
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).clickable { holidayType = "Multiple Days" }) {
                                RadioButton(selected = holidayType == "Multiple Days", onClick = { holidayType = "Multiple Days" }, colors = RadioButtonDefaults.colors(selectedColor = AppColors.Admin))
                                Text("Multiple Days")
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        if (holidayType == "One Day") {
                            // Date Selection
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { datePickerDialog.show() }
                                    .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = "Calendar", tint = AppColors.Admin)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Holiday Date", fontSize = 12.sp, color = Color.Gray)
                                        Text(selectedDate, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                                    }
                                }
                                Icon(Icons.Default.Edit, contentDescription = "Edit Date", tint = AppColors.Admin)
                            }
                        } else {
                            // Multiple Days Date Selection
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { datePickerDialog.show() }
                                        .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = "From Date", tint = AppColors.Admin)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("From", fontSize = 12.sp, color = Color.Gray)
                                        Text(selectedDate, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                                    }
                                }
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { toDatePickerDialog.show() }
                                        .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = "To Date", tint = AppColors.Admin)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("To", fontSize = 12.sp, color = Color.Gray)
                                        Text(toDate, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        // Holiday for All Button / Menu option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isAllHoliday) AppColors.Admin.copy(alpha = 0.1f) else Color(0xFFF5F5F5),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { 
                                    isAllHoliday = !isAllHoliday 
                                    if (isAllHoliday) {
                                        selectedColleges = collegesList.map { it.name }.toSet()
                                    } else {
                                        selectedColleges = emptySet()
                                        selectedCourses = emptySet()
                                        selectedBatches = emptySet()
                                    }
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isAllHoliday) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = "Holiday for all",
                                    tint = if (isAllHoliday) AppColors.Admin else Color.Gray
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Holiday for All", fontWeight = FontWeight.Bold, color = if (isAllHoliday) AppColors.Admin else Color.Black)
                                    Text("Declares holiday for all colleges, batches, and courses", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                            Button(
                                onClick = {
                                    isAllHoliday = true
                                    selectedColleges = collegesList.map { it.name }.toSet()
                                    selectedCourses = emptySet()
                                    selectedBatches = emptySet()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Admin),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Select All", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Target Selectors (Colleges, Courses, Batches) if not Holiday for All
            if (!isAllHoliday) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Select Target Colleges", fontWeight = FontWeight.Bold, color = AppColors.Navy)
                                TextButton(onClick = {
                                    selectedColleges = collegesList.map { it.name }.toSet()
                                }) {
                                    Text("Select All", color = AppColors.Admin)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            if (isLoadingColleges) {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                            } else if (collegesList.isEmpty()) {
                                Text("No colleges registered.", color = Color.Gray, modifier = Modifier.padding(8.dp))
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    collegesList.forEach { college ->
                                        val isSelected = selectedColleges.contains(college.name)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedColleges = if (isSelected) {
                                                        selectedColleges - college.name
                                                    } else {
                                                        selectedColleges + college.name
                                                    }
                                                }
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = isSelected,
                                                onCheckedChange = { checked ->
                                                    selectedColleges = if (checked == true) {
                                                        selectedColleges + college.name
                                                    } else {
                                                        selectedColleges - college.name
                                                    }
                                                },
                                                colors = CheckboxDefaults.colors(checkedColor = AppColors.Admin)
                                            )
                                            Text(college.name, fontSize = 14.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Selective Course Targets
                item {
                    AnimatedVisibility(visible = selectedColleges.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Select Target Courses", fontWeight = FontWeight.Bold, color = AppColors.Navy)
                                    TextButton(onClick = {
                                        selectedCourses = availableCourses.toSet()
                                    }) {
                                        Text("Select All", color = AppColors.Admin)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                if (availableCourses.isEmpty()) {
                                    Text("No courses found for selected colleges.", color = Color.Gray, fontSize = 12.sp)
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        availableCourses.forEach { courseName ->
                                            val isSelected = selectedCourses.contains(courseName)
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        selectedCourses = if (isSelected) {
                                                            selectedCourses - courseName
                                                        } else {
                                                            selectedCourses + courseName
                                                        }
                                                    }
                                                    .padding(vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = isSelected,
                                                    onCheckedChange = { checked ->
                                                        selectedCourses = if (checked == true) {
                                                            selectedCourses + courseName
                                                        } else {
                                                            selectedCourses - courseName
                                                        }
                                                    },
                                                    colors = CheckboxDefaults.colors(checkedColor = AppColors.Admin)
                                                )
                                                Text(courseName, fontSize = 14.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Selective Year/Batch Targets
                item {
                    AnimatedVisibility(visible = selectedCourses.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Select Target Batches / Years", fontWeight = FontWeight.Bold, color = AppColors.Navy)
                                    TextButton(onClick = {
                                        selectedBatches = availableBatches.toSet()
                                    }) {
                                        Text("Select All", color = AppColors.Admin)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                if (availableBatches.isEmpty()) {
                                    Text("No batches found for selected courses.", color = Color.Gray, fontSize = 12.sp)
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        availableBatches.forEach { batch ->
                                            val isSelected = selectedBatches.contains(batch)
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        selectedBatches = if (isSelected) {
                                                            selectedBatches - batch
                                                        } else {
                                                            selectedBatches + batch
                                                        }
                                                    }
                                                    .padding(vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = isSelected,
                                                    onCheckedChange = { checked ->
                                                        selectedBatches = if (checked == true) {
                                                            selectedBatches + batch
                                                        } else {
                                                            selectedBatches - batch
                                                        }
                                                    },
                                                    colors = CheckboxDefaults.colors(checkedColor = AppColors.Admin)
                                                )
                                                Text(batch, fontSize = 14.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Submit Button
            item {
                Button(
                    onClick = {
                        if (holidayName.isBlank()) {
                            Toast.makeText(context, "Please enter a holiday title", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (!isAllHoliday && selectedColleges.isEmpty()) {
                            Toast.makeText(context, "Please select at least one college or declare for all", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        coroutineScope.launch {
                            try {
                                val holidayDoc = hashMapOf(
                                    "name" to holidayName.trim(),
                                    "date" to selectedDate,
                                    "isAll" to isAllHoliday,
                                    "selectedColleges" to selectedColleges.toList(),
                                    "selectedCourses" to selectedCourses.toList(),
                                    "selectedBatches" to selectedBatches.toList(),
                                    "timestamp" to System.currentTimeMillis()
                                )

                                firestore.collection("holidays").add(holidayDoc).await()
                                Toast.makeText(context, "Holiday successfully declared!", Toast.LENGTH_SHORT).show()
                                
                                // Reset fields
                                holidayName = ""
                                isAllHoliday = false
                                selectedColleges = emptySet()
                                selectedCourses = emptySet()
                                selectedBatches = emptySet()
                                
                                refreshHolidays()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "Failed to declare holiday: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Admin),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Celebration, contentDescription = "Declare")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Declare Holiday", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Divider
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray)
                Text(
                    text = "Declared Holidays History",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = AppColors.Navy,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Holiday History list
            if (isLoadingHolidays) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AppColors.Admin)
                    }
                }
            } else if (declaredHolidays.isEmpty()) {
                item {
                    Text(
                        text = "No holidays declared yet.",
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(24.dp)
                    )
                }
            } else {
                items(declaredHolidays) { holiday ->
                    val id = holiday["id"] as? String ?: ""
                    val name = holiday["name"] as? String ?: "Holiday"
                    val date = holiday["date"] as? String ?: ""
                    val isAll = holiday["isAll"] as? Boolean ?: false
                    val colleges = holiday["selectedColleges"] as? List<*> ?: emptyList<Any>()

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AppColors.Navy)
                                Text("Date: $date", fontSize = 14.sp, color = Color.Gray)
                                Text(
                                    text = if (isAll) "Applies to: ALL" else "Applies to: ${colleges.joinToString(", ")}",
                                    fontSize = 12.sp,
                                    color = AppColors.Admin,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        try {
                                            firestore.collection("holidays").document(id).delete().await()
                                            Toast.makeText(context, "Holiday removed successfully", Toast.LENGTH_SHORT).show()
                                            refreshHolidays()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error deleting: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Holiday", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}

private suspend fun loadHolidays(firestore: FirebaseFirestore, onLoaded: (List<Map<String, Any>>) -> Unit) {
    try {
        val snapshot = firestore.collection("holidays")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get().await()
        
        val list = snapshot.documents.mapNotNull { doc ->
            val data = doc.data
            if (data != null) {
                data["id"] = doc.id
                data
            } else null
        }
        onLoaded(list)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
