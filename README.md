# Mr White

A party game for one phone, passed around the table. Everybody gets a secret word, but not
everybody gets the same one. Describe your word without giving it away, then vote out the impostors.

Native Android app (Kotlin, Jetpack Compose). Available in **English, Portuguese and Spanish**,
each with its own list of 500 word pairs. No network, no permissions, no ads.

## How it plays

- **Civilians** all get the same word. **Undercover** players get a similar but different word and do not
  know it. **Mr White** gets no word and has to bluff.
- The phone shows **one card at a time**, face down, with the player's name under it. Only that player
  flips it (chime), memorizes the word, hides it and passes the phone on. Flipping a card that was
  already flipped plays a harsh buzz, so peeking is audible to the whole table.
- Players take turns describing their word, then vote. The app only tells you whether the player voted
  out was Mr White. Mr White then gets one guess at the Civilians' word.
- **Roles:** the recommended number of impostors (Undercover + Mr White) follows the table of the
  Intruso game in [joguinhos](https://github.com/zepinto/joguinhos): 3 players 1+0, 4-6 players 1+1,
  7-8 players 2+1, 9 or more 2+2. The counts can be changed on the first screen as long as Civilians
  stay the majority. The player who opens the discussion is never Mr White and changes every game.
- **Winning:** Civilians win when every impostor has been voted out. The impostors win when only 2 players
  are left. Mr White wins straight away by guessing the Civilians' word.

The full rules are also inside the app (the **?** button).

## Build

Requirements: JDK 17 or newer and the Android SDK (platform 35).

```sh
echo "sdk.dir=$HOME/Android/Sdk" > local.properties   # path to your Android SDK
./gradlew testDebugUnitTest assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The app blocks screenshots and the recents thumbnail so that nobody can capture a card.
For emulator test builds where you need screenshots, build with `-PallowScreenshots`:

```sh
./gradlew assembleDebug -PallowScreenshots
```

## Layout

| Path | What |
|---|---|
| `app/src/main/java/com/zepinto/mrwhite/Game.kt` | Roles, dealing, voting and win rules (plain Kotlin, unit tested) |
| `.../GameViewModel.kt` | Game state machine |
| `.../Screens.kt`, `Theme.kt` | Compose UI and look |
| `.../Strings.kt` | All texts in the three languages, including the rules page |
| `.../Sfx.kt` | The two sounds, synthesised at start-up |
| `app/src/main/assets/word_pairs_{en,pt,es}.txt` | 500 word pairs per language, one `word|similar word` per line |
