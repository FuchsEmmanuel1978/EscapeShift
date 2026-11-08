#!/usr/bin/env python3
"""Creates schematic 2D inspection maps directly from level puzzle devices.
The PNG maps are diagrams for design review, not screenshots or game sprites.
"""
from pathlib import Path
import sys,math
sys.path.insert(0,str(Path(__file__).resolve().parent))
from puzzle_lab import load
from PIL import Image,ImageDraw,ImageFont
ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'tools/puzzles/previews';OUT.mkdir(parents=True,exist_ok=True)
try:
  REG=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',15)
  SMALL=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',12)
  BOLD=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf',18)
except OSError:
  REG=SMALL=BOLD=ImageFont.load_default()
COLORS={'crate':'#E88A30','pressure_button':'#43B5B8','heavy_pressure_plate':'#43B5B8','weight_button':'#43B5B8','timed_button':'#43B5B8',
 'speed_button':'#43B5B8','code_button':'#43B5B8','inverse_button':'#43B5B8','pushable_wheel':'#F2F4F6','rotating_wheel':'#F2F4F6',
 'moving_platform':'#91A6B9','sliding_platform':'#91A6B9','adjustable_platform':'#91A6B9','lowering_platform':'#91A6B9',
 'number_token':'#F0C56F','letter_token':'#F0C56F','iq_token':'#F0C56F','exit_gate':'#F2E4D6','word_exit':'#F2E4D6',
 'false_exit':'#E96B6B','laser':'#E96B6B','surprise_wall':'#9AA0A8','lift_wall':'#9AA0A8',
 'gravity_zone':'#A77CE0','spring':'#71CD84','movable_spring':'#71CD84','ground_gap':'#2B2927',
 'word_board':'#F0C56F','sequence_board':'#F0C56F','moving_spike':'#E96B6B'}

def draw_level(n):
    data=load(n);p=data['puzzle'];W=1100;H=212
    img=Image.new('RGB',(W,H),'#10181F');d=ImageDraw.Draw(img)
    d.text((20,12),f'{n:02d}  {p["title"]}',fill='#FCFCFC',font=BOLD)
    d.text((20,42),' + '.join(p['concepts'][:4]),fill='#C9D4D9',font=REG)
    y=132; d.line((20,y,W-20,y),fill='#F17A2A',width=6)
    d.rectangle((20,y+6,W-20,H-18),fill='#29343C')
    d.line((18,82,1080,82),fill='#3D4852',width=1)
    scale=(W-75)/data['worldWidth']
    for idx,obj in enumerate(p['devices']):
        col=COLORS.get(obj['kind'],'#A8B8C1')
        x=25+int(obj['x']*scale); rect_w=max(8,min(58,int(obj['width']*scale)))
        if obj['kind']=='ground_gap':
            d.rectangle((x,y-2,x+rect_w,H-18),fill='#10181F')
            d.line((x,y,x+rect_w,y),fill='#D54E41',width=2)
        elif 'wheel' in obj['kind']:
            d.ellipse((x,93,x+rect_w,93+rect_w),fill=col,outline='#0C1217',width=2)
            d.ellipse((x+rect_w*.38,93+rect_w*.38,x+rect_w*.62,93+rect_w*.62),fill='#10181F')
        else:
            top=90+((idx%3)*10)
            d.rounded_rectangle((x,top,x+rect_w,top+25),radius=3,fill=col)
        show=obj['id'].replace('letter_','l_').replace('crate','c').replace('platform','p').replace('pad','b')
        d.text((x,154+(idx%2)*19),show[:10],fill='#E3E7E8',font=SMALL)
    d.text((20,H-16),p['hint'][:116],fill='#F0C56F',font=SMALL)
    img.save(OUT/f'level_{n:02d}.png')
    return img

for world,nums in [(3,range(21,31)),(4,range(31,41))]:
    rows=[draw_level(n) for n in nums]
    montage=Image.new('RGB',(1100,len(rows)*212),'#10181F')
    for i,img in enumerate(rows):montage.paste(img,(0,i*212))
    montage.save(OUT/f'monde_{world}_10_puzzles.png',optimize=True)
print('Generated',len(list(OUT.glob('level_*.png'))),'individual maps + 2 overview boards in',OUT)
