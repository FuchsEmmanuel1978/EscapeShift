Avant publi
snapshot
faire niveau
a la fin marquer rejouer au lieu next pour niveau suivant
A la fin du niveau tu n'as pas debloquer le niveau suivant toujours le cadena
Diveristé de niveau
Page d'acceuil a la place android
Son basique
bdd integre?
mode debug pour tester tout les niveaux. Fait un fichier de config
icon
Pour la V7, je recommande de créer un véritable générateur de niveaux, qui construit des parcours avec embranchements, plateformes, caisses et objectifs, puis utilise un solveur pour vérifier leur faisabilité.


V2 Apres publi
refactoring code
test unitaire
il faut un plein ecran sur le tel
multi langue
bacjground joli
Charte graphique
Design ameliore
Pub
Son par evenement
mettre le code au bon endreoit et mettre git

V3
plusieurs personnage
Monde différents


Fin de jeu:
faire des trous de longueur grande avec saut impossibles sans le shift pour faire le pont
Faire des pieges non visible: a) le triangle recule pas surprise 2) le rythme du laser evolue de maniere aléatoire de temps en temps
c) la scie fait un petit bond non classique (translation par defaut)
d) un bouton te fait mourir (mais tu ne le sais pas...)
Certaines pieces sont pas accessible. Verifier dans le test simulateur que c'est possible, rajoute des boites à deplacer ou des plateformes pour reussir
faire l'image de l'avant en bloc qui s'echaine bien visuellement et laisser vide quand le trou pour saut est dans le niveau
Enlever le pousse sur la caisse
Par defaut on va directement au niveau suivant sans arriver sur menu next et menu

code pub:
210877569

niveau 10 : la scie va jusqu'a la position du laser
piece inaccessible
animation mort et entre porte
moteur de niveau : papa

Mondes
1 : indus tres facile 
2 : Montagne facile
3: jungle
4:egypte/desert
5:bonbon
6:volcan
7:
8
9
10:espace

Objectif du package:
 vitesse hero x1.2 ==> faire dans le fichier de config
Enlever en haut Texte niveau x/10 
Enlever en Le texte shift on ou off
a la place du bouton menu mettre un bouton pause "||" en transparence. Si on clique dessus, plusieurs possibilité
revenir au menu ou restart ou continue
Floutage noir qui retrecie vers le hero dans le moins en moins transparent

Animation:
hero mort
bates qui desvcent



biss:
bouton skip proposer apres plusieurs mort


Admob
Recompense 3 vies:
ca-app-pub-0381082091623162~1990264622
ca-app-pub-0381082091623162/6092840366
Passage niveau:
ca-app-pub-0381082091623162~1990264622
ca-app-pub-0381082091623162/5107382551











**Prompt à copier-coller dans ChatGPT :**
> **Rôle :** Tu es un game designer et level designer expert, spécialisé dans les jeux de plateforme **2D** punitifs et déceptifs (type *die & retry*).
> **Objectif :** Je veux enrichir mon jeu **2D** en y intégrant **l'ensemble des mécaniques et pièges du jeu *Level Devil***. Tu dois adapter ces éléments pour les intégrer de manière fluide dans mon gameplay 2D (latéral / scroll ou écrans fixes).
> ---
>
>
> ### 📜 Liste de référence des mécaniques et pièges 2D de *Level Devil* à intégrer :
>
>
> #### 1. Pièges de terrain et décors traîtres (Tilemap / Plateformes 2D)
>
>
> * **Sol qui s'effondre / Trous invisibles :** Le carrelage ou les blocs du sol se dérobent au dernier moment sous les pieds du joueur lors d'un saut ou d'une course.
> * **Pics jaillissants (Spikes) :** Pics cachés (au sol, au plafond ou le long des murs verticaux 2D) qui sortent soudainement dès que le joueur s'en approche.
> * **Pics volants / Homing :** Pics qui se détachent du décor et foncent en ligne droite ou en arc de cercle vers le joueur.
> * **Plateformes mobiles 2D déceptives :**
> * Plateformes qui tombent par gravité dès qu'on saute dessus.
> * Plateformes qui fuient horizontalement ou verticalement quand le joueur s'approche pour atterrir.
> * Plateformes qui s'écartent brusquement en plein saut pour créer un piège inévitable.
>
>
> * **Murs et plafonds écrasants :** Le plafond 2D descend ou les murs latéraux se resserrent soudainement.
>
>
> #### 2. Subversion de l'objectif (La Porte 2D)
>
>
> * **Porte fuyarde :** La porte de sortie glisse sur le plan horizontal ou vertical quand le joueur s'en approche.
> * **Porte volante / téléportée :** La porte s'envole ou se téléporte à un autre endroit de l'écran juste avant d'être touchée.
> * **Fausse porte :** La porte s'avère être un piège mortel (ex. des mâchoires s'ouvrent ou des pics jaillissent à la place de la serrure).
> * **Porte verrouillée à la dernière seconde :** Nécessite une clé invisible ou un interrupteur caché dans le niveau.
>
>
> #### 3. Perturbation des commandes et des contrôles 2D
>
>
> * **Inversion des commandes horizontales :** La touche gauche fait aller à droite, et inversement.
> * **Changement dynamique des touches :** Les commandes changent en plein milieu du niveau sans avertissement.
> * **Inversion de gravité :** Le personnage 2D se retrouve la tête en bas (plafond devient sol).
> * **Inertie / Sol glissant :** La physique 2D du joueur change soudainement (effet patinoire ou saut lourd).
>
>
> #### 4. Visibilité et illusion d'optique en 2D
>
>
> * **Niveaux dans le noir (Blackout) :** Écran noir complet en 2D où seul le personnage (ou une petite zone de lumière autour de lui) est visible.
> * **Faux éléments de décor (Fake walls) :** Des murs qui ont l'air solides mais qui sont traversables, ou à l'inverse, des passages ouverts qui sont bloqués par un mur invisible.
> * **Dédoublement / Illusions :** Plusieurs faux clones du personnage ou plusieurs portes identiques.
>
>
> #### 5. Mécaniques de rythme et système
>
>
> * **Respawn instantané :** Réapparition immédiate au dernier checkpoint pour maintenir un rythme *die & retry* ultra-rapide.
> * **Conditionnement du joueur :** Un même piège change de comportement lors de la tentative suivante pour tromper la mémoire musculaire du joueur.
>
>
> ---
>
>
> ### 🎯 Livrable attendu :
>
>
> En te basant sur cette liste, peux-tu me proposer :
> 1. Une **intégration structurée** de ces mécaniques dans les niveaux 2D de mon jeu.
> 2. Une **courbe de progression** (comment introduire les pièges du plus simple au plus tordu au fil des mondes).
> 3. Des **idées de combinaisons originales** de plusieurs de ces pièges 2D au sein d'un même tableau.