package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.DownloadLog
import com.example.viewmodel.VidSaverViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: VidSaverViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val searchQuery by viewModel.historySearchQuery.collectAsState()
    val activePlatformFilter by viewModel.historyPlatformFilter.collectAsState()
    val filteredDownloads by viewModel.filteredDownloads.collectAsState()
    val isConverting by viewModel.isConverting.collectAsState()
    val conversionMessage by viewModel.conversionMessage.collectAsState()

    var activePlaybackUnit by remember { mutableStateOf<DownloadLog?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Saved Media Library",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Offline downloads and format converters",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Search text field
        TextField(
            value = searchQuery,
            onValueChange = { viewModel.historySearchQuery.value = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search by title...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.historySearchQuery.value = "" }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Platform filter row
        PlatformFilterChips(
            selectedFilter = activePlatformFilter,
            onFilterSelected = { viewModel.historyPlatformFilter.value = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Converting progress snack overlay
        AnimatedVisibility(
            visible = isConverting || conversionMessage != null,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            conversionMessage?.let { msg ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .border(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFFC084FC), strokeWidth = 2.dp)
                        Text(text = msg, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (filteredDownloads.isEmpty()) {
            EmptyHistoryView(hasFilters = searchQuery.isNotBlank() || activePlatformFilter != null) {
                // reset filters
                viewModel.historySearchQuery.value = ""
                viewModel.historyPlatformFilter.value = null
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredDownloads, key = { it.id }) { log ->
                    HistoryItemCard(
                        log = log,
                        onPlayClick = { activePlaybackUnit = log },
                        onExtractAudio = {
                            viewModel.convertVideoToAudio(log, "MP3")
                            Toast.makeText(context, "Transcoding conversion triggered...", Toast.LENGTH_SHORT).show()
                        },
                        onDeleteClick = {
                            viewModel.deleteHistoryItem(log.id)
                            Toast.makeText(context, "Removed file record", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    // Active Playback Overlay Dialog
    activePlaybackUnit?.let { log ->
        VideoPlayerPreviewDialog(log = log) {
            activePlaybackUnit = null
        }
    }
}

@Composable
fun PlatformFilterChips(
    selectedFilter: String?,
    onFilterSelected: (String?) -> Unit
) {
    val platforms = listOf(
        FilterChipItem("All", null, MaterialTheme.colorScheme.onSurface),
        FilterChipItem("TikTok", "TIKTOK", Color(0xFFFE2C55)),
        FilterChipItem("Instagram", "INSTAGRAM", Color(0xFFE1306C)),
        FilterChipItem("Twitter", "TWITTER", Color(0xFF1DA1F2)),
        FilterChipItem("Facebook", "FACEBOOK", Color(0xFF1877F2))
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(platforms) { platform ->
            val isSelected = selectedFilter == platform.value
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) platform.accentColor.copy(alpha = 0.25f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                    .border(
                        1.dp,
                        if (isSelected) platform.accentColor else Color.Transparent,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { onFilterSelected(platform.value) }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = platform.label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) platform.accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

data class FilterChipItem(
    val label: String,
    val value: String?,
    val accentColor: Color
)

@Composable
fun EmptyHistoryView(hasFilters: Boolean, onResetClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(40.dp)
                )
            }
            Text(
                text = if (hasFilters) "No Matching Logs" else "Media Library Empty",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (hasFilters) "Try adjusting your search query or platform category filters."
                       else "Your downloaded social clips, shorts, and extracted audios will be accessible offline here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 280.dp)
            )

            if (hasFilters) {
                Button(
                    onClick = onResetClick,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Clear Filter Controls", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun HistoryItemCard(
    log: DownloadLog,
    onPlayClick: () -> Unit,
    onExtractAudio: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val platformColor = getPlatformColor(log.platform)
    val isAudio = log.filePath.endsWith(".mp3")
    val sizeMb = log.fileSize.toDouble() / (1024 * 1024)
    val formattedDate = remember(log.timestamp) {
        val sdf = SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.getDefault())
        sdf.format(Date(log.timestamp))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail / Icon representation
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onPlayClick() }
            ) {
                if (isAudio) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF8B5CF6).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = Color(0xFF8B5CF6)
                        )
                    }
                } else {
                    AsyncImage(
                        model = log.thumbnailUrl,
                        contentDescription = "Cover preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Indicator Play Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = "Play",
                            modifier = Modifier.size(26.dp),
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(platformColor.copy(alpha = 0.15f), RoundedCornerShape(5.dp))
                            .padding(horizontal = 5.dp, vertical = 1.5.dp)
                    ) {
                        Text(
                            text = log.platform,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = platformColor
                        )
                    }
                    Text(
                        text = String.format("%.1f MB", sizeMb),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = log.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = formattedDate,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            // Options buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Audio Extraction button if not already extracted
                if (!isAudio) {
                    IconButton(
                        onClick = onExtractAudio,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF8B5CF6).copy(alpha = 0.1f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Audiotrack,
                            contentDescription = "Convert to Audio MP3",
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF8B5CF6)
                        )
                    }
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete record",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * Built-In Interactive Offline Media Player Simulator Overlay.
 */
@Composable
fun VideoPlayerPreviewDialog(
    log: DownloadLog,
    onDismissRequest: () -> Unit
) {
    val platformColor = getPlatformColor(log.platform)
    var isPlaying by remember { mutableStateOf(true) }
    var playbackProgress by remember { mutableFloatStateOf(0.2f) }
    val duration = log.durationSeconds ?: 45L

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .border(1.dp, platformColor.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(platformColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "VidSaver Media Player Preview",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismissRequest, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close player",
                            modifier = Modifier.size(18.dp),
                            tint = Color.LightGray
                        )
                    }
                }

                // Video screen representation
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1F1F1F))
                ) {
                    AsyncImage(
                        model = log.thumbnailUrl,
                        contentDescription = "Playback canvas",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Video Player Dark Gradient Mask
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                                )
                            )
                    )

                    // Play Pause Indicator trigger overlay
                    IconButton(
                        onClick = { isPlaying = !isPlaying },
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play Control",
                            modifier = Modifier.size(32.dp),
                            tint = Color.White
                        )
                    }

                    // Platform Stamp
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .background(platformColor, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(log.platform, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Metadata Label info
                Text(
                    text = log.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                // Simulating scrubbing timeline
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Slider(
                        value = playbackProgress,
                        onValueChange = { playbackProgress = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = platformColor,
                            activeTrackColor = platformColor,
                            inactiveTrackColor = Color.DarkGray
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val elapsed = (duration * playbackProgress).toLong()
                        val rawElapsedMin = elapsed / 60
                        val rawElapsedSec = elapsed % 60
                        val rawDurMin = duration / 60
                        val rawDurSec = duration % 60

                        Text(
                            text = String.format("%02d:%02d", rawElapsedMin, rawElapsedSec),
                            color = Color.Gray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = String.format("%02d:%02d", rawDurMin, rawDurSec),
                            color = Color.Gray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Interactive Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val context = LocalContext.current
                    Button(
                        onClick = { openMediaFile(context, log) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = platformColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Media", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { shareMediaFile(context, log) },
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // File path detail
                Text(
                    text = "Offline save: ${log.filePath.substringAfterLast("/")}",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

fun openMediaFile(context: Context, log: DownloadLog) {
    try {
        val uri = if (log.filePath.startsWith("content://")) {
            Uri.parse(log.filePath)
        } else {
            val file = File(log.filePath)
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        }

        val isAudio = log.filePath.contains(".mp3")
        val mimeType = if (isAudio) "audio/*" else "video/*"

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Play media with"))
    } catch (e: Exception) {
        Toast.makeText(context, "No app available to play this file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}

fun shareMediaFile(context: Context, log: DownloadLog) {
    try {
        val uri = if (log.filePath.startsWith("content://")) {
            Uri.parse(log.filePath)
        } else {
            val file = File(log.filePath)
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        }

        val isAudio = log.filePath.contains(".mp3")
        val mimeType = if (isAudio) "audio/mpeg" else "video/mp4"

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, log.title)
            putExtra(Intent.EXTRA_TEXT, "Downloaded via VidSaver: ${log.title}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share media via"))
    } catch (e: Exception) {
        Toast.makeText(context, "Unable to share file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}
