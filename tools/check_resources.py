#!/usr/bin/env python3
"""Check actual native recipe resources and references against the supplied NTM binary."""
from pathlib import Path
import hashlib,json,struct,zlib,zipfile
ROOT=Path(__file__).resolve().parents[1]
R=ROOT/'src/main/resources'
jar=ROOT/'ntmjar/ntm-next.jar'
expected='923a0a2bea1deded10a71cc7273cb687d728b27fe1c2ba06774150f09c28a273'
assert hashlib.sha256(jar.read_bytes()).hexdigest()==expected,'Wrong NTM binary'
jsonfiles=list(R.rglob('*.json'))
for p in jsonfiles:json.loads(p.read_text())
recipes={p.stem:json.loads(p.read_text()) for p in (R/'data/hbmmayo/recipe').glob('*.json')}
used=set()
def item(value):
 if isinstance(value,str):
  assert ':' in value,value
  used.add(value)
for name,r in recipes.items():
 if r['type'].startswith('hbm:'):
  assert r['type'] in {'hbm:mixer','hbm:chemical_plant','hbm:purex'}
  assert r['duration']>0
  for f in r.get('input_fluids',[])+r.get('output_fluids',[]):assert f['amount']>0
  for i in r.get('input_items',[]):item(i['ingredient']);assert i.get('count',1)>0
  for i in r.get('output_items',[]):item(i['id']);assert i.get('count',1)>0
 else:
  assert r['type'] in {'minecraft:crafting_shaped','minecraft:crafting_shapeless'}
  for v in r.get('key',{}).values():item(v)
  for v in r.get('ingredients',[]):item(v)
  item(r['result']['id'])
with zipfile.ZipFile(jar) as z:
 names=set(z.namelist())
 for id in used:
  namespace,path=id.split(':',1)
  if namespace=='hbm':assert f'assets/hbm/items/{path}.json' in names,f'Unverified native item reference: {id}'
  if namespace=='hbmmayo':assert (R/f'assets/hbmmayo/items/{path}.json').is_file(),id
# Recipe proportions and container costs are specification-level invariants.
emulsion=recipes['mayonnaise_emulsion']
assert [f['amount'] for f in emulsion['input_fluids']]==[700,300]
assert emulsion['output_fluids'][0]['amount']==1000 and emulsion['duration']==160
assert 'power' not in emulsion,'Mixer energy override was explicitly waived'
industrial=recipes['industrial_mayonnaise']
assert [f['amount'] for f in industrial['input_fluids']]==[1000,100]
assert industrial['output_fluids'][0]['amount']==1000
filling=recipes['industrial_filling']
assert filling['input_items'][0]['count']==4 and filling['output_items'][0]['count']==4
colloid=recipes['flashgold_colloid']
assert [i['ingredient'] for i in colloid['input_items']]==['hbm:billet_balefire_gold','hbm:pellet_charged']
assert colloid['input_fluids'][0]['amount']==colloid['output_fluids'][0]['amount']==500
flash=recipes['flashgold_mayonnaise']
assert [f['amount'] for f in flash['input_fluids']]==[1000,500]
assert flash['output_fluids'][0]['amount']==1500
assert recipes['flashgold_filling']['input_fluids'][0]['amount']==250
for name,pattern in [('passive_flashgold_stabilizer',['SCS','GFG','SES']),('entropy_reclaimer',['TAT','GFG','TET'])]:
 r=recipes[name];assert r['pattern']==pattern and r['key']['E']=='hbm:red_connector'
 assert not any('battery' in id for id in r['key'].values())
for name in ['mayonnaise_coolant_adapter','flashgold_stabilization_chamber']:
 loot=json.loads((R/f'data/hbmmayo/loot_table/blocks/{name}.json').read_text())
 fn=loot['pools'][0]['entries'][0]['functions'][0]
 assert fn['function']=='minecraft:copy_components' and fn['include']==['hbmmayo:machine_state']
langs=[json.loads((R/f'assets/hbmmayo/lang/{l}.json').read_text()) for l in ['en_us','ru_ru']]
assert langs[0].keys()==langs[1].keys()
for id in used:
 if id.startswith('hbmmayo:'):
  name=id.split(':',1)[1]
  assert any(f'{kind}.hbmmayo.{name}' in langs[0] for kind in ['item','block']),name
images=list((R/'assets/hbmmayo').rglob('*.png'))
for p in images:
 data=p.read_bytes();assert data[:8]==b'\x89PNG\r\n\x1a\n';assert struct.unpack('>II',data[16:24])==(16,16)
 offset=8
 while offset<len(data):
  length=struct.unpack('>I',data[offset:offset+4])[0]
  assert zlib.crc32(data[offset+4:offset+8+length])&0xffffffff==struct.unpack('>I',data[offset+8+length:offset+12+length])[0]
  offset+=length+12
print(f'PASS: {len(recipes)} native/crafting recipes, {len(jsonfiles)} JSON files, {len(images)} PNG files, localization and NTM resource references')
print('Not a Minecraft registry-load or in-game processing test.')
