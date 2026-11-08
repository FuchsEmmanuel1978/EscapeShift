# ESCAPE SHIFT V25 — laboratoire d'énigmes (mondes 3 et 4)

**Base : V24.0.** Aucun changement dans les sprites, fonds, boutons ou niveaux jouables 1–20.

## Statut réel

Ce pack introduit **20 prototypes de puzzles** (`level_21.json` à `level_40.json`)
et le moteur `ExperimentalPuzzleEngine.kt`, plus des tests Python/Kotlin.

Les scripts valident les **états logiques, les combinaisons, l'ordre des actions,
les verrous, les codes et les séquences**. Ils **ne valident PAS encore** : la
physique des déplacements, le saut, la poussée de caisse avec collisions, la
vitesse réelle d'une plateforme, l'interaction tactile avec les roues ou les
animations des objets. C'est un laboratoire technique, **pas 20 nouveaux niveaux
jouables sur Android**. Le jeu existant et ses 20 niveaux restent inchangés.

Le champ `experimental.status=logic_prototype` signale cette distinction dans
chaque JSON. Les mondes 3/4 restent au stade des aperçus actuels dans le jeu.

## Fichiers ajoutés

- `app/src/main/assets/levels/level_21.json` à `level_40.json` : plans et
  logique (position, taille, poids, angles, boutons, énigmes, solution de référence).
- `app/src/main/java/com/escapeshift/game/ExperimentalPuzzleEngine.kt` :
  interpréteur Kotlin des actions/conditions de puzzle (pur Kotlin, sans UI).
- `app/src/test/java/com/escapeshift/game/ExperimentalPuzzleTest.kt` :
  tests automatiques JUnit.
- `tools/puzzles/generate_puzzle_lab.py` : génération initiale des 20 prototypes.
- `tools/puzzles/puzzle_lab.py` : inspecteur + simulateur logique Python.
- `tools/puzzles/test_puzzles.py` : tests de validation et de mauvais choix.
- `tools/puzzles/make_previews.py` : aperçus schématiques, sans sprites.
- `tools/puzzles/previews/*.png` : 20 plans + 2 planches.

`JsonLevelData.kt` : changement **minime** de visibilité du lecteur `MiniJson`
(`private` -> `internal`) afin de le réutiliser dans le moteur expérimental.
`LevelCatalog.MAX_LEVEL` reste 20 ; aucune progression n'est débloquée.

## Commandes Windows dans le dossier projet

```powershell
py tools/puzzles/puzzle_lab.py validate
py tools/puzzles/puzzle_lab.py inspect 25
py tools/puzzles/puzzle_lab.py replay 30
py tools/puzzles/test_puzzles.py
py tools/puzzles/make_previews.py
.\gradlew.bat :app:testDebugUnitTest
```

`replay` déroule la solution de référence du fichier JSON. `validate`
contrôle 20/20, une tentative de sortie sans solution, les erreurs de séquence
et l'expiration du laser du niveau 22. Modifier `devices`, `rules` ou `solution`
dans un JSON puis relancer ces commandes permet d'étudier chaque énigme.

**Note :** modifier les données d'un JSON ne change pas la version Android
installée tant qu'un nouvel APK n'a pas été construit et installé. La liaison
aux collisions et commandes tactiles sera l'étape d'implémentation suivante.

## Monde 3 — un puzzle principal par niveau

| Niveau | Mécanisme et règle testée |
|---|---|
| 21 | Pousser une plateforme sur le trou, revenir, pousser une caisse sur le bouton de porte |
| 22 | Roue roulante sur une plaque temporisée (90 ticks) pour éteindre le laser |
| 23 | 2 caisses sur 4 boutons ; choisir la bonne cadence de 3 plateformes |
| 24 | Plaque poids 3 = 2 caisses + héros ; verrouiller, récupérer une caisse et caler le tremplin |
| 25 | Mastermind : 10 caisses, 5 emplacements, seulement le nombre de positions correctes |
| 26 | Une roue à 135° aligne les trous et le parcours de sauts |
| 27 | Ramasser exclusivement les nombres premiers croissants, 17 en dernier |
| 28 | Placer les caisses pour ajuster des murs, déplacer un tremplin |
| 29 | Mot sur la porte GAME : collecter O → V → E → R |
| 30 | **Trois mécanismes en un** : suite A1 C3 F6 J10 O15 ? = U21, nombres premiers → 13, puis gravité et commandes inversées |

## Monde 4 — dix combinaisons inédites

| Niveau | Défi hybride | Disciplines |
|---|---|---|
| 31 | Pont à pousser + poids de caisses 2 et 3 = 5 | Mécanique, arithmétique |
| 32 | Lire SUGAR à l'envers = RAGUS, puis roue à 180° | Langage, orientation |
| 33 | Répartir 2 et 3 sur les bons pads, synchroniser plateformes, traverser gravité inversée | Nombres premiers, mouvements |
| 34 | Le bouton s'active au relâchement, puis ajuster le tremplin | Logique inversée, physique |
| 35 | Suite 1,4,9,16,? =25, puis éviter mur et pic mobiles | Mathématiques, réflexes |
| 36 | Code Mastermind de caisses + vérifier le palindrome RADAR | Déduction, langue |
| 37 | Collecter 2,3,5,7, puis franchir une plateforme sous gravité inversée | Mathématiques, gravité |
| 38 | Angle 270°, poids des caisses et placement de tremplin | Géométrie, mécanique |
| 39 | Former SWEET, verrou de caisse et révéler une fausse porte | Lettres, poids, tromperie |
| 40 | Suite x(x+1) → 30, poids 2+3, roue 90°, contraire HOT/COLD, pont final | Multi-domaines |

## Schéma des JSON

Les propriétés de niveau connues sont conservées (`world`, `level`,
`worldWidth`, `ground`, `objects`, `route`). Les nouvelles notions sont placées
sous `puzzle` :

```json
"puzzle": {
  "prototype": true,
  "title": "La combinaison aveugle",
  "concepts": ["mastermind", "10_crates_5_buttons"],
  "devices": [
    {"id": "crate01", "kind": "crate", "x": 284, "y": 401, "width": 48, "height": 43}
  ],
  "combination": {
    "id": "mastermind",
    "pads": ["pad1", "pad2", "pad3", "pad4", "pad5"],
    "target": {"pad1": "crate02", "pad2": "crate04", "pad3": "crate06", "pad4": "crate08", "pad5": "crate10"},
    "feedback": "count_correct_positions_only"
  },
  "goal": [{"var": "solved:mastermind", "eq": true}],
  "rules": [],
  "solution": [
    {"action": "submit", "target": "mastermind", "value": {"pad1": "crate02", "pad2": "crate04", "pad3": "crate06", "pad4": "crate08", "pad5": "crate10"}},
    {"action": "exit", "target": "doorA"}
  ]
}
```

Les vrais fichiers comportent l'ensemble des devices, plusieurs essais et
les coordonnées complètes. `solution` est un **script de test**, pas une
solution affichée aux joueurs (elle est embarquée dans les JSON de prototype,
à retirer avant publication).

## Tests avant d'activer ces niveaux

1. Réutiliser les sprites actuels pour les `devices` compatibles.
2. Mettre en œuvre le rendu provisoire des nouvelles roues et plaques
   **sans redesign global**.
3. Connecter `GameWorld` (collisions, poids, position, time step) à `ExperimentalPuzzleEngine`.
4. Ajouter l'interaction tactile et les indices localisés.
5. Tester chaque niveau dans le moteur physique, y compris en cas d'échec,
   de mauvais code, de mauvaise charge, de mort et de redémarrage.
6. Créer un solveur physique ou des routes recalées (le solveur logique seul
   ne prouve pas que l'on peut atteindre les objets à pied).
7. Activer progressivement les niveaux 21 à 40 et adapter la progression.

Le monde 4 ne doit pas être ouvert en production tant que ces conditions ne
sont pas remplies.
