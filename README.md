# ESCAPE SHIFT V27 — Monde 3 : énigmes physiques 24 à 30

**Base : V26.** Livraison limitée aux niveaux **24–30**, sans changement de design, de fonds, de musique, de publicités ni de traduction des anciens textes. Les niveaux 1–23 restent disponibles. Les niveaux 31–40 restent des **prototypes logiques non jouables** ; seul le premier niveau de ces mondes peut servir à l'aperçu de fond.

## Jouer dans Android Studio

1. Décompresser le projet complet dans `C:\Dev\EscapeShift_V27` (hors OneDrive).
2. Ouvrir le dossier dans Android Studio, synchroniser Gradle et exécuter `app` en **Debug**.
3. Dans **MONDES**, faire glisser jusqu'au monde **3 (Désert égyptien)**.
4. Choisir une des dix cases : niveaux **21–30**. Le mode debug peut déverrouiller toutes les cases selon `DevConfig.UNLOCK_ALL_LEVELS`.
5. Commandes habituelles : gauche, droite, saut, SHIFT. **ACT** (bouton au-dessus de SHIFT) sert à soulever/déposer une caisse ou à tourner une roue proche. Ce bouton apparaît seulement sur les niveaux contenant le nouveau moteur d'énigmes.

**Important :** la touche ACT ne téléporte jamais une caisse : on la prend près du héros, on la transporte dans la scène, et on la dépose devant lui. Les caisses déposées sur une plaque sont verrouillées pour éviter qu'une collision les fasse glisser accidentellement. Le héros peut sauter par-dessus. Le joueur doit atteindre réellement la porte après avoir résolu l'énigme.

## Niveaux ajoutés

| Niveau | Objectif | Condition physique pour ouvrir |
|---|---|---|
| **24** | Deux caisses et héros sur une grosse plaque, puis caisse sur le tremplin | Poids total au moins 3 atteint, puis `crateB` placée sur `springA` |
| **25** | Mastermind physique : 10 caisses / 5 plaques | Les caisses 02, 04, 06, 08, 10 sur les plaques 1–5 ; feedback `N/5` sans révéler lesquelles |
| **26** | Régler une roue pour aligner les plateformes puis franchir un trou | Angle de roue = **135°**, puis véritable traversée du trou |
| **27** | Ramasser les nombres premiers dans l'ordre | `2, 3, 5, 7, 11, 13, 17` sans toucher aux nombres composites |
| **28** | Deux caisses règlent des murs, tremplin à pousser | `crateA` sur `plateB`, `crateB` sur `plateA`, tremplin au `targetX` |
| **29** | Trouver le mot lié à GAME | Ramasser **O → V → E → R** ; mauvaise lettre = verrouillage de la tentative |
| **30** | Test de suites + gravité + commandes inversées | Ramasser **U21** et **13**, traverser physiquement les deux zones |

### JSON éditables

Les coordonnées et dimensions sont définies dans :

```
app/src/main/assets/levels/level_24.json
...
app/src/main/assets/levels/level_30.json
```

Sous `puzzle.devices` : `id`, `kind`, `x`, `y`, `width`, `height`, `requiredWeight`, `weight`, `angles`, `targetX`, etc. Les suites sont dans `puzzle.sequences` ; le Mastermind dans `puzzle.combination`.

Les rubriques `puzzle.rules` et `puzzle.solution` sont encore utilisées **par le simulateur logique expérimental**, tandis que les interactions du jeu Android sont calculées par `AdvancedPuzzle.kt` à partir des vraies positions et collisions. Ne pas croire qu'une modification de `puzzle.solution` modifie la physique.

## Exemples de commandes

```powershell
# Depuis le dossier racine du projet
py tools/levels/level_tools.py list 28
py tools/levels/level_tools.py edit 28 --device springA --set targetX=2010
py tools/levels/level_tools.py check
py tools/puzzles/test_puzzles.py
.\tools\levels\test_levels.bat
```

Dans Android Studio, le test JUnit `World3AdvancedPhysicalTest` contient **sept parcours gagnants reproductibles**, tous avec `GameWorld.tick(GameInput(...))`, sans téléportation. **Après modification des JSON, relancer ces tests** : le passage d'un simulateur scénarisé peut être invalidé par un nouvel emplacement d'obstacle.

## Architecture de la V27

- `AdvancedPuzzle.kt` : règles des plaques, roues, séquences, gravité, commandes inversées et portes.
- `GamePhysics.kt` : transport de caisses, collisions avec murs et plateformes, saut et déplacement réel.
- `GameView.kt` : dessin des objets avec le style 2D existant et bouton ACT.
- `JsonLevelData.kt` : charge les dispositifs déclarés dans les JSON.
- `WorldCatalog.kt` : uniquement les mondes 1, 2 et les niveaux 21–30 du monde 3 sont jouables ; monde 4 non développé.
- `World3AdvancedPhysicalTest.kt` : sept parcours de non-régression de bout en bout et test des portes verrouillées.
- `tools/puzzles` : simulateur de logique des 20 énigmes (à distinguer du test physique).

## Vérifications réalisées

- Compilation Kotlin avec interfaces Android simulées : **réussie**.
- Niveaux historiques **1 à 20** : **20/20** parcours gagnants avec `LevelPlaytester`.
- Niveaux **21 à 23** : **3/3** parcours gagnants via `GameWorld.tick`.
- Niveaux **24 à 30** : **7/7** parcours gagnants via `GameWorld.tick` ; les portes ne s'ouvrent pas au départ.
- Validation statique des niveaux **1 à 30** : **30/30**.
- Tests Python du laboratoire logique : **15/15**.

**Non vérifié ici :** compilation native Android/AAB, performance sur Xiaomi, accessibilité des touches pour un enfant, équilibre de difficulté. Les parcours scénarisés prouvent *au moins une solution*, pas toutes les solutions possibles, ni leur simplicité. Tester d'abord sur téléphone avant publication Google Play.

## Préserver le dépôt GitHub et Google Play

Créer une branche `feature/world3-physical-24-30`. Conserver hors du dépôt les clés `.jks` et les secrets. Le fichier `StartioConfig.kt` peut devoir être reconfiguré sur une nouvelle copie complète ; n'y publier aucun secret privé. Ne pas remplacer la version déjà diffusée sur Google Play sans validation du jeu sur téléphone.
