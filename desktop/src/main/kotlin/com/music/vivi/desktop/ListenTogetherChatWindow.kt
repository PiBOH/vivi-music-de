package com.music.vivi.desktop

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Shared state of the Listen Together chat window.
 *
 * The chat is a real top-level window, not a panel inside the room screen, so
 * its visibility cannot live in the screen's composition: the user can close
 * the room screen (or browse the library) while the window stays open, and the
 * socket keeps delivering messages in the background. A tiny object holding two
 * snapshot states is enough to bridge the two:
 *
 *  * [visible]: read by [ListenTogetherClient]'s event loop to decide whether
 *    an incoming message still deserves a notification (it does not while the
 *    window is open and the user is looking at it), and by the room screen to
 *    render the Chat button;
 *  * [unread]: how many messages arrived while the window was closed. It is
 *    reset by [open], so the badge on the Chat button counts exactly what the
 *    user has not seen yet.
 *
 * Deliberately not persisted: a window that reopens itself on every launch is
 * worse than one the user opens on purpose.
 */
object ListenTogetherChatWindow {
    private val _visible = mutableStateOf(false)
    val visible: State<Boolean> = _visible

    private val _unread = mutableIntStateOf(0)
    val unread: State<Int> = _unread

    fun open() {
        _visible.value = true
        _unread.value = 0
    }

    fun close() {
        _visible.value = false
    }

    /** What the keyboard shortcut does: the same button, twice. */
    fun toggle() {
        if (_visible.value) close() else open()
    }

    /** Called for every message from another user while the window is closed. */
    fun bumpUnread() {
        _unread.value += 1
    }
}

/** A row of the message list: a day separator or an actual message. */
private sealed interface ChatRow {
    data class Day(val startOfDay: Long) : ChatRow
    data class Message(val message: LtChatMessage) : ChatRow
}

/**
 * The chat window's body.
 *
 * Telegram's layout on purpose, because that is the shape users already know:
 * own messages right-aligned in the accent colour, everyone else's left in a
 * neutral surface, a day separator when the day changes, the author on the
 * first line of each bubble, an inline quote for replies, and a composer that
 * can carry a quote of its own. The quote travels to the relay in the same
 * `reply_to` field the Android client uses, so a reply is a reply on both
 * platforms.
 */
@Composable
fun ListenTogetherChatWindowContent(
    language: String,
    manager: ListenTogetherManager,
    onClose: () -> Unit,
) {
    val room by manager.roomState.collectAsState()
    val messages by manager.chatMessages.collectAsState()
    val myUserId by manager.userId.collectAsState()
    val connection by manager.connectionState.collectAsState()

    // Dedupe before keying anything: a repeated key is a hard crash in Compose,
    // and the list is keyed by author + timestamp + text.
    val chat = remember(messages) {
        messages.distinctBy { "${it.timestamp}-${it.userId}-${it.message}" }
    }
    var input by remember { mutableStateOf("") }
    var replyTarget by remember { mutableStateOf<LtChatMessage?>(null) }
    val inputFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) { runCatching { inputFocus.requestFocus() } }

    fun send() {
        val text = input.trim()
        if (text.isEmpty()) return
        val quote = replyTarget?.let { LtRepliedMessage(it.username, it.message) }
        manager.sendChatMessage(text, quote)
        input = ""
        replyTarget = null
    }

    Column(Modifier.fillMaxSize()) {
        // ---- Header: what room this is, and how healthy the link is ----
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    Localization.get(language, "comments"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                room?.let {
                    Text(
                        it.roomCode,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            ConnectionBadge(connection, language)
            Spacer(Modifier.width(4.dp))
            // An explicit close control, next to the one the window decoration
            // already has: the window is opened from the room and from a
            // keyboard shortcut, so it has to be as easy to put away as it is
            // to bring up (the shortcut toggles it too).
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = Localization.get(language, "close"),
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        if (room == null) {
            // No room, no conversation: explain instead of showing a dead
            // composer the user would keep typing into.
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    Localization.get(language, "listen_together_description"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp),
                )
            }
            return@Column
        }

        ListenTogetherChatList(
            chat = chat,
            myUserId = myUserId,
            language = language,
            onReply = { replyTarget = it },
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )

        // ---- Composer ----
        replyTarget?.let { target ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .width(3.dp)
                            .height(30.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            Localization.get(language, "lt_reply") + ": " + target.username,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            target.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = { replyTarget = null }) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = Localization.get(language, "cancel"),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text(Localization.get(language, "comments")) },
                maxLines = 5,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(inputFocus)
                    // Enter sends, Shift+Enter makes a new line: the desktop
                    // convention the mobile keyboard cannot express, and the
                    // one people expect in a chat box.
                    .onPreviewKeyEvent { event ->
                        if (
                            event.type == KeyEventType.KeyDown &&
                            event.key == Key.Enter &&
                            !event.isShiftPressed
                        ) {
                            send()
                            true
                        } else {
                            false
                        }
                    },
            )
            Spacer(Modifier.width(8.dp))
            FilledIconButton(
                onClick = { send() },
                enabled = input.isNotBlank(),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Icon(
                    Icons.Filled.Send,
                    contentDescription = Localization.get(language, "lt_send_message"),
                )
            }
        }
    }
}

/**
 * The conversation itself, without the window chrome around it: header, empty
 * state, day separators and bubbles. Split out from
 * [ListenTogetherChatWindowContent] for one practical reason: a `Window` needs a
 * display, so this is the half that
 * `./gradlew :desktop:chatWindowRenderCheck` can render off screen and check for
 * the two things that would be visible as broken, that it draws at all and that
 * a bubble which quotes another one draws strictly more than the same bubble
 * without the quote.
 */
@Composable
fun ListenTogetherChatList(
    chat: List<LtChatMessage>,
    myUserId: String?,
    language: String,
    onReply: (LtChatMessage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = remember(chat) {
        buildList {
            var lastDay = Long.MIN_VALUE
            chat.forEach { message ->
                val day = dayStart(message.timestamp)
                if (day != lastDay) {
                    add(ChatRow.Day(day))
                    lastDay = day
                }
                add(ChatRow.Message(message))
            }
        }
    }
    val listState = rememberLazyListState()
    // Follow the conversation only when the user is already at the bottom: an
    // incoming message must never yank the list away from someone reading back.
    val atBottom by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            last >= listState.layoutInfo.totalItemsCount - 2
        }
    }
    LaunchedEffect(rows.size) {
        if (rows.isNotEmpty() && atBottom) listState.animateScrollToItem(rows.lastIndex)
    }

    if (rows.isEmpty()) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.AutoMirrored.Filled.Reply,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(32.dp),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    Localization.get(language, "lt_no_messages"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 16.dp,
            vertical = 12.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(rows, key = { row ->
            when (row) {
                is ChatRow.Day -> "day-${row.startOfDay}"
                is ChatRow.Message ->
                    "${row.message.timestamp}-${row.message.userId}-${row.message.message}"
            }
        }) { row ->
            when (row) {
                is ChatRow.Day -> DaySeparator(row.startOfDay, language)
                is ChatRow.Message -> MessageBubble(
                    message = row.message,
                    mine = row.message.userId == myUserId,
                    language = language,
                    onReply = onReply,
                )
            }
        }
    }
}

@Composable
private fun DaySeparator(startOfDay: Long, language: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(10.dp),
        ) {
            Text(
                dayLabel(startOfDay, language),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

/**
 * One message. [mine] flips the alignment and the colour, exactly like the
 * mobile chat: the author's own bubbles carry the accent, the others a neutral
 * surface. The reply affordance appears on hover, so it never adds noise to a
 * bubble the user is only reading.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: LtChatMessage,
    mine: Boolean,
    language: String,
    onReply: (LtChatMessage) -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    val bubbleShape = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = if (mine) 18.dp else 4.dp,
        bottomEnd = if (mine) 4.dp else 18.dp,
    )
    val container = if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
    val content = if (mine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val quoteColor = if (mine) {
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }

    Row(
        Modifier.fillMaxWidth().hoverable(interaction),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (mine) {
            ReplyAffordance(hovered = hovered, language = language, onReply = { onReply(message) })
        }
        Surface(
            color = container,
            shape = bubbleShape,
            modifier = Modifier.widthIn(max = 460.dp),
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (!mine) {
                    Text(
                        message.username,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
                message.replyTo?.let { quote ->
                    Surface(
                        color = quoteColor,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(),
                    ) {
                        Row(Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .width(3.dp)
                                    .height(24.dp)
                                    .background(content.copy(alpha = 0.7f), RoundedCornerShape(2.dp))
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    quote.username,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = content.copy(alpha = 0.9f),
                                )
                                Text(
                                    quote.message,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = content.copy(alpha = 0.8f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
                // Selectable on purpose: right-click gets the app-wide Material 3
                // context menu, so "copy this message" needs no extra button.
                SelectionContainer {
                    Text(message.message, style = MaterialTheme.typography.bodyMedium, color = content)
                }
                Text(
                    timeLabel(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = content.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
        if (!mine) {
            ReplyAffordance(hovered = hovered, language = language, onReply = { onReply(message) })
        }
    }
}

/** Hover-revealed reply button; reserves its slot so the bubble never shifts. */
@Composable
private fun ReplyAffordance(
    hovered: Boolean,
    language: String,
    onReply: () -> Unit,
) {
    Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
        if (hovered) {
            IconButton(onClick = onReply, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.Reply,
                    contentDescription = Localization.get(language, "lt_reply"),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Timestamps
// ---------------------------------------------------------------------------

/**
 * The relay sends the chat timestamp in milliseconds (same as the mobile
 * client). A value that looks like seconds is scaled up, so a future protocol
 * change cannot print a 1970 date.
 */
private fun normalizedMillis(timestamp: Long): Long =
    if (timestamp in 1L until 100_000_000_000L) timestamp * 1000L else timestamp

private fun dayStart(timestamp: Long): Long = runCatching {
    Instant.ofEpochMilli(normalizedMillis(timestamp))
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}.getOrDefault(0L)

private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private fun timeLabel(timestamp: Long): String = runCatching {
    Instant.ofEpochMilli(normalizedMillis(timestamp)).atZone(ZoneId.systemDefault()).format(timeFormatter)
}.getOrDefault("")

/**
 * The separator is the full localized date rather than "Today"/"Yesterday":
 * the date needs no new string in each of the 52 languages, and it reads
 * correctly in all of them (the app language's locale drives the format, so it
 * is "8 ott 2026" in Italian and "Oct 8, 2026" in English).
 */
private fun dayLabel(startOfDay: Long, language: String): String = runCatching {
    val locale = Languages.jvmLocale(language)
    val date = LocalDate.ofInstant(Instant.ofEpochMilli(startOfDay), ZoneId.systemDefault())
    date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
}.getOrDefault("")
