package com.escapeshift.game

/** CONFIGURATION DE DEVELOPPEMENT — mettre ENABLED = false avant publication. */
object DevConfig {
    const val ENABLED = false
    const val UNLOCK_ALL_LEVELS = false     // Uniquement pour tester, ne change pas la sauvegarde
    const val SHOW_HITBOXES = false       // Cadres de collision pendant le jeu
    const val SHOW_DEBUG_STATS = false     // Position / niveau / nombre d'actions
    const val START_LEVEL = 1            // Niveau lancé par le bouton JOUER (1..10)
    const val ALLOW_LEVEL_SHORTCUTS = false // Sur clavier : PageUp / PageDown
}
