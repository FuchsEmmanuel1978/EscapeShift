# ESCAPE SHIFT V25 — Laboratoire des casse-têtes

Cette version repart du **projet complet V24** et inclut 20 prototypes
éditables en JSON pour les mondes 3 (désert égyptien) et 4 (bonbons).

**Pas de changement de design**, ni de règles dans les niveaux jouables 1 à 20.

Les prototypes sont **logiquement simulables** avec Python et Kotlin, mais
**pas encore jouables sur Android** : les nouvelles roues, commandes de gravité,
poids composés, puzzles alphabétiques et plateformes synchronisées ne sont pas
encore reliés au rendu et à la physique existante. Ceci est une livraison de
conception et de moteur expérimental, pas une V25 publiée sur Google Play.

Consulter `tools/puzzles/README_PUZZLE_LAB.md` pour la matrice des vingt
énigmes, leur schéma JSON et les commandes de test.

## Tester la solution logique

Dans le terminal Android Studio depuis le projet :

```powershell
py tools/puzzles/puzzle_lab.py inspect 21
py tools/puzzles/puzzle_lab.py replay 21
py tools/puzzles/puzzle_lab.py validate
py tools/puzzles/test_puzzles.py
```

Pour les anciens niveaux jouables :

```powershell
.\tools\levels\test_levels.bat
```

Les 10 mondes et les previews restent disponibles comme auparavant ; les
niveaux expérimentaux 21 à 40 restent **non accessibles dans l'appli** tant
que leurs interactions et collisions ne sont pas intégrées.
