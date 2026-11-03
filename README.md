# ESCAPE SHIFT V20 — Monde 2 / Cimes enneigées

Base technique : V19 JSON (elle-même basée sur V18.1). Le système d'énigmes de l'ancienne V19 n'est PAS réintroduit.

## Quoi de neuf

- Mondes 1 et 2 jouables : niveaux 1 à 20, toujours 10 niveaux par monde.
- Les niveaux 11 à 20 sont définis **uniquement** dans `app/src/main/assets/levels/level_11.json` … `level_20.json`.
- Fond monde 2 : `app/src/main/res/drawable-nodpi/world_02_snow.png` (panorama 3:1 et parallaxe).
- Scies, caisses, monstres, murs, pics simples/doubles/triples, trous et lasers : mêmes sprites V19.
- Flocons : objets JSON `snowflake`, dessinés par de simples cercles blancs animés. Ils sont **décoratifs et non mortels en V20**. Il n'y a pas de sprite à remplacer à ce stade.
- Jusqu'à 3 caisses indépendantes par niveau ; collision et poussée physiques.
- Nouvel objet `crate_switch` (plaque de pression) : une caisse doit rester dessus. Les niveaux 17 à 20 exigent aussi les boutons `switch` du héros pour déverrouiller la porte.
- Deux boutons manuels distincts doivent désormais être tous activés pour ouvrir la porte dans les niveaux 17 à 20.
- SHIFT requis 1, 2, 3 ou 4 fois selon le niveau, contrôlé par des lasers rouges/bleus.
- Progression sauvegardée : finir le niveau 10 débloque 11 ; finir 19 débloque 20 ; NEXT respecte les verrous.
- Titres et indices des niveaux 11..20 ajoutés aux 10 fichiers de langue intégrés dans `app/src/main/assets/i18n`.

## Édition simple d'un niveau

Chaque objet possède un identifiant stable et une géométrie :

```json
{"id":"saw_01", "type":"saw", "x": 2520, "y": 410,
 "width": 30, "height": 34, "amplitude": 50, "period": 160, "phase": 16}
```

Pour déplacer la scie du niveau 17 :

```powershell
py tools/levels/level_tools.py edit 17 --object saw_01 --set x=2550
```

Pour augmenter la largeur du trou du niveau 18 :

```powershell
py tools/levels/level_tools.py edit 18 --gap gap_01 --set width=155
```

La commande crée un backup dans `tools/levels/backups/`. Les changements de JSON embarqués nécessitent de reconstruire et réinstaller l'APK pour être visibles sur le Xiaomi.

## Simulateur de jeu

Avec Android Studio / JDK / Gradle fonctionnels :

```powershell
.\tools\levels\test_levels.bat
```

Cette commande lance la validation statique, les tests unitaires **avec les vraies règles physiques**, puis le joueur virtuel et produit les CSV dans `tools/levels/reports`.

Pour un niveau particulier, via Java (JDK du PC) :

```powershell
java -jar tools/levels/level_simulator.jar app/src/main/assets/levels 18
```

Pour étudier les phases d'une scie (optionnel) :

```powershell
py tools/levels/calibrate_saw.py 18 saw_02 --step 8
py tools/levels/calibrate_saw.py 18 saw_02 --step 8 --apply
```

`--apply` crée une sauvegarde, et ne choisit qu'une phase qui fait réussir la simulation. Un bot qui échoue ne prouve pas qu'un niveau est mathématiquement impossible : le bot est une stratégie déterministe, pas un solveur général.

Prévisualisation 2D (nécessite matplotlib) :

```powershell
py tools/levels/level_tools.py preview 20
```

## Structure JSON

- `world` : 1 ou 2 ; `level` : 1..20.
- `ground.gaps` : trous, leurs positions et largeurs ; il n'existe pas de bloc graphique de trou.
- `objects` : murs, caisses, portes, plateformes, pics, scies, monstres, lasers, boutons, plaques `crate_switch`, flocons, pièces.
- `route` : liste ordonnée des identifiants des défis du joueur virtuel. Les caisses devant les grands murs sont traitées par une phase d'approche spécifique.
- `difficulty` : cible de game design (documentaire), les collisions sont calculées depuis `objects`.
- `world2Modules` : décrit la correspondance caisse / mur pour les outils d'édition (le moteur utilise les coordonnées d'objets).

Pour les scies : `amplitude` est le déplacement maximal de chaque côté (amplitude 80 = course 160 unités de bord à bord). Pour les pics : `count` entre 1 et 3 et `spacing` définissent les copies.

La plaque s'active si une caisse couvre sa zone ; un joueur qui marche dessus ne l'active pas. Les interrupteurs manuels, eux, s'activent au contact du joueur.

## Statut des tests

- 20/20 parcours terminés avec le bot utilisant `GameWorld`.
- 47/47 tests Kotlin automatisés en environnement Android simulé.
- Affichage Android simulé : 20/20 niveaux et 3 ratios d'écran.
- Difficulté estimée croissante sur le monde 2 : cf. `tools/levels/WORLD2_DIFFICULTY.csv`.

Le vrai APK Android n'a pas été compilé dans cet environnement : **faire Build → Rebuild Project et tester sur le Xiaomi 12** avant GitHub et Google Play. Avant toute publication publique, actualiser le `targetSdk` selon les exigences Google Play en vigueur et vérifier la monétisation Start.io, notamment selon le public cible.

Ne jamais mettre le fichier `.jks`, `key.properties` ou les mots de passe dans le dépôt GitHub.
