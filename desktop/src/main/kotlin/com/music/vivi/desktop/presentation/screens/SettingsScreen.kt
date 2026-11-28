package com.music.vivi.desktop.presentation.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.music.vivi.desktop.presentation.screens.settings.ArtistSettingsDetailScreen
import com.music.vivi.desktop.presentation.screens.settings.ContributorsDetailScreen
import com.music.vivi.desktop.data.library.LibraryHealthGroup
import com.music.vivi.desktop.presentation.screens.settings.DeviceSyncScreen
import com.music.vivi.desktop.presentation.screens.settings.DuplicatesDetailScreen
import com.music.vivi.desktop.presentation.screens.settings.IntroSettingsDetailScreen
import com.music.vivi.desktop.presentation.screens.settings.Legacy155Actions
import com.music.vivi.desktop.presentation.screens.settings.Legacy155SettingsScreen
import com.music.vivi.desktop.presentation.screens.settings.isLegacy155
import com.music.vivi.desktop.presentation.screens.settings.LibraryHealthDetailScreen
import com.music.vivi.desktop.presentation.screens.settings.LicensesDetailScreen
import com.music.vivi.desktop.presentation.screens.settings.MusicFoldersDetailScreen
import com.music.vivi.desktop.presentation.screens.settings.PaletteStyleDetailScreen
import com.music.vivi.desktop.presentation.screens.settings.SettingsCategoryDetail
import com.music.vivi.desktop.presentation.screens.settings.SettingsOverviewMenu
import com.music.vivi.desktop.presentation.screens.settings.SettingsSearchCatalog
import com.music.vivi.desktop.presentation.screens.settings.SettingsSearchResults
import com.music.vivi.desktop.presentation.screens.settings.SettingsSection
import com.music.vivi.desktop.presentation.viewmodel.PlayerViewModel

enum class LibrarySubDetail {
    NONE,
    FOLDERS,
    ARTISTS,
    HEALTH,
    DUPLICATES,
    PALETTE_STYLE,
    LICENSES,
    CONTRIBUTORS
}

@Composable
fun SettingsScreen(
    viewModel: PlayerViewModel,
    isDarkTheme: Boolean,
    isDynamicColor: Boolean,
    onToggleDarkTheme: () -> Unit,
    onToggleDynamicColor: () -> Unit,
    onRescanLibrary: () -> Unit,
    activeSection: SettingsSection? = null,
    activeSubDetail: LibrarySubDetail = LibrarySubDetail.NONE,
    searchQuery: String = "",
    onClearSearch: () -> Unit = {},
    onNavigate: (SettingsSection?, LibrarySubDetail) -> Unit = { _, _ -> },
    onBack: () -> Unit = {},
    legacyActions: Legacy155Actions? = null,
    modifier: Modifier = Modifier
) {
    var healthFocus by remember { mutableStateOf<LibraryHealthGroup?>(null) }

    // The card selects the group it opens; leaving Library Health clears the
    // selection so a later visit from search starts on the group list.
    LaunchedEffect(activeSection, activeSubDetail) {
        if (activeSubDetail != LibrarySubDetail.HEALTH) healthFocus = null
    }

    // Search runs over the native categories; a query on a 1.55 page stays on
    // that page instead of jumping to a different settings surface.
    val isNativeSettingsSurface = activeSection != null &&
        !activeSection.isLegacy155 &&
        activeSection != SettingsSection.INTRO
    if (searchQuery.isNotBlank() && isNativeSettingsSurface) {
        val searchResults = remember(searchQuery) {
            SettingsSearchCatalog.search(searchQuery)
        }
        SettingsSearchResults(
            query = searchQuery,
            results = searchResults,
            isDarkTheme = isDarkTheme,
            onSelect = { item ->
                onClearSearch()
                onNavigate(item.section, item.subDetail)
            },
            modifier = modifier
        )
        return
    }

    AnimatedContent(
        targetState = Pair(activeSection, activeSubDetail),
        transitionSpec = {
            val initialDepth = when {
                initialState.first == null -> 0
                initialState.second != LibrarySubDetail.NONE -> 2
                else -> 1
            }
            val targetDepth = when {
                targetState.first == null -> 0
                targetState.second != LibrarySubDetail.NONE -> 2
                else -> 1
            }
            val isForward = targetDepth > initialDepth

            if (isForward) {
                slideInHorizontally(initialOffsetX = { it / 3 }, animationSpec = tween(260)) + fadeIn(tween(260)) togetherWith
                        slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(200)) + fadeOut(tween(200))
            } else {
                slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(260)) + fadeIn(tween(260)) togetherWith
                        slideOutHorizontally(targetOffsetX = { it / 3 }, animationSpec = tween(200)) + fadeOut(tween(200))
            }
        },
        modifier = modifier.fillMaxSize(),
        label = "SettingsNavigation"
    ) { (section, subDetail) ->
        when {
            section == null -> {
                SettingsOverviewMenu(
                    onSelectSection = {
                        onNavigate(it, LibrarySubDetail.NONE)
                    }
                )
            }
            section == SettingsSection.DEVICE_SYNC -> {
                DeviceSyncScreen(
                    viewModel = viewModel,
                    onBack = { onNavigate(null, LibrarySubDetail.NONE) }
                )
            }
            section.isLegacy155 -> {
                Legacy155SettingsScreen(
                    section = section,
                    viewModel = viewModel,
                    onBack = { onNavigate(null, LibrarySubDetail.NONE) },
                    onNavigate = { target -> onNavigate(target, LibrarySubDetail.NONE) },
                    actions = legacyActions
                )
            }
            section == SettingsSection.INTRO -> {
                IntroSettingsDetailScreen(
                    viewModel = viewModel,
                    onBack = onBack
                )
            }
            section == SettingsSection.LIBRARY && subDetail == LibrarySubDetail.FOLDERS -> {
                MusicFoldersDetailScreen(
                    viewModel = viewModel,
                    onBack = onBack
                )
            }
            section == SettingsSection.LIBRARY && subDetail == LibrarySubDetail.ARTISTS -> {
                ArtistSettingsDetailScreen(
                    viewModel = viewModel,
                    onBack = onBack,
                    onRescanLibrary = onRescanLibrary
                )
            }
            section == SettingsSection.LIBRARY && subDetail == LibrarySubDetail.HEALTH -> {
                LibraryHealthDetailScreen(
                    viewModel = viewModel,
                    onBack = onBack,
                    onOpenDuplicates = { onNavigate(SettingsSection.LIBRARY, LibrarySubDetail.DUPLICATES) },
                    initialGroup = healthFocus
                )
            }
            section == SettingsSection.LIBRARY && subDetail == LibrarySubDetail.DUPLICATES -> {
                DuplicatesDetailScreen(
                    viewModel = viewModel,
                    onBack = onBack
                )
            }
            section == SettingsSection.APPEARANCE && subDetail == LibrarySubDetail.PALETTE_STYLE -> {
                PaletteStyleDetailScreen(
                    viewModel = viewModel,
                    isDarkTheme = isDarkTheme,
                    onBack = onBack
                )
            }
            subDetail == LibrarySubDetail.LICENSES -> {
                LicensesDetailScreen(
                    onBack = onBack
                )
            }
            subDetail == LibrarySubDetail.CONTRIBUTORS -> {
                ContributorsDetailScreen(
                    onBack = onBack
                )
            }
            else -> {
                SettingsCategoryDetail(
                    section = section,
                    viewModel = viewModel,
                    isDarkTheme = isDarkTheme,
                    isDynamicColor = isDynamicColor,
                    onToggleDarkTheme = onToggleDarkTheme,
                    onToggleDynamicColor = onToggleDynamicColor,
                    onRescanLibrary = onRescanLibrary,
                    onOpenFoldersDetail = { onNavigate(section, LibrarySubDetail.FOLDERS) },
                    onOpenArtistsDetail = { onNavigate(section, LibrarySubDetail.ARTISTS) },
                    onOpenHealthDetail = { group ->
                        healthFocus = group
                        onNavigate(section, LibrarySubDetail.HEALTH)
                    },
                    onOpenDuplicatesDetail = { onNavigate(section, LibrarySubDetail.DUPLICATES) },
                    onOpenPaletteStyleDetail = { onNavigate(section, LibrarySubDetail.PALETTE_STYLE) },
                    onOpenLicensesDetail = { onNavigate(section, LibrarySubDetail.LICENSES) },
                    onOpenContributorsDetail = { onNavigate(section, LibrarySubDetail.CONTRIBUTORS) },
                    onOpenLegacyPage = { target -> onNavigate(target, LibrarySubDetail.NONE) },
                    onBack = onBack
                )
            }
        }
    }
}
