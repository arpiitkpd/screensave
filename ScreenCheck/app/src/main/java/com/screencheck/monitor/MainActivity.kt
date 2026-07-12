package com.screencheck.monitor

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

private enum class Screen { ONBOARDING, USAGE }

class MainActivity : ComponentActivity() {

    private var locationGranted by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val locationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted -> locationGranted = granted }

        setContent {
            MaterialTheme(colorScheme = screenCheckColors()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val context = this@MainActivity
                    var screen by remember {
                        mutableStateOf(if (UserPrefs.isOnboarded(context)) Screen.USAGE else Screen.ONBOARDING)
                    }

                    when (screen) {
                        Screen.ONBOARDING -> OnboardingScreen(
                            onRequestLocation = {
                                val already = ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.ACCESS_COARSE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                                if (already) {
                                    locationGranted = true
                                } else {
                                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                }
                            },
                            locationGranted = locationGranted,
                            onContinue = { username, age, wantsLocation ->
                                UserPrefs.saveOnboarding(context, username, age, wantsLocation && locationGranted)
                                screen = Screen.USAGE
                            }
                        )
                        Screen.USAGE -> UsageScreen(context = context)
                    }
                }
            }
        }
    }
}

@Composable
private fun screenCheckColors() = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFFC1502E),
    secondary = androidx.compose.ui.graphics.Color(0xFF7C8A5B),
    background = androidx.compose.ui.graphics.Color(0xFFF1E9DE),
    surface = androidx.compose.ui.graphics.Color(0xFFF1E9DE)
)

@Composable
private fun OnboardingScreen(
    onRequestLocation: () -> Unit,
    locationGranted: Boolean,
    onContinue: (username: String, age: Int, wantsLocation: Boolean) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var ageText by remember { mutableStateOf("") }
    var wantsLocation by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Set up ScreenCheck", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "We'll use this to track your Instagram time. Nothing leaves your phone in this prototype.",
            fontSize = 13.sp
        )
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = ageText,
            onValueChange = { input -> if (input.length <= 3 && input.all { it.isDigit() }) ageText = input },
            label = { Text("Age") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = wantsLocation,
                onCheckedChange = { checked ->
                    wantsLocation = checked
                    if (checked && !locationGranted) onRequestLocation()
                }
            )
            Spacer(Modifier.width(4.dp))
            Column {
                Text("Share location (optional)", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text(
                    if (locationGranted) "Granted" else "Used later for area-based comparisons, not required",
                    fontSize = 11.sp
                )
            }
        }
        Spacer(Modifier.height(28.dp))

        val age = ageText.toIntOrNull() ?: 0
        Button(
            onClick = { onContinue(username.trim(), age, wantsLocation) },
            enabled = username.isNotBlank() && age in 5..120,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue")
        }
    }
}

@Composable
private fun UsageScreen(context: android.content.Context) {
    var hasAccess by remember { mutableStateOf(UsageStatsHelper.hasUsageAccess(context)) }
    var usageMillis by remember { mutableStateOf(0L) }
    val username = remember { UserPrefs.getUsername(context) }

    fun refresh() {
        hasAccess = UsageStatsHelper.hasUsageAccess(context)
        usageMillis = if (hasAccess) UsageStatsHelper.getInstagramUsageTodayMillis(context) else 0L
    }

    LaunchedEffect(Unit) { refresh() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Hey, $username", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("Here's your real Instagram usage today.", fontSize = 13.sp)
        Spacer(Modifier.height(28.dp))

        if (!hasAccess) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("Usage access is off", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Android requires you to turn this on manually — there's no in-app prompt for it. " +
                            "Tap below, find ScreenCheck in the list, and allow usage access.",
                        fontSize = 12.5.sp
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = { UsageStatsHelper.openUsageAccessSettings(context) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Open usage access settings") }
                }
            }
        } else {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("INSTAGRAM TODAY", fontSize = 11.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        UsageStatsHelper.formatDuration(usageMillis),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        OutlinedButton(onClick = { refresh() }, modifier = Modifier.fillMaxWidth()) {
            Text("Refresh")
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Note: Android updates usage stats in the background periodically, " +
                "so this may lag your live session by a minute or two.",
            fontSize = 10.5.sp
        )
    }
}
