package com.zepinto.mrwhite

import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale

/** Languages the game is available in. Each has its own UI texts and its own word list in assets. */
enum class Lang(val code: String, val flag: String, val label: String) {
    EN("en", "🇬🇧", "English"),
    PT("pt", "🇵🇹", "Português"),
    ES("es", "🇪🇸", "Español");

    val wordFile get() = "word_pairs_$code.txt"

    val strings: Strings
        get() = when (this) {
            EN -> English
            PT -> Portuguese
            ES -> Spanish
        }

    companion object {
        fun fromCode(code: String?) = entries.firstOrNull { it.code == code }

        /** The phone's language if the game has it, English otherwise. */
        fun deviceDefault() = fromCode(Locale.getDefault().language) ?: EN
    }
}

val LocalStrings = staticCompositionLocalOf { English }

/** One block of the rules page: a heading and its lines. */
data class RulesSection(val emoji: String, val title: String, val lines: List<String>)

/** Every user-visible text. Adding a field forces all three languages to provide it. */
data class Strings(
    val tagline: String,
    val whoIsPlaying: String,
    val add: String,
    val nameTaken: String,
    val maxPlayers: (Int) -> String,
    val undercover: String,
    val mrWhiteLabel: String,
    val majorityHint: String,
    val mrWhiteChip: (Int) -> String,
    val undercoverChip: (Int) -> String,
    val civiliansChip: (Int) -> String,
    val addAtLeast: (Int) -> String,
    val volumeHint: String,
    val dealCards: String,

    val leaveTitle: String,
    val leaveText: String,
    val leave: String,
    val stay: String,

    val cardOf: (Int, Int) -> String,
    val memorize: String,
    val hiddenPass: String,
    val tapToFlip: (String) -> String,
    val hideCard: String,
    val passTo: (String) -> String,
    val startDiscussion: String,
    val yourWord: String,
    val youAre: String,
    val noWordForYou: String,

    val whoIsGhost: String,
    val round: (Int) -> String,
    val discussHint: String,
    val speaksFirst: (String) -> String,
    val stillIn: String,
    val startVoting: String,

    val timeToVote: String,
    val voteHint: String,
    val pickPlayer: String,
    val voteOut: (String) -> String,
    val voteOutQuestion: (String) -> String,
    val cantUndo: String,
    val voteOutConfirm: String,
    val cancel: String,

    val wasMrWhite: (String) -> String,
    val lastChance: (String) -> String,
    val secretWordHint: String,
    val guess: String,
    val noIdea: String,
    val guessRight: (String) -> String,
    val mrWhiteSteals: String,
    val noGuess: String,
    val guessWrong: (String) -> String,
    val wordWas: (String) -> String,
    val wasNotMrWhite: (String) -> String,
    val huntContinues: String,
    val seeResults: String,
    val nextRound: String,

    val civiliansWin: String,
    val mrWhiteWins: String,
    val impostorsWin: String,
    val noWord: String,
    val votedOut: String,
    val civilian: String,
    val undercoverRole: String,
    val mrWhiteRole: String,
    val newRound: String,
    val editPlayers: String,

    val rulesButton: String,
    val rulesTitle: String,
    val back: String,
    val rulesSections: List<RulesSection>,
) {
    fun roleName(role: Role) = when (role) {
        Role.CIVILIAN -> civilian
        Role.UNDERCOVER -> undercoverRole
        Role.MR_WHITE -> mrWhiteRole
    }
}

val English = Strings(
    tagline = "One phone. One secret word.",
    whoIsPlaying = "Who's playing?",
    add = "Add",
    nameTaken = "That name is already on the list",
    maxPlayers = { "That's the maximum of $it players" },
    undercover = "Undercover",
    mrWhiteLabel = "Mr White",
    majorityHint = "Civilians must always be the majority.",
    mrWhiteChip = { "$it Mr White" },
    undercoverChip = { "$it Undercover" },
    civiliansChip = { "$it Civilians" },
    addAtLeast = { "Add at least $it players to start" },
    volumeHint = "Turn the volume up: every card flip makes a sound.",
    dealCards = "Deal the cards",

    leaveTitle = "Leave this game?",
    leaveText = "The current round will be lost.",
    leave = "Leave",
    stay = "Stay",

    cardOf = { i, n -> "Card $i of $n" },
    memorize = "Memorize it, then hide the card.",
    hiddenPass = "Hidden. Pass the phone on.",
    tapToFlip = { "Tap the card to flip it. Only $it should look." },
    hideCard = "Hide card",
    passTo = { "Pass to $it" },
    startDiscussion = "Start the discussion",
    yourWord = "YOUR WORD",
    youAre = "YOU ARE",
    noWordForYou = "No word for you. Listen closely and bluff your way through!",

    whoIsGhost = "Who's the ghost?",
    round = { "Round $it" },
    discussHint = "Take turns describing your word without saying it. Then vote.",
    speaksFirst = { "$it speaks first" },
    stillIn = "Still in the game",
    startVoting = "Start voting",

    timeToVote = "Time to vote!",
    voteHint = "Count the votes together, then tap the player who got the most.",
    pickPlayer = "Pick a player",
    voteOut = { "Vote out $it" },
    voteOutQuestion = { "Vote out $it?" },
    cantUndo = "This can't be undone.",
    voteOutConfirm = "Vote out",
    cancel = "Cancel",

    wasMrWhite = { "$it was Mr White!" },
    lastChance = { "Last chance, $it: guess the Civilians' secret word to win." },
    secretWordHint = "The secret word is...",
    guess = "Guess",
    noIdea = "No idea",
    guessRight = { "\"$it\" is right!" },
    mrWhiteSteals = "Mr White steals the win.",
    noGuess = "No guess.",
    guessWrong = { "\"$it\" is wrong." },
    wordWas = { "The word was $it." },
    wasNotMrWhite = { "$it was not Mr White" },
    huntContinues = "The hunt goes on...",
    seeResults = "See the results",
    nextRound = "Next round",

    civiliansWin = "Civilians win!",
    mrWhiteWins = "Mr White wins!",
    impostorsWin = "The impostors win!",
    noWord = "no word",
    votedOut = "voted out",
    civilian = "Civilian",
    undercoverRole = "Undercover",
    mrWhiteRole = "Mr White",
    newRound = "New round",
    editPlayers = "Edit players",

    rulesButton = "Rules",
    rulesTitle = "Rules",
    back = "Back",
    rulesSections = listOf(
        RulesSection("🎯", "The idea", listOf(
            "Everyone gets a secret word, except one or more players who get nothing.",
            "Civilians all get the same word.",
            "Undercover players get a similar but different word. They don't know they are different.",
            "Mr White gets no word at all and has to bluff.",
        )),
        RulesSection("👥", "Roles", listOf(
            "The game deals the roles by itself. Recommended number of impostors (Undercover + Mr White): 3 players 1+0, 4 to 6 players 1+1, 7 to 8 players 2+1, 9 or more 2+2.",
            "You can change the numbers on the first screen, but Civilians must always be the majority. With 4 players that means a single impostor.",
            "The player who opens the discussion is never Mr White, and it changes every game.",
        )),
        RulesSection("🃏", "The cards", listOf(
            "The phone shows one card at a time, face down, with the player's name under it.",
            "Only that player taps the card to flip it, memorizes the word and hides it again.",
            "Then pass the phone on. Every flip makes a sound: a chime for the first look, a harsh buzz if a card is flipped again. If you hear the buzz, someone peeked!",
        )),
        RulesSection("🎙️", "Discussion", listOf(
            "Take turns saying a word or a short phrase that describes your word.",
            "Don't be too obvious, or the Undercover and Mr White will work out the word. Don't be too vague, or you will look suspicious.",
            "Mr White has to listen closely and bluff.",
        )),
        RulesSection("🗳️", "Voting", listOf(
            "When everybody has spoken, vote together for who you think is an impostor.",
            "Tap the player who got the most votes. The app only tells you whether that player was Mr White. Civilians and Undercover are not revealed.",
            "Then the game goes on with a new round of discussion, unless somebody has already won.",
        )),
        RulesSection("👻", "Mr White's last chance", listOf(
            "If Mr White is voted out, they get one guess at the Civilians' word.",
            "A correct guess wins the game for Mr White. A wrong guess means Mr White is out and the game goes on.",
        )),
        RulesSection("🏆", "Who wins", listOf(
            "Civilians win when every Undercover and Mr White has been voted out.",
            "The impostors win when only 2 players are left.",
            "Mr White wins straight away by guessing the Civilians' word.",
        )),
    ),
)

val Portuguese = Strings(
    tagline = "Um telemóvel. Uma palavra secreta.",
    whoIsPlaying = "Quem vai jogar?",
    add = "Adicionar",
    nameTaken = "Esse nome já está na lista",
    maxPlayers = { "O máximo é $it jogadores" },
    undercover = "Intrusos",
    mrWhiteLabel = "Mr White",
    majorityHint = "Os civis têm de ser sempre a maioria.",
    mrWhiteChip = { "$it Mr White" },
    undercoverChip = { if (it == 1) "1 Intruso" else "$it Intrusos" },
    civiliansChip = { "$it Civis" },
    addAtLeast = { "Adiciona pelo menos $it jogadores para começar" },
    volumeHint = "Sobe o volume: cada carta virada faz um som.",
    dealCards = "Distribuir as cartas",

    leaveTitle = "Sair do jogo?",
    leaveText = "A ronda atual será perdida.",
    leave = "Sair",
    stay = "Ficar",

    cardOf = { i, n -> "Carta $i de $n" },
    memorize = "Memoriza e depois esconde a carta.",
    hiddenPass = "Escondida. Passa o telemóvel.",
    tapToFlip = { "Toca na carta para a virar. Só $it deve olhar." },
    hideCard = "Esconder carta",
    passTo = { "Passar para $it" },
    startDiscussion = "Começar a discussão",
    yourWord = "A TUA PALAVRA",
    youAre = "TU ÉS",
    noWordForYou = "Não tens palavra. Ouve com atenção e disfarça!",

    whoIsGhost = "Quem é o fantasma?",
    round = { "Ronda $it" },
    discussHint = "Descrevam a vossa palavra à vez, sem a dizer. Depois votem.",
    speaksFirst = { "$it fala primeiro" },
    stillIn = "Ainda em jogo",
    startVoting = "Começar a votação",

    timeToVote = "Hora de votar!",
    voteHint = "Contem os votos em conjunto e toquem no jogador mais votado.",
    pickPlayer = "Escolhe um jogador",
    voteOut = { "Eliminar $it" },
    voteOutQuestion = { "Eliminar $it?" },
    cantUndo = "Esta ação não pode ser desfeita.",
    voteOutConfirm = "Eliminar",
    cancel = "Cancelar",

    wasMrWhite = { "$it era o Mr White!" },
    lastChance = { "Última hipótese, $it: adivinha a palavra secreta dos Civis para ganhar." },
    secretWordHint = "A palavra secreta é...",
    guess = "Adivinhar",
    noIdea = "Não faço ideia",
    guessRight = { "\"$it\" é a palavra certa!" },
    mrWhiteSteals = "O Mr White rouba a vitória.",
    noGuess = "Sem resposta.",
    guessWrong = { "\"$it\" não é a palavra." },
    wordWas = { "A palavra era $it." },
    wasNotMrWhite = { "$it não era o Mr White" },
    huntContinues = "A caçada continua...",
    seeResults = "Ver resultados",
    nextRound = "Próxima ronda",

    civiliansWin = "Os Civis ganham!",
    mrWhiteWins = "O Mr White ganha!",
    impostorsWin = "Os impostores ganham!",
    noWord = "sem palavra",
    votedOut = "eliminado",
    civilian = "Civil",
    undercoverRole = "Intruso",
    mrWhiteRole = "Mr White",
    newRound = "Nova ronda",
    editPlayers = "Editar jogadores",

    rulesButton = "Regras",
    rulesTitle = "Regras",
    back = "Voltar",
    rulesSections = listOf(
        RulesSection("🎯", "A ideia", listOf(
            "Todos recebem uma palavra secreta, exceto um ou mais jogadores que não recebem nenhuma.",
            "Os civis recebem todos a mesma palavra.",
            "Os intrusos recebem uma palavra semelhante mas diferente. Não sabem que são diferentes.",
            "O Mr White não recebe palavra nenhuma e tem de disfarçar.",
        )),
        RulesSection("👥", "Papéis", listOf(
            "O jogo distribui os papéis sozinho. Número recomendado de impostores (Intrusos + Mr White): 3 jogadores 1+0, 4 a 6 jogadores 1+1, 7 a 8 jogadores 2+1, 9 ou mais 2+2.",
            "Podes alterar os números no primeiro ecrã, mas os civis têm de ser sempre a maioria. Com 4 jogadores, isso significa apenas um impostor.",
            "O jogador que abre a discussão nunca é o Mr White e muda a cada jogo.",
        )),
        RulesSection("🃏", "As cartas", listOf(
            "O telemóvel mostra uma carta de cada vez, virada para baixo, com o nome do jogador por baixo.",
            "Só esse jogador toca na carta para a virar, memoriza a palavra e volta a escondê-la.",
            "Depois passa o telemóvel. Cada carta virada faz um som: um toque agradável na primeira vez e um zumbido forte se a carta for virada outra vez. Se ouvires o zumbido, alguém espreitou!",
        )),
        RulesSection("🎙️", "Discussão", listOf(
            "À vez, cada jogador diz uma palavra ou uma frase curta que descreva a sua palavra.",
            "Não sejas demasiado óbvio, ou os intrusos e o Mr White descobrem a palavra. Não sejas vago demais, ou pareces suspeito.",
            "O Mr White tem de ouvir com atenção e disfarçar.",
        )),
        RulesSection("🗳️", "Votação", listOf(
            "Depois de todos falarem, votem juntos em quem acham que é um impostor.",
            "Toca no jogador mais votado. A app só diz se esse jogador era o Mr White. Civis e intrusos não são revelados.",
            "Depois o jogo continua com uma nova ronda de discussão, a não ser que alguém já tenha ganho.",
        )),
        RulesSection("👻", "A última hipótese do Mr White", listOf(
            "Se o Mr White for eliminado, tem uma tentativa para adivinhar a palavra dos civis.",
            "Se acertar, o Mr White ganha o jogo. Se errar, fica eliminado e o jogo continua.",
        )),
        RulesSection("🏆", "Quem ganha", listOf(
            "Os civis ganham quando todos os intrusos e o Mr White forem eliminados.",
            "Os impostores ganham quando restarem apenas 2 jogadores.",
            "O Mr White ganha logo se adivinhar a palavra dos civis.",
        )),
    ),
)

val Spanish = Strings(
    tagline = "Un móvil. Una palabra secreta.",
    whoIsPlaying = "¿Quién juega?",
    add = "Añadir",
    nameTaken = "Ese nombre ya está en la lista",
    maxPlayers = { "El máximo es de $it jugadores" },
    undercover = "Infiltrados",
    mrWhiteLabel = "Mr White",
    majorityHint = "Los civiles deben ser siempre mayoría.",
    mrWhiteChip = { "$it Mr White" },
    undercoverChip = { if (it == 1) "1 Infiltrado" else "$it Infiltrados" },
    civiliansChip = { "$it Civiles" },
    addAtLeast = { "Añade al menos $it jugadores para empezar" },
    volumeHint = "Sube el volumen: cada carta que se gira hace un sonido.",
    dealCards = "Repartir las cartas",

    leaveTitle = "¿Salir de la partida?",
    leaveText = "Se perderá la ronda actual.",
    leave = "Salir",
    stay = "Quedarse",

    cardOf = { i, n -> "Carta $i de $n" },
    memorize = "Memorízala y luego oculta la carta.",
    hiddenPass = "Oculta. Pasa el móvil.",
    tapToFlip = { "Toca la carta para girarla. Solo $it debe mirar." },
    hideCard = "Ocultar carta",
    passTo = { "Pasar a $it" },
    startDiscussion = "Empezar el debate",
    yourWord = "TU PALABRA",
    youAre = "ERES",
    noWordForYou = "No tienes palabra. ¡Escucha con atención y disimula!",

    whoIsGhost = "¿Quién es el fantasma?",
    round = { "Ronda $it" },
    discussHint = "Describid vuestra palabra por turnos, sin decirla. Luego votad.",
    speaksFirst = { "$it habla primero" },
    stillIn = "Siguen en juego",
    startVoting = "Empezar la votación",

    timeToVote = "¡Hora de votar!",
    voteHint = "Contad los votos juntos y tocad al jugador más votado.",
    pickPlayer = "Elige un jugador",
    voteOut = { "Eliminar a $it" },
    voteOutQuestion = { "¿Eliminar a $it?" },
    cantUndo = "No se puede deshacer.",
    voteOutConfirm = "Eliminar",
    cancel = "Cancelar",

    wasMrWhite = { "¡$it era Mr White!" },
    lastChance = { "Última oportunidad, $it: adivina la palabra secreta de los Civiles para ganar." },
    secretWordHint = "La palabra secreta es...",
    guess = "Adivinar",
    noIdea = "Ni idea",
    guessRight = { "¡\"$it\" es la palabra correcta!" },
    mrWhiteSteals = "Mr White se lleva la victoria.",
    noGuess = "Sin respuesta.",
    guessWrong = { "\"$it\" no es la palabra." },
    wordWas = { "La palabra era $it." },
    wasNotMrWhite = { "$it no era Mr White" },
    huntContinues = "La caza continúa...",
    seeResults = "Ver resultados",
    nextRound = "Siguiente ronda",

    civiliansWin = "¡Ganan los Civiles!",
    mrWhiteWins = "¡Gana Mr White!",
    impostorsWin = "¡Ganan los impostores!",
    noWord = "sin palabra",
    votedOut = "eliminado",
    civilian = "Civil",
    undercoverRole = "Infiltrado",
    mrWhiteRole = "Mr White",
    newRound = "Nueva ronda",
    editPlayers = "Editar jugadores",

    rulesButton = "Reglas",
    rulesTitle = "Reglas",
    back = "Volver",
    rulesSections = listOf(
        RulesSection("🎯", "La idea", listOf(
            "Todos reciben una palabra secreta, salvo uno o más jugadores que no reciben ninguna.",
            "Los civiles reciben todos la misma palabra.",
            "Los infiltrados reciben una palabra parecida pero distinta. No saben que son diferentes.",
            "Mr White no recibe ninguna palabra y tiene que disimular.",
        )),
        RulesSection("👥", "Roles", listOf(
            "El juego reparte los roles solo. Número recomendado de impostores (Infiltrados + Mr White): 3 jugadores 1+0, de 4 a 6 jugadores 1+1, de 7 a 8 jugadores 2+1, 9 o más 2+2.",
            "Puedes cambiar los números en la primera pantalla, pero los civiles deben ser siempre mayoría. Con 4 jugadores, eso significa un solo impostor.",
            "El jugador que abre el debate nunca es Mr White y cambia en cada partida.",
        )),
        RulesSection("🃏", "Las cartas", listOf(
            "El móvil muestra una carta cada vez, boca abajo, con el nombre del jugador debajo.",
            "Solo ese jugador toca la carta para girarla, memoriza la palabra y la vuelve a ocultar.",
            "Después pasa el móvil. Cada carta girada suena: un tintineo la primera vez y un zumbido fuerte si se gira otra vez. ¡Si oyes el zumbido, alguien ha mirado!",
        )),
        RulesSection("🎙️", "Debate", listOf(
            "Por turnos, cada jugador dice una palabra o una frase corta que describa su palabra.",
            "No seas demasiado obvio, o los infiltrados y Mr White descubrirán la palabra. No seas demasiado vago, o parecerás sospechoso.",
            "Mr White tiene que escuchar con atención y disimular.",
        )),
        RulesSection("🗳️", "Votación", listOf(
            "Cuando todos hayan hablado, votad juntos a quién creéis que es un impostor.",
            "Toca al jugador más votado. La app solo dice si era Mr White. Los civiles y los infiltrados no se revelan.",
            "Después el juego sigue con una nueva ronda de debate, salvo que alguien ya haya ganado.",
        )),
        RulesSection("👻", "La última oportunidad de Mr White", listOf(
            "Si eliminan a Mr White, tiene un intento para adivinar la palabra de los civiles.",
            "Si acierta, Mr White gana la partida. Si falla, queda eliminado y el juego continúa.",
        )),
        RulesSection("🏆", "Quién gana", listOf(
            "Los civiles ganan cuando todos los infiltrados y Mr White han sido eliminados.",
            "Los impostores ganan cuando quedan solo 2 jugadores.",
            "Mr White gana al instante si adivina la palabra de los civiles.",
        )),
    ),
)
