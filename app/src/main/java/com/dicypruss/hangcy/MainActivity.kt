package com.dicypruss.hangcy

import android.Manifest
import android.app.role.RoleManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    private val prefs by lazy { RejectPreferences(this) }
    private val history by lazy { RejectedCallStore.get(this) }
    private var roleHeld by mutableStateOf(false)
    private var rejectIncoming by mutableStateOf(false)
    private var rejectedCalls by mutableStateOf<List<RejectedCall>>(emptyList())
    private val onHistoryChanged: () -> Unit = {
        runOnUiThread {
            rejectedCalls = history.load()
        }
    }

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { }

    private val requestRole = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        roleHeld = isCallScreeningRoleHeld()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        rejectIncoming = prefs.isRejectIncoming()
        val missing = buildList {
            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.READ_CONTACTS)
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
                    HangcyScreen(
                        roleHeld = roleHeld,
                        rejectIncoming = rejectIncoming,
                        rejectedCalls = rejectedCalls,
                        onRequestRole = ::requestCallScreeningRole,
                        onRejectIncomingChange = { enabled ->
                            rejectIncoming = enabled
                            prefs.setRejectIncoming(enabled)
                        },
                    )
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
    }

    override fun onStop() {
        history.removeListener(onHistoryChanged)
        super.onStop()
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
}

@Composable
private fun HangcyScreen(
    roleHeld: Boolean,
    rejectIncoming: Boolean,
    rejectedCalls: List<RejectedCall>,
    onRequestRole: () -> Unit,
    onRejectIncomingChange: (Boolean) -> Unit,
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.reject_incoming),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f).padding(end = 16.dp),
            )
            Switch(
                checked = rejectIncoming,
                onCheckedChange = onRejectIncomingChange,
            )
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
    }
}
