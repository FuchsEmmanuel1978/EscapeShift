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



biss:
bouton skip proposer apres plusieurs mort
