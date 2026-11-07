#!/usr/bin/env python3
"""ESCAPE SHIFT V19 - edit/validate/preview JSON levels. Python 3.10+.

No Android dependency for edit/check. Plotting requires matplotlib (optional).
Playability is validated by the Kotlin test runner using the real game physics.
"""
import argparse
import json
from pathlib import Path
from datetime import datetime
from collections import Counter

PROJECT = Path(__file__).resolve().parents[2]
LEVELS = PROJECT / 'app' / 'src' / 'main' / 'assets' / 'levels'
BACKUPS = Path(__file__).resolve().parent / 'backups'

SUPPORTED = {'wall','platform','crate','bridge','spike','laser_red','laser_blue','laser_timed','spring','key','coin','switch','conveyor','saw','monster','door','crate_switch','snowflake'}
ROUTEABLE = {'spike','laser_red','laser_blue','laser_timed','spring','key','switch','conveyor','saw','monster','wall'}
INT_FIELDS = {'count', 'phase', 'period', 'activeTicks', 'delayTicks', 'durationTicks', 'transitionTicks', 'periodTicks'}
NUMBER_FIELDS = {'x','y','width','height','count','spacing','phase','period','activeTicks','amplitude','offsetX','speed', 'radius', 'delayTicks', 'durationTicks', 'transitionTicks', 'periodTicks', 'travelX', 'travelY', 'thresholdSpeed'}
TRAP_TYPES = {'collapse_floor', 'rising_spikes', 'surprise_wall', 'moving_platform', 'moving_exit', 'reverse_zone'}
TRIGGERS = {'player_near', 'player_on', 'player_jump', 'approach_fast', 'player_enter'}

def path_for(n):
    if not 1 <= n <= 20: raise ValueError('Only levels 1..20 exist')
    return LEVELS / f'level_{n:02d}.json'

def load(n):
    return json.loads(path_for(n).read_text(encoding='utf-8'))

def validate(d):
    errors=[]
    def expect(cond,msg):
        if not cond:errors.append(msg)
    expect(d.get('schemaVersion')==1,'schemaVersion must be 1')
    expect(d.get('world') == (d.get('level', 1)-1)//10+1, 'level/world mismatch')
    expect(isinstance(d.get('worldWidth'),(int,float)) and d['worldWidth']>500,'worldWidth must be > 500')
    expect(isinstance(d.get('playerStart'),dict),'playerStart missing')
    ground=d.get('ground',{})
    expect(isinstance(ground,dict) and 'y' in ground and 'depth' in ground,'ground missing y/depth')
    width=d.get('worldWidth',0)
    seen=set()
    old=0
    for gap in sorted(ground.get('gaps',[]),key=lambda g:g.get('x',0)):
        ident=gap.get('id')
        expect(bool(ident) and ident not in seen,f'invalid/repeated gap id {ident}')
        seen.add(ident)
        x=gap.get('x',-1); w=gap.get('width',-1)
        expect(isinstance(x,(int,float)) and isinstance(w,(int,float)) and old<=x and w>0 and x+w<=width,f'invalid gap {ident} x={x} width={w}')
        old=x+w
    objects=d.get('objects',[])
    expect(isinstance(objects,list),'objects must be a list')
    for o in objects:
        ident=o.get('id'); t=o.get('type')
        expect(bool(ident) and ident not in seen,f'invalid/repeated object id {ident}')
        seen.add(ident)
        expect(t in SUPPORTED,f'unsupported object type {t}')
        x=o.get('x',-1); y=o.get('y',0); w=o.get('width',-1); h=o.get('height',-1)
        expect(all(isinstance(v,(int,float)) for v in (x,y,w,h)) and x>=0 and w>0 and h>0,f'bad geometry {ident}')
        count=o.get('count',1);spacing=o.get('spacing',0)
        expect(isinstance(count,int) and 1<=count<=3 and spacing>=0,f'bad count/spacing {ident}')
        if isinstance(w,(int,float)):expect(x+count*w+(count-1)*spacing<=width+1e-5,f'{ident} out of world')
        period=o.get('period',120);active=o.get('activeTicks',64)
        expect(isinstance(period,int) and period>0 and 0<active<=period,f'bad period {ident}')
    traps=d.get('traps',[])
    expect(isinstance(traps,list),'traps must be a list')
    for t in traps:
        ident=t.get('id')
        expect(isinstance(ident,str) and ident not in seen, f'invalid or repeated trap id {ident}')
        seen.add(ident)
        kind=t.get('type')
        expect(kind in TRAP_TYPES, f'unknown trap type: {kind}')
        expect(t.get('trigger','player_near') in TRIGGERS, f'invalid trigger {ident}')
        x=t.get('x',-1);y=t.get('y');w=t.get('width',-1);h=t.get('height',-1)
        expect(all(isinstance(v,(int,float)) for v in (x,y,w,h)),f'invalid trap geometry: {ident}')
        if all(isinstance(v,(int,float)) for v in (x,y,w,h)):
            expect(x>=0 and w>0 and h>0 and x+w<=width, f'invalid trap bounds {ident}')
        for k in ('radius','delayTicks','durationTicks'):
            expect(isinstance(t.get(k,0),(int,float)) and t.get(k,0)>=0, f'invalid {ident}.{k}')
        for k in ('transitionTicks','periodTicks'):
            expect(isinstance(t.get(k,1),int) and t.get(k,1)>0, f'invalid {ident}.{k}')
        if kind=='collapse_floor':
            expect(y==ground.get('y'), f'{ident} must be at ground.y')
            expect(all(not (x<g['x']+g['width'] and g['x']<x+w) for g in ground.get('gaps', [])),f'{ident} intersects gap')
    expect(sum(t.get('type')=='moving_exit' for t in traps)<=1,'only one moving exit')
    expect(sum(o.get('type')=='door' for o in objects)==1,'exactly one door required')
    expect(sum(o.get('type')=='crate' for o in objects) <= (1 if d.get('world') == 1 else 3), 'too many crates')
    expect(sum(o.get('type')=='bridge' for o in objects)<=1,'only one SHIFT bridge supported currently')
    route=d.get('route',[])
    types={o['id']:o['type'] for o in objects if 'id' in o and 'type' in o}
    types.update({g['id']:'gap' for g in ground.get('gaps',[]) if 'id' in g})
    expect(len(route)==len(set(route)),'duplicate route IDs')
    for ident in route:
        expect(ident in types, f'route element {ident} missing')
        if ident in types:expect(types[ident] in ROUTEABLE|{'gap'},f'{ident} cannot be a route checkpoint')
    expect(0<=d.get('backtrackCount',0)<=len(route),'bad backtrackCount')
    return errors

def edit(level,object_id,gap_id,trap_id,assignments):
    d=load(level)
    if object_id:
        matches=[o for o in d['objects'] if o.get('id')==object_id]
    elif trap_id:
        matches=[o for o in d.get('traps',[]) if o.get('id')==trap_id]
    else:
        matches=[o for o in d['ground']['gaps'] if o.get('id')==gap_id]
    if len(matches)!=1:
        raise ValueError(f'Unknown object/gap/trap: {object_id or gap_id or trap_id}')
    target=matches[0]
    for a in assignments:
        if '=' not in a:raise ValueError(f'Use FIELD=NUMBER, got {a}')
        key,value=a.split('=',1)
        if key not in NUMBER_FIELDS:raise ValueError(f'Unsupported field {key}')
        if gap_id and key not in {'x','width'}:raise ValueError('Only x/width can be edited on a gap')
        try:v=int(value) if key in INT_FIELDS else float(value)
        except ValueError:raise ValueError(f'Invalid numeric value {value}')
        target[key]=v
    issues=validate(d)
    if issues:raise ValueError('File not written:\n'+'\n'.join('  - '+e for e in issues))
    file=path_for(level)
    BACKUPS.mkdir(parents=True,exist_ok=True)
    backup=BACKUPS/(f'level_{level:02d}_{datetime.now():%Y%m%d_%H%M%S}.json')
    backup.write_bytes(file.read_bytes())
    file.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print('Saved:',file)
    print('Backup:',backup)
    print('REMINDER: run the Kotlin virtual player; passing static checks does not guarantee a playable level.')

def preview(d,output):
    try:
        import matplotlib.pyplot as plt
        from matplotlib.patches import Rectangle, Polygon
    except ImportError:
        raise RuntimeError('Preview requires matplotlib: py -m pip install matplotlib')
    palette={'wall':'#53606d','platform':'#e8af51','crate':'#c6813d','bridge':'#38baa1',
             'laser_red':'#ed5f5f','laser_blue':'#5d9ce8','laser_timed':'#f4ba67',
             'spring':'#50cf94','key':'#f9df77','coin':'#eec75a','switch':'#6bc7d5',
             'conveyor':'#39b1c4','saw':'#d1d5db','monster':'#ee7e3b','door':'#87efb5','crate_switch':'#ffe28a','snowflake':'#f8fafc'}
    fig,ax=plt.subplots(figsize=(15,4.5))
    ax.set_facecolor('#111923');fig.patch.set_facecolor('#111923')
    world_width=d['worldWidth'];gy=d['ground']['y'];gd=d['ground']['depth']
    gaps=sorted(d['ground']['gaps'],key=lambda g:g['x'])
    segments=[];cursor=0
    for g in gaps:
        if g['x']>cursor:segments.append((cursor,g['x']))
        cursor=g['x']+g['width']
    if cursor<world_width:segments.append((cursor,world_width))
    for x1,x2 in segments:ax.add_patch(Rectangle((x1,gy),x2-x1,gd,color='#303943',alpha=.9))
    for o in d['objects']:
        x,y,w,h=o['x'],o['y'],o['width'],o['height'];t=o['type']
        if t=='spike':
            for i in range(o.get('count',1)):
                xx=x+i*(w+o.get('spacing',0))
                ax.add_patch(Polygon([(xx,y+h),(xx+w/2,y),(xx+w,y+h)],closed=True,color='#f39239'))
        else:
            ax.add_patch(Rectangle((x,y),w,h,color=palette.get(t,'#eff1f2'),alpha=.9))
        if t!='coin':ax.text(x,y-5,o['id'],color='#f4f4f4',fontsize=6,rotation=45)
    trapPalette={'collapse_floor':'#ff8750','rising_spikes':'#e83232','surprise_wall':'#ffcb52','moving_platform':'#72dcff','moving_exit':'#a0f4b7','reverse_zone':'#9955ff'}
    for tr in d.get('traps',[]):
        ax.add_patch(Rectangle((tr['x'],tr['y']),tr['width'],tr['height'],
            fill=False,edgecolor=trapPalette.get(tr['type'],'white'),linewidth=1.7,linestyle='--'))
        ax.text(tr['x'],tr['y']-7,tr['type'],color=trapPalette.get(tr['type'],'white'),fontsize=7)
    start=d['playerStart'];ax.scatter([start['x']],[start['y']],s=65,c='#ffbf77',marker='o',label='Player start')
    report=Path(__file__).resolve().parent/'reports'/f"level_{d['level']:02d}.csv"
    if report.is_file():
        import csv
        with report.open() as f:
            rows=list(csv.DictReader(f))
        ax.plot([float(row['x']) for row in rows],[float(row['y']) for row in rows],color='#60c6e7',alpha=.75,lw=1.2,label='Scripted winning path (last test)')
    ax.set_xlim(0,world_width)
    ax.set_ylim(max(600,gy+gd+15),min(-30,start['y']-150))
    ax.set_title(f"ESCAPE SHIFT  |  World 1, Level {d['level']:02d}  |  {len(d['objects'])} objects",color='white')
    ax.set_xlabel('x  —  world units',color='white');ax.set_ylabel('y (downwards)',color='white')
    ax.tick_params(colors='white');ax.grid(alpha=.12)
    ax.legend(loc='upper right',facecolor='#1a2532',labelcolor='white')
    fig.tight_layout();output=Path(output);output.parent.mkdir(parents=True,exist_ok=True)
    fig.savefig(output,dpi=140,facecolor=fig.get_facecolor());plt.close(fig)
    print('Preview:',output)

def score(d):
    counter=Counter(o['type'] for o in d['objects'])
    count=sum(counter.values())
    weights={'spike':2,'laser_red':1.5,'laser_blue':2,'laser_timed':3,'saw':3.5,'monster':4,'spring':1,'key':2,'switch':2,'crate_switch':4,'snowflake':2,'conveyor':1.5,'crate':1.5,'bridge':2}
    fine_spikes = sum((o.get('count',1)-1)*2.5 for o in d['objects'] if o['type']=='spike')
    saw_sweep = sum(o.get('amplitude',0)*.025 for o in d['objects'] if o['type']=='saw')
    jump_distance = sum(g['width']*.025 for g in d['ground']['gaps'])
    pressure_puzzle = 6*counter.get('crate_switch',0)
    trapScore = sum({'collapse_floor':5,'rising_spikes':4,'surprise_wall':4,'moving_exit':4,'reverse_zone':6,'moving_platform':3}.get(t['type'],0) for t in d.get('traps',[]))
    return round(sum(weights.get(k,0)*v for k,v in counter.items()) + trapScore + fine_spikes + saw_sweep + jump_distance + pressure_puzzle + .004*d['worldWidth'] + 3*d.get('backtrackCount',0), 1)

def main():
    ap=argparse.ArgumentParser(description='Edit and validate ESCAPE SHIFT worlds 1 & 2 levels')
    sub=ap.add_subparsers(dest='command',required=True)
    sub.add_parser('check',help='Validate all 20 JSON files')
    p=sub.add_parser('list',help='List objects and gaps for one level');p.add_argument('level',type=int)
    p=sub.add_parser('preview',help='Draw a level layout PNG');p.add_argument('level',type=int);p.add_argument('--out',default=None)
    p=sub.add_parser('edit',help='Update coordinates or dimensions, with automatic backup')
    p.add_argument('level',type=int)
    selector=p.add_mutually_exclusive_group(required=True)
    selector.add_argument('--object',dest='object_id');selector.add_argument('--gap',dest='gap_id');selector.add_argument('--trap',dest='trap_id')
    p.add_argument('--set',action='append',required=True,metavar='FIELD=NUMBER')
    args=ap.parse_args()
    if args.command=='check':
        fail=False
        for n in range(1,21):
            d=load(n);errors=validate(d);status='PASS' if not errors else 'FAIL'
            print(f'Level {n:02d}: {status}  objects={len(d["objects"])}  gaps={len(d["ground"]["gaps"])}  difficulty_proxy={score(d)}')
            for e in errors:print(' -',e)
            fail|=bool(errors)
        if fail:raise SystemExit(1)
    elif args.command=='list':
        d=load(args.level)
        print('Gaps:')
        for item in d['ground']['gaps']:print(f'  {item["id"]:28s} x={item["x"]:6} width={item["width"]}')
        print('Objects:')
        for item in d['objects']:print(f'  {item["id"]:28s} {item["type"]:15s} x={item["x"]:6} y={item["y"]:6} w={item["width"]:5} h={item["height"]:5}')
        print('Event traps:')
        for t in d.get('traps',[]):
            print(f'  {t["id"]:28s} {t["type"]:15s} x={t["x"]} y={t["y"]} trigger={t.get("trigger","player_near")} delay={t.get("delayTicks",12)}')
        print('Route:', ' → '.join(d['route']))
    elif args.command=='preview':
        d=load(args.level);errors=validate(d)
        if errors:raise ValueError('\n'.join(errors))
        preview(d,args.out or str(Path(__file__).resolve().parent/'previews'/f'level_{args.level:02d}.png'))
    elif args.command=='edit':edit(args.level,args.object_id,args.gap_id,args.set)

if __name__=='__main__':main()
