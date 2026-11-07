# ESCAPE SHIFT V24.0 — L'usine des pièges

Base : V23.1. Projet Android Studio Kotlin **complet**. Aucune fonctionnalité de l'ancienne V19 escape-game n'est reprise.

## Implémenté dans cette version

- **Pièges réactifs JSON** dans `app/src/main/assets/levels/level_01.json` à `level_10.json`.
- Six types disponibles : `collapse_floor`, `rising_spikes`, `surprise_wall`, `moving_platform`, `moving_exit`, `reverse_zone`.
- Déclencheurs configurables : `player_near`, `player_on`, `player_jump`, `approach_fast`, `player_enter`.
- Délais d'activation en *ticks physiques* (60 ticks = 1 seconde), amplitudes et distances modifiables.
- Affichage 2D Canvas orange/gris/noir des obstacles ; conserver les sprites originaux et les fonds des dix mondes.
- 3 cœurs au monde 1 : une mort consomme un cœur, puis réapparition en 0,28 s et réinitialisation des pièges ; GAME OVER seulement au troisième décès. Les autres mondes gardent le comportement d'origine.
- Les publicités Start.io n'interviennent pas entre les vies. Le système de résultat de niveau déjà présent est conservé.
- **20 niveaux JSON existants conservés**, avec deux pièges dynamiques introduits par niveau dans le monde 1. Les mondes 2+ ne subissent pas de changements de physique.
- Moteur d'événements indépendant d'Android (`TrapEngine.kt`) pour les tests Kotlin du simulateur.
- Éditeur `--trap` ajouté à `tools/levels/level_tools.py` ; commande `preview` montre les zones de pièges.

## État des fonctionnalités demandées

| Fonction | Statut V24.0 |
| --- | --- |
| Sol qui s'effondre | intégré (JSON + physique + rendu) |
| Pics qui jaillissent | intégré |
| Mur surprise | intégré |
| Plateforme mobile | intégré |
| Porte qui se déplace | intégré |
| Commandes gauche/droite inversées dans une zone | moteur intégré, exemple dans le niveau 9 |
| Trois cœurs et mort rapide | intégré dans le monde 1 |
| Caisse sur plaque de pression | **déjà disponible** dans le moteur V20, conservé |
| Poutre manipulable, roue élévatrice, roue roulante, boutons pilotant plusieurs plateformes | **à développer en V24.1** |
| Gravité inversée et variantes par tentative | **à développer en V24.2** |

Ces ajouts sont une première phase jouable, pas encore la refonte définitive de tous les puzzles du monde 1. Les nouvelles positions sont calibrées pour conserver les parcours gagnants du joueur virtuel : leur difficulté réelle devra être ajustée après essai sur téléphone.

## Ouvrir et exécuter

1. Sauvegarder le projet précédent dans GitHub Desktop (**commit** et **push**).
2. Ouvrir ce répertoire dans Android Studio (dossier contenant `settings.gradle.kts`).
3. **Sync Gradle**, puis `Build > Rebuild Project`, puis `Run` (sur le Xiaomi).
4. Vérifier les niveaux 1, 3, 4, 8 et 10 : ils contiennent différentes catégories de pièges.
5. Vérifier la gestion des vies en mourant volontairement trois fois au niveau 1.

La configuration locale de l'identifiant Start.io n'est pas automatiquement transférée entre projets. Vérifier `StartioConfig.kt` avant de tester vos publicités.

## Tests disponibles

```
py tools/levels/level_tools.py check
py tools/levels/level_tools.py list 1
py tools/levels/level_tools.py preview 1
java -jar tools/levels/level_simulator.jar app/src/main/assets/levels
```

Le script Windows `tools/levels/test_levels.bat` réalise aussi le contrôle statique, les tests Gradle (nécessite SDK et connexion pour les dépendances), puis le simulateur.

## Modifier un piège sans toucher au code

```
py tools/levels/level_tools.py edit 1 --trap trap_01_01 --set delayTicks=18
py tools/levels/level_tools.py edit 1 --trap trap_01_02 --set x=1790 --set radius=110
py tools/levels/level_tools.py check
java -jar tools/levels/level_simulator.jar app/src/main/assets/levels
```

Le script `edit` crée une sauvegarde avant modification. L'APK doit être reconstruit pour prendre en compte les JSON modifiés.

### Exemple de piège dans `level_01.json`

```json
{
  "id": "trap_01_01",
  "type": "collapse_floor",
  "x": 1475,
  "y": 444,
  "width": 88,
  "height": 146,
  "trigger": "player_on",
  "radius": 18,
  "delayTicks": 14,
  "transitionTicks": 18
}
```

Les pièges résident dans le tableau facultatif `traps` du JSON. Les autres objets (`objects`), la route (`route`) et la géométrie du sol (`ground`) restent compatibles avec la V23.1.

## Ce que les vérifications ne prouvent pas encore

- Compilation Kotlin avec stubs Android : vérifiée dans l'environnement de préparation. **L'APK natif Android SDK / Xiaomi n'a pas été compilé ni exécuté ici.**
- Simulateur physique : 20/20 parcours gagnants, mais cela ne remplace pas une évaluation humaine des surprises.
- Le joueur virtuel existant conserve sa stratégie scénarisée ; un solveur explorant toutes les variantes de pièges reste à développer.
- Les publicités réelles demeurent soumises au consentement et aux règles Google Play liées à l'âge du public.
