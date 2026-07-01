package com.rajeducational.erp.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import com.rajeducational.erp.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCouncilVotingControlScreen(navController: NavController) {
    var isVotingEnabled by remember { mutableStateOf(false) }
    var candidateName by remember { mutableStateOf("") }
    var candidateDesignation by remember { mutableStateOf("") }
    var candidates by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    val firestore = FirebaseFirestore.getInstance()

    LaunchedEffect(Unit) {
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
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Council Voting Control", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Admin)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Background)
                .padding(padding)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Voting System Enabled", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Switch(
                            checked = isVotingEnabled,
                            onCheckedChange = { checked ->
                                firestore.collection("settings").document("voting")
                                    .set(mapOf("isEnabled" to checked))
                            }
                        )
                    }
                    
                    var showResetDialog by remember { mutableStateOf(false) }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset Voting System", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    
                    if (showResetDialog) {
                        AlertDialog(
                            onDismissRequest = { showResetDialog = false },
                            title = { Text("Reset Voting System") },
                            text = { Text("Are you sure you want to reset the voting system? This will delete all candidates and clear all student votes. This action cannot be undone.") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        firestore.collection("council_candidates").get().addOnSuccessListener { snapshot ->
                                            for (doc in snapshot.documents) {
                                                doc.reference.delete()
                                            }
                                        }
                                        firestore.collection("student_votes").get().addOnSuccessListener { snapshot ->
                                            for (doc in snapshot.documents) {
                                                doc.reference.delete()
                                            }
                                        }
                                        showResetDialog = false
                                    }
                                ) {
                                    Text("Reset", color = Color.Red, fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showResetDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { navController.navigate("admin_council_voting_stats") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Navy),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.List, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View Statistics of the Vote", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Add Candidate", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = candidateName,
                        onValueChange = { candidateName = it },
                        label = { Text("Candidate Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = candidateDesignation,
                        onValueChange = { candidateDesignation = it },
                        label = { Text("Designation") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (candidateName.isNotBlank() && candidateDesignation.isNotBlank()) {
                                firestore.collection("council_candidates").add(
                                    mapOf(
                                        "name" to candidateName,
                                        "designation" to candidateDesignation,
                                        "votes" to 0
                                    )
                                )
                                candidateName = ""
                                candidateDesignation = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Admin),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Add Candidate")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Current Candidates",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = AppColors.Navy,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(candidates) { candidate ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    candidate["name"] as? String ?: "",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    candidate["designation"] as? String ?: "",
                                    color = AppColors.TextSecondary,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Votes: ${candidate["votes"] ?: 0}",
                                    fontWeight = FontWeight.Medium,
                                    color = AppColors.Admin,
                                    fontSize = 14.sp
                                )
                            }
                            IconButton(onClick = {
                                val id = candidate["id"] as? String
                                if (id != null) {
                                    firestore.collection("council_candidates").document(id).delete()
                                }
                            }) {
                                Icon(Icons.Default.Delete, "Delete", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}
