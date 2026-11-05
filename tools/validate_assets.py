from pathlib import Path
import wave,json,struct,hashlib
import numpy as np,trimesh
from PIL import Image
R=Path(__file__).resolve().parents[1];errors=[];report={'models':[],'audio':[],'images':[]}
for f in sorted((R/'assets/models').glob('*.glb')):
 raw=f.read_bytes();magic,version,length=struct.unpack_from('<III',raw);assert magic==0x46546c67 and version==2 and length==len(raw),f
 n=struct.unpack_from('<I',raw,12)[0];j=json.loads(raw[20:20+n]);sc=trimesh.load(f);assert len(sc.geometry)>0
 for mesh in sc.geometry.values():assert np.isfinite(mesh.vertices).all() and len(mesh.faces)>0
 anim=j.get('animations',[])
 if f.stem!='crew_rocket':assert anim and any(c['target']['path']=='rotation' for a in anim for c in a['channels'])
 if f.stem=='sun':assert sum(c['target']['path']=='scale' for a in anim for c in a['channels'])==32
 report['models'].append({'file':f.name,'bytes':len(raw),'meshCount':len(sc.geometry),'triangleCount':sum(len(m.faces) for m in sc.geometry.values()),'animationClips':len(anim)})
assert len(report['models'])==10
for f in sorted((R/'assets/audio').rglob('*.wav')):
 with wave.open(str(f),'rb') as w:
  assert w.getsampwidth()==2 and w.getframerate()==24000;frames=w.getnframes();data=np.frombuffer(w.readframes(frames),dtype='<i2');peak=int(np.abs(data.astype(np.int32)).max());assert frames>0 and peak>30 and peak<32767,f
  report['audio'].append({'file':str(f.relative_to(R/'assets')),'durationSeconds':round(frames/24000,3),'channels':w.getnchannels(),'peak':peak})
assert len(list((R/'assets/audio').glob('*/*.wav')))==18
locales=[json.loads((R/'assets/locales'/f'{s}.json').read_text()) for s in ['zh-Hans','zh-Hant','en']];assert all(set(x)==set(locales[0]) and all(x.values()) for x in locales);report['locales']={'languages':3,'keysPerLanguage':len(locales[0])}
for f in (R/'assets/preview').glob('*.png'):
 with Image.open(f) as im:im.verify()
 report['images'].append(f.name)
with Image.open(R/'assets/preview/sun-earth-motion.gif') as im:assert im.n_frames==24
assert not list(R.glob('app/src/**/*.kt'))
report['status']='PASS';(R/'reports/asset-validation.json').write_text(json.dumps(report,ensure_ascii=False,indent=2))
print('PASS:',len(report['models']),'GLBs;',len(report['audio']),'WAVs; 3 languages ×',len(locales[0]),'keys;',len(report['images']),'PNG previews; 24 GIF frames.')
manifest=[]
for f in sorted((R/'assets').rglob('*')):
 if f.is_file():manifest.append({'file':str(f.relative_to(R)),'bytes':f.stat().st_size,'sha256':hashlib.sha256(f.read_bytes()).hexdigest()})
(R/'reports/asset-checksums.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2))
