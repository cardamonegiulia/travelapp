package com.example.travelapp.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.travelapp.data.remote.ApiClient
import com.example.travelapp.ui.theme.CoverPlaceholderIcon
import com.example.travelapp.ui.theme.TravelBlue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

@Composable
fun AuthedAsyncImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    var bitmap by remember(url) { mutableStateOf<ImageBitmap?>(null) }
    var isLoading by remember(url) { mutableStateOf(url != null) }

    LaunchedEffect(url) {
        if (url.isNullOrBlank()) {
            bitmap = null
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                val req = Request.Builder().url(url).build()
                ApiClient.getHttpClient(context).newCall(req).execute().use { resp ->
                    val body = resp.body
                    if (resp.isSuccessful && body != null) {
                        BitmapFactory.decodeStream(body.byteStream())?.asImageBitmap()
                    } else {
                        null
                    }
                }
            }.getOrNull()
        }
        isLoading = false
    }

    Box(
        modifier = modifier.background(Color(0xFFE2E8F0)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        } else if (isLoading) {
            CircularProgressIndicator(
                color = TravelBlue,
                strokeWidth = 2.dp,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = null,
                tint = CoverPlaceholderIcon,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}