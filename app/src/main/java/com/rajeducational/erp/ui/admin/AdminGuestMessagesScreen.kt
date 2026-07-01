package com.rajeducational.erp.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
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
import com.google.firebase.firestore.Query
import com.rajeducational.erp.data.GuestMessage
import com.rajeducational.erp.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminGuestMessagesScreen(navController: NavController) {
    var messages by remember { mutableStateOf<List<GuestMessage>>(emptyList()) }
    val firestore = FirebaseFirestore.getInstance()

    LaunchedEffect(Unit) {
        firestore.collection("guest_messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val msgs = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(GuestMessage::class.java)?.copy(id = doc.id)
                    }
                    messages = msgs
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Guest Messages", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Admin)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(AppColors.Background)
        ) {
            if (messages.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No guest messages available.", color = AppColors.TextSecondary)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                    items(messages) { message ->
                        GuestMessageCard(message = message, firestore = firestore)
                    }
                }
            }
        }
    }
}

@Composable
fun GuestMessageCard(message: GuestMessage, firestore: FirebaseFirestore) {
    var replyText by remember { mutableStateOf(message.reply) }
    var isReplying by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                if (!message.readByAdmin) {
                    firestore.collection("guest_messages").document(message.id).update("readByAdmin", true)
                }
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("From: ${message.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AppColors.Navy, modifier = Modifier.weight(1f))
                if (!message.readByAdmin) {
                    Box(modifier = Modifier.size(10.dp).background(Color.Red, androidx.compose.foundation.shape.CircleShape))
                }
            }
            if (message.phone.isNotBlank()) {
                Text("Phone: ${message.phone}", fontSize = 14.sp, color = AppColors.TextSecondary)
            }
            if (message.email.isNotBlank()) {
                Text("Email: ${message.email}", fontSize = 14.sp, color = AppColors.TextSecondary)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Message:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = AppColors.Navy)
            Text(if (message.message.isNotBlank()) message.message else "(No text content)", fontSize = 14.sp, color = AppColors.TextPrimary)

            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))

            if (message.reply.isNotBlank()) {
                Text("Admin Reply:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AppColors.Admin)
                Text(message.reply, fontSize = 14.sp, color = AppColors.TextPrimary)
            } else {
                if (isReplying) {
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        label = { Text("Write your reply") },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        maxLines = 4
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { isReplying = false }) {
                            Text("Cancel", color = AppColors.TextSecondary)
                        }
                        Button(
                            onClick = {
                                if (replyText.isNotBlank()) {
                                    isSending = true
                                    firestore.collection("guest_messages").document(message.id)
                                        .update(
                                            "reply", replyText,
                                            "readByGuest", false
                                        )
                                        .addOnCompleteListener {
                                            isSending = false
                                            isReplying = false
                                        }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Admin),
                            enabled = !isSending
                        ) {
                            Text(if (isSending) "Sending..." else "Send Reply")
                        }
                    }
                } else {
                    Button(
                        onClick = { isReplying = true },
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Admin)
                    ) {
                        Text("Reply to Guest")
                    }
                }
            }
        }
    }
}
