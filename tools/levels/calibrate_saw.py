#!/usr/bin/env python3
"""Try alternative saw phases through the REAL Kotlin World2Playtester.

It never overwrites the level unless --apply. Keep game closed while calibrating.
Example: py tools/levels/calibrate_saw.py 18 saw_02 --step 8 --apply
"""
from __future__ import annotations
import argparse, json, subprocess
from pathlib import Path
from datetime import datetime

PROJECT = Path(__file__).resolve().parents[2]
LEVELS = PROJECT / 'app/src/main/assets/levels'
SIMULATOR = PROJECT / 'tools/levels/level_simulator.jar'


def main():
    ap=argparse.ArgumentParser(description='Find a safe moving-saw phase with the real Kotlin physics')
    ap.add_argument('level', type=int)
    ap.add_argument('saw_id')
    ap.add_argument('--step', type=int, default=8)
    ap.add_argument('--apply', action='store_true', help='Save the first winning phase')
    a=ap.parse_args()
    if not 11<=a.level<=20: ap.error('Only levels 11..20')
    if not 1<=a.step<=80: ap.error('step must be 1..80')
    if not SIMULATOR.exists(): ap.error('Missing level_simulator.jar')
    path=LEVELS / f'level_{a.level:02}.json'
    original=path.read_bytes()
    data=json.loads(original)
    matches=[o for o in data['objects'] if o.get('id')==a.saw_id and o.get('type')=='saw']
    if len(matches)!=1: ap.error(f'Cannot find saw {a.saw_id}')
    saw=matches[0]
    found=[]
    try:
        for phase in range(0,saw.get('period',160),a.step):
            saw['phase']=phase
            path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
            result=subprocess.run(['java','-jar',str(SIMULATOR),str(LEVELS),str(a.level)],
                                  cwd=PROJECT,capture_output=True,text=True)
            if result.returncode==0:
                found.append(phase)
                print(f'PASS phase={phase}')
                if a.apply: break
    finally:
        path.write_bytes(original)
    if not found:
        raise SystemExit('No passing phase found. Adjust level geometry or virtual-player policy.')
    print('Safe candidates:',found)
    if a.apply:
        backup=PROJECT/'tools/levels/backups'/f'level_{a.level:02}_{datetime.now():%Y%m%d_%H%M%S}.json'
        backup.parent.mkdir(parents=True,exist_ok=True)
        backup.write_bytes(original)
        data=json.loads(original); next(o for o in data['objects'] if o['id']==a.saw_id)['phase']=found[0]
        path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
        print('Applied safe phase',found[0],'backup:',backup)

if __name__=='__main__': main()
