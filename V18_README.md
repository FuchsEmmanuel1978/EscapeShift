# ESCAPE SHIFT V18 — Start.io uniquement sur BRAVO / GAME OVER

## Correctif
- Avant: la V17 affichait uniquement après les victoires, jamais après une défaite,
  et imposait un délai de 120 s même en debug.
- Maintenant: le jeu émet un seul événement de fin de partie (gagnée ou perdue).
  Start.io peut afficher un interstitiel 1,4 s après BRAVO ou GAME OVER,
  mais uniquement s'il est déjà préchargé et si le joueur est encore sur cet écran.
- Aucun affichage au milieu d'un niveau, après START/NEXT/REJOUER, ou au démarrage.
- En debug, délai minimal de 0 seconde pour vérifier les deux événements sur téléphone
  en utilisant UNIQUEMENT les publicités de test Start.io.
- En release, délai minimal de 120 secondes pour éviter des interstitiels à chaque
  mort rapide (politique Google Play, UX); un résultat n'implique jamais une pub garantie.
- Deux entrées AndroidManifest désactivent explicitement les publicités automatiques
  Start.io splash / retour en arrière.

## Code
- `GameView.kt` : signale victoire ET défaite une seule fois.
- `MainActivity.kt` : transmet l'événement au contrôleur Start.io.
- `StartioInterstitials.kt` : programme l'affichage avec contrôle de l'écran.
- `EndOfLevelAdsConfig.kt` : nouveaux réglages, séparés du SDK.
- `AndroidManifest.xml` : désactivation Start.io des publicités automatiques.

## À préserver !
La configuration existante `StartioConfig.kt` n'a PAS été remplacée dans le correctif :
votre APP_ID Start.io local est conservé. En version complète, l'ID est celui de la V17
(blanc par défaut), à renseigner si besoin.

Si la V17 a été modifiée localement, faites une sauvegarde / commit avant de fusionner.

## Tests
- Tests unitaires pour victoire, défaite, délais de cooldown et debug.
- Tests existants des mondes, des 10 niveaux et de la progression conservés.
- Compilation Kotlin hors Android avec interfaces SDK simulées; compilation Gradle Android
  et publicités réelles à confirmer sur votre Xiaomi.

## Publication Google Play
La version release garde les gardes-fous de la V17 pour audience/confidentialité/consentement.
N'activez pas Start.io pour les enfants sans conformité de l'audience Google Play.
