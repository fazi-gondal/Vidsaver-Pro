package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.MediaFormat
import com.example.model.ResolvedVideoInfo
import com.example.viewmodel.VidSaverViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: VidSaverViewModel,
    onNavigateToQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val inputUrl by viewModel.inputUrl.collectAsState()
    val isResolving by viewModel.isResolving.collectAsState()
    val resolvedInfo by viewModel.resolvedVideoInfo.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Clipboard fetch helper safely retrieved
    val clipboardManager = remember {
        try {
            context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        } catch (e: Exception) {
            null
        }
    }

    LaunchedEffect(key1 = true) {
        // Auto-paste logic if enabled
        if (viewModel.settingsAutoPaste.value && clipboardManager != null) {
            try {
                if (clipboardManager.hasPrimaryClip()) {
                    val clipData = clipboardManager.primaryClip
                    if (clipData != null && clipData.itemCount > 0) {
                        val clipText = clipData.getItemAt(0).text?.toString() ?: ""
                        val isSocialUrl = clipText.contains("tiktok.com") || 
                                          clipText.contains("instagram.com") || 
                                          clipText.contains("twitter.com") || 
                                          clipText.contains("x.com") || 
                                          clipText.contains("facebook.com")
                        
                        if (isSocialUrl && clipText != inputUrl) {
                            viewModel.onUrlInputChanged(clipText)
                            Toast.makeText(context, "URL detected on clipboard & pasted!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            HeaderSection()
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Paste & Resolve Link",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = inputUrl,
                        onValueChange = { viewModel.onUrlInputChanged(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("https://www.instagram.com/p/...") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (inputUrl.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onUrlInputChanged("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        try {
                                            if (clipboardManager != null && clipboardManager.hasPrimaryClip()) {
                                                val clipData = clipboardManager.primaryClip
                                                if (clipData != null && clipData.itemCount > 0) {
                                                    val text = clipData.getItemAt(0).text?.toString() ?: ""
                                                    if (text.isNotBlank()) {
                                                        viewModel.onUrlInputChanged(text)
                                                        Toast.makeText(context, "Clipboard contents pasted", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                                                    }
                                                } else {
                                                    Toast.makeText(context, "No clipboard data available", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                Toast.makeText(context, "Clipboard is empty or unsupported", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            Toast.makeText(context, "Failed to read clipboard: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )

                    Button(
                        onClick = { viewModel.resolveUrl() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = !isResolving && inputUrl.isNotBlank(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (isResolving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Fetching Video Data...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.CloudDownload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Resolve Media Link", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }

        item {
            errorMessage?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(it, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 14.sp)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            PlatformShortcuts { url ->
                viewModel.onUrlInputChanged(url)
                viewModel.resolveUrl()
            }
        }

        // Animated expansion of metadata preview once resolved successfully
        item {
            AnimatedVisibility(
                visible = resolvedInfo != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                resolvedInfo?.let { info ->
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        VideoMetadataCard(info)
                    }
                }
            }
        }

        // Display Available Formats if resolved info is present
        resolvedInfo?.let { info ->
            item {
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "SELECT DOWNLOAD FORMAT",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(info.formats) { format ->
                FormatOptionRow(format) {
                    viewModel.startFormatDownload(format)
                    Toast.makeText(context, "Added to batch downloads!", Toast.LENGTH_SHORT).show()
                    onNavigateToQueue()
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "VidSaver for Android v1.1.0",
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Made with ❤️ by Fazi Gondal",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun HeaderSection() {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFE2C55), // Pink
                                Color(0xFF1877F2), // Blue
                                Color(0xFF25F4EE)  // Teal
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CloudDownload,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "VidSaver",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Batch Social Media Video Resolver",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PlatformShortcuts(onUrlSelected: (String) -> Unit) {
    Column {
        Text(
            text = "Supported Services Shortcuts",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val context = LocalContext.current
            val shortcuts = listOf(
                ShortcutItem("TikTok", "https://www.tiktok.com/@discover_tok/video/73910293", Color(0xFF000000), Icons.Default.MusicNote),
                ShortcutItem("Instagram", "https://www.instagram.com/reel/C8jF92M_ar7/", Color(0xFFE1306C), Icons.Default.CameraAlt),
                ShortcutItem("Twitter", "https://x.com/tech_guru/status/17812901", Color(0xFF1DA1F2), Icons.Default.Tag),
                ShortcutItem("Facebook", "https://www.facebook.com/watch/?v=129381023", Color(0xFF1877F2), Icons.Default.Videocam)
            )

            shortcuts.forEach { item ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(item.color.copy(alpha = 0.15f))
                        .border(1.dp, item.color.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .clickable {
                            onUrlSelected(item.exampleUrl)
                            Toast.makeText(context, "${item.name} simulated clip pasted!", Toast.LENGTH_SHORT).show()
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.name,
                            tint = item.color,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = item.name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

data class ShortcutItem(
    val name: String,
    val exampleUrl: String,
    val color: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun VideoMetadataCard(info: ResolvedVideoInfo) {
    val platformColor = getPlatformColor(info.platform)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, platformColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp)
        ) {
            // Video Thumbnail preview with Coil
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                AsyncImage(
                    model = info.thumbnailUrl,
                    contentDescription = "Thumbnail",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Duration Badge
                info.durationSeconds?.let {
                    val minutes = it / 60
                    val seconds = it % 60
                    val durationStr = String.format("%02d:%02d", minutes, seconds)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(durationStr, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Platform Badge
                Box(
                    modifier = Modifier
                        .background(platformColor.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = info.platform,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = platformColor
                    )
                }

                Text(
                    text = info.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                info.authorName?.let {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = it,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FormatOptionRow(format: MediaFormat, onDownloadClicked: () -> Unit) {
    val isAudio = format.extension == "mp3" || format.extension == "m4a"
    val formatColor = if (isAudio) Color(0xFF8B5CF6) else Color(0xFF10B981)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(formatColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isAudio) Icons.Default.MusicNote else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = formatColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = format.qualityLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = format.extension.uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = formatColor
                        )
                        val sizeMb = format.sizeBytes.toDouble() / (1024 * 1024)
                        Text(
                            text = String.format("%.1f MB", sizeMb),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Button(
                onClick = onDownloadClicked,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = formatColor),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Get", fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

fun getPlatformColor(platform: String): Color {
    return when (platform.uppercase()) {
        "TIKTOK" -> Color(0xFFFE2C55)
        "INSTAGRAM" -> Color(0xFFE1306C)
        "TWITTER" -> Color(0xFF1DA1F2)
        "FACEBOOK" -> Color(0xFF1877F2)
        else -> Color(0xFF6366F1)
    }
}
