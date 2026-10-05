package com.zepinto.mrwhite

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class Stage {
    SETUP,
    /** One card at a time, face down, with its owner's name under it. */
    TABLE,
    DISCUSSION,
    /** Everybody picks who to vote out. */
    VOTE,
    /** Result of the vote; Mr White gets one guess here. */
    ELIMINATED,
    GAME_OVER,
}

/** Mr White's guess after being voted out. */
enum class Guess { PENDING, CORRECT, WRONG }

data class UiState(
    val players: List<String> = emptyList(),
    /** The player's chosen counts; null means "use the recommendation for this table size". */
    val undercoverChoice: Int? = null,
    val mrWhiteChoice: Int? = null,
    val lang: Lang = Lang.EN,
    val stage: Stage = Stage.SETUP,
    val deal: List<Assignment> = emptyList(),
    val pair: WordPair? = null,
    val firstSpeaker: String = "",
    /** Index in [players] of the player who opens the discussion (rotates every game, never Mr White). */
    val startIndex: Int = 0,
    /** The order in which the phone goes round: indices into [deal], shuffled. */
    val order: List<Int> = emptyList(),
    /** Which entry of [order] is on screen. */
    val position: Int = 0,
    /** The card on screen is face up. */
    val cardUp: Boolean = false,
    /** Deal indices of cards that were flipped at least once, even if hidden again straight away. */
    val seen: Set<Int> = emptySet(),
    /** Indices of players still in the game. */
    val alive: Set<Int> = emptySet(),
    val round: Int = 1,
    /** The player voted out most recently. */
    val eliminated: Int? = null,
    /** Set only when the player voted out was Mr White. */
    val guess: Guess? = null,
    val lastGuess: String = "",
    val winner: Winner? = null,
) {
    /** Deal index of the card on screen. */
    val current get() = order[position]
    val onLastCard get() = position == order.lastIndex
    val civilianWord get() = Rules.civilianWord(deal).orEmpty()

    /** (Undercover, Mr White) that will be dealt for the current player list. */
    val counts get() = Dealer.resolve(players.size, undercoverChoice, mrWhiteChoice)
}

/** All calls come from the main thread (Compose), so no locking is needed. */
class GameViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("mrwhite", Context.MODE_PRIVATE)
    private val words = WordRepository(app)
    private val sfx = Sfx()

    private val _state = MutableStateFlow(
        UiState(
            players = prefs.getString(KEY_PLAYERS, "").orEmpty().split('\n')
                .filter { it.isNotBlank() }.distinctBy { it.lowercase() },
            undercoverChoice = prefs.getInt(KEY_UC, -1).takeIf { it >= 0 },
            mrWhiteChoice = prefs.getInt(KEY_MW, -1).takeIf { it >= 0 },
            lang = Lang.fromCode(prefs.getString(KEY_LANG, null)) ?: Lang.deviceDefault(),
        )
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    // ---- setup ----

    fun addPlayer(raw: String): Boolean {
        val name = raw.trim().take(MAX_NAME_LENGTH)
        val s = _state.value
        if (name.isEmpty() || s.players.size >= MAX_PLAYERS || s.players.any { it.equals(name, ignoreCase = true) }) return false
        _state.update { it.copy(players = it.players + name) }
        savePrefs()
        return true
    }

    fun removePlayer(index: Int) {
        _state.update { it.copy(players = it.players.filterIndexed { i, _ -> i != index }) }
        savePrefs()
    }

    fun setLanguage(lang: Lang) {
        _state.update { it.copy(lang = lang) }
        savePrefs()
    }

    /**
     * Change the number of Undercover and/or Mr White by the given steps.
     * Refused when it would leave no impostor or when the Civilians would no longer be the majority.
     */
    fun adjustImpostors(undercoverStep: Int, mrWhiteStep: Int) {
        val s = _state.value
        val (undercover, mrWhite) = s.counts
        val newUndercover = undercover + undercoverStep
        val newMrWhite = mrWhite + mrWhiteStep
        val total = newUndercover + newMrWhite
        if (newUndercover < 0 || newMrWhite < 0 || total < 1 || total > Dealer.maxImpostors(s.players.size)) return
        _state.update { it.copy(undercoverChoice = newUndercover, mrWhiteChoice = newMrWhite) }
        savePrefs()
    }

    // ---- round ----

    fun startGame() {
        val s = _state.value
        if (s.players.size < Dealer.MIN_PLAYERS) return
        val pair = words.next(s.lang)
        // The opening player rotates every game (as in the joguinhos Intruso game) and is never Mr White.
        val start = (prefs.getInt(KEY_START, -1) + 1).mod(s.players.size)
        prefs.edit().putInt(KEY_START, start).apply()
        val deal = Dealer.deal(s.players, s.undercoverChoice, s.mrWhiteChoice, pair, start)
        _state.update {
            it.copy(
                stage = Stage.TABLE,
                deal = deal,
                pair = pair,
                firstSpeaker = s.players[start],
                startIndex = start,
                order = deal.indices.shuffled(),
                position = 0,
                cardUp = false,
                seen = emptySet(),
                alive = deal.indices.toSet(),
                round = 1,
                eliminated = null,
                guess = null,
                lastGuess = "",
                winner = null,
            )
        }
    }

    /**
     * Tap on the card: flip it up, or back down if it is already up.
     * Flipping a card for the first time plays a chime. Flipping one that was already flipped plays a harsh buzz,
     * so someone peeking before the phone has been handed over is audible to the whole table.
     */
    fun flip() {
        val s = _state.value
        if (s.stage != Stage.TABLE || s.order.isEmpty()) return
        if (s.cardUp) {
            _state.update { it.copy(cardUp = false) }
        } else {
            val card = s.current
            sfx.play(if (card in s.seen) Sfx.Kind.WARNING else Sfx.Kind.REVEAL)
            _state.update { it.copy(cardUp = true, seen = it.seen + card) }
        }
    }

    /** Turn the card face down (also done whenever the app leaves the foreground). */
    fun hide() {
        if (_state.value.cardUp) _state.update { it.copy(cardUp = false) }
    }

    /** Pass the phone on. Only allowed once this card has been looked at and is face down again. */
    fun next() {
        val s = _state.value
        if (s.stage != Stage.TABLE || s.cardUp || s.current !in s.seen) return
        if (s.onLastCard) _state.update { it.copy(stage = Stage.DISCUSSION) }
        else _state.update { it.copy(position = it.position + 1) }
    }

    fun startVoting() {
        if (_state.value.stage == Stage.DISCUSSION) _state.update { it.copy(stage = Stage.VOTE) }
    }

    /** Remove a player from the game. Only Mr White is announced as such; everybody else is just "not Mr White". */
    fun voteOut(index: Int) {
        val s = _state.value
        if (s.stage != Stage.VOTE || index !in s.alive) return
        val alive = s.alive - index
        val isMrWhite = s.deal[index].role == Role.MR_WHITE
        _state.update {
            it.copy(
                stage = Stage.ELIMINATED,
                alive = alive,
                eliminated = index,
                guess = if (isMrWhite) Guess.PENDING else null,
                lastGuess = "",
                winner = if (isMrWhite) null else Rules.winner(s.deal, alive),
            )
        }
    }

    /** Mr White's one guess at the Civilians' word. A blank guess counts as a miss. */
    fun guessWord(text: String) {
        val s = _state.value
        if (s.stage != Stage.ELIMINATED || s.guess != Guess.PENDING) return
        val correct = Rules.isCorrectGuess(text, s.civilianWord)
        _state.update {
            it.copy(
                guess = if (correct) Guess.CORRECT else Guess.WRONG,
                lastGuess = text.trim(),
                winner = if (correct) Winner.MR_WHITE else Rules.winner(s.deal, s.alive),
            )
        }
    }

    /** Leave the result screen: on to the results if somebody has won, otherwise another discussion round. */
    fun continueAfterElimination() {
        val s = _state.value
        if (s.stage != Stage.ELIMINATED || s.guess == Guess.PENDING) return
        if (s.winner != null) {
            _state.update { it.copy(stage = Stage.GAME_OVER) }
        } else {
            _state.update {
                it.copy(
                    stage = Stage.DISCUSSION,
                    round = it.round + 1,
                    eliminated = null,
                    guess = null,
                    // The opener of the game speaks first again, or the next player still in, going round the table.
                    firstSpeaker = it.deal[(0 until it.deal.size).map { k -> (it.startIndex + k) % it.deal.size }.first { i -> i in it.alive }].player,
                )
            }
        }
    }

    fun newRound() = startGame()

    fun backToSetup() {
        _state.update {
            it.copy(stage = Stage.SETUP, cardUp = false, order = emptyList(), position = 0, seen = emptySet(), deal = emptyList(), alive = emptySet(), eliminated = null, guess = null, winner = null)
        }
    }

    private fun savePrefs() {
        val s = _state.value
        prefs.edit()
            .putString(KEY_PLAYERS, s.players.joinToString("\n"))
            .putInt(KEY_UC, s.undercoverChoice ?: -1)
            .putInt(KEY_MW, s.mrWhiteChoice ?: -1)
            .putString(KEY_LANG, s.lang.code)
            .apply()
    }

    override fun onCleared() {
        sfx.release()
    }

    companion object {
        const val MAX_PLAYERS = 20
        const val MAX_NAME_LENGTH = 20
        private const val KEY_PLAYERS = "players"
        private const val KEY_UC = "undercover_count"
        private const val KEY_MW = "mrwhite_count"
        private const val KEY_START = "starting_player"
        private const val KEY_LANG = "lang"
    }
}
