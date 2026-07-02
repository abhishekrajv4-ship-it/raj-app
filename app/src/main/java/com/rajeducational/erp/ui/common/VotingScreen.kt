package com.rajeducational.erp.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.rajeducational.erp.theme.AppColors
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VotingScreen(
    navController: NavController,
    userType: String // "student", "teacher", "staff"
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefsName = when(userType) {
        "student" -> "StudentPrefs"
        "teacher" -> "TeacherPrefs"
        "staff" -> "StaffPrefs"
        else -> "StudentPrefs"
    }
    val userIdKey = when(userType) {
        "student" -> "student_id"
        "teacher" -> "teacher_id"
        "staff" -> "staff_id"
        else -> "student_id"
    }
    val votesCollection = "${userType}_votes"
    
    val appColor = when(userType) {
        "student" -> AppColors.Student
        "teacher" -> AppColors.Teacher
        "staff" -> AppColors.Staff
        else -> AppColors.Student
    }

    var isVotingEnabled by remember { mutableStateOf(false) }
    var candidates by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    val firestore = FirebaseFirestore.getInstance()
    var votedFor by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var hasVoted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val sharedPrefs = context.getSharedPreferences(prefsName, android.content.Context.MODE_PRIVATE)
        val userId = sharedPrefs.getString(userIdKey, null)
        
        firestore.collection("settings").document("voting")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    isVotingEnabled = snapshot.getBoolean("isEnabled") ?: false
                }
            }

        firestore.collection("council_candidates")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    candidates = snapshot.documents.map { doc ->
                        val data = doc.data?.toMutableMap() ?: mutableMapOf()
                        data["id"] = doc.id
                        data
                    }
                }
            }
            
        if (userId != null) {
            firestore.collection(votesCollection).document(userId)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists()) {
                        hasVoted = true
                    }
                }
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Council Voting") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = appColor, titleContentColor = Color.White, navigationIconContentColor = Color.White)) }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(AppColors.Background).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("Council Elections", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppColors.Navy, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
            
            if (!isVotingEnabled) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Block, "Closed", tint = Color(0xFFFF9800), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Voting is currently closed", fontWeight = FontWeight.Bold, color = AppColors.Navy)
                        Text("Please check back later or contact admin.", fontSize = 14.sp, color = AppColors.TextSecondary, textAlign = TextAlign.Center)
                    }
                }
            } else if (hasVoted) {
                 Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CheckCircle, "Voted", tint = Color(0xFF4CAF50), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("You have already voted!", fontWeight = FontWeight.Bold, color = AppColors.Navy)
                        Text("Thank you for participating in the council elections.", fontSize = 14.sp, color = AppColors.TextSecondary, textAlign = TextAlign.Center)
                    }
                }
            } else {
                Text("Select one candidate per position", fontSize = 13.sp, color = AppColors.TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
                
                val positions = candidates.mapNotNull { it["designation"] as? String }.distinct()
                
                if (positions.isEmpty()) {
                    Text("No candidates available for voting.", color = AppColors.TextSecondary, modifier = Modifier.padding(16.dp))
                }
                
                positions.forEach { pos ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(pos, fontWeight = FontWeight.Bold, color = appColor)
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            val posCandidates = candidates.filter { it["designation"] == pos }
                            posCandidates.forEach { candidate ->
                                val name = candidate["name"] as? String ?: ""
                                val id = candidate["id"] as? String ?: ""
                                val isSelected = votedFor[pos] == id
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable { 
                                        val newVotes = votedFor.toMutableMap()
                                        newVotes[pos] = id
                                        votedFor = newVotes
                                    }.border(1.dp, if (isSelected) appColor else Color(0xFFEEEEEE), RoundedCornerShape(10.dp)).padding(12.dp), 
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = isSelected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = appColor))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) appColor else AppColors.TextPrimary)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }
                }
                
                if (positions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (votedFor.size == positions.size) {
                                val sharedPrefs = context.getSharedPreferences(prefsName, android.content.Context.MODE_PRIVATE)
                                val userId = sharedPrefs.getString(userIdKey, null)
                                if (userId != null) {
                                    firestore.collection(votesCollection).document(userId).set(mapOf("voted" to true))
                                    votedFor.forEach { (_, candidateId) ->
                                        firestore.collection("council_candidates").document(candidateId)
                                            .update("votes", FieldValue.increment(1))
                                    }
                                    hasVoted = true
                                    android.widget.Toast.makeText(context, "Votes submitted successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    android.widget.Toast.makeText(context, "Error: User ID not found. Please relogin.", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                android.widget.Toast.makeText(context, "Please vote for all positions.", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }, 
                        modifier = Modifier.fillMaxWidth(), 
                        colors = ButtonDefaults.buttonColors(containerColor = appColor), 
                        shape = RoundedCornerShape(12.dp)
                    ) { 
                        Text("Submit Votes", modifier = Modifier.padding(8.dp), fontWeight = FontWeight.Bold) 
                    }
                }
            }
        }
    }
}
