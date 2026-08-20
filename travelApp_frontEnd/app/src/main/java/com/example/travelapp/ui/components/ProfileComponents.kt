package com.example.travelapp.ui.components

import android.content.Context
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelapp.ui.theme.AccentOrange
import com.example.travelapp.ui.theme.BadgeGrey
import com.example.travelapp.ui.theme.ChevronGrey
import com.example.travelapp.ui.theme.IconGrey
import com.example.travelapp.ui.theme.LogoutBackground
import com.example.travelapp.ui.theme.LogoutRed
import com.example.travelapp.ui.theme.NavSelectedBlue
import com.example.travelapp.ui.theme.SurfaceWhite
import com.example.travelapp.ui.theme.TextPrimary
import com.example.travelapp.ui.theme.TextSecondary
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val HeaderCardShape = RoundedCornerShape(16.dp)
private val RowCardShape = RoundedCornerShape(14.dp)

/** Titolo di una sezione della schermata profilo (es. "Le mie attività"). */
@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary,
        modifier = modifier.padding(start = 4.dp, bottom = 10.dp)
    )
}

/** Icona dentro il badge circolare pastello usato dalle righe di menu. */
@Composable
fun IconBadge(
    icon: ImageVector,
    tint: Color,
    background: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .background(color = background, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Riga cliccabile "badge + testo + chevron": è il mattone comune a tutte le
 * voci di menu, così da non ripetere la stessa Card per ogni voce.
 */
@Composable
fun ProfileMenuRow(
    icon: ImageVector,
    title: String,
    iconTint: Color,
    badgeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ProfileRowCard(modifier = modifier.clickable(onClick = onClick)) {
        IconBadge(icon = icon, tint = iconTint, background = badgeColor)
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = ChevronGrey,
            modifier = Modifier.size(22.dp)
        )
    }
}

/** Riga con interruttore, per le preferenze booleane (es. tema scuro). */
@Composable
fun ProfileSwitchRow(
    icon: ImageVector,
    title: String,
    iconTint: Color,
    badgeColor: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    ProfileRowCard(modifier = modifier) {
        IconBadge(icon = icon, tint = iconTint, background = badgeColor)
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = SurfaceWhite,
                checkedTrackColor = NavSelectedBlue,
                checkedBorderColor = Color.Transparent,
                uncheckedThumbColor = SurfaceWhite,
                uncheckedTrackColor = ChevronGrey,
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

/** Card bianca contenitore, condivisa da tutte le righe di lista. */
@Composable
private fun ProfileRowCard(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Card(
        shape = RowCardShape,
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            content = content
        )
    }
}

/**
 * Intestazione del profilo: avatar, nome, email e bottone per aggiungere la
 * foto profilo.
 *
 * [avatarUrl] è l'URI della foto scelta dall'utente: viene decodificato in
 * locale, così il modulo non dipende da una libreria di image loading. Quando
 * Coil sarà disponibile basterà sostituire l'avatar con
 * `AsyncImage(model = avatarUrl)` per supportare anche gli URL remoti.
 */
@Composable
fun ProfileHeaderCard(
    name: String,
    email: String,
    avatarUrl: String?,
    onAddProfilePhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = HeaderCardShape,
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            ProfileAvatar(avatarUrl = avatarUrl)
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = email,
                fontSize = 13.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAddProfilePhoto,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentOrange,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = ProfileIcons.AddAPhoto,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Aggiungi foto profilo",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ProfileAvatar(
    avatarUrl: String?,
    modifier: Modifier = Modifier
) {
    val photo = rememberAvatarBitmap(avatarUrl)

    Box(
        modifier = modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(color = BadgeGrey, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (photo != null) {
            Image(
                bitmap = photo,
                contentDescription = "Foto del profilo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Immagine del profilo",
                tint = IconGrey,
                modifier = Modifier.size(44.dp)
            )
        }
    }
}

/**
 * Decodifica fuori dal main thread la foto indicata da [avatarUrl], che è
 * l'URI restituito dal selettore di immagini di sistema. Restituisce `null`
 * finché la decodifica non è finita, o se l'URI non è leggibile.
 */
@Composable
private fun rememberAvatarBitmap(avatarUrl: String?): ImageBitmap? {
    val context = LocalContext.current
    var bitmap by remember(avatarUrl) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(avatarUrl) {
        bitmap = avatarUrl?.let { url ->
            withContext(Dispatchers.IO) { decodeLocalImage(context, url) }
        }
    }
    return bitmap
}

private fun decodeLocalImage(context: Context, url: String): ImageBitmap? = runCatching {
    val uri = url.toUri()
    val resolver = context.contentResolver
    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, _, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    } else {
        @Suppress("DEPRECATION")
        MediaStore.Images.Media.getBitmap(resolver, uri)
    }
    bitmap.asImageBitmap()
}.getOrNull()

/** Bottone di logout a larghezza piena, in fondo alla schermata. */
@Composable
fun LogoutButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RowCardShape,
        colors = CardDefaults.cardColors(containerColor = LogoutBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp)
        ) {
            Icon(
                imageVector = ProfileIcons.Logout,
                contentDescription = null,
                tint = LogoutRed,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Logout",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = LogoutRed
            )
        }
    }
}
