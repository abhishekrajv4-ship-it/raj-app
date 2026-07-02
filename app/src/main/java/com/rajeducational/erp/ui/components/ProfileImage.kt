package com.rajeducational.erp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

@Composable
fun ProfileImage(urlOrBase64: String, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop) {
    if (urlOrBase64.startsWith("data:image")) {
        var bitmap: android.graphics.Bitmap? = null
        try {
            val base64 = urlOrBase64.substringAfter("base64,")
            val bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
            bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Profile Photo",
                modifier = modifier,
                contentScale = contentScale
            )
            return
        }
    }
    
    AsyncImage(
        model = urlOrBase64,
        contentDescription = "Profile Photo",
        modifier = modifier,
        contentScale = contentScale
    )
}
