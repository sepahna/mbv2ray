package com.v2ray.ang.ui.main

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.ui.compose.AppTopBar
import com.v2ray.ang.ui.compose.verticalScrollbar

@Composable
fun MainTopBar(
    isLoading: Boolean,
    showSearch: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchClose: () -> Unit,
    onSearchToggle: (Boolean) -> Unit,
    onMenuClick: () -> Unit,
    onAction: (MainAction) -> Unit,
    onMoreMenuAction: (MainMoreMenuAction) -> Unit
) {
    var showImportMenu by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val importMenuScrollState = rememberScrollState()
    val moreMenuScrollState = rememberScrollState()
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val maxMenuHeight = LocalConfiguration.current.screenHeightDp.dp - statusBarHeight - navBarHeight - 20.dp
    val channelTitle = stringResource(R.string.title_site_channel)
    val buyTitle = stringResource(R.string.title_site_buy)

    AppTopBar(
        title = "",
        onBackClick = {},
        isLoading = isLoading,
        isSearchActive = showSearch,
        searchQuery = searchQuery,
        onSearchQueryChange = onSearchQueryChange,
        onSearchClose = onSearchClose,
        searchPlaceholder = stringResource(R.string.menu_item_search),
        navigationIcon = {
            if (showSearch) {
                IconButton(onClick = onSearchClose) {
                    Icon(painterResource(R.drawable.ic_arrow_back_24dp), contentDescription = stringResource(R.string.acc_back))
                }
            } else {
                IconButton(onClick = onMenuClick) {
                    Icon(painterResource(R.drawable.ic_menu_24dp), contentDescription = stringResource(R.string.acc_open_menu))
                }
            }
        },
        actions = {
            if (!showSearch) {
                // Order: more, add, channel, buy, search, gauge (real-delay test for current group).
                Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                    BarIcon(R.drawable.ic_more_vert_24dp, stringResource(R.string.acc_more)) {
                        showMenu = true
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        scrollState = moreMenuScrollState,
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .heightIn(max = maxMenuHeight)
                            .verticalScrollbar(moreMenuScrollState)
                    ) {
                        MoreMenuContent { action ->
                            showMenu = false
                            onMoreMenuAction(action)
                        }
                    }
                }
                Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                    BarIcon(R.drawable.ic_add_24dp, stringResource(R.string.acc_add)) {
                        showImportMenu = true
                    }
                    DropdownMenu(
                        expanded = showImportMenu,
                        onDismissRequest = { showImportMenu = false },
                        scrollState = importMenuScrollState,
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .heightIn(max = maxMenuHeight)
                            .verticalScrollbar(importMenuScrollState)
                    ) {
                        ImportMenuContent(
                            onAction = { action ->
                                showImportMenu = false
                                onAction(action)
                            }
                        )
                    }
                }
                BarIcon(R.drawable.ic_channel_24dp, channelTitle) {
                    onAction(MainAction.OpenSite("${AppConfig.SITE_URL}/channel/", channelTitle))
                }
                BarIcon(R.drawable.ic_cart_24dp, buyTitle) {
                    onAction(MainAction.OpenSite("${AppConfig.SITE_URL}/plans", buyTitle))
                }
                BarIcon(R.drawable.ic_search_24dp, stringResource(R.string.acc_search)) {
                    onSearchToggle(true)
                }
                BarIcon(
                    R.drawable.ic_gauge_24dp,
                    stringResource(R.string.title_real_ping_all_server)
                ) {
                    onAction(MainAction.TestRealAllServers)
                }
            }
        }
    )
}

@Composable
private fun BarIcon(
    @DrawableRes iconRes: Int,
    description: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .semantics {
                role = Role.Button
                contentDescription = description
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
    }
}
