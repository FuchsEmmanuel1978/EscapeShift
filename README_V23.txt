ESCAPE SHIFT V23 — Patch DIFFERENTIEL V22 -> V23

Contient UNIQUEMENT les fichiers modifies/ajoutes et ce README.
1. Sauvegarder le projet V22 avec GitHub Desktop (Commit puis Push).
2. Extraire le ZIP.
3. Copier le dossier app de ce correctif dans C:\Dev\EscapeShift.
   Accepter fusion des dossiers et remplacement des fichiers existants.
4. Android Studio : Build > Rebuild Project > Run.
5. Tester en priorite niveaux 2,3,6 (pont SHIFT) et 8,9,10 (tapis).
6. Si tout fonctionne, Commit puis Push sur GitHub.

Modifications :
- Sol du monde 1 : res/drawable-nodpi/ground_factory_tile.png, tuile PNG
  repetee en avant-plan. Les gaps des JSON sont de vrais trous (pas d'image).
- Visuel hero +50%, lasers +25% (hitboxes inchanges), monde 1 uniquement.
- Boutons sans images, plats, sans contour, fond blanc alpha 51 = 80% transparent.
  Deplacement triangles alpha 128, saut ▲ alpha 51, SHIFT # alpha 128.
- Reglages : GameVisualConfig.kt.
- Portes ouvertes/fermees flat 2D, sans texte SORTIE au-dessus.
- Trous SHIFT 270 unites : saut classique insuffisant, pont obligatoire.
- Tapis 270 unites avec fort recul -800 unites/s ; saut d'elan pour passer.
- Affichage du PNG piece + compteur global dedoublonne et persistant.
- Apercus des 9 autres mondes synchronises sur le niveau 1 V22.

Les JSON des niveaux 11 a 20 ne sont pas modifies.
Le score total de pieces est cree a partir de la V23 (pas d'historique
retroactif des pieces recoltees dans la V22, non memorisees individuellement).

Tests locaux : 20/20 parcours simules PASS, 53/53 tests Kotlin PASS,
20/20 JSON analyses PASS, compilation Kotlin avec interfaces Android
simulees PASS. Compilation APK native sur Android Studio restant a valider.

Le correctif ne contient aucune cle de signature, aucun fichier .jks
et ne remplace pas StartioConfig.kt.
