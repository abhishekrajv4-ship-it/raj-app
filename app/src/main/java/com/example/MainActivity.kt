package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.google.firebase.firestore.FirebaseFirestore
import com.rajeducational.erp.navigation.AppNavigation
import com.rajeducational.erp.theme.ERPTheme

import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ERPTheme {
                val context = LocalContext.current
                val prefs = context.getSharedPreferences("AdminPrefs", Context.MODE_PRIVATE)
                val firestore = FirebaseFirestore.getInstance()
                var isMaintenance by remember { mutableStateOf(false) }
                var maintenanceMessage by remember { mutableStateOf("") }
                var maintenanceUrl by remember { mutableStateOf("") }
                
                var bypassMaintenance by remember { mutableStateOf(false) }
                var showAdminPasswordDialog by remember { mutableStateOf(false) }
                var passwordInput by remember { mutableStateOf("") }

                LaunchedEffect(Unit) {
                    firestore.collection("settings").document("maintenance")
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) return@addSnapshotListener
                            if (snapshot != null && snapshot.exists()) {
                                isMaintenance = snapshot.getBoolean("enabled") ?: false
                                maintenanceMessage = snapshot.getString("message") ?: ""
                                maintenanceUrl = snapshot.getString("url") ?: ""
                            }
                        }
                }
                
                if (showAdminPasswordDialog) {
                    AlertDialog(
                        onDismissRequest = { showAdminPasswordDialog = false },
                        title = { Text("Super Admin Login") },
                        text = {
                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                label = { Text("Password") },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        confirmButton = {
                            Button(onClick = {
                                val currentPass = prefs.getString("admin_password", "2311")
                                if (passwordInput == currentPass) {
                                    bypassMaintenance = true
                                    showAdminPasswordDialog = false
                                } else {
                                    Toast.makeText(context, "Incorrect password", Toast.LENGTH_SHORT).show()
                                }
                            }) { Text("Login") }
                        },
                        dismissButton = {
                            Button(onClick = { showAdminPasswordDialog = false }) { Text("Cancel") }
                        }
                    )
                }

                if (isMaintenance && !bypassMaintenance) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFFFEBEE))
                    ) {
                        // Settings Button at Top Right
                        IconButton(
                            onClick = { 
                                passwordInput = ""
                                showAdminPasswordDialog = true 
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                                .padding(top = 24.dp)
                        ) {
                            Icon(Icons.Default.Settings, "Settings", tint = Color.Gray)
                        }
                        
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                modifier = Modifier.padding(24.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFD32F2F)),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Maintenance",
                                        tint = Color.White,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Maintenance Mode",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = maintenanceMessage.ifBlank { "The application is currently undergoing maintenance. Please check back later." },
                                        fontSize = 16.sp,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                    
                                    if (maintenanceUrl.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(24.dp))
                                        Text(text = "Redirect URL: $maintenanceUrl", color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                try {
                                                    var url = maintenanceUrl
                                                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                                        url = "http://$url"
                                                    }
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                    startActivity(intent)
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                                        ) {
                                            Text("More Information", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    AppNavigation(startDestination = "landing")
                }
            }
        }
    }
}
