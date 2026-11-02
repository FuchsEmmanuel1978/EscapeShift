# ESCAPE SHIFT V17 — intégration publicitaire Start.io (TEST)

Base : V16, 10 mondes et 10 niveaux actifs du monde 1 conservés.

- SDK Start.io 5.2.4 configuré dans `app/build.gradle.kts`.
- Réglages dans `app/src/main/java/com/escapeshift/game/StartioConfig.kt`.
- Écran de victoire : une pub interstitielle de test peut apparaître **1,4 s après la victoire**, seulement si préchargée, si le niveau suivant est jouable, et si le délai de 2 min depuis la précédente pub est écoulé.
- Jamais à l'ouverture du jeu, jamais pendant une partie, jamais après GAME OVER.
- Si aucune pub prête : NEXT reste utilisable et aucune attente n'est imposée.
- Pub annulée quand le joueur quitte l'écran de victoire ou commence le niveau suivant.
- App ID Start.io réel : **non fourni**. Mettre votre ID dans `StartioConfig.APP_ID`.
- Publicités réelles désactivées en version release par quatre drapeaux de validation.
- La version debug utilise l'App ID **demo officiel** et le mode test du SDK.

Voir `STARTIO_README.md` pour les paramètres, les exigences Google Play / vie privée et le mode d'installation.

## Installation sur le projet GitHub local

1. Faire une sauvegarde (commit GitHub) des modifications actuelles.
2. Décompresser `ESCAPE_SHIFT_V17_STARTIO_CORRECTIF.zip`.
3. Copier / fusionner son dossier `app` dans `C:\Dev\EscapeShift`.
4. Android Studio : Sync Gradle puis Build > Rebuild Project, Run sur le Xiaomi 12.
5. Rester en Debug : utiliser les **pubs de test** uniquement. Aucun ID personnel n'est nécessaire pour voir les annonces de démonstration si l'inventaire test est disponible.
6. Lancer niveau 1, terminer, rester sur l'écran BRAVO : publicité test seulement si une pub a déjà été préchargée.

**Limites des vérifications :** compilation Kotlin réalisée avec stubs Android/Start.io, tests de logique et rendu simulés. Compilation APK et chargement SDK réels restent à faire dans Android Studio avec Maven Central accessible.
