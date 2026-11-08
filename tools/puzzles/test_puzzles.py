#!/usr/bin/env python3
"""Local regression checks for Escape Shift puzzle prototypes.
These are logic tests, not movement/physics validation.
"""
import sys, unittest
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parent))
from puzzle_lab import load,replay,validate,PuzzleEngine

class PuzzleLabTest(unittest.TestCase):
    def test_twenty_schemas(self):
        for n in range(21,41):
            with self.subTest(level=n): self.assertEqual(validate(load(n)),[])
    def test_twenty_golden_scripts(self):
        for n in range(21,41):
            with self.subTest(level=n):
                ok,errors,_=replay(n)
                self.assertTrue(ok, str(errors))
    def test_no_easy_door_bypass(self):
        for n in range(21,41):
            with self.subTest(level=n):
                p=PuzzleEngine(load(n))
                self.assertFalse(p.act({'action':'exit','target':'doorA'}))
    def test_laser_window_expires(self):
        p=PuzzleEngine(load(22))
        p.act({'action':'push','target':'wheelA','value':'plateA'})
        self.assertTrue(p.get('flag:laser_disabled'))
        p.act({'action':'wait','target':'clock','value':91})
        self.assertFalse(p.get('flag:laser_disabled'))
        p.act({'action':'cross','target':'laserA'})
        self.assertFalse(p.act({'action':'exit','target':'doorA'}))
    def test_laser_success_quickly(self):
        ok, _, _=replay(22)
        self.assertTrue(ok)
    def test_mastermind_feedback_only_count(self):
        p=PuzzleEngine(load(25))
        first=load(25)['puzzle']['solution'][0]
        p.act(first)
        self.assertEqual(p.feedback['mastermind']['total'],5)
        self.assertEqual(p.feedback['mastermind']['matches'],1)
        self.assertFalse(p.get('solved:mastermind'))
    def test_mastermind_duplicate_crate_rejected(self):
        p=PuzzleEngine(load(25))
        with self.assertRaises(ValueError):
            p.act({'action':'submit','target':'mastermind','value':{f'pad{i}':'crate01' for i in range(1,6)}})
    def test_prime_decoy_locks_door(self):
        p=PuzzleEngine(load(27))
        p.act({'action':'collect','target':'n4'})
        for step in load(27)['puzzle']['solution']: p.act(step)
        self.assertTrue(p.failed)
        self.assertFalse(p.won)
    def test_word_out_of_order_locks(self):
        p=PuzzleEngine(load(29))
        p.act({'action':'collect','target':'V1'})
        for step in load(29)['puzzle']['solution']: p.act(step)
        self.assertFalse(p.won)
    def test_wrong_iq_choice_fails(self):
        p=PuzzleEngine(load(30))
        p.act({'action':'collect','target':'T20'})
        for step in load(30)['puzzle']['solution']: p.act(step)
        self.assertFalse(p.won)
    def test_platform_sync_requires_right_buttons(self):
        p=PuzzleEngine(load(23))
        p.act({'action':'place','target':'crateA','value':'pad2'})
        p.act({'action':'place','target':'crateB','value':'pad3'})
        p.act({'action':'press','target':'sync'})
        self.assertFalse(p.get('flag:speed_sync'))
    def test_heavy_plate_requires_hero_and_both_crates(self):
        p=PuzzleEngine(load(24))
        p.act({'action':'place','target':'crateA','value':'plateA'})
        p.act({'action':'place','target':'crateB','value':'plateA'})
        p.act({'action':'press','target':'latch'})
        self.assertFalse(p.get('flag:lift_lowered'))
        p.act({'action':'stand','target':'plateA'})
        p.act({'action':'press','target':'latch'})
        self.assertTrue(p.get('flag:lift_lowered'))
    def test_wrong_wheel_angle_rejected(self):
        p=PuzzleEngine(load(26))
        p.act({'action':'rotate','target':'wheelA','value':90})
        p.act({'action':'cross','target':'gapA'})
        self.assertFalse(p.act({'action':'exit','target':'doorA'}))
    def test_level30_requires_gravity_and_inverted_controls(self):
        p=PuzzleEngine(load(30))
        p.act({'action':'collect','target':'U21'})
        p.act({'action':'collect','target':'13'})
        self.assertFalse(p.act({'action':'exit','target':'doorA'}))
        # A separate attempt completes the whole sequence.
        good,errors,_=replay(30)
        self.assertTrue(good,str(errors))
    def test_worlds_correct(self):
        for n in range(21,31): self.assertEqual(load(n)['world'],3)
        for n in range(31,41): self.assertEqual(load(n)['world'],4)

if __name__=='__main__':unittest.main(verbosity=2)
