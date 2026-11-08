package com.escapeshift.game

/** World 1 narrative, separated from the platform physics and from Android. */
enum class EscapePuzzle { CODE, SYMBOLS, ITEM, COMBINATION, FINAL }

data class EscapeRoom(
    val level: Int, val zone: String, val title: String, val objective: String,
    val sceneDescription: String, val clue: String, val item: String = "",
    val puzzle: EscapePuzzle, val expected: String = "", val requiredItem: String = "",
    val reward: String = "", val target: String = "porte de secours"
)

object EscapeStory {
    val rooms = listOf(
        EscapeRoom(1,"SECTEUR A : ACCES", "Portique d'entree", "Lire le panneau et ouvrir le portique.",
            "Un panneau technique porte des marques effacees.", "Quatre chiffres graves : 3 - 1 - 4 - 2.",
            puzzle=EscapePuzzle.CODE, expected="3142", reward="badge", target="clavier du portique"),
        EscapeRoom(2,"SECTEUR A : ACCES", "Entrepot", "Fabriquer un passe-partout pour la serrure.",
            "Derriere une caisse, un crochet de maintenance.", "Le badge est trop epais. Un crochet peut l'adapter.",
            item="crochet", puzzle=EscapePuzzle.COMBINATION, requiredItem="passepartout", target="serrure mecanique"),
        EscapeRoom(3,"SECTEUR A : ACCES", "Couloir maintenance", "Couper le fil qui bloque l'issue.",
            "Une pince isolee est posee sur un etabli.", "Le verrou est relie a un fil rouge.",
            item="pince", puzzle=EscapePuzzle.ITEM, requiredItem="pince", target="fil de securite"),
        EscapeRoom(4,"SECTEUR B : ATELIERS", "Atelier des symboles", "Retrouver la suite sur le schema.",
            "Un schema mural montre trois formes geometriques.", "Lecture de gauche a droite : TRIANGLE, ROND, CARRE.",
            puzzle=EscapePuzzle.SYMBOLS, expected="TRC", target="console des formes"),
        EscapeRoom(5,"SECTEUR B : ATELIERS", "Bureau du chef", "Decryptage du code du coffre.",
            "Un tableau porte un inventaire de machines.", "Quatre machines actives : 4 - 2 - 0 - 7.",
            puzzle=EscapePuzzle.CODE, expected="4207", reward="bobine", target="coffre du bureau"),
        EscapeRoom(6,"SECTEUR B : ATELIERS", "Centrale electrique", "Reparer l'alimentation de la porte.",
            "Un fusible neuf est dans un boitier ouvert.", "La bobine du bureau peut etre assemblee au fusible.",
            item="fusible", puzzle=EscapePuzzle.COMBINATION, requiredItem="fusible_charge", target="tableau electrique"),
        EscapeRoom(7,"SECTEUR B : ATELIERS", "Patrouille robotique", "Neutraliser la serrure magnetique.",
            "Un aimant industriel est tombe pres du robot.", "L'aimant peut liberer le verrou magnetique.",
            item="aimant", puzzle=EscapePuzzle.ITEM, requiredItem="aimant", target="verrou magnetique"),
        EscapeRoom(8,"SECTEUR C : LABORATOIRE", "Convoyeurs", "Assembler un badge d'acces laboratoire.",
            "Une carte laboratoire est coincee sur un tapis.", "La carte laboratoire doit etre renforcee avec le passe-partout.",
            item="carte_labo", puzzle=EscapePuzzle.COMBINATION, requiredItem="badge_labo", target="sas laboratoire"),
        EscapeRoom(9,"SECTEUR C : LABORATOIRE", "Laboratoire secret", "Trouver la puce et reproduire la sequence.",
            "Une puce de commande repose pres d'un tableau.", "Tableau : ROND, TRIANGLE, CARRE.",
            item="puce", puzzle=EscapePuzzle.SYMBOLS, expected="RTC", target="panneau scientifique"),
        EscapeRoom(10,"SECTEUR C : LABORATOIRE", "Coeur de l'usine", "Fabriquer la cle du coeur, puis entrer le code final.",
            "Le plan de l'usine relie le laboratoire au coeur central.", "Souviens-toi du code du bureau (4207). Puce + aimant.",
            puzzle=EscapePuzzle.FINAL, expected="4207", requiredItem="cle_coeur", target="coeur central")
    )
    fun room(level: Int) = rooms[(level - 1).coerceIn(0,9)]
    val names = mapOf("badge" to "Badge", "crochet" to "Crochet", "passepartout" to "Passe-partout",
        "pince" to "Pince isolee", "bobine" to "Bobine", "fusible" to "Fusible",
        "fusible_charge" to "Fusible charge", "aimant" to "Aimant", "carte_labo" to "Carte labo",
        "badge_labo" to "Badge labo", "puce" to "Puce IA", "cle_coeur" to "Cle du coeur")
    val recipes = mapOf(setOf("badge", "crochet") to "passepartout",
        setOf("bobine", "fusible") to "fusible_charge",
        setOf("carte_labo", "passepartout") to "badge_labo",
        setOf("puce", "aimant") to "cle_coeur")
    fun label(item: String): String = names[item] ?: item
}

/** Domain state survives retries; only solving a room opens its platform exit. */
class World1Adventure {
    val inventory = linkedSetOf<String>()
    private val solved = linkedSetOf<Int>()
    private val completed = linkedSetOf<Int>()
    private val collected = linkedSetOf<Int>()
    private val inspected = linkedSetOf<Int>()
    private val used = linkedSetOf<Int>()
    private val symbolStates = mutableMapOf<Int, String>()
    private val codeStates = mutableMapOf<Int, String>()
    var level: Int = 1; private set
    var message = ""; private set
    var hintsUsed = 0; private set

    fun enter(room: Int) {
        level = room.coerceIn(1,10)
        message = "${EscapeStory.room(level).title} : ${EscapeStory.room(level).objective}"
    }
    fun isSolved(room: Int = level): Boolean = room in solved
    fun markCompleted(room: Int): Boolean {
        if (!isSolved(room)) return false
        completed.add(room)
        return true
    }
    fun latestUnfinished(): Int = (1..10).firstOrNull {it !in completed} ?: 10
    fun isCollected(room: Int = level): Boolean = room in collected
    fun isInspected(room: Int = level): Boolean = room in inspected
    fun isInserted(room: Int = level): Boolean = room in used
    fun code(): String = codeStates[level].orEmpty()
    fun symbols(): String = symbolStates[level].orEmpty()

    fun examineScene(nearScene: Boolean): Boolean {
        if (!nearScene) return say("Approche du panneau ou de l'objet pour l'examiner.")
        inspected.add(level)
        return say(EscapeStory.room(level).clue)
    }
    fun collect(nearScene: Boolean): Boolean {
        val room = EscapeStory.room(level)
        if (!nearScene) return say("Approche de l'objet pour le ramasser.")
        if (room.item.isEmpty()) return say("Rien a ramasser ici. Examine le decor.")
        if (level in collected) return say("Cet objet a deja ete ramasse.")
        collected.add(level)
        inventory.add(room.item)
        return say("Objet obtenu : ${EscapeStory.label(room.item)}.")
    }
    fun examineItem(item: String): Boolean {
        if (item !in inventory) return say("Choisis d'abord un objet dans le sac.")
        val text = when (item) {
            "badge" -> "Badge : un support de metal epais."
            "crochet" -> "Crochet : se fixe dans une fente de badge."
            "passepartout" -> "Passe-partout : ouvre les verrous mecaniques."
            "pince" -> "Pince isolee : coupe le fil electrique."
            "bobine" -> "Bobine : peut charger un fusible."
            "fusible" -> "Fusible : manque d'energie."
            "fusible_charge" -> "Fusible charge : retablit un circuit."
            "aimant" -> "Aimant : ouvre un verrou, sert aussi au coeur."
            "carte_labo" -> "Carte labo : a renforcer avec le passe-partout."
            "badge_labo" -> "Badge labo : acces au laboratoire."
            "puce" -> "Puce IA : utilisable dans le coeur central."
            "cle_coeur" -> "Cle du coeur : module de commande actif."
            else -> "Objet de l'usine."
        }
        return say(text)
    }
    fun combine(first: String, second: String): Boolean {
        if (first == second || first !in inventory || second !in inventory)
            return say("Choisis deux objets DIFFERENTS du sac.")
        val result = EscapeStory.recipes[setOf(first, second)] ?: return say("Ces objets ne s'assemblent pas.")
        if (result in inventory) return say("Tu as deja cree cet objet.")
        // Components remain reusable as narrative evidence across rooms, simplifying backtracking.
        inventory.add(result)
        return say("Assemblage reussi : ${EscapeStory.label(result)} !")
    }
    fun use(item: String, nearExit: Boolean): Boolean {
        val room = EscapeStory.room(level)
        if (!nearExit) return say("Approche de ${room.target} pour utiliser cet objet.")
        if (level in solved) return say("La porte est deja ouverte.")
        if (room.puzzle == EscapePuzzle.CODE || room.puzzle == EscapePuzzle.SYMBOLS)
            return say("Ce mecanisme exige un code ou une suite de symboles.")
        if (item !in inventory) return say("Choisis un objet du sac avant UTILISER.")
        if (item != room.requiredItem) return say("${EscapeStory.label(item)} ne fonctionne pas sur ${room.target}.")
        used.add(level)
        if (room.puzzle == EscapePuzzle.FINAL) return say("Cle du coeur inseree. Entre le code a quatre chiffres.")
        return solve()
    }
    fun digit(n: Int, nearExit: Boolean): Boolean {
        val room = EscapeStory.room(level)
        if (room.puzzle != EscapePuzzle.CODE && room.puzzle != EscapePuzzle.FINAL)
            return say("Pas de clavier numerique dans cette salle.")
        if (!nearExit) return say("Approche du clavier de sortie.")
        if (room.puzzle == EscapePuzzle.FINAL && level !in used)
            return say("Le coeur ne repond pas. Insere d'abord la cle du coeur.")
        if (isSolved()) return say("Code deja valide.")
        val candidate = code() + n.coerceIn(0,9).toString()
        if (candidate.length < 4) {
            codeStates[level] = candidate
            return say("Code : ${"●".repeat(candidate.length)}${"○".repeat(4-candidate.length)}")
        }
        codeStates[level] = ""
        return if (candidate == room.expected) solve() else say("Code incorrect. Regarde les indices dans cette salle.")
    }
    fun symbol(s: String, nearExit: Boolean): Boolean {
        val room = EscapeStory.room(level)
        if (room.puzzle != EscapePuzzle.SYMBOLS) return say("Pas de console a symboles ici.")
        if (!nearExit) return say("Approche de la console des symboles.")
        if (isSolved()) return say("Console deja deverrouillee.")
        if (s !in listOf("T","R","C")) return say("Symbole inconnu.")
        val candidate = symbols() + s
        if (candidate.length < 3) {
            symbolStates[level] = candidate
            return say("Sequence : ${candidate.length}/3 symboles.")
        }
        symbolStates[level] = ""
        return if (candidate == room.expected) solve() else say("Mauvais ordre. Examine le schema mural.")
    }
    fun hint(): String {
        hintsUsed++
        val r = EscapeStory.room(level)
        val result = when (r.puzzle) {
            EscapePuzzle.CODE -> "Indice : examine la scene puis tape le code sur le clavier pres de la sortie."
            EscapePuzzle.SYMBOLS -> "Indice : examine le schema et reproduis les formes dans l'ordre."
            EscapePuzzle.ITEM -> "Indice : ramasse l'objet, selectionne-le puis UTILISER devant la porte."
            EscapePuzzle.COMBINATION -> "Indice : selectionne deux objets, COMBINER, puis UTILISER le resultat."
            EscapePuzzle.FINAL -> "Indice : puce + aimant, utilise la cle obtenue, puis souviens-toi du bureau."
        }
        message = result
        return result
    }
    fun serialize(): String = listOf(inventory.joinToString(","),solved.sorted().joinToString(","),
        collected.sorted().joinToString(","),inspected.sorted().joinToString(","),used.sorted().joinToString(","),
        completed.sorted().joinToString(",")).joinToString("|")
    fun restore(s: String) {
        val parts=s.split('|')
        if(parts.size !in 5..6) return
        inventory.clear(); solved.clear();collected.clear();inspected.clear();used.clear();completed.clear()
        parts[0].split(',').filter {it in EscapeStory.names}.forEach {inventory.add(it)}
        fun readInts(src:String) = src.split(',').mapNotNull{it.toIntOrNull()}.filter{it in 1..10}
        solved.addAll(readInts(parts[1]));collected.addAll(readInts(parts[2]));
        inspected.addAll(readInts(parts[3]));used.addAll(readInts(parts[4]))
        if(parts.size==6)completed.addAll(readInts(parts[5]).filter{it in solved})
    }
    private fun solve(): Boolean {
        solved.add(level)
        val room=EscapeStory.room(level)
        if(room.reward.isNotBlank())inventory.add(room.reward)
        return say(if(level==10) "EVASION REUSSIE ! Le coeur s'ouvre, l'usine est libre." else
            "Porte deverrouillee ! Rejoins la sortie pour entrer dans la salle suivante.")
    }
    private fun say(s: String): Boolean {message=s; return isSolved()}
}
