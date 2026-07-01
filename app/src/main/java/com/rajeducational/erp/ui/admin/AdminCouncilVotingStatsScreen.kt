package com.rajeducational.erp.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
fun AdminCouncilVotingStatsScreen(navController: NavController) {
    var candidates by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    val firestore = FirebaseFirestore.getInstance()

    LaunchedEffect(Unit) {
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
                title = { Text("Voting Statistics", fontWeight = FontWeight.Bold, color = Color.White) },
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
            val positions = candidates.mapNotNull { it["designation"] as? String }.distinct()
            
            if (positions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No voting statistics available.", color = AppColors.TextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(positions) { pos ->
                        Text(
                            pos,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = AppColors.Navy,
                            modifier = Modifier.padding(bottom = 8.dp, top = if (positions.indexOf(pos) == 0) 0.dp else 16.dp)
                        )
                        
                        val posCandidates = candidates.filter { it["designation"] == pos }.sortedByDescending { (it["votes"] as? Number)?.toInt() ?: 0 }
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                posCandidates.forEachIndexed { index, candidate ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            candidate["name"] as? String ?: "",
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 16.sp,
                                            color = AppColors.TextPrimary
                                        )
                                        
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = AppColors.Background),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                "${candidate["votes"] ?: 0} Votes",
                                                fontWeight = FontWeight.Bold,
                                                color = AppColors.Admin,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                    if (index < posCandidates.size - 1) {
                                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
