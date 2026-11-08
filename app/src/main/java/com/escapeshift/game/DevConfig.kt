package com.escapeshift.game

/** CONFIGURATION DE DEVELOPPEMENT — mettre ENABLED = false avant publication. */
object DevConfig {
    const val ENABLED = true
    const val UNLOCK_ALL_LEVELS = true     // Uniquement pour tester, ne change pas la sauvegarde
    const val SHOW_HITBOXES = false       // Cadres de collision pendant le jeu
    const val SHOW_DEBUG_STATS = true     // Position / niveau / nombre d'actions
    const val FORCE_LANGUAGE = ""       // "" = langue Android; debug uniquement, ex: "ja", "de", "pt-BR"
    const val START_LEVEL = 1            // Niveau lancé par le bouton JOUER (1..23)
    const val ALLOW_LEVEL_SHORTCUTS = true // Sur clavier : PageUp / PageDown
}
