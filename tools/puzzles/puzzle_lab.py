#!/usr/bin/env python3
"""Logic-only solver, validator and CLI for experimental Escape Shift puzzles.
No claim of physical reachability: this engine models decisions, switches and gates,
not collisions, acceleration, timings, or actual device/player movement.
"""
import argparse, json, sys
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
LEVELS=ROOT/'app/src/main/assets/levels'
ALLOWED_ACTIONS={'push','return','place','move','rotate','press','release','stand','leave','collect','submit','jump','run','enter','cross','exit','wait'}
ID_CHARS=set('abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-')

class PuzzleError(ValueError): pass

def load(n):
    path=LEVELS/f'level_{n:02}.json'
    data=json.loads(path.read_text(encoding='utf8'))
    if 'puzzle' not in data: raise PuzzleError(f'Level {n}: no puzzle section')
    return data

class PuzzleEngine:
    def __init__(self,level):
        self.level=level
        self.p=level['puzzle']
        self.state={}
        self.failed=False
        self.won=False
        self.tick=0
        self.trace=[]
        self.feedback={}
        self.collected=set()
        self.sequence_steps={s['id']:0 for s in self.p.get('sequences',[])}
        self.devices={d['id']:d for d in self.p['devices']}
        self.rule_hits=[]
        self.expires={}  # key -> clock tick when the temporary flag turns off
    def get(self,key):
        if key.startswith('sequence:'): return self.sequence_steps.get(key.split(':',1)[1],0)
        return self.state.get(key,False)
    def conditions(self,clauses):
        return all(self.get(c['var'])==c['eq'] for c in clauses)
    def _sequenced_collect(self,target):
        known=False
        for seq in self.p.get('sequences',[]):
            items=seq['items']; decoys=seq.get('decoys',[])
            if target not in items and target not in decoys: continue
            known=True
            idx=self.sequence_steps[seq['id']]
            if idx < len(items) and target==items[idx] and target not in self.collected:
                self.sequence_steps[seq['id']]+=1
            elif seq.get('failOnWrong',True):
                self.failed=True
                self.trace.append(f'MAUVAIS OBJET : {target} casse la séquence {seq["id"]}')
            break
        return known
    def _expire_flags(self):
        for key,deadline in list(self.expires.items()):
            if self.tick >= deadline:
                self.state[key]=False
                del self.expires[key]
                self.trace.append('EXPIRATION : '+key)

    def act(self,evt):
        if self.won: raise PuzzleError('Puzzle already solved')
        action=evt['action']; target=evt.get('target',''); value=evt.get('value')
        if action not in ALLOWED_ACTIONS: raise PuzzleError(f'Unknown action {action}')
        self.tick+=1
        # Simple state mutation for generic action primitives.
        self._expire_flags()
        if action in ('place','move','push'):
            self.state['at:'+target]=value
        elif action=='rotate': self.state['angle:'+target]=value
        elif action=='press': self.state['pressed:'+target]=True
        elif action=='release': self.state['pressed:'+target]=False
        elif action=='stand': self.state['hero:'+target]=True
        elif action=='leave': self.state['hero:'+target]=False
        elif action=='enter': self.state['zone:'+target]=value
        elif action=='cross': self.state['crossed:'+target]=True
        elif action=='collect':
            if target in self.collected: self.failed=True
            self._sequenced_collect(target)
            self.collected.add(target)
        elif action=='submit':
            combo=self.p.get('combination')
            if combo is None or target!=combo['id']: raise PuzzleError('No such combination lock')
            if not isinstance(value,dict): raise PuzzleError('Submit value must map pads to crates')
            pads=combo['pads']; expected=combo['target']
            if set(value)!=set(pads) or len(set(value.values()))!=len(value):
                raise PuzzleError('Assign exactly one distinct crate to each pad')
            hits=sum(value[k]==expected[k] for k in pads)
            self.feedback[combo['id']]={'matches':hits,'total':len(pads),
                'description':f'{hits}/{len(pads)} bonnes positions (non révélées)'}
            self.state['solved:'+combo['id']]=hits==len(pads)
            self.trace.append('INDICE : '+self.feedback[combo['id']]['description'])
        elif action=='wait':
            self.tick+=int(value or 0)
            self._expire_flags()
        # Apply rule effects if BOTH event and conditional state match.
        for rule in self.p.get('rules',[]):
            on=rule['on']
            if on.get('action')!=action or on.get('target')!=target: continue
            if 'value' in on and on['value']!=value: continue
            if not self.conditions(rule.get('if',[])): continue
            self.state.update(rule.get('set',{}))
            for key,period in rule.get('expires',{}).items():
                self.expires[key]=self.tick+int(period)
            self.rule_hits.append((rule['on']['action'],target))
            self.trace.append('DÉCLENCHEMENT : '+rule.get('message',''))
        if action=='exit':
            self.won=not self.failed and self.conditions(self.p['goal'])
            self.trace.append('SORTIE : '+('OUVERTE' if self.won else 'VERROUILLÉE'))
        if not self.trace or action!='exit':
            self.trace.append(f'[{self.tick:03}] {action} {target} {value if value is not None else ""}'.strip())
        return self.won

def validate(level):
    errors=[]
    p=level['puzzle']; n=level['level']
    if n not in range(21,41): errors.append('Level not world3/4')
    if level['world']!=(n-1)//10+1: errors.append('Wrong world ID')
    if not p.get('prototype') and not (24 <= int(level.get('level', 0)) <= 30 and level.get('experimental', {}).get('status') == 'physical_beta'):
        errors.append('Puzzle must be a prototype or an implemented physical beta in 24..30')
    devices=p.get('devices',[]); ids=[d['id'] for d in devices]
    if len(ids)!=len(set(ids)): errors.append('Duplicate device ID')
    if not devices: errors.append('No devices')
    for d in devices:
        if not set(d['id'])<=ID_CHARS: errors.append('Invalid device ID '+d['id'])
        if d['x']<0 or d['x']+d['width']>level['worldWidth']: errors.append('Object outside world '+d['id'])
        if d['width']<=0 or d['height']<=0: errors.append('Non-positive size '+d['id'])
    if len(ids)>60: errors.append('Too many devices')
    if not p.get('solution') or p['solution'][-1]['action']!='exit': errors.append('Solution must end with exit')
    varset={'sequence:'+s['id'] for s in p.get('sequences',[])}
    for seq in p.get('sequences',[]):
        if len(seq['items'])!=len(set(seq['items'])): errors.append('Duplicate sequence item '+seq['id'])
        for token in seq['items']+seq.get('decoys',[]):
            if token not in ids: errors.append('Unknown sequence token '+token)
    for rule in p.get('rules',[]):
        if 'on' not in rule or 'set' not in rule: errors.append('Bad rule')
        varset.update(rule.get('set',{}).keys())
    for c in p['goal']:
        key=c['var']
        if key not in varset and not key.startswith(('at:','angle:','solved:','crossed:','pressed:','hero:')):
            errors.append('Goal not produced by rules or primitives: '+key)
    for act in p['solution']:
        if act.get('action') not in ALLOWED_ACTIONS: errors.append('Unknown action '+str(act))
        targ=act.get('target','')
        if act['action'] in {'collect','place','push','move','rotate','stand','leave','cross','enter'} and targ not in ids:
            errors.append(f'Unknown device referenced by action {targ}')
    return errors

def replay(n, verbose=False):
    data=load(n)
    errs=validate(data)
    if errs: return False,errs,None
    eng=PuzzleEngine(data)
    try:
        for step in data['puzzle']['solution']: eng.act(step)
    except Exception as exc:
        return False,[str(exc)],eng
    if not eng.won: return False,[f'Solution failed; failed={eng.failed}; state={eng.state}; seq={eng.sequence_steps}'],eng
    if verbose:
        print(f'\nNIVEAU {n} • {data["puzzle"]["title"]}')
        for item in eng.trace: print(' -',item)
        print('  => VALIDATION LOGIQUE : OK')
    return True,[],eng

def all_levels(): return range(21,41)
def run_suite(verbose=True):
    all_good=True; rows=[]
    for n in all_levels():
        ok,errors,engine=replay(n)
        # Anti-cheat test: trying only the exit must NEVER pass.
        blank=PuzzleEngine(load(n))
        blank.act({'action':'exit','target':'doorA'})
        if blank.won: ok=False;errors.append('Unprotected exit')
        # Some puzzle are ordered: verify decoy failure.
        seqs=load(n)['puzzle'].get('sequences',[])
        for seq in seqs:
            if seq.get('decoys'):
                test=PuzzleEngine(load(n))
                test.act({'action':'collect','target':seq['decoys'][0]})
                if not test.failed: ok=False;errors.append('Decoy not fatal for '+seq['id'])
        # Laser must be blocked after its timed wheel plate expires.
        if n==22:
            timer=PuzzleEngine(load(n))
            timer.act({'action':'push','target':'wheelA','value':'plateA'})
            timer.act({'action':'wait','target':'tick','value':91})
            timer.act({'action':'cross','target':'laserA'})
            timer.act({'action':'exit','target':'doorA'})
            if timer.won: ok=False;errors.append('Expired laser still open')
        # Basic reversed choice should not unlock a condition.
        rows.append((n,ok,len(load(n)['puzzle']['devices']),len(load(n)['puzzle']['solution']),errors))
        all_good &= ok
        if verbose: print(f'level_{n:02}: {"PASS" if ok else "FAIL"}  {len(load(n)["puzzle"]["devices"])} devices  {len(load(n)["puzzle"]["solution"])} actions'+((' '+str(errors)) if errors else ''))
    if verbose: print('\nLOGIC PASS',sum(1 for row in rows if row[1]),'/ 20')
    return all_good,rows

def main():
    parser=argparse.ArgumentParser(description='Escape Shift experimental puzzle lab')
    sub=parser.add_subparsers(dest='command',required=True)
    sub.add_parser('validate')
    r=sub.add_parser('replay');r.add_argument('level',type=int)
    i=sub.add_parser('inspect');i.add_argument('level',type=int)
    args=parser.parse_args()
    if args.command=='validate':
        ok,_=run_suite();sys.exit(0 if ok else 1)
    if args.command=='replay':
        success,errors,_=replay(args.level,True)
        if errors:print('\n'.join(errors));sys.exit(1)
    elif args.command=='inspect':
        data=load(args.level); p=data['puzzle']
        print(f'NIVEAU {args.level}: {p["title"]}\n{p["hint"]}\n')
        print('Mécaniques:',', '.join(p['concepts']))
        for d in p['devices']: print(f'  {d["id"]:16} {d["kind"]:22} ({d["x"]:5}, {d["y"]:3}) {d["width"]}x{d["height"]}')
        print('Solution de test:')
        for i,step in enumerate(p['solution'],1): print(f'  {i:2} {step}')

if __name__=='__main__': main()
