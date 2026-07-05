package com.example.myapplication

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.data.notification.NotificationScheduler
import com.example.myapplication.ui.workout.EquipmentScreen
import com.example.myapplication.ui.workout.ExecutionScreen
import com.example.myapplication.ui.workout.HomeScreen
import com.example.myapplication.ui.workout.HistoryScreen
import com.example.myapplication.ui.workout.WorkoutViewModel
import com.example.myapplication.ui.workout.SettingsScreen
import com.example.myapplication.ui.workout.PoemSettingsScreen
import com.example.myapplication.ui.workout.ManualWorkoutScreen
import com.example.myapplication.ui.components.ApiKeyDialog
import com.example.myapplication.data.network.UpdateManager
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val workoutViewModel by lazy { WorkoutViewModel(application) }
    private val updateManager by lazy { UpdateManager(this) }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            NotificationScheduler.scheduleDailyNotification(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestNotificationPermission()

        setContent {
            MaterialTheme {
                WorkoutApp(viewModel = workoutViewModel, updateManager = updateManager)
            }
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            NotificationScheduler.scheduleDailyNotification(this)
        }
    }
}

@Composable
fun WorkoutApp(
    viewModel: WorkoutViewModel,
    updateManager: UpdateManager,
    navController: NavHostController = rememberNavController()
) {
    var showApiKeyDialog by remember { mutableStateOf(!viewModel.hasApiKey()) }
    var showNewsApiKeyDialog by remember { mutableStateOf(viewModel.hasApiKey() && !viewModel.hasNewsApiKey()) }
    var updateSha by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var isDownloading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        updateSha = updateManager.checkForUpdates()
    }

    if (updateSha != null) {
        AlertDialog(
            onDismissRequest = { if (!isDownloading) updateSha = null },
            title = { Text(if (isDownloading) "Téléchargement..." else "Mise à jour disponible") },
            text = { Text(if (isDownloading) "Veuillez patienter pendant le téléchargement de la nouvelle version." else "Une nouvelle version est disponible. Voulez-vous l'installer maintenant ?") },
            confirmButton = {
                if (!isDownloading) {
                    Button(onClick = {
                        scope.launch {
                            isDownloading = true
                            val success = updateManager.downloadAndInstallApk()
                            if (success) {
                                updateManager.updateCurrentSha(updateSha!!)
                            }
                            isDownloading = false
                            updateSha = null
                        }
                    }) {
                        Text("Installer")
                    }
                }
            },
            dismissButton = {
                if (!isDownloading) {
                    TextButton(onClick = { updateSha = null }) {
                        Text("Plus tard")
                    }
                }
            }
        )
    }

    if (showApiKeyDialog) {
        ApiKeyDialog(
            onDismiss = { showApiKeyDialog = false },
            onSave = { key ->
                viewModel.saveApiKey(key)
                showApiKeyDialog = false
                if (!viewModel.hasNewsApiKey()) showNewsApiKeyDialog = true
            }
        )
    }

    if (showNewsApiKeyDialog) {
        ApiKeyDialog(
            title = "Clé NewsAPI.org",
            description = "Veuillez entrer votre clé NewsAPI pour afficher les actualités françaises.",
            onDismiss = { showNewsApiKeyDialog = false },
            onSave = { key ->
                viewModel.saveNewsApiKey(key)
                showNewsApiKeyDialog = false
            }
        )
    }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onStartWorkoutClick = { navController.navigate("execution") },
                onAddManualWorkoutClick = { date -> navController.navigate("manual_workout/$date") },
                onSettingsClick = { navController.navigate("settings") }, // 🟢 Envoie vers Paramètres
                onHistoryClick = { navController.navigate("history") }
            )
        }
        composable("manual_workout/{date}") { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date") ?: ""
            ManualWorkoutScreen(
                viewModel = viewModel,
                date = date,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable("execution") {
            ExecutionScreen(viewModel = viewModel, onBackClick = { navController.popBackStack() })
        }
        // 🟢 LA NOUVELLE PAGE
        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                updateManager = updateManager,
                onHomeClick = { navController.navigate("home") { popUpTo("home") { inclusive = true } } },
                onHistoryClick = { navController.navigate("history") },
                onEquipmentClick = { navController.navigate("equipment") },
                onPoemClick = { navController.navigate("poem_journal") }
            )
        }
        composable("poem_journal") {
            PoemSettingsScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onHomeClick = { navController.navigate("home") { popUpTo("home") { inclusive = true } } },
                onHistoryClick = { navController.navigate("history") },
                onSettingsClick = { navController.navigate("settings") { popUpTo("settings") { inclusive = true } } }
            )
        }
        composable("equipment") {
            EquipmentScreen(
                viewModel = viewModel,
                onHomeClick = { navController.navigate("home") { popUpTo("home") { inclusive = true } } },
                onHistoryClick = { navController.navigate("history") },
            )
        }
        composable("history") {
            HistoryScreen(
                viewModel = viewModel,
                onHomeClick = { navController.navigate("home") { popUpTo("home") { inclusive = true } } },
                onSettingsClick = { navController.navigate("settings") }
            )
        }
    }
}