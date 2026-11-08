#!/usr/bin/env python3
"""Generate self-contained prototype puzzles for worlds 3 and 4.
These tests validate puzzle-state logic, not pathfinding or Android interactions.
"""
import json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'app/src/main/assets/levels'
OUT.mkdir(parents=True,exist_ok=True)

def D(name,kind,x,y=401,width=48,height=43,**extra):
    return dict(id=name,kind=kind,x=x,y=y,width=width,height=height,**extra)
def A(action,target=None,value=None,**other):
    obj=dict(action=action)
    if target is not None: obj['target']=target
    if value is not None: obj['value']=value
    obj.update(other)
    return obj
def C(var, eq=True): return {'var':var,'eq':eq}
def R(action,target,conditions,sets,message,value=None,expires=None):
    rule={'on':{'action':action,'target':target},'if':conditions,'set':sets,'message':message}
    if value is not None: rule['on']['value']=value
    if expires: rule['expires']=expires
    return rule

def base(number,title,mechanics,devices,goal,solution,rules=None,sequences=None,combination=None,hint=None,
         design_note=None, world_width=2750, highlights=None):
    world=(number-1)//10+1
    puzzle={
      'prototype': True,
      'title':title,
      'concepts':mechanics,
      'goal':goal,
      'hint':hint or 'Observer les mécanismes et tester les états.',
      'devices':devices,
      'rules':rules or [],
      'solution':solution,
      'designNote': design_note or '',
      'validationScope':'logical_state_machine_only',
    }
    if sequences: puzzle['sequences']=sequences
    if combination: puzzle['combination']=combination
    # Deliberately only the old door in the live physics layer. The experimental
    # devices live in puzzle.devices: do not claim they already work in GameWorld.
    data={
      'schemaVersion':1,'world':world,'level':number,
      'titleKey':f'level_{number:02}_title','hintKey':f'level_{number:02}_hint',
      'intro':'OPEN','backtrackCount':0,'worldWidth':world_width,
      'playerStart':{'x':78,'y':405},
      'ground':{'y':444,'depth':146,'gaps':[]},
      'objects':[{'id':'exit_door','type':'door','x':world_width-138,'y':354,'width':65,'height':90}],
      'route':[],
      'puzzle':puzzle,
      'experimental':{'status':'logic_prototype','rendering':'existing sprites later','physics':'not integrated',
                      'worldPreview':'unchanged','goal':'solve puzzle then implement movement/collisions'},
    }
    path=OUT/f'level_{number:02}.json'
    path.write_text(json.dumps(data,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
    return path

levels=[]

# 21: bridge then crate switch. One module only.
levels.append(base(21,'La poutre et le détour',['bridge_push','return_trip','crate_switch'],[
 D('bridgeA','sliding_platform',560,410,170,22,to=['start','gap']),D('gapA','ground_gap',790,444,235,140),
 D('crateA','crate',250),D('plateA','pressure_button',1580,433,82,11),D('doorA','exit_gate',2480,354,65,90)],
 [C('flag:bridge_set'),C('flag:returned'),C('flag:crate_plate')],
 [A('push','bridgeA','gap'),A('cross','gapA'),A('return','gapA'),A('push','crateA','plateA'),A('exit','doorA')],
 [R('push','bridgeA',[],{'flag:bridge_set':True},'Le plateau couvre désormais le trou.','gap'),
  R('return','gapA',[C('flag:bridge_set')],{'flag:returned':True},'Le héros revient chercher la caisse.'),
  R('push','crateA',[C('flag:returned')],{'flag:crate_plate':True},'La caisse maintient la porte ouverte.','plateA')],
 hint='Déplace d’abord la poutre, fais un aller-retour puis pose la caisse sur le bouton.'))

# 22: rolling wheel and precise timing laser.
levels.append(base(22,'La roue du laser',['rolling_wheel','timed_pressure_plate','laser_gate'],[
 D('wheelA','pushable_wheel',480,378,72,72,weight=3),D('plateA','timed_button',970,432,80,12,windowTicks=90),
 D('laserA','laser',1450,287,18,157),D('doorA','exit_gate',2480,354,65,90)],
 [C('flag:laser_disabled'),C('flag:laser_crossed')],
 [A('push','wheelA','plateA'),A('cross','laserA'),A('exit','doorA')],
 [R('push','wheelA',[],{'flag:laser_disabled':True},'Le laser est coupé : il faut franchir rapidement.','plateA',expires={'flag:laser_disabled':90}),
  R('cross','laserA',[C('flag:laser_disabled')],{'flag:laser_crossed':True},'Le faisceau est franchi.')],
 hint='Place la roue au bon moment ; le laser est neutralisé temporairement.',
 design_note='Le vrai timer exige la physique du joueur et un décompte à l’échelle GameWorld.'))

# 23: choose 2/4 buttons for moving platforms cadence.
levels.append(base(23,'Deux caisses, quatre boutons',['crate_choice','moving_platform_speed','rhythm_jumps'],[
 *[D(f'pad{i}','speed_button',720+(i-1)*150,433,72,11,speedFactor=[1.4,0.7,1.1,0.9][i-1]) for i in range(1,5)],
 D('crateA','crate',320),D('crateB','crate',390),
 *[D(f'platform{i}','moving_platform',1430+i*195,370-i*34,120,19,baseSpeed=[130,170,190][i-1]) for i in range(1,4)],
 D('doorA','exit_gate',2480,354,65,90)],
 [C('flag:speed_sync'),C('flag:three_jumps')],
 [A('place','crateA','pad1'),A('place','crateB','pad4'),A('press','sync'),A('jump','sequence_3'),A('exit','doorA')],
 [R('press','sync',[C('at:crateA','pad1'),C('at:crateB','pad4')],{'flag:speed_sync':True},'Les trois plateformes sont synchronisées.'),
  R('jump','sequence_3',[C('flag:speed_sync')],{'flag:three_jumps':True},'La cadence est bonne.')],
 hint='Quatre boutons, mais seulement deux donnent la bonne cadence : 1 et 4 dans la version de test.'))

# 24: weight two crates+hero, lower platform, then move second crate to trampoline
levels.append(base(24,'Poids total',['three_body_weight','latched_platform','spring_crate'],[
 D('plateA','heavy_pressure_plate',550,433,175,11,requiredWeight=3),
 D('crateA','crate',220,weight=1),D('crateB','crate',310,weight=1),
 D('liftA','lowering_platform',1180,300,165,20),D('springA','spring',1650,428,85,16),
 D('doorA','exit_gate',2480,354,65,90)],
 [C('flag:lift_lowered'),C('flag:spring_ready')],
 [A('place','crateA','plateA'),A('place','crateB','plateA'),A('stand','plateA'),A('press','latch'),A('leave','plateA'),A('place','crateB','springA'),A('press','activate_spring'),A('exit','doorA')],
 [R('press','latch',[C('at:crateA','plateA'),C('at:crateB','plateA'),C('hero:plateA')],{'flag:lift_lowered':True},'Le poids total abaisse et verrouille le plateau.'),
  R('press','activate_spring',[C('flag:lift_lowered'),C('at:crateB','springA')],{'flag:spring_ready':True},'Le tremplin est calé par la caisse.')],
 hint='Deux caisses et le héros sur la plaque : verrouille le plateau puis récupère une caisse.'))

# 25 mastermind 10 crates 5 pads, hidden pattern feedback.
crates=[D(f'crate{i:02}','crate',210+i*74,weight=1,symbol=['☀','△','◇','○','★','☾','+','□','▣','∞'][i-1]) for i in range(1,11)]
pads=[D(f'pad{i}','code_button',1370+(i-1)*150,433,78,11) for i in range(1,6)]
target={'pad1':'crate02','pad2':'crate04','pad3':'crate06','pad4':'crate08','pad5':'crate10'}
first_guess={'pad1':'crate01','pad2':'crate03','pad3':'crate06','pad4':'crate07','pad5':'crate09'}
levels.append(base(25,'La combinaison aveugle',['mastermind','10_crates_5_buttons','partial_feedback'],crates+pads+[D('doorA','exit_gate',2480,354,65,90)],
 [C('solved:mastermind')],
 [A('submit','mastermind',first_guess),A('submit','mastermind',target),A('exit','doorA')],
 combination={'id':'mastermind','pads':list(target),'target':target,'feedback':'count_correct_positions_only','maxGuesses':12},
 hint='Seul le nombre de caisses bien placées est indiqué, pas lesquelles.',
 design_note='Le nombre de combinaisons possibles est important. Ajouter des indices visuels dans une version jouable.',world_width=2900))

# 26 wheel angle unique; run jumps phase.
levels.append(base(26,'L’angle unique',['rotating_wheel','moving_gaps','timed_run_jump'],[
 D('wheelA','rotating_wheel',400,359,86,86,angles=[0,45,90,135,180,225,270,315]),
 D('platformA','adjustable_platform',900,355,170,20),D('platformB','adjustable_platform',1240,303,160,20),
 D('gapA','ground_gap',1580,444,255,146),D('doorA','exit_gate',2480,354,65,90)],
 [C('angle:wheelA',135),C('flag:crossed_gap')],
 [A('rotate','wheelA',135),A('run','segmentA'),A('jump','gapA'),A('cross','gapA'),A('exit','doorA')],
 [R('rotate','wheelA',[],{'flag:aligned':True},'Plateformes et trous alignés. ',135),
  R('cross','gapA',[C('angle:wheelA',135)],{'flag:crossed_gap':True},'Saut franchi.')],
 hint='Un seul angle de roue aligne tous les passages.'))

# 27 prime numbers incl 17 final
prime_vals=[2,3,5,7,11,13,17]
decoys=[4,6,9,12,15]
dev=[D(f'n{n}','number_token',220+i*165,315 if i%2==0 else 395,36,36,value=n) for i,n in enumerate(prime_vals+decoys)]
levels.append(base(27,'Les nombres premiers',['prime_filter','ordered_collection','decoy_failure'],dev+[D('doorA','exit_gate',2480,354,65,90,number=17)],
 [C('sequence:primes',7)],
 [*[A('collect',f'n{n}') for n in prime_vals],A('exit','doorA')],
 sequences=[{'id':'primes','items':[f'n{n}' for n in prime_vals],'decoys':[f'n{n}' for n in decoys],'failOnWrong':True}],
 hint='Récupère les nombres premiers en ordre croissant. Le dernier est 17, inscrit sur la porte.',
 design_note='Sur mobile, il faudra tester la lisibilité des nombres et l’ordre spatial de collecte.'))

# 28 walls by crates and trampoline
levels.append(base(28,'Ascenseur de murs',['weighted_walls','spring_move','geometry_combo'],[
 D('crateA','crate',250),D('crateB','crate',350),D('plateA','weight_button',870,433,82,11),D('plateB','weight_button',1100,433,82,11),
 D('wallA','lift_wall',1440,300,55,144),D('wallB','lift_wall',1640,265,55,179),
 D('springA','movable_spring',1850,428,85,16),D('doorA','exit_gate',2480,354,65,90)],
 [C('flag:walls_aligned'),C('flag:spring_positioned')],
 [A('place','crateA','plateB'),A('place','crateB','plateA'),A('press','align'),A('move','springA','launch_marker'),A('exit','doorA')],
 [R('press','align',[C('at:crateA','plateB'),C('at:crateB','plateA')],{'flag:walls_aligned':True},'Les murs sont à la bonne hauteur.'),
  R('move','springA',[C('flag:walls_aligned')],{'flag:spring_positioned':True},'Le tremplin est placé.','launch_marker')],
 hint='Le poids de chaque caisse détermine la hauteur des murs ; place ensuite le tremplin.'))

# 29 word OVER after GAME
letter_tokens=[('O1','O'),('V1','V'),('E1','E'),('R1','R'),('E2','E'),('X1','X')]
levels.append(base(29,'GAME → OVER',['word_association','ordered_letters','distractors'],[
 *[D(k,'letter_token',300+i*280,355 if i%2 else 398,40,44,letter=v) for i,(k,v) in enumerate(letter_tokens)],
 D('doorA','word_exit',2480,354,65,90,word='GAME')],
 [C('sequence:over',4)],
 [*[A('collect',x) for x in ['O1','V1','E1','R1']],A('exit','doorA')],
 sequences=[{'id':'over','items':['O1','V1','E1','R1'],'decoys':['E2','X1'],'failOnWrong':True}],
 hint='Le mot indiqué est GAME. Quel mot célèbre le complète ? Collecte O V E R dans cet ordre.'))

# 30 IQ 11th + 10th combined
levels.append(base(30,'Double test et gravité',['iq_series','letters_and_numbers','prime_series','gravity_flip','reverse_controls'],[
 D('panelA','sequence_board',290,255,350,82,text='A1 C3 F6 J10 O15 ?'),
 D('panelB','sequence_board',750,255,340,82,text='2 3 5 7 11 ?'),
 D('U21','iq_token',1350,390,70,40),D('T20','iq_token',1460,390,70,40),
 D('13','iq_token',1660,390,70,40),D('12','iq_token',1780,390,70,40),
 D('gravityZone','gravity_zone',1920,250,100,194),D('reverseZone','reverse_controls_zone',2130,280,110,164),
 D('doorA','exit_gate',2480,354,65,90)],
 [C('sequence:iq_letters',1),C('sequence:iq_numbers',1),C('flag:gravity_on'),C('flag:controls_reversed')],
 [A('collect','U21'),A('collect','13'),A('enter','gravityZone','up'),A('enter','reverseZone','reversed'),A('exit','doorA')],
 [R('enter','gravityZone',[C('sequence:iq_letters',1),C('sequence:iq_numbers',1)],{'flag:gravity_on':True},'La gravité pointe vers le haut.','up'),
  R('enter','reverseZone',[C('flag:gravity_on')],{'flag:controls_reversed':True},'Gauche et droite sont inversés.','reversed')],
 sequences=[{'id':'iq_letters','items':['U21'],'decoys':['T20'],'failOnWrong':True},
            {'id':'iq_numbers','items':['13'],'decoys':['12'],'failOnWrong':True}],
 hint='Trouve le symbole qui complète A1 C3 F6 J10 O15 ? et le nombre qui suit 2 3 5 7 11 ?',
 design_note='Combine les deux suites avec les zones de gravité et de commandes inversées ; un mauvais choix bloque la porte.'))

# World4 combinations: each level 2+ mechanisms from world3 with a novel twist.
levels.append(base(31,'Pont de sucre pondéré',['bridge_push','weighted_plate','prime_choice'],[
 D('bridgeA','sliding_platform',610,400,170,22),D('gapA','ground_gap',840,444,215,140),
 D('crate2','crate',260,weight=2),D('crate3','crate',330,weight=3),D('plateA','weighted_button',1540,432,110,12,requiredWeight=5),
 D('doorA','exit_gate',2480,354,65,90)],
 [C('flag:bridge_set'),C('flag:five_weight')],
 [A('push','bridgeA','gapA'),A('place','crate2','plateA'),A('place','crate3','plateA'),A('press','weigh'),A('exit','doorA')],
 [R('push','bridgeA',[],{'flag:bridge_set':True},'Pont déplacé.','gapA'),
  R('press','weigh',[C('at:crate2','plateA'),C('at:crate3','plateA'),C('flag:bridge_set')],{'flag:five_weight':True},'2+3=5 : sortie déverrouillée.')],
 hint='Deux caisses pèsent 2 et 3 : fabrique 5 pour sortir.'))

levels.append(base(32,'Mot miroir et roue',['word_reverse','wheel_angle','moving_platform'],[
 D('panelA','word_board',300,330,220,44,text='SUGAR'),
 *[D(f'letter_{i}','letter_token',780+i*155,390,36,43,letter=ch) for i,ch in enumerate('RAGUS')],
 D('wheelA','rotating_wheel',1750,345,80,80),D('doorA','exit_gate',2480,354,65,90)],
 [C('sequence:reverse',5),C('flag:wheel_aligned')],
 [*[A('collect',f'letter_{i}') for i in range(5)],A('rotate','wheelA',180),A('exit','doorA')],
 [R('rotate','wheelA',[C('sequence:reverse',5)],{'flag:wheel_aligned':True},'Roue déverrouillée par le mot miroir.',180)],
 sequences=[{'id':'reverse','items':[f'letter_{i}' for i in range(5)],'decoys':[],'failOnWrong':True}],
 hint='Lis SUGAR à l’envers, collecte R A G U S puis tourne la roue à 180°.'))

levels.append(base(33,'Deux poids, trois vitesses',['platform_cadence','prime_weights','gravity_toggle'],[
 D('crate2','crate',290,weight=2),D('crate3','crate',380,weight=3),
 D('pad1','speed_button',840,433,85,11),D('pad2','speed_button',960,433,85,11),
 D('pad3','speed_button',1080,433,85,11),D('platformA','moving_platform',1420,335,125,20),
 D('gravityZone','gravity_zone',1850,210,200,234),D('doorA','exit_gate',2480,354,65,90)],
 [C('flag:cadence'),C('flag:gravity_ready')],
 [A('place','crate2','pad1'),A('place','crate3','pad3'),A('press','sync'),A('enter','gravityZone','up'),A('exit','doorA')],
 [R('press','sync',[C('at:crate2','pad1'),C('at:crate3','pad3')],{'flag:cadence':True},'Plateformes synchronisées.'),
  R('enter','gravityZone',[C('flag:cadence')],{'flag:gravity_ready':True},'Le champ inversé donne accès à la sortie.','up')],
 hint='Associe les poids premiers à la cadence et inverse la gravité au bon endroit.'))

levels.append(base(34,'Le bouton qui ment',['inverted_button','move_crate','spring_path'],[
 D('crateA','crate',250),D('padA','inverse_button',800,432,100,12),
 D('springA','movable_spring',1440,430,90,14),D('doorA','exit_gate',2480,354,65,90)],
 [C('flag:door_unlocked'),C('flag:spring_ready')],
 [A('place','crateA','padA'),A('press','padA'),A('release','padA'),A('move','springA','launch'),A('exit','doorA')],
 [R('release','padA',[C('at:crateA','padA')],{'flag:door_unlocked':True},'Un relâchement ouvre la porte.'),
  R('move','springA',[C('flag:door_unlocked')],{'flag:spring_ready':True},'Tremplin repositionné.','launch')],
 hint='Le bouton n’agit pas quand on appuie, mais quand on le relâche.'))

levels.append(base(35,'Nombres, mur et triangle',['number_sequence','surprise_wall','moving_spike'],[
 D('panelA','sequence_board',250,287,230,52,text='1 4 9 16 ?'),
 D('25','number_token',780,390,45,40),D('24','number_token',870,390,45,40),
 D('wallA','surprise_wall',1500,300,70,144),D('spikeA','moving_spike',1880,416,48,28),D('doorA','exit_gate',2480,354,65,90)],
 [C('sequence:squares',1),C('flag:wall_dodged'),C('flag:spike_dodged')],
 [A('collect','25'),A('jump','wallA'),A('jump','spikeA'),A('exit','doorA')],
 [R('jump','wallA',[C('sequence:squares',1)],{'flag:wall_dodged':True},'Mur anticipé.'),
  R('jump','spikeA',[C('flag:wall_dodged')],{'flag:spike_dodged':True},'Triangle évité.')],
 sequences=[{'id':'squares','items':['25'],'decoys':['24'],'failOnWrong':True}],
 hint='La suite des carrés donne 25. Le mur et le triangle sont ensuite à anticiper.'))

levels.append(base(36,'Mélange et palindrome',['mastermind','word_palindrome','crate_buttons'],[
 *[D(f'crate{i}','crate',230+i*85,weight=1) for i in range(1,6)],
 *[D(f'pad{i}','code_button',1120+i*145,433,76,11) for i in range(1,4)],
 D('panelA','word_board',850,270,150,45,text='RADAR'),D('doorA','exit_gate',2480,354,65,90)],
 [C('solved:code'),C('flag:palindrome')],
 [A('submit','code',{'pad1':'crate5','pad2':'crate3','pad3':'crate1'}),A('press','check_word','RADAR'),A('exit','doorA')],
 [R('press','check_word',[C('solved:code')],{'flag:palindrome':True},'RADAR se lit dans les deux sens.','RADAR')],
 combination={'id':'code','pads':['pad1','pad2','pad3'],'target':{'pad1':'crate5','pad2':'crate3','pad3':'crate1'},'feedback':'count_correct_positions_only','maxGuesses':8},
 hint='Trouve la combinaison de caisses, puis confirme le palindrome.'))

levels.append(base(37,'L’échelle des nombres',['prime_filter','gravity_flip','moving_platform'],[
 *[D(f'n{v}','number_token',270+i*205,350,43,43,value=v) for i,v in enumerate([2,3,5,7,4,8])],
 D('gravityZone','gravity_zone',1610,280,145,164),D('platformA','moving_platform',1870,305,155,20),
 D('doorA','exit_gate',2480,354,65,90)],
 [C('sequence:primes',4),C('flag:gravity_flipped'),C('flag:platform_crossed')],
 [*[A('collect',f'n{n}') for n in (2,3,5,7)],A('enter','gravityZone','up'),A('cross','platformA'),A('exit','doorA')],
 [R('enter','gravityZone',[C('sequence:primes',4)],{'flag:gravity_flipped':True},'Gravité inversée !','up'),
  R('cross','platformA',[C('flag:gravity_flipped')],{'flag:platform_crossed':True},'Plateforme franchie.')],
 sequences=[{'id':'primes','items':['n2','n3','n5','n7'],'decoys':['n4','n8'],'failOnWrong':True}],
 hint='Recueille 2,3,5,7 puis passe sur la plateforme sous gravité inversée.'))

levels.append(base(38,'Engrenage et équilibre',['wheel_angle','weighted_walls','spring_alignment'],[
 D('wheelA','rotating_wheel',290,365,80,80),D('crateA','crate',580),D('crateB','crate',690),
 D('padA','pressure_button',1090,433,85,11),D('padB','pressure_button',1240,433,85,11),
 D('wallA','lift_wall',1580,280,62,164),D('springA','movable_spring',1980,430,95,14),
 D('doorA','exit_gate',2480,354,65,90)],
 [C('flag:wheel_ok'),C('flag:walls_ok'),C('flag:spring_ok')],
 [A('rotate','wheelA',270),A('place','crateA','padB'),A('place','crateB','padA'),A('press','align_walls'),A('move','springA','spotA'),A('exit','doorA')],
 [R('rotate','wheelA',[],{'flag:wheel_ok':True},'Premier engrenage verrouillé.',270),
  R('press','align_walls',[C('flag:wheel_ok'),C('at:crateA','padB'),C('at:crateB','padA')],{'flag:walls_ok':True},'Murs alignés.'),
  R('move','springA',[C('flag:walls_ok')],{'flag:spring_ok':True},'Tremplin aligné.','spotA')],
 hint='270° pour la roue, puis caisses sur plaques opposées et tremplin.'))

levels.append(base(39,'Le mot qui ouvre la trappe',['letter_collection','weight_plate','false_door'],[
 *[D(f'letter_{i}','letter_token',230+i*215,385,42,45,letter=ch) for i,ch in enumerate('SWEET')],
 D('crateA','crate',1440),D('padA','pressure_button',1780,433,100,11),
 D('fakeDoor','false_exit',2100,354,65,90),D('doorA','exit_gate',2480,354,65,90)],
 [C('sequence:sweet',5),C('flag:weight_ok'),C('flag:fake_revealed')],
 [*[A('collect',f'letter_{i}') for i in range(5)],A('place','crateA','padA'),A('press','reveal'),A('exit','doorA')],
 [R('place','crateA',[C('sequence:sweet',5)],{'flag:weight_ok':True},'La plaque s’active.','padA'),
  R('press','reveal',[C('flag:weight_ok')],{'flag:fake_revealed':True},'La première porte était un leurre.')],
 sequences=[{'id':'sweet','items':[f'letter_{i}' for i in range(5)],'decoys':[],'failOnWrong':True}],
 hint='Le mot SWEET, une caisse, puis la révélation d’une fausse porte.'))

levels.append(base(40,'Le coffre des quatre lois',['iq_series','weight_balance','wheel_angle','word_code','moving_bridge'],[
 D('panelA','sequence_board',210,300,230,60,text='2 6 12 20 ?'),D('30','number_token',560,390,45,45),D('28','number_token',650,390,45,45),
 D('crate2','crate',780,weight=2),D('crate3','crate',880,weight=3),
 D('padA','heavy_pressure_plate',1120,433,140,11,requiredWeight=5),
 D('wheelA','rotating_wheel',1450,365,80,80),D('wordA','word_board',1720,300,140,60,text='HOT'),
 D('bridgeA','sliding_platform',1980,410,160,22),D('gapA','ground_gap',2180,444,200,140),
 D('doorA','exit_gate',2612,354,65,90)],
 [C('sequence:series',1),C('flag:weight_ok'),C('flag:wheel_ok'),C('flag:word_ok'),C('flag:bridge_ok')],
 [A('collect','30'),A('place','crate2','padA'),A('place','crate3','padA'),A('press','weigh'),A('rotate','wheelA',90),A('press','wordA','COLD'),A('push','bridgeA','gapA'),A('exit','doorA')],
 [R('press','weigh',[C('at:crate2','padA'),C('at:crate3','padA'),C('sequence:series',1)],{'flag:weight_ok':True},'La somme vaut 5.'),
  R('rotate','wheelA',[C('flag:weight_ok')],{'flag:wheel_ok':True},'Angle 90° validé.',90),
  R('press','wordA',[C('flag:wheel_ok')],{'flag:word_ok':True},'HOT a pour contraire COLD.','COLD'),
  R('push','bridgeA',[C('flag:word_ok')],{'flag:bridge_ok':True},'Pont abaissé.','gapA')],
 sequences=[{'id':'series','items':['30'],'decoys':['28'],'failOnWrong':True}],
 hint='Suite x(x+1), somme des poids, angle de roue, mot contraire HOT/COLD et pont final.',world_width=2820,
 design_note='Prototype de cinq modules successifs ; ajouter des indices pour la langue avant sa publication.'))

if __name__=='__main__':
    print('Wrote',len(levels),'puzzle prototypes:',levels[0].name,'to',levels[-1].name)
