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

class MainActivity : ComponentActivity() {

    private val workoutViewModel by lazy { WorkoutViewModel(application) }

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
                WorkoutApp(viewModel = workoutViewModel)
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
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onStartWorkoutClick = { navController.navigate("execution") },
                onSettingsClick = { navController.navigate("settings") }, // 🟢 Envoie vers Paramètres
                onHistoryClick = { navController.navigate("history") }
            )
        }
        composable("execution") {
            ExecutionScreen(viewModel = viewModel, onBackClick = { navController.popBackStack() })
        }
        // 🟢 LA NOUVELLE PAGE
        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                onHomeClick = { navController.navigate("home") { popUpTo("home") { inclusive = true } } },
                onHistoryClick = { navController.navigate("history") },
                onEquipmentClick = { navController.navigate("equipment") }
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