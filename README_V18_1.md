# ESCAPE SHIFT — V18.1 MULTILINGUE AUTONOME

**Base : V18 avec les modifications multilingues de la V18 I18N.**
La V19 Escape Game **n'est pas incluse**. Gameplay, dix niveaux jouables du monde 1,
10 mondes dans le menu, sons, graphismes, Start.io et physique sont conservés.

## Un seul dépôt : EscapeShift

Tous les fichiers de traduction se trouvent dans le **projet Android** :

`app/src/main/assets/i18n/`

- `en.json` — anglais (repli par défaut)
- `fr.json` — français
- `es.json` — espagnol
- `pt-BR.json` — portugais brésilien
- `de.json` — allemand
- `it.json` — italien
- `ja.json` — japonais
- `ko.json` — coréen
- `hi.json` — hindi
- `id.json` — indonésien

**Plus de dépôt de langues séparé, plus de script de copie/synchronisation.**
Éditez les JSON directement dans `C:\Dev\EscapeShift\app\src\main\assets\i18n`.
Les fichiers font partie du même commit/push GitHub que le code Kotlin.

## Langue système et langue de test

`GameStrings.kt` lit les JSON depuis les assets de l'APK. Il prend la langue Android
(en suivant l'ordre des langues de l'OS) et se replie en anglais si non disponible.

Pour forcer une langue pendant le développement :
`app/src/main/java/com/escapeshift/game/DevConfig.kt` :

`const val FORCE_LANGUAGE = "ja"` — japonais
`const val FORCE_LANGUAGE = "fr"` — français
`const val FORCE_LANGUAGE = ""` — langue du téléphone

L'override n'est pris en compte que pour une version débogable et si DevConfig.ENABLED
est actif. Ne mettez pas un override dans une version de publication.

## Installation du correctif

1. Sauvegardez votre projet actuel dans GitHub Desktop (commit avant fusion).
2. Décompressez `ESCAPE_SHIFT_V18_1_CORRECTIF.zip`.
3. Copiez le dossier `app` du correctif dans `C:\Dev\EscapeShift` et fusionnez.
4. Android Studio : **Build > Rebuild Project**, puis **Run**.
5. Vérifiez français et japonais (`FORCE_LANGUAGE = "ja"`) sur votre téléphone.
6. Lorsque le build fonctionne, commit et push dans votre dépôt `EscapeShift`.

## Important

- Le nom de package reste `com.escapeshift.game`.
- `versionCode = 21`, `versionName = 2.0.1-i18n-local`, sous réserve de votre historique
  des versions Play Console (le code doit rester strictement croissant).
- Le correctif ne remplace pas votre `StartioConfig.kt` personnel.
- Les textes intégrés aux images (logo) et le nom Android dans `strings.xml` ne peuvent
  pas être chargés directement via JSON sans adaptations spécifiques d'Android.
- Avant Google Play, relisez les traductions avec des locuteurs natifs et effectuez
  les validations Android et publicitaires habituelles.
