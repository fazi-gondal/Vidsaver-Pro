package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.AppDatabase
import com.example.repository.DownloadRepository
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.QueueScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.VidSaverViewModel
import com.example.viewmodel.VidSaverViewModelFactory

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

data class TabDetails(
    val title: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector
)
