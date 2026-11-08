# ESCAPE SHIFT V26 — MONDE 3 : PHYSIQUE DES NIVEAUX 21, 22 ET 23

Base: **V25 Puzzle Lab** (elle-même issue de V24, avec les 20 premiers niveaux en place).

## Installation (projet complet)
1. Décompresser le ZIP complet dans `C:\Dev\EscapeShift_V26` (hors OneDrive).
2. Ouvrir le dossier dans Android Studio, laisser Gradle synchroniser.
3. Compiler `app` en Debug puis **Run** sur le Xiaomi 12.
4. Dans **MONDES**, faire un swipe jusqu'au **monde 3 — Désert**.
5. En Debug (`DevConfig.ENABLED=true`, `UNLOCK_ALL_LEVELS=true`), les cases **1, 2, 3** ouvrent les niveaux **21, 22, 23**.
6. Les niveaux 24 à 40 restent des prototypes logiques désactivés.

## Ce qui fonctionne réellement avec les touches normales
- **21 — Pont et détour** : le héros pousse latéralement un pont jusqu'au trou, le traverse, revient, pousse la caisse sur la plaque et rejoint la sortie. La porte exige les 3 conditions *pont correctement placé + aller-retour + caisse sur plaque*.
- **22 — Roue et laser** : le héros pousse la roue jusqu'au bouton ; le laser est éteint pendant `windowTicks`, puis se réactive. Franchir le laser pendant la fenêtre désactive le verrou de la porte. Toucher le laser actif tue le héros.
- **23 — Deux caisses / quatre boutons** : les boutons 1 et 4 doivent recevoir chacun une caisse. Les trois plateformes mobiles changent de rythme et d'amplitude selon la combinaison. La sortie exige la synchronisation et **trois vrais atterrissages dans l'ordre 1 → 2 → 3**.

Le rendu reste volontairement simple en Canvas 2D orange/gris/noir/blanc. Aucune modification des fonds, sons ou sprites existants.

## JSON éditables
Les dispositifs sont tous dans :

`app/src/main/assets/levels/level_21.json` (et 22, 23)

Chaque dispositif se trouve sous **`puzzle.devices`**. Modifier : `x`, `y`, `width`, `height`, `targetX` (limite de poussée), `windowTicks` (fenêtre laser), et, pour les plateaux, `syncedTravel`, `unsyncedTravel`, `syncPeriodTicks`, `unsyncPeriodTicks`, `baseSpeed`.

Les trous sont définis à deux endroits **pour compatibilité avec le système de niveaux** : `ground.gaps` et le dispositif `ground_gap` du niveau 21. Les valeurs x/width doivent correspondre ; le validateur les compare. Le niveau 23 utilise `ground.gaps` uniquement.

La section `puzzle.rules` conserve les solutions logiques de la V25 pour le Puzzle Lab. **Les actions physiques de ces trois niveaux sont maintenant traitées par `PhysicalPuzzle.kt`**, et la source de leurs positions est `puzzle.devices`. Ne pas modifier des règles de `puzzle.rules` en pensant changer les collisions : ces règles restent utiles uniquement au laboratoire logique.

Exemples depuis la racine du projet :

```powershell
py tools/levels/level_tools.py list 23
py tools/levels/level_tools.py edit 22 --device plateA --set windowTicks=150
py tools/levels/level_tools.py edit 23 --device platform2 --set x=1630
py tools/levels/level_tools.py preview 23
py tools/levels/level_tools.py check
.\tools\levels\test_levels.bat
```

L'éditeur crée une sauvegarde avant chaque modification. Pour le niveau 21, modifier l'intervalle `gapA_ground` et `gapA` ensemble si le trou change.

## Tests

- Kotlin host avec interfaces Android simulées : **compilation réussie**.
- **20/20 anciens niveaux** : parcours de joueur virtuel toujours gagnants.
- **21, 22, 23** : trois parcours gagnants simulant les commandes, collisions et sauts réels via `GameWorld.tick()` : `tools/levels/PhysicalPuzzlePlaytest.kt`.
- `tools/levels/physical_puzzle_playtest.jar` : compilé à partir du nouveau moteur, pour tests sans Android Studio.
- JSON 1..23 : validations de structure réussies.
- Monde 3 : boutons/roue/pont/plateaux réellement interactifs ; aucun déclenchement fictif de victoire.

**Limite :** ces parcours gagnants prouvent la faisabilité d'au moins une stratégie pour chaque niveau, mais pas qu'ils sont intuitifs ou agréables. Évaluer le jeu sur le Xiaomi et adapter le timing avant publication. Il n'a pas été possible de compiler l'APK natif avec le SDK Android ici.

## Changelog et suivi GitHub
- Les niveaux 1..20 et les 17 autres prototypes 24..40 sont conservés.
- Développement recommandé sur une branche `feature/world3-physical-21-23`.
- Ne pas envoyer les clés de signature Google Play dans Git.
