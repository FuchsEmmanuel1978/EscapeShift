#!/usr/bin/env python3
"""Try safe movement phases for a World 1 saw or monster using actual GameWorld physics.

Examples:
  py tools/levels/tune_patrol_world1.py 7 challenge_extra_01 --step 10
  py tools/levels/tune_patrol_world1.py 7 challenge_extra_01 --step 10 --apply

Works with a compiled `tools/levels/level_simulator.jar` (packaged in V22).
Only --apply changes a level; the original is restored even on failure.
"""
from __future__ import annotations
import argparse
from datetime import datetime
import json
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[2]
LEVELS = ROOT / 'app/src/main/assets/levels'
SIMULATOR = ROOT / 'tools/levels/level_simulator.jar'

def main():
    p = argparse.ArgumentParser(description='Calibrate World 1 saw/monster movement with Kotlin physics')
    p.add_argument('level', type=int)
    p.add_argument('object_id')
    p.add_argument('--step', type=int, default=10)
    p.add_argument('--apply', action='store_true')
    args = p.parse_args()
    if args.level not in range(1, 11): p.error('Only levels 1-10 are handled')
    if not 1 <= args.step <= 60: p.error('--step must be between 1 and 60')
    if not SIMULATOR.exists(): p.error('Simulator JAR missing: compile Kotlin or download complete V22')
    path = LEVELS / f'level_{args.level:02}.json'
    original = path.read_bytes()
    obj = json.loads(original)
    hits = [x for x in obj['objects'] if x.get('id') == args.object_id and x.get('type') in ('saw', 'monster')]
    if len(hits) != 1: p.error('Object must be a saw or monster ID found in the level JSON')
    moving = hits[0]
    original_phase = moving.get('phase', 0)
    solutions = []
    try:
        for phase in range(0, moving.get('period', 150), args.step):
            moving['phase'] = phase
            path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
            result = subprocess.run(['java', '-jar', str(SIMULATOR), str(LEVELS), str(args.level)], cwd=ROOT, capture_output=True, text=True)
            if result.returncode == 0:
                solutions.append(phase)
                print(f'PASS: level={args.level:02} object={args.object_id} phase={phase}')
                if args.apply: break
    finally:
        path.write_bytes(original)
    if not solutions:
        raise SystemExit('No winning phase found. Try changing the obstacle position or the route.')
    print(f'Winning phase candidates: {solutions}')
    if args.apply:
        backups = ROOT / 'tools/levels/backups'
        backups.mkdir(exist_ok=True, parents=True)
        backup = backups / f'level_{args.level:02}_{datetime.now():%Y%m%d_%H%M%S}.json'
        backup.write_bytes(original)
        obj = json.loads(original)
        selected = next(x for x in obj['objects'] if x['id'] == args.object_id)
        selected['phase'] = solutions[0]
        path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
        print(f'Saved phase {original_phase} -> {solutions[0]}. Backup: {backup}')

if __name__ == '__main__': main()
