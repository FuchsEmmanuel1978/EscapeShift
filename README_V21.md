# ESCAPE SHIFT V21 — 10 panoramas et aperçu jouable

Base : **V20 (20 niveaux JSON)**. Les fichiers `level_01.json` à `level_20.json` sont **inchangés**.

## Nouveautés
- 10 fonds panoramiques exacts 2172 x 724 (ratio 3:1), visibles par swipe dans l'écran MONDES et lors des parties.
- Monde 1 : Usine. Monde 2 : Neige. Monde 3 : Désert égyptien. Monde 4 : Bonbons. Monde 5 : Volcan. Monde 6 : Forêt tropicale. Monde 7 : Orbite. Monde 8 : Océan. Monde 9 : Ville futuriste. Monde 10 : Monde cosmique final.
- Dans l'écran MONDES, le premier niveau de **chaque** monde peut être joué pour découvrir le décor. Mondes 2..10 : ce premier niveau est une **copie exacte de la géométrie du niveau 1**, à partir d'un fichier JSON séparé dans `app/src/main/assets/previews/`.
- Tous les autres niveaux des mondes 3 à 10 sont encore verrouillés.
- La V20 conserve ses vrais niveaux 11..20. **Le vrai niveau 11 n'est pas remplacé** : cliquer sur 11 dans MONDES lance la prévisualisation; jouer au niveau 11 depuis NEXT du niveau 10 lance son niveau normal V20. La progression et les données réelles du monde 2 restent intactes.
- Les parties d'aperçu n'avancent pas la progression sauvegardée et ne proposent pas NEXT vers un niveau inexistant.
- Les textes du menu des mondes sont traduits dans les 10 JSON i18n.

## Fichiers
- `app/src/main/res/drawable-nodpi/world_XX_*.png`: images du jeu, incluent les dix nouveaux fonds.
- `app/src/main/res/drawable-nodpi/` contient les dix PNG nécessaires au jeu. Le pack source `ESCAPE_SHIFT_V21_BACKGROUNDS.zip` est livré séparément pour éviter de doubler la taille du téléchargement.
- `app/src/main/assets/previews/level_11.json`, `level_21.json` ... `level_91.json`: 9 copies indépendantes et éditables du niveau 1.
- `WorldPreviewCatalog.kt`: charge la géométrie d'un aperçu via le parseur JSON V20.
- `WorldBackgrounds.kt`: mapping des images pour l'affichage des mondes et la parallaxe.
- `WorldCatalog.kt`: statut jouable des premiers niveaux.

## Installation (dépôt GitHub)
1. Faire Commit / Push de sauvegarde.
2. Dézipper le correctif V21 dans `C:\Dev\EscapeShift`, accepter fusion et remplacement.
3. Dans Android Studio, Build > Rebuild Project et Run.
4. Tester swipe mondes 1→10, lancement des niveaux 21/31/.../91, puis un vrai niveau du monde 2.
5. Après validation, Commit / Push origin.

## Vérification
Les JSON V20 restent des sources de vérité; les aperçus sont des fichiers isolés sans effet sur les autres niveaux.
La vraie compilation Android et le rendu sur Xiaomi doivent être vérifiés localement.
