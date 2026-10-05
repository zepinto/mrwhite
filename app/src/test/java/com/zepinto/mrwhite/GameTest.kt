package com.zepinto.mrwhite

import java.io.File
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameTest {
    private val pair = WordPair("Coffee", "Tea")

    @Test
    fun recommendedCountsFollowTheJoguinhosTable() {
        assertEquals(1 to 0, Dealer.recommended(3))
        for (n in 4..6) assertEquals("n=$n", 1 to 1, Dealer.recommended(n))
        for (n in 7..8) assertEquals("n=$n", 2 to 1, Dealer.recommended(n))
        for (n in 9..12) assertEquals("n=$n", 2 to 2, Dealer.recommended(n))
    }

    @Test
    fun civiliansAreAlwaysTheMajorityWhateverIsChosen() {
        for (n in Dealer.MIN_PLAYERS..20) {
            val choices = listOf<Int?>(null, 0, 1, 2, 3, 5, 9)
            for (uc in choices) for (mw in choices) {
                val (u, m) = Dealer.resolve(n, uc, mw)
                assertTrue("n=$n uc=$uc mw=$mw gave $u+$m", u + m >= 1)
                assertTrue("n=$n uc=$uc mw=$mw gave $u+$m", n - u - m > u + m)
            }
        }
        // the recommendation for 4 players (1+1) would leave only half civilians, so Mr White is dropped
        assertEquals(1 to 0, Dealer.resolve(4, null, null))
        assertEquals(1 to 1, Dealer.resolve(5, null, null))
        assertEquals(0 to 1, Dealer.resolve(5, 0, 1))
    }

    @Test
    fun dealFollowsCountsAndTheOpenerIsNeverMrWhite() {
        for (n in Dealer.MIN_PLAYERS..12) for (start in 0 until n) repeat(20) { seed ->
            val players = (1..n).map { "P$it" }
            val deal = Dealer.deal(players, null, null, pair, start, Random(seed))
            val (uc, mw) = Dealer.resolve(n, null, null)
            assertEquals(players, deal.map { it.player })
            assertEquals(uc, deal.count { it.role == Role.UNDERCOVER })
            assertEquals(mw, deal.count { it.role == Role.MR_WHITE })
            assertTrue(deal[start].role != Role.MR_WHITE)
            deal.filter { it.role == Role.MR_WHITE }.forEach { assertNull(it.word) }
            val civilianWords = deal.filter { it.role == Role.CIVILIAN }.map { it.word }.toSet()
            assertEquals(1, civilianWords.size)
            deal.filter { it.role == Role.UNDERCOVER }.forEach { assertNotEquals(civilianWords.single(), it.word) }
        }
    }

    @Test
    fun customCountsAreDealtAsChosen() {
        val players = (1..9).map { "P$it" }
        val deal = Dealer.deal(players, 3, 1, pair, 4, Random(1))
        assertEquals(3, deal.count { it.role == Role.UNDERCOVER })
        assertEquals(1, deal.count { it.role == Role.MR_WHITE })
        assertEquals(5, deal.count { it.role == Role.CIVILIAN })
        // asking for too many is squeezed to a civilian majority: 9 players allow at most 4 impostors
        val squeezed = Dealer.deal(players, 6, 6, pair, 0, Random(2))
        assertEquals(4, squeezed.count { it.role != Role.CIVILIAN })
    }

    @Test
    fun everyLanguageHas500CleanUniquePairs() {
        for (lang in Lang.entries) {
            val file = listOf("src/main/assets/${lang.wordFile}", "app/src/main/assets/${lang.wordFile}").map(::File).first { it.exists() }
            val pairs = parseWordPairs(file.readText())
            assertEquals("${lang.code} pair count", 500, pairs.size)
            assertEquals("${lang.code} unique pairs", 500, pairs.toSet().size)
            pairs.forEach { assertNotEquals("${lang.code}: ${it.common}", it.common.lowercase(), it.odd.lowercase()) }
            val words = pairs.flatMap { listOf(it.common.lowercase(), it.odd.lowercase()) }
            assertEquals("${lang.code} words used twice", words.size, words.toSet().size)
        }
    }

    @Test
    fun everyLanguageHasTextsAndAFlag() {
        for (lang in Lang.entries) {
            assertTrue(lang.flag.isNotEmpty() && lang.label.isNotEmpty())
            assertTrue(lang.strings.tapToFlip("Ana").contains("Ana"))
            assertTrue(lang.strings.passTo("Ana").contains("Ana"))
        }
        assertEquals(Lang.EN, Lang.fromCode("en"))
        assertEquals(null, Lang.fromCode("fr"))
    }
}

class RulesTest {
    private fun deal(vararg roles: Role) = roles.mapIndexed { i, r ->
        Assignment("P$i", r, if (r == Role.MR_WHITE) null else if (r == Role.CIVILIAN) "Coffee" else "Tea")
    }

    @Test
    fun guessIgnoresCaseSpacesAccentsAndPlural() {
        assertTrue(Rules.isCorrectGuess("  coffee ", "Coffee"))
        assertTrue(Rules.isCorrectGuess("COFFEES", "Coffee"))
        assertTrue(Rules.isCorrectGuess("ice-cream", "Ice cream"))
        assertTrue(Rules.isCorrectGuess("café", "Cafe"))
        assertTrue(!Rules.isCorrectGuess("tea", "Coffee"))
        assertTrue(!Rules.isCorrectGuess("", "Coffee"))
        assertTrue(!Rules.isCorrectGuess("   ", "Coffee"))
    }

    @Test
    fun civiliansWinOnceAllImpostorsAreOut() {
        val d = deal(Role.MR_WHITE, Role.UNDERCOVER, Role.CIVILIAN, Role.CIVILIAN, Role.CIVILIAN)
        assertEquals(null, Rules.winner(d, setOf(0, 1, 2, 3, 4)))
        assertEquals(null, Rules.winner(d, setOf(1, 2, 3, 4))) // Mr White out, Undercover still hiding
        assertEquals(Winner.CIVILIANS, Rules.winner(d, setOf(2, 3, 4)))
    }

    @Test
    fun impostorsWinWhenTwoRemain() {
        val d = deal(Role.MR_WHITE, Role.CIVILIAN, Role.CIVILIAN, Role.CIVILIAN)
        assertEquals(null, Rules.winner(d, setOf(0, 1, 2)))
        assertEquals(Winner.IMPOSTORS, Rules.winner(d, setOf(0, 1)))
        // three players: voting out a civilian leaves Mr White with one civilian
        val three = deal(Role.MR_WHITE, Role.CIVILIAN, Role.CIVILIAN)
        assertEquals(Winner.IMPOSTORS, Rules.winner(three, setOf(0, 2)))
        assertEquals(Winner.CIVILIANS, Rules.winner(three, setOf(1, 2)))
    }

    @Test
    fun severalImpostorsAreAllRequiredToBeVotedOut() {
        // 9 players: 2 Undercover (0,1) + 2 Mr White (2,3) + 5 civilians
        val d = deal(Role.UNDERCOVER, Role.UNDERCOVER, Role.MR_WHITE, Role.MR_WHITE,
            Role.CIVILIAN, Role.CIVILIAN, Role.CIVILIAN, Role.CIVILIAN, Role.CIVILIAN)
        assertEquals(null, Rules.winner(d, setOf(0, 1, 2, 4, 5, 6, 7, 8)))   // one Mr White out
        assertEquals(null, Rules.winner(d, setOf(0, 4, 5, 6, 7, 8)))        // 3 impostors out, 1 left
        assertEquals(Winner.CIVILIANS, Rules.winner(d, setOf(4, 5, 6, 7, 8)))
        assertEquals(Winner.IMPOSTORS, Rules.winner(d, setOf(0, 4)))
    }

    @Test
    fun civilianWordIsTheCommonWord() {
        assertEquals("Coffee", Rules.civilianWord(deal(Role.MR_WHITE, Role.UNDERCOVER, Role.CIVILIAN, Role.CIVILIAN)))
    }
}
