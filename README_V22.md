# ESCAPE SHIFT V22 — Monde 1 plus difficile et vertical

Base de travail : **V21 complète**. Aucun niveau des mondes 2 à 10 n'a été modifié.

## Le nouveau monde 1

- Le niveau 1 commence avec **quatre défis** (pic, laser, scie, monstre), au lieu d'un seul pic.
- Les niveaux 1–5, 7 et 8 imposent une **montée de trois plateformes** jusqu'à une porte placée très au-dessus du sol. Cette montée est obligatoire pour terminer le niveau : il ne suffit pas de courir sous les plateformes.
- Les niveaux 6, 9 et 10 gardent leurs séquences d'aller-retour, avec des plateformes hautes et des pièces facultatives. Le niveau 6 comporte maintenant une scie et un monstre supplémentaires avant une carte à récupérer, puis un retour à la sortie.
- Les scies et monstres ont des courses horizontales plus grandes : les valeurs `amplitude`, `period` et `phase` sont configurées dans les JSON.
- Les 10 niveaux sont éditables dans `app/src/main/assets/levels/level_01.json` … `level_10.json`.
- Les arrière-plans, les sons, le système de langues, Start.io, le swipe entre mondes, les 10 niveaux enneigés (11 à 20) et les niveaux d'aperçu des mondes non développés sont préservés.

## Comprendre les montées verticales

Exemple dans `level_01.json` :

```json
{"id":"exit_stair_01", "type":"platform", "x":1535, "y":365, "width":138, "height":20}
{"id":"exit_stair_02", "type":"platform", "x":1653, "y":291, "width":138, "height":20}
{"id":"exit_stair_03", "type":"platform", "x":1771, "y":217, "width":138, "height":20}
```

Le héros saute sur les plateformes, à travers les plateformes supérieures puis atterrit dessus. Le changement de hauteur entre deux marches est volontairement limité à 74–79 unités pour respecter la physique du saut normal (environ 90 unités de hauteur théorique maximale).

La porte `exit_door` est placée en hauteur : `y=125`, donc elle ne peut pas être atteinte depuis le sol. On peut personnaliser les hauteurs et positions dans le JSON, puis tester la nouvelle trajectoire.

## Tester les niveaux et recalibrer les patrouilles

Depuis le dossier racine du projet :

```powershell
# Valider les JSON
py tools/levels/level_tools.py check

# Simuler les 20 niveaux avec la vraie physique Kotlin du jeu (JAR fourni)
java -jar tools/levels/level_simulator.jar app/src/main/assets/levels

# Aperçu visuel d'un niveau
py tools/levels/level_tools.py preview 1

# Chercher un autre timing sûr pour la scie du niveau 1
py tools/levels/tune_patrol_world1.py 1 challenge_extra_02 --step 10

# Appliquer une phase testée (avec sauvegarde automatique)
py tools/levels/tune_patrol_world1.py 1 challenge_extra_02 --step 10 --apply
```

**Attention :** le joueur virtuel est scénarisé. Un `PASS` prouve l'existence de la trajectoire jouée ; un `FAIL` ne prouve pas forcément que le niveau est impossible. Après une modification de JSON, une recompilation Android est nécessaire pour que le changement soit intégré à l'APK installé sur le téléphone.

## Dossiers du correctif

Copier le contenu de `app/` et `tools/` du correctif dans les mêmes dossiers de votre projet `C:\Dev\EscapeShift`, après un commit de sauvegarde. **Ne pas copier un nouveau dossier `EscapeShift_V22` à l'intérieur de votre dépôt existant**.

## Validation

- 20/20 niveaux terminables avec la physique Kotlin du jeu.
- Les 7 ascensions obligatoires ont été exécutées complètement (3 paliers chacun).
- Tous les JSON 1 à 20 sont valides.
- 10/10 arrière-plans panoramiques, exactement 2172 x 724 pixels.
- Compilation Kotlin des moteurs JSON, physique et simulateur : OK.
- Compilation Android/Gradle native non effectuée dans cet environnement. Tester `Build > Rebuild Project` et `Run` sur le Xiaomi 12 avant publication.
