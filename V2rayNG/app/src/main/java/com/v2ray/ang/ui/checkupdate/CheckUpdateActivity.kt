package com.v2ray.ang.ui.checkupdate

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.v2ray.ang.handler.ApkUpdateInstaller
import com.v2ray.ang.handler.AppUpdateUi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.R
import com.v2ray.ang.core.CoreNativeManager
import com.v2ray.ang.ui.base.BaseComponentActivity
import com.v2ray.ang.ui.compose.AppTopBar
import com.v2ray.ang.ui.compose.NavigationBarsSpacer
import com.v2ray.ang.ui.compose.SettingsMenuItem
import com.v2ray.ang.ui.compose.SettingsSwitchItem
import com.v2ray.ang.ui.compose.VersionInfoBlock
import com.v2ray.ang.ui.compose.verticalScrollbar

class CheckUpdateActivity : BaseComponentActivity() {

    private val viewModel: CheckUpdateViewModel by viewModels()
    private var updateUi by mutableStateOf<AppUpdateUi?>(null)
    private var updateJob: Job? = null
    private var pendingUpdateApk: File? = null

    private val unknownSourcesLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            pendingUpdateApk?.let { apk -> ApkUpdateInstaller.install(this, apk) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            viewModel.checkForUpdates()
        }
    }

    @Composable
    override fun ScreenContent() {
        CheckUpdateScreen(
            viewModel = viewModel,
            onBackClick = { finish() },
            onDownload = { url, version -> startDownload(url, version) }
        )
        updateUi?.let { state ->
            UpdateProgressDialog(
                state = state,
                onUpdateNow = {},
                onLater = { cancelDownload() }
            )
        }
    }

    private fun startDownload(url: String, version: String) {
        updateJob?.cancel()
        updateUi = AppUpdateUi(version = version)
        updateJob = lifecycleScope.launch {
            val downloaded = runCatching {
                ApkUpdateInstaller.download(applicationContext, url) { received, total ->
                    updateUi = updateUi?.copy(received = received, total = total)
                }
            }
            downloaded.onSuccess { apk ->
                updateUi = updateUi?.copy(finished = true, received = apk.length())
                pendingUpdateApk = apk
                if (!ApkUpdateInstaller.canInstallPackages(this@CheckUpdateActivity)) {
                    unknownSourcesLauncher.launch(
                        Intent(
                            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse("package:$packageName")
                        )
                    )
                } else {
                    ApkUpdateInstaller.install(this@CheckUpdateActivity, apk)
                }
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                updateUi = updateUi?.copy(error = error.message)
            }
        }
    }

    private fun cancelDownload() {
        updateJob?.cancel()
        updateUi = null
    }
}

@Composable
fun CheckUpdateScreen(
    viewModel: CheckUpdateViewModel,
    onBackClick: () -> Unit,
    onDownload: (url: String, version: String) -> Unit
) {

    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val checkPreRelease by viewModel.checkPreRelease.collectAsStateWithLifecycle()
    val showUpdateDialog by viewModel.showUpdateDialog.collectAsStateWithLifecycle()
    val updateResult by viewModel.updateResult.collectAsStateWithLifecycle()

    val libVersion = CoreNativeManager.getLibVersion()
    val versionText = "v${BuildConfig.VERSION_NAME} ($libVersion)"

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.update_check_for_update),
                onBackClick = onBackClick,
                isLoading = isLoading
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsSwitchItem(
                icon = painterResource(R.drawable.ic_source_code_24dp),
                title = stringResource(R.string.update_check_pre_release),
                checked = checkPreRelease,
                onCheckedChange = { viewModel.toggleCheckPreRelease(it) }
            )
            SettingsMenuItem(
                icon = painterResource(R.drawable.ic_check_update_24dp),
                title = stringResource(R.string.update_check_for_update),
                onClick = { viewModel.checkForUpdates() }
            )
            VersionInfoBlock(versionText = versionText)
            NavigationBarsSpacer()
        }
    }

    if (showUpdateDialog && updateResult != null) {
        val result = updateResult!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissUpdateDialog() },
            title = { Text(stringResource(R.string.update_new_version_found, result.latestVersion ?: "")) },
            text = {
                val scrollState = rememberScrollState()
                Text(
                    text = result.releaseNotes.orEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .verticalScrollbar(scrollState)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissUpdateDialog()
                    result.downloadUrl?.let { onDownload(it, result.latestVersion.orEmpty()) }
                }) {
                    Text(stringResource(R.string.update_now))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissUpdateDialog() }) {
                    Text(stringResource(R.string.update_later))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}
