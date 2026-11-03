from pathlib import Path
import json
ROOT=Path(__file__).resolve().parent / 'candidates'
ROOT.mkdir(parents=True,exist_ok=True)
print('Candidate files only. They are NOT automatically validated or copied into the Android game.')
# level, jump width, amplitude class, saw count, spike groups, spike multiplicity, monsters, crates, switches, pressure, walls, flakes, shift changes
CFG=[
(11,90,23,1,2,1,1,1,1,0,1,1,1),
(12,94,50,1,3,1,1,1,1,0,1,2,1),
(13,100,23,1,2,2,1,1,1,0,1,3,1),
(14,113,50,1,3,2,1,1,1,0,1,4,2),
(15,118,23,1,3,2,1,1,1,0,2,5,2),
(16,123,50,1,3,2,2,2,1,0,2,6,2),
(17,128,50,1,4,2,2,2,2,1,2,7,3),
(18,145,80,2,4,2,2,2,2,1,2,8,3),
(19,149,80,3,1,3,5,3,2,1,3,9,3),
(20,155,80,3,1,3,5,3,2,1,3,10,4)
]
# Assemble varied progression rather than all of one type in a row.
NAME_FR={11:'Premiers flocons',12:'Sentier gelé',13:'Pics givrés',14:'Couloir des vents',15:'Remparts de glace',16:'Patrouille blanche',17:'Plaque de pression',18:'Tempête mécanique',19:'La pente interdite',20:'Le sommet verrouillé'}

def obj(ident,t,x,y,w,h,**kw):
    return dict(id=ident,type=t,x=round(x,2),y=round(y,2),width=round(w,2),height=round(h,2),**kw)
for (level,gapw,sawamp,sawn,groups,multi,mons,crates,buttons,plate,walls,flake,shifts) in CFG:
    objects=[]; route=[]; gaps=[]; modules=[]
    # Three physically climbable walls with matching movable crates.
    for i in range(crates):
        cx=225+i*610
        wx=cx+150
        objects.append(obj(f'crate_{i+1:02d}','crate',cx,400,46,44))
        objects.append(obj(f'crate_wall_{i+1:02d}','wall',wx,324,42,120))
        modules.append((cx,wx))
        if i==0 and plate:
            objects.append(obj('crate_pressure_01','crate_switch',wx-46,431,46,13))
    # Additional low walls to traverse with a normal jump.
    for i in range(max(0,walls-crates)):
        wx=modules[-1][1]+260+i*280
        ident=f'free_wall_{i+1:02d}'
        objects.append(obj(ident,'wall',wx,369,38,75))
        route.append(ident)
    startHazard = modules[-1][1]+(340 if walls==crates else 340+max(0,walls-crates)*280)
    # At least 7 unique positions in early levels, more as difficulty rises.
    events=[('gap',1)] + [('spike', i+1) for i in range(groups)] + [('saw',i+1) for i in range(sawn)] + [('monster',i+1) for i in range(mons)] + [('laser',i+1) for i in range(shifts)] + [('switch',i+1) for i in range(buttons)]
    # shuffled deterministic order, maintaining laser alternation and chronological route.
    import random
    rng=random.Random(2200+level)
    # interleave by types fairly: order based seed rotate. Avoid same hazard consecutive if possible
    queues={t:[(t,k) for tt,k in events if tt==t] for t in ['gap','spike','saw','monster','laser','switch']}
    order=[]; prev=None
    priority=['gap','spike','saw','monster','laser','switch']
    if level%2==0: priority=['gap','saw','spike','laser','monster','switch']
    if level>=17: priority=['gap','spike','laser','monster','saw','switch']
    # rotate starting order (gap first prevents hazards in crate sector)
    while any(queues.values()):
        for t in priority:
            if queues[t]:
                order.append(queues[t].pop(0))
    # Most switches near the tail of the run, so the finish requires them.
    order=[ev for ev in order if ev[0]!='switch']+[ev for ev in order if ev[0]=='switch']
    pos=startHazard
    for t,i in order:
        if t=='gap':
            gaps.append(dict(id='gap_01',x=pos,width=gapw))
            route.append('gap_01')
        elif t=='spike':
            ident=f'spike_{i:02d}'
            objects.append(obj(ident,'spike',pos,421 if multi==1 else 421,34,23,count=multi,spacing=3))
            route.append(ident)
        elif t=='saw':
            ident=f'saw_{i:02d}'
            objects.append(obj(ident,'saw',pos,410,30,34,amplitude=sawamp,phase=(level*17+i*29)%160,period=160))
            route.append(ident)
        elif t=='monster':
            ident=f'monster_{i:02d}'
            objects.append(obj(ident,'monster',pos,398,62,46,amplitude=16 if level<16 else 22,phase=(level*23+i*31)%138,period=138))
            route.append(ident)
        elif t=='laser':
            ident=f'laser_{i:02d}'
            kind='laser_red' if i%2==1 else 'laser_blue'
            objects.append(obj(ident,kind,pos,291,16,153))
            route.append(ident)
        elif t=='switch':
            ident=f'switch_{i:02d}'
            objects.append(obj(ident,'switch',pos,431,47,13))
            route.append(ident)
        pos+=310 if t in {'gap','saw','monster','spike'} else 265
    # Falling snow balls are placed through the entire course; each has a deterministic moving circle.
    # First choose locations between challenges, out of crate area.
    end=pos
    for i in range(flake):
        x=startHazard+175+i*(end-startHazard-200)/max(1,flake)
        # tuned to keep visual density modest, not grouped on fixed hazards
        objects.append(obj(f'snowflake_{i+1:02d}','snowflake',x,92 + 12*(i%3),24,24,
                           phase=(34+i*31+level*7)%160,period=160,amplitude=0,
                           speed=160+(i%3)*17))
    # occasional elevated platform + bonus coin - route remains traversable from ground below.
    for i in range(1+level//16):
        x=startHazard+120+i*1250
        objects.append(obj(f'platform_{i+1:02d}','platform',x,344-20*(i%2),128,20))
        objects.append(obj(f'coin_{i+1:02d}','coin',x+50,308-20*(i%2),24,24))
    objects.append(obj('exit_door','door',pos+35,354,65,90))
    width=pos+225
    d=dict(schemaVersion=1,world=2,level=level,titleKey=f'level_{level:02d}_title',hintKey=f'level_{level:02d}_hint',intro='OPEN',backtrackCount=0,worldWidth=width,
           playerStart=dict(x=78,y=405),ground=dict(y=444,depth=146,gaps=gaps),objects=objects,route=route,
           difficulty=dict(jump={'90':'petit'}.get(str(gapw), 'petit' if gapw<=100 else 'moyen' if gapw<140 else 'grand'),
                           sawAmplitude=sawamp,spikeGroups=groups,spikesPerGroup=multi,
                           monsters=mons,crates=crates,buttons=buttons,cratePressurePads=plate,walls=walls,snowflakes=flake,shiftChanges=shifts),
           world2Modules=[dict(crateId=f'crate_{i+1:02d}',wallId=f'crate_wall_{i+1:02d}',mustClimb=True,pressureId='crate_pressure_01' if plate and i==0 else None) for i in range(crates)])
    (ROOT/f'level_{level:02d}.json').write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n')
    print(level,width,len(objects),len(route),len(gaps),len(d['world2Modules']))
