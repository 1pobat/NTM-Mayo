#!/usr/bin/env python3
"""Original deterministic 16x16 Minecraft-style source assets; no upstream assets copied."""
from pathlib import Path
import json, struct, zlib
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources/assets/hbmmayo'
def write(path,s):
    p=ROOT/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(s,ensure_ascii=False,indent=2)+'\n')
def png(path,pixels):
    def chunk(t,d):return struct.pack('>I',len(d))+t+d+struct.pack('>I',zlib.crc32(t+d)&0xffffffff)
    data=b''.join(b'\0'+bytes(v for color in row for v in color) for row in pixels)
    p=ROOT/path;p.parent.mkdir(parents=True,exist_ok=True)
    p.write_bytes(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',16,16,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(data))+chunk(b'IEND',b''))
def color(c,delta=0):return tuple(max(0,min(255,v+delta)) for v in c)+(255,)
CLEAR=(0,0,0,0)
items={
'empty_mayonnaise_jar':('Empty Mayonnaise Jar','Пустая банка для майонеза',(167,200,202),'jar'),
'shielded_mayonnaise_jar':('Shielded Mayonnaise Jar','Экранированная банка для майонеза',(98,111,127),'jar'),
'industrial_mayonnaise_jar':('Industrial Mayonnaise (250 mB)','Технический майонез (250 мБ)',(235,224,137),'jar'),
'flashgold_mayonnaise_jar':('Flashgold Mayonnaise (250 mB)','Майонез с Flashgold (250 мБ)',(245,189,40),'jar'),
'empty_capsule':('Empty Capsule','Пустая капсула',(166,195,204),'capsule'),
'flashgold_colloid_capsule':('Flashgold Colloid Capsule','Капсула коллоида Flashgold',(255,201,43),'capsule'),
'empty_injector':('Empty Injector','Пустой инъектор',(147,183,185),'injector'),
'flashgold_stabilizer_injector':('Flashgold Stabilizer Injector','Инъектор стабилизатора Flashgold',(79,204,122),'injector'),
'passive_flashgold_stabilizer':('Passive Flashgold Stabilizer','Пассивный стабилизатор Flashgold',(51,133,87),'module'),
'entropy_reclaimer':('Entropy Reclaimer','Утилизатор энтропии',(52,47,64),'module'),
}
fluids={
'industrial_mayonnaise':('Industrial Mayonnaise','Технический майонез',(235,224,137)),
'flashgold_mayonnaise':('Flashgold Mayonnaise','Майонез с Flashgold',(245,189,40)),
'mayonnaise_emulsion':('Mayonnaise Emulsion','Майонезная эмульсия',(246,233,191)),
'flashgold_colloid':('Flashgold Colloid','Коллоид Flashgold',(255,204,24)),
'burnt_mayonnaise':('Burnt Mayonnaise','Пригоревший майонез',(71,39,22)),
'sunflower_seed_oil':('Sunflower Seed Oil','Подсолнечное масло',(218,178,45)),
'dissolved_egg':('Dissolved Egg','Растворённое яйцо',(240,204,121)),
'salient_green':('Salient Green','Салиент-грин',(92,188,73)),
}
blocks={'flashgold_stabilization_chamber':('Flashgold Stabilization Chamber','Камера стабилизации Flashgold',(66,117,91)),
'mayonnaise_coolant_adapter':('Mayonnaise Coolant Adapter','Адаптер майонезного охлаждения',(128,126,79))}
items['machine_frame']=('Machine Frame','Рама машины',(119,134,144),'frame')
en={};ru={}
for name,(eng,rus,c,shape) in items.items():
    px=[[CLEAR for x in range(16)] for y in range(16)]
    for y in range(2,15):
        for x in range(3,13):
            inside=(3<=y<=13 and 4<=x<=11) if shape=='jar' else (3<=y<=13 and 5<=x<=10) if shape=='capsule' else (4<=y<=12 and 3<=x<=12) if shape=='module' else (4<=y<=11 and 6<=x<=9)
            if inside:
                edge=(x in (4,11) or y in (3,13)) if shape=='jar' else (x in (5,10) or y in (3,13)) if shape=='capsule' else (x in (3,12) or y in (4,12)) if shape=='module' else (x in (6,9) or y in (4,11))
                px[y][x]=color(c,-65 if edge else ((x+y)%3)*7)
    if shape=='jar':
        for y in (2,3):
            for x in range(4,12):px[y][x]=color((130,136,142),20 if y==2 else -25)
        for x in range(6,10):px[9][x]=color((245,241,211))
        px[6][5]=color((255,255,234))
    elif shape=='module':
        for a in range(5,11):px[8][a]=color((220,169,44))
        for y in range(6,11):px[y][8]=color((255,211,73))
        for x in (4,11):
            for y in (5,11):px[y][x]=color((165,171,164))
    elif shape=='injector':
        for y in range(12,16):px[y][7]=color((165,182,188))
        for x in range(5,11):px[3][x]=color((103,114,120))
    if shape=='frame':
        px=[[CLEAR for x in range(16)] for y in range(16)]
        for y in range(2,14):
            for x in range(2,14):
                if x in (2,3,12,13) or y in (2,3,12,13):px[y][x]=color(c,25 if x==2 or y==2 else -20)
        for x in (3,12):
            for y in (3,12):px[y][x]=color((228,216,166))
    png(Path('textures/item')/(name+'.png'),px)
    write(Path('models/item')/(name+'.json'),{'parent':'minecraft:item/generated','textures':{'layer0':'hbmmayo:item/'+name}})
    write(Path('items')/(name+'.json'),{'model':{'type':'minecraft:model','model':'hbmmayo:item/'+name}})
    en['item.hbmmayo.'+name]=eng;ru['item.hbmmayo.'+name]=rus
for name,(eng,rus,c) in fluids.items():
    for style in ('still','flow'):
        px=[[color(c,((x*17+y*7+(x*y)%11)%5-2)*4) for x in range(16)] for y in range(16)]
        png(Path('textures/fluid')/(name+'_'+style+'.png'),px)
    en['fluid.hbmmayo.'+name]=eng;ru['fluid.hbmmayo.'+name]=rus
    en['block.hbmmayo.'+name]=eng;ru['block.hbmmayo.'+name]=rus
for name,(eng,rus,c) in blocks.items():
    px=[[color(c,-55 if x in (0,15) or y in (0,15) else -25 if x in (1,14) or y in (1,14) else (x+y)%3*3) for x in range(16)] for y in range(16)]
    for y in range(4,12):
        for x in range(4,12):px[y][x]=color((219,176,51) if x in (4,11) or y in (4,11) else (40,65,57))
    png(Path('textures/block')/(name+'.png'),px)
    write(Path('models/block')/(name+'.json'),{'parent':'minecraft:block/cube_all','textures':{'all':'hbmmayo:block/'+name}})
    write(Path('blockstates')/(name+'.json'),{'variants':{'':{'model':'hbmmayo:block/'+name}}})
    write(Path('items')/(name+'.json'),{'model':{'type':'minecraft:model','model':'hbmmayo:block/'+name}})
    en['block.hbmmayo.'+name]=eng;ru['block.hbmmayo.'+name]=rus
extra={
 'message.hbmmayo.machine_status':('HE: %s | Coolant: %s mB | Service: %s ticks','Энергия: %s HE | Теплоноситель: %s мБ | Работа: %s тиков'),
 'subtitles.hbmmayo.flashgold_annihilation':('Flashgold annihilates','Аннигиляция Flashgold'),
 'death.attack.hbmmayo.flashgold_annihilation':('%1$s was annihilated by Flashgold','%1$s аннигилировал из-за Flashgold'),
 'death.attack.hbmmayo.flashgold_annihilation.player':('%1$s was annihilated by Flashgold while fighting %2$s','%1$s аннигилировал из-за Flashgold в бою с %2$s'),
 'gui.hbmmayo.instability':('FLASHGOLD INSTABILITY: %s%%','НЕСТАБИЛЬНОСТЬ FLASHGOLD: %s%%'),
 'itemGroup.hbmmayo':('HBM Mayonnaise','HBM Mayonnaise'),
 'message.hbmmayo.already_infused':('Flashgold Infusion is already active.','Усиление Flashgold уже активно.'),
 'message.hbmmayo.not_infused':('Flashgold Infusion is required.','Требуется усиление Flashgold.'),
 'message.hbmmayo.injector_cooldown':('Injector cooldown: %s s','Перезарядка инъектора: %s с'),
 'message.hbmmayo.coolant_burnt':('Drain all burnt mayonnaise before refilling.','Удалите весь пригоревший майонез перед заправкой.'),
 'message.hbmmayo.module_conflict':('Only one Flashgold chest module may be installed.','Можно установить только один нагрудный модуль Flashgold.'),
}
for key,(eng,rus) in extra.items():en[key]=eng;ru[key]=rus
write(Path('lang/en_us.json'),en);write(Path('lang/ru_ru.json'),ru)

# Native NTM sprite paths for the five registered addon fluids.
import shutil
for name in ('industrial_mayonnaise','flashgold_mayonnaise','mayonnaise_emulsion','flashgold_colloid','burnt_mayonnaise'):
    for folder in ('textures/gui/fluids','textures/block/fluid','textures/models/tank'):
        dst=ROOT/folder/(name+'.png');dst.parent.mkdir(parents=True,exist_ok=True)
        shutil.copy(ROOT/'textures/fluid'/(name+'_still.png'),dst)
