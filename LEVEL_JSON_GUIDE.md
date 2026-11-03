# ESCAPE SHIFT V19 — Édition des niveaux par JSON

Cette V19 repart de **V18.1 multilingue**, **pas** de l'ancienne V19 « escape game ».

## 1. Où éditer ?

Chaque niveau jouable du monde 1 correspond à **un seul fichier** :

```text
app/src/main/assets/levels/
  level_01.json
  level_02.json
  ...
  level_10.json
```

Le jeu Android charge `levels/level_XX.json` via `AssetManager`, puis crée les plateformes, le sol, les pièges et les objets à partir de ces coordonnées. Il n'y a **plus de placement fixe des obstacles dans `GamePhysics.kt`**.

Le système de mondes, les 10 langues (`assets/i18n`), la progression, les sprites, les effets sonores et le comportement publicitaire restent ceux de la V18.1.

## 2. Coordonnées : x, y, width, height

- `x` : distance depuis le bord **gauche du niveau**, pas depuis le bord de l'écran.
- `y` : distance depuis le **haut** (les valeurs augmentent vers le bas).
- `width` : largeur horizontale ; pour agrandir un saut, modifier `ground.gaps[*].width`.
- `height` : hauteur verticale.
- Un écran de référence fait **560 unités de haut**, et environ **1 000 unités de large** suivant le téléphone. Un niveau peut être beaucoup plus long : `worldWidth`.
- `ground.y` vaut 444 dans les niveaux convertis ; `ground.depth` vaut 146.
- Un **trou** est un intervalle sans sol (`ground.gaps`). Aucun bloc graphique invisible n'est placé dedans.

### Exemple : trou de 140 unités

```json
"ground": {
  "y": 444,
  "depth": 146,
  "gaps": [{ "id": "gap_01", "x": 450, "width": 140 }]
}
```

Un trou s'étend ici de **x=450 à x=590**. Le moteur reconstruit automatiquement les deux morceaux de sol autour. Pour augmenter sa largeur de 20 unités, remplacer `140` par `160` (la fin sera alors à x=610). **Attention :** si le trou est franchi par un pont SHIFT, modifier également la position/largeur du pont correspondant : élargir seulement le vide peut rendre le niveau impossible.

### Exemple : pic, pic double, pic triple

```json
{
  "id": "spike_03", "type": "spike",
  "x": 1250, "y": 421,
  "width": 34, "height": 23,
  "count": 3, "spacing": 3
}
```

`count` vaut 1, 2 ou 3. `spacing` est l'espace entre deux pics. La collision et le sprite répété suivent ces valeurs. Mettre `height=46` et `y=398` pour doubler la hauteur tout en gardant le pied à `y=444`.

### Exemple : laser et scie mobile

```json
{ "id": "laser_red_01", "type": "laser_red", "x": 950, "y": 300, "width": 15, "height": 144 }
```

```json
{
  "id": "saw_01", "type": "saw",
  "x": 1600, "y": 405, "width": 33, "height": 39,
  "offsetX": 35, "amplitude": 55, "period": 155, "phase": 38
}
```

La scie occupe à chaque instant la position `x + offsetX + amplitude × sin(2π(tick+phase)/period)`. Augmenter `amplitude` élargit sa patrouille horizontale ; `period` augmente ou diminue la durée d'un aller-retour.

### Exemple : caisse, porte, plateforme et bouton

```json
{ "id": "starter_crate", "type": "crate", "x": 201, "y": 400, "width": 46, "height": 44 }
{ "id": "exit_door", "type": "door", "x": 1500, "y": 354, "width": 65, "height": 90 }
{ "id": "platform_01", "type": "platform", "x": 1000, "y": 340, "width": 150, "height": 20 }
{ "id": "switch_01", "type": "switch", "x": 1420, "y": 431, "width": 48, "height": 13 }
```

Les plateformes `platform` sont **traversables par dessous** et solides sur leur face supérieure. Les murs `wall` ont une collision entière. La caisse est poussable horizontalement, mais n'a pas encore de vraie simulation de chute verticale : conserver sa base au niveau du sol pour un comportement cohérent.

## 3. Types d'objets pris en charge

| `type` | Signification | Paramètres optionnels |
|---|---|---|
| `wall` | Mur solide | — |
| `platform` | Plateau traversable par dessous | — |
| `crate` | Caisse poussable | — |
| `bridge` | Pont SHIFT | — |
| `spike` | Pic simple, double ou triple | `count`, `spacing` |
| `laser_red` | Laser dangereux si SHIFT OFF | — |
| `laser_blue` | Laser dangereux si SHIFT ON | — |
| `laser_timed` | Laser temporisé | `phase`, `period`, `activeTicks` |
| `saw` | Scie mobile | `phase`, `amplitude`, `period`, `offsetX` |
| `monster` | Monstre mobile (saut sur la tête possible) | `phase`, `amplitude`, `period`, `offsetX` |
| `conveyor` | Tapis roulant | `speed` (ex. -145) |
| `spring` | Tremplin | — |
| `key` | Carte d'accès | — |
| `switch` | Interrupteur | — |
| `coin` | Pièce bonus | — |
| `door` | Sortie (exactement une) | — |

`activeTicks` doit être compris entre 1 et `period`. Les objets doivent rester à l'intérieur de `worldWidth`. Après un déplacement du dernier obstacle, il peut être nécessaire d'agrandir `worldWidth` et de repositionner la porte.

## 4. `route` — les checkpoints du joueur virtuel

La liste `route` contient des **identifiants d'objets**, dans l'ordre d'un parcours gagnant :

```json
"route": ["spike_01", "laser_red_02", "saw_03"]
```

Le joueur virtuel récupère les positions directement depuis les objets, pas depuis une seconde liste de coordonnées.

Pour créer un trou franchissable par un saut, lui donner un `id` dans `ground.gaps`, puis ajouter son identifiant à `route` au bon endroit, par exemple `"route": ["gap_01", "spike_01"]`. Les trous par pont SHIFT existants ne doivent pas être ajoutés comme sauts classiques.

Le simulateur historique reste **scénarisé** : son PASS prouve l'existence d'au moins un parcours gagnant ; son FAIL signifie *« parcours simulé non réussi »*, ce qui peut venir d'un niveau devenu impossible **ou** d'une stratégie de bot à recalibrer. Il ne recherche pas automatiquement tous les chemins possibles. Certains changements importants (plusieurs caisses, embranchements complexes, nouvelles mécaniques) demanderont une évolution du simulateur.

## 5. Trois façons de modifier un niveau

### A. Directement dans Android Studio

Ouvrir `app/src/main/assets/levels/level_03.json` ; modifier `x`, `y`, `width`, etc., puis reconstruire / relancer l'application.

### B. Script Python avec sauvegarde automatique

Depuis `C:\Dev\EscapeShift` :

```powershell
py tools/levels/level_tools.py list 3
py tools/levels/level_tools.py edit 3 --object spike_03 --set x=1420 --set width=52
py tools/levels/level_tools.py edit 3 --gap gap_01 --set width=155
py tools/levels/level_tools.py check
py tools/levels/level_tools.py preview 3
```

**Important :** `spike_03` est un exemple de nom, vérifiez les identifiants réels avec `list 3` avant de modifier. Le script enregistre une sauvegarde datée dans `tools/levels/backups` (ignorée par Git).

Pour générer les cartes en PNG avec `preview`, il faut `matplotlib` : `py -m pip install matplotlib`. Pour `list`, `edit` et `check`, aucune bibliothèque Python n'est nécessaire.

### C. Tests unitaires + joueur virtuel Kotlin

Depuis la racine du projet, exécuter :

```powershell
.\gradlew.bat :app:testDebugUnitTest --rerun-tasks
```

ou double-cliquer `tools/levels/test_levels.bat`. Celui-ci vérifie les JSON, exécute les tests Kotlin, puis utilise le simulateur autonome `level_simulator.jar` (Java doit être installé).

Test rapide des JSON sans Gradle :

```powershell
java -jar tools/levels/level_simulator.jar app/src/main/assets/levels
java -jar tools/levels/level_simulator.jar app/src/main/assets/levels 3
```

Résultat : `PASS` ou `FAIL`, durée du parcours, sauts, SHIFT, demi-tours, et traces CSV dans `tools/levels/reports/`. Le JAR contient le moteur **V19 au moment de sa création** : si vous modifiez ensuite le **code physique Kotlin**, reconstruisez le JAR ou fiez-vous aux tests Gradle.

## 6. Vérification avant commit GitHub

1. Modifier le JSON du niveau concerné.
2. Lancer `py tools/levels/level_tools.py check`.
3. Lancer le simulateur Kotlin (`test_levels.bat` ou JAR).
4. Regarder `tools/levels/previews/level_XX.png` et essayer le niveau dans Android Studio.
5. **Commit / Push** seulement après tests réussis et essai sur le téléphone.

## 7. Limites volontaires de cette V19

- Le monde **1 uniquement** contient 10 niveaux jouables ; les mondes 2–10 restent des écrans de navigation sans gameplay.
- Pas de générateur aléatoire à ce stade : les JSON sont **édités à la main**, puis testés.
- Le moteur des physiques V18.1 est conservé ; une caisse ne tombe pas encore dans un trou.
- Le joueur virtuel n'est pas un solveur universel, et le score de difficulté Python est **une estimation heuristique**, pas une preuve de jouabilité.
- Les modifications des JSON embarqués prennent effet **après recompilation/réinstallation** sur Android, pas pendant une partie en cours.

Tous les JSON se trouvent dans le même dépôt GitHub que le code, dans `app/src/main/assets/levels/`.

## V20 — Monde 2

Les dix nouveaux fichiers `level_11.json` … `level_20.json` conservent le schéma 1. Nouveaux types d'objets : `crate_switch` (plaque sous une caisse) et `snowflake` (rond animé sans dégâts). Plusieurs objets `crate` peuvent coexister, jusqu'à trois. Pour déplacer, réduire/agrandir, ou changer la phase d'une scie, utiliser `tools/levels/level_tools.py edit`. Voir `README_V20.md` pour les commandes et l'exécution du simulateur.
