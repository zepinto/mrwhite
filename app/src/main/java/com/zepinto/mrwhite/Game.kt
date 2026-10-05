package com.zepinto.mrwhite

import android.content.Context
import java.text.Normalizer
import kotlin.random.Random

enum class Role { CIVILIAN, UNDERCOVER, MR_WHITE }

data class WordPair(val common: String, val odd: String)

/** What one player was dealt. [word] is null for Mr White. */
data class Assignment(val player: String, val role: Role, val word: String?)

fun parseWordPairs(text: String): List<WordPair> =
    text.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .mapNotNull { line ->
            val parts = line.split('|')
            if (parts.size == 2) WordPair(parts[0].trim().capitalized(), parts[1].trim().capitalized()) else null
        }
        .toList()

private fun String.capitalized() = replaceFirstChar { it.uppercase() }

object Dealer {
    const val MIN_PLAYERS = 3

    /**
     * The recommended (Undercover, Mr White) for a table size. This is the table of the Intruso game
     * in the joguinhos repository: 2-3 players 1+0, 4-6 players 1+1, 7-8 players 2+1, 9 or more 2+2.
     */
    fun recommended(players: Int): Pair<Int, Int> = when {
        players <= 3 -> 1 to 0
        players <= 6 -> 1 to 1
        players <= 8 -> 2 to 1
        else -> 2 to 2
    }

    /** Civilians must stay the majority, so Undercover plus Mr White can be at most (players - 1) / 2. */
    fun maxImpostors(players: Int) = (players - 1) / 2

    /**
     * The counts actually used: the player's choice (or the recommendation when there is none)
     * squeezed into what the table allows. Mr White is dropped first, and there is always at least one impostor.
     */
    fun resolve(players: Int, undercoverChoice: Int?, mrWhiteChoice: Int?): Pair<Int, Int> {
        val recommended = recommended(players)
        var undercover = (undercoverChoice ?: recommended.first).coerceAtLeast(0)
        var mrWhite = (mrWhiteChoice ?: recommended.second).coerceAtLeast(0)
        val max = maxImpostors(players)
        while (undercover + mrWhite > max) {
            if (mrWhite > 0) mrWhite-- else undercover--
        }
        if (undercover + mrWhite == 0) undercover = 1
        return undercover to mrWhite
    }

    /**
     * Deal the roles. The player at [startIndex] opens the discussion and is never Mr White.
     * Which word of the pair is the "common" one is random.
     */
    fun deal(
        players: List<String>,
        undercoverChoice: Int?,
        mrWhiteChoice: Int?,
        pair: WordPair,
        startIndex: Int,
        rng: Random = Random.Default,
    ): List<Assignment> {
        require(players.size >= MIN_PLAYERS) { "need at least $MIN_PLAYERS players" }
        require(startIndex in players.indices) { "bad start index" }
        val (undercover, mrWhite) = resolve(players.size, undercoverChoice, mrWhiteChoice)
        val swap = rng.nextBoolean()
        val civilianWord = if (swap) pair.odd else pair.common
        val undercoverWord = if (swap) pair.common else pair.odd

        val roles = Array(players.size) { Role.CIVILIAN }
        players.indices.filter { it != startIndex }.shuffled(rng).take(mrWhite).forEach { roles[it] = Role.MR_WHITE }
        players.indices.filter { roles[it] == Role.CIVILIAN }.shuffled(rng).take(undercover).forEach { roles[it] = Role.UNDERCOVER }

        return players.mapIndexed { i, name ->
            Assignment(
                name, roles[i], when (roles[i]) {
                    Role.CIVILIAN -> civilianWord
                    Role.UNDERCOVER -> undercoverWord
                    Role.MR_WHITE -> null
                }
            )
        }
    }
}

/** Hands out word pairs from the asset file of the chosen language, never repeating one until all have been used. */
class WordRepository(private val context: Context) {
    private val all = mutableMapOf<Lang, List<WordPair>>()
    private val unused = mutableMapOf<Lang, ArrayDeque<WordPair>>()

    fun next(lang: Lang): WordPair {
        val pairs = all.getOrPut(lang) {
            context.assets.open(lang.wordFile).bufferedReader().use { parseWordPairs(it.readText()) }
        }
        val queue = unused.getOrPut(lang) { ArrayDeque() }
        if (queue.isEmpty()) queue.addAll(pairs.shuffled())
        return queue.removeFirst()
    }
}

enum class Winner { CIVILIANS, IMPOSTORS, MR_WHITE }

/** Voting-round rules, kept free of Android so they can be unit tested. */
object Rules {
    /** The word the Civilians share, which Mr White must guess when voted out. */
    fun civilianWord(deal: List<Assignment>): String? = deal.firstOrNull { it.role == Role.CIVILIAN }?.word

    /** Ignores case, spaces, punctuation, accents and a trailing plural "s". */
    fun isCorrectGuess(guess: String, word: String): Boolean {
        val g = normalize(guess)
        val w = normalize(word)
        return g.isNotEmpty() && (g == w || g.removeSuffix("s") == w.removeSuffix("s"))
    }

    private fun normalize(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD).filter { it.isLetterOrDigit() }.lowercase()

    /**
     * Who has won with only [alive] players left, or null while play goes on.
     * Civilians win once every Undercover and Mr White is out; the impostors win when two players remain.
     * Mr White's successful guess is a separate win, decided by the caller.
     */
    fun winner(deal: List<Assignment>, alive: Set<Int>): Winner? = when {
        alive.none { deal[it].role != Role.CIVILIAN } -> Winner.CIVILIANS
        alive.size <= 2 -> Winner.IMPOSTORS
        else -> null
    }
}
