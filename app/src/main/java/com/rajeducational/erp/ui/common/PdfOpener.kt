package com.rajeducational.erp.ui.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object PdfOpener {
    fun openPdf(context: Context, urlOrUri: String, coroutineScope: CoroutineScope) {
        if (urlOrUri.isBlank()) {
            Toast.makeText(context, "Report link is empty.", Toast.LENGTH_SHORT).show()
            return
        }

        if (urlOrUri.startsWith("content://")) {
            launchViewIntent(context, Uri.parse(urlOrUri))
            return
        }

        if (urlOrUri.startsWith("http://") || urlOrUri.startsWith("https://")) {
            Toast.makeText(context, "Opening report...", Toast.LENGTH_SHORT).show()
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val url = URL(urlOrUri)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 15000
                    connection.readTimeout = 15000
                    connection.connect()

                    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                        val inputStream = connection.inputStream
                        // Create a temporary cache file to store the downloaded PDF safely
                        val fileName = "view_report_${System.currentTimeMillis()}.pdf"
                        val file = File(context.cacheDir, fileName)
                        FileOutputStream(file).use { outputStream ->
                            val buffer = ByteArray(4096)
                            var bytesRead: Int
                            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                outputStream.write(buffer, 0, bytesRead)
                            }
                        }
                        inputStream.close()

                        val localUri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.provider",
                            file
                        )

                        withContext(Dispatchers.Main) {
                            launchViewIntent(context, localUri)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Failed to load PDF (HTTP ${connection.responseCode})", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        // Fallback: try opening directly as web URL in case an external browser/viewer supports it
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlOrUri)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (ex: Exception) {
                            Toast.makeText(context, "No application found to open this report link.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            return
        }

        // Fallback for local files or standard URIs
        try {
            val uri = Uri.parse(urlOrUri)
            launchViewIntent(context, uri)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to parse report URL", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchViewIntent(context: Context, uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Second fallback: if no app can handle application/pdf, try just ACTION_VIEW with the URI
            try {
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (ex: Exception) {
                Toast.makeText(context, "No PDF viewer found. Please install a PDF reader (e.g. Adobe Acrobat).", Toast.LENGTH_LONG).show()
            }
        }
    }
}
