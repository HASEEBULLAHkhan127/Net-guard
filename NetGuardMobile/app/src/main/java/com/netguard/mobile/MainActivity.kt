package com.netguard.mobile

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import java.util.Calendar

private val Bg = Color(0xFF07111F)
private val Surface = Color(0xFF101E30)
private val Surface2 = Color(0xFF17283D)
private val Accent = Color(0xFF42A5F5)
private val Mint = Color(0xFF35D6A0)
private val TextMuted = Color(0xFF9CB0C8)
private val Warning = Color(0xFFFFC857)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NetGuardTheme { NetGuardRoot() } }
    }
}

@Composable
private fun NetGuardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent,
            secondary = Mint,
            background = Bg,
            surface = Surface,
            onBackground = Color.White,
            onSurface = Color.White
        ),
        content = content
    )
}

private enum class Screen(val label: String) {
    HOME("Home"), APPS("Apps"), CONNECTIONS("Connections"), ALERTS("Alerts")
}

@Composable
private fun NetGuardRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var screen by remember { mutableStateOf(Screen.HOME) }
    var report by remember { mutableStateOf(UsageReport.empty()) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Grant Usage Access to read per-app network totals.") }

    fun refresh() {
        scope.launch {
            loading = true
            val result = withContext(Dispatchers.IO) {
                AppUsageReader(context).readToday()
            }
            report = result
            message = when {
                !result.hasAccess -> "Usage Access is not enabled yet. Grant it in Android Settings, then refresh."
                result.error != null -> result.error
                else -> "Updated just now · Today's device totals"
            }
            loading = false
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Scaffold(
        containerColor = Bg,
        bottomBar = {
            NavigationBar(containerColor = Surface) {
                val items = listOf(
                    Screen.HOME to Icons.Default.Home,
                    Screen.APPS to Icons.Default.Apps,
                    Screen.CONNECTIONS to Icons.Default.SettingsEthernet,
                    Screen.ALERTS to Icons.Default.Security
                )
                items.forEach { (item, icon) ->
                    NavigationBarItem(
                        selected = screen == item,
                        onClick = { screen = item },
                        icon = { Icon(icon, contentDescription = item.label) },
                        label = { Text(item.label, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Accent,
                            selectedTextColor = Accent,
                            indicatorColor = Surface2,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        )
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Bg)
        ) {
            Header(onRefresh = { refresh() }, loading = loading)
            when (screen) {
                Screen.HOME -> HomeScreen(
                    report = report,
                    message = message,
                    loading = loading,
                    onGrantAccess = {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    },
                    onRefresh = { refresh() },
                    onOpenApps = { screen = Screen.APPS }
                )
                Screen.APPS -> AppsScreen(
                    report = report,
                    message = message,
                    onGrantAccess = {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    },
                    onRefresh = { refresh() }
                )
                Screen.CONNECTIONS -> InfoScreen(
                    title = "Connection monitor",
                    description = "Per-connection and domain-to-app attribution is not available through the standard per-app usage API. This module is planned for a later, consent-based local VPN implementation.",
                    icon = Icons.Default.SettingsEthernet
                )
                Screen.ALERTS -> AlertsScreen(report)
            }
        }
    }
}

@Composable
private fun Header(onRefresh: () -> Unit, loading: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("NETGUARD", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
            Text("Mobile Security", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        FilledTonalIconButton(onClick = onRefresh, enabled = !loading) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }
    }
}

@Composable
private fun HomeScreen(
    report: UsageReport,
    message: String,
    loading: Boolean,
    onGrantAccess: () -> Unit,
    onRefresh: () -> Unit,
    onOpenApps: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
            item {
                StatusCard(report.hasAccess, message, onGrantAccess)
            }
            item {
                Text("TODAY'S NETWORK USAGE", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard("Downloaded", formatBytes(report.rxBytes), Accent, Modifier.weight(1f))
                    MetricCard("Uploaded", formatBytes(report.txBytes), Mint, Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                MetricCard("Total data", formatBytes(report.rxBytes + report.txBytes), Color.White, Modifier.fillMaxWidth())
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Top apps", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    TextButton(onClick = onOpenApps) { Text("See all") }
                }
            }
            if (report.apps.isEmpty()) {
                item { EmptyCard("No per-app totals available yet. Grant Usage Access and tap refresh.") }
            } else {
                items(report.apps.take(5), key = { it.uid }) { app ->
                    AppUsageRow(app, report.maxAppBytes)
                }
            }
            item {
                Text(
                    "Figures are read from Android's network statistics for today. Some devices or carriers may report mobile totals differently.",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun AppsScreen(
    report: UsageReport,
    message: String,
    onGrantAccess: () -> Unit,
    onRefresh: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Text("App data usage", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Per-app totals for today · Wi-Fi + mobile", color = TextMuted, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        if (!report.hasAccess) {
            StatusCard(false, message, onGrantAccess)
        } else if (report.apps.isEmpty()) {
            EmptyCard("No app traffic totals were returned for today. Try using a few apps, then refresh.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 18.dp)) {
                items(report.apps, key = { it.uid }) { app ->
                    AppUsageDetail(app, report.maxAppBytes)
                }
            }
        }
    }
}

@Composable
private fun AppUsageRow(app: AppUsage, maxBytes: Long) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Surface, RoundedCornerShape(14.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(36.dp).background(Surface2, CircleShape), contentAlignment = Alignment.Center) {
            Text(app.name.take(1).uppercase(), color = Accent, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(app.name, color = Color.White, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { if (maxBytes > 0) (app.totalBytes.toFloat() / maxBytes).coerceIn(0f, 1f) else 0f },
                modifier = Modifier.fillMaxWidth().height(5.dp),
                color = Accent,
                trackColor = Surface2
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(formatBytes(app.totalBytes), color = Accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
private fun AppUsageDetail(app: AppUsage, maxBytes: Long) {
    Column(Modifier.fillMaxWidth().background(Surface, RoundedCornerShape(14.dp)).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(app.name, color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(formatBytes(app.totalBytes), color = Accent, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { if (maxBytes > 0) (app.totalBytes.toFloat() / maxBytes).coerceIn(0f, 1f) else 0f },
            modifier = Modifier.fillMaxWidth().height(5.dp),
            color = Accent,
            trackColor = Surface2
        )
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("↓ ${formatBytes(app.rxBytes)} received", color = TextMuted, fontSize = 11.sp)
            Text("↑ ${formatBytes(app.txBytes)} sent", color = TextMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier.background(Surface, RoundedCornerShape(16.dp)).padding(16.dp)) {
        Text(title, color = TextMuted, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        Text(value, color = color, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatusCard(hasAccess: Boolean, message: String, onGrantAccess: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Surface, RoundedCornerShape(16.dp)).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(9.dp).background(if (hasAccess) Mint else Warning, CircleShape))
            Spacer(Modifier.width(9.dp))
            Text(if (hasAccess) "Usage access available" else "Permission needed", color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Text(message, color = TextMuted, fontSize = 12.sp)
        if (!hasAccess) {
            Spacer(Modifier.height(10.dp))
            Button(onClick = onGrantAccess, colors = ButtonDefaults.buttonColors(containerColor = Accent)) {
                Text("Open Usage Access settings", color = Bg)
            }
        }
    }
}

@Composable
private fun AlertsScreen(report: UsageReport) {
    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Security alerts", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Basic local usage heuristics", color = TextMuted, fontSize = 12.sp)
        val threshold = 500L * 1024L * 1024L
        val highUsage = report.apps.filter { it.totalBytes >= threshold }
        if (!report.hasAccess) {
            EmptyCard("Grant Usage Access to evaluate today's per-app totals.")
        } else if (highUsage.isEmpty()) {
            EmptyCard("No apps crossed the current high-usage review threshold (500 MB today). This is not a malware scan.")
        } else {
            highUsage.forEach { app ->
                Column(Modifier.fillMaxWidth().background(Surface, RoundedCornerShape(14.dp)).padding(14.dp)) {
                    Text("High data usage to review", color = Warning, fontWeight = FontWeight.Bold)
                    Text("${app.name} · ${formatBytes(app.totalBytes)} today", color = Color.White)
                    Text("High usage is not proof of malicious activity.", color = TextMuted, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun InfoScreen(title: String, description: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(icon, contentDescription = null, tint = Accent, modifier = Modifier.size(34.dp))
        Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(description, color = TextMuted, fontSize = 14.sp)
        EmptyCard("This feature is not active in version 0.1.0.")
    }
}

@Composable
private fun EmptyCard(text: String) {
    Text(text, color = TextMuted, fontSize = 13.sp, modifier = Modifier.fillMaxWidth().background(Surface, RoundedCornerShape(14.dp)).padding(16.dp))
}

private data class AppUsage(
    val uid: Int,
    val name: String,
    val rxBytes: Long,
    val txBytes: Long
) {
    val totalBytes: Long get() = rxBytes + txBytes
}

private data class UsageReport(
    val hasAccess: Boolean,
    val apps: List<AppUsage>,
    val rxBytes: Long,
    val txBytes: Long,
    val error: String? = null
) {
    val maxAppBytes: Long get() = apps.maxOfOrNull { it.totalBytes } ?: 0L
    companion object {
        fun empty() = UsageReport(false, emptyList(), 0L, 0L)
    }
}

private class AppUsageReader(private val context: Context) {
    fun readToday(): UsageReport {
        if (!hasUsageAccess()) return UsageReport(false, emptyList(), 0L, 0L)
        return try {
            val start = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val end = System.currentTimeMillis()

            val labels = context.packageManager.getInstalledApplications(0)
                .associate { it.uid to context.packageManager.getApplicationLabel(it).toString() }

            val totals = mutableMapOf<Int, LongArray>()
            readNetworkType(ConnectivityManager.TYPE_WIFI, start, end, totals)
            readNetworkType(ConnectivityManager.TYPE_MOBILE, start, end, totals)

            val apps = totals.mapNotNull { (uid, bytes) ->
                if (uid < 10_000 || uid == Process.myUid()) return@mapNotNull null
                val total = bytes[0] + bytes[1]
                if (total <= 0L) return@mapNotNull null
                AppUsage(uid, labels[uid] ?: "App UID $uid", bytes[0], bytes[1])
            }.sortedByDescending { it.totalBytes }

            UsageReport(
                hasAccess = true,
                apps = apps,
                rxBytes = apps.sumOf { it.rxBytes },
                txBytes = apps.sumOf { it.txBytes }
            )
        } catch (e: SecurityException) {
            UsageReport(true, emptyList(), 0L, 0L, "Android denied access to network statistics. Check Usage Access in Settings.")
        } catch (e: Exception) {
            UsageReport(true, emptyList(), 0L, 0L, "Could not read network statistics: ${e.javaClass.simpleName}")
        }
    }

    private fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        return appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        ) == AppOpsManager.MODE_ALLOWED
    }

    private fun readNetworkType(type: Int, start: Long, end: Long, totals: MutableMap<Int, LongArray>) {
        val manager = context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
        val stats = manager.querySummary(type, null, start, end)
        try {
            val bucket = NetworkStats.Bucket()
            while (stats.hasNextBucket()) {
                if (!stats.getNextBucket(bucket)) break
                val uid = bucket.uid
                val values = totals.getOrPut(uid) { longArrayOf(0L, 0L) }
                values[0] += bucket.rxBytes
                values[1] += bucket.txBytes
            }
        } finally {
            stats.close()
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "${DecimalFormat("#,##0.0").format(kb)} KB"
    val mb = kb / 1024.0
    if (mb < 1024) return "${DecimalFormat("#,##0.0").format(mb)} MB"
    return "${DecimalFormat("#,##0.00").format(mb / 1024.0)} GB"
}
