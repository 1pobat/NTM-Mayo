#!/usr/bin/env python3
"""Inspect a Java 25 JAR without loading classes or requiring javap/JDK 25."""
import argparse, hashlib, json, re, struct, zipfile
from pathlib import Path

class ClassReader:
    def __init__(self, data): self.data=data; self.pos=0
    def take(self,n):
        if self.pos+n > len(self.data): raise ValueError('Truncated class file')
        result=self.data[self.pos:self.pos+n]; self.pos+=n; return result
    def u1(self): return self.take(1)[0]
    def u2(self): return struct.unpack('>H',self.take(2))[0]
    def u4(self): return struct.unpack('>I',self.take(4))[0]
    def attributes(self):
        for _ in range(self.u2()): self.u2(); self.take(self.u4())
    def members(self,pool):
        result=[]
        for _ in range(self.u2()):
            access,name,descriptor=self.u2(),self.u2(),self.u2()
            if access & 5: result.append(dict(name=pool[name],descriptor=pool[descriptor],access=access))
            self.attributes()
        return result
    def parse(self):
        if self.u4()!=0xcafebabe: raise ValueError('Not a Java class')
        minor,major=self.u2(),self.u2(); pool=[None]*self.u2(); i=1
        while i<len(pool):
            tag=self.u1()
            if tag==1: pool[i]=self.take(self.u2()).decode('utf-8',errors='replace')
            elif tag in (3,4): self.take(4)
            elif tag in (5,6): self.take(8); i+=1
            elif tag in (7,8,16,19,20): pool[i]=self.u2()
            elif tag in (9,10,11,12,17,18): self.take(4)
            elif tag==15: self.take(3)
            else: raise ValueError(f'Unknown constant pool tag {tag}')
            i+=1
        access,cls,sup=self.u2(),self.u2(),self.u2()
        interfaces=[pool[pool[self.u2()]] for _ in range(self.u2())]
        return dict(name=pool[pool[cls]],superclass=pool[pool[sup]] if sup else None,
                    major=major,minor=minor,access=access,interfaces=interfaces,
                    fields=self.members(pool),methods=self.members(pool))

def audit(path):
    with zipfile.ZipFile(path) as z:
        manifest=z.read('META-INF/MANIFEST.MF').decode('utf-8').replace('\r\n','\n').replace('\n ','')
        names=[n for n in z.namelist() if n.endswith('.class') and not n.startswith('META-INF/')]
        # Full descriptors make overloads distinguishable; this does not infer registry IDs.
        classes=[ClassReader(z.read(n)).parse() for n in names]
        mods=[n for n in z.namelist() if n.endswith('neoforge.mods.toml')]
        metadata={n:z.read(n).decode('utf-8') for n in mods}
        patterns=re.compile(r'fluid|mixer|chem|purex|rbmk|lubri|armor|shield|damage|energy|particle|upgrade|flashgold',re.I)
        relevant=[c for c in classes if patterns.search(c['name'])]
        return dict(sha256=hashlib.sha256(path.read_bytes()).hexdigest(),manifest=manifest,
                    mod_metadata=metadata,class_count=len(classes),all_class_names=[c['name'] for c in classes],
                    relevant_classes=relevant)

def main():
    parser=argparse.ArgumentParser(); parser.add_argument('jar',type=Path)
    parser.add_argument('--output',type=Path,default=Path('build/ntm-api.json')); args=parser.parse_args()
    if not args.jar.is_file(): parser.error(f'Local NTM JAR is absent: {args.jar.resolve()}')
    result=audit(args.jar); args.output.parent.mkdir(parents=True,exist_ok=True)
    args.output.write_text(json.dumps(result,indent=2),encoding='utf-8')
    print(f"Audited {result['class_count']} classes; report: {args.output}")
    print(f"SHA-256: {result['sha256']}")
if __name__=='__main__': main()
