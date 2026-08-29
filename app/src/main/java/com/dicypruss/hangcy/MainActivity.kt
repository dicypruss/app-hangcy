package com.dicypruss.hangcy

import android.Manifest
import android.app.role.RoleManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.dicypruss.hangcy.history.RejectedCall
import com.dicypruss.hangcy.history.RejectedCallStore
import com.dicypruss.hangcy.prefs.RejectPreferences
import com.dicypruss.hangcy.sim.SimLine
import com.dicypruss.hangcy.sim.SimRepository
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    private val prefs by lazy { RejectPreferences(this) }
    private val history by lazy { RejectedCallStore.get(this) }
    private val sims by lazy { SimRepository(this) }
    private var roleHeld by mutableStateOf(false)
    private var simLines by mutableStateOf<List<SimLine>>(emptyList())
    private var rejectWhenUnknown by mutableStateOf(false)
    private var rejectBySubId by mutableStateOf<Map<Int, Boolean>>(emptyMap())
    private var rejectedCalls by mutableStateOf<List<RejectedCall>>(emptyList())
    private var showPrivacy by mutableStateOf(false)
    private val privacyText by lazy { loadPrivacyText() }
    private val onHistoryChanged: () -> Unit = {
        runOnUiThread {
            rejectedCalls = history.load()
        }
    }

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        refreshSimState()
    }

    private val requestRole = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        roleHeld = isCallScreeningRoleHeld()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        refreshSimState()
        val missing = buildList {
            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.READ_CONTACTS)
            }
            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.READ_PHONE_STATE)
            }
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (missing.isNotEmpty()) {
            requestPermissions.launch(missing.toTypedArray())
        }
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (showPrivacy) {
                        BackHandler { showPrivacy = false }
                        PrivacyScreen(
                            body = privacyText,
                            onBack = { showPrivacy = false },
                        )
                    } else {
                        HangcyScreen(
                            roleHeld = roleHeld,
                            simLines = simLines,
                            rejectWhenUnknown = rejectWhenUnknown,
                            rejectBySubId = rejectBySubId,
                            rejectedCalls = rejectedCalls,
                            onRequestRole = ::requestCallScreeningRole,
                            onRejectWhenUnknownChange = { enabled ->
                                rejectWhenUnknown = enabled
                                prefs.setRejectWhenUnknown(enabled)
                            },
                            onRejectSubChange = { subscriptionId, enabled ->
                                rejectBySubId = rejectBySubId + (subscriptionId to enabled)
                                prefs.setRejectForSubscription(subscriptionId, enabled)
                            },
                            onPrivacyClick = { showPrivacy = true },
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        history.addListener(onHistoryChanged)
    }

    override fun onResume() {
        super.onResume()
        roleHeld = isCallScreeningRoleHeld()
        rejectedCalls = history.load()
        refreshSimState()
    }

    override fun onStop() {
        history.removeListener(onHistoryChanged)
        super.onStop()
    }

    private fun refreshSimState() {
        val lines = sims.lines()
        simLines = lines
        rejectWhenUnknown = prefs.isRejectWhenUnknown()
        rejectBySubId = lines.associate { line ->
            line.subscriptionId to prefs.isRejectForSubscription(line.subscriptionId)
        }
    }

    private fun isCallScreeningRoleHeld(): Boolean {
        val roleManager = getSystemService(RoleManager::class.java)
        return roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
    }

    private fun requestCallScreeningRole() {
        val roleManager = getSystemService(RoleManager::class.java)
        requestRole.launch(
            roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING),
        )
    }

    private fun loadPrivacyText(): String {
        return try {
            assets.open(PRIVACY_ASSET).bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            ""
        }
    }

    private companion object {
        const val PRIVACY_ASSET = "privacy.txt"
    }
}

@Composable
private fun HangcyScreen(
    roleHeld: Boolean,
    simLines: List<SimLine>,
    rejectWhenUnknown: Boolean,
    rejectBySubId: Map<Int, Boolean>,
    rejectedCalls: List<RejectedCall>,
    onRequestRole: () -> Unit,
    onRejectWhenUnknownChange: (Boolean) -> Unit,
    onRejectSubChange: (Int, Boolean) -> Unit,
    onPrivacyClick: () -> Unit,
) {
    val unknownNumber = stringResource(R.string.unknown_number)
    val timeFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
        )
        if (roleHeld) {
            Text(
                text = stringResource(R.string.caller_id_active),
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            Button(onClick = onRequestRole) {
                Text(stringResource(R.string.set_as_caller_id))
            }
        }
        if (simLines.isEmpty()) {
            RejectSwitchRow(
                title = stringResource(R.string.reject_incoming),
                checked = rejectWhenUnknown,
                onCheckedChange = onRejectWhenUnknownChange,
            )
        } else {
            simLines.forEach { line ->
                RejectSwitchRow(
                    title = line.displayName,
                    checked = rejectBySubId[line.subscriptionId] == true,
                    onCheckedChange = { enabled ->
                        onRejectSubChange(line.subscriptionId, enabled)
                    },
                )
            }
        }
        Text(
            text = stringResource(R.string.rejected_calls),
            style = MaterialTheme.typography.titleMedium,
        )
        if (rejectedCalls.isEmpty()) {
            Text(
                text = stringResource(R.string.no_rejected_calls),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(rejectedCalls, key = { "${it.atMillis}-${it.number}" }) { call ->
                    Column {
                        Text(
                            text = call.number.ifBlank { unknownNumber },
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = timeFormat.format(Date(call.atMillis)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        TextButton(onClick = onPrivacyClick) {
            Text(stringResource(R.string.privacy))
        }
    }
}

@Composable
private fun RejectSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun PrivacyScreen(
    body: String,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TextButton(onClick = onBack) {
            Text(stringResource(R.string.back))
        }
        Text(
            text = stringResource(R.string.privacy),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = body,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
