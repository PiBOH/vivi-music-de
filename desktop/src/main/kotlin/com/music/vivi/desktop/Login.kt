package com.music.vivi.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Desktop YouTube sign-in — Material 3, two clear paths:
 *
 *  1. **Sign in with Google** (primary): opens the embedded sign-in window
 *     immediately. The window loads the Google sign-in page directly, the
 *     session cookies are captured automatically when Google redirects back to
 *     YouTube Music, and the window closes by itself (see [LoginWebView]).
 *  2. **Manual cookies** (secondary, collapsed): paste the Cookie header from
 *     the browser — kept for systems where the embedded window can't run.
 *
 * [onLoggedIn] is invoked after a successful, validated login.
 */
@Composable
fun LoginScreen(language: String, onBack: () -> Unit, onLoggedIn: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    ) {
        BackButton(language, onBack)
        LoginContent(language, onLoggedIn)
    }
}

/**
 * The actual sign-in content (Google window path + manual cookies), usable both
 * as a standalone screen and embedded directly in the Account settings so the
 * user never has to go through an intermediate "Log in" screen. Keeps no scroll
 * modifier of its own: the parent container owns scrolling.
 */
@Composable
fun LoginContent(language: String, onLoggedIn: () -> Unit) {
    val scope = rememberCoroutineScope()

    // Signed-in state (restored from settings on entry).
    var accountName by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().accountName) }
    var isLoggedIn by remember { mutableStateOf(LoginManager.isLoggedIn()) }

    var manualOpen by remember { mutableStateOf(false) }
    var cookie by remember { mutableStateOf("") }
    var dataSyncId by remember { mutableStateOf("") }
    var visitorData by remember { mutableStateOf("") }

    var status by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var savingCookie by remember { mutableStateOf(false) }
    var waitingForWindow by remember { mutableStateOf(false) }
    // An attempt that follows a failure starts from an empty cookie store: a
    // stale half-session is what makes the capture come back without
    // LOGIN_INFO (E1033), and signing in with the old cookies still in place is
    // what kept the error coming back. "Sign in with Google" after a failure is
    // therefore a fresh sign-in, which is the fix every report found by hand
    // ("I cleared the cache and it worked").
    var freshSession by remember { mutableStateOf(false) }

    fun refreshAccount() {
        isLoggedIn = LoginManager.isLoggedIn()
        accountName = DesktopSettings.load().accountName
    }

    if (isLoggedIn) {
        // Signed in: the Account section's own block (who, playlist sync, the
        // way out) and nothing that offers to sign in again. The pink banner
        // that used to sit above the card said the same thing a second time.
        AccountSection(
            language = language,
            isLoggedIn = true,
            onOpenLogin = {},
            onLogout = {
                scope.launch(Dispatchers.IO) {
                    LoginManager.logout()
                    refreshAccount()
                }
            },
            onLoggedIn = onLoggedIn,
        )
    } else {
        Column {
            // ------------------------------------------------ path 1: Google window
            // The shape Import from Spotify uses, because the two do the same job:
            // a head that states the state once (mark, what it is for, the state as
            // one pill), then one card that numbers the steps and ends in the button
            // that opens the window. It used to be a card with a title, three
            // bullets and the button, which read as a form rather than as a sign-in.
            GoogleSignInHero(language = language, accountName = accountName)
            GoogleSignInCard(
                language = language,
                waiting = waitingForWindow,
                saving = savingCookie,
                // While the manual card is open its submit button is the action:
                // the window button leaves in the same frame the card unfolds.
                manualOpen = manualOpen,
                onStart = {
                    error = null
                    status = null
                    waitingForWindow = true
                    val opened = LoginWebView.openEmbedded(
                        language,
                        resetFirst = freshSession,
                    ) { captured ->
                        waitingForWindow = false
                        if (captured?.cookie != null) {
                            // Auto-captured session: validate + persist exactly
                            // like the manual flow, then hand back to the app.
                            scope.launch {
                                savingCookie = true
                                try {
                                    val account = withContext(Dispatchers.IO) {
                                        LoginManager.login(
                                            cookie = captured.cookie,
                                            dataSyncIdOverride = captured.dataSyncId,
                                            visitorDataOverride = captured.visitorData,
                                        )
                                    }
                                    status = "${Localization.get(language, "logged_in_as")}: ${account.name}"
                                    freshSession = false
                                    refreshAccount()
                                    onLoggedIn()
                                } catch (e: Exception) {
                                    // Keep the captured cookies in the manual field
                                    // so a retry is a single click.
                                    cookie = captured.cookie
                                    dataSyncId = captured.dataSyncId.orEmpty()
                                    visitorData = captured.visitorData.orEmpty()
                                    manualOpen = true
                                    // The next window starts from a clean store.
                                    freshSession = true
                                    error = e.message ?: (e::class.simpleName ?: "error")
                                } finally {
                                    savingCookie = false
                                }
                            }
                        } else if (!LoginManager.isLoggedIn()) {
                            status = Localization.get(language, "login_window_closed")
                        }
                    }
                    if (!opened) {
                        waitingForWindow = false
                        error = Localization.get(language, "login_webview_unavailable")
                    }
                },
            )

            status?.let {
                SelectionContainer { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp)) }
            }
            error?.let {
                // Selectable so a failed login (e.g. the E-code detail) can be copied for a report.
                SelectionContainer {
                    Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                }
                // And where the code is explained: the message carries one (E1033,
                // E1029, E1030) and it is only useful if it can be looked up.
                Text(
                    "ERRORS.md · ${Localization.get(language, "website")}: $ERROR_PAGE_URL",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            // Embedded window unavailable → offer the browser fallback right here.
            if (error == Localization.get(language, "login_webview_unavailable")) {
                Button(
                    onClick = { LoginWebView.openBrowser() },
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Text(Localization.get(language, "login_open_browser"))
                }
            }

            // ------------------------------------------------ path 2: manual cookies
            // One header row until it is asked, then the fields: the same folding
            // card the settings screens use for a group that is set once, so the
            // fallback sits in the page's own system instead of being a second
            // full-size card that competes with the primary path.
            Spacer(Modifier.height(12.dp))
            M3CollapsibleCard(
                title = Localization.get(language, "login_manual_title"),
                expanded = manualOpen,
                onExpandedChange = { manualOpen = it },
            ) {
                Text(
                    Localization.get(language, "login_instructions"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = cookie,
                    onValueChange = { cookie = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(140.dp),
                    label = { Text(Localization.get(language, "cookie_label")) },
                )

                // Optional manual fallbacks, used only when the automatic
                // extraction from the music.youtube.com shell fails.
                Text(
                    Localization.get(language, "advanced_login_hint"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
                OutlinedTextField(
                    value = dataSyncId,
                    onValueChange = { dataSyncId = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    label = { Text(Localization.get(language, "data_sync_id_label")) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = visitorData,
                    onValueChange = { visitorData = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    label = { Text(Localization.get(language, "visitor_data_label")) },
                    singleLine = true,
                )

                Button(
                    onClick = {
                        scope.launch {
                            savingCookie = true
                            error = null
                            status = null
                            try {
                                val account = withContext(Dispatchers.IO) {
                                    LoginManager.login(
                                        cookie = cookie,
                                        dataSyncIdOverride = dataSyncId.ifBlank { null },
                                        visitorDataOverride = visitorData.ifBlank { null },
                                    )
                                }
                                status = "${Localization.get(language, "logged_in_as")}: ${account.name}"
                                refreshAccount()
                                onLoggedIn()
                            } catch (e: Exception) {
                                error = e.message ?: (e::class.simpleName ?: "error")
                            } finally {
                                savingCookie = false
                            }
                        }
                    },
                    enabled = cookie.isNotBlank() && !savingCookie,
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                ) {
                    Text(if (savingCookie) Localization.get(language, "logging_in") else Localization.get(language, "login"))
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * The head of the sign-in content: the Google mark on its tile, what the screen
 * is for and, when a session exists, the state as the title - the same shape
 * `Import from Spotify` has ([SpotifyHero]), so the two sign-ins read as one
 * family.
 *
 * The state is the title, not a pill: with no session the head carries the
 * name of the sign-in (`login_google`), and with one it says the account is
 * connected (`google_connected_title`), which is what the screen is then for.
 * The "Logged in as ..." badge it used to draw repeated in one more line what
 * the title and the sidebar already say, and it was the only screen in this
 * family with a badge.
 */
@Composable
internal fun GoogleSignInHero(language: String, accountName: String) {
    SignInHero(
        title = Localization.get(
            language,
            if (accountName.isNotBlank()) "google_connected_title" else "login_google",
        ),
        promise = Localization.get(language, "login_promise"),
        stateText = null,
        active = accountName.isNotBlank(),
        mark = { GoogleMark(30.dp) },
    )
}

/**
 * The one card: the three steps the app already printed (`login_step1`-`3`),
 * numbered because a sign-in is a sequence, the warning it prints before the
 * first attempt rather than after the second failure, and the button that opens
 * the window.
 *
 * The button is the card's only action, and the manual-cookie card below carries
 * one of its own, so only one of the two is offered at a time: with
 * [manualOpen] the button leaves and the card gets shorter by its row, in the
 * same frame the form below unfolds ([Animations.signInActionExit]).
 */
@Composable
internal fun GoogleSignInCard(
    language: String,
    waiting: Boolean,
    saving: Boolean,
    manualOpen: Boolean,
    onStart: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    GoogleMark(22.dp)
                }
                Column(Modifier.padding(start = 12.dp)) {
                    Text(
                        Localization.get(language, "login_google"),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        Localization.get(language, "login_one_window"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            SignInStepList(
                language = language,
                keys = listOf("login_step1", "login_step2", "login_step3"),
            )
            // Said before the first attempt, not after the second failure: in the
            // large majority of reports the session is only handed over on the
            // second try (the first capture comes back without everything YouTube
            // issues - ERRORS.md E1033), and a user who does not know that
            // concludes that sign-in is broken.
            Text(
                Localization.get(language, "login_retry_hint"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(top = 12.dp),
            )
            // One action at a time: the card that opens below carries its own
            // button, so this one leaves (upwards, into the card) and the card
            // loses its row while the form unfolds - both start in the frame the
            // manual card is asked for. The padding travels with the button, so
            // nothing is left behind where it used to be.
            AnimatedVisibility(
                visible = !manualOpen,
                enter = Animations.signInActionEnter(),
                exit = Animations.signInActionExit(),
            ) {
                Button(
                    onClick = onStart,
                    enabled = !waiting && !saving,
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                ) {
                    GoogleMark(18.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when {
                            saving -> Localization.get(language, "login_saving")
                            waiting -> Localization.get(language, "login_waiting")
                            else -> Localization.get(language, "login_google")
                        },
                    )
                }
            }
        }
    }
}
