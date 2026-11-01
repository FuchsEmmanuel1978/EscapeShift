# ESCAPE SHIFT — V13 : Design partagé et nouveaux défis

V13 reprend la V12. Compatible avec Android Studio Kotlin, package `com.escapeshift.game`.

## Modifications

- **GAME OVER orange**, boutons de victoire/défaite et menu 2D orange/blanc avec bordure noire.
- Commandes à l'écran en aplat blanc à **5 % d'opacité**, aucun cadre ni liseré orange, seulement le texte/flèche visible.
- Barre du haut : à gauche `PIÈCES x/y`, au centre niveau, à droite SHIFT ON/OFF orange et MENU 2D.
- Portes **fermée** (volet graphite rayé, voyant orange) et **ouverte** (ouverture claire, montant noir/orange).
- Pics orange/blanc/noir en tailles normales, hautes ou en double. Dimensions des collisions synchronisées avec le dessin.
- Tapis plus contrastés, rayures animées et déplacement vers la gauche accéléré.
- Monstres à côté des tapis sur les niveaux 8-10 ; saut dessus pour les éliminer.
- Scies : oscillation horizontale de 110 unités (contre 52 précédemment).
- Plateformes aériennes **traversables par en dessous**, atterrissage possible par le dessus. Pièces bonus dessinées au-dessus, collectables, avec compteur dans la barre supérieure.
- **Refactoring** : `GameDesign.kt` contient toutes les fonctions de design partagé pour les menus, HUD, boutons, portes, pics, tapis, pièces et choix de niveaux.
- Fond panoramique et musique de la V12 conservés ; dix niveaux, progression, debug, animations PNG conservés.

## Installer le correctif dans le projet existant

1. Extraire `escape_shift_v13_correctif.zip`.
2. Copier le dossier `app` extrait par-dessus le dossier `app` de votre projet EscapeShift V12, accepter fusion et remplacement.
3. Dans Android Studio : **Build > Rebuild Project** puis **Run**.

Le correctif contient exclusivement les sources Kotlin modifiées, pas les grandes images ni la musique ; le projet complet contient tout.

## Changer l'apparence plus tard

Fichier central : `app/src/main/java/com/escapeshift/game/GameDesign.kt`

- `drawControl` : boutons transparents, à 5 % de blanc
- `drawMenuButton` : Rejouer, Menu, Next, Retour
- `drawLevelTile` : sélection de niveaux
- `drawDoor` : état ouvert ou fermé
- `drawSpike` : pics
- `drawConveyor` : tapis
- `drawCoin` : pièces
- couleurs centralisées en tête de fichier

## Tests

- 27/27 tests Kotlin/JUnit sur interfaces Android de substitution (y compris 10/10 parcours automatisés, pièces et monstres).
- Rendu simulé sur 3 formats, pour les 10 niveaux : OK.
- Pas de compilation APK Android native dans cet environnement. Une vérification réelle sur Xiaomi/émulateur est nécessaire.
- Le joueur virtuel suit des scripts déterministes : il prouve l'existence d'un parcours, pas que chaque itinéraire est sûr.
