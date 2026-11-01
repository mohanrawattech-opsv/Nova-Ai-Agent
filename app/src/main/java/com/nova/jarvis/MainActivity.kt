package com.nova.jarvis

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.AlarmClock
import android.provider.Settings
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : ComponentActivity() {
    private var status by mutableStateOf("Systems online. What shall we do?")
    private lateinit var voiceLauncher: androidx.activity.result.ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        voiceLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let(::handleCommand)
            }
        }
        setContent {
            NovaScreen(
                status = status,
                onCommand = ::handleCommand,
                onMic = ::startVoiceInput,
                onEnableAccess = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                accessEnabled = JarvisAccessibilityService.isEnabled(this)
            )
        }
    }

    private fun startVoiceInput() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 41)
            status = "Allow microphone access, then tap SPEAK again."
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "I'm listening…")
        }
        runCatching { voiceLauncher.launch(intent) }.onFailure { status = "Voice recognition isn't available on this device." }
    }

    private fun handleCommand(raw: String) {
        val command = raw.trim()
        val lower = command.lowercase(Locale.getDefault())
        try {
            when {
                lower.startsWith("open ") -> {
                    val app = lower.removePrefix("open ").trim()
                    val pkg = when {
                        "youtube" in app -> "com.google.android.youtube"
                        "whatsapp" in app -> "com.whatsapp"
                        "chrome" in app -> "com.android.chrome"
                        "settings" in app -> "com.android.settings"
                        "camera" in app -> "com.android.camera"
                        "maps" in app -> "com.google.android.apps.maps"
                        "gmail" in app -> "com.google.android.gm"
                        else -> null
                    }
                    val launch = pkg?.let { packageManager.getLaunchIntentForPackage(it) }
                    if (launch != null) { startActivity(launch); status = "Opening $app." }
                    else status = "I couldn't find $app. Try YouTube, WhatsApp, Chrome, Gmail, Maps or Settings."
                }
                lower.startsWith("set alarm") || lower.startsWith("alarm") -> {
                    val time = Regex("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?", RegexOption.IGNORE_CASE).find(lower)
                    if (time == null) status = "Tell me a time, for example: Set alarm for 6:30 AM."
                    else {
                        var hour = time.groupValues[1].toInt()
                        val minute = time.groupValues[2].ifBlank { "0" }.toInt()
                        if (time.groupValues[3].equals("pm", true) && hour < 12) hour += 12
                        if (time.groupValues[3].equals("am", true) && hour == 12) hour = 0
                        startActivity(Intent(AlarmClock.ACTION_SET_ALARM).putExtra(AlarmClock.EXTRA_HOUR, hour).putExtra(AlarmClock.EXTRA_MINUTES, minute).putExtra(AlarmClock.EXTRA_SKIP_UI, false))
                        status = "Opening alarm screen for %02d:%02d.".format(hour, minute)
                    }
                }
                lower.startsWith("click ") -> {
                    val target = command.substringAfter(" ").trim()
                    if (JarvisAccessibilityService.isEnabled(this)) { JarvisAccessibilityService.requestClick(target); status = "Trying to click “$target”." }
                    else status = "Enable Nova JARVIS Control in Accessibility settings first."
                }
                lower.startsWith("type ") -> {
                    val value = command.substringAfter(" ").trim()
                    if (JarvisAccessibilityService.isEnabled(this)) { JarvisAccessibilityService.requestType(value); status = "Trying to type your text." }
                    else status = "Enable Nova JARVIS Control in Accessibility settings first."
                }
                lower.contains("screenshot") || lower.contains("screen shot") -> {
                    if (JarvisAccessibilityService.isEnabled(this)) { JarvisAccessibilityService.requestScreenshot(); status = "Requesting screenshot. Android may show a capture indicator." }
                    else status = "Enable Nova JARVIS Control in Accessibility settings first."
                }
                lower.contains("suit") || lower.contains("सूट") -> status = "At once, boss. I can suggest an outfit, but I can't physically dress you yet."
                else -> status = "I heard: “$command”. Try Open YouTube, Set alarm for 6 AM, Take a screenshot, Click Search, or Type hello."
            }
        } catch (e: Exception) { status = "Action failed: ${e.message ?: "unknown error"}" }
    }
}

@Composable
private fun NovaScreen(status: String, onCommand: (String) -> Unit, onMic: () -> Unit, onEnableAccess: () -> Unit, accessEnabled: Boolean) {
    var input by remember { mutableStateOf("") }
    val bg = Color(0xFF050711)
    MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF69D8FF), background = bg, surface = Color(0xFF11182A))) {
        Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF111A35), bg, Color(0xFF090B18)))).padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("NOVA", color = Color(0xFF72DFFF), fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                    Text("J.A.R.V.I.S.", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                }
                Surface(color = Color(0xFF123044), shape = RoundedCornerShape(50)) { Text("● ONLINE", Modifier.padding(horizontal = 13.dp, vertical = 8.dp), color = Color(0xFF70E5C2), fontSize = 11.sp) }
            }
            Spacer(Modifier.height(38.dp))
            Box(Modifier.size(220.dp).background(Brush.radialGradient(listOf(Color(0xFF287DB8), Color(0xFF292B72), Color.Transparent)), CircleShape), contentAlignment = Alignment.Center) {
                Surface(Modifier.size(142.dp), shape = CircleShape, color = Color(0xFF101B39), border = BorderStroke(2.dp, Color(0xFF5EDBFF))) {
                    Box(contentAlignment = Alignment.Center) { Text("N", color = Color(0xFF9AEAFF), fontSize = 64.sp, fontWeight = FontWeight.Light) }
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("AT YOUR SERVICE", color = Color(0xFF75DFFF), fontSize = 11.sp, letterSpacing = 3.sp)
            Spacer(Modifier.height(14.dp))
            Surface(color = Color(0xCC141D32), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Color(0xFF293D61)), modifier = Modifier.fillMaxWidth()) {
                Text(status, Modifier.padding(18.dp), color = Color(0xFFE3EDFF), fontSize = 15.sp)
            }
            Spacer(Modifier.height(18.dp))
            OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Give me a command…", color = Color.Gray) }, singleLine = true, shape = RoundedCornerShape(16.dp))
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { if (input.isNotBlank()) { onCommand(input); input = "" } }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Default.Send, null); Spacer(Modifier.width(7.dp)); Text("EXECUTE")
                }
                Button(onClick = onMic, shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.Mic, null); Text(" SPEAK") }
            }
            Spacer(Modifier.height(18.dp))
            OutlinedButton(onClick = onEnableAccess, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Text(if (accessEnabled) "ACCESSIBILITY ENABLED" else "ENABLE DEVICE CONTROL")
            }
            Spacer(Modifier.height(10.dp))
            Text("Try: “Open YouTube” • “Set alarm for 6 AM” • “Take a screenshot”", color = Color(0xFF8291AE), fontSize = 11.sp)
            Spacer(Modifier.weight(1f))
            Text("NOVA AI • ANDROID AUTONOMY", color = Color(0xFF53627F), fontSize = 10.sp, letterSpacing = 2.sp)
        }
    }
}
