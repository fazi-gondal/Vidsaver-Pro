package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.lifecycle.ViewModelProvider
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Downloading
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppDatabase
import com.example.repository.DownloadRepository
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.QueueScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.VidSaverViewModel
import com.example.viewmodel.VidSaverViewModelFactory
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize local services
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = DownloadRepository(applicationContext, database.downloadDao())
        
        // 2. Instantiate custom ViewModel using Provider Factory
        val viewModel = ViewModelProvider(
            this,
            VidSaverViewModelFactory(application, repository)
        )[VidSaverViewModel::class.java]

        setContent {
            MyApplicationTheme {
                var showSplash by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    delay(1600) // 1.6s delay
                    showSplash = false
                }

                Crossfade(
                    targetState = showSplash,
                    animationSpec = tween(durationMillis = 500, easing = EaseOutQuad),
                    label = "SplashTransition"
                ) { isSplash ->
                    if (isSplash) {
                        SplashScreen()
                    } else {
                        var selectedTab by remember { mutableIntStateOf(0) }

                        val tabs = listOf(
                            TabDetails("Downloader", Icons.Filled.CloudDownload, Icons.Outlined.CloudDownload),
                            TabDetails("Queue", Icons.Filled.Downloading, Icons.Outlined.Downloading),
                            TabDetails("Library", Icons.Filled.FolderSpecial, Icons.Outlined.FolderSpecial),
                            TabDetails("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
                        )

                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            bottomBar = {
                                NavigationBar {
                                    tabs.forEachIndexed { index, tab ->
                                        val isSelected = selectedTab == index
                                        NavigationBarItem(
                                            selected = isSelected,
                                            onClick = { selectedTab = index },
                                            label = { Text(tab.title) },
                                            icon = {
                                                Icon(
                                                    imageVector = if (isSelected) tab.activeIcon else tab.inactiveIcon,
                                                    contentDescription = tab.title
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        ) { innerPadding ->
                            val modifier = Modifier.padding(innerPadding)
                            when (selectedTab) {
                                0 -> HomeScreen(
                                    viewModel = viewModel,
                                    onNavigateToQueue = { selectedTab = 1 }, // Auto switch to Queue tab on start download
                                    modifier = modifier
                                )
                                1 -> QueueScreen(
                                    viewModel = viewModel,
                                    modifier = modifier
                                )
                                2 -> HistoryScreen(
                                    viewModel = viewModel,
                                    modifier = modifier
                                )
                                3 -> SettingsScreen(
                                    viewModel = viewModel,
                                    modifier = modifier
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SplashScreen() {
    var startAnim by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0.7f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "LogoScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(700, easing = EaseOutQuad),
        label = "LogoAlpha"
    )

    LaunchedEffect(Unit) {
        startAnim = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111112)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_download_logo),
                contentDescription = "VidSaver App Logo",
                modifier = Modifier
                    .size(130.dp)
                    .scale(scale)
                    .alpha(alpha)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "VidSaver",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier
                    .alpha(alpha)
                    .scale(scale)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Ultimate Video Downloader",
                color = Color.Gray,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
                modifier = Modifier.alpha(alpha * 0.8f)
            )
        }
    }
}

data class TabDetails(
    val title: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector
)
