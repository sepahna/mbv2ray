package com.v2ray.ang.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.R
import com.v2ray.ang.dto.GroupMapItem
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.ui.compose.ConfirmDialog
import com.v2ray.ang.ui.compose.QRCodeDialog
import com.v2ray.ang.ui.compose.verticalScrollbar
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    onAction: (MainAction) -> Unit,
    onNavigate: (MainDestination) -> Unit,
) {
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val groups = uiState.groups
    val isLoading by mainViewModel.isLoading.collectAsStateWithLifecycle()
    val isRunning = uiState.isRunning
    val displayText = mainViewModel.formatStatus(uiState.status)
    val selectedGuid = uiState.selectedGuid
    val doubleColumnDisplay = uiState.doubleColumnDisplay
    val confirmRemove = uiState.confirmRemove
    val shareQRCodeBitmap = uiState.shareQRCodeBitmap

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showDelAllConfirm by remember { mutableStateOf(false) }
    var showDelDuplicateConfirm by remember { mutableStateOf(false) }
    var showDelInvalidConfirm by remember { mutableStateOf(false) }
    var showRemoveConfirm by rememberSaveable(stateSaver = ServerDeleteTarget.Saver) {
        mutableStateOf<ServerDeleteTarget?>(null)
    }
    var showServerSheet by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var shareTarget by remember { mutableStateOf<Triple<String, ProfileItem, Boolean>?>(null) }
    var tabMenu by remember { mutableStateOf<GroupMapItem?>(null) }
    var moveTarget by remember { mutableStateOf<GroupMapItem?>(null) }
    var deleteTarget by remember { mutableStateOf<GroupMapItem?>(null) }
    val removeServer: (String, String) -> Unit = { guid, profileName ->
        if (confirmRemove) {
            showRemoveConfirm = ServerDeleteTarget(guid, profileName)
        } else {
            onAction(MainAction.RemoveServer(guid))
        }
    }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { groups.size.coerceAtLeast(1) }
    )

    val lazyListStates = remember { mutableStateMapOf<String, LazyListState>() }
    val lazyGridStates = remember { mutableStateMapOf<String, LazyGridState>() }

    val selectedGroupId = uiState.selectedGroupId
    val groupStateFlow = remember(selectedGroupId) { mainViewModel.serverGroupState(selectedGroupId) }
    val groupState by groupStateFlow.collectAsStateWithLifecycle()
    val selectedRow = groupState.rows.firstOrNull { it.guid == selectedGuid }
        ?: groupState.rows.firstOrNull()
    val selectedGroup = groups.firstOrNull { it.id == selectedGroupId }
    val noServerLabel = stringResource(R.string.hub_no_server)
    val chooseServerLabel = stringResource(R.string.hub_choose_server)
    val selectedServerName = selectedRow?.remarks ?: noServerLabel
    val selectedServerMeta = run {
        val groupName = selectedGroup?.remarks.orEmpty()
        val type = selectedRow?.typeDescription.orEmpty()
        when {
            groupName.isNotBlank() && type.isNotBlank() -> "$groupName · $type"
            groupName.isNotBlank() -> groupName
            type.isNotBlank() -> type
            else -> chooseServerLabel
        }
    }
    val selectedPing = selectedRow?.testDelayMillis ?: 0L

    LaunchedEffect(groups) {
        val validGroupIds = groups.map { it.id }.toSet()
        lazyListStates.keys.retainAll(validGroupIds)
        lazyGridStates.keys.retainAll(validGroupIds)
    }

    LaunchedEffect(groups, uiState.selectedGroupId) {
        if (groups.isEmpty()) return@LaunchedEffect
        val selectedIndex = groups.indexOfFirst { it.id == uiState.selectedGroupId }
            .takeIf { it >= 0 } ?: 0
        if (!pagerState.isScrollInProgress && pagerState.settledPage != selectedIndex) {
            pagerState.scrollToPage(selectedIndex)
        }
    }

    val latestGroups by rememberUpdatedState(groups)

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                val currentGroups = latestGroups
                if (page in currentGroups.indices) {
                    onAction(MainAction.SelectGroup(currentGroups[page].id))
                }
            }
    }

    MainDialogs(
        showDelAllConfirm = showDelAllConfirm,
        onDismissDelAll = { showDelAllConfirm = false },
        onConfirmDelAll = { showDelAllConfirm = false; onAction(MainAction.RemoveAllServers) },
        showDelDuplicateConfirm = showDelDuplicateConfirm,
        onDismissDelDuplicate = { showDelDuplicateConfirm = false },
        onConfirmDelDuplicate = { showDelDuplicateConfirm = false; onAction(MainAction.RemoveDuplicateServers) },
        showDelInvalidConfirm = showDelInvalidConfirm,
        onDismissDelInvalid = { showDelInvalidConfirm = false },
        onConfirmDelInvalid = { showDelInvalidConfirm = false; onAction(MainAction.RemoveInvalidServers) },
        showRemoveConfirm = showRemoveConfirm,
        onDismissRemove = { showRemoveConfirm = null },
        onConfirmRemove = { guid -> showRemoveConfirm = null; onAction(MainAction.RemoveServer(guid)) }
    )

    if (shareTarget != null) {
        val (guid, profile, more) = shareTarget!!
        ShareMethodDialog(
            guid = guid,
            profile = profile,
            more = more,
            onDismiss = { shareTarget = null },
            onAction = onAction,
            onRemove = removeServer,
        )
    }
    if (shareQRCodeBitmap != null) {
        QRCodeDialog(bitmap = shareQRCodeBitmap, onDismiss = { onAction(MainAction.DismissQRCodeDialog) })
    }
    tabMenu?.let { group ->
        GroupTabActionDialog(
            name = group.remarks,
            onMove = {
                tabMenu = null
                moveTarget = group
            },
            onDelete = {
                tabMenu = null
                deleteTarget = group
            },
            onDismiss = { tabMenu = null }
        )
    }
    moveTarget?.let { group ->
        GroupMoveDialog(
            group = group,
            groups = groups,
            onMoveBefore = { beforeId ->
                mainViewModel.moveSubscription(group.id, beforeId)
                moveTarget = null
            },
            onMoveLast = {
                mainViewModel.moveSubscription(group.id, null)
                moveTarget = null
            },
            onDismiss = { moveTarget = null }
        )
    }
    deleteTarget?.let { group ->
        ConfirmDialog(
            title = stringResource(R.string.group_tab_delete_title),
            message = stringResource(R.string.group_tab_delete_message, group.remarks),
            confirmText = stringResource(R.string.action_yes),
            dismissText = stringResource(R.string.action_no),
            onConfirm = { mainViewModel.deleteSubscription(group.id) },
            onDismiss = { deleteTarget = null }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            MainDrawerContent(
                drawerState = drawerState,
                onNavigate = { route ->
                    scope.launch { drawerState.close() }
                    onNavigate(route)
                }
            )
        }
    ) {
        Scaffold(
            contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
            containerColor = Color.Transparent,
            topBar = {
                HubTopBar(
                    isLoading = isLoading,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onSearchClick = {
                        showSearch = true
                        showServerSheet = true
                    },
                    onSettingsClick = { onNavigate(MainDestination.Settings) },
                    onMoreMenuAction = { action ->
                        when (action) {
                            MainMoreMenuAction.RestartService -> onAction(MainAction.RestartService)
                            MainMoreMenuAction.DeleteAll -> showDelAllConfirm = true
                            MainMoreMenuAction.DeleteDuplicate -> showDelDuplicateConfirm = true
                            MainMoreMenuAction.DeleteInvalid -> showDelInvalidConfirm = true
                            MainMoreMenuAction.ExportAll -> onAction(MainAction.ExportAll)
                            MainMoreMenuAction.LocateSelected -> {
                                showServerSheet = true
                                onAction(MainAction.LocateSelectedServer)
                            }
                            MainMoreMenuAction.SortByTestResults -> onAction(MainAction.SortByTestResults)
                            MainMoreMenuAction.TestAll -> onAction(MainAction.TestAllServers)
                            MainMoreMenuAction.TestAllRealPing -> onAction(MainAction.TestRealAllServers)
                            MainMoreMenuAction.UpdateSubscriptions -> onAction(MainAction.UpdateSubscriptions)
                        }
                    },
                    onAction = onAction,
                )
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF0B0E16), Color(0xFF07090F), Color(0xFF05070C))
                        )
                    )
                    .padding(innerPadding)
            ) {
                MainHomeHub(
                    groups = groups,
                    selectedGroupIndex = pagerState.currentPage.coerceIn(
                        0,
                        (groups.size - 1).coerceAtLeast(0)
                    ),
                    isRunning = isRunning,
                    statusText = displayText,
                    selectedServerName = selectedServerName,
                    selectedServerMeta = selectedServerMeta,
                    selectedPingMillis = selectedPing,
                    onSelectGroup = { targetIndex ->
                        scope.launch {
                            pagerState.navigateToPageOptimized(
                                targetPage = targetIndex,
                                animateAdjacentPage = true
                            )
                        }
                    },
                    onToggleService = { onAction(MainAction.ToggleService) },
                    onOpenServers = { showServerSheet = true },
                    onTestCurrent = { onAction(MainAction.TestCurrentServer) },
                )
            }
        }
    }

    if (showServerSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showServerSheet = false
                showSearch = false
                searchQuery = ""
                onAction(MainAction.Search(""))
            },
            sheetState = sheetState,
            containerColor = Color(0xFF10131C),
            scrimColor = Color.Black.copy(alpha = 0.55f),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(560.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.title_server),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    IconButton(onClick = {
                        showSearch = !showSearch
                        if (!showSearch) {
                            searchQuery = ""
                            onAction(MainAction.Search(""))
                        }
                    }) {
                        Icon(
                            painter = painterResource(
                                if (showSearch) R.drawable.ic_arrow_back_24dp else R.drawable.ic_search_24dp
                            ),
                            contentDescription = stringResource(R.string.acc_search),
                            tint = Color.White,
                        )
                    }
                }
                if (showSearch) {
                    androidx.compose.material3.OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            onAction(MainAction.Search(it))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        singleLine = true,
                        placeholder = { Text(stringResource(R.string.menu_item_search)) },
                        shape = RoundedCornerShape(14.dp),
                    )
                }
                if (groups.isNotEmpty()) {
                    if (groups.size > 1) {
                        GroupTabBar(
                            groups = groups,
                            selectedTabIndex = pagerState.currentPage.coerceIn(0, groups.lastIndex),
                            mainViewModel = mainViewModel,
                            onTabClick = { targetIndex ->
                                scope.launch {
                                    pagerState.navigateToPageOptimized(
                                        targetPage = targetIndex,
                                        animateAdjacentPage = true
                                    )
                                }
                            },
                            onTabLongClick = { tabMenu = it }
                        )
                    }
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        userScrollEnabled = true,
                        beyondViewportPageCount = 1,
                        key = { page -> groups.getOrNull(page)?.id ?: "group-page-$page" }
                    ) { page ->
                        val group = groups.getOrNull(page) ?: return@HorizontalPager
                        GroupPagerPage(
                            groupId = group.id,
                            mainViewModel = mainViewModel,
                            selectedGuid = selectedGuid,
                            locateTarget = uiState.locateTarget,
                            doubleColumnDisplay = doubleColumnDisplay,
                            searchQuery = searchQuery,
                            lazyListStates = lazyListStates,
                            lazyGridStates = lazyGridStates,
                            onSelectServer = { guid ->
                                onAction(MainAction.SelectServer(guid))
                                showServerSheet = false
                            },
                            onEditServer = { guid, profile -> onAction(MainAction.EditServer(guid, profile)) },
                            onShareServer = { guid, profile ->
                                shareTarget = Triple(guid, profile, false)
                            },
                            onMoreServer = { guid, profile ->
                                shareTarget = Triple(guid, profile, true)
                            },
                            onRemoveServer = removeServer,
                            onPingServer = { guid -> onAction(MainAction.TestServerRealPing(guid)) },
                            usageInfo = group.usageInfo,
                            groupTitle = group.remarks,
                            onOpenLink = { url -> onAction(MainAction.OpenSubscriptionLink(url)) },
                            contentPadding = PaddingValues(bottom = 24.dp)
                        )
                    }
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.hub_no_server), color = Color(0xFF8B93A7))
                    }
                }
            }
        }
    }
}

@Composable
private fun HubTopBar(
    isLoading: Boolean,
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onMoreMenuAction: (MainMoreMenuAction) -> Unit,
    onAction: (MainAction) -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    var showImportMenu by remember { mutableStateOf(false) }
    val moreMenuScrollState = rememberScrollState()
    val importMenuScrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onMenuClick) {
            Icon(
                painterResource(R.drawable.ic_menu_24dp),
                contentDescription = stringResource(R.string.acc_open_menu),
                tint = Color.White,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF9B8CFF), Color(0xFF6D5CFF))),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_lock_24dp),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.size(10.dp))
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
                Text(
                    text = stringResource(R.string.hub_brand_subtitle),
                    color = Color(0xFF8B93A7),
                    fontSize = 11.sp,
                )
            }
        }
        if (isLoading) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(18.dp),
                strokeWidth = 2.dp,
                color = Color(0xFF9B8CFF),
            )
        }
        IconButton(onClick = onSearchClick) {
            Icon(
                painterResource(R.drawable.ic_search_24dp),
                contentDescription = stringResource(R.string.acc_search),
                tint = Color.White,
            )
        }
        IconButton(onClick = onSettingsClick) {
            Icon(
                painterResource(R.drawable.ic_settings_24dp),
                contentDescription = stringResource(R.string.title_settings),
                tint = Color.White,
            )
        }
        Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
            IconButton(onClick = { showImportMenu = true }) {
                Icon(
                    painterResource(R.drawable.ic_add_24dp),
                    contentDescription = stringResource(R.string.acc_add),
                    tint = Color.White,
                )
            }
            DropdownMenu(
                expanded = showImportMenu,
                onDismissRequest = { showImportMenu = false },
                scrollState = importMenuScrollState,
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .heightIn(max = 420.dp)
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
        Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    painterResource(R.drawable.ic_more_vert_24dp),
                    contentDescription = stringResource(R.string.acc_more),
                    tint = Color.White,
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                scrollState = moreMenuScrollState,
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScrollbar(moreMenuScrollState)
            ) {
                MoreMenuContent { action ->
                    showMenu = false
                    onMoreMenuAction(action)
                }
            }
        }
    }
}
