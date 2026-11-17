package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.rounded.AddBox
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.local.SessionPreferences
import com.example.data.local.ShotDeskDatabase
import com.example.data.remote.CloudSyncDataSource
import com.example.data.repository.NetworkMonitor
import com.example.data.repository.ProductionRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.auth.signOutGoogleAndFirebase
import com.example.ui.components.TopHeaderBar
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.i18n.stringsForLanguage
import com.example.ui.screens.CreateScreen
import com.example.ui.screens.PlanScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WorkScreen
import com.example.ui.theme.LogoIvoryHeaderStart
import com.example.ui.theme.LogoMidnightNavy
import com.example.ui.theme.ShotDeskTheme
import com.example.ui.theme.StudioGold
import com.example.ui.theme.StudioGoldDark
import com.example.ui.viewmodel.MainTab
import com.example.ui.viewmodel.ShotDeskViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShotDeskAppRoot()
        }
    }
}

@Composable
fun ShotDeskAppRoot() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val viewModel: ShotDeskViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val firestore = FirebaseFirestore.getInstance(databaseId)
                val auth = Firebase.auth
                val roomDb = ShotDeskDatabase.getInstance(app)
                val prefs = SessionPreferences(app)
                val networkMonitor = NetworkMonitor(app)
                val cloudDataSource = CloudSyncDataSource(firestore, auth)
                val repository = ProductionRepository(
                    context = app,
                    dao = roomDb.productionDao(),
                    cloud = cloudDataSource,
                    sessionPreferences = prefs,
                    networkMonitor = networkMonitor,
                    auth = auth,
                )
                ShotDeskViewModel(
                    repository = repository,
                    sessionPreferences = prefs,
                    cloudSyncDataSource = cloudDataSource,
                    auth = auth,
                )
            }
        },
    )

    val session by viewModel.session.collectAsStateWithLifecycle()
    val firebaseUser by viewModel.firebaseUser.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        val credentialManager = CredentialManager.create(context)
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = { user -> viewModel.onGoogleSignInCompleted(user) },
            onUnauthenticated = {},
            scope = coroutineScope,
        )
    }

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeSnackbar()
        }
    }

    val isAuthenticated = session.isLoggedIn || firebaseUser != null
    val strings = remember(session.languageCode) { stringsForLanguage(session.languageCode) }
    val layoutDirection = if (strings.isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection,
        LocalAppStrings provides strings,
    ) {
        ShotDeskTheme(
            darkTheme = session.isDarkMode,
            isArabic = strings.isArabic,
        ) {
            if (!isAuthenticated) {
                AuthScreen(
                    hasSavedLocalAccount = session.nickname.isNotBlank(),
                    savedNicknameHint = session.nickname,
                    currentLanguageCode = session.languageCode,
                    errorMessage = authError,
                    onToggleLanguage = viewModel::setLanguageCode,
                    onCreateAccount = { fullName, nickname, password ->
                        viewModel.createLocalAccount(fullName, nickname, password)
                    },
                    onLogIn = { nickname, password ->
                        viewModel.logInLocalAccount(nickname, password)
                    },
                    onGoogleSignInSuccess = { user ->
                        viewModel.onGoogleSignInCompleted(user)
                    },
                    onGoogleSignInError = { err ->
                        viewModel.onGoogleSignInError(err)
                    },
                )
            } else {
                MainWorkspace(
                    viewModel = viewModel,
                    snackbarHostState = snackbarHostState,
                    onLogOut = {
                        viewModel.logOutLocalSession()
                        val credentialManager = CredentialManager.create(context)
                        signOutGoogleAndFirebase(
                            credentialManager = credentialManager,
                            scope = coroutineScope,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun MainWorkspace(
    viewModel: ShotDeskViewModel,
    snackbarHostState: SnackbarHostState,
    onLogOut: () -> Unit,
) {
    val s = LocalAppStrings.current
    val session by viewModel.session.collectAsStateWithLifecycle()
    val firebaseUser by viewModel.firebaseUser.collectAsStateWithLifecycle()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val syncState by viewModel.syncIndicatorState.collectAsStateWithLifecycle()
    val cacheStats by viewModel.cacheStats.collectAsStateWithLifecycle()
    val selectedCreateProjectId by viewModel.selectedCreateProjectId.collectAsStateWithLifecycle()
    val editingShotId by viewModel.editingShotId.collectAsStateWithLifecycle()
    val selectedWorkProjectId by viewModel.selectedWorkProjectId.collectAsStateWithLifecycle()
    val selectedDetailProjectId by viewModel.selectedDetailProjectId.collectAsStateWithLifecycle()
    val planSearchQuery by viewModel.planSearchQuery.collectAsStateWithLifecycle()
    val planFilter by viewModel.planFilter.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(MainTab.CREATE) }

    val activeProjectTitle = when (currentTab) {
        MainTab.CREATE -> projects.firstOrNull { it.project.id == selectedCreateProjectId }?.project?.title
        MainTab.PLAN -> projects.firstOrNull { it.project.id == selectedDetailProjectId }?.project?.title
        MainTab.WORK -> projects.firstOrNull { it.project.id == selectedWorkProjectId }?.project?.title
        MainTab.SETTINGS -> null
    }

    val headerTitle = when (currentTab) {
        MainTab.CREATE -> activeProjectTitle ?: s.headerCreateTitle
        MainTab.PLAN -> activeProjectTitle ?: s.headerPlanTitle
        MainTab.WORK -> activeProjectTitle ?: s.headerWorkTitle
        MainTab.SETTINGS -> s.headerSettingsTitle
    }

    val headerSubtitle = when (currentTab) {
        MainTab.CREATE -> s.headerCreateSubtitle
        MainTab.PLAN -> if (selectedDetailProjectId != null) {
            s.headerPlanDetailSubtitle
        } else {
            s.headerPlanCountSubtitle(projects.size)
        }
        MainTab.WORK -> s.headerWorkSubtitle
        MainTab.SETTINGS -> "@${session.nickname.ifBlank { "director" }}"
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 840.dp
        val navContainerColor = if (session.isDarkMode) LogoMidnightNavy else LogoIvoryHeaderStart
        val navAccentGold = if (session.isDarkMode) StudioGold else StudioGoldDark

        Scaffold(
            topBar = {
                TopHeaderBar(
                    title = headerTitle,
                    subtitle = headerSubtitle,
                    isDarkTheme = session.isDarkMode,
                    syncState = syncState,
                    pendingCount = cacheStats.pendingChangesCount,
                    onSyncPillClick = {
                        if (firebaseUser == null) {
                            currentTab = MainTab.SETTINGS
                            viewModel.showMessage(s.msgConnectGoogleNotice)
                        } else {
                            viewModel.forceSyncNow()
                        }
                    },
                )
            },
            bottomBar = {
                if (!useNavigationRail) {
                    Column {
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = navAccentGold.copy(alpha = 0.28f),
                        )
                        NavigationBar(
                            containerColor = navContainerColor,
                            windowInsets = WindowInsets.navigationBars,
                        ) {
                            MainTab.entries.forEach { tab ->
                                val selected = currentTab == tab
                                val label = tabLabel(tab, s)
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentTab = tab },
                                    icon = {
                                        Icon(
                                            imageVector = tabIcon(tab, selected),
                                            contentDescription = label,
                                        )
                                    },
                                    label = { Text(label) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = navAccentGold,
                                        selectedTextColor = navAccentGold,
                                        indicatorColor = navAccentGold.copy(alpha = 0.16f),
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    ),
                                    modifier = Modifier.testTag("nav_tab_${tab.route}"),
                                )
                            }
                        }
                    }
                }
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            modifier = Modifier.fillMaxSize(),
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = navContainerColor,
                    ) {
                        MainTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            val label = tabLabel(tab, s)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = tabIcon(tab, selected),
                                        contentDescription = label,
                                    )
                                },
                                label = { Text(label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = navAccentGold,
                                    selectedTextColor = navAccentGold,
                                    indicatorColor = navAccentGold.copy(alpha = 0.16f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                                modifier = Modifier.testTag("nav_rail_${tab.route}"),
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (currentTab) {
                        MainTab.CREATE -> CreateScreen(
                            projects = projects,
                            selectedProjectId = selectedCreateProjectId,
                            editingShotId = editingShotId,
                            onSelectProject = viewModel::selectCreateProject,
                            onCreateProject = { title -> viewModel.createProject(title) },
                            onSaveShot = viewModel::saveShotAndContinue,
                            onFinishWorkAndSaveProject = { projId, shotId, shotNum, shotType, dialogue, visuals, vfx, sfx, notes, duration, resetForm ->
                                viewModel.finishWorkAndSaveProject(
                                    projectId = projId,
                                    pendingShotId = shotId,
                                    pendingShotNumber = shotNum,
                                    pendingShotType = shotType,
                                    pendingDialogue = dialogue,
                                    pendingVisuals = visuals,
                                    pendingVfx = vfx,
                                    pendingSfx = sfx,
                                    pendingNotes = notes,
                                    pendingDurationSeconds = duration,
                                ) {
                                    resetForm()
                                    currentTab = MainTab.PLAN
                                }
                            },
                            onEditShot = viewModel::startEditingShot,
                            onCancelEditShot = viewModel::cancelEditingShot,
                            onDuplicateShot = viewModel::duplicateShot,
                            onMoveShot = viewModel::moveShot,
                            onDeleteShot = viewModel::deleteShot,
                        )

                        MainTab.PLAN -> PlanScreen(
                            projects = projects,
                            searchQuery = planSearchQuery,
                            activeFilter = planFilter,
                            selectedDetailProjectId = selectedDetailProjectId,
                            isDarkTheme = session.isDarkMode,
                            onSearchQueryChange = viewModel::updatePlanSearchQuery,
                            onFilterChange = viewModel::updatePlanFilter,
                            onOpenDetail = viewModel::openProjectDetail,
                            onEditInCreate = { projectId ->
                                viewModel.selectCreateProject(projectId)
                                viewModel.openProjectDetail(null)
                                currentTab = MainTab.CREATE
                            },
                            onStartShootingInWork = { projectId ->
                                viewModel.selectWorkProject(projectId)
                                currentTab = MainTab.WORK
                            },
                            onToggleProjectStatus = viewModel::toggleProjectStatus,
                            onDeleteProject = viewModel::deleteProject,
                            onExportFeedback = viewModel::showMessage,
                        )

                        MainTab.WORK -> WorkScreen(
                            projects = projects,
                            selectedProjectId = selectedWorkProjectId,
                            isDarkTheme = session.isDarkMode,
                            onSelectProject = viewModel::selectWorkProject,
                            onToggleShotCompleted = viewModel::toggleShotCompleted,
                        )

                        MainTab.SETTINGS -> SettingsScreen(
                            session = session,
                            firebaseUser = firebaseUser,
                            cacheStats = cacheStats,
                            onSelectLanguage = viewModel::setLanguageCode,
                            onToggleDarkMode = viewModel::setDarkMode,
                            onForceSyncNow = viewModel::forceSyncNow,
                            onClearLocalCache = viewModel::clearLocalCache,
                            onUpdateProfile = viewModel::updateProfile,
                            onGoogleSignInSuccess = viewModel::onGoogleSignInCompleted,
                            onGoogleSignInError = viewModel::onGoogleSignInError,
                            onLogOut = onLogOut,
                        )
                    }
                }
            }
        }
    }
}

private fun tabLabel(tab: MainTab, strings: AppStrings): String = when (tab) {
    MainTab.CREATE -> strings.tabCreate
    MainTab.PLAN -> strings.tabPlan
    MainTab.WORK -> strings.tabWork
    MainTab.SETTINGS -> strings.tabSettings
}

private fun tabIcon(tab: MainTab, selected: Boolean) = when (tab) {
    MainTab.CREATE -> if (selected) Icons.Rounded.AddBox else Icons.Outlined.AddBox
    MainTab.PLAN -> if (selected) Icons.Rounded.FolderCopy else Icons.Outlined.FolderCopy
    MainTab.WORK -> if (selected) Icons.Rounded.Videocam else Icons.Outlined.Videocam
    MainTab.SETTINGS -> if (selected) Icons.Rounded.Settings else Icons.Outlined.Settings
}
