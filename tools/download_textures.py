from pathlib import Path
from concurrent.futures import ThreadPoolExecutor
from urllib.request import urlopen
import json,time
out=Path('assets/textures');out.mkdir(exist_ok=True)
names=['sun','mercury','venus_surface','venus_atmosphere','earth_daymap','earth_clouds','mars','jupiter','saturn','saturn_ring_alpha','uranus','neptune']
def get(name):
 ext='png' if 'ring' in name else 'jpg';fn=f'2k_{name}.{ext}';url='https://www.solarsystemscope.com/textures/download/'+fn
 for attempt in range(3):
  try:
   data=urlopen(url,timeout=45).read();(out/fn).write_bytes(data);return {'file':str(out/fn),'url':url,'bytes':len(data),'license':'CC BY 4.0','author':'Solar System Scope / INOVE'}
  except Exception as e:
   if attempt==2:return {'file':fn,'error':str(e)}
   time.sleep(1)
with ThreadPoolExecutor(max_workers=8) as ex:results=list(ex.map(get,names))
Path('reports/texture-downloads.json').write_text(json.dumps(results,indent=2));print(json.dumps(results,indent=2))
