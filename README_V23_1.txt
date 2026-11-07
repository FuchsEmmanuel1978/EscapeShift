ESCAPE SHIFT V23.1 — CORRECTIF V23 -> V23.1

Ce ZIP contient SEULEMENT les fichiers ajoutes ou modifies.
Base obligatoire : V23 (les modifications V23 du terrain, des images et des JSON doivent deja etre presentes).

MODIFICATIONS
1. Vitesse REELLE du heros x1.20, reglable dans GameVisualConfig.kt :
   const val HERO_SPEED_MULTIPLIER = 1.20f
   Effet sur les niveaux des deux mondes et les apercus.
2. Barre superieure : suppression des textes NIVEAU x/10 et SHIFT ON/OFF.
   Le compteur global de pieces reste affiche.
3. En haut a droite, bouton PAUSE || 2D blanc transparent, sans liseret.
   Menu pause : CONTINUER, RECOMMENCER, MENU. Le niveau reste immobile
   durant la pause et reprend sans rattrapage de physique.
   Les bruitages et la musique se mettent en pause puis reprennent.
4. GAME OVER : effet d'iris noir, cercle de vision qui se referme
   sur le heros, contours adoucis/opacite croissante (~1.05 s),
   puis le panneau GAME OVER habituel.
5. Menus pause traduits dans les 10 JSON de app/src/main/assets/i18n.
6. Joueur virtuel recale pour la nouvelle vitesse (petits relachements
   directionnels, sans changer les fichiers JSON des niveaux).
7. Version code Android 27, versionName 2.5.1-v23.1-pause.

INSTALLATION
1. Faire un Commit de sauvegarde de la V23 via GitHub Desktop.
2. Extraire le ZIP, copier/fusionner le dossier app dans C:\Dev\EscapeShift.
3. Android Studio : Build > Rebuild Project, puis Run.
4. Tester pause > continuer/recommencer/menu, puis la mort sur
   le niveau 1, et les niveaux 8/10/20 avec vitesse accrue.
5. Si tout fonctionne : Commit puis Push origin.

VALIDATION SUR POSTE DE DEVELOPPEMENT
- Compilation de tous les fichiers Kotlin : OK (interfaces Android simulees).
- 53/53 tests de logique : PASS.
- Simulateur de vrais mouvements et collisions : 20/20 parcours gagnants.
- Tests d'ecran pause, retour/restart et iris : PASS (Android simule).
- 10 JSON de traduction : tous valides et 101 cles identiques.
- Compilation d'APK Android native et UX sur Xiaomi : NON effectuees ici.

Aucune modification des 20 JSON de niveaux, backgrounds, images du jeu,
identifiant Start.io, fichiers de signature ou sauvegardes existantes.
