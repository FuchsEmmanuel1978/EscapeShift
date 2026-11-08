# ESCAPE SHIFT V19 — moteur de niveaux JSON

Base : **ESCAPE SHIFT V18.1 multilingue**, sans les changements de la V19 « escape game ».

## Nouveautés

- Les **10 niveaux du monde 1** se trouvent dans `app/src/main/assets/levels/level_01.json` à `level_10.json`.
- Les objets, ponts, trous, murs, plateformes, caisse, laser, pic, scie, monstre, tapis, porte, pièces, interrupteurs sont définis par leurs coordonnées.
- `JsonLevelData.kt` : parseur JSON, validation et chargement.
- `GamePhysics.kt` : lit les formes/obstacles du JSON à la place du générateur précédent ; règles physiques V18.1 préservées.
- `MainActivity.kt` : connecte les assets Android au moteur avant l'ouverture de la partie.
- `LevelPlaytester.kt` : route de test basée sur des identifiants d'objets et compatible avec les trous.
- `tools/levels/level_tools.py` : inspection, édition, vérification et aperçu graphique.
- `tools/levels/level_simulator.jar` : simulateur Windows/Java prêt à tester les JSON.
- `tools/levels/test_levels.bat` : validation automatisée depuis Windows avec Gradle et le simulateur.

**Documentation principale :** `LEVEL_JSON_GUIDE.md`.

## Installation sur C:\Dev\EscapeShift

1. Sauvegarder la branche V18.1 avec GitHub Desktop.
2. Décompresser le correctif V19 ; fusionner son dossier `app` avec `C:\Dev\EscapeShift\app`, et copier le dossier `tools/levels`.
3. Android Studio → **Build / Rebuild Project**, puis **Run**.
4. Modifier un niveau dans `app/src/main/assets/levels/level_XX.json`.
5. Tester le JSON et le joueur virtuel : `tools\levels\test_levels.bat`.

Le projet complet peut aussi être ouvert séparément dans Android Studio.

## Important

La V19 ne change ni Start.io ni le système multilingue (toujours dans `assets/i18n`). N'envoyez pas cette version sur Google Play avant l'avoir testée sur téléphone et avant d'avoir revérifié les paramètres Google Play de votre projet local.
