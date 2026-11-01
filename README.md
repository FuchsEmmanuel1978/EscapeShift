# ESCAPE SHIFT — V15 Graphismes 2D

Version de développement basée sur **V14 Audio** avec les 10 niveaux, le fond industriel à parallaxe, les sons et les cinq images d'animation du héros.

## Contenu de cette mise à jour
- Pics PNG industriels, utilisables à différentes tailles, par groupes de 1 à 3.
- Deux états graphiques pour la sortie : `door_closed.png` et `door_open.png`.
- Caisse en PNG 2D (le mouvement et les collisions restent identiques).
- Pièces bonus en PNG, avec leur mouvement vertical.
- Tremplin en PNG, affiché au-dessus de la zone de rebond.
- Panneaux de murs 2D en deux variantes. Les **sols et plateformes en hauteur** ne sont pas changés.
- Boutons tactiles 2D OFF / ON : les quatre touches changent d'état quand on appuie. Le bouton SHIFT reste visuellement ON tant que le pouvoir est activé.
- Le tapis roulant, les lasers, les scies, les monstres et les sons de proximité sont préservés.

## Architecture

Les styles et les ressources graphiques sont dans :

`app/src/main/java/com/escapeshift/game/GameDesign.kt`

Les emplacements et commandes d'affichage sont dans :

`app/src/main/java/com/escapeshift/game/GameView.kt`

Les PNG sont dans `app/src/main/res/drawable-nodpi/`, faciles à remplacer **par d'autres PNG de même nom**.

Les collisions, la progression, les mouvements du héros et la répartition des niveaux restent dans `GamePhysics.kt` (inchangé).

## Installer dans Android Studio / GitHub

- **Projet complet** : décompresser et ouvrir le dossier `EscapeShift` dans Android Studio.
- **Correctif** : copier le dossier `app` du correctif au même emplacement dans le projet existant, et accepter la fusion/remplacement.
- Pour un dépôt GitHub : copier les fichiers dans votre clone local, vérifier les changements dans GitHub Desktop, puis commit/push.
- Éviter de stocker le clone dans OneDrive ; utiliser par exemple `C:\Dev\EscapeShift`.

Exécuter `Build > Rebuild Project`, puis `Run` sur le Xiaomi 12 ou l'émulateur.

## Vérification et limites

- Kotlin : compilation avec API Android simulées réussie.
- 27 tests de logique existants : réussis.
- 10 niveaux : parcours gagnants du joueur virtuel confirmés.
- Rendu simulé : 10 niveaux et trois formats d'écran confirmés.
- **Le build APK / AAB natif Android n'a pas été exécuté ici**. Valider dans Android Studio.

**Publication Google Play** : ce ZIP est une version de **développement**, non signée. Le `targetSdk=35` de la base V14 doit être porté à la valeur demandée par Google Play lors de la prochaine release. Signer avec la clé `.jks` déjà utilisée ; ne jamais publier cette clé sur GitHub.
