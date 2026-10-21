#!/usr/bin/env python3
"""List external method/field descriptors referenced by the supplied Java classfiles."""
import json,struct,sys,zipfile
from pathlib import Path

def references(data):
 pos=8
 def take(n):
  nonlocal pos
  b=data[pos:pos+n];pos+=n;return b
 def u1():return take(1)[0]
 def u2():return struct.unpack('>H',take(2))[0]
 pool=[None]*u2();i=1
 while i<len(pool):
  t=u1()
  if t==1:v=take(u2()).decode('utf-8','replace')
  elif t in (3,4):v=take(4).hex()
  elif t in (5,6):v=take(8).hex();pool[i]=(t,v);i+=1;pool[i]=None;i+=1;continue
  elif t in (7,8,16,19,20):v=u2()
  elif t in (9,10,11,12,17,18):v=(u2(),u2())
  elif t==15:v=(u1(),u2())
  else:raise ValueError(t)
  pool[i]=(t,v);i+=1
 def utf(n):return pool[n][1]
 result=[]
 for entry in pool:
  if entry is not None and entry[0] in (9,10,11):
   cls,nt=entry[1];name,desc=pool[nt][1]
   result.append((utf(pool[cls][1]),utf(name),utf(desc)))
 return result
if __name__=='__main__':
 found=set()
 with zipfile.ZipFile(sys.argv[1]) as z:
  for n in z.namelist():
   if n.endswith('.class'):found.update(references(z.read(n)))
 out=Path(sys.argv[2]);out.parent.mkdir(parents=True,exist_ok=True)
 out.write_text('\n'.join(' '.join(row) for row in sorted(found))+'\n')
 print(f'{len(found)} unique member references -> {out}')
