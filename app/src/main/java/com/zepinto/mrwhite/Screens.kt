package com.zepinto.mrwhite

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

private val Pill = RoundedCornerShape(50)
private val Soft = RoundedCornerShape(22.dp)

@Composable
fun MrWhiteApp(vm: GameViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val t = state.lang.strings
    val owner = LocalLifecycleOwner.current
    var confirmLeave by remember { mutableStateOf(false) }
    var showRules by rememberSaveable { mutableStateOf(false) }

    // A face-up card must never stay on screen when the app is left; it counts as seen.
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) vm.hide()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    BackHandler(enabled = state.stage != Stage.SETUP) {
        if (state.cardUp) vm.hide() else confirmLeave = true
    }
    // Declared after the leave handler so it wins: Back closes the rules page first.
    BackHandler(enabled = showRules) { showRules = false }
    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text(t.leaveTitle) },
            text = { Text(t.leaveText) },
            confirmButton = { TextButton(onClick = { confirmLeave = false; vm.backToSetup() }) { Text(t.leave) } },
            dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text(t.stay) } },
        )
    }

    CompositionLocalProvider(LocalStrings provides t) {
        if (showRules) {
            RulesScreen(onBack = { showRules = false })
            return@CompositionLocalProvider
        }
        when (state.stage) {
            Stage.SETUP -> SetupScreen(
                state = state,
                onAdd = vm::addPlayer,
                onRemove = vm::removePlayer,
                onImpostors = vm::adjustImpostors,
                onLanguage = vm::setLanguage,
                onStart = vm::startGame,
                onRules = { showRules = true },
            )
            Stage.TABLE -> CardScreen(state, onFlip = vm::flip, onHide = vm::hide, onNext = vm::next)
            Stage.DISCUSSION -> DiscussionScreen(state, onStartVoting = vm::startVoting, onRules = { showRules = true })
            Stage.VOTE -> VoteScreen(state, onVote = vm::voteOut)
            Stage.ELIMINATED -> ResultScreen(state, onGuess = vm::guessWord, onContinue = vm::continueAfterElimination)
            Stage.GAME_OVER -> GameOverScreen(state, onNewRound = vm::newRound, onSetup = vm::backToSetup)
        }
    }
}

private fun roleEmoji(role: Role) = when (role) {
    Role.CIVILIAN -> "🙂"
    Role.UNDERCOVER -> "🕵️"
    Role.MR_WHITE -> "👻"
}

private fun roleColor(role: Role) = when (role) {
    Role.CIVILIAN -> Palette.Mint
    Role.UNDERCOVER -> Palette.Coral
    Role.MR_WHITE -> Palette.Lilac
}

@Composable
private fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = Palette.Pink,
    content: Color = Color.White,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = Pill,
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = content,
            disabledContainerColor = Color.White.copy(alpha = 0.12f),
            disabledContentColor = Color.White.copy(alpha = 0.4f),
        ),
        modifier = modifier.heightIn(min = 56.dp),
    ) { Text(text, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold) }
}

@Composable
private fun Chip(text: String, color: Color) {
    Surface(shape = Pill, color = color.copy(alpha = 0.18f)) {
        Text(
            text,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun LanguagePicker(selected: Lang, onSelect: (Lang) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Lang.entries.forEach { lang ->
            val on = lang == selected
            Surface(
                shape = Pill,
                color = if (on) Palette.Aqua.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f),
                modifier = Modifier
                    .weight(1f)
                    .border(2.dp, if (on) Palette.Aqua else Color.Transparent, Pill)
                    .clip(Pill)
                    .clickable { onSelect(lang) },
            ) {
                Row(
                    Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(lang.flag, fontSize = 18.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(lang.label, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp, maxLines = 1)
                }
            }
        }
    }
}

/** A label with a minus button, the current number and a plus button. */
@Composable
private fun Stepper(
    emoji: String,
    label: String,
    value: Int,
    canDecrease: Boolean,
    canIncrease: Boolean,
    onChange: (Int) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 22.sp)
        Spacer(Modifier.width(10.dp))
        Text(label, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        StepButton("\u2212", canDecrease) { onChange(-1) }
        Text(
            "$value",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(40.dp),
        )
        StepButton("+", canIncrease) { onChange(1) }
    }
}

@Composable
private fun StepButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = if (enabled) Palette.Aqua.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.06f),
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = if (enabled) Palette.Aqua else Color.White.copy(alpha = 0.3f))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SetupScreen(
    state: UiState,
    onAdd: (String) -> Boolean,
    onRemove: (Int) -> Unit,
    onImpostors: (Int, Int) -> Unit,
    onLanguage: (Lang) -> Unit,
    onStart: () -> Unit,
    onRules: () -> Unit,
) {
    val t = LocalStrings.current
    var name by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }
    val submit = {
        if (onAdd(name)) {
            name = ""
            nameError = null
        } else if (name.isNotBlank()) {
            nameError = if (state.players.size >= GameViewModel.MAX_PLAYERS) t.maxPlayers(GameViewModel.MAX_PLAYERS) else t.nameTaken
        }
    }
    val n = state.players.size
    val (undercover, mrWhite) = state.counts
    val maxImpostors = Dealer.maxImpostors(n)
    val impostors = undercover + mrWhite
    val canStart = n >= Dealer.MIN_PLAYERS

    Column(
        Modifier
            .fillMaxSize()
            .partyBackground()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Ghost(Modifier.size(72.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Mr White", fontSize = 36.sp, fontWeight = FontWeight.Black)
                Text(t.tagline, color = Palette.Lilac)
            }
            Surface(
                shape = CircleShape,
                color = Palette.Aqua.copy(alpha = 0.2f),
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .semantics { contentDescription = t.rulesButton }
                    .clickable(onClick = onRules),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("?", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Palette.Aqua)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        LanguagePicker(state.lang, onLanguage)
        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; nameError = null },
                isError = nameError != null,
                placeholder = { Text(t.whoIsPlaying) },
                singleLine = true,
                shape = Pill,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            PillButton(t.add, submit, enabled = name.isNotBlank(), color = Palette.Aqua, content = Palette.Ink)
        }
        nameError?.let { Text(it, color = Palette.Pink, fontSize = 13.sp, modifier = Modifier.padding(start = 16.dp, top = 4.dp)) }
        Spacer(Modifier.height(8.dp))

        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(state.players, key = { _, p -> p }) { i, player ->
                Surface(
                    shape = Soft,
                    color = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(),
                ) {
                    Row(
                        Modifier.padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Avatar(player, i)
                        Spacer(Modifier.width(12.dp))
                        Text(player, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        TextButton(onClick = { onRemove(i) }) { Text("✕", color = Palette.Pink, fontSize = 18.sp) }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Surface(shape = Soft, color = Color.White.copy(alpha = 0.08f), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Stepper(
                    emoji = "🕵️",
                    label = t.undercover,
                    value = undercover,
                    canDecrease = canStart && undercover > 0 && impostors > 1,
                    canIncrease = canStart && impostors < maxImpostors,
                    onChange = { onImpostors(it, 0) },
                )
                Stepper(
                    emoji = "👻",
                    label = t.mrWhiteLabel,
                    value = mrWhite,
                    canDecrease = canStart && mrWhite > 0 && impostors > 1,
                    canIncrease = canStart && impostors < maxImpostors,
                    onChange = { onImpostors(0, it) },
                )
                Text(t.majorityHint, fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
            }
        }
        Spacer(Modifier.height(10.dp))

        if (canStart) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (mrWhite > 0) Chip("👻 ${t.mrWhiteChip(mrWhite)}", Palette.Lilac)
                if (undercover > 0) Chip("🕵️ ${t.undercoverChip(undercover)}", Palette.Coral)
                Chip("🙂 ${t.civiliansChip(n - impostors)}", Palette.Mint)
            }
        } else {
            Text(t.addAtLeast(Dealer.MIN_PLAYERS), color = Palette.Lilac)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "🔊 ${t.volumeHint}",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.6f),
        )
        Spacer(Modifier.height(10.dp))
        PillButton(t.dealCards, onStart, Modifier.fillMaxWidth(), enabled = canStart)
    }
}

/**
 * One card at a time. It lies face down with its owner's name under it; tapping flips it.
 * "Pass to ..." only appears once the card has been looked at and turned back over.
 */
@Composable
private fun CardScreen(state: UiState, onFlip: () -> Unit, onHide: () -> Unit, onNext: () -> Unit) {
    val t = LocalStrings.current
    val index = state.current
    val assignment = state.deal[index]
    val seenThis = index in state.seen
    val nextName = if (state.onLastCard) null else state.deal[state.order[state.position + 1]].player

    Column(
        Modifier
            .fillMaxSize()
            .partyBackground()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Chip("\uD83C\uDCCF ${t.cardOf(state.position + 1, state.deal.size)}", Palette.Sun)
        Spacer(Modifier.height(16.dp))

        // A new composition per card, so the flip state never carries over to the next player.
        androidx.compose.runtime.key(state.position) {
            FlipCard(
                up = state.cardUp,
                onTap = onFlip,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(0.88f),
                back = { CardBackFace(index) },
                front = { CardFrontFace(assignment) },
            )
        }

        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(assignment.player, index, size = 44.dp)
            Spacer(Modifier.width(12.dp))
            Text(assignment.player, fontSize = 28.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        }
        Spacer(Modifier.height(4.dp))
        Text(
            when {
                state.cardUp -> t.memorize
                seenThis -> t.hiddenPass
                else -> t.tapToFlip(assignment.player)
            },
            color = Palette.Lilac,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(14.dp))
        when {
            state.cardUp -> PillButton(t.hideCard, onHide, Modifier.fillMaxWidth(), color = Palette.Aqua, content = Palette.Ink)
            seenThis -> PillButton(
                if (nextName != null) t.passTo(nextName) else t.startDiscussion,
                onNext,
                Modifier.fillMaxWidth(),
            )
            else -> Spacer(Modifier.height(56.dp))
        }
    }
}

/** A card that flips around its vertical axis. The front is only composed once it has turned past the edge. */
@Composable
private fun FlipCard(
    up: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    back: @Composable () -> Unit,
    front: @Composable () -> Unit,
) {
    val rotation by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (up) 180f else 0f,
        animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow),
        label = "flip",
    )
    Box(
        modifier
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 16f * density
            }
            .clip(RoundedCornerShape(32.dp))
            .clickable(onClick = onTap),
    ) {
        // Gate on `up` too: hiding must be instant, not after the spring has swung past the edge.
        if (up && rotation > 90f) {
            Box(Modifier.graphicsLayer { rotationY = 180f }) { front() }
        } else {
            back()
        }
    }
}

@Composable
private fun CardBackFace(index: Int) {
    val shape = RoundedCornerShape(32.dp)
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(Palette.Grape, Palette.Ink)))
            .border(3.dp, Palette.avatar(index), shape),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Ghost(Modifier.size(140.dp), body = Color.White.copy(alpha = 0.9f))
            Spacer(Modifier.height(12.dp))
            Text("?", fontSize = 64.sp, fontWeight = FontWeight.Black, color = Palette.avatar(index))
        }
    }
}

/** Same look for every role, so a glance from the side reveals nothing. */
@Composable
private fun CardFrontFace(assignment: Assignment) {
    val t = LocalStrings.current
    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(Palette.Grape, Palette.Pink)))
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (assignment.word != null) {
            Text(t.yourWord, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                assignment.word,
                color = Color.White,
                fontSize = when {
                    assignment.word.length > 14 -> 24.sp
                    assignment.word.length > 11 -> 28.sp
                    assignment.word.length > 7 -> 32.sp
                    else -> 46.sp
                },
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )
        } else {
            Ghost(Modifier.size(96.dp))
            Spacer(Modifier.height(8.dp))
            Text(t.youAre, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
            Text(t.mrWhiteRole, color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(12.dp))
            Text(
                t.noWordForYou,
                color = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiscussionScreen(state: UiState, onStartVoting: () -> Unit, onRules: () -> Unit) {
    val t = LocalStrings.current
    Column(
        Modifier
            .fillMaxSize()
            .partyBackground()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Ghost(Modifier.size(96.dp))
        Spacer(Modifier.height(8.dp))
        Text(
            if (state.round == 1) t.whoIsGhost else t.round(state.round),
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            t.discussHint,
            color = Palette.Lilac,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Chip("\uD83C\uDF99\uFE0F ${t.speaksFirst(state.firstSpeaker)}", Palette.Sun)
        Spacer(Modifier.height(20.dp))
        Text(t.stillIn, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.alive.sorted().forEach { i -> Chip(state.deal[i].player, Palette.avatar(i)) }
        }
        Spacer(Modifier.height(32.dp))
        PillButton(t.startVoting, onStartVoting, Modifier.fillMaxWidth())
        TextButton(onClick = onRules) { Text("📖 ${t.rulesButton}", color = Palette.Aqua, fontWeight = FontWeight.Bold) }
    }
}

/** The group counts the votes out loud; one person taps the player who got the most. */
@Composable
private fun VoteScreen(state: UiState, onVote: (Int) -> Unit) {
    val t = LocalStrings.current
    var selected by rememberSaveable { mutableIntStateOf(-1) }
    var confirm by remember { mutableStateOf(false) }
    val candidates = state.alive.sorted()

    Column(
        Modifier
            .fillMaxSize()
            .partyBackground()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(t.timeToVote, fontSize = 30.sp, fontWeight = FontWeight.Black)
        Text(t.voteHint, color = Palette.Lilac)
        Spacer(Modifier.height(12.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(candidates) { i ->
                val picked = i == selected
                Surface(
                    shape = Soft,
                    color = if (picked) Palette.Aqua.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.08f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, if (picked) Palette.Aqua else Color.Transparent, Soft)
                        .clip(Soft)
                        .clickable { selected = i },
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Avatar(state.deal[i].player, i, size = 44.dp)
                        Spacer(Modifier.width(14.dp))
                        Text(state.deal[i].player, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        if (picked) Text("\uD83D\uDDF3\uFE0F", fontSize = 22.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        PillButton(
            if (selected in candidates) t.voteOut(state.deal[selected].player) else t.pickPlayer,
            { confirm = true },
            Modifier.fillMaxWidth(),
            enabled = selected in candidates,
        )
    }

    if (confirm && selected in candidates) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(t.voteOutQuestion(state.deal[selected].player)) },
            text = { Text(t.cantUndo) },
            confirmButton = { TextButton(onClick = { confirm = false; onVote(selected) }) { Text(t.voteOutConfirm) } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text(t.cancel) } },
        )
    }
}

/** Says only whether the player was Mr White. Mr White then gets one guess at the Civilians' word. */
@Composable
private fun ResultScreen(state: UiState, onGuess: (String) -> Unit, onContinue: () -> Unit) {
    val t = LocalStrings.current
    val out = state.eliminated?.let { state.deal.getOrNull(it) } ?: return
    val isMrWhite = out.role == Role.MR_WHITE
    var guess by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .partyBackground()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (isMrWhite) {
            Ghost(Modifier.size(130.dp))
            Spacer(Modifier.height(12.dp))
            Text(t.wasMrWhite(out.player), fontSize = 30.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            when (state.guess) {
                Guess.PENDING -> {
                    Text(
                        t.lastChance(out.player),
                        color = Palette.Lilac,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = guess,
                        onValueChange = { guess = it },
                        placeholder = { Text(t.secretWordHint) },
                        singleLine = true,
                        shape = Pill,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { if (guess.isNotBlank()) onGuess(guess) }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    PillButton(t.guess, { onGuess(guess) }, Modifier.fillMaxWidth(), enabled = guess.isNotBlank(), color = Palette.Aqua, content = Palette.Ink)
                    TextButton(onClick = { onGuess("") }) { Text(t.noIdea, color = Palette.Aqua, fontWeight = FontWeight.Bold) }
                }
                Guess.CORRECT -> {
                    Text("\uD83C\uDF89 ${t.guessRight(state.lastGuess)}", fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(6.dp))
                    Text(t.mrWhiteSteals, color = Palette.Lilac)
                }
                else -> {
                    Text(
                        if (state.lastGuess.isBlank()) t.noGuess else t.guessWrong(state.lastGuess),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(6.dp))
                    // The word stays secret while the game goes on, or the Undercover would learn it.
                    if (state.winner != null) Text(t.wordWas(state.civilianWord), color = Palette.Lilac)
                }
            }
        } else {
            Text("\uD83D\uDE42", fontSize = 80.sp)
            Spacer(Modifier.height(12.dp))
            Text(t.wasNotMrWhite(out.player), fontSize = 30.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(t.huntContinues, color = Palette.Lilac)
        }

        if (state.guess != Guess.PENDING) {
            Spacer(Modifier.height(28.dp))
            PillButton(if (state.winner != null) t.seeResults else t.nextRound, onContinue, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun GameOverScreen(state: UiState, onNewRound: () -> Unit, onSetup: () -> Unit) {
    val t = LocalStrings.current
    val (emoji, title) = when (state.winner) {
        Winner.CIVILIANS -> "\uD83C\uDF89" to t.civiliansWin
        Winner.MR_WHITE -> "\uD83D\uDC7B" to t.mrWhiteWins
        else -> "\uD83D\uDD75\uFE0F" to t.impostorsWin
    }
    Column(
        Modifier
            .fillMaxSize()
            .partyBackground()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(emoji, fontSize = 64.sp)
        Text(title, fontSize = 32.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        state.pair?.let { Text("${it.common}  \u2022  ${it.odd}", color = Palette.Lilac, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(16.dp))
        state.deal.forEachIndexed { i, a ->
            Surface(shape = Soft, color = Color.White.copy(alpha = 0.08f), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(a.player, i, size = 36.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(a.player, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(
                            (a.word ?: t.noWord) + if (i in state.alive) "" else "  \u00B7  ${t.votedOut}",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.6f),
                        )
                    }
                    Chip("${roleEmoji(a.role)} ${t.roleName(a.role)}", roleColor(a.role))
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        PillButton(t.newRound, onNewRound, Modifier.fillMaxWidth())
        TextButton(onClick = onSetup) { Text(t.editPlayers, color = Palette.Aqua, fontWeight = FontWeight.Bold) }
    }
}

/** The rules of the game, in the chosen language. Opened from the setup and discussion screens. */
@Composable
private fun RulesScreen(onBack: () -> Unit) {
    val t = LocalStrings.current
    Column(
        Modifier
            .fillMaxSize()
            .partyBackground()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) {
                Text("\u2190 ${t.back}", color = Palette.Aqua, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
        Text("📖 ${t.rulesTitle}", fontSize = 32.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(12.dp))
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            t.rulesSections.forEach { section ->
                Surface(shape = Soft, color = Color.White.copy(alpha = 0.08f), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${section.emoji}  ${section.title}", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Sun)
                        section.lines.forEach { line ->
                            Row {
                                Text("\u2022", color = Palette.Lilac, modifier = Modifier.width(18.dp))
                                Text(line, color = Color.White.copy(alpha = 0.9f))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
